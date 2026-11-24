package com.shiguangji.modules.item;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shiguangji.common.dto.Dtos.HistoryEventDto;
import com.shiguangji.common.dto.Dtos.ItemDto;
import com.shiguangji.common.dto.Dtos.ItemImageDto;
import com.shiguangji.common.entity.AppUser;
import com.shiguangji.common.entity.Category;
import com.shiguangji.common.entity.Favorite;
import com.shiguangji.common.entity.Item;
import com.shiguangji.common.entity.ItemHistory;
import com.shiguangji.common.entity.ItemImage;
import com.shiguangji.common.mapper.AppUserMapper;
import com.shiguangji.common.mapper.CategoryMapper;
import com.shiguangji.common.mapper.FavoriteMapper;
import com.shiguangji.common.mapper.ItemHistoryMapper;
import com.shiguangji.common.mapper.ItemImageMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 物品聚合：分类名、卖家、图片、履历、收藏状态。
 */
@Component
@RequiredArgsConstructor
public class ItemAssembler {

    private final CategoryMapper categoryMapper;
    private final AppUserMapper userMapper;
    private final ItemImageMapper imageMapper;
    private final ItemHistoryMapper historyMapper;
    private final FavoriteMapper favoriteMapper;

    private final Map<Long, String> categoryCache = new ConcurrentHashMap<>();

    public ItemDto toDto(Item item, Long viewerId) {
        Category category = item.getCategoryId() == null ? null : categoryMapper.selectById(item.getCategoryId());
        AppUser holder = item.getCurrentHolderId() == null ? null : user(item.getCurrentHolderId());
        List<ItemImageDto> images = images(item.getItemId());
        List<HistoryEventDto> history = history(item.getItemId());
        boolean favorited = viewerId != null && favoriteMapper.selectCount(new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getUserId, viewerId).eq(Favorite::getItemId, item.getItemId())) > 0;
        Integer coverSeed = images.stream()
                .filter(img -> Boolean.TRUE.equals(img.isCover()) && img.seed() != null)
                .map(ItemImageDto::seed).findFirst().orElse(null);
        return new ItemDto(item.getItemId(), item.getTitle(), item.getCategoryId(),
                category == null ? null : category.getName(), item.getPrice(), item.getOriginalPrice(),
                item.getPurchasePrice(), item.getConditionCode(), item.getDescription(), item.getStatus(),
                item.getSource(), item.getNotesTag(), item.getCurrentHolderId(),
                holder == null ? null : holder.getNickname(), holder == null ? null : holder.getCreditScore(),
                coverSeed, images, history, item.getCreatedAt(), item.getUpdatedAt(), item.getRejectReason(),
                favorited);
    }

    public List<ItemImageDto> images(Long itemId) {
        return imageMapper.selectList(new LambdaQueryWrapper<ItemImage>()
                        .eq(ItemImage::getItemId, itemId)
                        .eq(ItemImage::getStatus, "active")
                        .orderByDesc(ItemImage::getIsCover)
                        .orderByAsc(ItemImage::getSortOrder)
                        .orderByAsc(ItemImage::getImageId))
                .stream().map(this::imageDto).toList();
    }

    public List<HistoryEventDto> history(Long itemId) {
        return historyMapper.selectList(new LambdaQueryWrapper<ItemHistory>()
                        .eq(ItemHistory::getItemId, itemId)
                        .orderByAsc(ItemHistory::getEventTime)
                        .orderByAsc(ItemHistory::getHistoryId))
                .stream()
                .map(h -> new HistoryEventDto(h.getHistoryId(), h.getEventType(), h.getEventTime(),
                        h.getDescription(), h.getOrderNo(),
                        h.getRelatedUserId() == null ? null : nickname(h.getRelatedUserId())))
                .toList();
    }

    public ItemImageDto imageDto(ItemImage image) {
        Integer seed = null;
        String url = null;
        String path = image.getPath();
        if (path != null && path.startsWith("seed:")) {
            try {
                seed = Integer.parseInt(path.substring(5));
            } catch (NumberFormatException ignored) {
                seed = Math.abs(path.hashCode() % 24) + 1;
            }
        } else {
            url = path;
        }
        return new ItemImageDto(image.getImageId(), seed, Boolean.TRUE.equals(image.getIsCover()),
                image.getStatus(), url);
    }

    private String nickname(Long userId) {
        AppUser user = user(userId);
        return user == null ? null : user.getNickname();
    }

    public AppUser user(Long userId) {
        return userMapper.selectById(userId);
    }

    public String categoryName(Long categoryId) {
        if (categoryId == null) {
            return null;
        }
        return categoryCache.computeIfAbsent(categoryId, id -> {
            Category c = categoryMapper.selectById(id);
            return c == null ? null : c.getName();
        });
    }
}
