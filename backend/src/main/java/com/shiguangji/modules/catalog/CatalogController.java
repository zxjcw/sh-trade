package com.shiguangji.modules.catalog;

import com.shiguangji.common.api.Result;
import com.shiguangji.common.dto.Dtos.CategoryDto;
import com.shiguangji.common.dto.Dtos.PickupPointDto;
import com.shiguangji.common.dto.Dtos.PublicStatsDto;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CatalogController {

    private final CatalogService catalogService;

    @GetMapping("/categories")
    public Result<List<CategoryDto>> categories() {
        return Result.ok(catalogService.flatEnabled());
    }

    @GetMapping("/pickup-points")
    public Result<List<PickupPointDto>> pickupPoints() {
        return Result.ok(catalogService.pickupPoints());
    }

    @GetMapping("/public/stats")
    public Result<PublicStatsDto> stats() {
        return Result.ok(catalogService.publicStats());
    }
}
