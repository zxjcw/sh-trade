package com.shiguangji.modules.bounty;

import com.shiguangji.common.api.Result;
import com.shiguangji.common.dto.Dtos.BountyDto;
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
@RequestMapping("/api/bounties")
@RequiredArgsConstructor
public class BountyController {

    private final BountyService bountyService;

    @GetMapping
    public Result<List<BountyDto>> list(@RequestParam(required = false, defaultValue = "hall") String scope) {
        // 大厅公开，未登录也能看（mine 仍要求登录）
        return Result.ok(bountyService.list(scope, CurrentUser.idOrNull()));
    }

    @PostMapping
    public Result<BountyDto> create(@RequestBody BountyService.CreateRequest request) {
        return Result.ok(bountyService.create(request));
    }

    @PostMapping("/{id}/close")
    public Result<BountyDto> close(@PathVariable Long id) {
        return Result.ok(bountyService.close(id));
    }
}
