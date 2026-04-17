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
 * @author Codex
 * @date 2026/04/12
 */
@Service
public class CrmBizCompanySnapshotServiceImpl implements ICrmBizCompanySnapshotService {

    private static final String CRM_BIZ_COMPANY_TABLE = "biz_company";
    private static final String CRM_BIZ_USER_TABLE = "biz_user_info";
    private static final String CRM_SAP_COMPANY_TABLE = "sap_company_info";
    private static final String SOURCE_TYPE_CRM = "CRM";
    private static final String TYPE_CODE_SITE_FIRST = "SITE_FIRST";
    private static final String TYPE_CODE_SITE_SECOND = "SITE_SECOND";
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
            wrapper.eq(CrmBizCompanySnapshot::getSapCompanyCode, query.getCompanyCode().trim());
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
            throw new ServiceException("CRM客户ID不能为空");
        }
        LambdaQueryWrapper<CrmBizCompanySnapshot> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CrmBizCompanySnapshot::getCustId, custId).last("LIMIT 1");
        CrmBizCompanySnapshot snapshot = crmBizCompanySnapshotMapper.selectOne(wrapper);
        if (snapshot == null) {
            throw new ServiceException("CRM公司快照不存在，请先执行同步任务");
        }

        SysCompany existingCompany = findExistingCompanyByCode(snapshot.getSapCompanyCode());
        return buildImportPreview(snapshot, existingCompany);
    }

    @Override
    public LocalDateTime getEarliestChangeTime() {
        JdbcTemplate crm = requireCrmJdbcTemplate();
        String sql = "SELECT MIN(t.change_time) FROM ("
                + "SELECT b.add_date AS change_time FROM " + CRM_BIZ_COMPANY_TABLE + " b "
                + "WHERE b.cust_rage IN (0, 3) AND b.add_date IS NOT NULL "
                + "UNION ALL "
                + "SELECT b.oper_time AS change_time FROM " + CRM_BIZ_COMPANY_TABLE + " b "
                + "WHERE b.cust_rage IN (0, 3) AND b.oper_time IS NOT NULL"
                + ") t";
        Timestamp timestamp = crm.queryForObject(sql, Timestamp.class);
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    @Override
    public CrmBizCompanySyncSummaryVO syncByTimeRange(LocalDateTime startInclusive, LocalDateTime endExclusive) {
        if (startInclusive == null || endExclusive == null || !startInclusive.isBefore(endExclusive)) {
            throw new ServiceException("CRM公司同步时间范围不合法");
        }
        JdbcTemplate crm = requireCrmJdbcTemplate();
        String sql = "SELECT b.cust_id, b.cust_name, u.contact_name AS juristic_cust_id, "
                + "u.cellphone AS group_contact_phone, u.cellphone AS cellphone, "
                + "b.company_address, b.cust_state, b.add_date, b.oper_time, b.sap_company_code, b.cust_rage, "
                + "s.sortl AS company_short_name, s.bezei AS province_name, s.sap_city AS city_name, "
                + "NULL AS district_name "
                + "FROM " + CRM_BIZ_COMPANY_TABLE + " b "
                + "LEFT JOIN " + CRM_BIZ_USER_TABLE + " u ON u.user_id = b.account_id "
                + "LEFT JOIN " + CRM_SAP_COMPANY_TABLE + " s ON s.sap_company_id = ("
                + "SELECT s2.sap_company_id FROM " + CRM_SAP_COMPANY_TABLE + " s2 "
                + "WHERE BINARY TRIM(s2.kunnr) = BINARY TRIM(b.sap_company_code) "
                + "ORDER BY COALESCE(s2.oper_time, s2.add_time) DESC, s2.oper_time DESC, "
                + "s2.add_time DESC, s2.sap_company_id DESC LIMIT 1"
                + ") "
                + "WHERE b.cust_rage IN (0, 3) "
                + "AND ((b.oper_time >= ? AND b.oper_time < ?) OR (b.add_date >= ? AND b.add_date < ?)) "
                + "ORDER BY b.cust_id ASC";

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
            LambdaQueryWrapper<CrmBizCompanySnapshot> wrapper = new LambdaQueryWrapper<>();
            wrapper.in(CrmBizCompanySnapshot::getCustId, custIds);
            existingCustIds = crmBizCompanySnapshotMapper.selectList(wrapper).stream()
                    .map(CrmBizCompanySnapshot::getCustId)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
        }

        String sql = "INSERT INTO crm_biz_company_snapshot ("
                + "cust_id, cust_name, juristic_cust_id, group_contact_phone, cellphone, company_address, "
                + "sap_company_code, cust_rage, company_short_name, province_name, city_name, district_name, "
                + "cust_state, add_date, oper_time, last_sync_time, create_time, update_time"
                + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW()) "
                + "ON DUPLICATE KEY UPDATE "
                + "cust_name = VALUES(cust_name), "
                + "juristic_cust_id = VALUES(juristic_cust_id), "
                + "group_contact_phone = VALUES(group_contact_phone), "
                + "cellphone = VALUES(cellphone), "
                + "company_address = VALUES(company_address), "
                + "sap_company_code = VALUES(sap_company_code), "
                + "cust_rage = VALUES(cust_rage), "
                + "company_short_name = VALUES(company_short_name), "
                + "province_name = VALUES(province_name), "
                + "city_name = VALUES(city_name), "
                + "district_name = VALUES(district_name), "
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
                ps.setString(7, row.getSapCompanyCode());
                ps.setObject(8, row.getCustRage());
                ps.setString(9, row.getCompanyShortName());
                ps.setString(10, row.getProvinceName());
                ps.setString(11, row.getCityName());
                ps.setString(12, row.getDistrictName());
                ps.setObject(13, row.getCustState());
                ps.setTimestamp(14, toTimestamp(row.getAddDate()));
                ps.setTimestamp(15, toTimestamp(row.getOperTime()));
                ps.setTimestamp(16, toTimestamp(syncTime));
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
                .map(CrmBizCompanySnapshot::getSapCompanyCode)
                .filter(StrUtil::isNotBlank)
                .distinct()
                .collect(Collectors.toList());
        Map<String, SysCompany> existingCompanyMap = new HashMap<>();
        if (CollUtil.isNotEmpty(companyCodes)) {
            LambdaQueryWrapper<SysCompany> wrapper = new LambdaQueryWrapper<>();
            wrapper.in(SysCompany::getCompanyCode, companyCodes);
            existingCompanyMap = sysCompanyMapper.selectList(wrapper).stream()
                    .collect(Collectors.toMap(SysCompany::getCompanyCode, item -> item, (a, b) -> a));
        }

        List<CrmBizCompanySnapshotVO> result = new ArrayList<>(records.size());
        for (CrmBizCompanySnapshot record : records) {
            CrmBizCompanySnapshotVO vo = new CrmBizCompanySnapshotVO();
            vo.setCustId(record.getCustId());
            vo.setCompanyCode(record.getSapCompanyCode());
            vo.setCompanyShortName(record.getCompanyShortName());
            vo.setCompanyName(record.getCustName());
            vo.setContactName(record.getJuristicCustId());
            vo.setContactPhone(resolveContactPhone(record));
            vo.setAddress(record.getCompanyAddress());
            vo.setProvinceName(record.getProvinceName());
            vo.setCityName(record.getCityName());
            vo.setDistrictName(record.getDistrictName());
            vo.setCustRage(record.getCustRage());
            vo.setTypeCode(resolveTypeCode(record.getCustRage()));
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

            String disabledReason = resolveImportDisabledReason(record, existingCompany);
            vo.setCanImport(disabledReason == null);
            vo.setImportDisabledReason(disabledReason);
            result.add(vo);
        }
        return result;
    }

    private CrmBizCompanyImportPreviewVO buildImportPreview(CrmBizCompanySnapshot snapshot, SysCompany existingCompany) {
        CrmBizCompanyImportPreviewVO vo = new CrmBizCompanyImportPreviewVO();
        vo.setCustId(snapshot.getCustId());
        vo.setCompanyName(snapshot.getCustName());
        vo.setCompanyShortName(snapshot.getCompanyShortName());
        vo.setCompanyCode(snapshot.getSapCompanyCode());
        vo.setAdminUsername(resolveDefaultAdminUsername(snapshot.getSapCompanyCode()));
        vo.setTypeCode(resolveTypeCode(snapshot.getCustRage()));
        vo.setContactName(snapshot.getJuristicCustId());
        vo.setContactPhone(resolveContactPhone(snapshot));
        vo.setAddress(snapshot.getCompanyAddress());
        vo.setProvinceName(snapshot.getProvinceName());
        vo.setCityName(snapshot.getCityName());
        vo.setDistrictName(snapshot.getDistrictName());
        vo.setServicePhone(null);
        vo.setSourceType(SOURCE_TYPE_CRM);
        vo.setStatus(resolveLocalStatus(snapshot.getCustState()));
        vo.setCustState(snapshot.getCustState());
        vo.setCustStateLabel(resolveCustStateLabel(snapshot.getCustState()));
        if (existingCompany != null) {
            vo.setExistingCompanyId(existingCompany.getId());
            vo.setExistingCompanyName(existingCompany.getCompanyName());
        }
        String disabledReason = resolveImportDisabledReason(snapshot, existingCompany);
        vo.setCanImport(disabledReason == null);
        vo.setImportDisabledReason(disabledReason);
        return vo;
    }

    private String resolveImportDisabledReason(CrmBizCompanySnapshot snapshot, SysCompany existingCompany) {
        if (existingCompany != null) {
            return "本地公司已存在";
        }
        if (StrUtil.isBlank(snapshot.getSapCompanyCode())) {
            return "缺少SAP公司编码，不能导入";
        }
        if (resolveTypeCode(snapshot.getCustRage()) == null) {
            return "客户范围不支持导入";
        }
        return null;
    }

    private String resolveTypeCode(Integer custRage) {
        if (Objects.equals(custRage, 0)) {
            return TYPE_CODE_SITE_FIRST;
        }
        if (Objects.equals(custRage, 3)) {
            return TYPE_CODE_SITE_SECOND;
        }
        return null;
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
            throw new ServiceException("当前未配置CRM数据源，请先完善 jasic.crm.datasource");
        }
        return crmJdbcTemplate;
    }

    private String resolveContactPhone(CrmBizCompanySnapshot snapshot) {
        return StrUtil.blankToDefault(StrUtil.trim(snapshot.getGroupContactPhone()), StrUtil.trim(snapshot.getCellphone()));
    }

    private Integer resolveLocalStatus(Integer custState) {
        return Objects.equals(custState, 1) ? 1 : 0;
    }

    private String resolveDefaultAdminUsername(String companyCode) {
        return StrUtil.trimToNull(companyCode);
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
                return "资料未填充";
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
        snapshot.setSapCompanyCode(StrUtil.trim(rs.getString("sap_company_code")));
        int custRage = rs.getInt("cust_rage");
        snapshot.setCustRage(rs.wasNull() ? null : custRage);
        snapshot.setCompanyShortName(StrUtil.trim(rs.getString("company_short_name")));
        snapshot.setProvinceName(StrUtil.trim(rs.getString("province_name")));
        snapshot.setCityName(StrUtil.trim(rs.getString("city_name")));
        snapshot.setDistrictName(StrUtil.trim(rs.getString("district_name")));
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
