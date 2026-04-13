package com.jasic.aftersales.system.service.support;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 同步任务执行上下文。
 *
 * <p>上下文由任务中心统一构造，再传入具体处理器，避免每个处理器自行查询执行历史。</p>
 *
 * @author Codex
 * @date 2026/04/12
 */
@Data
@Builder
public class SyncTaskExecutionContext {

    /** 当前这次执行的触发时间，可作为本次数据窗口上界。 */
    private LocalDateTime executionTime;

    /** 最近一次成功执行结束时间，增量任务通常以此推导下次窗口起点。 */
    private LocalDateTime lastSuccessEndTime;
}
