package com.jasic.aftersales.system.service;

import com.jasic.aftersales.system.domain.entity.SysArea;
import com.jasic.aftersales.system.domain.vo.SysAreaOptionVO;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 行政区划 Service
 *
 * @author Zoro
 * @date 2026/04/17
 */
public interface ISysAreaService {

    /**ROOT_PARENT_CODE 字段，用于当前类内部业务处理。*/
    String ROOT_PARENT_CODE = "0";
    /**LEVEL_PROVINCE 字段，用于当前类内部业务处理。*/
    String LEVEL_PROVINCE = "PROVINCE";
    /**LEVEL_CITY 字段，用于当前类内部业务处理。*/
    String LEVEL_CITY = "CITY";
    /**LEVEL_DISTRICT 字段，用于当前类内部业务处理。*/
    String LEVEL_DISTRICT = "DISTRICT";
    /**STATUS_ENABLED 字段，用于当前类内部业务处理。*/
    Integer STATUS_ENABLED = 1;

    /**
     * 查询指定父级下的行政区划选项
     *
     * @param parentCode 父级编码，空表示省级根节点
     * @return 下拉选项
     */
    List<SysAreaOptionVO> listOptionsByParentCode(String parentCode);

    /**
     * 按编码查询行政区划
     *
     * @param areaCode 行政区编码
     * @return 行政区
     */
    SysArea getByAreaCode(String areaCode);

    /**
     * 按编码批量查询行政区划
     *
     * @param areaCodes 行政区编码集合
     * @return 编码到行政区的映射
     */
    Map<String, SysArea> getByAreaCodes(Collection<String> areaCodes);

    /**
     * 根据 CRM 文本尝试匹配标准行政区
     *
     * @param provinceName CRM 省份名称
     * @param cityName CRM 城市名称
     * @param districtName CRM 区县名称
     * @param detailAddress CRM 详细地址
     * @return 匹配结果
     */
    AreaMatchResult matchRegion(String provinceName, String cityName, String districtName, String detailAddress);

    /**
     * 行政区匹配结果
     */
    class AreaMatchResult {

        /**
     * 系统Area字段。
     *
     * @param province province，当前业务处理所需的输入值。
     * @param city city，当前业务处理所需的输入值。
     * @param district district，当前业务处理所需的输入值。
     * @return 业务处理结果
         */
        private final SysArea province;

        /**city 字段，用于当前类内部业务处理。*/
        private final SysArea city;

        /**district 字段，用于当前类内部业务处理。*/
        private final SysArea district;

        /**
     * 构造系统Area实例。
     *
     * @param province province，当前业务处理所需的输入值。
     * @param city city，当前业务处理所需的输入值。
     * @param district district，当前业务处理所需的输入值。
     * @return 业务处理结果
         */
        public AreaMatchResult(SysArea province, SysArea city, SysArea district) {
            this.province = province;
            this.city = city;
            this.district = district;
        }

        /**
     * 获取Province。
     *
     * @return 业务处理结果
         */
        public SysArea getProvince() {
            return province;
        }

        /**
     * 获取City。
     *
     * @return 业务处理结果
         */
        public SysArea getCity() {
            return city;
        }

        /**
     * 获取District。
     *
     * @return 业务处理结果
         */
        public SysArea getDistrict() {
            return district;
        }

        /**
     * 判断是否Matched。
         */
        public boolean isMatched() {
            return province != null && city != null && district != null;
        }
    }
}


