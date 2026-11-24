package com.shiguangji.modules.trade;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shiguangji.common.dto.Dtos.DeliveryInfoDto;
import com.shiguangji.common.dto.Dtos.OrderDto;
import com.shiguangji.common.entity.AppUser;
import com.shiguangji.common.entity.DeliveryRecord;
import com.shiguangji.common.entity.Item;
import com.shiguangji.common.entity.ItemImage;
import com.shiguangji.common.entity.PickupPoint;
import com.shiguangji.common.entity.Review;
import com.shiguangji.common.entity.TradeOrder;
import com.shiguangji.common.mapper.AppUserMapper;
import com.shiguangji.common.mapper.DeliveryRecordMapper;
import com.shiguangji.common.mapper.ItemImageMapper;
import com.shiguangji.common.mapper.ItemMapper;
import com.shiguangji.common.mapper.PickupPointMapper;
import com.shiguangji.common.mapper.ReviewMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderAssembler {

    private final ItemMapper itemMapper;
    private final ItemImageMapper imageMapper;
    private final AppUserMapper userMapper;
    private final PickupPointMapper pickupPointMapper;
    private final DeliveryRecordMapper deliveryMapper;
    private final ReviewMapper reviewMapper;

    public OrderDto toDto(TradeOrder order, Long viewerId) {
        Item item = itemMapper.selectById(order.getItemId());
        AppUser buyer = userMapper.selectById(order.getBuyerId());
        AppUser seller = userMapper.selectById(order.getSellerId());
        PickupPoint point = order.getPickupPointId() == null ? null
                : pickupPointMapper.selectById(order.getPickupPointId());
        Integer coverSeed = item == null ? null : coverSeed(item.getItemId());
        DeliveryRecord delivery = deliveryMapper.selectOne(new LambdaQueryWrapper<DeliveryRecord>()
                .eq(DeliveryRecord::getOrderNo, order.getOrderNo()));
        DeliveryInfoDto deliveryInfo = delivery == null ? null : new DeliveryInfoDto(
                delivery.getAgreedTime(), delivery.getMeetupPlace(), delivery.getVerifyTime(),
                delivery.getInboundTime(), delivery.getPickupCode(), delivery.getOutboundTime());
        boolean reviewed = viewerId != null && reviewMapper.selectCount(new LambdaQueryWrapper<Review>()
                .eq(Review::getOrderNo, order.getOrderNo())
                .eq(Review::getReviewerId, viewerId)
                .ne(Review::getStatus, "void")) > 0;
        return new OrderDto(order.getOrderNo(), order.getOrderType(), order.getItemId(),
                item == null ? null : item.getTitle(), coverSeed,
                item == null ? null : item.getConditionCode(),
                order.getBuyerId(), buyer == null ? null : buyer.getNickname(),
                order.getSellerId(), seller == null ? null : seller.getNickname(),
                order.getAmount(), order.getDeliveryMethod(), order.getPickupPointId(),
                point == null ? null : point.getName(), order.getTradeStatus(), order.getAfterSaleStatus(),
                order.getCreatedAt(), order.getPayDeadline(), order.getConfirmDeadline(), order.getPaidAt(),
                order.getCompletedAt(), reviewed, order.getSourceBountyId(), order.getPlanId(), deliveryInfo);
    }

    private Integer coverSeed(Long itemId) {
        ItemImage cover = imageMapper.selectOne(new LambdaQueryWrapper<ItemImage>()
                .eq(ItemImage::getItemId, itemId)
                .eq(ItemImage::getStatus, "active")
                .orderByDesc(ItemImage::getIsCover)
                .orderByAsc(ItemImage::getSortOrder)
                .last("LIMIT 1"));
        if (cover == null || cover.getPath() == null || !cover.getPath().startsWith("seed:")) {
            return null;
        }
        try {
            return Integer.parseInt(cover.getPath().substring(5));
        } catch (NumberFormatException ex) {
            return Math.abs(cover.getPath().hashCode() % 24) + 1;
        }
    }
}
