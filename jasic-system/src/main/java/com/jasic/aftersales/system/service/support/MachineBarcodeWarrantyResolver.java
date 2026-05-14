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
     * 构造机器条码保修实例。
     */
    private MachineBarcodeWarrantyResolver() {
    }

    /**
     * 解析保修状态。
     *
     * @param barcode 参数
     * @param lastOutDate 参数
     * @param scanDate 参数
     * @param fallbackStatus 参数
     * @return 处理结果
     */
    public static String resolveWarrantyStatus(String barcode, LocalDateTime lastOutDate, LocalDateTime scanDate,
                                               String fallbackStatus) {
        if (StrUtil.isBlank(StrUtil.trim(barcode))) {
            return OUT_OF_WARRANTY;
        }
        // 调用resolveExpireTime方法，复用统一能力并保证业务规则一致。
        LocalDateTime expireTime = resolveExpireTime(lastOutDate, scanDate);
        if (expireTime != null) {
            return !LocalDateTime.now().isAfter(expireTime) ? IN_WARRANTY : OUT_OF_WARRANTY;
        }
        // 调用trim方法，复用统一能力并保证业务规则一致。
        String normalizedFallback = StrUtil.trim(fallbackStatus);
        return StrUtil.isBlank(normalizedFallback) ? OUT_OF_WARRANTY : normalizedFallback;
    }

    /**
     * 解析保修状态。
     *
     * @param barcodeArchive 参数
     * @return 处理结果
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
     * 解析LastOutDate。
     *
     * @param barcodeArchive 参数
     * @return 处理结果
     */
    public static LocalDateTime resolveLastOutDate(MachineBarcode barcodeArchive) {
        if (barcodeArchive == null) {
            return null;
        }
        return resolveLastOutDate(barcodeArchive.getLastOutDate(), barcodeArchive.getScanDate());
    }

    /**
     * 解析LastOutDate。
     *
     * @param lastOutDate 参数
     * @param scanDate 参数
     * @return 处理结果
     */
    public static LocalDateTime resolveLastOutDate(LocalDateTime lastOutDate, LocalDateTime scanDate) {
        return lastOutDate != null ? lastOutDate : scanDate;
    }

    /**
     * 解析ExpireTime。
     *
     * @param lastOutDate 参数
     * @param scanDate 参数
     * @return 处理结果
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


