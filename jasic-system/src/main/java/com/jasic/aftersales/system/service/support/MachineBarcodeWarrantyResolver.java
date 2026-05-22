package com.jasic.aftersales.system.service.support;

import cn.hutool.core.util.StrUtil;
import com.jasic.aftersales.system.domain.entity.MachineBarcode;

import java.time.LocalDateTime;

/**
 * 条码质保状态计算工具
 *
 * @author Zoro
 * @date 2026/04/07
 */
public final class MachineBarcodeWarrantyResolver {

    /**IN_WARRANTY 常量，用于固定当前类内部复用的业务编码、默认值或配置边界。*/
    public static final String IN_WARRANTY = "IN_WARRANTY";
    /**OUT_OF_WARRANTY 常量，用于固定当前类内部复用的业务编码、默认值或配置边界。*/
    public static final String OUT_OF_WARRANTY = "OUT_OF_WARRANTY";

    /**
     * 构造机器条码保修实例。
     */
    private MachineBarcodeWarrantyResolver() {
    }

    /**
     * 解析保修状态。
     *
     * @param barcode 业务编码，用于匹配枚举、配置或外部系统数据。
     * @param lastOutDate 时间值，用于业务节点记录或时效判断。
     * @param scanDate 时间值，用于业务节点记录或时效判断。
     * @param fallbackStatus 业务状态编码，用于状态流转或展示判断。
     * @return 业务处理结果
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
     * 解析保修状态。
     *
     * @param barcodeArchive 业务编码，用于匹配枚举、配置或外部系统数据。
     * @return 业务处理结果
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
     * @param barcodeArchive 业务编码，用于匹配枚举、配置或外部系统数据。
     * @return 业务处理结果
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
     * @param lastOutDate 时间值，用于业务节点记录或时效判断。
     * @param scanDate 时间值，用于业务节点记录或时效判断。
     * @return 业务处理结果
     */
    public static LocalDateTime resolveLastOutDate(LocalDateTime lastOutDate, LocalDateTime scanDate) {
        return lastOutDate != null ? lastOutDate : scanDate;
    }

    /**
     * 解析ExpireTime。
     *
     * @param lastOutDate 时间值，用于业务节点记录或时效判断。
     * @param scanDate 时间值，用于业务节点记录或时效判断。
     * @return 业务处理结果
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


