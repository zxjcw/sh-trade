package com.shiguangji.modules.ops;

import com.shiguangji.common.api.Result;
import com.shiguangji.common.dto.Dtos.ArbitrationDto;
import com.shiguangji.common.dto.Dtos.ItemDto;
import com.shiguangji.common.dto.Dtos.ReportDataDto;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/ops")
@RequiredArgsConstructor
public class OpsController {

    private final OpsService opsService;

    @GetMapping("/items/pending")
    public Result<List<ItemDto>> pending() {
        return Result.ok(opsService.pendingItems());
    }

    @PostMapping("/items/{id}/audit")
    public Result<ItemDto> audit(@PathVariable Long id, @RequestBody OpsService.AuditRequest request) {
        return Result.ok(opsService.audit(id, request));
    }

    @GetMapping("/arbitrations")
    public Result<List<ArbitrationDto>> arbitrations() {
        return Result.ok(opsService.arbitrations());
    }

    @PostMapping("/arbitrations/{id}/resolve")
    public Result<ArbitrationDto> resolve(@PathVariable Long id, @RequestBody OpsService.ResolveRequest request) {
        return Result.ok(opsService.resolve(id, request));
    }

    @GetMapping("/report")
    public Result<ReportDataDto> report() {
        return Result.ok(opsService.report());
    }
}
