package com.shiguangji.modules.item;

import com.shiguangji.common.api.Result;
import com.shiguangji.common.dto.Dtos.FavoriteResult;
import com.shiguangji.common.dto.Dtos.ItemDto;
import com.shiguangji.common.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ItemController {

    private final ItemService itemService;

    @GetMapping("/items")
    public Result<List<ItemDto>> list(@RequestParam(required = false) String keyword,
                                      @RequestParam(required = false) Long categoryId,
                                      @RequestParam(required = false) String condition,
                                      @RequestParam(required = false) String status,
                                      @RequestParam(required = false) String sort,
                                      @RequestParam(required = false) String scope,
                                      @RequestParam(required = false) Integer limit) {
        // 公开浏览：未登录时 viewerId 为 null（scope=mine 仍要求登录）
        return Result.ok(itemService.list(keyword, categoryId, condition, status, sort, scope, limit,
                CurrentUser.idOrNull(), CurrentUser.isOpsOrFalse()));
    }

    @GetMapping("/items/{id}")
    public Result<ItemDto> detail(@PathVariable Long id) {
        return Result.ok(itemService.detail(id, CurrentUser.idOrNull()));
    }

    @PostMapping("/items")
    public Result<ItemDto> create(@RequestBody ItemService.ItemRequest request) {
        return Result.ok(itemService.create(request));
    }

    @PostMapping("/items/{id}/relist")
    public Result<ItemDto> relist(@PathVariable Long id,
                                  @RequestBody(required = false) ItemService.RelistRequest request) {
        return Result.ok(itemService.relist(id, request));
    }

    @PostMapping("/items/{id}/favorite")
    public Result<FavoriteResult> favorite(@PathVariable Long id) {
        return Result.ok(itemService.toggleFavorite(id));
    }

    @GetMapping("/favorites")
    public Result<List<ItemDto>> favorites() {
        return Result.ok(itemService.favorites(CurrentUser.id()));
    }
}
