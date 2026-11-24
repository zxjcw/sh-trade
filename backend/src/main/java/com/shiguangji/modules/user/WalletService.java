package com.shiguangji.modules.user;

import com.shiguangji.common.config.AppProperties;
import com.shiguangji.common.dto.Dtos.BalanceDto;
import com.shiguangji.common.dto.Dtos.PaymentRecordDto;
import com.shiguangji.common.dto.Dtos.RechargeResult;
import com.shiguangji.common.entity.AppUser;
import com.shiguangji.common.entity.PaymentRecord;
import com.shiguangji.common.exception.BizException;
import com.shiguangji.common.mapper.AppUserMapper;
import com.shiguangji.common.mapper.PaymentRecordMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WalletService {

    private final AppUserMapper userMapper;
    private final PaymentRecordMapper paymentRecordMapper;
    private final UserService userService;
    private final AppProperties properties;

    public BalanceDto balance(Long userId) {
        return new BalanceDto(userService.get(userId).getBalance());
    }

    @Transactional
    public RechargeResult recharge(Long userId, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw BizException.badRequest("充值金额必须大于 0");
        }
        BigDecimal max = properties.limits().rechargeMax() == null
                ? null : BigDecimal.valueOf(properties.limits().rechargeMax());
        if (max != null && amount.compareTo(max) > 0) {
            throw BizException.badRequest("单次充值不能超过 " + max.stripTrailingZeros().toPlainString() + " 元");
        }
        AppUser user = userMapper.lockById(userId);
        if (user == null) {
            throw BizException.notFound("用户不存在");
        }
        userMapper.addBalance(userId, amount);
        BigDecimal after = user.getBalance().add(amount);
        user.setBalance(after);
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);

        PaymentRecord record = new PaymentRecord();
        record.setOrderNo(null);
        record.setAccountType("user");
        record.setUserId(userId);
        record.setAccountIdentifier(userId);
        record.setPaymentType("recharge");
        record.setAmount(amount);
        record.setBalanceSnapshot(after);
        record.setStatus("success");
        record.setPaymentMethod("wallet");
        record.setExternalNo("RC" + UUID.randomUUID().toString().replace("-", "").substring(0, 18).toUpperCase());
        record.setRemark("钱包充值");
        record.setCreatedAt(LocalDateTime.now());
        paymentRecordMapper.insert(record);
        return new RechargeResult(after, toDto(record));
    }

    public PaymentRecordDto toDto(PaymentRecord r) {
        return new PaymentRecordDto(r.getPaymentId(), r.getOrderNo(), r.getAccountType(), r.getPaymentType(),
                r.getAmount(), r.getBalanceSnapshot(), r.getExternalNo(), r.getCreatedAt(), r.getRemark());
    }
}
