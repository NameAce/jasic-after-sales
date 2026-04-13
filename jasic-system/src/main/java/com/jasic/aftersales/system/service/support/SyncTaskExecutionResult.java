package com.jasic.aftersales.system.service.support;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 同步任务执行结果。
 *
 * <p>该对象用于统一沉淀处理器输出，便于日志中心记录本次同步覆盖的数据范围和摘要信息。</p>
 *
 * @author Codex
 * @date 2026/04/12
 */
@Data
@Builder
public class SyncTaskExecutionResult {

    /** 本次同步实际处理的数据开始时间。 */
    private LocalDateTime dataStartTime;

    /** 本次同步实际处理的数据结束时间。 */
    private LocalDateTime dataEndTime;

    /** 面向日志和页面展示的执行结果说明。 */
    private String message;
}
