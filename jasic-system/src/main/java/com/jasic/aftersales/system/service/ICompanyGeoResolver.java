package com.jasic.aftersales.system.service;

import java.math.BigDecimal;

/**
 * 公司地址解析 Service 接口
 *
 * @author Codex
 * @date 2026/04/02
 */
public interface ICompanyGeoResolver {

    /**
     * 根据地址解析经纬度
     *
     * @param address 公司地址
     * @return 经纬度结果
     */
    GeoLocation resolve(String address);

    /**
     * 经纬度结果
     */
    class GeoLocation {

        /**
     * BigDecimal字段。
     *
     * @param longitude 参数
     * @param latitude 参数
     * @return 处理结果
         */
        private final BigDecimal longitude;

        private final BigDecimal latitude;

        /**
     * 构造公司地理实例。
     *
     * @param longitude 参数
     * @param latitude 参数
     * @return 处理结果
         */
        public GeoLocation(BigDecimal longitude, BigDecimal latitude) {
            this.longitude = longitude;
            this.latitude = latitude;
        }

        /**
     * 获取Longitude。
     *
     * @return 处理结果
         */
        public BigDecimal getLongitude() {
            return longitude;
        }

        /**
     * 获取Latitude。
     *
     * @return 处理结果
         */
        public BigDecimal getLatitude() {
            return latitude;
        }
    }
}


