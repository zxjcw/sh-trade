package com.shiguangji.modules.trade;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.shiguangji.common.config.AppProperties;
import com.shiguangji.common.dto.Dtos.OrderDto;
import com.shiguangji.common.entity.AppUser;
import com.shiguangji.common.entity.Bounty;
import com.shiguangji.common.entity.CreditRecord;
import com.shiguangji.common.entity.DeliveryRecord;
import com.shiguangji.common.entity.Item;
import com.shiguangji.common.entity.ItemHistory;
import com.shiguangji.common.entity.PickupPoint;
import com.shiguangji.common.entity.Review;
import com.shiguangji.common.entity.TradeOrder;
import com.shiguangji.common.exception.BizException;
import com.shiguangji.common.mapper.AppUserMapper;
import com.shiguangji.common.mapper.BountyMapper;
import com.shiguangji.common.mapper.CreditRecordMapper;
import com.shiguangji.common.mapper.DeliveryRecordMapper;
import com.shiguangji.common.mapper.ItemHistoryMapper;
import com.shiguangji.common.mapper.ItemMapper;
import com.shiguangji.common.mapper.PickupPointMapper;
import com.shiguangji.common.mapper.ReviewMapper;
import com.shiguangji.common.mapper.TradeOrderMapper;
import com.shiguangji.modules.notification.NotificationService;
import com.shiguangji.modules.payment.LedgerService;
import com.shiguangji.modules.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class OrderService {

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final TradeOrderMapper orderMapper;
    private final ItemMapper itemMapper;
    private final ItemHistoryMapper historyMapper;
    private final ReviewMapper reviewMapper;
    private final CreditRecordMapper creditRecordMapper;
    private final DeliveryRecordMapper deliveryMapper;
    private final PickupPointMapper pickupPointMapper;
    private final BountyMapper bountyMapper;
    private final AppUserMapper userMapper;
    private final OrderAssembler assembler;
    private final LedgerService ledgerService;
    private final NotificationService notificationService;
    private final UserService userService;
    private final AppProperties properties;

    public record CreateRequest(Long itemId, String deliveryMethod, Long pickupPointId, Long bountyId) {
    }

    public record ReviewRequest(Integer rating, List<String> tags, String content) {
    }

    public record AfterSaleRequest(String reason, String evidence) {
    }

    /** 约定面交：time 由前端传 "yyyy-MM-dd HH:mm" 之类的字符串，容错解析。 */
    public record MeetupRequest(String time, String place) {
    }

    @Transactional
    public OrderDto create(CreateRequest request) {
        AppUser me = userService.current();
        userService.requireVerified(me);
        if (request.itemId() == null) {
            throw BizException.badRequest("请选择要购买的物品");
        }
        Item item = itemMapper.selectById(request.itemId());
        if (item == null) {
            throw BizException.notFound("物品不存在");
        }
        if (Objects.equals(item.getCurrentHolderId(), me.getUserId())) {
            throw BizException.badRequest("不能购买自己的物品");
        }
        if (!"on_sale".equals(item.getStatus())) {
            throw BizException.conflict("该物品已被下单或已下架");
        }
        String method = request.deliveryMethod() == null || request.deliveryMethod().isBlank()
                ? "meetup" : request.deliveryMethod();
        if (!method.equals("meetup") && !method.equals("pickup")) {
            throw BizException.badRequest("交付方式不正确");
        }
        Long pointId = null;
        if ("pickup".equals(method)) {
            if (request.pickupPointId() == null) {
                throw BizException.badRequest("请选择自提点");
            }
            PickupPoint point = pickupPointMapper.selectById(request.pickupPointId());
            if (point == null) {
                throw BizException.badRequest("自提点不存在");
            }
            pointId = point.getPickupPointId();
        }
        int locked = itemMapper.update(null, new LambdaUpdateWrapper<Item>()
                .eq(Item::getItemId, item.getItemId())
                .eq(Item::getStatus, "on_sale")
                .set(Item::getStatus, "locked")
                .set(Item::getUpdatedAt, LocalDateTime.now()));
        if (locked == 0) {
            throw BizException.conflict("该物品已被下单或已下架");
        }
        LocalDateTime now = LocalDateTime.now();
        TradeOrder order = new TradeOrder();
        order.setOrderNo(nextOrderNo(now.toLocalDate()));
        order.setOrderType("normal");
        order.setBuyerId(me.getUserId());
        order.setSellerId(item.getCurrentHolderId());
        order.setItemId(item.getItemId());
        order.setAmount(item.getPrice());
        order.setDeliveryMethod(method);
        order.setPickupPointId(pointId);
        order.setTradeStatus("pending_payment");
        order.setAfterSaleStatus("none");
        order.setSourceBountyId(request.bountyId());
        order.setPayDeadline(now.plus(properties.timeout().orderPay()));
        order.setCreatedAt(now);
        order.setUpdatedAt(now);
        orderMapper.insert(order);

        notificationService.send(order.getSellerId(), "order", "order", order.getOrderNo(),
                me.getNickname() + " 下单了你的「" + item.getTitle() + "」，请等待对方支付。", order.getOrderNo());
        return assembler.toDto(order, me.getUserId());
    }

    @Transactional
    public OrderDto pay(String orderNo) {
        AppUser me = userService.current();
        TradeOrder order = require(orderNo);
        if (!Objects.equals(order.getBuyerId(), me.getUserId())) {
            throw BizException.forbidden("只有买家可以支付");
        }
        if (!"pending_payment".equals(order.getTradeStatus())) {
            if (hasPaid(orderNo)) {
                return assembler.toDto(order, me.getUserId());
            }
            throw BizException.badRequest("当前订单状态不可支付");
        }
        if (order.getPayDeadline() != null && LocalDateTime.now().isAfter(order.getPayDeadline())) {
            cancelExpired(order);
            throw BizException.badRequest("支付超时，订单已自动取消");
        }
        ledgerService.buyerPay(orderNo, me.getUserId(), order.getAmount());
        order.setPaidAt(LocalDateTime.now());
        order.setTradeStatus("pending_delivery");
        order.setUpdatedAt(LocalDateTime.now());
        orderMapper.updateById(order);
        Item item = itemMapper.selectById(order.getItemId());
        notificationService.send(order.getSellerId(), "order", "order", orderNo,
                "买家已付款，请尽快与买家约定交付（「" + (item == null ? "物品" : item.getTitle()) + "」）。", orderNo);
        return assembler.toDto(order, me.getUserId());
    }

    @Transactional
    public OrderDto cancel(String orderNo) {
        AppUser me = userService.current();
        TradeOrder order = require(orderNo);
        if (!Objects.equals(order.getBuyerId(), me.getUserId())) {
            throw BizException.forbidden("只有买家可以取消订单");
        }
        if (!"pending_payment".equals(order.getTradeStatus())) {
            throw BizException.badRequest("只有待支付订单可以取消");
        }
        cancelExpired(order);
        return assembler.toDto(orderMapper.selectById(orderNo), me.getUserId());
    }

    @Transactional
    public OrderDto confirm(String orderNo) {
        AppUser me = userService.current();
        TradeOrder order = require(orderNo);
        if (!Objects.equals(order.getBuyerId(), me.getUserId())) {
            throw BizException.forbidden("只有买家可以确认收货");
        }
        if ("completed".equals(order.getTradeStatus())) {
            return assembler.toDto(order, me.getUserId());
        }
        if (!"pending_confirm".equals(order.getTradeStatus())) {
            throw BizException.badRequest("当前订单状态不可确认收货");
        }
        complete(order);
        return assembler.toDto(order, me.getUserId());
    }

    /** 完成交易：放款、物品归属转移、履历、信用、悬赏、通知。幂等。 */
    @Transactional
    public void complete(TradeOrder order) {
        if ("completed".equals(order.getTradeStatus())) {
            return;
        }
        String orderNo = order.getOrderNo();
        Item item = itemMapper.selectById(order.getItemId());
        ledgerService.sellerPayout(orderNo, order.getSellerId(), order.getAmount());

        order.setTradeStatus("completed");
        order.setCompletedAt(LocalDateTime.now());
        order.setConfirmDeadline(null);
        order.setUpdatedAt(LocalDateTime.now());
        orderMapper.updateById(order);

        if (item != null) {
            item.setStatus("completed");
            item.setCurrentHolderId(order.getBuyerId());
            item.setUpdatedAt(LocalDateTime.now());
            itemMapper.updateById(item);
            ItemHistory history = new ItemHistory();
            history.setItemId(item.getItemId());
            history.setEventType("deal");
            history.setEventTime(LocalDateTime.now());
            history.setDescription("成交：由 " + nickname(order.getSellerId()) + " 交付给 "
                    + nickname(order.getBuyerId()));
            history.setRelatedUserId(order.getBuyerId());
            history.setOrderNo(orderNo);
            historyMapper.insert(history);
        }
        addCredit(order.getBuyerId(), 2, "order_complete", orderNo, "交易完成");
        addCredit(order.getSellerId(), 2, "order_complete", orderNo, "交易完成");

        if (order.getSourceBountyId() != null) {
            Bounty bounty = bountyMapper.selectById(order.getSourceBountyId());
            if (bounty != null && "open".equals(bounty.getStatus())) {
                bounty.setStatus("matched");
                bounty.setMatchCount((bounty.getMatchCount() == null ? 0 : bounty.getMatchCount()) + 1);
                bountyMapper.updateById(bounty);
            }
        }
        notificationService.send(order.getSellerId(), "order", "order", orderNo, "交易完成，货款已到账。", orderNo);
        notificationService.send(order.getBuyerId(), "order", "order", orderNo, "交易完成，记得给卖家评价。", orderNo);
    }

    @Transactional
    public OrderDto review(String orderNo, ReviewRequest request) {
        AppUser me = userService.current();
        TradeOrder order = require(orderNo);
        boolean isBuyer = Objects.equals(order.getBuyerId(), me.getUserId());
        boolean isSeller = Objects.equals(order.getSellerId(), me.getUserId());
        if (!isBuyer && !isSeller) {
            throw BizException.forbidden("只有交易双方可以评价");
        }
        if (!"completed".equals(order.getTradeStatus())) {
            throw BizException.badRequest("交易完成后才能评价");
        }
        Integer rating = request.rating();
        if (rating == null || rating < 1 || rating > 5) {
            throw BizException.badRequest("请给出 1~5 星评价");
        }
        Long exists = reviewMapper.selectCount(new LambdaQueryWrapper<Review>()
                .eq(Review::getOrderNo, orderNo)
                .eq(Review::getReviewerId, me.getUserId())
                .ne(Review::getStatus, "void"));
        if (exists != null && exists > 0) {
            throw BizException.conflict("你已评价过该订单");
        }
        Review review = new Review();
        review.setOrderNo(orderNo);
        review.setReviewerId(me.getUserId());
        review.setTargetUserId(isBuyer ? order.getSellerId() : order.getBuyerId());
        review.setRating(rating);
        review.setTags(toJsonArray(request.tags()));
        review.setContent(request.content());
        review.setStatus("valid");
        review.setCreatedAt(LocalDateTime.now());
        reviewMapper.insert(review);
        notificationService.send(review.getTargetUserId(), "system", "order", orderNo,
                "你收到一条 " + rating + " 星评价。", orderNo);
        return assembler.toDto(order, me.getUserId());
    }

    @Transactional
    public OrderDto aftersale(String orderNo, AfterSaleRequest request) {
        AppUser me = userService.current();
        TradeOrder order = require(orderNo);
        if (!Objects.equals(order.getBuyerId(), me.getUserId())) {
            throw BizException.forbidden("只有买家可以申请售后");
        }
        String status = order.getTradeStatus();
        if ("cancelled".equals(status) || "refunded".equals(order.getAfterSaleStatus())) {
            throw BizException.badRequest("当前订单不可申请售后");
        }
        if ("completed".equals(status)) {
            LocalDateTime completedAt = order.getCompletedAt();
            if (completedAt == null || LocalDateTime.now().isAfter(completedAt.plus(properties.timeout().aftersaleClaim()))) {
                throw BizException.badRequest("已超过售后申请时限");
            }
        }
        String reason = request == null || request.reason() == null || request.reason().isBlank()
                ? "买家申请售后" : request.reason().trim();
        if (request != null && request.evidence() != null && !request.evidence().isBlank()) {
            reason = reason + "（凭证：" + request.evidence().trim() + "）";
        }
        if ("pending_delivery".equals(status)) {
            ledgerService.refundToBuyer(orderNo, order.getBuyerId(), order.getAmount());
            order.setTradeStatus("cancelled");
            order.setAfterSaleStatus("refunded");
            order.setCancelledAt(LocalDateTime.now());
            order.setUpdatedAt(LocalDateTime.now());
            orderMapper.updateById(order);
            reopenItem(order.getItemId());
            notificationService.send(order.getSellerId(), "aftersale", "order", orderNo,
                    "买家在交付前申请退款，已原路退回（原因：" + reason + "）。", orderNo);
            return assembler.toDto(order, me.getUserId());
        }
        order.setAfterSaleStatus("refunding");
        order.setUpdatedAt(LocalDateTime.now());
        orderMapper.updateById(order);
        notificationService.send(order.getSellerId(), "aftersale", "order", orderNo,
                "买家申请售后：" + reason + "，请在 2 分钟内处理。", orderNo);
        return assembler.toDto(order, me.getUserId());
    }

    public List<OrderDto> list(Long userId, String role, String status) {
        LambdaQueryWrapper<TradeOrder> wrapper = new LambdaQueryWrapper<>();
        String scope = role == null ? "all" : role;
        switch (scope) {
            case "buyer" -> wrapper.eq(TradeOrder::getBuyerId, userId);
            case "seller" -> wrapper.eq(TradeOrder::getSellerId, userId);
            default -> wrapper.and(w -> w.eq(TradeOrder::getBuyerId, userId)
                    .or().eq(TradeOrder::getSellerId, userId));
        }
        if (status != null && !status.isBlank() && !"all".equals(status)) {
            wrapper.eq(TradeOrder::getTradeStatus, status);
        }
        wrapper.orderByDesc(TradeOrder::getCreatedAt).orderByDesc(TradeOrder::getOrderNo);
        return orderMapper.selectList(wrapper).stream()
                .map(order -> assembler.toDto(order, userId)).toList();
    }

    public OrderDto detail(String orderNo, Long userId, boolean ops) {
        TradeOrder order = require(orderNo);
        boolean participant = Objects.equals(order.getBuyerId(), userId)
                || Objects.equals(order.getSellerId(), userId);
        if (!participant && !ops) {
            throw BizException.notFound("订单不存在");
        }
        return assembler.toDto(order, userId);
    }

    @Transactional
    public OrderDto meetup(String orderNo, MeetupRequest request) {
        AppUser me = userService.current();
        TradeOrder order = require(orderNo);
        if (!Objects.equals(order.getSellerId(), me.getUserId())) {
            throw BizException.forbidden("只有卖家可以约定交付");
        }
        if (!"pending_delivery".equals(order.getTradeStatus())) {
            throw BizException.badRequest("当前订单状态不可约定交付");
        }
        DeliveryRecord record = deliveryOf(orderNo);
        record.setMethod("meetup");
        record.setAgreedTime(request == null ? null : parseTime(request.time()));
        record.setMeetupPlace(request == null ? null : request.place());
        if (record.getDeliveryId() == null) {
            deliveryMapper.insert(record);
        } else {
            deliveryMapper.updateById(record);
        }
        notificationService.send(order.getBuyerId(), "order", "order", orderNo,
                "卖家已约定交付时间与地点，请按时赴约并确认。", orderNo);
        return assembler.toDto(order, me.getUserId());
    }

    @Transactional
    public OrderDto verify(String orderNo) {
        AppUser me = userService.current();
        TradeOrder order = require(orderNo);
        if (!Objects.equals(order.getBuyerId(), me.getUserId())
                && !Objects.equals(order.getSellerId(), me.getUserId())) {
            throw BizException.forbidden("只有交易双方可以确认交付");
        }
        if (!"pending_delivery".equals(order.getTradeStatus())) {
            throw BizException.badRequest("当前订单状态不可确认交付");
        }
        DeliveryRecord record = deliveryOf(orderNo);
        record.setVerifyTime(LocalDateTime.now());
        record.setMethod(record.getMethod() == null ? order.getDeliveryMethod() : record.getMethod());
        if (record.getDeliveryId() == null) {
            deliveryMapper.insert(record);
        } else {
            deliveryMapper.updateById(record);
        }
        moveToPendingConfirm(order);
        return assembler.toDto(orderMapper.selectById(orderNo), me.getUserId());
    }

    @Transactional
    public OrderDto inbound(String orderNo) {
        AppUser me = userService.current();
        TradeOrder order = require(orderNo);
        if (!Objects.equals(order.getSellerId(), me.getUserId())) {
            throw BizException.forbidden("只有卖家可以将物品送达自提点");
        }
        if (!"pickup".equals(order.getDeliveryMethod())) {
            throw BizException.badRequest("该订单不是自提方式");
        }
        if (!"pending_delivery".equals(order.getTradeStatus())) {
            throw BizException.badRequest("当前订单状态不可入库");
        }
        DeliveryRecord record = deliveryOf(orderNo);
        record.setMethod("pickup");
        record.setPickupPointId(order.getPickupPointId());
        record.setInboundTime(LocalDateTime.now());
        record.setPickupCode(randomCode());
        if (record.getDeliveryId() == null) {
            deliveryMapper.insert(record);
        } else {
            deliveryMapper.updateById(record);
        }
        notificationService.send(order.getBuyerId(), "order", "order", orderNo,
                "物品已到达自提点，取件码 " + record.getPickupCode() + "，请凭码取件。", orderNo);
        return assembler.toDto(order, me.getUserId());
    }

    @Transactional
    public OrderDto outbound(String orderNo) {
        AppUser me = userService.current();
        TradeOrder order = require(orderNo);
        if (!Objects.equals(order.getBuyerId(), me.getUserId())) {
            throw BizException.forbidden("只有买家可以确认取件");
        }
        if (!"pending_delivery".equals(order.getTradeStatus())) {
            throw BizException.badRequest("当前订单状态不可确认取件");
        }
        DeliveryRecord record = deliveryOf(orderNo);
        if (record.getDeliveryId() == null || record.getInboundTime() == null) {
            throw BizException.badRequest("物品尚未到达自提点");
        }
        record.setOutboundTime(LocalDateTime.now());
        deliveryMapper.updateById(record);
        moveToPendingConfirm(order);
        return assembler.toDto(orderMapper.selectById(orderNo), me.getUserId());
    }

    private void moveToPendingConfirm(TradeOrder order) {
        order.setTradeStatus("pending_confirm");
        order.setConfirmDeadline(LocalDateTime.now().plus(properties.timeout().autoConfirm()));
        order.setUpdatedAt(LocalDateTime.now());
        orderMapper.updateById(order);
        Item item = itemMapper.selectById(order.getItemId());
        notificationService.send(order.getBuyerId(), "order", "order", order.getOrderNo(),
                "交付已确认，请在 " + readable(properties.timeout().autoConfirm())
                        + "内确认收货（「" + (item == null ? "物品" : item.getTitle()) + "」）。", order.getOrderNo());
    }

    private String readable(java.time.Duration duration) {
        long minutes = duration.toMinutes();
        if (minutes >= 1) {
            return minutes + " 分钟";
        }
        return Math.max(1, duration.toSeconds()) + " 秒";
    }

    public void cancelExpired(TradeOrder order) {
        order.setTradeStatus("cancelled");
        order.setCancelledAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());
        orderMapper.updateById(order);
        reopenItem(order.getItemId());
    }

    /** 定时任务：卖家超时未处理售后，自动退款给买家。 */
    @Transactional
    public void autoRefundAfterSale(TradeOrder order) {
        String orderNo = order.getOrderNo();
        boolean paidOut = "completed".equals(order.getTradeStatus());
        if (paidOut) {
            try {
                ledgerService.chargebackSeller(orderNo, order.getSellerId(), order.getAmount());
            } catch (BizException ignored) {
                // 卖家余额不足时不扣回，差额由平台承担
            }
        }
        ledgerService.refundToBuyer(orderNo, order.getBuyerId(), order.getAmount());
        order.setAfterSaleStatus("refunded");
        order.setUpdatedAt(LocalDateTime.now());
        if (!paidOut) {
            order.setTradeStatus("cancelled");
            order.setCancelledAt(LocalDateTime.now());
        }
        orderMapper.updateById(order);
        Item item = itemMapper.selectById(order.getItemId());
        if (item != null && !"exchange".equals(order.getOrderType())) {
            if (paidOut) {
                item.setCurrentHolderId(order.getSellerId());
            }
            item.setStatus("on_sale");
            item.setUpdatedAt(LocalDateTime.now());
            itemMapper.updateById(item);
        }
        notificationService.send(order.getBuyerId(), "aftersale", "order", orderNo,
                "卖家超时未处理售后，已自动退款到你的钱包。", orderNo);
        notificationService.send(order.getSellerId(), "aftersale", "order", orderNo,
                "你超时未处理售后，系统已自动退款给买家。", orderNo);
    }

    public void reopenItem(Long itemId) {        itemMapper.update(null, new LambdaUpdateWrapper<Item>()
                .eq(Item::getItemId, itemId)
                .eq(Item::getStatus, "locked")
                .set(Item::getStatus, "on_sale")
                .set(Item::getUpdatedAt, LocalDateTime.now()));
    }

    public TradeOrder require(String orderNo) {
        TradeOrder order = orderMapper.selectById(orderNo);
        if (order == null) {
            throw BizException.notFound("订单不存在");
        }
        return order;
    }

    private DeliveryRecord deliveryOf(String orderNo) {
        DeliveryRecord record = deliveryMapper.selectOne(new LambdaQueryWrapper<DeliveryRecord>()
                .eq(DeliveryRecord::getOrderNo, orderNo));
        if (record == null) {
            record = new DeliveryRecord();
            record.setOrderNo(orderNo);
        }
        return record;
    }

    private boolean hasPaid(String orderNo) {
        return orderMapper.selectById(orderNo) != null
                && orderMapper.selectById(orderNo).getPaidAt() != null;
    }

    private void addCredit(Long userId, int delta, String reason, String orderNo, String remark) {
        Long exists = creditRecordMapper.selectCount(new LambdaQueryWrapper<CreditRecord>()
                .eq(CreditRecord::getUserId, userId)
                .eq(CreditRecord::getOrderNo, orderNo)
                .eq(CreditRecord::getReason, reason));
        if (exists != null && exists > 0) {
            return;
        }
        userMapper.addCredit(userId, delta);
        CreditRecord record = new CreditRecord();
        record.setUserId(userId);
        record.setChangeValue(delta);
        record.setReason(reason);
        record.setOrderNo(orderNo);
        record.setCreatedAt(LocalDateTime.now());
        creditRecordMapper.insert(record);
    }

    private String nickname(Long userId) {
        AppUser user = userMapper.selectById(userId);
        return user == null ? "用户" : user.getNickname();
    }

    private String nextOrderNo(LocalDate date) {
        String prefix = "SO" + date.format(DAY);
        int seq = orderMapper.maxSequence(prefix) + 1;
        String candidate = prefix + String.format("%03d", seq);
        while (orderMapper.selectById(candidate) != null && seq < 999) {
            seq++;
            candidate = prefix + String.format("%03d", seq);
        }
        return candidate;
    }

    private String randomCode() {
        return String.format("%06d", new SecureRandom().nextInt(1_000_000));
    }

    /** 容错解析前端传来的约定时间：支持 ISO、"yyyy-MM-dd HH:mm"、"yyyy-MM-dd HH:mm:ss"。 */
    private static LocalDateTime parseTime(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String text = raw.trim().replace('T', ' ');
        if (text.length() == 16) {
            text = text + ":00";
        }
        try {
            return LocalDateTime.parse(text, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private String toJsonArray(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return null;
        }
        return tags.stream().filter(Objects::nonNull).map(t -> "\"" + t.replace("\"", "") + "\"")
                .reduce((a, b) -> a + "," + b).map(s -> "[" + s + "]").orElse(null);
    }
}
