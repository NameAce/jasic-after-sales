package com.jasic.aftersales.system.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.jasic.aftersales.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * B端员工实体
 *
 * @author Zoro
 * @date 2026/03/18
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user")
public class SysUser extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 登录用户名 */
    private String username;

    /** 密码（BCrypt加密） */
    private String password;

    /** 真实姓名 */
    private String realName;

    /** 手机号 */
    private String phone;

    /** 邮箱 */
    private String email;

    /** 头像URL */
    private String avatar;

    /** 微信openid（小程序登录绑定） */
    private String openid;

    /** 微信unionid */
    private String unionid;

    /** 性别（0=未知，1=男，2=女） */
    private Integer sex;

    /** 状态（1=正常，0=停用） */
    private Integer status;

    /** 是否删除（逻辑删除） */
    @TableLogic
    private Integer isDeleted;

    /** 备注 */
    private String remark;

    /** 最后登录时间 */
    private LocalDateTime lastLoginTime;
}
