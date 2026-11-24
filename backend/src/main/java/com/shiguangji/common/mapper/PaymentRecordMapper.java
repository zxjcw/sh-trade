package com.shiguangji.common.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shiguangji.common.entity.PaymentRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface PaymentRecordMapper extends BaseMapper<PaymentRecord> {

    @Select("SELECT COUNT(*) FROM payment_record WHERE order_no = #{orderNo} AND payment_type = #{type} AND account_identifier = #{account}")
    int countByOrderTypeAccount(@Param("orderNo") String orderNo, @Param("type") String type,
                                @Param("account") Long account);
}
