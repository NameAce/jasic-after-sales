package com.jasic.aftersales.system.service.impl;

import cn.hutool.core.util.StrUtil;
import com.jasic.aftersales.common.exception.ServiceException;
import com.jasic.aftersales.system.domain.vo.MachineBarcodeSyncResultVO;
import com.jasic.aftersales.system.service.IMachineBarcodeSyncService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
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
import java.util.List;
import java.util.regex.Pattern;

/**
 * 条码主数据同步 Service 实现
 *
 * @author Codex
 * @date 2026/04/07
 */
@Service
public class MachineBarcodeSyncServiceImpl implements IMachineBarcodeSyncService {

    private static final Pattern TABLE_NAME_PATTERN = Pattern.compile("^[A-Za-z0-9_$.]+$");
    private static final String CRM_BARCODE_TABLE = "order_deliver_barcode";
    private static final String CRM_DEALER_SCAN_TABLE = "saas_warehouse_scan_outstorage";
    private static final int DEFAULT_BATCH_SIZE = 1000;

    @Resource
    private JdbcTemplate jdbcTemplate;

    @Autowired(required = false)
    @Qualifier("crmJdbcTemplate")
    private JdbcTemplate crmJdbcTemplate;

    @Override
    public MachineBarcodeSyncResultVO fullSyncFromCrm() {
        JdbcTemplate crm = requireCrmJdbcTemplate();
        String barcodeTable = validateTableName(CRM_BARCODE_TABLE, "CRM 主条码表");
        String dealerScanTable = validateTableName(CRM_DEALER_SCAN_TABLE, "CRM 经销商扫码表");
        int batchSize = DEFAULT_BATCH_SIZE;

        MachineBarcodeSyncResultVO result = new MachineBarcodeSyncResultVO();
        result.setStartTime(LocalDateTime.now());
        result.setBarcodeProcessedCount(syncBarcodeBase(crm, barcodeTable, batchSize));
        DealerSyncSummary dealerSummary = syncDealerOutbound(crm, dealerScanTable, batchSize);
        result.setDealerProcessedCount(dealerSummary.getProcessedCount());
        result.setDealerUpdatedCount(dealerSummary.getUpdatedCount());
        result.setEndTime(LocalDateTime.now());
        return result;
    }

    private int syncBarcodeBase(JdbcTemplate crm, String tableName, int batchSize) {
        String lastBarcode = "";
        int total = 0;
        while (true) {
            String sql = "SELECT barcode, cust_id, sales_org, product_code, product_name, product_trumpet, "
                    + "product_model, scan_date, add_time "
                    + "FROM " + tableName + " "
                    + "WHERE barcode IS NOT NULL AND TRIM(barcode) <> '' AND barcode > ? "
                    + "ORDER BY barcode ASC LIMIT ?";
            List<CrmBarcodeRow> rows = crm.query(sql, new Object[]{lastBarcode, batchSize}, CRM_BARCODE_ROW_MAPPER);
            if (rows.isEmpty()) {
                break;
            }
            batchUpsertBarcodeRows(rows);
            total += rows.size();
            lastBarcode = rows.get(rows.size() - 1).getBarcode();
        }
        return total;
    }

    private DealerSyncSummary syncDealerOutbound(JdbcTemplate crm, String tableName, int batchSize) {
        String lastBarcode = "";
        int processedCount = 0;
        int updatedCount = 0;
        while (true) {
            String sql = "SELECT barcode, MAX(add_time) AS dealer_out_date "
                    + "FROM " + tableName + " "
                    + "WHERE barcode IS NOT NULL AND TRIM(barcode) <> '' AND barcode > ? "
                    + "GROUP BY barcode ORDER BY barcode ASC LIMIT ?";
            List<DealerOutboundRow> rows = crm.query(sql, new Object[]{lastBarcode, batchSize}, DEALER_OUTBOUND_ROW_MAPPER);
            if (rows.isEmpty()) {
                break;
            }
            processedCount += rows.size();
            updatedCount += batchUpdateDealerOutbound(rows);
            lastBarcode = rows.get(rows.size() - 1).getBarcode();
        }
        return new DealerSyncSummary(processedCount, updatedCount);
    }

    private void batchUpsertBarcodeRows(List<CrmBarcodeRow> rows) {
        String sql = "INSERT INTO machine_barcode ("
                + "barcode, hq_company_id, cust_id, sales_org, product_code, product_name, product_trumpet, "
                + "product_model, machine_no, scan_date, crm_add_time, last_sync_time, status, create_time, update_time"
                + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW()) "
                + "ON DUPLICATE KEY UPDATE "
                + "hq_company_id = COALESCE(machine_barcode.hq_company_id, VALUES(hq_company_id)), "
                + "cust_id = VALUES(cust_id), "
                + "sales_org = VALUES(sales_org), "
                + "product_code = VALUES(product_code), "
                + "product_name = VALUES(product_name), "
                + "product_trumpet = VALUES(product_trumpet), "
                + "product_model = VALUES(product_model), "
                + "machine_no = VALUES(machine_no), "
                + "scan_date = VALUES(scan_date), "
                + "crm_add_time = VALUES(crm_add_time), "
                + "last_sync_time = VALUES(last_sync_time), "
                + "status = VALUES(status), "
                + "update_time = NOW()";
        LocalDateTime syncTime = LocalDateTime.now();
        jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                CrmBarcodeRow row = rows.get(i);
                ps.setString(1, row.getBarcode());
                ps.setObject(2, null);
                ps.setString(3, row.getCustId());
                ps.setString(4, row.getSalesOrg());
                ps.setString(5, row.getProductCode());
                ps.setString(6, row.getProductName());
                ps.setString(7, row.getProductTrumpet());
                ps.setString(8, row.getProductModel());
                ps.setString(9, row.getProductTrumpet());
                ps.setTimestamp(10, toTimestamp(row.getScanDate()));
                ps.setTimestamp(11, toTimestamp(row.getAddTime()));
                ps.setTimestamp(12, toTimestamp(syncTime));
                ps.setInt(13, 1);
            }

            @Override
            public int getBatchSize() {
                return rows.size();
            }
        });
    }

    private int batchUpdateDealerOutbound(List<DealerOutboundRow> rows) {
        String sql = "UPDATE machine_barcode "
                + "SET dealer_out_date = ?, last_sync_time = ?, update_time = NOW() "
                + "WHERE barcode = ?";
        LocalDateTime syncTime = LocalDateTime.now();
        int[] results = jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                DealerOutboundRow row = rows.get(i);
                ps.setTimestamp(1, toTimestamp(row.getDealerOutDate()));
                ps.setTimestamp(2, toTimestamp(syncTime));
                ps.setString(3, row.getBarcode());
            }

            @Override
            public int getBatchSize() {
                return rows.size();
            }
        });
        int count = 0;
        for (int value : results) {
            if (value > 0) {
                count += value;
            }
        }
        return count;
    }

    private JdbcTemplate requireCrmJdbcTemplate() {
        if (crmJdbcTemplate == null) {
            throw new ServiceException("当前未配置 CRM 数据源，请先完善 jasic.crm.datasource");
        }
        return crmJdbcTemplate;
    }

    private String validateTableName(String tableName, String label) {
        String normalized = StrUtil.trim(tableName);
        if (StrUtil.isBlank(normalized)) {
            throw new ServiceException(label + "未在 MachineBarcodeSyncServiceImpl 中配置");
        }
        if (!TABLE_NAME_PATTERN.matcher(normalized).matches()) {
            throw new ServiceException(label + "配置不合法");
        }
        return normalized;
    }

    private Timestamp toTimestamp(LocalDateTime value) {
        return value == null ? null : Timestamp.valueOf(value);
    }

    private static LocalDateTime toLocalDateTime(ResultSet rs, String column) throws SQLException {
        Timestamp value = rs.getTimestamp(column);
        return value == null ? null : value.toLocalDateTime();
    }

    private static final RowMapper<CrmBarcodeRow> CRM_BARCODE_ROW_MAPPER = (rs, rowNum) -> {
        CrmBarcodeRow row = new CrmBarcodeRow();
        row.setBarcode(StrUtil.trim(rs.getString("barcode")));
        row.setCustId(StrUtil.trim(rs.getString("cust_id")));
        row.setSalesOrg(StrUtil.trim(rs.getString("sales_org")));
        row.setProductCode(StrUtil.trim(rs.getString("product_code")));
        row.setProductName(StrUtil.trim(rs.getString("product_name")));
        row.setProductTrumpet(StrUtil.trim(rs.getString("product_trumpet")));
        row.setProductModel(StrUtil.trim(rs.getString("product_model")));
        row.setScanDate(toLocalDateTime(rs, "scan_date"));
        row.setAddTime(toLocalDateTime(rs, "add_time"));
        return row;
    };

    private static final RowMapper<DealerOutboundRow> DEALER_OUTBOUND_ROW_MAPPER = (rs, rowNum) -> {
        DealerOutboundRow row = new DealerOutboundRow();
        row.setBarcode(StrUtil.trim(rs.getString("barcode")));
        row.setDealerOutDate(toLocalDateTime(rs, "dealer_out_date"));
        return row;
    };

    private static class CrmBarcodeRow {
        private String barcode;
        private String custId;
        private String salesOrg;
        private String productCode;
        private String productName;
        private String productTrumpet;
        private String productModel;
        private LocalDateTime scanDate;
        private LocalDateTime addTime;

        public String getBarcode() { return barcode; }
        public void setBarcode(String barcode) { this.barcode = barcode; }
        public String getCustId() { return custId; }
        public void setCustId(String custId) { this.custId = custId; }
        public String getSalesOrg() { return salesOrg; }
        public void setSalesOrg(String salesOrg) { this.salesOrg = salesOrg; }
        public String getProductCode() { return productCode; }
        public void setProductCode(String productCode) { this.productCode = productCode; }
        public String getProductName() { return productName; }
        public void setProductName(String productName) { this.productName = productName; }
        public String getProductTrumpet() { return productTrumpet; }
        public void setProductTrumpet(String productTrumpet) { this.productTrumpet = productTrumpet; }
        public String getProductModel() { return productModel; }
        public void setProductModel(String productModel) { this.productModel = productModel; }
        public LocalDateTime getScanDate() { return scanDate; }
        public void setScanDate(LocalDateTime scanDate) { this.scanDate = scanDate; }
        public LocalDateTime getAddTime() { return addTime; }
        public void setAddTime(LocalDateTime addTime) { this.addTime = addTime; }
    }

    private static class DealerOutboundRow {
        private String barcode;
        private LocalDateTime dealerOutDate;

        public String getBarcode() { return barcode; }
        public void setBarcode(String barcode) { this.barcode = barcode; }
        public LocalDateTime getDealerOutDate() { return dealerOutDate; }
        public void setDealerOutDate(LocalDateTime dealerOutDate) { this.dealerOutDate = dealerOutDate; }
    }

    private static class DealerSyncSummary {
        private final int processedCount;
        private final int updatedCount;

        private DealerSyncSummary(int processedCount, int updatedCount) {
            this.processedCount = processedCount;
            this.updatedCount = updatedCount;
        }

        public int getProcessedCount() {
            return processedCount;
        }

        public int getUpdatedCount() {
            return updatedCount;
        }
    }
}
