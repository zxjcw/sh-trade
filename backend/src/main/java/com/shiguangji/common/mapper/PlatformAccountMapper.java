package com.shiguangji.common.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shiguangji.common.entity.PlatformAccount;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;

@Mapper
public interface PlatformAccountMapper extends BaseMapper<PlatformAccount> {

    @Select("SELECT * FROM platform_account WHERE account_no = 1 FOR UPDATE")
    PlatformAccount lock();

    @Update("UPDATE platform_account SET escrow_balance = escrow_balance + #{amount}, total_inflow = total_inflow + #{amount}, updated_at = NOW() WHERE account_no = 1")
    int inflow(@Param("amount") BigDecimal amount);

    @Update("UPDATE platform_account SET escrow_balance = escrow_balance - #{amount}, total_outflow = total_outflow + #{amount}, updated_at = NOW() WHERE account_no = 1")
    int outflow(@Param("amount") BigDecimal amount);
}
