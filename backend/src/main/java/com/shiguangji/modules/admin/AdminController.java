package com.shiguangji.modules.admin;

import com.shiguangji.common.api.Result;
import com.shiguangji.common.dto.Dtos.CategoryDto;
import com.shiguangji.common.dto.Dtos.PickupPointDto;
import com.shiguangji.common.dto.Dtos.RosterEntryDto;
import com.shiguangji.common.dto.Dtos.UserDto;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/roster")
    public Result<List<RosterEntryDto>> roster(@RequestParam(required = false) String keyword) {
        return Result.ok(adminService.roster(keyword));
    }

    @PostMapping("/roster/import")
    public Result<Map<String, Object>> importRoster(@RequestBody AdminService.RosterImportRequest request) {
        return Result.ok(adminService.importRoster(request));
    }

    @GetMapping("/categories")
    public Result<List<CategoryDto>> categories() {
        return Result.ok(adminService.categoryTree());
    }

    @PostMapping("/categories")
    public Result<CategoryDto> createCategory(@RequestBody AdminService.CategoryRequest request) {
        return Result.ok(adminService.createCategory(request));
    }

    @PutMapping("/categories/{id}")
    public Result<CategoryDto> updateCategory(@PathVariable Long id,
                                              @RequestBody AdminService.CategoryRequest request) {
        return Result.ok(adminService.updateCategory(id, request));
    }

    @DeleteMapping("/categories/{id}")
    public Result<Void> deleteCategory(@PathVariable Long id) {
        adminService.deleteCategory(id);
        return Result.ok();
    }

    @GetMapping("/pickup-points")
    public Result<List<PickupPointDto>> pickupPoints() {
        return Result.ok(adminService.pickupPoints());
    }

    @PostMapping("/pickup-points")
    public Result<PickupPointDto> createPickupPoint(@RequestBody AdminService.PickupPointRequest request) {
        return Result.ok(adminService.createPickupPoint(request));
    }

    @PutMapping("/pickup-points/{id}")
    public Result<PickupPointDto> updatePickupPoint(@PathVariable Long id,
                                                    @RequestBody AdminService.PickupPointRequest request) {
        return Result.ok(adminService.updatePickupPoint(id, request));
    }

    @DeleteMapping("/pickup-points/{id}")
    public Result<Void> deletePickupPoint(@PathVariable Long id) {
        adminService.deletePickupPoint(id);
        return Result.ok();
    }

    @GetMapping("/operators")
    public Result<List<UserDto>> operators() {
        return Result.ok(adminService.operators());
    }

    @PostMapping("/operators")
    public Result<UserDto> createOperator(@RequestBody AdminService.OperatorRequest request) {
        return Result.ok(adminService.createOperator(request));
    }
}
