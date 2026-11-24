package com.shiguangji.modules.notification;

import com.shiguangji.common.api.Result;
import com.shiguangji.common.dto.Dtos.NotificationDto;
import com.shiguangji.common.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public Result<List<NotificationDto>> list() {
        return Result.ok(notificationService.list(CurrentUser.id()));
    }

    @PostMapping("/{id}/read")
    public Result<Void> read(@PathVariable Long id) {
        notificationService.markRead(CurrentUser.id(), id);
        return Result.ok();
    }

    @PostMapping("/read-all")
    public Result<Void> readAll() {
        notificationService.markAllRead(CurrentUser.id());
        return Result.ok();
    }
}
