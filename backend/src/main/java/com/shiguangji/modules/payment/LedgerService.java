package com.shiguangji.modules.payment;

import com.shiguangji.common.entity.AppUser;
import com.shiguangji.common.entity.PaymentRecord;
import com.shiguangji.common.exception.BizException;
import com.shiguangji.common.mapper.AppUserMapper;
import com.shiguangji.common.mapper.PaymentRecordMapper;
import com.shiguangji.common.mapper.PlatformAccountMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 资金账本：买家/卖家钱包与平台托管账户之间的所有资金移动都走这里。
 * 每个动作写两条流水（用户侧 + 平台侧），保证可对账。
 */
@Service
@RequiredArgsConstructor
public class LedgerService {

    /** platform_account 表里平台账户的主键，固定为 1。 */
    private static final long PLATFORM_ACCOUNT = 1L;

    /**
     * 平台流水的账户标识。payment_record 的唯一键是
     * (order_no, payment_type, account_identifier)，不含 account_type，
     * 平台账户号 1 会与 user_id=1 撞号，所以平台侧统一用 0 作为哨兵值
     * （用户主键从 1 开始）。该列仅用于对账，不参与关联查询。
     */
    private static final long PLATFORM_LEDGER_ID = 0L;

    private final AppUserMapper userMapper;
    private final PlatformAccountMapper platformMapper;
    private final PaymentRecordMapper recordMapper;

    /** 买家付款：买家钱包 → 平台托管。 */
    public void buyerPay(String orderNo, Long buyerId, BigDecimal amount) {
        AppUser buyer = userMapper.lockById(buyerId);
        if (buyer == null) {
            throw BizException.notFound("买家不存在");
        }
        if (buyer.getBalance().compareTo(amount) < 0) {
            throw BizException.badRequest("余额不足，请先充值");
        }
        userMapper.deductBalance(buyerId, amount);
        BigDecimal after = buyer.getBalance().subtract(amount);
        write(orderNo, "user", buyerId, "payment", amount, after, "买家支付，资金进入平台托管");
        platformMapper.inflow(amount);
        write(orderNo, "platform", PLATFORM_LEDGER_ID, "payment", amount, escrow(), "托管收款");
    }

    /** 确认收货：平台托管 → 卖家钱包。 */
    public void sellerPayout(String orderNo, Long sellerId, BigDecimal amount) {
        if (platformMapper.outflow(amount) == 0) {
            throw BizException.conflict("托管账户余额不足，无法放款");
        }
        userMapper.addBalance(sellerId, amount);
        AppUser seller = userMapper.selectById(sellerId);
        write(orderNo, "platform", PLATFORM_LEDGER_ID, "payout", amount, escrow(), "托管放款");
        write(orderNo, "user", sellerId, "payout", amount, seller.getBalance(), "交易完成，货款到账");
    }

    /** 退款：平台托管 → 买家钱包。 */
    public void refundToBuyer(String orderNo, Long buyerId, BigDecimal amount) {
        if (platformMapper.outflow(amount) == 0) {
            throw BizException.conflict("托管账户余额不足，无法退款");
        }
        userMapper.addBalance(buyerId, amount);
        AppUser buyer = userMapper.selectById(buyerId);
        write(orderNo, "platform", PLATFORM_LEDGER_ID, "refund", amount, escrow(), "托管退款");
        write(orderNo, "user", buyerId, "refund", amount, buyer.getBalance(), "订单退款到账");
    }

    /** 仲裁扣回：卖家钱包 → 平台托管（再退给买家时走 refundToBuyer）。 */
    public void chargebackSeller(String orderNo, Long sellerId, BigDecimal amount) {
        AppUser seller = userMapper.lockById(sellerId);
        if (seller == null) {
            throw BizException.notFound("卖家不存在");
        }
        if (seller.getBalance().compareTo(amount) < 0) {
            throw BizException.badRequest("卖家余额不足，无法扣回");
        }
        userMapper.deductBalance(sellerId, amount);
        BigDecimal after = seller.getBalance().subtract(amount);
        write(orderNo, "user", sellerId, "chargeback", amount, after, "仲裁扣回");
        platformMapper.inflow(amount);
        write(orderNo, "platform", PLATFORM_LEDGER_ID, "chargeback", amount, escrow(), "仲裁扣回入托管");
    }

    /** 查询当前托管余额（写平台流水时作为余额快照）。 */
    private BigDecimal escrow() {
        var account = platformMapper.selectById(PLATFORM_ACCOUNT);
        return account == null ? BigDecimal.ZERO : account.getEscrowBalance();
    }

    private void write(String orderNo, String accountType, Long accountIdentifier, String type,
                       BigDecimal amount, BigDecimal snapshot, String remark) {
        PaymentRecord record = new PaymentRecord();
        record.setOrderNo(orderNo);
        record.setAccountType(accountType);
        record.setUserId("user".equals(accountType) ? accountIdentifier : null);
        record.setAccountIdentifier(accountIdentifier);
        record.setPaymentType(type);
        record.setAmount(amount);
        record.setBalanceSnapshot(snapshot);
        record.setStatus("success");
        record.setPaymentMethod("wallet");
        record.setExternalNo("PM" + UUID.randomUUID().toString().replace("-", "").substring(0, 20).toUpperCase());
        record.setRemark(remark);
        record.setCreatedAt(LocalDateTime.now());
        recordMapper.insert(record);
    }
}
