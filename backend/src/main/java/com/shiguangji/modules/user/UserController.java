package com.shiguangji.modules.user;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shiguangji.common.api.Result;
import com.shiguangji.common.dto.Dtos.BalanceDto;
import com.shiguangji.common.dto.Dtos.CreditRecordDto;
import com.shiguangji.common.dto.Dtos.PaymentRecordDto;
import com.shiguangji.common.dto.Dtos.RechargeResult;
import com.shiguangji.common.dto.Dtos.ReviewDto;
import com.shiguangji.common.dto.Dtos.UserDto;
import com.shiguangji.common.entity.CreditRecord;
import com.shiguangji.common.entity.PaymentRecord;
import com.shiguangji.common.mapper.CreditRecordMapper;
import com.shiguangji.common.mapper.PaymentRecordMapper;
import com.shiguangji.common.security.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class UserController {

    private final WalletService walletService;
    private final VerificationService verificationService;
    private final ReviewQueryService reviewQueryService;
    private final PaymentRecordMapper paymentRecordMapper;
    private final CreditRecordMapper creditRecordMapper;

    public record RechargeRequest(@DecimalMin(value = "0.01", message = "充值金额必须大于 0") BigDecimal amount) {
    }

    public record VerificationRequest(@NotBlank String studentNo, @NotBlank String name,
                                      String college, String major, String grade) {
    }

    @GetMapping("/wallet")
    public Result<BalanceDto> wallet() {
        return Result.ok(walletService.balance(CurrentUser.id()));
    }

    @PostMapping("/wallet/recharge")
    public Result<RechargeResult> recharge(@Valid @RequestBody RechargeRequest request) {
        return Result.ok(walletService.recharge(CurrentUser.id(), request.amount()));
    }

    @GetMapping("/payments")
    public Result<List<PaymentRecordDto>> payments() {
        Long userId = CurrentUser.id();
        return Result.ok(paymentRecordMapper.selectList(new LambdaQueryWrapper<PaymentRecord>()
                        .eq(PaymentRecord::getUserId, userId)
                        .orderByDesc(PaymentRecord::getCreatedAt)
                        .orderByDesc(PaymentRecord::getPaymentId))
                .stream().map(walletService::toDto).toList());
    }

    @GetMapping("/credit/records")
    public Result<List<CreditRecordDto>> creditRecords() {
        Long userId = CurrentUser.id();
        List<CreditRecordDto> records = creditRecordMapper.selectList(new LambdaQueryWrapper<CreditRecord>()
                        .eq(CreditRecord::getUserId, userId)
                        .orderByDesc(CreditRecord::getCreatedAt)
                        .orderByDesc(CreditRecord::getCreditId))
                .stream()
                .map(r -> new CreditRecordDto(r.getCreditId(), r.getChangeValue(), r.getReason(),
                        r.getOrderNo(), r.getCreatedAt()))
                .toList();
        return Result.ok(records);
    }

    @GetMapping("/reviews/me")
    public Result<List<ReviewDto>> reviews() {
        return Result.ok(reviewQueryService.mine(CurrentUser.id()));
    }

    @PostMapping("/verification")
    public Result<UserDto> verify(@Valid @RequestBody VerificationRequest request) {
        return Result.ok(verificationService.submit(request.studentNo(), request.name(),
                request.college(), request.major(), request.grade()));
    }
}
