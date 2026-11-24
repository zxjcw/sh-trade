package com.shiguangji.modules.user;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shiguangji.common.dto.Dtos.ReviewDto;
import com.shiguangji.common.entity.AppUser;
import com.shiguangji.common.entity.Item;
import com.shiguangji.common.entity.Review;
import com.shiguangji.common.entity.TradeOrder;
import com.shiguangji.common.mapper.AppUserMapper;
import com.shiguangji.common.mapper.ItemMapper;
import com.shiguangji.common.mapper.ReviewMapper;
import com.shiguangji.common.mapper.TradeOrderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ReviewQueryService {

    private final ReviewMapper reviewMapper;
    private final TradeOrderMapper orderMapper;
    private final ItemMapper itemMapper;
    private final AppUserMapper userMapper;

    /** 我发出的评价 + 我收到的评价。 */
    public List<ReviewDto> mine(Long userId) {
        List<Review> reviews = reviewMapper.selectList(new LambdaQueryWrapper<Review>()
                .and(w -> w.eq(Review::getReviewerId, userId).or().eq(Review::getTargetUserId, userId))
                .orderByDesc(Review::getCreatedAt)
                .orderByDesc(Review::getReviewId));
        List<ReviewDto> result = new ArrayList<>();
        for (Review review : reviews) {
            TradeOrder order = orderMapper.selectById(review.getOrderNo());
            Item item = order == null ? null : itemMapper.selectById(order.getItemId());
            result.add(new ReviewDto(review.getReviewId(), review.getOrderNo(),
                    item == null ? null : item.getTitle(), review.getReviewerId(),
                    nameOf(review.getReviewerId()), nameOf(review.getTargetUserId()), review.getRating(),
                    parseTags(review.getTags()), review.getContent(), review.getStatus(), review.getCreatedAt()));
        }
        return result;
    }

    private String nameOf(Long userId) {
        if (userId == null) {
            return null;
        }
        AppUser user = userMapper.selectById(userId);
        return user == null ? null : user.getNickname();
    }

    /** tags 列为 JSON 数组字符串，容错解析。 */
    public static List<String> parseTags(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        String body = json.trim();
        if (body.startsWith("[")) {
            body = body.substring(1);
        }
        if (body.endsWith("]")) {
            body = body.substring(0, body.length() - 1);
        }
        List<String> tags = new ArrayList<>();
        for (String part : body.split(",")) {
            String tag = part.trim().replaceAll("^\"|\"$", "");
            if (!tag.isEmpty()) {
                tags.add(tag);
            }
        }
        return tags.stream().filter(Objects::nonNull).toList();
    }
}
