package com.jasic.aftersales.system.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 微信绑定记录实体
 *
 * @author Codex
 * @date 2026/04/10
 */
@Data
@TableName("wechat_bind_record")
public class WechatBindRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 操作类型 */
    private String operateType;

    /** 操作来源 */
    private String operateSource;

    /** 微信openid快照 */
    private String openid;

    /** 微信手机号快照 */
    private String wechatPhone;

    /** 操作人ID */
    private Long operatorUserId;

    /** 操作人用户名 */
    private String operatorUsername;

    /** 操作时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime operateTime;
}
