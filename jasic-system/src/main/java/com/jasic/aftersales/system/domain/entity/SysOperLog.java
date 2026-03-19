package com.jasic.aftersales.system.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.jasic.aftersales.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 操作日志实体
 *
 * @author Zoro
 * @date 2026/03/18
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_oper_log")
public class SysOperLog extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 操作模块 */
    private String title;

    /** 操作类型（0=其他，1=新增，2=修改，3=删除，4=授权，5=导出，6=登录，7=登出，8=强制下线） */
    private Integer operType;

    /** 请求方法（类名.方法名） */
    private String method;

    /** 请求方式（GET/POST/PUT/DELETE） */
    private String requestMethod;

    /** 请求URL */
    private String requestUrl;

    /** 请求参数 */
    private String requestParam;

    /** 返回结果 */
    private String responseResult;

    /** 操作人ID */
    private Long userId;

    /** 操作人用户名 */
    private String username;

    /** 操作人当前公司ID */
    private Long companyId;

    /** 操作IP */
    private String ip;

    /** 操作状态（1=成功，0=失败） */
    private Integer status;

    /** 错误信息 */
    private String errorMsg;

    /** 操作时间 */
    private LocalDateTime operTime;

    /** 耗时（毫秒） */
    private Long costTime;
}
