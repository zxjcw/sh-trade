package com.shiguangji.modules.catalog;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shiguangji.common.dto.Dtos.CategoryDto;
import com.shiguangji.common.dto.Dtos.PickupPointDto;
import com.shiguangji.common.dto.Dtos.PublicStatsDto;
import com.shiguangji.common.entity.Category;
import com.shiguangji.common.entity.PickupPoint;
import com.shiguangji.common.mapper.CategoryMapper;
import com.shiguangji.common.mapper.PickupPointMapper;
import com.shiguangji.common.mapper.ReportMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CatalogService {

    private final CategoryMapper categoryMapper;
    private final PickupPointMapper pickupPointMapper;
    private final ReportMapper reportMapper;

    /** 分类树：只含启用项，两级。 */
    public List<CategoryDto> categoryTree(boolean includeDisabled) {
        List<Category> all = categoryMapper.selectList(new LambdaQueryWrapper<Category>()
                .orderByAsc(Category::getParentId)
                .orderByAsc(Category::getCategoryId));
        if (!includeDisabled) {
            all = all.stream().filter(c -> Boolean.TRUE.equals(c.getEnabled())).toList();
        }
        Map<Long, List<Category>> byParent = all.stream()
                .filter(c -> c.getParentId() != null)
                .collect(Collectors.groupingBy(Category::getParentId));
        List<CategoryDto> result = new ArrayList<>();
        for (Category root : all) {
            if (root.getParentId() != null) {
                continue;
            }
            List<CategoryDto> children = byParent.getOrDefault(root.getCategoryId(), List.of()).stream()
                    .map(child -> new CategoryDto(child.getCategoryId(), child.getName(), child.getParentId(),
                            Boolean.TRUE.equals(child.getEnabled()), List.of()))
                    .toList();
            result.add(new CategoryDto(root.getCategoryId(), root.getName(), root.getParentId(),
                    Boolean.TRUE.equals(root.getEnabled()), children));
        }
        return result;
    }

    public List<CategoryDto> flatEnabled() {
        return categoryMapper.selectList(new LambdaQueryWrapper<Category>()
                        .eq(Category::getEnabled, true)
                        .orderByAsc(Category::getParentId)
                        .orderByAsc(Category::getCategoryId))
                .stream()
                .map(c -> new CategoryDto(c.getCategoryId(), c.getName(), c.getParentId(), true, null))
                .toList();
    }

    public List<PickupPointDto> pickupPoints() {
        return pickupPointMapper.selectList(new LambdaQueryWrapper<PickupPoint>()
                        .orderByAsc(PickupPoint::getPickupPointId))
                .stream()
                .map(p -> new PickupPointDto(p.getPickupPointId(), p.getName(), p.getAddress()))
                .toList();
    }

    public PublicStatsDto publicStats() {
        BigDecimal amount = reportMapper.completedAmount();
        return new PublicStatsDto(reportMapper.verifiedUsers(), reportMapper.onSaleItems(),
                reportMapper.completedCount(), amount == null ? BigDecimal.ZERO : amount);
    }
}
