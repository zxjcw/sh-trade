package com.shiguangji.common.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shiguangji.common.entity.AppUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;

@Mapper
public interface AppUserMapper extends BaseMapper<AppUser> {

    @Select("SELECT * FROM app_user WHERE user_id = #{id} FOR UPDATE")
    AppUser lockById(@Param("id") Long id);

    @Update("UPDATE app_user SET balance = balance + #{delta} WHERE user_id = #{id}")
    int addBalance(@Param("id") Long id, @Param("delta") BigDecimal delta);

    @Update("UPDATE app_user SET balance = balance - #{amount} WHERE user_id = #{id} AND balance >= #{amount}")
    int deductBalance(@Param("id") Long id, @Param("amount") BigDecimal amount);

    @Update("UPDATE app_user SET credit_score = credit_score + #{delta} WHERE user_id = #{id}")
    int addCredit(@Param("id") Long id, @Param("delta") int delta);
}
