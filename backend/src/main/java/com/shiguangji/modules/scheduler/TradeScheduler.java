package com.shiguangji.modules.scheduler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shiguangji.common.config.AppProperties;
import com.shiguangji.common.entity.Bounty;
import com.shiguangji.common.entity.ExchangePlan;
import com.shiguangji.common.entity.Review;
import com.shiguangji.common.entity.TradeOrder;
import com.shiguangji.common.mapper.BountyMapper;
import com.shiguangji.common.mapper.ExchangePlanMapper;
import com.shiguangji.common.mapper.ReviewMapper;
import com.shiguangji.common.mapper.TradeOrderMapper;
import com.shiguangji.modules.exchange.ExchangeService;
import com.shiguangji.modules.notification.NotificationService;
import com.shiguangji.modules.trade.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * 交易相关定时任务：支付超时取消、自动确认收货、评价窗口兜底、售后超时自动退款、
 * 置换方案确认超时、求购过期。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TradeScheduler {

    private final OrderService orderService;
    private final ExchangeService exchangeService;
    private final TradeOrderMapper orderMapper;
    private final ReviewMapper reviewMapper;
    private final ExchangePlanMapper planMapper;
    private final BountyMapper bountyMapper;
    private final NotificationService notificationService;
    private final AppProperties properties;

    @Scheduled(fixedDelay = 3000)
    public void tick() {
        LocalDateTime now = LocalDateTime.now();
        cancelUnpaid(now);
        autoConfirm(now);
        fillReviews(now);
        aftersaleTimeout(now);
        expirePlans(now);
        expireBounties(now);
    }

    private void cancelUnpaid(LocalDateTime now) {
        List<TradeOrder> orders = orderMapper.selectList(new LambdaQueryWrapper<TradeOrder>()
                .eq(TradeOrder::getTradeStatus, "pending_payment")
                .lt(TradeOrder::getPayDeadline, now));
        for (TradeOrder order : orders) {
            try {
                orderService.cancelExpired(order);
                notificationService.send(order.getBuyerId(), "order", "order", order.getOrderNo(),
                        "订单 " + order.getOrderNo() + " 支付超时，已自动取消。", order.getOrderNo());
            } catch (Exception ex) {
                log.warn("取消超时订单失败 {}: {}", order.getOrderNo(), ex.getMessage());
            }
        }
    }

    private void autoConfirm(LocalDateTime now) {
        List<TradeOrder> orders = orderMapper.selectList(new LambdaQueryWrapper<TradeOrder>()
                .eq(TradeOrder::getTradeStatus, "pending_confirm")
                .lt(TradeOrder::getConfirmDeadline, now));
        for (TradeOrder order : orders) {
            try {
                orderService.complete(order);
            } catch (Exception ex) {
                log.warn("自动确认收货失败 {}: {}", order.getOrderNo(), ex.getMessage());
            }
        }
    }

    private void fillReviews(LocalDateTime now) {
        LocalDateTime deadline = now.minus(properties.timeout().reviewWindow());
        List<TradeOrder> orders = orderMapper.selectList(new LambdaQueryWrapper<TradeOrder>()
                .eq(TradeOrder::getTradeStatus, "completed")
                .lt(TradeOrder::getCompletedAt, deadline));
        for (TradeOrder order : orders) {
            try {
                writeSystemReview(order, order.getBuyerId(), order.getSellerId(), now);
                writeSystemReview(order, order.getSellerId(), order.getBuyerId(), now);
            } catch (Exception ex) {
                log.warn("自动评价失败 {}: {}", order.getOrderNo(), ex.getMessage());
            }
        }
    }

    private void writeSystemReview(TradeOrder order, Long reviewerId, Long targetId, LocalDateTime now) {
        Long exists = reviewMapper.selectCount(new LambdaQueryWrapper<Review>()
                .eq(Review::getOrderNo, order.getOrderNo())
                .eq(Review::getReviewerId, reviewerId)
                .ne(Review::getStatus, "void"));
        if (exists != null && exists > 0) {
            return;
        }
        Review review = new Review();
        review.setOrderNo(order.getOrderNo());
        review.setReviewerId(reviewerId);
        review.setTargetUserId(targetId);
        review.setRating(5);
        review.setContent("评价超时，系统默认好评");
        review.setStatus("system");
        review.setCreatedAt(now);
        reviewMapper.insert(review);
        notificationService.send(reviewerId, "order", "order", order.getOrderNo(),
                "订单 " + order.getOrderNo() + " 评价超时，已生成系统默认好评。", order.getOrderNo());
    }

    private void aftersaleTimeout(LocalDateTime now) {
        LocalDateTime deadline = now.minus(properties.timeout().aftersaleReply());
        List<TradeOrder> orders = orderMapper.selectList(new LambdaQueryWrapper<TradeOrder>()
                .eq(TradeOrder::getAfterSaleStatus, "refunding")
                .lt(TradeOrder::getUpdatedAt, deadline));
        for (TradeOrder order : orders) {
            try {
                orderService.autoRefundAfterSale(order);
            } catch (Exception ex) {
                log.warn("售后超时自动退款失败 {}: {}", order.getOrderNo(), ex.getMessage());
            }
        }
    }

    private void expirePlans(LocalDateTime now) {
        List<ExchangePlan> plans = planMapper.selectList(new LambdaQueryWrapper<ExchangePlan>()
                .eq(ExchangePlan::getStatus, "pending")
                .lt(ExchangePlan::getConfirmDeadline, now));
        for (ExchangePlan plan : plans) {
            try {
                exchangeService.expirePlan(plan.getPlanId());
            } catch (Exception ex) {
                log.warn("置换方案超时作废失败 {}: {}", plan.getPlanId(), ex.getMessage());
            }
        }
    }

    private void expireBounties(LocalDateTime now) {
        List<Bounty> bounties = bountyMapper.selectList(new LambdaQueryWrapper<Bounty>()
                .eq(Bounty::getStatus, "open")
                .lt(Bounty::getExpiresAt, now));
        for (Bounty bounty : bounties) {
            bounty.setStatus("expired");
            bounty.setClosedAt(now);
            bountyMapper.updateById(bounty);
            notificationService.send(bounty.getBuyerId(), "bounty", "bounty", bounty.getBountyId(),
                    "你的求购「" + (bounty.getKeyword() == null ? "" : bounty.getKeyword()) + "」已到期关闭。", null);
        }
    }
}
