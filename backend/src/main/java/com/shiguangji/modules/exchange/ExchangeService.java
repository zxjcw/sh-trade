package com.shiguangji.modules.exchange;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shiguangji.common.config.AppProperties;
import com.shiguangji.common.dto.Dtos.ExchangeIntentDto;
import com.shiguangji.common.dto.Dtos.ExchangeParticipantDto;
import com.shiguangji.common.dto.Dtos.ExchangePlanDto;
import com.shiguangji.common.entity.AppUser;
import com.shiguangji.common.entity.ExchangeIntent;
import com.shiguangji.common.entity.ExchangePlan;
import com.shiguangji.common.entity.Item;
import com.shiguangji.common.entity.ItemImage;
import com.shiguangji.common.entity.TradeOrder;
import com.shiguangji.common.exception.BizException;
import com.shiguangji.common.mapper.AppUserMapper;
import com.shiguangji.common.mapper.ExchangeIntentMapper;
import com.shiguangji.common.mapper.ExchangePlanMapper;
import com.shiguangji.common.mapper.ItemImageMapper;
import com.shiguangji.common.mapper.ItemMapper;
import com.shiguangji.common.mapper.TradeOrderMapper;
import com.shiguangji.modules.item.ItemAssembler;
import com.shiguangji.modules.notification.NotificationService;
import com.shiguangji.modules.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ExchangeService {

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final ExchangeIntentMapper intentMapper;
    private final ExchangePlanMapper planMapper;
    private final ItemMapper itemMapper;
    private final ItemImageMapper imageMapper;
    private final TradeOrderMapper orderMapper;
    private final AppUserMapper userMapper;
    private final ItemAssembler assembler;
    private final UserService userService;
    private final NotificationService notificationService;
    private final AppProperties properties;

    public record CreateRequest(Long myItemId, String matchType, Long targetItemId,
                                Long targetCategoryId, String keyword) {
    }

    public List<ExchangeIntentDto> intents(Long userId) {
        return intentMapper.selectList(new LambdaQueryWrapper<ExchangeIntent>()
                        .eq(ExchangeIntent::getUserId, userId)
                        .orderByDesc(ExchangeIntent::getCreatedAt)
                        .orderByDesc(ExchangeIntent::getIntentId))
                .stream().map(this::toDto).toList();
    }

    @Transactional
    public ExchangeIntentDto create(CreateRequest request) {
        AppUser me = userService.current();
        userService.requireVerified(me);
        if (request.myItemId() == null) {
            throw BizException.badRequest("请选择用于置换的物品");
        }
        Item myItem = itemMapper.selectById(request.myItemId());
        if (myItem == null || !Objects.equals(myItem.getCurrentHolderId(), me.getUserId())) {
            throw BizException.notFound("物品不存在");
        }
        if (!"on_sale".equals(myItem.getStatus())) {
            throw BizException.badRequest("只有上架中的物品可以发起置换");
        }
        String matchType = "specific".equals(request.matchType()) ? "specific" : "condition";
        Item targetItem = null;
        if ("specific".equals(matchType)) {
            if (request.targetItemId() == null) {
                throw BizException.badRequest("请选择想置换的目标物品");
            }
            targetItem = itemMapper.selectById(request.targetItemId());
            if (targetItem == null || !"on_sale".equals(targetItem.getStatus())) {
                throw BizException.badRequest("目标物品不存在或不可置换");
            }
            if (Objects.equals(targetItem.getCurrentHolderId(), me.getUserId())) {
                throw BizException.badRequest("不能与自己置换");
            }
        } else if (request.targetCategoryId() == null && (request.keyword() == null || request.keyword().isBlank())) {
            throw BizException.badRequest("请填写期望的分类或关键词");
        }
        Long active = intentMapper.selectCount(new LambdaQueryWrapper<ExchangeIntent>()
                .eq(ExchangeIntent::getUserId, me.getUserId())
                .eq(ExchangeIntent::getMyItemId, myItem.getItemId())
                .eq(ExchangeIntent::getStatus, "active"));
        if (active != null && active > 0) {
            throw BizException.conflict("该物品已有进行中的置换意向");
        }
        ExchangeIntent intent = new ExchangeIntent();
        intent.setUserId(me.getUserId());
        intent.setMyItemId(myItem.getItemId());
        intent.setMatchType(matchType);
        intent.setTargetItemId(targetItem == null ? null : targetItem.getItemId());
        intent.setTargetCategoryId(request.targetCategoryId());
        intent.setKeyword(request.keyword());
        intent.setStatus("active");
        intent.setCreatedAt(LocalDateTime.now());
        intentMapper.insert(intent);
        match(intent);
        return toDto(intent);
    }

    @Transactional
    public List<ExchangePlanDto> plans(Long userId) {
        List<ExchangeIntent> mine = intentMapper.selectList(new LambdaQueryWrapper<ExchangeIntent>()
                .eq(ExchangeIntent::getUserId, userId));
        List<Long> intentIds = mine.stream().map(ExchangeIntent::getIntentId).toList();
        List<ExchangePlan> plans = planMapper.selectList(new LambdaQueryWrapper<ExchangePlan>()
                .and(w -> {
                    if (!intentIds.isEmpty()) {
                        w.in(ExchangePlan::getIntentAId, intentIds)
                                .or().in(ExchangePlan::getIntentBId, intentIds)
                                .or().in(ExchangePlan::getIntentCId, intentIds)
                                .or().eq(ExchangePlan::getRingType, 2);
                    } else {
                        w.eq(ExchangePlan::getRingType, 2);
                    }
                })
                .orderByDesc(ExchangePlan::getCreatedAt));
        return plans.stream()
                .map(plan -> toDto(plan, userId))
                .filter(dto -> dto.participants().stream()
                        .anyMatch(p -> Objects.equals(p.userId(), userId)))
                .toList();
    }

    @Transactional
    public ExchangePlanDto confirm(Long planId) {
        AppUser me = userService.current();
        ExchangePlan plan = require(planId);
        if (!"pending".equals(plan.getStatus())) {
            throw BizException.badRequest("方案当前状态不可确认");
        }
        List<ExchangeIntent> intents = intentsOf(plan);
        int index = indexOfUser(intents, me.getUserId());
        if (index < 0) {
            throw BizException.forbidden("你不在该置换方案中");
        }
        LocalDateTime now = LocalDateTime.now();
        if (plan.getConfirmDeadline() != null && now.isAfter(plan.getConfirmDeadline())) {
            voidPlan(plan, intents, "置换确认超时，方案已失效");
            throw BizException.badRequest("确认已超时，方案已失效");
        }
        switch (index) {
            case 0 -> plan.setConfirmedAAt(now);
            case 1 -> plan.setConfirmedBAt(now);
            default -> plan.setConfirmedCAt(now);
        }
        planMapper.updateById(plan);

        if (allConfirmed(plan)) {
            activate(plan, intents);
        } else {
            for (ExchangeIntent intent : intents) {
                if (!Objects.equals(intent.getUserId(), me.getUserId())) {
                    notificationService.send(intent.getUserId(), "exchange", "plan", plan.getPlanId(),
                            me.getNickname() + " 已确认置换方案，等待你确认。", null);
                }
            }
        }
        return toDto(planMapper.selectById(planId), me.getUserId());
    }

    @Transactional
    public ExchangePlanDto reject(Long planId) {
        AppUser me = userService.current();
        ExchangePlan plan = require(planId);
        if (!"pending".equals(plan.getStatus())) {
            throw BizException.badRequest("方案当前状态不可拒绝");
        }
        List<ExchangeIntent> intents = intentsOf(plan);
        if (indexOfUser(intents, me.getUserId()) < 0) {
            throw BizException.forbidden("你不在该置换方案中");
        }
        voidPlan(plan, intents, me.getNickname() + " 拒绝了置换方案");
        return toDto(planMapper.selectById(planId), me.getUserId());
    }

    /** 定时任务调用：确认超时的方案作废。 */
    @Transactional
    public void expirePlan(Long planId) {
        ExchangePlan plan = planMapper.selectById(planId);
        if (plan == null || !"pending".equals(plan.getStatus())) {
            return;
        }
        voidPlan(plan, intentsOf(plan), "置换确认超时，方案已失效");
    }

    private void voidPlan(ExchangePlan plan, List<ExchangeIntent> intents, String message) {        plan.setStatus("void");
        planMapper.updateById(plan);
        for (ExchangeIntent intent : intents) {
            intent.setStatus("active");
            intentMapper.updateById(intent);
            notificationService.send(intent.getUserId(), "exchange", "plan", plan.getPlanId(), message, null);
        }
    }

    private void activate(ExchangePlan plan, List<ExchangeIntent> intents) {
        plan.setStatus("active");
        planMapper.updateById(plan);
        List<ExchangeIntent> ring = new ArrayList<>(intents);
        String prefix = "SO" + LocalDate.now().format(DAY);
        int base = orderMapper.maxSequence(prefix) + 1;
        for (int i = 0; i < ring.size(); i++) {
            ExchangeIntent giver = ring.get(i);
            ExchangeIntent receiver = ring.get((i + 1) % ring.size());
            Item item = itemMapper.selectById(giver.getMyItemId());
            if (item == null) {
                continue;
            }
            createExchangeOrder(item, giver.getUserId(), receiver.getUserId(), plan.getPlanId(), prefix, base + i);
            item.setStatus("locked");
            item.setUpdatedAt(LocalDateTime.now());
            itemMapper.updateById(item);
            giver.setStatus("matched");
            intentMapper.updateById(giver);
            notificationService.send(giver.getUserId(), "exchange", "plan", plan.getPlanId(),
                    "置换方案已生效，请按约定完成交付。", null);
        }
    }

    private void createExchangeOrder(Item item, Long sellerId, Long buyerId, Long planId,
                                     String prefix, int seq) {
        LocalDateTime now = LocalDateTime.now();
        String orderNo = prefix + String.format("%03d", seq);
        while (orderMapper.selectById(orderNo) != null && seq < 999) {
            seq++;
            orderNo = prefix + String.format("%03d", seq);
        }
        TradeOrder order = new TradeOrder();
        order.setOrderNo(orderNo);
        order.setOrderType("exchange");
        order.setBuyerId(buyerId);
        order.setSellerId(sellerId);
        order.setItemId(item.getItemId());
        order.setAmount(java.math.BigDecimal.ZERO);
        order.setDeliveryMethod("meetup");
        order.setTradeStatus("pending_delivery");
        order.setAfterSaleStatus("none");
        order.setPlanId(planId);
        order.setPaidAt(now);
        order.setCreatedAt(now);
        order.setUpdatedAt(now);
        orderMapper.insert(order);
    }

    private boolean allConfirmed(ExchangePlan plan) {
        return plan.getConfirmedAAt() != null && plan.getConfirmedBAt() != null
                && (plan.getRingType() == null || plan.getRingType() == 2 || plan.getConfirmedCAt() != null);
    }

    private ExchangePlan require(Long planId) {
        ExchangePlan plan = planMapper.selectById(planId);
        if (plan == null) {
            throw BizException.notFound("置换方案不存在");
        }
        return plan;
    }

    /**
     * 方案参与意向。表约束 ck_plan_participants 规定 2 人环只落 intent_a_id（B/C 必须为空），
     * 因此 2 人环的第二方意向按 A 的物品与目标物品实时反查。
     */
    private List<ExchangeIntent> intentsOf(ExchangePlan plan) {
        List<ExchangeIntent> list = new ArrayList<>();
        ExchangeIntent a = plan.getIntentAId() == null ? null : intentMapper.selectById(plan.getIntentAId());
        if (a == null) {
            return List.of();
        }
        list.add(a);
        if (plan.getRingType() != null && plan.getRingType() == 2 && plan.getIntentBId() == null) {
            ExchangeIntent partner = partnerOf(a);
            if (partner != null) {
                list.add(partner);
            }
            return list;
        }
        if (plan.getIntentBId() != null) {
            list.add(intentMapper.selectById(plan.getIntentBId()));
        }
        if (plan.getIntentCId() != null) {
            list.add(intentMapper.selectById(plan.getIntentCId()));
        }
        return list.stream().filter(Objects::nonNull).toList();
    }

    /** 反查与 A 成 2 人环的另一方意向。 */
    private ExchangeIntent partnerOf(ExchangeIntent a) {
        Item myItem = itemMapper.selectById(a.getMyItemId());
        if (myItem == null) {
            return null;
        }
        List<ExchangeIntent> candidates = intentMapper.selectList(new LambdaQueryWrapper<ExchangeIntent>()
                .in(ExchangeIntent::getStatus, "active", "matched")
                .ne(ExchangeIntent::getUserId, a.getUserId()));
        for (ExchangeIntent candidate : candidates) {
            Item other = itemMapper.selectById(candidate.getMyItemId());
            if (other == null) {
                continue;
            }
            if (matches(candidate, myItem) && matches(a, other)) {
                return candidate;
            }
        }
        return null;
    }

    private int indexOfUser(List<ExchangeIntent> intents, Long userId) {
        for (int i = 0; i < intents.size(); i++) {
            if (Objects.equals(intents.get(i).getUserId(), userId)) {
                return i;
            }
        }
        return -1;
    }

    /** 简单撮合：优先 2 人成环，其次 3 人成环。 */
    private void match(ExchangeIntent mine) {
        Item myItem = itemMapper.selectById(mine.getMyItemId());
        if (myItem == null) {
            return;
        }
        List<ExchangeIntent> candidates = intentMapper.selectList(new LambdaQueryWrapper<ExchangeIntent>()
                .eq(ExchangeIntent::getStatus, "active")
                .ne(ExchangeIntent::getUserId, mine.getUserId()));
        List<ExchangeIntent> wantsMine = candidates.stream()
                .filter(candidate -> matches(candidate, myItem)).toList();
        for (ExchangeIntent b : wantsMine) {
            Item bItem = itemMapper.selectById(b.getMyItemId());
            if (bItem == null) {
                continue;
            }
            if (matches(mine, bItem)) {
                createPlan(2, mine, b, null);
                return;
            }
        }
        for (ExchangeIntent b : wantsMine) {
            Item bItem = itemMapper.selectById(b.getMyItemId());
            if (bItem == null) {
                continue;
            }
            for (ExchangeIntent c : candidates) {
                if (Objects.equals(c.getUserId(), b.getUserId())
                        || Objects.equals(c.getUserId(), mine.getUserId())) {
                    continue;
                }
                Item cItem = itemMapper.selectById(c.getMyItemId());
                if (cItem == null) {
                    continue;
                }
                if (matches(c, bItem) && matches(mine, cItem)) {
                    createPlan(3, mine, b, c);
                    return;
                }
            }
        }
    }

    private boolean matches(ExchangeIntent intent, Item item) {
        if (intent == null || item == null || !"on_sale".equals(item.getStatus())) {
            return false;
        }
        if (Objects.equals(intent.getUserId(), item.getCurrentHolderId())) {
            return false;
        }
        if ("specific".equals(intent.getMatchType())) {
            return Objects.equals(intent.getTargetItemId(), item.getItemId());
        }
        boolean categoryOk = intent.getTargetCategoryId() == null
                || Objects.equals(intent.getTargetCategoryId(), item.getCategoryId());
        boolean keywordOk = intent.getKeyword() == null || intent.getKeyword().isBlank()
                || (item.getTitle() != null && item.getTitle().contains(intent.getKeyword()));
        return categoryOk && keywordOk;
    }

    private void createPlan(int ringType, ExchangeIntent a, ExchangeIntent b, ExchangeIntent c) {
        ExchangePlan plan = new ExchangePlan();
        plan.setRingType(ringType);
        plan.setIntentAId(a.getIntentId());
        // 2 人环受 ck_plan_participants 约束：B/C 必须为空，第二方按 A 反查
        plan.setIntentBId(ringType == 2 ? null : b.getIntentId());
        plan.setIntentCId(c == null ? null : c.getIntentId());
        plan.setStatus("pending");
        plan.setConfirmDeadline(LocalDateTime.now().plus(properties.timeout().exchangeConfirm()));
        plan.setCreatedAt(LocalDateTime.now());
        planMapper.insert(plan);
        for (ExchangeIntent intent : new ExchangeIntent[]{a, b, c}) {
            if (intent == null) {
                continue;
            }
            intent.setStatus("matched");
            intentMapper.updateById(intent);
            notificationService.send(intent.getUserId(), "exchange", "plan", plan.getPlanId(),
                    "为你找到一个 " + ringType + " 人置换方案，请在 10 分钟内确认。", null);
        }
    }

    private ExchangeIntentDto toDto(ExchangeIntent intent) {
        Item item = itemMapper.selectById(intent.getMyItemId());
        AppUser user = userMapper.selectById(intent.getUserId());
        return new ExchangeIntentDto(intent.getIntentId(), intent.getUserId(),
                user == null ? null : user.getNickname(), intent.getMyItemId(),
                item == null ? null : item.getTitle(), item == null ? null : coverSeed(item.getItemId()),
                intent.getMatchType(), intent.getTargetItemId(), intent.getTargetItemId() == null ? null
                : titleOf(intent.getTargetItemId()), intent.getTargetCategoryId(),
                assembler.categoryName(intent.getTargetCategoryId()), intent.getKeyword(), intent.getStatus(),
                intent.getCreatedAt());
    }

    private ExchangePlanDto toDto(ExchangePlan plan, Long viewerId) {
        List<ExchangeParticipantDto> participants = new ArrayList<>();
        List<ExchangeIntent> intents = intentsOf(plan);
        for (int i = 0; i < intents.size(); i++) {
            ExchangeIntent intent = intents.get(i);
            Item item = itemMapper.selectById(intent.getMyItemId());
            AppUser user = userMapper.selectById(intent.getUserId());
            Boolean confirmed = switch (i) {
                case 0 -> plan.getConfirmedAAt() != null;
                case 1 -> plan.getConfirmedBAt() != null;
                default -> plan.getConfirmedCAt() != null;
            };
            participants.add(new ExchangeParticipantDto(intent.getUserId(),
                    user == null ? null : user.getNickname(), item == null ? null : item.getTitle(),
                    item == null ? null : coverSeed(item.getItemId()), confirmed));
        }
        return new ExchangePlanDto(plan.getPlanId(), plan.getRingType(), participants, plan.getStatus(),
                plan.getConfirmDeadline(), plan.getCreatedAt());
    }

    private String titleOf(Long itemId) {
        Item item = itemMapper.selectById(itemId);
        return item == null ? null : item.getTitle();
    }

    private Integer coverSeed(Long itemId) {
        ItemImage image = imageMapper.selectOne(new LambdaQueryWrapper<ItemImage>()
                .eq(ItemImage::getItemId, itemId)
                .eq(ItemImage::getStatus, "active")
                .orderByDesc(ItemImage::getIsCover)
                .last("LIMIT 1"));
        if (image == null || image.getPath() == null || !image.getPath().startsWith("seed:")) {
            return null;
        }
        try {
            return Integer.parseInt(image.getPath().substring(5));
        } catch (NumberFormatException ex) {
            return Math.abs(image.getPath().hashCode() % 24) + 1;
        }
    }
}
