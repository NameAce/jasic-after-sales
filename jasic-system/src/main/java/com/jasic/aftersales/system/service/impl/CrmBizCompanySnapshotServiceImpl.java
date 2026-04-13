package com.jasic.aftersales.system.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jasic.aftersales.common.core.domain.PageResult;
import com.jasic.aftersales.common.exception.ServiceException;
import com.jasic.aftersales.system.domain.entity.CrmBizCompanySnapshot;
import com.jasic.aftersales.system.domain.entity.SysCompany;
import com.jasic.aftersales.system.domain.query.CrmBizCompanySnapshotQuery;
import com.jasic.aftersales.system.domain.vo.CrmBizCompanyImportPreviewVO;
import com.jasic.aftersales.system.domain.vo.CrmBizCompanySnapshotVO;
import com.jasic.aftersales.system.domain.vo.CrmBizCompanySyncSummaryVO;
import com.jasic.aftersales.system.mapper.CrmBizCompanySnapshotMapper;
import com.jasic.aftersales.system.mapper.SysCompanyMapper;
import com.jasic.aftersales.system.service.ICrmBizCompanySnapshotService;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * CRM 公司快照服务实现。
 *
 * <p>实现策略遵循当前业务约定：</p>
 * <ul>
 *     <li>外部 {@code biz_company} 先全量沉淀到本地快照层；</li>
 *     <li>本地公司新增时仅从快照读取预填信息，不直接把外部数据写入 {@code sys_company}；</li>
 *     <li>本地是否已存在公司，统一通过 {@code sys_company.company_code = cust_id} 判断。</li>
 * </ul>
 *
 * @author Codex
 * @date 2026/04/12
 */
@Service
public class CrmBizCompanySnapshotServiceImpl implements ICrmBizCompanySnapshotService {

    private static final String CRM_BIZ_COMPANY_TABLE = "biz_company";
    private static final int DEFAULT_BATCH_SIZE = 500;

    @Resource(name = "jdbcTemplate")
    private JdbcTemplate jdbcTemplate;

    @Resource(name = "crmJdbcTemplate")
    private JdbcTemplate crmJdbcTemplate;

    @Resource
    private CrmBizCompanySnapshotMapper crmBizCompanySnapshotMapper;

    @Resource
    private SysCompanyMapper sysCompanyMapper;

    @Override
    public PageResult<CrmBizCompanySnapshotVO> listPage(CrmBizCompanySnapshotQuery query) {
        Page<CrmBizCompanySnapshot> page = new Page<>(query.getPageNum(), query.getPageSize());
        LambdaQueryWrapper<CrmBizCompanySnapshot> wrapper = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(query.getCompanyCode())) {
            // 客户编码在 CRM 中是数字主键；若输入非数字，直接构造空结果条件，避免无效全表扫描。
            if (StrUtil.isNumeric(query.getCompanyCode().trim())) {
                wrapper.eq(CrmBizCompanySnapshot::getCustId, Long.valueOf(query.getCompanyCode().trim()));
            } else {
                wrapper.eq(CrmBizCompanySnapshot::getCustId, -1L);
            }
        }
        if (StrUtil.isNotBlank(query.getCompanyName())) {
            wrapper.like(CrmBizCompanySnapshot::getCustName, query.getCompanyName().trim());
        }
        if (query.getCustState() != null) {
            wrapper.eq(CrmBizCompanySnapshot::getCustState, query.getCustState());
        }
        wrapper.orderByDesc(CrmBizCompanySnapshot::getOperTime)
                .orderByDesc(CrmBizCompanySnapshot::getAddDate)
                .orderByDesc(CrmBizCompanySnapshot::getCustId);
        Page<CrmBizCompanySnapshot> result = crmBizCompanySnapshotMapper.selectPage(page, wrapper);
        return PageResult.of(buildSnapshotVOList(result.getRecords()), result.getTotal(), query.getPageNum(), query.getPageSize());
    }

    @Override
    public CrmBizCompanyImportPreviewVO getImportPreview(Long custId) {
        if (custId == null) {
            throw new ServiceException("CRM 客户ID不能为空");
        }
        LambdaQueryWrapper<CrmBizCompanySnapshot> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CrmBizCompanySnapshot::getCustId, custId).last("LIMIT 1");
        CrmBizCompanySnapshot snapshot = crmBizCompanySnapshotMapper.selectOne(wrapper);
        if (snapshot == null) {
            throw new ServiceException("CRM 公司快照不存在，请先执行同步任务");
        }

        CrmBizCompanyImportPreviewVO vo = new CrmBizCompanyImportPreviewVO();
        vo.setCustId(snapshot.getCustId());
        vo.setCompanyName(snapshot.getCustName());
        vo.setCompanyCode(String.valueOf(snapshot.getCustId()));
        vo.setContactName(snapshot.getJuristicCustId());
        vo.setContactPhone(resolveContactPhone(snapshot));
        vo.setAddress(snapshot.getCompanyAddress());
        vo.setStatus(resolveLocalStatus(snapshot.getCustState()));
        vo.setCustState(snapshot.getCustState());
        vo.setCustStateLabel(resolveCustStateLabel(snapshot.getCustState()));

        // 导入前先判断本地是否已有同编码公司，避免重复建档。
        SysCompany existingCompany = findExistingCompanyByCode(vo.getCompanyCode());
        if (existingCompany != null) {
            vo.setExistingCompanyId(existingCompany.getId());
            vo.setExistingCompanyName(existingCompany.getCompanyName());
        }
        return vo;
    }

    @Override
    public LocalDateTime getEarliestChangeTime() {
        JdbcTemplate crm = requireCrmJdbcTemplate();
        // 首次同步既要覆盖历史新增，也要覆盖历史修改，因此同时取 add_date 和 oper_time 的最小值。
        String sql = "SELECT MIN(t.change_time) FROM ("
                + "SELECT add_date AS change_time FROM " + CRM_BIZ_COMPANY_TABLE + " WHERE add_date IS NOT NULL "
                + "UNION ALL "
                + "SELECT oper_time AS change_time FROM " + CRM_BIZ_COMPANY_TABLE + " WHERE oper_time IS NOT NULL"
                + ") t";
        Timestamp timestamp = crm.queryForObject(sql, Timestamp.class);
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    @Override
    public CrmBizCompanySyncSummaryVO syncByTimeRange(LocalDateTime startInclusive, LocalDateTime endExclusive) {
        if (startInclusive == null || endExclusive == null || !startInclusive.isBefore(endExclusive)) {
            throw new ServiceException("CRM 公司同步时间范围不合法");
        }
        JdbcTemplate crm = requireCrmJdbcTemplate();
        // 按已确认口径：oper_time 命中时间窗，或 add_date 命中时间窗，任一满足即纳入同步。
        String sql = "SELECT cust_id, cust_name, juristic_cust_id, group_contact_phone, cellphone, "
                + "company_address, cust_state, add_date, oper_time "
                + "FROM " + CRM_BIZ_COMPANY_TABLE + " "
                + "WHERE (oper_time >= ? AND oper_time < ?) "
                + "OR (add_date >= ? AND add_date < ?) "
                + "ORDER BY cust_id ASC";

        List<CrmBizCompanySnapshot> batch = new ArrayList<>(DEFAULT_BATCH_SIZE);
        SyncCounter counter = new SyncCounter();
        LocalDateTime syncTime = LocalDateTime.now();
        crm.query(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
            ps.setFetchSize(Integer.MIN_VALUE);
            ps.setTimestamp(1, toTimestamp(startInclusive));
            ps.setTimestamp(2, toTimestamp(endExclusive));
            ps.setTimestamp(3, toTimestamp(startInclusive));
            ps.setTimestamp(4, toTimestamp(endExclusive));
            return ps;
        }, rs -> {
            while (rs.next()) {
                CrmBizCompanySnapshot row = CRM_BIZ_COMPANY_ROW_MAPPER.mapRow(rs, rs.getRow());
                if (row == null || row.getCustId() == null) {
                    continue;
                }
                batch.add(row);
                counter.processedCount++;
                if (batch.size() >= DEFAULT_BATCH_SIZE) {
                    flushBatch(batch, syncTime, counter);
                    batch.clear();
                }
            }
            return null;
        });

        if (CollUtil.isNotEmpty(batch)) {
            flushBatch(batch, syncTime, counter);
        }

        CrmBizCompanySyncSummaryVO summary = new CrmBizCompanySyncSummaryVO();
        summary.setDataStartTime(startInclusive);
        summary.setDataEndTime(endExclusive);
        summary.setProcessedCount(counter.processedCount);
        summary.setInsertedCount(counter.insertedCount);
        summary.setUpdatedCount(counter.updatedCount);
        return summary;
    }

    private void flushBatch(List<CrmBizCompanySnapshot> rows, LocalDateTime syncTime, SyncCounter counter) {
        List<Long> custIds = rows.stream()
                .map(CrmBizCompanySnapshot::getCustId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        Set<Long> existingCustIds = Collections.emptySet();
        if (CollUtil.isNotEmpty(custIds)) {
            // 先识别本批次内哪些 cust_id 已存在，便于统计新增与更新数量。
            LambdaQueryWrapper<CrmBizCompanySnapshot> wrapper = new LambdaQueryWrapper<>();
            wrapper.in(CrmBizCompanySnapshot::getCustId, custIds);
            existingCustIds = crmBizCompanySnapshotMapper.selectList(wrapper).stream()
                    .map(CrmBizCompanySnapshot::getCustId)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
        }

        // 快照层采用幂等 upsert，保证每日增量同步可重复执行而不产生重复数据。
        String sql = "INSERT INTO crm_biz_company_snapshot ("
                + "cust_id, cust_name, juristic_cust_id, group_contact_phone, cellphone, company_address, "
                + "cust_state, add_date, oper_time, last_sync_time, create_time, update_time"
                + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW()) "
                + "ON DUPLICATE KEY UPDATE "
                + "cust_name = VALUES(cust_name), "
                + "juristic_cust_id = VALUES(juristic_cust_id), "
                + "group_contact_phone = VALUES(group_contact_phone), "
                + "cellphone = VALUES(cellphone), "
                + "company_address = VALUES(company_address), "
                + "cust_state = VALUES(cust_state), "
                + "add_date = VALUES(add_date), "
                + "oper_time = VALUES(oper_time), "
                + "last_sync_time = VALUES(last_sync_time), "
                + "update_time = NOW()";
        jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                CrmBizCompanySnapshot row = rows.get(i);
                ps.setObject(1, row.getCustId());
                ps.setString(2, row.getCustName());
                ps.setString(3, row.getJuristicCustId());
                ps.setString(4, row.getGroupContactPhone());
                ps.setString(5, row.getCellphone());
                ps.setString(6, row.getCompanyAddress());
                ps.setObject(7, row.getCustState());
                ps.setTimestamp(8, toTimestamp(row.getAddDate()));
                ps.setTimestamp(9, toTimestamp(row.getOperTime()));
                ps.setTimestamp(10, toTimestamp(syncTime));
            }

            @Override
            public int getBatchSize() {
                return rows.size();
            }
        });

        for (CrmBizCompanySnapshot row : rows) {
            if (existingCustIds.contains(row.getCustId())) {
                counter.updatedCount++;
            } else {
                counter.insertedCount++;
            }
        }
    }

    private List<CrmBizCompanySnapshotVO> buildSnapshotVOList(List<CrmBizCompanySnapshot> records) {
        if (CollUtil.isEmpty(records)) {
            return Collections.emptyList();
        }
        List<String> companyCodes = records.stream()
                .map(item -> item.getCustId() == null ? null : String.valueOf(item.getCustId()))
                .filter(StrUtil::isNotBlank)
                .distinct()
                .collect(Collectors.toList());
        Map<String, SysCompany> existingCompanyMap = new HashMap<>();
        if (CollUtil.isNotEmpty(companyCodes)) {
            // 快照展示页需要直接看到“已存在/可导入”，因此这里一次性回表补齐本地存在性信息。
            LambdaQueryWrapper<SysCompany> wrapper = new LambdaQueryWrapper<>();
            wrapper.in(SysCompany::getCompanyCode, companyCodes);
            existingCompanyMap = sysCompanyMapper.selectList(wrapper).stream()
                    .collect(Collectors.toMap(SysCompany::getCompanyCode, item -> item, (a, b) -> a));
        }

        List<CrmBizCompanySnapshotVO> result = new ArrayList<>(records.size());
        for (CrmBizCompanySnapshot record : records) {
            CrmBizCompanySnapshotVO vo = new CrmBizCompanySnapshotVO();
            vo.setCustId(record.getCustId());
            vo.setCompanyCode(record.getCustId() == null ? null : String.valueOf(record.getCustId()));
            vo.setCompanyName(record.getCustName());
            vo.setContactName(record.getJuristicCustId());
            vo.setContactPhone(resolveContactPhone(record));
            vo.setAddress(record.getCompanyAddress());
            vo.setCustState(record.getCustState());
            vo.setCustStateLabel(resolveCustStateLabel(record.getCustState()));
            vo.setAddDate(record.getAddDate());
            vo.setOperTime(record.getOperTime());
            vo.setLastSyncTime(record.getLastSyncTime());
            SysCompany existingCompany = existingCompanyMap.get(vo.getCompanyCode());
            if (existingCompany != null) {
                vo.setExistingCompanyId(existingCompany.getId());
                vo.setExistingCompanyName(existingCompany.getCompanyName());
            }
            result.add(vo);
        }
        return result;
    }

    private SysCompany findExistingCompanyByCode(String companyCode) {
        if (StrUtil.isBlank(companyCode)) {
            return null;
        }
        LambdaQueryWrapper<SysCompany> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysCompany::getCompanyCode, companyCode).last("LIMIT 1");
        return sysCompanyMapper.selectOne(wrapper);
    }

    private JdbcTemplate requireCrmJdbcTemplate() {
        if (crmJdbcTemplate == null) {
            throw new ServiceException("当前未配置客户关系管理（CRM）数据源，请先完善 jasic.crm.datasource");
        }
        return crmJdbcTemplate;
    }

    private String resolveContactPhone(CrmBizCompanySnapshot snapshot) {
        return StrUtil.blankToDefault(StrUtil.trim(snapshot.getGroupContactPhone()), StrUtil.trim(snapshot.getCellphone()));
    }

    private Integer resolveLocalStatus(Integer custState) {
        // 当前导入策略只区分启用/停用：CRM 审核通过映射启用，其余状态统一按停用预置。
        return Objects.equals(custState, 1) ? 1 : 0;
    }

    private String resolveCustStateLabel(Integer custState) {
        if (custState == null) {
            return "未知";
        }
        switch (custState) {
            case 0:
                return "待审核";
            case 1:
                return "审核通过";
            case 2:
                return "审核不通过";
            case 3:
                return "注销";
            case 4:
                return "资料已保存";
            case 5:
                return "申请注销";
            case 6:
                return "资料未填写";
            case 9:
                return "删除";
            default:
                return "状态" + custState;
        }
    }

    private Timestamp toTimestamp(LocalDateTime value) {
        return value == null ? null : Timestamp.valueOf(value);
    }

    private static LocalDateTime toLocalDateTime(ResultSet rs, String column) throws SQLException {
        Timestamp value = rs.getTimestamp(column);
        return value == null ? null : value.toLocalDateTime();
    }

    private static final RowMapper<CrmBizCompanySnapshot> CRM_BIZ_COMPANY_ROW_MAPPER = (rs, rowNum) -> {
        CrmBizCompanySnapshot snapshot = new CrmBizCompanySnapshot();
        long custId = rs.getLong("cust_id");
        snapshot.setCustId(rs.wasNull() ? null : custId);
        snapshot.setCustName(StrUtil.trim(rs.getString("cust_name")));
        snapshot.setJuristicCustId(StrUtil.trim(rs.getString("juristic_cust_id")));
        snapshot.setGroupContactPhone(StrUtil.trim(rs.getString("group_contact_phone")));
        snapshot.setCellphone(StrUtil.trim(rs.getString("cellphone")));
        snapshot.setCompanyAddress(StrUtil.trim(rs.getString("company_address")));
        int custState = rs.getInt("cust_state");
        snapshot.setCustState(rs.wasNull() ? null : custState);
        snapshot.setAddDate(toLocalDateTime(rs, "add_date"));
        snapshot.setOperTime(toLocalDateTime(rs, "oper_time"));
        return snapshot;
    };

    private static class SyncCounter {
        private int processedCount;
        private int insertedCount;
        private int updatedCount;
    }
}
