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
 * @author Zoro
 * @date 2026/04/12
 */
@Slf4j
@Service
public class MachineBarcodeSyncServiceImpl implements IMachineBarcodeSyncService {

    /**TABLE_NAME_PATTERN 常量，用于固定当前类内部复用的业务编码、默认值或配置边界。*/
    private static final Pattern TABLE_NAME_PATTERN = Pattern.compile("^[A-Za-z0-9_$.]+$");
    /**CRM_BARCODE_TABLE 常量，用于固定当前类内部复用的业务编码、默认值或配置边界。*/
    private static final String CRM_BARCODE_TABLE = "order_deliver_barcode";
    /**CRM_PRODUCT_TABLE 常量，用于固定当前类内部复用的业务编码、默认值或配置边界。*/
    private static final String CRM_PRODUCT_TABLE = "sap_product_info";
    /**LOCAL_COMPANY_MAPPING_TABLE 常量，用于固定当前类内部复用的业务编码、默认值或配置边界。*/
    private static final String LOCAL_COMPANY_MAPPING_TABLE = "crm_company_mapping";
    /**DEFAULT_BATCH_SIZE 常量，用于固定当前类内部复用的业务编码、默认值或配置边界。*/
    private static final int DEFAULT_BATCH_SIZE = 1000;
    /**PROGRESS_LOG_INTERVAL 常量，用于固定当前类内部复用的业务编码、默认值或配置边界。*/
    private static final int PROGRESS_LOG_INTERVAL = 10000;

    /**jdbcTemplate 依赖，用于协同完成当前业务流程中的数据访问、规则校验或状态处理。*/
    @Resource(name = "jdbcTemplate")
    private JdbcTemplate jdbcTemplate;

    /**
     * JDBC模板模板依赖。
     *
     * @return 业务处理结果
     */
    @Resource(name = "crmJdbcTemplate")
    private JdbcTemplate crmJdbcTemplate;

    /**
     * 处理fullSyncFromCrm业务逻辑。
     *
     * <p>说明：该方法用于执行业务流程编排，确保调用链路清晰可维护。</p>
     * @return 业务处理结果
     */
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
     * 获取EarliestAddTime。
     *
     * @return 业务处理结果
     */
    @Override
    public LocalDateTime getEarliestAddTime() {
        JdbcTemplate crm = requireCrmJdbcTemplate();
        String barcodeTable = validateTableName(CRM_BARCODE_TABLE, "CRM 主条码表");
        String sql = "SELECT MIN(add_time) FROM " + barcodeTable
                + " WHERE barcode IS NOT NULL AND TRIM(barcode) <> '' AND add_time IS NOT NULL";
        Timestamp timestamp = crm.queryForObject(sql, Timestamp.class);
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    /**
     * 同步ByAddTimeRange。
     *
     * @param startInclusive startInclusive，当前业务处理所需的输入值。
     * @param endExclusive endExclusive，当前业务处理所需的输入值。
     * @return 业务处理结果
     */
    @Override
    public MachineBarcodeSyncResultVO syncByAddTimeRange(LocalDateTime startInclusive, LocalDateTime endExclusive) {
        if (startInclusive == null || endExclusive == null || !startInclusive.isBefore(endExclusive)) {
            throw new ServiceException("条码同步时间范围不合法");
        }
        log.info("条码同步开始，数据范围=[{}, {})", startInclusive, endExclusive);

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
     * 同步条码基础。
     *
     * @param crm crm，当前业务处理所需的输入值。
     * @param barcodeTable 业务编码，用于匹配枚举、配置或外部系统数据。
     * @param startInclusive startInclusive，当前业务处理所需的输入值。
     * @param endExclusive endExclusive，当前业务处理所需的输入值。
     * @param mappingSnapshot 业务映射数据，用于批量组装或快速查找。
     * @param productSnapshotMap 业务映射数据，用于批量组装或快速查找。
     * @return 业务处理结果
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
     * mergeSummary。
     *
     * @param target target，当前业务处理所需的输入值。
     * @param source source，当前业务处理所需的输入值。
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
     * nextMonthStart。
     *
     * @param current current，当前业务处理所需的输入值。
     * @return 业务处理结果
     */
    private LocalDateTime nextMonthStart(LocalDateTime current) {
        return current.toLocalDate()
                .withDayOfMonth(1)
                .plusMonths(1)
                .atStartOfDay();
    }

    /**
     * load公司Mappings。
     *
     * @param tableName tableName，当前业务处理所需的输入值。
     * @return 业务处理结果
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
     * loadAllProductSnapshots。
     *
     * @param crm crm，当前业务处理所需的输入值。
     * @param tableName tableName，当前业务处理所需的输入值。
     * @return 业务处理结果
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
     * enrich公司Mapping。
     *
     * @param row row，当前业务处理所需的输入值。
     * @param mappingSnapshot 业务映射数据，用于批量组装或快速查找。
     * @param summary summary，当前业务处理所需的输入值。
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
     * enrichProductInfo。
     *
     * @param row row，当前业务处理所需的输入值。
     * @param productMap 业务映射数据，用于批量组装或快速查找。
     * @param summary summary，当前业务处理所需的输入值。
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
     * batch新增条码Rows。
     *
     * @param rows rows，当前业务处理所需的输入值。
     * @param syncTime 时间值，用于业务节点记录或时效判断。
     * @param summary summary，当前业务处理所需的输入值。
     */
    private void batchInsertBarcodeRows(List<CrmBarcodeRow> rows, LocalDateTime syncTime, SyncSummary summary) {
        String sql = "INSERT IGNORE INTO machine_barcode ("
                + "barcode, deliver_number, hq_company_id, cust_id, sales_org, product_code, "
                + "product_name, machine_no, product_model, scan_date, crm_add_time, last_sync_time, status, create_time, update_time"
                + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())";
        int[] results = jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
            /**
     * setValues。
     *
     * @param ps ps，当前业务处理所需的输入值。
     * @param i i，当前业务处理所需的输入值。
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
     * 获取BatchSize。
     *
     * @return 业务处理结果
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
     * requireCRMJDBC模板。
     *
     * @return 业务处理结果
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
     * @param tableName tableName，当前业务处理所需的输入值。
     * @param label label，当前业务处理所需的输入值。
     * @return 业务处理结果
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
     * toTimestamp。
     *
     * @param value value，当前业务处理所需的输入值。
     * @return 业务处理结果
     */
    private Timestamp toTimestamp(LocalDateTime value) {
        return value == null ? null : Timestamp.valueOf(value);
    }

    /**
     * 规范化NullableText。
     *
     * @param value value，当前业务处理所需的输入值。
     * @return 业务处理结果
     */
    private String normalizeNullableText(String value) {
        String normalized = StrUtil.trim(value);
        return StrUtil.isBlank(normalized) ? null : normalized;
    }

    /**
     * toLocalDateTime。
     *
     * @param rs rs，当前业务处理所需的输入值。
     * @param column column，当前业务处理所需的输入值。
     * @return 业务处理结果
     */
    private static LocalDateTime toLocalDateTime(ResultSet rs, String column) throws SQLException {
        Timestamp value = rs.getTimestamp(column);
        return value == null ? null : value.toLocalDateTime();
    }

    /**CRM_BARCODE_ROW_MAPPER 常量，用于固定当前类内部复用的业务编码、默认值或配置边界。*/
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

    /**PRODUCT_SNAPSHOT_ROW_MAPPER 常量，用于固定当前类内部复用的业务编码、默认值或配置边界。*/
    private static final RowMapper<ProductSnapshot> PRODUCT_SNAPSHOT_ROW_MAPPER = (rs, rowNum) -> {
        ProductSnapshot snapshot = new ProductSnapshot();
        snapshot.setProductCode(StrUtil.trim(rs.getString("product_numeric")));
        snapshot.setSalesOrg(StrUtil.trim(rs.getString("sales_org")));
        snapshot.setProductName(StrUtil.trim(rs.getString("product_name")));
        snapshot.setProductModel(StrUtil.trim(rs.getString("product_model")));
        snapshot.setMachineNo(StrUtil.trim(rs.getString("product_trumpet")));
        return snapshot;
    };

    /**COMPANY_MAPPING_ROW_MAPPER 常量，用于固定当前类内部复用的业务编码、默认值或配置边界。*/
    private static final RowMapper<CompanyMappingRow> COMPANY_MAPPING_ROW_MAPPER = (rs, rowNum) -> {
        CompanyMappingRow row = new CompanyMappingRow();
        row.setCustId(StrUtil.trim(rs.getString("cust_id")));
        row.setSalesOrg(StrUtil.trim(rs.getString("sales_org")));
        long hqCompanyId = rs.getLong("hq_company_id");
        row.setHqCompanyId(rs.wasNull() ? null : hqCompanyId);
        return row;
    };

    /**SyncSummary 服务实现，负责业务校验、状态流转、数据持久化和跨模块协同。

@author Zoro*/
    private static class SyncSummary {
        /**
     * LocalDateTime字段。
     *
     * @return 业务处理结果
         */
        private LocalDateTime startTime;
        /**endTime 字段，用于当前类内部业务处理。*/
        private LocalDateTime endTime;
        /**processedCount 字段，用于当前类内部业务处理。*/
        private int processedCount;
        /**insertedCount 字段，用于当前类内部业务处理。*/
        private int insertedCount;
        /**skippedExistingCount 字段，用于当前类内部业务处理。*/
        private int skippedExistingCount;
        /**hqUnmatchedCount 字段，用于当前类内部业务处理。*/
        private int hqUnmatchedCount;
        /**hqConflictCount 字段，用于当前类内部业务处理。*/
        private int hqConflictCount;
        /**productUnmatchedCount 字段，用于当前类内部业务处理。*/
        private int productUnmatchedCount;

        /**
         * 获取StartTime相关数据。
         *
         * <p>说明：该方法用于执行业务流程编排，确保调用链路清晰可维护。</p>
         * @return 业务处理结果
         */
        public LocalDateTime getStartTime() {
            return startTime;
        }

        /**
     * setStartTime。
     *
     * @param startTime 时间值，用于业务节点记录或时效判断。
         */
        public void setStartTime(LocalDateTime startTime) {
            this.startTime = startTime;
        }

        /**
     * 获取EndTime。
     *
     * @return 业务处理结果
         */
        public LocalDateTime getEndTime() {
            return endTime;
        }

        /**
     * setEndTime。
     *
     * @param endTime 时间值，用于业务节点记录或时效判断。
         */
        public void setEndTime(LocalDateTime endTime) {
            this.endTime = endTime;
        }

        /**
     * 获取ProcessedCount。
     *
     * @return 业务处理结果
         */
        public int getProcessedCount() {
            return processedCount;
        }

        /**
     * 新增ProcessedCount。
     *
     * @param value value，当前业务处理所需的输入值。
         */
        public void addProcessedCount(int value) {
            this.processedCount += value;
        }

        /**
     * 获取InsertedCount。
     *
     * @return 业务处理结果
         */
        public int getInsertedCount() {
            return insertedCount;
        }

        /**
     * 新增InsertedCount。
     *
     * @param value value，当前业务处理所需的输入值。
         */
        public void addInsertedCount(int value) {
            this.insertedCount += value;
        }

        /**
     * 获取SkippedExistingCount。
     *
     * @return 业务处理结果
         */
        public int getSkippedExistingCount() {
            return skippedExistingCount;
        }

        /**
     * 新增SkippedExistingCount。
     *
     * @param value value，当前业务处理所需的输入值。
         */
        public void addSkippedExistingCount(int value) {
            this.skippedExistingCount += value;
        }

        /**
     * 获取总部UnmatchedCount。
     *
     * @return 业务处理结果
         */
        public int getHqUnmatchedCount() {
            return hqUnmatchedCount;
        }

        /**
     * 新增总部UnmatchedCount。
     *
     * @param value value，当前业务处理所需的输入值。
         */
        public void addHqUnmatchedCount(int value) {
            this.hqUnmatchedCount += value;
        }

        /**
     * 获取总部ConflictCount。
     *
     * @return 业务处理结果
         */
        public int getHqConflictCount() {
            return hqConflictCount;
        }

        /**
     * 新增总部ConflictCount。
     *
     * @param value value，当前业务处理所需的输入值。
         */
        public void addHqConflictCount(int value) {
            this.hqConflictCount += value;
        }

        /**
     * 获取ProductUnmatchedCount。
     *
     * @return 业务处理结果
         */
        public int getProductUnmatchedCount() {
            return productUnmatchedCount;
        }

        /**
     * 新增ProductUnmatchedCount。
     *
     * @param value value，当前业务处理所需的输入值。
         */
        public void addProductUnmatchedCount(int value) {
            this.productUnmatchedCount += value;
        }
    }

    /**CompanyMappingSnapshot 服务实现，负责业务校验、状态流转、数据持久化和跨模块协同。

@author Zoro*/
    private static class CompanyMappingSnapshot {
        /**salesOrgByCustId 字段，用于当前类内部业务处理。*/
        private final Map<String, String> salesOrgByCustId = new LinkedHashMap<>();
        /**hqCompanyIdByCustId 字段，用于当前类内部业务处理。*/
        private final Map<String, Long> hqCompanyIdByCustId = new LinkedHashMap<>();
        /**hqCompanyIdBySalesOrg 字段，用于当前类内部业务处理。*/
        private final Map<String, Long> hqCompanyIdBySalesOrg = new LinkedHashMap<>();

        /**
     * 获取SalesOrgByCustID。
     *
     * @return 业务处理结果
         */
        public Map<String, String> getSalesOrgByCustId() {
            return salesOrgByCustId;
        }

        /**
     * 获取总部公司IDByCustID。
     *
     * @return 业务处理结果
         */
        public Map<String, Long> getHqCompanyIdByCustId() {
            return hqCompanyIdByCustId;
        }

        /**
     * 获取总部公司IDBySalesOrg。
     *
     * @return 业务处理结果
         */
        public Map<String, Long> getHqCompanyIdBySalesOrg() {
            return hqCompanyIdBySalesOrg;
        }
    }

    /**CompanyMappingRow 服务实现，负责业务校验、状态流转、数据持久化和跨模块协同。

@author Zoro*/
    private static class CompanyMappingRow {
        /**
     * 内部字段，用于保存当前流程需要复用的业务值。
     *
     * @return 业务处理结果
         */
        private String custId;
        /**salesOrg 字段，用于当前类内部业务处理。*/
        private String salesOrg;
        /**hqCompanyId 字段，用于当前类内部业务处理。*/
        private Long hqCompanyId;

        /**
         * 获取CustId相关数据。
         *
         * <p>说明：该方法用于执行业务流程编排，确保调用链路清晰可维护。</p>
         * @return 业务处理结果
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
     * @return 业务处理结果
         */
        public String getSalesOrg() {
            return salesOrg;
        }

        /**
     * setSalesOrg。
     *
     * @param salesOrg salesOrg，当前业务处理所需的输入值。
         */
        public void setSalesOrg(String salesOrg) {
            this.salesOrg = salesOrg;
        }

        /**
     * 获取总部公司ID。
     *
     * @return 业务处理结果
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

    /**CrmBarcodeRow 服务实现，负责业务校验、状态流转、数据持久化和跨模块协同。

@author Zoro*/
    private static class CrmBarcodeRow {
        /**
     * 内部字段，用于保存当前流程需要复用的业务值。
     *
     * @return 业务处理结果
         */
        private String barcode;
        /**deliverNumber 字段，用于当前类内部业务处理。*/
        private String deliverNumber;
        /**custId 字段，用于当前类内部业务处理。*/
        private String custId;
        /**salesOrg 字段，用于当前类内部业务处理。*/
        private String salesOrg;
        /**productCode 字段，用于当前类内部业务处理。*/
        private String productCode;
        /**productName 字段，用于当前类内部业务处理。*/
        private String productName;
        /**productModel 字段，用于当前类内部业务处理。*/
        private String productModel;
        /**machineNo 字段，用于当前类内部业务处理。*/
        private String machineNo;
        /**hqCompanyId 字段，用于当前类内部业务处理。*/
        private Long hqCompanyId;
        /**scanDate 字段，用于当前类内部业务处理。*/
        private LocalDateTime scanDate;
        /**addTime 字段，用于当前类内部业务处理。*/
        private LocalDateTime addTime;
        /**hqConflict 字段，用于当前类内部业务处理。*/
        private boolean hqConflict;

        /**
         * 获取Barcode相关数据。
         *
         * <p>说明：该方法用于执行业务流程编排，确保调用链路清晰可维护。</p>
         * @return 业务处理结果
         */
        public String getBarcode() {
            return barcode;
        }

        /**
     * set条码。
     *
     * @param barcode 业务编码，用于匹配枚举、配置或外部系统数据。
         */
        public void setBarcode(String barcode) {
            this.barcode = barcode;
        }

        /**
     * 获取DeliverNumber。
     *
     * @return 业务处理结果
         */
        public String getDeliverNumber() {
            return deliverNumber;
        }

        /**
     * setDeliverNumber。
     *
     * @param deliverNumber deliverNumber，当前业务处理所需的输入值。
         */
        public void setDeliverNumber(String deliverNumber) {
            this.deliverNumber = deliverNumber;
        }

        /**
     * 获取CustID。
     *
     * @return 业务处理结果
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
     * @return 业务处理结果
         */
        public String getSalesOrg() {
            return salesOrg;
        }

        /**
     * setSalesOrg。
     *
     * @param salesOrg salesOrg，当前业务处理所需的输入值。
         */
        public void setSalesOrg(String salesOrg) {
            this.salesOrg = salesOrg;
        }

        /**
     * 获取Product编码。
     *
     * @return 业务处理结果
         */
        public String getProductCode() {
            return productCode;
        }

        /**
     * setProduct编码。
     *
     * @param productCode 业务编码，用于匹配枚举、配置或外部系统数据。
         */
        public void setProductCode(String productCode) {
            this.productCode = productCode;
        }

        /**
     * 获取Product名称。
     *
     * @return 业务处理结果
         */
        public String getProductName() {
            return productName;
        }

        /**
     * setProduct名称。
     *
     * @param productName productName，当前业务处理所需的输入值。
         */
        public void setProductName(String productName) {
            this.productName = productName;
        }

        /**
     * 获取ProductModel。
     *
     * @return 业务处理结果
         */
        public String getProductModel() {
            return productModel;
        }

        /**
     * setProductModel。
     *
     * @param productModel productModel，当前业务处理所需的输入值。
         */
        public void setProductModel(String productModel) {
            this.productModel = productModel;
        }

        /**
     * 获取机器编号。
     *
     * @return 业务处理结果
         */
        public String getMachineNo() {
            return machineNo;
        }

        /**
     * set机器编号。
     *
     * @param machineNo machineNo，当前业务处理所需的输入值。
         */
        public void setMachineNo(String machineNo) {
            this.machineNo = machineNo;
        }

        /**
     * 获取总部公司ID。
     *
     * @return 业务处理结果
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
     * @return 业务处理结果
         */
        public LocalDateTime getScanDate() {
            return scanDate;
        }

        /**
     * set扫描Date。
     *
     * @param scanDate 时间值，用于业务节点记录或时效判断。
         */
        public void setScanDate(LocalDateTime scanDate) {
            this.scanDate = scanDate;
        }

        /**
     * 获取AddTime。
     *
     * @return 业务处理结果
         */
        public LocalDateTime getAddTime() {
            return addTime;
        }

        /**
     * setAddTime。
     *
     * @param addTime 时间值，用于业务节点记录或时效判断。
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
     * @param hqConflict hqConflict，当前业务处理所需的输入值。
         */
        public void setHqConflict(boolean hqConflict) {
            this.hqConflict = hqConflict;
        }
    }

    /**ProductSnapshot 服务实现，负责业务校验、状态流转、数据持久化和跨模块协同。

@author Zoro*/
    private static class ProductSnapshot {
        /**
     * 内部字段，用于保存当前流程需要复用的业务值。
     *
     * @return 业务处理结果
         */
        private String productCode;
        /**salesOrg 字段，用于当前类内部业务处理。*/
        private String salesOrg;
        /**productName 字段，用于当前类内部业务处理。*/
        private String productName;
        /**productModel 字段，用于当前类内部业务处理。*/
        private String productModel;
        /**machineNo 字段，用于当前类内部业务处理。*/
        private String machineNo;

        /**
         * 获取ProductCode相关数据。
         *
         * <p>说明：该方法用于执行业务流程编排，确保调用链路清晰可维护。</p>
         * @return 业务处理结果
         */
        public String getProductCode() {
            return productCode;
        }

        /**
     * setProduct编码。
     *
     * @param productCode 业务编码，用于匹配枚举、配置或外部系统数据。
         */
        public void setProductCode(String productCode) {
            this.productCode = productCode;
        }

        /**
     * 获取SalesOrg。
     *
     * @return 业务处理结果
         */
        public String getSalesOrg() {
            return salesOrg;
        }

        /**
     * setSalesOrg。
     *
     * @param salesOrg salesOrg，当前业务处理所需的输入值。
         */
        public void setSalesOrg(String salesOrg) {
            this.salesOrg = salesOrg;
        }

        /**
     * 获取Product名称。
     *
     * @return 业务处理结果
         */
        public String getProductName() {
            return productName;
        }

        /**
     * setProduct名称。
     *
     * @param productName productName，当前业务处理所需的输入值。
         */
        public void setProductName(String productName) {
            this.productName = productName;
        }

        /**
     * 获取ProductModel。
     *
     * @return 业务处理结果
         */
        public String getProductModel() {
            return productModel;
        }

        /**
     * setProductModel。
     *
     * @param productModel productModel，当前业务处理所需的输入值。
         */
        public void setProductModel(String productModel) {
            this.productModel = productModel;
        }

        /**
     * 获取机器编号。
     *
     * @return 业务处理结果
         */
        public String getMachineNo() {
            return machineNo;
        }

        /**
     * set机器编号。
     *
     * @param machineNo machineNo，当前业务处理所需的输入值。
         */
        public void setMachineNo(String machineNo) {
            this.machineNo = machineNo;
        }
    }

    /**ProductKey 服务实现，负责业务校验、状态流转、数据持久化和跨模块协同。

@author Zoro*/
    private static class ProductKey {
        /**
     * 内部字段，用于保存当前流程需要复用的业务值。
     *
     * @param productCode 业务编码，用于匹配枚举、配置或外部系统数据。
     * @param salesOrg salesOrg，当前业务处理所需的输入值。
     * @return 业务处理结果
         */
        private final String productCode;
        /**salesOrg 字段，用于当前类内部业务处理。*/
        private final String salesOrg;

        /**
     * 构造机器条码同步实例。
     *
     * @param productCode 业务编码，用于匹配枚举、配置或外部系统数据。
     * @param salesOrg salesOrg，当前业务处理所需的输入值。
     * @return 业务处理结果
         */
        private ProductKey(String productCode, String salesOrg) {
            this.productCode = productCode;
            this.salesOrg = salesOrg;
        }

        /**
     * equals。
     *
     * @param o o，当前业务处理所需的输入值。
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
     * 判断是否存在h编码。
     *
     * @return 业务处理结果
         */
        @Override
        public int hashCode() {
            return Objects.hash(productCode, salesOrg);
        }
    }
}


