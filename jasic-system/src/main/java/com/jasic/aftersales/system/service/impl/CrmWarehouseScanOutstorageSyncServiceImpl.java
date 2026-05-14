package com.jasic.aftersales.system.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jasic.aftersales.common.exception.ServiceException;
import com.jasic.aftersales.system.domain.entity.CrmWarehouseScanOutstorageSnapshot;
import com.jasic.aftersales.system.domain.entity.MachineBarcode;
import com.jasic.aftersales.system.domain.vo.CrmWarehouseScanOutstorageSyncSummaryVO;
import com.jasic.aftersales.system.mapper.MachineBarcodeMapper;
import com.jasic.aftersales.system.service.ICrmWarehouseScanOutstorageSyncService;
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
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * CRM 閿€鍞嚭搴撴壂鐮佸悓姝ユ湇鍔″疄鐜般€?
 *
 * <p>瀹炵幇鎷嗕负涓や釜闃舵锛?/p>
 * <ul>
 *     <li>鍏堟寜婧愯〃涓婚敭澧為噺钀藉湴閿€鍞嚭搴撴壂鐮佹槑缁嗗揩鐓э紱</li>
 *     <li>鍐嶅熀浜庢湰鍦板揩鐓ф寜鏉＄爜鑱氬悎鏈€鏃╂壂鐮佹椂闂达紝瑕嗙洊鍥炲啓鏈湴閿€鍞渶鍚庡嚭搴撴棩鏈熴€?/li>
 * </ul>
 *
 * @author Codex
 * @date 2026/04/12
 */
@Service
public class CrmWarehouseScanOutstorageSyncServiceImpl implements ICrmWarehouseScanOutstorageSyncService {

    private static final String CRM_OUTSTORAGE_TABLE = "saas_warehouse_scan_outstorage";
    private static final int DEFAULT_BATCH_SIZE = 1000;

    @Resource(name = "jdbcTemplate")
    private JdbcTemplate jdbcTemplate;

    /**
     * ???????
     *
     * @return ????
     */
    @Resource(name = "crmJdbcTemplate")
    private JdbcTemplate crmJdbcTemplate;

    @Resource
    private MachineBarcodeMapper machineBarcodeMapper;

    @Override
    public CrmWarehouseScanOutstorageSyncSummaryVO syncIncremental() {
        // ?????????????????????????????
        JdbcTemplate crm = requireCrmJdbcTemplate();
        Long lastSourceId = getLocalMaxSourceId();
        // 褰撳墠婧愯〃鎸?scan_outstorage_id 閫掑杩藉姞锛屾晠浣跨敤涓婚敭娓告爣鎷夊彇澧為噺鏄庣粏銆?
        String sql = "SELECT scan_outstorage_id, ware_id, warehouse_id, scan_code, scan_date, cust_id, product_numeric "
                + "FROM " + CRM_OUTSTORAGE_TABLE + " "
                + "WHERE scan_outstorage_id > ? "
                + "ORDER BY scan_outstorage_id ASC";

        List<CrmWarehouseScanOutstorageSnapshot> batch = new ArrayList<>(DEFAULT_BATCH_SIZE);
        SyncCounter counter = new SyncCounter();
        LocalDateTime syncTime = LocalDateTime.now();
        crm.query(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
            ps.setFetchSize(Integer.MIN_VALUE);
            ps.setLong(1, lastSourceId == null ? 0L : lastSourceId);
            return ps;
        }, rs -> {
            while (rs.next()) {
                CrmWarehouseScanOutstorageSnapshot row = CRM_OUTSTORAGE_ROW_MAPPER.mapRow(rs, rs.getRow());
                if (row == null || row.getSourceId() == null) {
                    continue;
                }
                batch.add(row);
                counter.syncedDetailCount++;
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

        if (CollUtil.isNotEmpty(counter.changedBarcodes)) {
            // 浠呴噸绠楁湰杞秹鍙婄殑鏉＄爜锛岄伩鍏嶆瘡娆″叏琛ㄨ仛鍚堛€?
            projectLastOutDate(counter.changedBarcodes, counter);
        }

        CrmWarehouseScanOutstorageSyncSummaryVO summary = new CrmWarehouseScanOutstorageSyncSummaryVO();
        summary.setSyncedDetailCount(counter.syncedDetailCount);
        summary.setAffectedBarcodeCount(counter.changedBarcodes.size());
        summary.setUpdatedMachineBarcodeCount(counter.updatedMachineBarcodeCount);
        summary.setUnmatchedBarcodeCount(counter.unmatchedBarcodeCount);
        summary.setLatestSourceId(counter.latestSourceId);
        return summary;
    }

    /**
     * ?? flushBatch ?????
     *
     * @param rows ??
     * @param syncTime ??
     * @param counter ??
     */
    private void flushBatch(List<CrmWarehouseScanOutstorageSnapshot> rows, LocalDateTime syncTime, SyncCounter counter) {
        // 鏄庣粏蹇収淇濈暀 CRM 鍘熷璁板綍锛岄噰鐢?source_id 骞傜瓑鏇存柊锛屽吋瀹归噸澶嶆墽琛屻€?
        String sql = "INSERT INTO crm_warehouse_scan_outstorage_snapshot ("
                + "source_id, ware_id, warehouse_id, scan_code, scan_date, cust_id, product_numeric, last_sync_time, create_time, update_time"
                + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW()) "
                + "ON DUPLICATE KEY UPDATE "
                + "ware_id = VALUES(ware_id), "
                + "warehouse_id = VALUES(warehouse_id), "
                + "scan_code = VALUES(scan_code), "
                + "scan_date = VALUES(scan_date), "
                + "cust_id = VALUES(cust_id), "
                + "product_numeric = VALUES(product_numeric), "
                + "last_sync_time = VALUES(last_sync_time), "
                + "update_time = NOW()";
        jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
            /**
             * ??Values?
             *
             * @param ps ??
             * @param i ??
             */
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                CrmWarehouseScanOutstorageSnapshot row = rows.get(i);
                ps.setObject(1, row.getSourceId());
                ps.setObject(2, row.getWareId());
                ps.setObject(3, row.getWarehouseId());
                ps.setString(4, row.getScanCode());
                ps.setTimestamp(5, toTimestamp(row.getScanDate()));
                ps.setObject(6, row.getCustId());
                ps.setString(7, row.getProductNumeric());
                ps.setTimestamp(8, toTimestamp(syncTime));
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

        for (CrmWarehouseScanOutstorageSnapshot row : rows) {
            if (StrUtil.isNotBlank(row.getScanCode())) {
                counter.changedBarcodes.add(row.getScanCode());
            }
            // 鎵规鎸?source_id 鍗囧簭澶勭悊锛屾渶鍚庝竴涓€煎嵆褰撳墠鍚屾娓告爣涓婄晫銆?
            counter.latestSourceId = row.getSourceId();
        }
    }

    /**
     * ?? projectLastOutDate ?????
     *
     * @param barcodeSet ??
     * @param counter ??
     */
    private void projectLastOutDate(Set<String> barcodeSet, SyncCounter counter) {
        List<String> barcodes = new ArrayList<>(barcodeSet);
        int batchSize = 500;
        for (int index = 0; index < barcodes.size(); index += batchSize) {
            List<String> slice = barcodes.subList(index, Math.min(index + batchSize, barcodes.size()));
            Map<String, LocalDateTime> minScanDateMap = queryMinScanDateMap(slice);
            // 鍙洿鏂版湰鍦板凡瀛樺湪鐨勬潯鐮佹。妗堬紱鏈尮閰嶆潯鐮佷粎鍋氳鏁帮紝涓嶈嚜鍔ㄥ缓妗ｃ€?
            LambdaQueryWrapper<MachineBarcode> wrapper = new LambdaQueryWrapper<>();
            wrapper.in(MachineBarcode::getBarcode, slice);
            // ??????????????????????????
            List<MachineBarcode> existingBarcodes = machineBarcodeMapper.selectList(wrapper);
            Set<String> existingBarcodeSet = existingBarcodes.stream()
                    .map(MachineBarcode::getBarcode)
                    .filter(StrUtil::isNotBlank)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            counter.unmatchedBarcodeCount += slice.size() - existingBarcodeSet.size();
            if (existingBarcodeSet.isEmpty()) {
                continue;
            }

            List<Map.Entry<String, LocalDateTime>> updates = existingBarcodes.stream()
                    .map(item -> new java.util.AbstractMap.SimpleEntry<>(
                            item.getBarcode(),
                            minScanDateMap.getOrDefault(item.getBarcode(), item.getScanDate())
                    ))
                    .filter(item -> StrUtil.isNotBlank(item.getKey()) && item.getValue() != null)
                    .collect(Collectors.toList());
            if (updates.isEmpty()) {
                continue;
            }
            // dealer_out_date 鍏佽瑕嗙洊宸叉湁鍊硷紝鍥犳杩欓噷鐩存帴鎸夋渶鏂拌仛鍚堢粨鏋滃洖鍐欍€?
            String updateSql = "UPDATE machine_barcode SET last_out_date = ?, last_sync_time = NOW(), update_time = NOW() "
                    + "WHERE barcode = ?";
            jdbcTemplate.batchUpdate(updateSql, new BatchPreparedStatementSetter() {
                /**
                 * ??Values?
                 *
                 * @param ps ??
                 * @param i ??
                 */
                @Override
                public void setValues(PreparedStatement ps, int i) throws SQLException {
                    Map.Entry<String, LocalDateTime> item = updates.get(i);
                    ps.setTimestamp(1, toTimestamp(item.getValue()));
                    ps.setString(2, item.getKey());
                }

                /**
                 * ??Batch Size?
                 *
                 * @return ????
                 */
                @Override
                public int getBatchSize() {
                    return updates.size();
                }
            });
            counter.updatedMachineBarcodeCount += updates.size();
        }
    }

    /**
     * ?????
     *
     * @param barcodes ??
     * @return ????
     */
    private Map<String, LocalDateTime> queryMinScanDateMap(List<String> barcodes) {
        if (CollUtil.isEmpty(barcodes)) {
            return Collections.emptyMap();
        }
        String placeholders = barcodes.stream().map(item -> "?").collect(Collectors.joining(","));
        // 涓氬姟鍙ｅ緞鏄庣‘鍙栧悓鏉＄爜鏈€鏃╂壂鐮佹椂闂达紝浣滀负閿€鍞渶鍚庡嚭搴撴棩鏈熷啓鍥炴湰鍦版潯鐮佹。妗堛€?
        String sql = "SELECT scan_code, MIN(scan_date) AS last_out_date "
                + "FROM crm_warehouse_scan_outstorage_snapshot "
                + "WHERE scan_code IN (" + placeholders + ") AND scan_date IS NOT NULL "
                + "GROUP BY scan_code";
        List<Map.Entry<String, LocalDateTime>> rows = jdbcTemplate.query(sql, ps -> {
            for (int i = 0; i < barcodes.size(); i++) {
                ps.setString(i + 1, barcodes.get(i));
            }
        }, (rs, rowNum) -> new java.util.AbstractMap.SimpleEntry<>(
                StrUtil.trim(rs.getString("scan_code")),
                toLocalDateTime(rs, "last_out_date")
        ));
        Map<String, LocalDateTime> result = new LinkedHashMap<>();
        for (Map.Entry<String, LocalDateTime> row : rows) {
            if (StrUtil.isNotBlank(row.getKey()) && row.getValue() != null) {
                result.put(row.getKey(), row.getValue());
            }
        }
        return result;
    }

    /**
     * ??Local Max Source Id?
     *
     * @return ????
     */
    private Long getLocalMaxSourceId() {
        // 蹇収琛ㄤ繚瀛樿繃鐨勬渶澶?source_id 鍗冲綋鍓嶅閲忓悓姝ユ父鏍囥€?
        String sql = "SELECT MAX(source_id) FROM crm_warehouse_scan_outstorage_snapshot";
        Long value = jdbcTemplate.queryForObject(sql, Long.class);
        return value == null ? 0L : value;
    }

    /**
     * ??????????
     *
     * @return ????
     */
    private JdbcTemplate requireCrmJdbcTemplate() {
        if (crmJdbcTemplate == null) {
            throw new ServiceException("褰撳墠鏈厤缃鎴峰叧绯荤鐞嗭紙CRM锛夋暟鎹簮锛岃鍏堝畬鍠?jasic.crm.datasource");
        }
        return crmJdbcTemplate;
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

    private static final RowMapper<CrmWarehouseScanOutstorageSnapshot> CRM_OUTSTORAGE_ROW_MAPPER = (rs, rowNum) -> {
        CrmWarehouseScanOutstorageSnapshot snapshot = new CrmWarehouseScanOutstorageSnapshot();
        long sourceId = rs.getLong("scan_outstorage_id");
        snapshot.setSourceId(rs.wasNull() ? null : sourceId);
        long wareId = rs.getLong("ware_id");
        snapshot.setWareId(rs.wasNull() ? null : wareId);
        long warehouseId = rs.getLong("warehouse_id");
        snapshot.setWarehouseId(rs.wasNull() ? null : warehouseId);
        snapshot.setScanCode(StrUtil.trim(rs.getString("scan_code")));
        snapshot.setScanDate(toLocalDateTime(rs, "scan_date"));
        long custId = rs.getLong("cust_id");
        snapshot.setCustId(rs.wasNull() ? null : custId);
        snapshot.setProductNumeric(StrUtil.trim(rs.getString("product_numeric")));
        return snapshot;
    };

    private static class SyncCounter {
        private int syncedDetailCount;
        private final Set<String> changedBarcodes = new LinkedHashSet<>();
        private int updatedMachineBarcodeCount;
        private int unmatchedBarcodeCount;
        private Long latestSourceId;
    }
}
