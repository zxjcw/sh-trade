package com.shiguangji.modules.trade;

import com.shiguangji.common.api.Result;
import com.shiguangji.common.dto.Dtos.OrderDto;
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
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @GetMapping
    public Result<List<OrderDto>> list(@RequestParam(required = false) String role,
                                       @RequestParam(required = false) String status) {
        return Result.ok(orderService.list(CurrentUser.id(), role, status));
    }

    @GetMapping("/{id}")
    public Result<OrderDto> detail(@PathVariable String id) {
        return Result.ok(orderService.detail(id, CurrentUser.id(), CurrentUser.isOps()));
    }

    @PostMapping
    public Result<OrderDto> create(@RequestBody OrderService.CreateRequest request) {
        return Result.ok(orderService.create(request));
    }

    @PostMapping("/{id}/pay")
    public Result<OrderDto> pay(@PathVariable String id) {
        return Result.ok(orderService.pay(id));
    }

    @PostMapping("/{id}/cancel")
    public Result<OrderDto> cancel(@PathVariable String id) {
        return Result.ok(orderService.cancel(id));
    }

    @PostMapping("/{id}/confirm")
    public Result<OrderDto> confirm(@PathVariable String id) {
        return Result.ok(orderService.confirm(id));
    }

    @PostMapping("/{id}/review")
    public Result<OrderDto> review(@PathVariable String id,
                                   @RequestBody OrderService.ReviewRequest request) {
        return Result.ok(orderService.review(id, request));
    }

    @PostMapping("/{id}/aftersale")
    public Result<OrderDto> aftersale(@PathVariable String id,
                                      @RequestBody(required = false) OrderService.AfterSaleRequest request) {
        return Result.ok(orderService.aftersale(id, request));
    }

    @PostMapping("/{id}/delivery/meetup")
    public Result<OrderDto> meetup(@PathVariable String id,
                                   @RequestBody(required = false) OrderService.MeetupRequest request) {
        return Result.ok(orderService.meetup(id, request));
    }

    @PostMapping("/{id}/delivery/verify")
    public Result<OrderDto> verify(@PathVariable String id) {
        return Result.ok(orderService.verify(id));
    }

    @PostMapping("/{id}/delivery/inbound")
    public Result<OrderDto> inbound(@PathVariable String id) {
        return Result.ok(orderService.inbound(id));
    }

    @PostMapping("/{id}/delivery/outbound")
    public Result<OrderDto> outbound(@PathVariable String id) {
        return Result.ok(orderService.outbound(id));
    }
}
