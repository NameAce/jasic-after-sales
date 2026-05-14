package com.jasic.aftersales.system.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jasic.aftersales.system.domain.entity.SysArea;
import com.jasic.aftersales.system.domain.vo.SysAreaOptionVO;
import com.jasic.aftersales.system.mapper.SysAreaMapper;
import com.jasic.aftersales.system.service.ISysAreaService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 行政区划 Service 实现
 *
 * @author Codex
 * @date 2026/04/17
 */
@Service
public class SysAreaServiceImpl implements ISysAreaService {

    private static final String[] AREA_ALIAS_SUFFIXES = new String[]{
            "特别行政区", "维吾尔自治区", "壮族自治区", "回族自治区", "自治区",
            "自治州", "自治县", "自治旗", "林区", "特区", "地区", "盟",
            "省", "市", "区", "县", "旗"
    };

    /**
     * 系统AreaMapper数据访问接口。
     *
     * @param parentCode 参数
     * @return 处理结果
     */
    @Resource
    private SysAreaMapper sysAreaMapper;

    /**
     * 查询listOptionsByParentCode相关业务数据。
     *
     * <p>说明：该方法用于执行业务流程编排，确保调用链路清晰可维护。</p>
     * @param parentCode 参数
     * @return 处理结果
     */
    @Override
    public List<SysAreaOptionVO> listOptionsByParentCode(String parentCode) {
        // 调用trim方法，复用统一能力并保证业务规则一致。
        List<SysArea> children = listEnabledChildren(StrUtil.blankToDefault(StrUtil.trim(parentCode), ROOT_PARENT_CODE));
        if (children.isEmpty()) {
            return Collections.emptyList();
        }
        return children.stream().map(this::buildOption).collect(Collectors.toList());
    }

    /**
     * 获取ByArea编码。
     *
     * @param areaCode 参数
     * @return 处理结果
     */
    @Override
    public SysArea getByAreaCode(String areaCode) {
        if (StrUtil.isBlank(areaCode)) {
            return null;
        }
        // 说明：执行该步骤以保证业务流程正确。
        SysArea area = sysAreaMapper.selectById(StrUtil.trim(areaCode));
        if (area == null || !Objects.equals(area.getStatus(), STATUS_ENABLED)) {
            return null;
        }
        return area;
    }

    /**
     * 获取ByAreaCodes。
     *
     * @param areaCodes 参数
     * @return 处理结果
     */
    @Override
    public Map<String, SysArea> getByAreaCodes(Collection<String> areaCodes) {
        if (areaCodes == null || areaCodes.isEmpty()) {
            return Collections.emptyMap();
        }
        Set<String> normalizedCodes = areaCodes.stream()
                .map(StrUtil::trim)
                .filter(StrUtil::isNotBlank)
                // 调用toCollection方法，复用统一能力并保证业务规则一致。
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (normalizedCodes.isEmpty()) {
            return Collections.emptyMap();
        }
        LambdaQueryWrapper<SysArea> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(SysArea::getAreaCode, normalizedCodes)
                // 调用eq方法，复用统一能力并保证业务规则一致。
                .eq(SysArea::getStatus, STATUS_ENABLED);
        // 说明：执行该步骤以保证业务流程正确。
        List<SysArea> areas = sysAreaMapper.selectList(wrapper);
        if (areas.isEmpty()) {
            return Collections.emptyMap();
        }
        return areas.stream().collect(Collectors.toMap(SysArea::getAreaCode, item -> item, (a, b) -> a, LinkedHashMap::new));
    }

    /**
     * match地区。
     *
     * @param provinceName 参数
     * @param cityName 参数
     * @param districtName 参数
     * @param detailAddress 参数
     * @return 处理结果
     */
    @Override
    public AreaMatchResult matchRegion(String provinceName, String cityName, String districtName, String detailAddress) {
        // 调用listEnabledChildren方法，复用统一能力并保证业务规则一致。
        SysArea province = findBestMatch(listEnabledChildren(ROOT_PARENT_CODE), provinceName, null);
        if (province == null) {
            return new AreaMatchResult(null, null, null);
        }

        // 调用getAreaCode方法，复用统一能力并保证业务规则一致。
        List<SysArea> cityAreas = listEnabledChildren(province.getAreaCode());
        // 调用findBestMatch方法，复用统一能力并保证业务规则一致。
        SysArea city = findBestMatch(cityAreas, cityName, null);
        SysArea district = null;
        if (city != null) {
            // 调用resolveDistrict方法，复用统一能力并保证业务规则一致。
            district = resolveDistrict(city, districtName, detailAddress);
        }

        if (city == null || district == null) {
            // 调用resolvePairByCityLikeDistrict方法，复用统一能力并保证业务规则一致。
            AreaPair pair = resolvePairByCityLikeDistrict(province, cityName, districtName, detailAddress);
            if (pair != null) {
                // 调用getCity方法，复用统一能力并保证业务规则一致。
                city = pair.getCity();
                // 调用getDistrict方法，复用统一能力并保证业务规则一致。
                district = pair.getDistrict();
            }
        }

        return new AreaMatchResult(province, city, district);
    }

    /**
     * 构建Option。
     *
     * @param area 参数
     * @return 处理结果
     */
    private SysAreaOptionVO buildOption(SysArea area) {
        // 调用SysAreaOptionVO方法，复用统一能力并保证业务规则一致。
        SysAreaOptionVO option = new SysAreaOptionVO();
        // 调用getAreaCode方法，复用统一能力并保证业务规则一致。
        option.setAreaCode(area.getAreaCode());
        // 调用getAreaName方法，复用统一能力并保证业务规则一致。
        option.setAreaName(area.getAreaName());
        // 调用getParentCode方法，复用统一能力并保证业务规则一致。
        option.setParentCode(area.getParentCode());
        // 调用getAreaLevel方法，复用统一能力并保证业务规则一致。
        option.setAreaLevel(area.getAreaLevel());
        // 调用getAreaLevel方法，复用统一能力并保证业务规则一致。
        option.setLeaf(LEVEL_DISTRICT.equals(area.getAreaLevel()));
        return option;
    }

    /**
     * 解析District。
     *
     * @param city 参数
     * @param districtName 参数
     * @param detailAddress 参数
     * @return 处理结果
     */
    private SysArea resolveDistrict(SysArea city, String districtName, String detailAddress) {
        // 调用getAreaCode方法，复用统一能力并保证业务规则一致。
        List<SysArea> districts = listEnabledChildren(city.getAreaCode());
        if (districts.isEmpty()) {
            return null;
        }
        // 调用findBestMatch方法，复用统一能力并保证业务规则一致。
        SysArea district = findBestMatch(districts, districtName, null);
        if (district != null) {
            return district;
        }
        return findBestMatch(districts, detailAddress, detailAddress);
    }

    /**
     * 解析PairByCityLikeDistrict。
     *
     * @param province 参数
     * @param cityName 参数
     * @param districtName 参数
     * @param detailAddress 参数
     * @return 处理结果
     */
    private AreaPair resolvePairByCityLikeDistrict(SysArea province, String cityName, String districtName, String detailAddress) {
        // 调用getAreaCode方法，复用统一能力并保证业务规则一致。
        List<SysArea> cities = listEnabledChildren(province.getAreaCode());
        if (cities.isEmpty()) {
            return null;
        }
        // 调用firstNonBlank方法，复用统一能力并保证业务规则一致。
        String districtLikeName = firstNonBlank(cityName, districtName);
        if (districtLikeName == null && detailAddress == null) {
            return null;
        }
        // 调用findUniquePairByDistrict方法，复用统一能力并保证业务规则一致。
        AreaPair matchedByDistrictName = findUniquePairByDistrict(cities, districtLikeName, null);
        if (matchedByDistrictName != null) {
            return matchedByDistrictName;
        }
        return findUniquePairByDistrict(cities, detailAddress, detailAddress);
    }

    /**
     * findUniquePairByDistrict。
     *
     * @param cities 参数
     * @param candidateName 参数
     * @param addressText 参数
     * @return 处理结果
     */
    private AreaPair findUniquePairByDistrict(List<SysArea> cities, String candidateName, String addressText) {
        if (StrUtil.isBlank(candidateName) && StrUtil.isBlank(addressText)) {
            return null;
        }
        AreaPair matched = null;
        for (SysArea city : cities) {
            // 调用getAreaCode方法，复用统一能力并保证业务规则一致。
            List<SysArea> districts = listEnabledChildren(city.getAreaCode());
            // 调用findBestMatch方法，复用统一能力并保证业务规则一致。
            SysArea district = findBestMatch(districts, candidateName, addressText);
            if (district == null) {
                continue;
            }
            if (matched != null) {
                return null;
            }
            // 调用AreaPair方法，复用统一能力并保证业务规则一致。
            matched = new AreaPair(city, district);
        }
        return matched;
    }

    /**
     * findBestMatch。
     *
     * @param candidates 参数
     * @param preferredName 参数
     * @param fuzzyAddressText 参数
     * @return 处理结果
     */
    private SysArea findBestMatch(List<SysArea> candidates, String preferredName, String fuzzyAddressText) {
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }
        // 调用normalizeAreaAlias方法，复用统一能力并保证业务规则一致。
        String normalizedPreferredName = normalizeAreaAlias(preferredName);
        // 调用normalizeAreaAlias方法，复用统一能力并保证业务规则一致。
        String normalizedAddressText = normalizeAreaAlias(fuzzyAddressText);

        // 调用findExactNameMatch方法，复用统一能力并保证业务规则一致。
        SysArea exact = findExactNameMatch(candidates, preferredName);
        if (exact != null) {
            return exact;
        }

        // 调用findUniqueAliasMatch方法，复用统一能力并保证业务规则一致。
        SysArea aliasMatch = findUniqueAliasMatch(candidates, normalizedPreferredName);
        if (aliasMatch != null) {
            return aliasMatch;
        }

        if (StrUtil.isBlank(normalizedAddressText)) {
            return null;
        }
        return findUniqueAddressMatch(candidates, normalizedAddressText);
    }

    /**
     * findExact名称Match。
     *
     * @param candidates 参数
     * @param targetName 参数
     * @return 处理结果
     */
    private SysArea findExactNameMatch(List<SysArea> candidates, String targetName) {
        // 调用normalizeText方法，复用统一能力并保证业务规则一致。
        String normalizedTargetName = normalizeText(targetName);
        if (normalizedTargetName == null) {
            return null;
        }
        SysArea matched = null;
        for (SysArea candidate : candidates) {
            if (!StrUtil.equals(normalizedTargetName, normalizeText(candidate.getAreaName()))) {
                continue;
            }
            if (matched != null) {
                return null;
            }
            matched = candidate;
        }
        return matched;
    }

    /**
     * findUniqueAliasMatch。
     *
     * @param candidates 参数
     * @param alias 参数
     * @return 处理结果
     */
    private SysArea findUniqueAliasMatch(List<SysArea> candidates, String alias) {
        if (alias == null) {
            return null;
        }
        SysArea matched = null;
        for (SysArea candidate : candidates) {
            if (!StrUtil.equals(alias, normalizeAreaAlias(candidate.getAreaName()))) {
                continue;
            }
            if (matched != null) {
                return null;
            }
            matched = candidate;
        }
        return matched;
    }

    /**
     * findUniqueAddressMatch。
     *
     * @param candidates 参数
     * @param normalizedAddressText 参数
     * @return 处理结果
     */
    private SysArea findUniqueAddressMatch(List<SysArea> candidates, String normalizedAddressText) {
        SysArea matched = null;
        for (SysArea candidate : candidates) {
            // 调用getAreaName方法，复用统一能力并保证业务规则一致。
            String areaName = normalizeText(candidate.getAreaName());
            // 调用getAreaName方法，复用统一能力并保证业务规则一致。
            String areaAlias = normalizeAreaAlias(candidate.getAreaName());
            // 调用normalizeAreaAlias方法，复用统一能力并保证业务规则一致。
            boolean contains = StrUtil.isNotBlank(areaName) && normalizedAddressText.contains(normalizeAreaAlias(areaName));
            if (!contains && StrUtil.isNotBlank(areaAlias)) {
                // 调用contains方法，复用统一能力并保证业务规则一致。
                contains = normalizedAddressText.contains(areaAlias);
            }
            if (!contains) {
                continue;
            }
            if (matched != null) {
                return null;
            }
            matched = candidate;
        }
        return matched;
    }

    /**
     * 分页查询EnabledChildren列表。
     *
     * @param parentCode 参数
     * @return 处理结果
     */
    private List<SysArea> listEnabledChildren(String parentCode) {
        LambdaQueryWrapper<SysArea> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysArea::getParentCode, parentCode)
                .eq(SysArea::getStatus, STATUS_ENABLED)
                .orderByAsc(SysArea::getSortNum)
                // 调用orderByAsc方法，复用统一能力并保证业务规则一致。
                .orderByAsc(SysArea::getAreaCode);
        // 说明：执行该步骤以保证业务流程正确。
        return sysAreaMapper.selectList(wrapper);
    }

    /**
     * 规范化AreaAlias。
     *
     * @param value 参数
     * @return 处理结果
     */
    private String normalizeAreaAlias(String value) {
        // 调用normalizeText方法，复用统一能力并保证业务规则一致。
        String normalized = normalizeText(value);
        if (normalized == null) {
            return null;
        }
        // 调用replace方法，复用统一能力并保证业务规则一致。
        normalized = normalized.replace(" ", "");
        boolean changed = true;
        while (changed) {
            changed = false;
            for (String suffix : AREA_ALIAS_SUFFIXES) {
                if (normalized.endsWith(suffix) && normalized.length() > suffix.length()) {
                    // 调用length方法，复用统一能力并保证业务规则一致。
                    normalized = normalized.substring(0, normalized.length() - suffix.length());
                    changed = true;
                    break;
                }
            }
        }
        return normalized;
    }

    /**
     * 规范化Text。
     *
     * @param value 参数
     * @return 处理结果
     */
    private String normalizeText(String value) {
        // 调用trim方法，复用统一能力并保证业务规则一致。
        String normalized = StrUtil.trim(value);
        return StrUtil.isBlank(normalized) ? null : normalized;
    }

    /**
     * firstNonBlank。
     *
     * @param first 参数
     * @param second 参数
     * @return 处理结果
     */
    private String firstNonBlank(String first, String second) {
        // 调用normalizeText方法，复用统一能力并保证业务规则一致。
        String normalizedFirst = normalizeText(first);
        if (normalizedFirst != null) {
            return normalizedFirst;
        }
        return normalizeText(second);
    }

    private static final class AreaPair {

        /**
     * 系统Area字段。
     *
     * @param city 参数
     * @param district 参数
     * @return 处理结果
         */
        private final SysArea city;

        private final SysArea district;

        /**
     * 构造系统Area实例。
     *
     * @param city 参数
     * @param district 参数
     * @return 处理结果
         */
        private AreaPair(SysArea city, SysArea district) {
            this.city = city;
            this.district = district;
        }

        /**
     * 获取City。
     *
     * @return 处理结果
         */
        private SysArea getCity() {
            return city;
        }

        /**
     * 获取District。
     *
     * @return 处理结果
         */
        private SysArea getDistrict() {
            return district;
        }
    }
}




