package com.jasic.aftersales.system.service;

import com.jasic.aftersales.common.core.domain.PageResult;
import com.jasic.aftersales.system.domain.query.CrmBizCompanySnapshotQuery;
import com.jasic.aftersales.system.domain.vo.CrmBizCompanyImportPreviewVO;
import com.jasic.aftersales.system.domain.vo.CrmBizCompanySnapshotVO;
import com.jasic.aftersales.system.domain.vo.CrmBizCompanySyncSummaryVO;

import java.time.LocalDateTime;

/**
 * CRM 公司快照服务接口。
 *
 * <p>职责边界如下：</p>
 * <ul>
 *     <li>负责维护 {@code biz_company} 在本地的快照数据；</li>
 *     <li>负责为“从 CRM 导入公司”提供查询和导入预览能力；</li>
 *     <li>不直接新增或覆盖本地 {@code sys_company}，仅提供快照与预览数据。</li>
 * </ul>
 *
 * @author Zoro
 * @date 2026/04/12
 */
public interface ICrmBizCompanySnapshotService {

    /**
     * 分页查询 CRM 公司快照列表。
     *
     * <p>该方法面向前端“从 CRM 导入”弹窗使用，返回的数据会额外标记本地是否已存在同
     * {@code company_code} 的公司，便于前端决定显示“可导入”还是“已存在”。</p>
     *
     * @param query 快照分页查询条件
     * @return 快照分页结果，包含本地存在性信息
     */
    PageResult<CrmBizCompanySnapshotVO> listPage(CrmBizCompanySnapshotQuery query);

    /**
     * 查询 CRM 公司导入预览信息。
     *
     * <p>返回值用于前端带入新增公司表单。如果本地已存在相同客户编码的公司，则只返回已存在
     * 标记和本地公司信息，由前端跳转到编辑页处理。</p>
     *
     * @param custId CRM 客户ID
     * @return 导入预览对象
     */
    CrmBizCompanyImportPreviewVO getImportPreview(Long custId);

    /**
     * 查询源表可用于增量同步的最早变更时间。
     *
     * <p>按照已确认口径，同时考虑 {@code add_date} 和 {@code oper_time} 两个字段，用于初始化
     * 首次同步的时间窗口。</p>
     *
     * @return 最早变更时间；若源表无有效时间，则返回 {@code null}
     */
    LocalDateTime getEarliestChangeTime();

    /**
     * 按时间窗口同步 CRM 公司快照。
     *
     * <p>同步条件为：</p>
     * <ul>
     *     <li>{@code oper_time} 落在窗口内；或</li>
     *     <li>{@code add_date} 落在窗口内。</li>
     * </ul>
     *
     * @param startInclusive 窗口开始时间，包含边界
     * @param endExclusive   窗口结束时间，不包含边界
     * @return 同步结果摘要
     */
    CrmBizCompanySyncSummaryVO syncByTimeRange(LocalDateTime startInclusive, LocalDateTime endExclusive);
}
