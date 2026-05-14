package com.jasic.aftersales.system.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.jasic.aftersales.common.exception.ServiceException;
import com.jasic.aftersales.system.domain.vo.MachineBarcodeSyncResultVO;
import com.jasic.aftersales.system.service.IMachineBarcodeSyncService;
import lombok.extern.slf4j.Slf4j;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * 条码主数据同步 Service 实现
 *
 * @author Codex
 * @date 2026/04/12
 */
@Slf4j
@Service
public class MachineBarcodeSyncServiceImpl implements IMachineBarcodeSyncService {

    private static final Pattern TABLE_NAME_PATTERN = Pattern.compile("^[A-Za-z0-9_$.]+$");
    private static final String CRM_BARCODE_TABLE = "order_deliver_barcode";
    private static final String CRM_PRODUCT_TABLE = "sap_product_info";
    private static final String LOCAL_COMPANY_MAPPING_TABLE = "crm_company_mapping";
    private static final int DEFAULT_BATCH_SIZE = 1000;
    private static final int PROGRESS_LOG_INTERVAL = 10000;

    @Resource(name = "jdbcTemplate")
    private JdbcTemplate jdbcTemplate;

    /**
     * JDBC模板模板依赖。
     *
     * @return 处理结果
     */
    @Resource(name = "crmJdbcTemplate")
    private JdbcTemplate crmJdbcTemplate;

    /**
     * 处理fullSyncFromCrm业务逻辑。
     *
     * <p>说明：该方法用于执行业务流程编排，确保调用链路清晰可维护。</p>
     * @return 处理结果
     */
    @Override
    public MachineBarcodeSyncResultVO fullSyncFromCrm() {
        // 调用getEarliestAddTime方法，复用统一能力并保证业务规则一致。
        LocalDateTime earliestAddTime = getEarliestAddTime();
        if (earliestAddTime == null) {
            // 调用MachineBarcodeSyncResultVO方法，复用统一能力并保证业务规则一致。
            MachineBarcodeSyncResultVO result = new MachineBarcodeSyncResultVO();
            // 调用now方法，复用统一能力并保证业务规则一致。
            result.setStartTime(LocalDateTime.now());
            // 调用now方法，复用统一能力并保证业务规则一致。
            result.setEndTime(LocalDateTime.now());
            return result;
        }
        return syncByAddTimeRange(earliestAddTime, LocalDateTime.now());
    }

    /**
     * 获取EarliestAddTime。
     *
     * @return 处理结果
     */
    @Override
    public LocalDateTime getEarliestAddTime() {
        // 说明：执行该步骤以保证业务流程正确。
        JdbcTemplate crm = requireCrmJdbcTemplate();
        // 调用validateTableName方法，复用统一能力并保证业务规则一致。
        String barcodeTable = validateTableName(CRM_BARCODE_TABLE, "CRM 主条码表");
        String sql = "SELECT MIN(add_time) FROM " + barcodeTable
                // 调用TRIM方法，复用统一能力并保证业务规则一致。
                + " WHERE barcode IS NOT NULL AND TRIM(barcode) <> '' AND add_time IS NOT NULL";
        // 调用queryForObject方法，复用统一能力并保证业务规则一致。
        Timestamp timestamp = crm.queryForObject(sql, Timestamp.class);
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    /**
     * 同步ByAddTimeRange。
     *
     * @param startInclusive 参数
     * @param endExclusive 参数
     * @return 处理结果
     */
    @Override
    public MachineBarcodeSyncResultVO syncByAddTimeRange(LocalDateTime startInclusive, LocalDateTime endExclusive) {
        if (startInclusive == null || endExclusive == null || !startInclusive.isBefore(endExclusive)) {
            throw new ServiceException("条码同步时间范围不合法");
        }
        // 调用info方法，复用统一能力并保证业务规则一致。
        log.info("条码同步开始，数据范围=[{}, {})", startInclusive, endExclusive);

        // 说明：执行该步骤以保证业务流程正确。
        JdbcTemplate crm = requireCrmJdbcTemplate();
        // 调用validateTableName方法，复用统一能力并保证业务规则一致。
        String barcodeTable = validateTableName(CRM_BARCODE_TABLE, "CRM 主条码表");
        // 调用validateTableName方法，复用统一能力并保证业务规则一致。
        String productTable = validateTableName(CRM_PRODUCT_TABLE, "SAP 物料表");
        // 调用validateTableName方法，复用统一能力并保证业务规则一致。
        String companyMappingTable = validateTableName(LOCAL_COMPANY_MAPPING_TABLE, "CRM 公司映射表");

        // 调用loadCompanyMappings方法，复用统一能力并保证业务规则一致。
        CompanyMappingSnapshot mappingSnapshot = loadCompanyMappings(companyMappingTable);
        log.info("条码同步已加载公司映射：custId映射 {} 条，salesOrg映射 {} 条",
                mappingSnapshot.getSalesOrgByCustId().size(),
                // 调用size方法，复用统一能力并保证业务规则一致。
                mappingSnapshot.getHqCompanyIdBySalesOrg().size());
        // 调用loadAllProductSnapshots方法，复用统一能力并保证业务规则一致。
        Map<ProductKey, ProductSnapshot> productSnapshotMap = loadAllProductSnapshots(crm, productTable);
        // 调用size方法，复用统一能力并保证业务规则一致。
        log.info("条码同步已加载物料快照 {} 条", productSnapshotMap.size());
        // 调用SyncSummary方法，复用统一能力并保证业务规则一致。
        SyncSummary summary = new SyncSummary();
        // 调用now方法，复用统一能力并保证业务规则一致。
        summary.setStartTime(LocalDateTime.now());

        LocalDateTime current = startInclusive;
        while (current.isBefore(endExclusive)) {
            // 调用nextMonthStart方法，复用统一能力并保证业务规则一致。
            LocalDateTime next = nextMonthStart(current);
            if (next.isAfter(endExclusive)) {
                next = endExclusive;
            }
            // 调用info方法，复用统一能力并保证业务规则一致。
            log.info("条码同步开始处理月切片，范围=[{}, {})", current, next);
            // 调用syncBarcodeBase方法，复用统一能力并保证业务规则一致。
            SyncSummary sliceSummary = syncBarcodeBase(crm, barcodeTable, current, next, mappingSnapshot, productSnapshotMap);
            // 调用mergeSummary方法，复用统一能力并保证业务规则一致。
            mergeSummary(summary, sliceSummary);
            log.info("条码同步完成月切片，范围=[{}, {})，处理 {} 条，新增 {} 条，跳过 {} 条，总部未匹配 {} 条，总部冲突 {} 条，物料未匹配 {} 条",
                    current,
                    next,
                    sliceSummary.getProcessedCount(),
                    sliceSummary.getInsertedCount(),
                    sliceSummary.getSkippedExistingCount(),
                    sliceSummary.getHqUnmatchedCount(),
                    sliceSummary.getHqConflictCount(),
                    // 调用getProductUnmatchedCount方法，复用统一能力并保证业务规则一致。
                    sliceSummary.getProductUnmatchedCount());
            current = next;
        }
        // 调用now方法，复用统一能力并保证业务规则一致。
        summary.setEndTime(LocalDateTime.now());
        log.info("条码同步结束，数据范围=[{}, {})，总处理 {} 条，新增 {} 条，跳过 {} 条，总部未匹配 {} 条，总部冲突 {} 条，物料未匹配 {} 条",
                startInclusive,
                endExclusive,
                summary.getProcessedCount(),
                summary.getInsertedCount(),
                summary.getSkippedExistingCount(),
                summary.getHqUnmatchedCount(),
                summary.getHqConflictCount(),
                // 调用getProductUnmatchedCount方法，复用统一能力并保证业务规则一致。
                summary.getProductUnmatchedCount());

        // 调用MachineBarcodeSyncResultVO方法，复用统一能力并保证业务规则一致。
        MachineBarcodeSyncResultVO result = new MachineBarcodeSyncResultVO();
        // 调用getStartTime方法，复用统一能力并保证业务规则一致。
        result.setStartTime(summary.getStartTime());
        // 调用getEndTime方法，复用统一能力并保证业务规则一致。
        result.setEndTime(summary.getEndTime());
        // 调用getProcessedCount方法，复用统一能力并保证业务规则一致。
        result.setBarcodeProcessedCount(summary.getProcessedCount());
        // 调用getInsertedCount方法，复用统一能力并保证业务规则一致。
        result.setInsertedCount(summary.getInsertedCount());
        // 调用getSkippedExistingCount方法，复用统一能力并保证业务规则一致。
        result.setSkippedExistingCount(summary.getSkippedExistingCount());
        // 调用getHqUnmatchedCount方法，复用统一能力并保证业务规则一致。
        result.setHqUnmatchedCount(summary.getHqUnmatchedCount());
        // 调用getHqConflictCount方法，复用统一能力并保证业务规则一致。
        result.setHqConflictCount(summary.getHqConflictCount());
        // 调用getProductUnmatchedCount方法，复用统一能力并保证业务规则一致。
        result.setProductUnmatchedCount(summary.getProductUnmatchedCount());
        // 调用setDealerProcessedCount方法，复用统一能力并保证业务规则一致。
        result.setDealerProcessedCount(0);
        // 调用setDealerUpdatedCount方法，复用统一能力并保证业务规则一致。
        result.setDealerUpdatedCount(0);
        return result;
    }

    /**
     * 同步条码基础。
     *
     * @param crm 参数
     * @param barcodeTable 参数
     * @param startInclusive 参数
     * @param endExclusive 参数
     * @param mappingSnapshot 参数
     * @param productSnapshotMap 参数
     * @return 处理结果
     */
    private SyncSummary syncBarcodeBase(JdbcTemplate crm, String barcodeTable, LocalDateTime startInclusive,
                                        LocalDateTime endExclusive, CompanyMappingSnapshot mappingSnapshot,
                                        Map<ProductKey, ProductSnapshot> productSnapshotMap) {
        // 调用SyncSummary方法，复用统一能力并保证业务规则一致。
        SyncSummary summary = new SyncSummary();
        final LocalDateTime[] latestAddTimeHolder = new LocalDateTime[1];
        final String[] latestBarcodeHolder = new String[1];
        String sql = "SELECT barcode, deliver_number, product_numeric, add_cust_id, scan_time, add_time "
                + "FROM " + barcodeTable + " "
                + "WHERE barcode IS NOT NULL AND TRIM(barcode) <> '' "
                + "AND add_time >= ? AND add_time < ? "
                + "ORDER BY add_time ASC, barcode ASC";

        List<CrmBarcodeRow> batchRows = new ArrayList<>(DEFAULT_BATCH_SIZE);
        // 调用now方法，复用统一能力并保证业务规则一致。
        LocalDateTime syncTime = LocalDateTime.now();
        crm.query(connection -> {
            // 调用prepareStatement方法，复用统一能力并保证业务规则一致。
            PreparedStatement ps = connection.prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
            // 调用setFetchSize方法，复用统一能力并保证业务规则一致。
            ps.setFetchSize(Integer.MIN_VALUE);
            // 调用toTimestamp方法，复用统一能力并保证业务规则一致。
            ps.setTimestamp(1, toTimestamp(startInclusive));
            // 调用toTimestamp方法，复用统一能力并保证业务规则一致。
            ps.setTimestamp(2, toTimestamp(endExclusive));
            return ps;
        }, rs -> {
            while (rs.next()) {
                // 调用getRow方法，复用统一能力并保证业务规则一致。
                CrmBarcodeRow row = CRM_BARCODE_ROW_MAPPER.mapRow(rs, rs.getRow());
                if (row == null || StrUtil.isBlank(row.getBarcode())) {
                    continue;
                }
                // 调用addProcessedCount方法，复用统一能力并保证业务规则一致。
                summary.addProcessedCount(1);
                // 调用getAddTime方法，复用统一能力并保证业务规则一致。
                latestAddTimeHolder[0] = row.getAddTime();
                // 调用getBarcode方法，复用统一能力并保证业务规则一致。
                latestBarcodeHolder[0] = row.getBarcode();
                // 调用enrichCompanyMapping方法，复用统一能力并保证业务规则一致。
                enrichCompanyMapping(row, mappingSnapshot, summary);
                // 调用enrichProductInfo方法，复用统一能力并保证业务规则一致。
                enrichProductInfo(row, productSnapshotMap, summary);
                // 调用add方法，复用统一能力并保证业务规则一致。
                batchRows.add(row);
                if (summary.getProcessedCount() % PROGRESS_LOG_INTERVAL == 0) {
                    log.info("条码同步切片进行中，范围=[{}, {})，已处理 {} 条，当前 add_time={}，当前 barcode={}",
                            startInclusive,
                            endExclusive,
                            summary.getProcessedCount(),
                            latestAddTimeHolder[0],
                            latestBarcodeHolder[0]);
                }
                if (batchRows.size() >= DEFAULT_BATCH_SIZE) {
                    // 调用batchInsertBarcodeRows方法，复用统一能力并保证业务规则一致。
                    batchInsertBarcodeRows(batchRows, syncTime, summary);
                    // 调用clear方法，复用统一能力并保证业务规则一致。
                    batchRows.clear();
                }
            }
            return null;
        });

        if (CollUtil.isNotEmpty(batchRows)) {
            // 调用batchInsertBarcodeRows方法，复用统一能力并保证业务规则一致。
            batchInsertBarcodeRows(batchRows, syncTime, summary);
        }
        return summary;
    }

    /**
     * mergeSummary。
     *
     * @param target 参数
     * @param source 参数
     */
    private void mergeSummary(SyncSummary target, SyncSummary source) {
        // 调用getProcessedCount方法，复用统一能力并保证业务规则一致。
        target.addProcessedCount(source.getProcessedCount());
        // 调用getInsertedCount方法，复用统一能力并保证业务规则一致。
        target.addInsertedCount(source.getInsertedCount());
        // 调用getSkippedExistingCount方法，复用统一能力并保证业务规则一致。
        target.addSkippedExistingCount(source.getSkippedExistingCount());
        // 调用getHqUnmatchedCount方法，复用统一能力并保证业务规则一致。
        target.addHqUnmatchedCount(source.getHqUnmatchedCount());
        // 调用getHqConflictCount方法，复用统一能力并保证业务规则一致。
        target.addHqConflictCount(source.getHqConflictCount());
        // 调用getProductUnmatchedCount方法，复用统一能力并保证业务规则一致。
        target.addProductUnmatchedCount(source.getProductUnmatchedCount());
    }

    /**
     * nextMonthStart。
     *
     * @param current 参数
     * @return 处理结果
     */
    private LocalDateTime nextMonthStart(LocalDateTime current) {
        return current.toLocalDate()
                .withDayOfMonth(1)
                .plusMonths(1)
                // 调用atStartOfDay方法，复用统一能力并保证业务规则一致。
                .atStartOfDay();
    }

    /**
     * load公司Mappings。
     *
     * @param tableName 参数
     * @return 处理结果
     */
    private CompanyMappingSnapshot loadCompanyMappings(String tableName) {
        String sql = "SELECT cust_id, sales_org, hq_company_id FROM " + tableName + " WHERE status = 1";
        // 调用query方法，复用统一能力并保证业务规则一致。
        List<CompanyMappingRow> rows = jdbcTemplate.query(sql, COMPANY_MAPPING_ROW_MAPPER);
        // 调用CompanyMappingSnapshot方法，复用统一能力并保证业务规则一致。
        CompanyMappingSnapshot snapshot = new CompanyMappingSnapshot();
        for (CompanyMappingRow row : rows) {
            if (StrUtil.isNotBlank(row.getCustId())) {
                // 调用getSalesOrg方法，复用统一能力并保证业务规则一致。
                snapshot.getSalesOrgByCustId().put(row.getCustId(), row.getSalesOrg());
                if (row.getHqCompanyId() != null) {
                    // 调用getHqCompanyId方法，复用统一能力并保证业务规则一致。
                    snapshot.getHqCompanyIdByCustId().put(row.getCustId(), row.getHqCompanyId());
                }
            }
            if (StrUtil.isNotBlank(row.getSalesOrg()) && row.getHqCompanyId() != null) {
                // 调用getHqCompanyId方法，复用统一能力并保证业务规则一致。
                snapshot.getHqCompanyIdBySalesOrg().put(row.getSalesOrg(), row.getHqCompanyId());
            }
        }
        return snapshot;
    }

    /**
     * loadAllProductSnapshots。
     *
     * @param crm 参数
     * @param tableName 参数
     * @return 处理结果
     */
    private Map<ProductKey, ProductSnapshot> loadAllProductSnapshots(JdbcTemplate crm, String tableName) {
        // 调用info方法，复用统一能力并保证业务规则一致。
        log.info("条码同步开始加载物料快照，来源表={}", tableName);
        String sql = "SELECT product_numeric, sales_org, product_name, product_model, product_trumpet "
                + "FROM " + tableName
                + " WHERE product_numeric IS NOT NULL AND TRIM(product_numeric) <> ''"
                // 调用TRIM方法，复用统一能力并保证业务规则一致。
                + " AND sales_org IS NOT NULL AND TRIM(sales_org) <> ''";
        // 调用query方法，复用统一能力并保证业务规则一致。
        List<ProductSnapshot> snapshots = crm.query(sql, PRODUCT_SNAPSHOT_ROW_MAPPER);
        Map<ProductKey, ProductSnapshot> result = new HashMap<>();
        for (ProductSnapshot snapshot : snapshots) {
            // 调用getSalesOrg方法，复用统一能力并保证业务规则一致。
            result.put(new ProductKey(snapshot.getProductCode(), snapshot.getSalesOrg()), snapshot);
        }
        return result;
    }

    /**
     * enrich公司Mapping。
     *
     * @param row 参数
     * @param mappingSnapshot 参数
     * @param summary 参数
     */
    private void enrichCompanyMapping(CrmBarcodeRow row, CompanyMappingSnapshot mappingSnapshot, SyncSummary summary) {
        // 调用getCustId方法，复用统一能力并保证业务规则一致。
        String salesOrg = normalizeNullableText(mappingSnapshot.getSalesOrgByCustId().get(row.getCustId()));
        // 调用setSalesOrg方法，复用统一能力并保证业务规则一致。
        row.setSalesOrg(salesOrg);

        // 调用getCustId方法，复用统一能力并保证业务规则一致。
        Long hqByCust = mappingSnapshot.getHqCompanyIdByCustId().get(row.getCustId());
        // 调用get方法，复用统一能力并保证业务规则一致。
        Long hqBySalesOrg = StrUtil.isBlank(salesOrg) ? null : mappingSnapshot.getHqCompanyIdBySalesOrg().get(salesOrg);
        if (hqByCust != null && hqBySalesOrg != null && !Objects.equals(hqByCust, hqBySalesOrg)) {
            // 调用setHqConflict方法，复用统一能力并保证业务规则一致。
            row.setHqConflict(true);
            // 调用addHqConflictCount方法，复用统一能力并保证业务规则一致。
            summary.addHqConflictCount(1);
            // 调用addHqUnmatchedCount方法，复用统一能力并保证业务规则一致。
            summary.addHqUnmatchedCount(1);
            return;
        }

        Long resolvedHqCompanyId = hqByCust != null ? hqByCust : hqBySalesOrg;
        // 调用setHqCompanyId方法，复用统一能力并保证业务规则一致。
        row.setHqCompanyId(resolvedHqCompanyId);
        if (resolvedHqCompanyId == null) {
            // 调用addHqUnmatchedCount方法，复用统一能力并保证业务规则一致。
            summary.addHqUnmatchedCount(1);
        }
    }

    /**
     * enrichProductInfo。
     *
     * @param row 参数
     * @param productMap 参数
     * @param summary 参数
     */
    private void enrichProductInfo(CrmBarcodeRow row, Map<ProductKey, ProductSnapshot> productMap, SyncSummary summary) {
        if (StrUtil.isBlank(row.getProductCode()) || StrUtil.isBlank(row.getSalesOrg())) {
            if (StrUtil.isNotBlank(row.getProductCode())) {
                // 调用addProductUnmatchedCount方法，复用统一能力并保证业务规则一致。
                summary.addProductUnmatchedCount(1);
            }
            return;
        }
        // 调用getSalesOrg方法，复用统一能力并保证业务规则一致。
        ProductSnapshot snapshot = productMap.get(new ProductKey(row.getProductCode(), row.getSalesOrg()));
        if (snapshot == null) {
            // 调用addProductUnmatchedCount方法，复用统一能力并保证业务规则一致。
            summary.addProductUnmatchedCount(1);
            return;
        }
        // 调用getProductName方法，复用统一能力并保证业务规则一致。
        row.setProductName(snapshot.getProductName());
        // 调用getProductModel方法，复用统一能力并保证业务规则一致。
        row.setProductModel(snapshot.getProductModel());
        // 调用getMachineNo方法，复用统一能力并保证业务规则一致。
        row.setMachineNo(snapshot.getMachineNo());
    }

    /**
     * batch新增条码Rows。
     *
     * @param rows 参数
     * @param syncTime 参数
     * @param summary 参数
     */
    private void batchInsertBarcodeRows(List<CrmBarcodeRow> rows, LocalDateTime syncTime, SyncSummary summary) {
        String sql = "INSERT IGNORE INTO machine_barcode ("
                + "barcode, deliver_number, hq_company_id, cust_id, sales_org, product_code, "
                + "product_name, machine_no, product_model, scan_date, crm_add_time, last_sync_time, status, create_time, update_time"
                // 调用NOW方法，复用统一能力并保证业务规则一致。
                + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())";
        int[] results = jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
            /**
     * setValues。
     *
     * @param ps 参数
     * @param i 参数
             */
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                // 调用get方法，复用统一能力并保证业务规则一致。
                CrmBarcodeRow row = rows.get(i);
                // 调用getBarcode方法，复用统一能力并保证业务规则一致。
                ps.setString(1, row.getBarcode());
                // 调用getDeliverNumber方法，复用统一能力并保证业务规则一致。
                ps.setString(2, row.getDeliverNumber());
                // 调用getHqCompanyId方法，复用统一能力并保证业务规则一致。
                ps.setObject(3, row.getHqCompanyId());
                // 调用getCustId方法，复用统一能力并保证业务规则一致。
                ps.setString(4, row.getCustId());
                // 调用getSalesOrg方法，复用统一能力并保证业务规则一致。
                ps.setString(5, row.getSalesOrg());
                // 调用getProductCode方法，复用统一能力并保证业务规则一致。
                ps.setString(6, row.getProductCode());
                // 调用getProductName方法，复用统一能力并保证业务规则一致。
                ps.setString(7, row.getProductName());
                // 调用getMachineNo方法，复用统一能力并保证业务规则一致。
                ps.setString(8, row.getMachineNo());
                // 调用getProductModel方法，复用统一能力并保证业务规则一致。
                ps.setString(9, row.getProductModel());
                // 调用getScanDate方法，复用统一能力并保证业务规则一致。
                ps.setTimestamp(10, toTimestamp(row.getScanDate()));
                // 调用getAddTime方法，复用统一能力并保证业务规则一致。
                ps.setTimestamp(11, toTimestamp(row.getAddTime()));
                // 调用toTimestamp方法，复用统一能力并保证业务规则一致。
                ps.setTimestamp(12, toTimestamp(syncTime));
                // 调用setInt方法，复用统一能力并保证业务规则一致。
                ps.setInt(13, 1);
            }

            /**
     * 获取BatchSize。
     *
     * @return 处理结果
             */
            @Override
            public int getBatchSize() {
                return rows.size();
            }
        });

        for (int result : results) {
            if (result > 0) {
                // 调用addInsertedCount方法，复用统一能力并保证业务规则一致。
                summary.addInsertedCount(1);
            } else {
                // 调用addSkippedExistingCount方法，复用统一能力并保证业务规则一致。
                summary.addSkippedExistingCount(1);
            }
        }
    }

    /**
     * requireCRMJDBC模板。
     *
     * @return 处理结果
     */
    private JdbcTemplate requireCrmJdbcTemplate() {
        if (crmJdbcTemplate == null) {
            throw new ServiceException("当前未配置客户关系管理（CRM）数据源，请先完善 jasic.crm.datasource");
        }
        return crmJdbcTemplate;
    }

    /**
     * 校验表名称。
     *
     * @param tableName 参数
     * @param label 参数
     * @return 处理结果
     */
    private String validateTableName(String tableName, String label) {
        // 调用trim方法，复用统一能力并保证业务规则一致。
        String normalized = StrUtil.trim(tableName);
        if (StrUtil.isBlank(normalized)) {
            throw new ServiceException(label + "未配置，请先完善条码同步参数");
        }
        if (!TABLE_NAME_PATTERN.matcher(normalized).matches()) {
            throw new ServiceException(label + "配置格式不合法");
        }
        return normalized;
    }

    /**
     * toTimestamp。
     *
     * @param value 参数
     * @return 处理结果
     */
    private Timestamp toTimestamp(LocalDateTime value) {
        return value == null ? null : Timestamp.valueOf(value);
    }

    /**
     * 规范化NullableText。
     *
     * @param value 参数
     * @return 处理结果
     */
    private String normalizeNullableText(String value) {
        // 调用trim方法，复用统一能力并保证业务规则一致。
        String normalized = StrUtil.trim(value);
        return StrUtil.isBlank(normalized) ? null : normalized;
    }

    /**
     * toLocalDateTime。
     *
     * @param rs 参数
     * @param column 参数
     * @return 处理结果
     */
    private static LocalDateTime toLocalDateTime(ResultSet rs, String column) throws SQLException {
        // 调用getTimestamp方法，复用统一能力并保证业务规则一致。
        Timestamp value = rs.getTimestamp(column);
        return value == null ? null : value.toLocalDateTime();
    }

    private static final RowMapper<CrmBarcodeRow> CRM_BARCODE_ROW_MAPPER = (rs, rowNum) -> {
        CrmBarcodeRow row = new CrmBarcodeRow();
        row.setBarcode(StrUtil.trim(rs.getString("barcode")));
        row.setDeliverNumber(StrUtil.trim(rs.getString("deliver_number")));
        row.setProductCode(StrUtil.trim(rs.getString("product_numeric")));
        row.setCustId(StrUtil.trim(rs.getString("add_cust_id")));
        row.setScanDate(toLocalDateTime(rs, "scan_time"));
        row.setAddTime(toLocalDateTime(rs, "add_time"));
        return row;
    };

    private static final RowMapper<ProductSnapshot> PRODUCT_SNAPSHOT_ROW_MAPPER = (rs, rowNum) -> {
        ProductSnapshot snapshot = new ProductSnapshot();
        snapshot.setProductCode(StrUtil.trim(rs.getString("product_numeric")));
        snapshot.setSalesOrg(StrUtil.trim(rs.getString("sales_org")));
        snapshot.setProductName(StrUtil.trim(rs.getString("product_name")));
        snapshot.setProductModel(StrUtil.trim(rs.getString("product_model")));
        snapshot.setMachineNo(StrUtil.trim(rs.getString("product_trumpet")));
        return snapshot;
    };

    private static final RowMapper<CompanyMappingRow> COMPANY_MAPPING_ROW_MAPPER = (rs, rowNum) -> {
        CompanyMappingRow row = new CompanyMappingRow();
        row.setCustId(StrUtil.trim(rs.getString("cust_id")));
        row.setSalesOrg(StrUtil.trim(rs.getString("sales_org")));
        long hqCompanyId = rs.getLong("hq_company_id");
        row.setHqCompanyId(rs.wasNull() ? null : hqCompanyId);
        return row;
    };

    private static class SyncSummary {
        /**
     * LocalDateTime字段。
     *
     * @return 处理结果
         */
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private int processedCount;
        private int insertedCount;
        private int skippedExistingCount;
        private int hqUnmatchedCount;
        private int hqConflictCount;
        private int productUnmatchedCount;

        /**
         * 获取StartTime相关数据。
         *
         * <p>说明：该方法用于执行业务流程编排，确保调用链路清晰可维护。</p>
         * @return 处理结果
         */
        public LocalDateTime getStartTime() {
            return startTime;
        }

        /**
     * setStartTime。
     *
     * @param startTime 参数
         */
        public void setStartTime(LocalDateTime startTime) {
            this.startTime = startTime;
        }

        /**
     * 获取EndTime。
     *
     * @return 处理结果
         */
        public LocalDateTime getEndTime() {
            return endTime;
        }

        /**
     * setEndTime。
     *
     * @param endTime 参数
         */
        public void setEndTime(LocalDateTime endTime) {
            this.endTime = endTime;
        }

        /**
     * 获取ProcessedCount。
     *
     * @return 处理结果
         */
        public int getProcessedCount() {
            return processedCount;
        }

        /**
     * 新增ProcessedCount。
     *
     * @param value 参数
         */
        public void addProcessedCount(int value) {
            this.processedCount += value;
        }

        /**
     * 获取InsertedCount。
     *
     * @return 处理结果
         */
        public int getInsertedCount() {
            return insertedCount;
        }

        /**
     * 新增InsertedCount。
     *
     * @param value 参数
         */
        public void addInsertedCount(int value) {
            this.insertedCount += value;
        }

        /**
     * 获取SkippedExistingCount。
     *
     * @return 处理结果
         */
        public int getSkippedExistingCount() {
            return skippedExistingCount;
        }

        /**
     * 新增SkippedExistingCount。
     *
     * @param value 参数
         */
        public void addSkippedExistingCount(int value) {
            this.skippedExistingCount += value;
        }

        /**
     * 获取总部UnmatchedCount。
     *
     * @return 处理结果
         */
        public int getHqUnmatchedCount() {
            return hqUnmatchedCount;
        }

        /**
     * 新增总部UnmatchedCount。
     *
     * @param value 参数
         */
        public void addHqUnmatchedCount(int value) {
            this.hqUnmatchedCount += value;
        }

        /**
     * 获取总部ConflictCount。
     *
     * @return 处理结果
         */
        public int getHqConflictCount() {
            return hqConflictCount;
        }

        /**
     * 新增总部ConflictCount。
     *
     * @param value 参数
         */
        public void addHqConflictCount(int value) {
            this.hqConflictCount += value;
        }

        /**
     * 获取ProductUnmatchedCount。
     *
     * @return 处理结果
         */
        public int getProductUnmatchedCount() {
            return productUnmatchedCount;
        }

        /**
     * 新增ProductUnmatchedCount。
     *
     * @param value 参数
         */
        public void addProductUnmatchedCount(int value) {
            this.productUnmatchedCount += value;
        }
    }

    private static class CompanyMappingSnapshot {
        private final Map<String, String> salesOrgByCustId = new LinkedHashMap<>();
        private final Map<String, Long> hqCompanyIdByCustId = new LinkedHashMap<>();
        private final Map<String, Long> hqCompanyIdBySalesOrg = new LinkedHashMap<>();

        /**
     * 获取SalesOrgByCustID。
     *
     * @return 处理结果
         */
        public Map<String, String> getSalesOrgByCustId() {
            return salesOrgByCustId;
        }

        /**
     * 获取总部公司IDByCustID。
     *
     * @return 处理结果
         */
        public Map<String, Long> getHqCompanyIdByCustId() {
            return hqCompanyIdByCustId;
        }

        /**
     * 获取总部公司IDBySalesOrg。
     *
     * @return 处理结果
         */
        public Map<String, Long> getHqCompanyIdBySalesOrg() {
            return hqCompanyIdBySalesOrg;
        }
    }

    private static class CompanyMappingRow {
        /**
     * String字段。
     *
     * @return 处理结果
         */
        private String custId;
        private String salesOrg;
        private Long hqCompanyId;

        /**
         * 获取CustId相关数据。
         *
         * <p>说明：该方法用于执行业务流程编排，确保调用链路清晰可维护。</p>
         * @return 处理结果
         */
        public String getCustId() {
            return custId;
        }

        /**
     * setCustID。
     *
     * @param custId cust ID
         */
        public void setCustId(String custId) {
            this.custId = custId;
        }

        /**
     * 获取SalesOrg。
     *
     * @return 处理结果
         */
        public String getSalesOrg() {
            return salesOrg;
        }

        /**
     * setSalesOrg。
     *
     * @param salesOrg 参数
         */
        public void setSalesOrg(String salesOrg) {
            this.salesOrg = salesOrg;
        }

        /**
     * 获取总部公司ID。
     *
     * @return 处理结果
         */
        public Long getHqCompanyId() {
            return hqCompanyId;
        }

        /**
     * set总部公司ID。
     *
     * @param hqCompanyId hq Company ID
         */
        public void setHqCompanyId(Long hqCompanyId) {
            this.hqCompanyId = hqCompanyId;
        }
    }

    private static class CrmBarcodeRow {
        /**
     * String字段。
     *
     * @return 处理结果
         */
        private String barcode;
        private String deliverNumber;
        private String custId;
        private String salesOrg;
        private String productCode;
        private String productName;
        private String productModel;
        private String machineNo;
        private Long hqCompanyId;
        private LocalDateTime scanDate;
        private LocalDateTime addTime;
        private boolean hqConflict;

        /**
         * 获取Barcode相关数据。
         *
         * <p>说明：该方法用于执行业务流程编排，确保调用链路清晰可维护。</p>
         * @return 处理结果
         */
        public String getBarcode() {
            return barcode;
        }

        /**
     * set条码。
     *
     * @param barcode 参数
         */
        public void setBarcode(String barcode) {
            this.barcode = barcode;
        }

        /**
     * 获取DeliverNumber。
     *
     * @return 处理结果
         */
        public String getDeliverNumber() {
            return deliverNumber;
        }

        /**
     * setDeliverNumber。
     *
     * @param deliverNumber 参数
         */
        public void setDeliverNumber(String deliverNumber) {
            this.deliverNumber = deliverNumber;
        }

        /**
     * 获取CustID。
     *
     * @return 处理结果
         */
        public String getCustId() {
            return custId;
        }

        /**
     * setCustID。
     *
     * @param custId cust ID
         */
        public void setCustId(String custId) {
            this.custId = custId;
        }

        /**
     * 获取SalesOrg。
     *
     * @return 处理结果
         */
        public String getSalesOrg() {
            return salesOrg;
        }

        /**
     * setSalesOrg。
     *
     * @param salesOrg 参数
         */
        public void setSalesOrg(String salesOrg) {
            this.salesOrg = salesOrg;
        }

        /**
     * 获取Product编码。
     *
     * @return 处理结果
         */
        public String getProductCode() {
            return productCode;
        }

        /**
     * setProduct编码。
     *
     * @param productCode 参数
         */
        public void setProductCode(String productCode) {
            this.productCode = productCode;
        }

        /**
     * 获取Product名称。
     *
     * @return 处理结果
         */
        public String getProductName() {
            return productName;
        }

        /**
     * setProduct名称。
     *
     * @param productName 参数
         */
        public void setProductName(String productName) {
            this.productName = productName;
        }

        /**
     * 获取ProductModel。
     *
     * @return 处理结果
         */
        public String getProductModel() {
            return productModel;
        }

        /**
     * setProductModel。
     *
     * @param productModel 参数
         */
        public void setProductModel(String productModel) {
            this.productModel = productModel;
        }

        /**
     * 获取机器编号。
     *
     * @return 处理结果
         */
        public String getMachineNo() {
            return machineNo;
        }

        /**
     * set机器编号。
     *
     * @param machineNo 参数
         */
        public void setMachineNo(String machineNo) {
            this.machineNo = machineNo;
        }

        /**
     * 获取总部公司ID。
     *
     * @return 处理结果
         */
        public Long getHqCompanyId() {
            return hqCompanyId;
        }

        /**
     * set总部公司ID。
     *
     * @param hqCompanyId hq Company ID
         */
        public void setHqCompanyId(Long hqCompanyId) {
            this.hqCompanyId = hqCompanyId;
        }

        /**
     * 获取扫描Date。
     *
     * @return 处理结果
         */
        public LocalDateTime getScanDate() {
            return scanDate;
        }

        /**
     * set扫描Date。
     *
     * @param scanDate 参数
         */
        public void setScanDate(LocalDateTime scanDate) {
            this.scanDate = scanDate;
        }

        /**
     * 获取AddTime。
     *
     * @return 处理结果
         */
        public LocalDateTime getAddTime() {
            return addTime;
        }

        /**
     * setAddTime。
     *
     * @param addTime 参数
         */
        public void setAddTime(LocalDateTime addTime) {
            this.addTime = addTime;
        }

        /**
     * 判断是否总部Conflict。
         */
        public boolean isHqConflict() {
            return hqConflict;
        }

        /**
     * set总部Conflict。
     *
     * @param hqConflict 参数
         */
        public void setHqConflict(boolean hqConflict) {
            this.hqConflict = hqConflict;
        }
    }

    private static class ProductSnapshot {
        /**
     * String字段。
     *
     * @return 处理结果
         */
        private String productCode;
        private String salesOrg;
        private String productName;
        private String productModel;
        private String machineNo;

        /**
         * 获取ProductCode相关数据。
         *
         * <p>说明：该方法用于执行业务流程编排，确保调用链路清晰可维护。</p>
         * @return 处理结果
         */
        public String getProductCode() {
            return productCode;
        }

        /**
     * setProduct编码。
     *
     * @param productCode 参数
         */
        public void setProductCode(String productCode) {
            this.productCode = productCode;
        }

        /**
     * 获取SalesOrg。
     *
     * @return 处理结果
         */
        public String getSalesOrg() {
            return salesOrg;
        }

        /**
     * setSalesOrg。
     *
     * @param salesOrg 参数
         */
        public void setSalesOrg(String salesOrg) {
            this.salesOrg = salesOrg;
        }

        /**
     * 获取Product名称。
     *
     * @return 处理结果
         */
        public String getProductName() {
            return productName;
        }

        /**
     * setProduct名称。
     *
     * @param productName 参数
         */
        public void setProductName(String productName) {
            this.productName = productName;
        }

        /**
     * 获取ProductModel。
     *
     * @return 处理结果
         */
        public String getProductModel() {
            return productModel;
        }

        /**
     * setProductModel。
     *
     * @param productModel 参数
         */
        public void setProductModel(String productModel) {
            this.productModel = productModel;
        }

        /**
     * 获取机器编号。
     *
     * @return 处理结果
         */
        public String getMachineNo() {
            return machineNo;
        }

        /**
     * set机器编号。
     *
     * @param machineNo 参数
         */
        public void setMachineNo(String machineNo) {
            this.machineNo = machineNo;
        }
    }

    private static class ProductKey {
        /**
     * String字段。
     *
     * @param productCode 参数
     * @param salesOrg 参数
     * @return 处理结果
         */
        private final String productCode;
        private final String salesOrg;

        /**
     * 构造机器条码同步实例。
     *
     * @param productCode 参数
     * @param salesOrg 参数
     * @return 处理结果
         */
        private ProductKey(String productCode, String salesOrg) {
            this.productCode = productCode;
            this.salesOrg = salesOrg;
        }

        /**
     * equals。
     *
     * @param o 参数
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof ProductKey)) {
                return false;
            }
            ProductKey productKey = (ProductKey) o;
            return Objects.equals(productCode, productKey.productCode)
                    // 调用equals方法，复用统一能力并保证业务规则一致。
                    && Objects.equals(salesOrg, productKey.salesOrg);
        }

        /**
     * 判断是否存在h编码。
     *
     * @return 处理结果
         */
        @Override
        public int hashCode() {
            return Objects.hash(productCode, salesOrg);
        }
    }
}


