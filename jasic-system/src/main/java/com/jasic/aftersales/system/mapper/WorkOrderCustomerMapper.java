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
 * @author Zoro
 * @date 2026/03/26
 */
@Mapper
public interface WorkOrderCustomerMapper {

    /**
     * 根据客户主键查询客户基础信息。
     *
     * @param id 客户ID
     * @return 客户信息
     */
    @Select("SELECT id, openid, phone, nickname, avatar, status, last_login_time AS lastLoginTime, " +
            "create_time AS createTime, update_time AS updateTime FROM c_user WHERE id = #{id}")
    WorkOrderCustomer selectById(@Param("id") Long id);

    /**
     * 根据手机号查询客户集合，按主键升序返回以便稳定展示。
     *
     * @param phone 手机号
     * @return 客户信息集合
     */
    @Select("SELECT id, openid, phone, nickname, avatar, status, last_login_time AS lastLoginTime, " +
            "create_time AS createTime, update_time AS updateTime FROM c_user WHERE phone = #{phone} ORDER BY id ASC")
    List<WorkOrderCustomer> selectByPhone(@Param("phone") String phone);

    /**
     * 按客户主键更新昵称并刷新更新时间。
     *
     * @param id 客户ID
     * @param nickname 客户昵称
     * @return 受影响行数
     */
    @Update("UPDATE c_user SET nickname = #{nickname}, update_time = NOW() WHERE id = #{id}")
    int updateNicknameById(@Param("id") Long id, @Param("nickname") String nickname);
}
