package com.shiguangji.modules.item;

import com.shiguangji.common.api.ErrorCodes;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shiguangji.common.dto.Dtos.FavoriteResult;
import com.shiguangji.common.dto.Dtos.ItemDto;
import com.shiguangji.common.entity.AppUser;
import com.shiguangji.common.entity.Category;
import com.shiguangji.common.entity.Favorite;
import com.shiguangji.common.entity.Item;
import com.shiguangji.common.entity.ItemImage;
import com.shiguangji.common.entity.TradeOrder;
import com.shiguangji.common.exception.BizException;
import com.shiguangji.common.mapper.CategoryMapper;
import com.shiguangji.common.mapper.FavoriteMapper;
import com.shiguangji.common.mapper.ItemImageMapper;
import com.shiguangji.common.mapper.ItemMapper;
import com.shiguangji.common.mapper.TradeOrderMapper;
import com.shiguangji.common.security.CurrentUser;
import com.shiguangji.modules.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ItemService {

    private final ItemMapper itemMapper;
    private final ItemImageMapper imageMapper;
    private final CategoryMapper categoryMapper;
    private final FavoriteMapper favoriteMapper;
    private final TradeOrderMapper orderMapper;
    private final ItemAssembler assembler;
    private final UserService userService;

    /**
     * 发布物品的请求体。前端只传 coverSeed（一个随机整数，用于生成占位图），
     * images 保留给需要传真实图片 URL 的场景（如管理员导入）。
     */
    public record ItemRequest(String title, Long categoryId, BigDecimal price, BigDecimal originalPrice,
                             BigDecimal purchasePrice, String condition, String description,
                             String notesTag, String source, Integer coverSeed, List<String> images) {
    }

    public record RelistRequest(BigDecimal price, BigDecimal purchasePrice, String notesTag,
                               String description, String condition) {
    }

    public List<ItemDto> list(String keyword, Long categoryId, String condition, String status,
                             String sort, String scope, Integer limit, Long viewerId, boolean ops) {
        LambdaQueryWrapper<Item> wrapper = new LambdaQueryWrapper<>();
        boolean mine = "mine".equals(scope);
        if (mine && viewerId == null) {
            throw new BizException(ErrorCodes.UNAUTHORIZED, "请先登录");
        }
        if (mine) {
            wrapper.eq(Item::getCurrentHolderId, viewerId);
            if (status != null && !status.isBlank()) {
                wrapper.eq(Item::getStatus, status);
            }
        } else if (ops && status != null && !status.isBlank()) {
            wrapper.eq(Item::getStatus, status);
        } else {
            wrapper.eq(Item::getStatus, "on_sale");
        }
        if (categoryId != null) {
            List<Long> ids = new ArrayList<>();
            ids.add(categoryId);
            categoryMapper.selectList(new LambdaQueryWrapper<Category>().eq(Category::getParentId, categoryId))
                    .forEach(c -> ids.add(c.getCategoryId()));
            wrapper.in(Item::getCategoryId, ids);
        }
        if (keyword != null && !keyword.isBlank()) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like(Item::getTitle, kw).or().like(Item::getDescription, kw));
        }
        if (condition != null && !condition.isBlank()) {
            wrapper.eq(Item::getConditionCode, condition);
        }
        switch (sort == null ? "latest" : sort) {
            case "price_asc" -> wrapper.orderByAsc(Item::getPrice);
            case "price_desc" -> wrapper.orderByDesc(Item::getPrice);
            default -> wrapper.orderByDesc(Item::getListedAt).orderByDesc(Item::getItemId);
        }
        wrapper.last("LIMIT " + Math.min(Math.max(limit == null ? 60 : limit, 1), 200));
        return itemMapper.selectList(wrapper).stream()
                .map(item -> assembler.toDto(item, viewerId)).toList();
    }

    public ItemDto detail(Long itemId, Long viewerId) {
        Item item = itemMapper.selectById(itemId);
        if (item == null) {
            throw BizException.notFound("物品不存在");
        }
        boolean visible = "on_sale".equals(item.getStatus())
                || Objects.equals(item.getCurrentHolderId(), viewerId)
                || CurrentUser.isOpsOrFalse()
                || participated(itemId, viewerId);
        if (!visible) {
            throw BizException.notFound("物品不存在");
        }
        return assembler.toDto(item, viewerId);
    }

    private boolean participated(Long itemId, Long viewerId) {
        if (viewerId == null) {
            return false;
        }
        return orderMapper.selectCount(new LambdaQueryWrapper<TradeOrder>()
                .eq(TradeOrder::getItemId, itemId)
                .and(w -> w.eq(TradeOrder::getBuyerId, viewerId).or().eq(TradeOrder::getSellerId, viewerId))) > 0;
    }

    @Transactional
    public ItemDto create(ItemRequest request) {
        AppUser me = userService.current();
        userService.requireVerified(me);
        if (request.title() == null || request.title().isBlank()) {
            throw BizException.badRequest("请填写物品标题");
        }
        if (request.price() == null || request.price().compareTo(BigDecimal.ZERO) <= 0) {
            throw BizException.badRequest("请填写正确的物品价格");
        }
        if (request.categoryId() == null) {
            throw BizException.badRequest("请选择物品分类");
        }
        Category category = categoryMapper.selectById(request.categoryId());
        if (category == null || !Boolean.TRUE.equals(category.getEnabled())) {
            throw BizException.badRequest("物品分类不存在或已停用");
        }
        Item item = new Item();
        item.setTitle(request.title().trim());
        item.setCategoryId(request.categoryId());
        item.setCurrentHolderId(me.getUserId());
        item.setPrice(request.price());
        // original_price 列 NOT NULL 且无默认值，前端可以不填，缺失时按售价兜底
        item.setOriginalPrice(request.originalPrice() == null ? request.price() : request.originalPrice());
        item.setPurchasePrice(request.purchasePrice());
        item.setConditionCode(normalizeCondition(request.condition()));
        item.setDescription(request.description());
        item.setNotesTag(request.notesTag() == null || request.notesTag().isBlank()
                ? "no_notes" : request.notesTag());
        item.setSource("platform".equals(request.source()) ? "platform" : "offline");
        item.setStatus("pending");
        item.setCreatedAt(LocalDateTime.now());
        item.setUpdatedAt(LocalDateTime.now());
        itemMapper.insert(item);
        if (request.images() != null && !request.images().isEmpty()) {
            saveImages(item.getItemId(), request.images());
        } else {
            saveImages(item.getItemId(), List.of("seed:" + (request.coverSeed() == null
                    ? java.util.concurrent.ThreadLocalRandom.current().nextInt(20, 920)
                    : request.coverSeed())));
        }
        return assembler.toDto(item, me.getUserId());
    }

    private static final java.util.Set<String> CONDITIONS =
            java.util.Set.of("new", "like_new", "good", "fair", "poor");

    static String normalizeCondition(String condition) {
        return condition != null && CONDITIONS.contains(condition) ? condition : "good";
    }

    @Transactional
    public ItemDto relist(Long itemId, RelistRequest request) {
        AppUser me = userService.current();
        userService.requireVerified(me);
        Item origin = itemMapper.selectById(itemId);
        if (origin == null) {
            throw BizException.notFound("物品不存在");
        }
        if (!"completed".equals(origin.getStatus())) {
            throw BizException.badRequest("只有已成交的物品可以再次上架");
        }
        if (!Objects.equals(origin.getCurrentHolderId(), me.getUserId())) {
            throw BizException.forbidden("只能上架自己持有的物品");
        }
        Item item = new Item();
        item.setTitle(origin.getTitle());
        item.setCategoryId(origin.getCategoryId());
        item.setCurrentHolderId(me.getUserId());
        item.setPrice(request != null && request.price() != null ? request.price() : origin.getPrice());
        item.setOriginalPrice(origin.getOriginalPrice());
        item.setPurchasePrice(request != null && request.purchasePrice() != null
                ? request.purchasePrice() : origin.getPurchasePrice());
        item.setConditionCode(request != null && request.condition() != null
                ? normalizeCondition(request.condition()) : origin.getConditionCode());
        item.setDescription(request != null && request.description() != null
                ? request.description() : origin.getDescription());
        item.setNotesTag(request != null && request.notesTag() != null ? request.notesTag() : origin.getNotesTag());
        item.setSource("platform");
        item.setStatus("pending");
        item.setCreatedAt(LocalDateTime.now());
        item.setUpdatedAt(LocalDateTime.now());
        itemMapper.insert(item);
        List<ItemImage> oldImages = imageMapper.selectList(new LambdaQueryWrapper<ItemImage>()
                .eq(ItemImage::getItemId, itemId)
                .eq(ItemImage::getStatus, "active"));
        for (ItemImage old : oldImages) {
            ItemImage copy = new ItemImage();
            copy.setItemId(item.getItemId());
            copy.setPath(old.getPath());
            copy.setSortOrder(old.getSortOrder());
            copy.setIsCover(old.getIsCover());
            copy.setStatus("active");
            imageMapper.insert(copy);
            old.setStatus("history");
            imageMapper.updateById(old);
        }
        return assembler.toDto(item, me.getUserId());
    }

    @Transactional
    public FavoriteResult toggleFavorite(Long itemId) {
        Long me = CurrentUser.id();
        Item item = itemMapper.selectById(itemId);
        if (item == null) {
            throw BizException.notFound("物品不存在");
        }
        Favorite existing = favoriteMapper.selectOne(new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getUserId, me).eq(Favorite::getItemId, itemId));
        if (existing != null) {
            favoriteMapper.deleteById(existing.getFavoriteId());
            return new FavoriteResult(false);
        }
        Favorite favorite = new Favorite();
        favorite.setUserId(me);
        favorite.setItemId(itemId);
        favoriteMapper.insert(favorite);
        return new FavoriteResult(true);
    }

    public List<ItemDto> favorites(Long viewerId) {
        List<Favorite> favorites = favoriteMapper.selectList(new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getUserId, viewerId)
                .orderByDesc(Favorite::getFavoriteId));
        List<ItemDto> result = new ArrayList<>();
        for (Favorite favorite : favorites) {
            Item item = itemMapper.selectById(favorite.getItemId());
            if (item != null) {
                result.add(assembler.toDto(item, viewerId));
            }
        }
        return result;
    }

    private void saveImages(Long itemId, List<String> images) {
        if (images == null || images.isEmpty()) {
            return;
        }
        int order = 0;
        for (String raw : images.stream().filter(Objects::nonNull).limit(6).toList()) {
            String path = raw.startsWith("seed:") ? raw : raw.trim();
            if (path.isBlank()) {
                continue;
            }
            ItemImage image = new ItemImage();
            image.setItemId(itemId);
            image.setPath(path);
            image.setSortOrder(order);
            image.setIsCover(order == 0);
            image.setStatus("active");
            imageMapper.insert(image);
            order++;
        }
    }
}
