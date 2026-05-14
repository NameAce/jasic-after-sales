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
         * ?? GeoLocation ?????
         *
         * @param longitude ??
         * @param latitude ??
         * @return ????
         */
        private final BigDecimal longitude;

        private final BigDecimal latitude;

        public GeoLocation(BigDecimal longitude, BigDecimal latitude) {
            this.longitude = longitude;
            this.latitude = latitude;
        }

        /**
         * ??Longitude?
         *
         * @return ????
         */
        public BigDecimal getLongitude() {
            return longitude;
        }

        /**
         * ??Latitude?
         *
         * @return ????
         */
        public BigDecimal getLatitude() {
            return latitude;
        }
    }
}
