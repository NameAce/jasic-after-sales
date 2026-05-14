package com.jasic.aftersales.system.mapper;

import com.jasic.aftersales.system.domain.entity.WorkOrderCustomer;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 工单客户 Mapper
 *
 * @author Codex
 * @date 2026/03/26
 */
@Mapper
public interface WorkOrderCustomerMapper {

    @Select("SELECT id, openid, phone, nickname, avatar, status, last_login_time AS lastLoginTime, " +
            "create_time AS createTime, update_time AS updateTime FROM c_user WHERE id = #{id}")
    WorkOrderCustomer selectById(@Param("id") Long id);

    @Select("SELECT id, openid, phone, nickname, avatar, status, last_login_time AS lastLoginTime, " +
            "create_time AS createTime, update_time AS updateTime FROM c_user WHERE phone = #{phone} ORDER BY id ASC")
    List<WorkOrderCustomer> selectByPhone(@Param("phone") String phone);

    @Update("UPDATE c_user SET nickname = #{nickname}, update_time = NOW() WHERE id = #{id}")
    int updateNicknameById(@Param("id") Long id, @Param("nickname") String nickname);
}
