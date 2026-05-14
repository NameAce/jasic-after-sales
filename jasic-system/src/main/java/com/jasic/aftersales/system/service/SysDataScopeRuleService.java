package com.jasic.aftersales.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jasic.aftersales.common.enums.DataScopeEnum;
import com.jasic.aftersales.common.enums.SubjectTypeEnum;
import com.jasic.aftersales.common.exception.ServiceException;
import com.jasic.aftersales.system.domain.entity.SysCompany;
import com.jasic.aftersales.system.domain.entity.SysCompanyType;
import com.jasic.aftersales.system.domain.vo.DataScopeOptionVO;
import com.jasic.aftersales.system.mapper.SysCompanyMapper;
import com.jasic.aftersales.system.mapper.SysCompanyTypeMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 数据范围规则服务。
 *
 * @author Zoro
 * @date 2026/03/25
 */
@Service
public class SysDataScopeRuleService {

    /**
     * ???????
     *
     * @param companyId ??ID
     * @return ????
     */
    @Resource
    private SysCompanyMapper sysCompanyMapper;

    @Resource
    private SysCompanyTypeMapper sysCompanyTypeMapper;

    /**
     * 获取当前公司可选的数据范围。
     *
     * @param companyId 公司ID
     * @return 数据范围选项
     */
    public List<DataScopeOptionVO> listOptionsByCompanyId(Long companyId) {
        // ??????????????????????????
        SysCompany company = sysCompanyMapper.selectById(companyId);
        if (company == null) {
            throw new ServiceException("公司不存在");
        }
        return listOptionsByTypeCode(company.getTypeCode());
    }

    /**
     * 按公司类型获取可选的数据范围。
     *
     * @param typeCode 公司类型编码
     * @return 数据范围选项
     */
    public List<DataScopeOptionVO> listOptionsByTypeCode(String typeCode) {
        SysCompanyType companyType = getCompanyType(typeCode);
        return buildOptions(companyType.getSubjectType(), typeCode);
    }

    /**
     * 获取全部公司类型的数据范围选项映射。
     *
     * @return typeCode -> 选项列表
     */
    public Map<String, List<DataScopeOptionVO>> listOptionMap() {
        LambdaQueryWrapper<SysCompanyType> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(SysCompanyType::getOrderNum);
        // ??????????????????????????
        List<SysCompanyType> companyTypes = sysCompanyTypeMapper.selectList(wrapper);
        return companyTypes.stream().collect(Collectors.toMap(
                SysCompanyType::getTypeCode,
                companyType -> buildOptions(companyType.getSubjectType(), companyType.getTypeCode()),
                (left, right) -> left,
                LinkedHashMap::new
        ));
    }

    /**
     * 校验当前公司是否允许指定数据范围。
     *
     * @param companyId  公司ID
     * @param dataScope 数据范围
     */
    public void validateByCompanyId(Long companyId, String dataScope) {
        // ??????????????????????????
        SysCompany company = sysCompanyMapper.selectById(companyId);
        if (company == null) {
            throw new ServiceException("公司不存在");
        }
        // ?????????????????????????????
        validateByTypeCode(company.getTypeCode(), dataScope);
    }

    /**
     * 校验公司类型是否允许指定数据范围。
     *
     * @param typeCode   公司类型编码
     * @param dataScope 数据范围
     */
    public void validateByTypeCode(String typeCode, String dataScope) {
        if (dataScope == null || dataScope.trim().isEmpty()) {
            throw new ServiceException("数据范围不能为空");
        }
        List<DataScopeOptionVO> options = listOptionsByTypeCode(typeCode);
        boolean matched = options.stream().anyMatch(option -> option.getValue().equals(dataScope));
        if (!matched) {
            throw new ServiceException("当前公司类型不支持该数据范围");
        }
    }

    /**
     * 获取默认数据范围。
     *
     * @param typeCode 公司类型编码
     * @return 默认数据范围编码
     */
    public String getDefaultDataScope(String typeCode) {
        List<DataScopeOptionVO> options = listOptionsByTypeCode(typeCode);
        return options.stream()
                .filter(DataScopeOptionVO::getDefaultOption)
                .map(DataScopeOptionVO::getValue)
                .findFirst()
                .orElse(DataScopeEnum.SELF.getCode());
    }

    /**
     * ??Company Type?
     *
     * @param typeCode ??????
     * @return ????
     */
    private SysCompanyType getCompanyType(String typeCode) {
        LambdaQueryWrapper<SysCompanyType> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysCompanyType::getTypeCode, typeCode);
        // ??????????????????????????
        SysCompanyType companyType = sysCompanyTypeMapper.selectOne(wrapper);
        if (companyType == null) {
            throw new ServiceException("公司类型不存在");
        }
        return companyType;
    }

    /**
     * ???????
     *
     * @param subjectType ????
     * @param typeCode ??????
     * @return ????
     */
    private List<DataScopeOptionVO> buildOptions(String subjectType, String typeCode) {
        SubjectTypeEnum subjectTypeEnum = SubjectTypeEnum.getByCode(subjectType);
        if (subjectTypeEnum == null) {
            throw new ServiceException("公司主体类型不合法");
        }
        switch (subjectTypeEnum) {
            case PLATFORM:
                return java.util.Collections.singletonList(option(DataScopeEnum.ALL.getCode(), "全部数据", true));
            case HQ:
                return java.util.Arrays.asList(
                        option(DataScopeEnum.ALL.getCode(), "全部数据", false),
                        option(DataScopeEnum.REGION.getCode(), "大区数据", false),
                        option(DataScopeEnum.SELF.getCode(), "仅本人", true)
                );
            case SERVICE:
                if ("SITE_FIRST".equals(typeCode)) {
                    return java.util.Arrays.asList(
                            option(DataScopeEnum.ALL.getCode(), "本公司及下级网点", false),
                            option(DataScopeEnum.COMPANY.getCode(), "仅本公司", true),
                            option(DataScopeEnum.SELF.getCode(), "仅本人", false)
                    );
                }
                return java.util.Arrays.asList(
                        option(DataScopeEnum.COMPANY.getCode(), "仅本公司", true),
                        option(DataScopeEnum.SELF.getCode(), "仅本人", false)
                );
            default:
                throw new ServiceException("暂不支持该主体类型的数据范围");
        }
    }

    /**
     * ?? option ?????
     *
     * @param value ???
     * @param label ??
     * @param defaultOption ??
     * @return ????
     */
    private DataScopeOptionVO option(String value, String label, boolean defaultOption) {
        DataScopeOptionVO option = new DataScopeOptionVO();
        option.setValue(value);
        option.setLabel(label);
        option.setDefaultOption(defaultOption);
        return option;
    }
}
