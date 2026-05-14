package com.jasic.aftersales.system.service.support;

import cn.hutool.core.util.StrUtil;
import com.jasic.aftersales.system.domain.entity.MachineBarcode;

import java.time.LocalDateTime;

/**
 * 条码质保状态计算工具
 *
 * @author Codex
 * @date 2026/04/07
 */
public final class MachineBarcodeWarrantyResolver {

    public static final String IN_WARRANTY = "IN_WARRANTY";
    public static final String OUT_OF_WARRANTY = "OUT_OF_WARRANTY";

    /**
     * ?? MachineBarcodeWarrantyResolver ???
     */
    private MachineBarcodeWarrantyResolver() {
    }

    /**
     * ???????
     *
     * @param barcode ??
     * @param lastOutDate ??
     * @param scanDate ??
     * @param fallbackStatus ??
     * @return ?????
     */
    public static String resolveWarrantyStatus(String barcode, LocalDateTime lastOutDate, LocalDateTime scanDate,
                                               String fallbackStatus) {
        if (StrUtil.isBlank(StrUtil.trim(barcode))) {
            return OUT_OF_WARRANTY;
        }
        LocalDateTime expireTime = resolveExpireTime(lastOutDate, scanDate);
        if (expireTime != null) {
            return !LocalDateTime.now().isAfter(expireTime) ? IN_WARRANTY : OUT_OF_WARRANTY;
        }
        String normalizedFallback = StrUtil.trim(fallbackStatus);
        return StrUtil.isBlank(normalizedFallback) ? OUT_OF_WARRANTY : normalizedFallback;
    }

    /**
     * ???????
     *
     * @param barcodeArchive ??
     * @return ?????
     */
    public static String resolveWarrantyStatus(MachineBarcode barcodeArchive) {
        if (barcodeArchive == null) {
            return OUT_OF_WARRANTY;
        }
        return resolveWarrantyStatus(
                barcodeArchive.getBarcode(),
                barcodeArchive.getLastOutDate(),
                barcodeArchive.getScanDate(),
                null
        );
    }

    /**
     * ???????
     *
     * @param barcodeArchive ??
     * @return ????
     */
    public static LocalDateTime resolveLastOutDate(MachineBarcode barcodeArchive) {
        if (barcodeArchive == null) {
            return null;
        }
        return resolveLastOutDate(barcodeArchive.getLastOutDate(), barcodeArchive.getScanDate());
    }

    /**
     * ???????
     *
     * @param lastOutDate ??
     * @param scanDate ??
     * @return ????
     */
    public static LocalDateTime resolveLastOutDate(LocalDateTime lastOutDate, LocalDateTime scanDate) {
        return lastOutDate != null ? lastOutDate : scanDate;
    }

    /**
     * ???????
     *
     * @param lastOutDate ??
     * @param scanDate ??
     * @return ????
     */
    private static LocalDateTime resolveExpireTime(LocalDateTime lastOutDate, LocalDateTime scanDate) {
        if (lastOutDate != null) {
            return lastOutDate.plusYears(3);
        }
        if (scanDate != null) {
            return scanDate.plusMonths(6);
        }
        return null;
    }
}
