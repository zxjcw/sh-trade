package com.shiguangji.modules.notification;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shiguangji.common.dto.Dtos.NotificationDto;
import com.shiguangji.common.entity.Notification;
import com.shiguangji.common.mapper.NotificationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationMapper notificationMapper;

    /** 站内信，失败不影响主流程（调用方自行捕获）。 */
    public void send(Long receiverId, String type, String relatedType, Object relatedId,
                     String content, String link) {
        Notification n = new Notification();
        n.setReceiverId(receiverId);
        n.setNotificationType(normalizeType(type));
        n.setRelatedType(relatedType);
        n.setRelatedId(relatedId == null ? null : String.valueOf(relatedId));
        n.setContent(content);
        n.setIsRead(false);
        n.setCreatedAt(LocalDateTime.now());
        notificationMapper.insert(n);
    }

    public List<NotificationDto> list(Long userId) {
        return notificationMapper.selectList(new LambdaQueryWrapper<Notification>()
                        .eq(Notification::getReceiverId, userId)
                        .orderByDesc(Notification::getCreatedAt)
                        .orderByDesc(Notification::getNotificationId))
                .stream().map(this::toDto).toList();
    }

    public void markRead(Long userId, Long id) {
        Notification n = notificationMapper.selectById(id);
        if (n == null || !n.getReceiverId().equals(userId)) {
            throw com.shiguangji.common.exception.BizException.notFound("通知不存在");
        }
        if (!Boolean.TRUE.equals(n.getIsRead())) {
            n.setIsRead(true);
            n.setReadAt(LocalDateTime.now());
            notificationMapper.updateById(n);
        }
    }

    public void markAllRead(Long userId) {
        List<Notification> unread = notificationMapper.selectList(new LambdaQueryWrapper<Notification>()
                .eq(Notification::getReceiverId, userId)
                .eq(Notification::getIsRead, false));
        LocalDateTime now = LocalDateTime.now();
        for (Notification n : unread) {
            n.setIsRead(true);
            n.setReadAt(now);
            notificationMapper.updateById(n);
        }
    }

    private NotificationDto toDto(Notification n) {
        return new NotificationDto(n.getNotificationId(), n.getReceiverId(), n.getNotificationType(),
                titleOf(n.getNotificationType()), n.getContent(), Boolean.TRUE.equals(n.getIsRead()),
                n.getCreatedAt(), linkOf(n));
    }

    private String titleOf(String type) {
        return switch (type) {
            case "verification" -> "学生认证";
            case "audit" -> "物品审核";
            case "bounty_match" -> "悬赏匹配";
            case "exchange_invite" -> "置换邀请";
            case "aftersale_result" -> "售后结果";
            case "arbitration_accepted" -> "仲裁受理";
            case "arbitration_result" -> "仲裁结果";
            case "order_completed" -> "交易完成";
            default -> "系统通知";
        };
    }

    /** 数据库 notification_type 只有 8 个枚举值，这里把业务别名归一化。 */
    static String normalizeType(String type) {
        if (type == null || type.isBlank()) {
            return "order_completed";
        }
        return switch (type) {
            case "verification", "audit", "bounty_match", "exchange_invite", "aftersale_result",
                 "arbitration_accepted", "arbitration_result", "order_completed" -> type;
            case "bounty" -> "bounty_match";
            case "exchange" -> "exchange_invite";
            case "aftersale" -> "aftersale_result";
            case "arbitration", "credit" -> "arbitration_result";
            default -> "order_completed";
        };
    }

    private String linkOf(Notification n) {
        String related = n.getRelatedId();
        if (related == null) {
            return null;
        }
        return switch (n.getNotificationType()) {
            case "audit" -> "/item/" + related;
            case "order_completed", "aftersale_result" -> "/orders/" + related;
            default -> null;
        };
    }
}
