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
     * ?? fullSyncFromCrm ?????
     *
     * @return ????
     */
    @Resource(name = "crmJdbcTemplate")
    private JdbcTemplate crmJdbcTemplate;

    @Override
    public MachineBarcodeSyncResultVO fullSyncFromCrm() {
        LocalDateTime earliestAddTime = getEarliestAddTime();
        if (earliestAddTime == null) {
            MachineBarcodeSyncResultVO result = new MachineBarcodeSyncResultVO();
            result.setStartTime(LocalDateTime.now());
            result.setEndTime(LocalDateTime.now());
            return result;
        }
        return syncByAddTimeRange(earliestAddTime, LocalDateTime.now());
    }

    /**
     * ??Earliest Add Time?
     *
     * @return ????
     */
    @Override
    public LocalDateTime getEarliestAddTime() {
        // ?????????????????????????????
        JdbcTemplate crm = requireCrmJdbcTemplate();
        String barcodeTable = validateTableName(CRM_BARCODE_TABLE, "CRM 主条码表");
        String sql = "SELECT MIN(add_time) FROM " + barcodeTable
                + " WHERE barcode IS NOT NULL AND TRIM(barcode) <> '' AND add_time IS NOT NULL";
        Timestamp timestamp = crm.queryForObject(sql, Timestamp.class);
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    /**
     * ???????
     *
     * @param startInclusive ???????
     * @param endExclusive ????????
     * @return ????
     */
    @Override
    public MachineBarcodeSyncResultVO syncByAddTimeRange(LocalDateTime startInclusive, LocalDateTime endExclusive) {
        if (startInclusive == null || endExclusive == null || !startInclusive.isBefore(endExclusive)) {
            throw new ServiceException("条码同步时间范围不合法");
        }
        log.info("条码同步开始，数据范围=[{}, {})", startInclusive, endExclusive);

        // ?????????????????????????????
        JdbcTemplate crm = requireCrmJdbcTemplate();
        String barcodeTable = validateTableName(CRM_BARCODE_TABLE, "CRM 主条码表");
        String productTable = validateTableName(CRM_PRODUCT_TABLE, "SAP 物料表");
        String companyMappingTable = validateTableName(LOCAL_COMPANY_MAPPING_TABLE, "CRM 公司映射表");

        CompanyMappingSnapshot mappingSnapshot = loadCompanyMappings(companyMappingTable);
        log.info("条码同步已加载公司映射：custId映射 {} 条，salesOrg映射 {} 条",
                mappingSnapshot.getSalesOrgByCustId().size(),
                mappingSnapshot.getHqCompanyIdBySalesOrg().size());
        Map<ProductKey, ProductSnapshot> productSnapshotMap = loadAllProductSnapshots(crm, productTable);
        log.info("条码同步已加载物料快照 {} 条", productSnapshotMap.size());
        SyncSummary summary = new SyncSummary();
        summary.setStartTime(LocalDateTime.now());

        LocalDateTime current = startInclusive;
        while (current.isBefore(endExclusive)) {
            LocalDateTime next = nextMonthStart(current);
            if (next.isAfter(endExclusive)) {
                next = endExclusive;
            }
            log.info("条码同步开始处理月切片，范围=[{}, {})", current, next);
            SyncSummary sliceSummary = syncBarcodeBase(crm, barcodeTable, current, next, mappingSnapshot, productSnapshotMap);
            mergeSummary(summary, sliceSummary);
            log.info("条码同步完成月切片，范围=[{}, {})，处理 {} 条，新增 {} 条，跳过 {} 条，总部未匹配 {} 条，总部冲突 {} 条，物料未匹配 {} 条",
                    current,
                    next,
                    sliceSummary.getProcessedCount(),
                    sliceSummary.getInsertedCount(),
                    sliceSummary.getSkippedExistingCount(),
                    sliceSummary.getHqUnmatchedCount(),
                    sliceSummary.getHqConflictCount(),
                    sliceSummary.getProductUnmatchedCount());
            current = next;
        }
        summary.setEndTime(LocalDateTime.now());
        log.info("条码同步结束，数据范围=[{}, {})，总处理 {} 条，新增 {} 条，跳过 {} 条，总部未匹配 {} 条，总部冲突 {} 条，物料未匹配 {} 条",
                startInclusive,
                endExclusive,
                summary.getProcessedCount(),
                summary.getInsertedCount(),
                summary.getSkippedExistingCount(),
                summary.getHqUnmatchedCount(),
                summary.getHqConflictCount(),
                summary.getProductUnmatchedCount());

        MachineBarcodeSyncResultVO result = new MachineBarcodeSyncResultVO();
        result.setStartTime(summary.getStartTime());
        result.setEndTime(summary.getEndTime());
        result.setBarcodeProcessedCount(summary.getProcessedCount());
        result.setInsertedCount(summary.getInsertedCount());
        result.setSkippedExistingCount(summary.getSkippedExistingCount());
        result.setHqUnmatchedCount(summary.getHqUnmatchedCount());
        result.setHqConflictCount(summary.getHqConflictCount());
        result.setProductUnmatchedCount(summary.getProductUnmatchedCount());
        result.setDealerProcessedCount(0);
        result.setDealerUpdatedCount(0);
        return result;
    }

    /**
     * ???????
     *
     * @param crm ??
     * @param barcodeTable ??
     * @param startInclusive ???????
     * @param endExclusive ????????
     * @param mappingSnapshot ??
     * @param productSnapshotMap ??
     * @return ????
     */
    private SyncSummary syncBarcodeBase(JdbcTemplate crm, String barcodeTable, LocalDateTime startInclusive,
                                        LocalDateTime endExclusive, CompanyMappingSnapshot mappingSnapshot,
                                        Map<ProductKey, ProductSnapshot> productSnapshotMap) {
        SyncSummary summary = new SyncSummary();
        final LocalDateTime[] latestAddTimeHolder = new LocalDateTime[1];
        final String[] latestBarcodeHolder = new String[1];
        String sql = "SELECT barcode, deliver_number, product_numeric, add_cust_id, scan_time, add_time "
                + "FROM " + barcodeTable + " "
                + "WHERE barcode IS NOT NULL AND TRIM(barcode) <> '' "
                + "AND add_time >= ? AND add_time < ? "
                + "ORDER BY add_time ASC, barcode ASC";

        List<CrmBarcodeRow> batchRows = new ArrayList<>(DEFAULT_BATCH_SIZE);
        LocalDateTime syncTime = LocalDateTime.now();
        crm.query(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
            ps.setFetchSize(Integer.MIN_VALUE);
            ps.setTimestamp(1, toTimestamp(startInclusive));
            ps.setTimestamp(2, toTimestamp(endExclusive));
            return ps;
        }, rs -> {
            while (rs.next()) {
                CrmBarcodeRow row = CRM_BARCODE_ROW_MAPPER.mapRow(rs, rs.getRow());
                if (row == null || StrUtil.isBlank(row.getBarcode())) {
                    continue;
                }
                summary.addProcessedCount(1);
                latestAddTimeHolder[0] = row.getAddTime();
                latestBarcodeHolder[0] = row.getBarcode();
                enrichCompanyMapping(row, mappingSnapshot, summary);
                enrichProductInfo(row, productSnapshotMap, summary);
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
                    batchInsertBarcodeRows(batchRows, syncTime, summary);
                    batchRows.clear();
                }
            }
            return null;
        });

        if (CollUtil.isNotEmpty(batchRows)) {
            batchInsertBarcodeRows(batchRows, syncTime, summary);
        }
        return summary;
    }

    /**
     * ?? mergeSummary ?????
     *
     * @param target ??
     * @param source ??
     */
    private void mergeSummary(SyncSummary target, SyncSummary source) {
        target.addProcessedCount(source.getProcessedCount());
        target.addInsertedCount(source.getInsertedCount());
        target.addSkippedExistingCount(source.getSkippedExistingCount());
        target.addHqUnmatchedCount(source.getHqUnmatchedCount());
        target.addHqConflictCount(source.getHqConflictCount());
        target.addProductUnmatchedCount(source.getProductUnmatchedCount());
    }

    /**
     * ?? nextMonthStart ?????
     *
     * @param current ??
     * @return ????
     */
    private LocalDateTime nextMonthStart(LocalDateTime current) {
        return current.toLocalDate()
                .withDayOfMonth(1)
                .plusMonths(1)
                .atStartOfDay();
    }

    /**
     * ?????
     *
     * @param tableName ??
     * @return ????
     */
    private CompanyMappingSnapshot loadCompanyMappings(String tableName) {
        String sql = "SELECT cust_id, sales_org, hq_company_id FROM " + tableName + " WHERE status = 1";
        List<CompanyMappingRow> rows = jdbcTemplate.query(sql, COMPANY_MAPPING_ROW_MAPPER);
        CompanyMappingSnapshot snapshot = new CompanyMappingSnapshot();
        for (CompanyMappingRow row : rows) {
            if (StrUtil.isNotBlank(row.getCustId())) {
                snapshot.getSalesOrgByCustId().put(row.getCustId(), row.getSalesOrg());
                if (row.getHqCompanyId() != null) {
                    snapshot.getHqCompanyIdByCustId().put(row.getCustId(), row.getHqCompanyId());
                }
            }
            if (StrUtil.isNotBlank(row.getSalesOrg()) && row.getHqCompanyId() != null) {
                snapshot.getHqCompanyIdBySalesOrg().put(row.getSalesOrg(), row.getHqCompanyId());
            }
        }
        return snapshot;
    }

    /**
     * ?????
     *
     * @param crm ??
     * @param tableName ??
     * @return ????
     */
    private Map<ProductKey, ProductSnapshot> loadAllProductSnapshots(JdbcTemplate crm, String tableName) {
        log.info("条码同步开始加载物料快照，来源表={}", tableName);
        String sql = "SELECT product_numeric, sales_org, product_name, product_model, product_trumpet "
                + "FROM " + tableName
                + " WHERE product_numeric IS NOT NULL AND TRIM(product_numeric) <> ''"
                + " AND sales_org IS NOT NULL AND TRIM(sales_org) <> ''";
        List<ProductSnapshot> snapshots = crm.query(sql, PRODUCT_SNAPSHOT_ROW_MAPPER);
        Map<ProductKey, ProductSnapshot> result = new HashMap<>();
        for (ProductSnapshot snapshot : snapshots) {
            result.put(new ProductKey(snapshot.getProductCode(), snapshot.getSalesOrg()), snapshot);
        }
        return result;
    }

    /**
     * ?? enrichCompanyMapping ?????
     *
     * @param row ??
     * @param mappingSnapshot ??
     * @param summary ??
     */
    private void enrichCompanyMapping(CrmBarcodeRow row, CompanyMappingSnapshot mappingSnapshot, SyncSummary summary) {
        String salesOrg = normalizeNullableText(mappingSnapshot.getSalesOrgByCustId().get(row.getCustId()));
        row.setSalesOrg(salesOrg);

        Long hqByCust = mappingSnapshot.getHqCompanyIdByCustId().get(row.getCustId());
        Long hqBySalesOrg = StrUtil.isBlank(salesOrg) ? null : mappingSnapshot.getHqCompanyIdBySalesOrg().get(salesOrg);
        if (hqByCust != null && hqBySalesOrg != null && !Objects.equals(hqByCust, hqBySalesOrg)) {
            row.setHqConflict(true);
            summary.addHqConflictCount(1);
            summary.addHqUnmatchedCount(1);
            return;
        }

        Long resolvedHqCompanyId = hqByCust != null ? hqByCust : hqBySalesOrg;
        row.setHqCompanyId(resolvedHqCompanyId);
        if (resolvedHqCompanyId == null) {
            summary.addHqUnmatchedCount(1);
        }
    }

    /**
     * ?? enrichProductInfo ?????
     *
     * @param row ??
     * @param productMap ??
     * @param summary ??
     */
    private void enrichProductInfo(CrmBarcodeRow row, Map<ProductKey, ProductSnapshot> productMap, SyncSummary summary) {
        if (StrUtil.isBlank(row.getProductCode()) || StrUtil.isBlank(row.getSalesOrg())) {
            if (StrUtil.isNotBlank(row.getProductCode())) {
                summary.addProductUnmatchedCount(1);
            }
            return;
        }
        ProductSnapshot snapshot = productMap.get(new ProductKey(row.getProductCode(), row.getSalesOrg()));
        if (snapshot == null) {
            summary.addProductUnmatchedCount(1);
            return;
        }
        row.setProductName(snapshot.getProductName());
        row.setProductModel(snapshot.getProductModel());
        row.setMachineNo(snapshot.getMachineNo());
    }

    /**
     * ?? batchInsertBarcodeRows ?????
     *
     * @param rows ??
     * @param syncTime ??
     * @param summary ??
     */
    private void batchInsertBarcodeRows(List<CrmBarcodeRow> rows, LocalDateTime syncTime, SyncSummary summary) {
        String sql = "INSERT IGNORE INTO machine_barcode ("
                + "barcode, deliver_number, hq_company_id, cust_id, sales_org, product_code, "
                + "product_name, machine_no, product_model, scan_date, crm_add_time, last_sync_time, status, create_time, update_time"
                + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())";
        int[] results = jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
            /**
             * ??Values?
             *
             * @param ps ??
             * @param i ??
             */
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                CrmBarcodeRow row = rows.get(i);
                ps.setString(1, row.getBarcode());
                ps.setString(2, row.getDeliverNumber());
                ps.setObject(3, row.getHqCompanyId());
                ps.setString(4, row.getCustId());
                ps.setString(5, row.getSalesOrg());
                ps.setString(6, row.getProductCode());
                ps.setString(7, row.getProductName());
                ps.setString(8, row.getMachineNo());
                ps.setString(9, row.getProductModel());
                ps.setTimestamp(10, toTimestamp(row.getScanDate()));
                ps.setTimestamp(11, toTimestamp(row.getAddTime()));
                ps.setTimestamp(12, toTimestamp(syncTime));
                ps.setInt(13, 1);
            }

            /**
             * ??Batch Size?
             *
             * @return ????
             */
            @Override
            public int getBatchSize() {
                return rows.size();
            }
        });

        for (int result : results) {
            if (result > 0) {
                summary.addInsertedCount(1);
            } else {
                summary.addSkippedExistingCount(1);
            }
        }
    }

    /**
     * ??????????
     *
     * @return ????
     */
    private JdbcTemplate requireCrmJdbcTemplate() {
        if (crmJdbcTemplate == null) {
            throw new ServiceException("当前未配置客户关系管理（CRM）数据源，请先完善 jasic.crm.datasource");
        }
        return crmJdbcTemplate;
    }

    /**
     * ???????
     *
     * @param tableName ??
     * @param label ??
     * @return ?????
     */
    private String validateTableName(String tableName, String label) {
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
     * ?? toTimestamp ?????
     *
     * @param value ???
     * @return ????
     */
    private Timestamp toTimestamp(LocalDateTime value) {
        return value == null ? null : Timestamp.valueOf(value);
    }

    /**
     * ????????
     *
     * @param value ???
     * @return ?????
     */
    private String normalizeNullableText(String value) {
        String normalized = StrUtil.trim(value);
        return StrUtil.isBlank(normalized) ? null : normalized;
    }

    /**
     * ?? toLocalDateTime ?????
     *
     * @param rs ??
     * @param column ??
     * @return ????
     */
    private static LocalDateTime toLocalDateTime(ResultSet rs, String column) throws SQLException {
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
         * ??Start Time?
         *
         * @return ????
         */
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private int processedCount;
        private int insertedCount;
        private int skippedExistingCount;
        private int hqUnmatchedCount;
        private int hqConflictCount;
        private int productUnmatchedCount;

        public LocalDateTime getStartTime() {
            return startTime;
        }

        /**
         * ??Start Time?
         *
         * @param startTime ??
         */
        public void setStartTime(LocalDateTime startTime) {
            this.startTime = startTime;
        }

        /**
         * ??End Time?
         *
         * @return ????
         */
        public LocalDateTime getEndTime() {
            return endTime;
        }

        /**
         * ??End Time?
         *
         * @param endTime ??
         */
        public void setEndTime(LocalDateTime endTime) {
            this.endTime = endTime;
        }

        /**
         * ??Processed Count?
         *
         * @return ????
         */
        public int getProcessedCount() {
            return processedCount;
        }

        /**
         * ?? addProcessedCount ?????
         *
         * @param value ???
         */
        public void addProcessedCount(int value) {
            this.processedCount += value;
        }

        /**
         * ??Inserted Count?
         *
         * @return ????
         */
        public int getInsertedCount() {
            return insertedCount;
        }

        /**
         * ?? addInsertedCount ?????
         *
         * @param value ???
         */
        public void addInsertedCount(int value) {
            this.insertedCount += value;
        }

        /**
         * ??Skipped Existing Count?
         *
         * @return ????
         */
        public int getSkippedExistingCount() {
            return skippedExistingCount;
        }

        /**
         * ?? addSkippedExistingCount ?????
         *
         * @param value ???
         */
        public void addSkippedExistingCount(int value) {
            this.skippedExistingCount += value;
        }

        /**
         * ??Hq Unmatched Count?
         *
         * @return ????
         */
        public int getHqUnmatchedCount() {
            return hqUnmatchedCount;
        }

        /**
         * ?? addHqUnmatchedCount ?????
         *
         * @param value ???
         */
        public void addHqUnmatchedCount(int value) {
            this.hqUnmatchedCount += value;
        }

        /**
         * ??Hq Conflict Count?
         *
         * @return ????
         */
        public int getHqConflictCount() {
            return hqConflictCount;
        }

        /**
         * ?? addHqConflictCount ?????
         *
         * @param value ???
         */
        public void addHqConflictCount(int value) {
            this.hqConflictCount += value;
        }

        /**
         * ??Product Unmatched Count?
         *
         * @return ????
         */
        public int getProductUnmatchedCount() {
            return productUnmatchedCount;
        }

        /**
         * ?? addProductUnmatchedCount ?????
         *
         * @param value ???
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
         * ??Sales Org By Cust Id?
         *
         * @return ????
         */
        public Map<String, String> getSalesOrgByCustId() {
            return salesOrgByCustId;
        }

        /**
         * ??Hq Company Id By Cust Id?
         *
         * @return ????
         */
        public Map<String, Long> getHqCompanyIdByCustId() {
            return hqCompanyIdByCustId;
        }

        /**
         * ??Hq Company Id By Sales Org?
         *
         * @return ????
         */
        public Map<String, Long> getHqCompanyIdBySalesOrg() {
            return hqCompanyIdBySalesOrg;
        }
    }

    private static class CompanyMappingRow {
        /**
         * ??Cust Id?
         *
         * @return ?????
         */
        private String custId;
        private String salesOrg;
        private Long hqCompanyId;

        public String getCustId() {
            return custId;
        }

        /**
         * ??Cust Id?
         *
         * @param custId cust ID
         */
        public void setCustId(String custId) {
            this.custId = custId;
        }

        /**
         * ??Sales Org?
         *
         * @return ?????
         */
        public String getSalesOrg() {
            return salesOrg;
        }

        /**
         * ??Sales Org?
         *
         * @param salesOrg ??
         */
        public void setSalesOrg(String salesOrg) {
            this.salesOrg = salesOrg;
        }

        /**
         * ??Hq Company Id?
         *
         * @return ????
         */
        public Long getHqCompanyId() {
            return hqCompanyId;
        }

        /**
         * ??Hq Company Id?
         *
         * @param hqCompanyId hq Company ID
         */
        public void setHqCompanyId(Long hqCompanyId) {
            this.hqCompanyId = hqCompanyId;
        }
    }

    private static class CrmBarcodeRow {
        /**
         * ??Barcode?
         *
         * @return ?????
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

        public String getBarcode() {
            return barcode;
        }

        /**
         * ??Barcode?
         *
         * @param barcode ??
         */
        public void setBarcode(String barcode) {
            this.barcode = barcode;
        }

        /**
         * ??Deliver Number?
         *
         * @return ?????
         */
        public String getDeliverNumber() {
            return deliverNumber;
        }

        /**
         * ??Deliver Number?
         *
         * @param deliverNumber ??
         */
        public void setDeliverNumber(String deliverNumber) {
            this.deliverNumber = deliverNumber;
        }

        /**
         * ??Cust Id?
         *
         * @return ?????
         */
        public String getCustId() {
            return custId;
        }

        /**
         * ??Cust Id?
         *
         * @param custId cust ID
         */
        public void setCustId(String custId) {
            this.custId = custId;
        }

        /**
         * ??Sales Org?
         *
         * @return ?????
         */
        public String getSalesOrg() {
            return salesOrg;
        }

        /**
         * ??Sales Org?
         *
         * @param salesOrg ??
         */
        public void setSalesOrg(String salesOrg) {
            this.salesOrg = salesOrg;
        }

        /**
         * ??Product Code?
         *
         * @return ?????
         */
        public String getProductCode() {
            return productCode;
        }

        /**
         * ??Product Code?
         *
         * @param productCode ??
         */
        public void setProductCode(String productCode) {
            this.productCode = productCode;
        }

        /**
         * ??Product Name?
         *
         * @return ?????
         */
        public String getProductName() {
            return productName;
        }

        /**
         * ??Product Name?
         *
         * @param productName ??
         */
        public void setProductName(String productName) {
            this.productName = productName;
        }

        /**
         * ??Product Model?
         *
         * @return ?????
         */
        public String getProductModel() {
            return productModel;
        }

        /**
         * ??Product Model?
         *
         * @param productModel ??
         */
        public void setProductModel(String productModel) {
            this.productModel = productModel;
        }

        /**
         * ??Machine No?
         *
         * @return ?????
         */
        public String getMachineNo() {
            return machineNo;
        }

        /**
         * ??Machine No?
         *
         * @param machineNo ??
         */
        public void setMachineNo(String machineNo) {
            this.machineNo = machineNo;
        }

        /**
         * ??Hq Company Id?
         *
         * @return ????
         */
        public Long getHqCompanyId() {
            return hqCompanyId;
        }

        /**
         * ??Hq Company Id?
         *
         * @param hqCompanyId hq Company ID
         */
        public void setHqCompanyId(Long hqCompanyId) {
            this.hqCompanyId = hqCompanyId;
        }

        /**
         * ??Scan Date?
         *
         * @return ????
         */
        public LocalDateTime getScanDate() {
            return scanDate;
        }

        /**
         * ??Scan Date?
         *
         * @param scanDate ??
         */
        public void setScanDate(LocalDateTime scanDate) {
            this.scanDate = scanDate;
        }

        /**
         * ??Add Time?
         *
         * @return ????
         */
        public LocalDateTime getAddTime() {
            return addTime;
        }

        /**
         * ??Add Time?
         *
         * @param addTime ??
         */
        public void setAddTime(LocalDateTime addTime) {
            this.addTime = addTime;
        }

        /**
         * ????Hq Conflict?
         *
         * @return true ??????
         */
        public boolean isHqConflict() {
            return hqConflict;
        }

        /**
         * ??Hq Conflict?
         *
         * @param hqConflict ??
         */
        public void setHqConflict(boolean hqConflict) {
            this.hqConflict = hqConflict;
        }
    }

    private static class ProductSnapshot {
        /**
         * ??Product Code?
         *
         * @return ?????
         */
        private String productCode;
        private String salesOrg;
        private String productName;
        private String productModel;
        private String machineNo;

        public String getProductCode() {
            return productCode;
        }

        /**
         * ??Product Code?
         *
         * @param productCode ??
         */
        public void setProductCode(String productCode) {
            this.productCode = productCode;
        }

        /**
         * ??Sales Org?
         *
         * @return ?????
         */
        public String getSalesOrg() {
            return salesOrg;
        }

        /**
         * ??Sales Org?
         *
         * @param salesOrg ??
         */
        public void setSalesOrg(String salesOrg) {
            this.salesOrg = salesOrg;
        }

        /**
         * ??Product Name?
         *
         * @return ?????
         */
        public String getProductName() {
            return productName;
        }

        /**
         * ??Product Name?
         *
         * @param productName ??
         */
        public void setProductName(String productName) {
            this.productName = productName;
        }

        /**
         * ??Product Model?
         *
         * @return ?????
         */
        public String getProductModel() {
            return productModel;
        }

        /**
         * ??Product Model?
         *
         * @param productModel ??
         */
        public void setProductModel(String productModel) {
            this.productModel = productModel;
        }

        /**
         * ??Machine No?
         *
         * @return ?????
         */
        public String getMachineNo() {
            return machineNo;
        }

        /**
         * ??Machine No?
         *
         * @param machineNo ??
         */
        public void setMachineNo(String machineNo) {
            this.machineNo = machineNo;
        }
    }

    private static class ProductKey {
        /**
         * ?? ProductKey ?????
         *
         * @param productCode ??
         * @param salesOrg ??
         * @return ????
         */
        private final String productCode;
        private final String salesOrg;

        private ProductKey(String productCode, String salesOrg) {
            this.productCode = productCode;
            this.salesOrg = salesOrg;
        }

        /**
         * ?? equals ?????
         *
         * @param o ??
         * @return true ??????
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
                    && Objects.equals(salesOrg, productKey.salesOrg);
        }

        /**
         * ??????h Code?
         *
         * @return ????
         */
        @Override
        public int hashCode() {
            return Objects.hash(productCode, salesOrg);
        }
    }
}
