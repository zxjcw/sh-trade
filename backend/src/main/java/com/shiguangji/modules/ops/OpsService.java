package com.shiguangji.modules.ops;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.shiguangji.common.dto.Dtos.ArbitrationDto;
import com.shiguangji.common.dto.Dtos.CategoryStatDto;
import com.shiguangji.common.dto.Dtos.ItemDto;
import com.shiguangji.common.dto.Dtos.MonthlyStatDto;
import com.shiguangji.common.dto.Dtos.ReportDataDto;
import com.shiguangji.common.entity.AppUser;
import com.shiguangji.common.entity.Arbitration;
import com.shiguangji.common.entity.AuditRecord;
import com.shiguangji.common.entity.Item;
import com.shiguangji.common.entity.ItemHistory;
import com.shiguangji.common.entity.Review;
import com.shiguangji.common.entity.TradeOrder;
import com.shiguangji.common.exception.BizException;
import com.shiguangji.common.mapper.AppUserMapper;
import com.shiguangji.common.mapper.ArbitrationMapper;
import com.shiguangji.common.mapper.AuditRecordMapper;
import com.shiguangji.common.mapper.CreditRecordMapper;
import com.shiguangji.common.mapper.ItemHistoryMapper;
import com.shiguangji.common.mapper.ItemMapper;
import com.shiguangji.common.mapper.ReportMapper;
import com.shiguangji.common.mapper.ReviewMapper;
import com.shiguangji.common.mapper.TradeOrderMapper;
import com.shiguangji.common.security.CurrentUser;
import com.shiguangji.modules.item.ItemAssembler;
import com.shiguangji.modules.notification.NotificationService;
import com.shiguangji.modules.payment.LedgerService;
import com.shiguangji.common.entity.CreditRecord;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class OpsService {

    private final ItemMapper itemMapper;
    private final ItemHistoryMapper historyMapper;
    private final AuditRecordMapper auditRecordMapper;
    private final ArbitrationMapper arbitrationMapper;
    private final ReviewMapper reviewMapper;
    private final TradeOrderMapper orderMapper;
    private final com.shiguangji.common.mapper.BountyMapper bountyMapper;
    private final CreditRecordMapper creditRecordMapper;
    private final AppUserMapper userMapper;
    private final ReportMapper reportMapper;
    private final ItemAssembler assembler;
    private final NotificationService notificationService;
    private final LedgerService ledgerService;

    /**
     * 审核请求。前端传 { result: 'approved' | 'rejected', reason }；
     * 同时兼容旧的 { approved: true/false } 形式。
     */
    public record AuditRequest(String result, Boolean approved, String reason) {
        boolean isApproved() {
            if (result != null && !result.isBlank()) {
                return "approved".equalsIgnoreCase(result.trim());
            }
            return Boolean.TRUE.equals(approved);
        }
    }

    /** 仲裁处理请求：{ result: 'buyer' | 'seller', note }。 */
    public record ResolveRequest(String result, String note) {
    }

    public List<ItemDto> pendingItems() {
        return itemMapper.selectList(new LambdaQueryWrapper<Item>()
                        .eq(Item::getStatus, "pending")
                        .orderByAsc(Item::getCreatedAt))
                .stream().map(item -> assembler.toDto(item, CurrentUser.id())).toList();
    }

    @Transactional
    public ItemDto audit(Long itemId, AuditRequest request) {
        Long me = CurrentUser.id();
        Item item = itemMapper.selectById(itemId);
        if (item == null) {
            throw BizException.notFound("物品不存在");
        }
        if (!"pending".equals(item.getStatus())) {
            throw BizException.badRequest("该物品不在待审核状态");
        }
        boolean approved = request != null && request.isApproved();
        LocalDateTime now = LocalDateTime.now();

        AuditRecord record = new AuditRecord();
        record.setItemId(itemId);
        record.setAuditorId(me);
        record.setResult(approved ? "approved" : "rejected");
        record.setReason(approved ? null : (request == null ? null : request.reason()));
        record.setCreatedAt(now);
        auditRecordMapper.insert(record);

        if (approved) {
            item.setStatus("on_sale");
            item.setListedAt(now);
            item.setRejectReason(null);
            item.setUpdatedAt(now);
            itemMapper.updateById(item);
            ItemHistory history = new ItemHistory();
            history.setItemId(itemId);
            history.setEventType("created");
            history.setEventTime(now);
            history.setDescription("建档上架：" + item.getTitle());
            history.setRelatedUserId(item.getCurrentHolderId());
            historyMapper.insert(history);
            notificationService.send(item.getCurrentHolderId(), "audit", "item", itemId,
                    "你的物品「" + item.getTitle() + "」已审核通过并上架。", "/item/" + itemId);
            matchBounties(item);
        } else {
            item.setStatus("rejected");
            item.setRejectReason(record.getReason() == null ? "不符合发布规范" : record.getReason());
            item.setUpdatedAt(now);
            itemMapper.updateById(item);
            notificationService.send(item.getCurrentHolderId(), "audit", "item", itemId,
                    "你的物品「" + item.getTitle() + "」未通过审核：" + item.getRejectReason(), "/item/" + itemId);
        }
        return assembler.toDto(item, me);
    }

    private void matchBounties(Item item) {
        List<com.shiguangji.common.entity.Bounty> bounties = bountiesOf(item);
        for (var bounty : bounties) {
            bounty.setMatchCount((bounty.getMatchCount() == null ? 0 : bounty.getMatchCount()) + 1);
            bountyMapper.updateById(bounty);
            notificationService.send(bounty.getBuyerId(), "bounty", "item", item.getItemId(),
                    "你求购的物品「" + item.getTitle() + "」已上架，预算吻合，快去看看。", "/item/" + item.getItemId());
        }
    }

    private List<com.shiguangji.common.entity.Bounty> bountiesOf(Item item) {
        List<com.shiguangji.common.entity.Bounty> result = new ArrayList<>();
        if (item.getPrice() == null) {
            return result;
        }
        List<com.shiguangji.common.entity.Bounty> open = bountyMapper.selectList(
                new LambdaQueryWrapper<com.shiguangji.common.entity.Bounty>()
                        .eq(com.shiguangji.common.entity.Bounty::getStatus, "open"));
        for (var bounty : open) {
            if (bounty.getCategoryId() != null && !Objects.equals(bounty.getCategoryId(), item.getCategoryId())) {
                continue;
            }
            if (bounty.getKeyword() != null && !bounty.getKeyword().isBlank()
                    && (item.getTitle() == null || !item.getTitle().contains(bounty.getKeyword().trim()))) {
                continue;
            }
            if (bounty.getBudget() != null) {
                BigDecimal tolerance = bounty.getPriceTolerance() == null
                        ? BigDecimal.valueOf(20) : bounty.getPriceTolerance();
                BigDecimal ratio = tolerance.divide(BigDecimal.valueOf(100));
                BigDecimal low = bounty.getBudget().multiply(BigDecimal.ONE.subtract(ratio));
                BigDecimal high = bounty.getBudget().multiply(BigDecimal.ONE.add(ratio));
                if (item.getPrice().compareTo(low) < 0 || item.getPrice().compareTo(high) > 0) {
                    continue;
                }
            }
            result.add(bounty);
        }
        return result;
    }

    public List<ArbitrationDto> arbitrations() {
        return arbitrationMapper.selectList(new LambdaQueryWrapper<Arbitration>()
                        .orderByAsc(Arbitration::getStatus)
                        .orderByDesc(Arbitration::getCreatedAt))
                .stream().map(this::toDto).toList();
    }

    @Transactional
    public ArbitrationDto resolve(Long id, ResolveRequest request) {
        Long me = CurrentUser.id();
        Arbitration arbitration = arbitrationMapper.selectById(id);
        if (arbitration == null) {
            throw BizException.notFound("仲裁记录不存在");
        }
        if (!"pending".equals(arbitration.getStatus())) {
            throw BizException.badRequest("该仲裁已处理");
        }
        String result = request == null || request.result() == null || request.result().isBlank()
                ? "seller" : request.result().trim();
        if (!result.equals("buyer") && !result.equals("seller")) {
            throw BizException.badRequest("处理结果只能是 buyer 或 seller");
        }
        arbitration.setStatus("resolved");
        arbitration.setResult(result);
        arbitration.setHandlerId(me);
        arbitration.setResolvedAt(LocalDateTime.now());
        if (request != null && request.note() != null && !request.note().isBlank()) {
            String evidence = arbitration.getEvidence() == null ? "" : arbitration.getEvidence();
            String note = "【处理说明】" + request.note().trim();
            String merged = evidence.isBlank() ? note : evidence + " " + note;
            arbitration.setEvidence(merged.length() > 1000 ? merged.substring(0, 1000) : merged);
        }
        arbitrationMapper.updateById(arbitration);

        if ("order".equals(arbitration.getObjectType())) {
            TradeOrder order = orderMapper.selectById(arbitration.getObjectId());
            if (order != null) {
                settleOrder(order, result);
            }
        }
        notificationService.send(arbitration.getApplicantId(), "arbitration", "arbitration", id,
                "仲裁已处理，结果：" + ("buyer".equals(result) ? "支持买家" : "支持卖家") + "。", null);
        return toDto(arbitration);
    }

    private void settleOrder(TradeOrder order, String result) {
        String orderNo = order.getOrderNo();
        if ("buyer".equals(result)) {
            if (!"refunded".equals(order.getAfterSaleStatus()) && order.getPaidAt() != null
                    && order.getAmount() != null && order.getAmount().compareTo(BigDecimal.ZERO) > 0) {
                boolean paidToSeller = "completed".equals(order.getTradeStatus());
                if (paidToSeller) {
                    try {
                        ledgerService.chargebackSeller(orderNo, order.getSellerId(), order.getAmount());
                    } catch (BizException ignored) {
                        // 卖家余额不足时不做扣回，仍按退款处理（差额由平台承担）
                    }
                }
                ledgerService.refundToBuyer(orderNo, order.getBuyerId(), order.getAmount());
            }
            order.setAfterSaleStatus("refunded");
            order.setUpdatedAt(LocalDateTime.now());
            orderMapper.updateById(order);
            itemMapper.update(null, new LambdaUpdateWrapper<Item>()
                    .eq(Item::getItemId, order.getItemId())
                    .eq(Item::getStatus, "locked")
                    .set(Item::getStatus, "on_sale")
                    .set(Item::getUpdatedAt, LocalDateTime.now()));
            List<Review> reviews = reviewMapper.selectList(new LambdaQueryWrapper<Review>()
                    .eq(Review::getOrderNo, orderNo)
                    .ne(Review::getStatus, "void"));
            for (Review review : reviews) {
                review.setStatus("void");
                reviewMapper.updateById(review);
            }
            rollbackCredit(orderNo, order.getBuyerId());
            rollbackCredit(orderNo, order.getSellerId());
            notificationService.send(order.getBuyerId(), "arbitration", "order", orderNo,
                    "仲裁结果支持买家，款项已退回钱包。", orderNo);
        } else {
            if ("refunding".equals(order.getAfterSaleStatus()) || "arbitrating".equals(order.getAfterSaleStatus())) {
                order.setAfterSaleStatus("none");
                order.setUpdatedAt(LocalDateTime.now());
                orderMapper.updateById(order);
            }
            notificationService.send(order.getBuyerId(), "arbitration", "order", orderNo,
                    "仲裁结果支持卖家，售后已驳回。", orderNo);
        }
        notificationService.send(order.getSellerId(), "arbitration", "order", orderNo,
                "订单 " + orderNo + " 的仲裁已处理完毕。", orderNo);
    }

    private void rollbackCredit(String orderNo, Long userId) {
        Long exists = creditRecordMapper.selectCount(new LambdaQueryWrapper<CreditRecord>()
                .eq(CreditRecord::getUserId, userId)
                .eq(CreditRecord::getOrderNo, orderNo)
                .eq(CreditRecord::getReason, "arbitration_rollback"));
        if (exists != null && exists > 0) {
            return;
        }
        if (creditRecordMapper.selectCount(new LambdaQueryWrapper<CreditRecord>()
                .eq(CreditRecord::getUserId, userId)
                .eq(CreditRecord::getOrderNo, orderNo)
                .eq(CreditRecord::getReason, "order_complete")) == 0) {
            return;
        }
        userMapper.addCredit(userId, -2);
        CreditRecord record = new CreditRecord();
        record.setUserId(userId);
        record.setChangeValue(-2);
        record.setReason("arbitration_rollback");
        record.setOrderNo(orderNo);
        record.setCreatedAt(LocalDateTime.now());
        creditRecordMapper.insert(record);
    }

    public ReportDataDto report() {
        long total = reportMapper.totalOrders();
        long refunded = reportMapper.refundedOrders();
        BigDecimal refundRate = total == 0 ? BigDecimal.ZERO
                : BigDecimal.valueOf(refunded).multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
        List<CategoryStatDto> categories = reportMapper.categoryStats().stream()
                .map(this::categoryStat).toList();
        List<MonthlyStatDto> months = reportMapper.monthlyStats().stream()
                .map(this::monthlyStat).toList();
        BigDecimal amount = reportMapper.completedAmount();
        return new ReportDataDto(reportMapper.completedCount(),
                amount == null ? BigDecimal.ZERO : amount, refundRate, reportMapper.pendingAudits(),
                reportMapper.pendingArbitrations(), categories, months);
    }

    private CategoryStatDto categoryStat(Map<String, Object> row) {
        return new CategoryStatDto(str(row.get("name")), num(row.get("cnt")), decimal(row.get("amount")));
    }

    private MonthlyStatDto monthlyStat(Map<String, Object> row) {
        return new MonthlyStatDto(str(row.get("month")), num(row.get("cnt")), decimal(row.get("amount")));
    }

    private String str(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private Long num(Object value) {
        return value == null ? 0L : ((Number) value).longValue();
    }

    private BigDecimal decimal(Object value) {
        return value == null ? BigDecimal.ZERO : new BigDecimal(String.valueOf(value));
    }

    private ArbitrationDto toDto(Arbitration a) {
        AppUser applicant = a.getApplicantId() == null ? null : userMapper.selectById(a.getApplicantId());
        AppUser handler = a.getHandlerId() == null ? null : userMapper.selectById(a.getHandlerId());
        String targetName = null;
        String orderId = null;
        if ("order".equals(a.getObjectType()) && a.getObjectId() != null) {
            TradeOrder order = orderMapper.selectById(a.getObjectId());
            if (order != null) {
                orderId = order.getOrderNo();
                Long targetId = Objects.equals(order.getBuyerId(), a.getApplicantId())
                        ? order.getSellerId() : order.getBuyerId();
                AppUser target = userMapper.selectById(targetId);
                targetName = target == null ? null : target.getNickname();
            }
        }
        return new ArbitrationDto(a.getArbitrationId(), orderId,
                applicant == null ? null : applicant.getNickname(), targetName, a.getArbitrationType(),
                a.getEvidence(), a.getStatus(), a.getResult(), handler == null ? null : handler.getNickname(),
                a.getCreatedAt(), a.getResolvedAt());
    }
}
