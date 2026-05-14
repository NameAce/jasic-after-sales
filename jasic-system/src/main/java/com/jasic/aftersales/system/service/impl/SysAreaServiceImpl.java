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
     * ???????
     *
     * @param parentCode ??
     * @return ????
     */
    @Resource
    private SysAreaMapper sysAreaMapper;

    @Override
    public List<SysAreaOptionVO> listOptionsByParentCode(String parentCode) {
        List<SysArea> children = listEnabledChildren(StrUtil.blankToDefault(StrUtil.trim(parentCode), ROOT_PARENT_CODE));
        if (children.isEmpty()) {
            return Collections.emptyList();
        }
        return children.stream().map(this::buildOption).collect(Collectors.toList());
    }

    /**
     * ??By Area Code?
     *
     * @param areaCode ??
     * @return ????
     */
    @Override
    public SysArea getByAreaCode(String areaCode) {
        if (StrUtil.isBlank(areaCode)) {
            return null;
        }
        // ??????????????????????????
        SysArea area = sysAreaMapper.selectById(StrUtil.trim(areaCode));
        if (area == null || !Objects.equals(area.getStatus(), STATUS_ENABLED)) {
            return null;
        }
        return area;
    }

    /**
     * ??By Area Codes?
     *
     * @param areaCodes ??
     * @return ????
     */
    @Override
    public Map<String, SysArea> getByAreaCodes(Collection<String> areaCodes) {
        if (areaCodes == null || areaCodes.isEmpty()) {
            return Collections.emptyMap();
        }
        Set<String> normalizedCodes = areaCodes.stream()
                .map(StrUtil::trim)
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (normalizedCodes.isEmpty()) {
            return Collections.emptyMap();
        }
        LambdaQueryWrapper<SysArea> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(SysArea::getAreaCode, normalizedCodes)
                .eq(SysArea::getStatus, STATUS_ENABLED);
        // ??????????????????????????
        List<SysArea> areas = sysAreaMapper.selectList(wrapper);
        if (areas.isEmpty()) {
            return Collections.emptyMap();
        }
        return areas.stream().collect(Collectors.toMap(SysArea::getAreaCode, item -> item, (a, b) -> a, LinkedHashMap::new));
    }

    /**
     * ?? matchRegion ?????
     *
     * @param provinceName ??
     * @param cityName ??
     * @param districtName ??
     * @param detailAddress ??
     * @return ????
     */
    @Override
    public AreaMatchResult matchRegion(String provinceName, String cityName, String districtName, String detailAddress) {
        SysArea province = findBestMatch(listEnabledChildren(ROOT_PARENT_CODE), provinceName, null);
        if (province == null) {
            return new AreaMatchResult(null, null, null);
        }

        List<SysArea> cityAreas = listEnabledChildren(province.getAreaCode());
        SysArea city = findBestMatch(cityAreas, cityName, null);
        SysArea district = null;
        if (city != null) {
            district = resolveDistrict(city, districtName, detailAddress);
        }

        if (city == null || district == null) {
            AreaPair pair = resolvePairByCityLikeDistrict(province, cityName, districtName, detailAddress);
            if (pair != null) {
                city = pair.getCity();
                district = pair.getDistrict();
            }
        }

        return new AreaMatchResult(province, city, district);
    }

    /**
     * ???????
     *
     * @param area ??
     * @return ????
     */
    private SysAreaOptionVO buildOption(SysArea area) {
        SysAreaOptionVO option = new SysAreaOptionVO();
        option.setAreaCode(area.getAreaCode());
        option.setAreaName(area.getAreaName());
        option.setParentCode(area.getParentCode());
        option.setAreaLevel(area.getAreaLevel());
        option.setLeaf(LEVEL_DISTRICT.equals(area.getAreaLevel()));
        return option;
    }

    /**
     * ???????
     *
     * @param city ??
     * @param districtName ??
     * @param detailAddress ??
     * @return ????
     */
    private SysArea resolveDistrict(SysArea city, String districtName, String detailAddress) {
        List<SysArea> districts = listEnabledChildren(city.getAreaCode());
        if (districts.isEmpty()) {
            return null;
        }
        SysArea district = findBestMatch(districts, districtName, null);
        if (district != null) {
            return district;
        }
        return findBestMatch(districts, detailAddress, detailAddress);
    }

    /**
     * ???????
     *
     * @param province ??
     * @param cityName ??
     * @param districtName ??
     * @param detailAddress ??
     * @return ????
     */
    private AreaPair resolvePairByCityLikeDistrict(SysArea province, String cityName, String districtName, String detailAddress) {
        List<SysArea> cities = listEnabledChildren(province.getAreaCode());
        if (cities.isEmpty()) {
            return null;
        }
        String districtLikeName = firstNonBlank(cityName, districtName);
        if (districtLikeName == null && detailAddress == null) {
            return null;
        }
        AreaPair matchedByDistrictName = findUniquePairByDistrict(cities, districtLikeName, null);
        if (matchedByDistrictName != null) {
            return matchedByDistrictName;
        }
        return findUniquePairByDistrict(cities, detailAddress, detailAddress);
    }

    /**
     * ?? findUniquePairByDistrict ?????
     *
     * @param cities ??
     * @param candidateName ??
     * @param addressText ??
     * @return ????
     */
    private AreaPair findUniquePairByDistrict(List<SysArea> cities, String candidateName, String addressText) {
        if (StrUtil.isBlank(candidateName) && StrUtil.isBlank(addressText)) {
            return null;
        }
        AreaPair matched = null;
        for (SysArea city : cities) {
            List<SysArea> districts = listEnabledChildren(city.getAreaCode());
            SysArea district = findBestMatch(districts, candidateName, addressText);
            if (district == null) {
                continue;
            }
            if (matched != null) {
                return null;
            }
            matched = new AreaPair(city, district);
        }
        return matched;
    }

    /**
     * ?? findBestMatch ?????
     *
     * @param candidates ??
     * @param preferredName ??
     * @param fuzzyAddressText ??
     * @return ????
     */
    private SysArea findBestMatch(List<SysArea> candidates, String preferredName, String fuzzyAddressText) {
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }
        String normalizedPreferredName = normalizeAreaAlias(preferredName);
        String normalizedAddressText = normalizeAreaAlias(fuzzyAddressText);

        SysArea exact = findExactNameMatch(candidates, preferredName);
        if (exact != null) {
            return exact;
        }

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
     * ?? findExactNameMatch ?????
     *
     * @param candidates ??
     * @param targetName ??
     * @return ????
     */
    private SysArea findExactNameMatch(List<SysArea> candidates, String targetName) {
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
     * ?? findUniqueAliasMatch ?????
     *
     * @param candidates ??
     * @param alias ??
     * @return ????
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
     * ?? findUniqueAddressMatch ?????
     *
     * @param candidates ??
     * @param normalizedAddressText ??
     * @return ????
     */
    private SysArea findUniqueAddressMatch(List<SysArea> candidates, String normalizedAddressText) {
        SysArea matched = null;
        for (SysArea candidate : candidates) {
            String areaName = normalizeText(candidate.getAreaName());
            String areaAlias = normalizeAreaAlias(candidate.getAreaName());
            boolean contains = StrUtil.isNotBlank(areaName) && normalizedAddressText.contains(normalizeAreaAlias(areaName));
            if (!contains && StrUtil.isNotBlank(areaAlias)) {
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
     * ???????
     *
     * @param parentCode ??
     * @return ????
     */
    private List<SysArea> listEnabledChildren(String parentCode) {
        LambdaQueryWrapper<SysArea> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysArea::getParentCode, parentCode)
                .eq(SysArea::getStatus, STATUS_ENABLED)
                .orderByAsc(SysArea::getSortNum)
                .orderByAsc(SysArea::getAreaCode);
        // ??????????????????????????
        return sysAreaMapper.selectList(wrapper);
    }

    /**
     * ????????
     *
     * @param value ???
     * @return ?????
     */
    private String normalizeAreaAlias(String value) {
        String normalized = normalizeText(value);
        if (normalized == null) {
            return null;
        }
        normalized = normalized.replace(" ", "");
        boolean changed = true;
        while (changed) {
            changed = false;
            for (String suffix : AREA_ALIAS_SUFFIXES) {
                if (normalized.endsWith(suffix) && normalized.length() > suffix.length()) {
                    normalized = normalized.substring(0, normalized.length() - suffix.length());
                    changed = true;
                    break;
                }
            }
        }
        return normalized;
    }

    /**
     * ????????
     *
     * @param value ???
     * @return ?????
     */
    private String normalizeText(String value) {
        String normalized = StrUtil.trim(value);
        return StrUtil.isBlank(normalized) ? null : normalized;
    }

    /**
     * ?? firstNonBlank ?????
     *
     * @param first ??
     * @param second ??
     * @return ?????
     */
    private String firstNonBlank(String first, String second) {
        String normalizedFirst = normalizeText(first);
        if (normalizedFirst != null) {
            return normalizedFirst;
        }
        return normalizeText(second);
    }

    private static final class AreaPair {

        /**
         * ?? AreaPair ?????
         *
         * @param city ??
         * @param district ??
         * @return ????
         */
        private final SysArea city;

        private final SysArea district;

        private AreaPair(SysArea city, SysArea district) {
            this.city = city;
            this.district = district;
        }

        /**
         * ??City?
         *
         * @return ????
         */
        private SysArea getCity() {
            return city;
        }

        /**
         * ??District?
         *
         * @return ????
         */
        private SysArea getDistrict() {
            return district;
        }
    }
}
