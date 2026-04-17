package com.jasic.aftersales.system.service;

import com.jasic.aftersales.system.domain.entity.SysArea;
import com.jasic.aftersales.system.domain.vo.SysAreaOptionVO;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 行政区划 Service
 *
 * @author Codex
 * @date 2026/04/17
 */
public interface ISysAreaService {

    String ROOT_PARENT_CODE = "0";
    String LEVEL_PROVINCE = "PROVINCE";
    String LEVEL_CITY = "CITY";
    String LEVEL_DISTRICT = "DISTRICT";
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

        private final SysArea province;

        private final SysArea city;

        private final SysArea district;

        public AreaMatchResult(SysArea province, SysArea city, SysArea district) {
            this.province = province;
            this.city = city;
            this.district = district;
        }

        public SysArea getProvince() {
            return province;
        }

        public SysArea getCity() {
            return city;
        }

        public SysArea getDistrict() {
            return district;
        }

        public boolean isMatched() {
            return province != null && city != null && district != null;
        }
    }
}
