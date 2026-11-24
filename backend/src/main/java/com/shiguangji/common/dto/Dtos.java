package com.shiguangji.common.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 前端契约 DTO（字段名与 frontend/src/types/index.ts 保持一致）。
 */
public final class Dtos {

    private Dtos() {
    }

    public record UserDto(
            Long id, String username, String nickname, String role, String verifyStatus,
            Integer creditScore, BigDecimal balance, String studentNo, String college,
            String major, String grade, String contact, Boolean banned, LocalDateTime createdAt) {
    }

    public record LoginResult(String token, UserDto user) {
    }

    public record CategoryDto(Long id, String name,
                               @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.ALWAYS) Long parentId,
                               Boolean enabled, List<CategoryDto> children) {
    }

    public record PickupPointDto(Long id, String name, String address) {
    }

    public record ItemImageDto(Long id, Integer seed, Boolean isCover, String status, String url) {
    }

    public record HistoryEventDto(Long id, String type, LocalDateTime time, String description,
                                  String orderId, String userName) {
    }

    public record ItemDto(
            Long id, String title, Long categoryId, String categoryName, BigDecimal price,
            BigDecimal originalPrice, BigDecimal purchasePrice, String condition, String description,
            String status, String source, String notesTag, Long sellerId, String sellerName,
            Integer sellerCredit, Integer coverSeed, List<ItemImageDto> images,
            List<HistoryEventDto> history, LocalDateTime createdAt, LocalDateTime updatedAt,
            String rejectReason, Boolean favorited) {
    }

    public record DeliveryInfoDto(LocalDateTime agreeTime, String meetupPlace, LocalDateTime verifyTime,
                                  LocalDateTime inboundTime, String pickupCode, LocalDateTime outboundTime) {
    }

    public record OrderDto(
            String id, String type, Long itemId, String itemTitle, Integer itemCoverSeed,
            String itemCondition, Long buyerId, String buyerName, Long sellerId, String sellerName,
            BigDecimal amount, String deliveryMethod, Long pickupPointId, String pickupPointName,
            String tradeStatus, String afterSaleStatus, LocalDateTime createdAt, LocalDateTime payDeadline,
            LocalDateTime confirmDeadline, LocalDateTime paidAt, LocalDateTime completedAt, Boolean reviewed,
            Long bountyId, Long planId, DeliveryInfoDto delivery) {
    }

    public record ReviewDto(Long id, String orderId, String itemTitle, Long reviewerId, String reviewerName,
                            String targetName, Integer rating, List<String> tags, String content,
                            String status, LocalDateTime createdAt) {
    }

    public record CreditRecordDto(Long id, Integer change, String reason, String orderId, LocalDateTime createdAt) {
    }

    public record PaymentRecordDto(Long id, String orderId, String accountType, String type, BigDecimal amount,
                                   BigDecimal balanceAfter, String externalNo, LocalDateTime createdAt, String remark) {
    }

    public record NotificationDto(Long id, Long receiverId, String type, String title, String content,
                                  Boolean read, LocalDateTime createdAt, String link) {
    }

    public record BountyDto(Long id, Long buyerId, String buyerName, Long categoryId, String categoryName,
                            String keyword, BigDecimal budget, String condition, String status,
                            LocalDateTime createdAt, LocalDateTime expiresAt, Integer matchCount) {
    }

    public record ExchangeIntentDto(Long id, Long userId, String userName, Long myItemId, String myItemTitle,
                                    Integer myItemSeed, String matchType, Long targetItemId, String targetItemTitle,
                                    Long targetCategoryId, String targetCategoryName, String keyword,
                                    String status, LocalDateTime createdAt) {
    }

    public record ExchangeParticipantDto(Long userId, String userName, String itemTitle, Integer itemSeed,
                                         Boolean confirmed) {
    }

    public record ExchangePlanDto(Long id, Integer ringType, List<ExchangeParticipantDto> participants,
                                  String status, LocalDateTime deadline, LocalDateTime createdAt) {
    }

    public record ArbitrationDto(Long id, String orderId, String applicantName, String targetName, String type,
                                 String evidence, String status, String result, String handler,
                                 LocalDateTime createdAt, LocalDateTime resolvedAt) {
    }

    public record CategoryStatDto(String name, Long count, BigDecimal amount) {
    }

    public record MonthlyStatDto(String month, Long count, BigDecimal amount) {
    }

    public record ReportDataDto(Long completedCount, BigDecimal completedAmount, BigDecimal refundRate,
                                Long pendingAudits, Long pendingArbitrations,
                                List<CategoryStatDto> categoryStats, List<MonthlyStatDto> monthlyStats) {
    }

    public record PublicStatsDto(Long verifiedUsers, Long onSaleItems, Long completedOrders, BigDecimal totalAmount) {
    }

    public record RosterEntryDto(String studentNo, String name, String college, String major, String grade) {
    }

    public record BalanceDto(BigDecimal balance) {
    }

    public record RechargeResult(BigDecimal balance, PaymentRecordDto record) {
    }

    public record FavoriteResult(Boolean favorited) {
    }
}
