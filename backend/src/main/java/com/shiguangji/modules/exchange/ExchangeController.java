package com.shiguangji.modules.exchange;

import com.shiguangji.common.api.Result;
import com.shiguangji.common.dto.Dtos.ExchangeIntentDto;
import com.shiguangji.common.dto.Dtos.ExchangePlanDto;
import com.shiguangji.common.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/exchange")
@RequiredArgsConstructor
public class ExchangeController {

    private final ExchangeService exchangeService;

    @GetMapping("/intents")
    public Result<List<ExchangeIntentDto>> intents() {
        return Result.ok(exchangeService.intents(CurrentUser.id()));
    }

    @PostMapping("/intents")
    public Result<ExchangeIntentDto> create(@RequestBody ExchangeService.CreateRequest request) {
        return Result.ok(exchangeService.create(request));
    }

    @GetMapping("/plans")
    public Result<List<ExchangePlanDto>> plans() {
        return Result.ok(exchangeService.plans(CurrentUser.id()));
    }

    @PostMapping("/plans/{id}/confirm")
    public Result<ExchangePlanDto> confirm(@PathVariable Long id) {
        return Result.ok(exchangeService.confirm(id));
    }

    @PostMapping("/plans/{id}/reject")
    public Result<ExchangePlanDto> reject(@PathVariable Long id) {
        return Result.ok(exchangeService.reject(id));
    }
}
