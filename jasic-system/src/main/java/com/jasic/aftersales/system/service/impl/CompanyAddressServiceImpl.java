package com.jasic.aftersales.system.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jasic.aftersales.common.exception.ServiceException;
import com.jasic.aftersales.framework.datapermission.CompanyDataAccessContext;
import com.jasic.aftersales.system.domain.dto.CompanyAddressCreateDTO;
import com.jasic.aftersales.system.domain.dto.CompanyAddressUpdateDTO;
import com.jasic.aftersales.system.domain.entity.CompanyAddress;
import com.jasic.aftersales.system.domain.vo.CompanyAddressVO;
import com.jasic.aftersales.system.mapper.CompanyAddressMapper;
import com.jasic.aftersales.system.service.ICompanyAddressService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 公司地址簿 Service 实现。
 *
 * @author Codex
 * @date 2026/04/11
 */
@Service
public class CompanyAddressServiceImpl implements ICompanyAddressService {

    private static final int MAX_ADDRESS_COUNT = 20;
    private static final int CONTACT_NAME_MAX_LENGTH = 64;
    private static final int CONTACT_PHONE_MAX_LENGTH = 32;
    private static final int ADDRESS_MAX_LENGTH = 255;

    /**
     * 公司AddressMapper数据访问接口。
     *
     * @return 处理结果
     */
    @Resource
    private CompanyAddressMapper companyAddressMapper;

    @Resource
    private CompanyDataAccessContext companyDataAccessContext;

    /**
     * 查询list相关业务数据。
     *
     * <p>说明：该方法用于执行业务流程编排，确保调用链路清晰可维护。</p>
     * @return 处理结果
     */
    @Override
    public List<CompanyAddressVO> list() {
        // 调用requireCurrentCompanyId方法，复用统一能力并保证业务规则一致。
        Long companyId = requireCurrentCompanyId();
        return listEntities(companyId).stream().map(this::buildAddressVO).collect(Collectors.toList());
    }

    /**
     * 根据ID查询公司Address详情。
     *
     * @param addressId address ID
     * @return 处理结果
     */
    @Override
    public CompanyAddressVO getById(Long addressId) {
        return buildAddressVO(requireOwnedAddress(addressId, requireCurrentCompanyId()));
    }

    /**
     * 创建公司Address。
     *
     * @param dto 参数
     * @return 处理结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(CompanyAddressCreateDTO dto) {
        // 说明：执行该步骤以保证业务流程正确。
        Long companyId = requireCurrentCompanyId();
        // 调用listEntities方法，复用统一能力并保证业务规则一致。
        List<CompanyAddress> currentAddresses = listEntities(companyId);
        if (currentAddresses.size() >= MAX_ADDRESS_COUNT) {
            throw new ServiceException("最多只能保存20条地址，请先删除一条后再新增");
        }
        // 调用CompanyAddress方法，复用统一能力并保证业务规则一致。
        CompanyAddress entity = new CompanyAddress();
        // 调用setCompanyId方法，复用统一能力并保证业务规则一致。
        entity.setCompanyId(companyId);
        // 调用getAddress方法，复用统一能力并保证业务规则一致。
        fillAddress(entity, dto.getContactName(), dto.getContactPhone(), dto.getAddress());
        // 调用getIsDefault方法，复用统一能力并保证业务规则一致。
        boolean shouldSetDefault = currentAddresses.isEmpty() || isDefaultFlag(dto.getIsDefault());
        // 调用setIsDefault方法，复用统一能力并保证业务规则一致。
        entity.setIsDefault(shouldSetDefault ? 1 : 0);
        if (shouldSetDefault) {
            // 调用clearDefault方法，复用统一能力并保证业务规则一致。
            clearDefault(companyId, null);
        }
        // 说明：执行该步骤以保证业务流程正确。
        companyAddressMapper.insert(entity);
        return entity.getId();
    }

    /**
     * 更新公司Address。
     *
     * @param dto 参数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(CompanyAddressUpdateDTO dto) {
        // 说明：执行该步骤以保证业务流程正确。
        Long companyId = requireCurrentCompanyId();
        // 调用getId方法，复用统一能力并保证业务规则一致。
        CompanyAddress entity = requireOwnedAddress(dto.getId(), companyId);
        // 调用getAddress方法，复用统一能力并保证业务规则一致。
        fillAddress(entity, dto.getContactName(), dto.getContactPhone(), dto.getAddress());
        // 调用getIsDefault方法，复用统一能力并保证业务规则一致。
        boolean setDefault = isDefaultFlag(dto.getIsDefault());
        // 调用getIsDefault方法，复用统一能力并保证业务规则一致。
        boolean wasDefault = isDefaultFlag(entity.getIsDefault());
        if (setDefault) {
            // 调用getId方法，复用统一能力并保证业务规则一致。
            clearDefault(companyId, entity.getId());
            // 调用setIsDefault方法，复用统一能力并保证业务规则一致。
            entity.setIsDefault(1);
        } else if (wasDefault) {
            // 调用getId方法，复用统一能力并保证业务规则一致。
            entity.setIsDefault(hasOtherAddress(companyId, entity.getId()) ? 0 : 1);
        } else {
            // 调用setIsDefault方法，复用统一能力并保证业务规则一致。
            entity.setIsDefault(0);
        }
        // 说明：执行该步骤以保证业务流程正确。
        companyAddressMapper.updateById(entity);
        if (!setDefault && wasDefault && entity.getIsDefault() == 0) {
            // 调用getId方法，复用统一能力并保证业务规则一致。
            promoteLatestAddressAsDefault(companyId, entity.getId());
        }
    }

    /**
     * 删除公司Address。
     *
     * @param addressId address ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long addressId) {
        // 说明：执行该步骤以保证业务流程正确。
        Long companyId = requireCurrentCompanyId();
        // 调用requireOwnedAddress方法，复用统一能力并保证业务规则一致。
        CompanyAddress entity = requireOwnedAddress(addressId, companyId);
        // 说明：执行该步骤以保证业务流程正确。
        companyAddressMapper.deleteById(entity.getId());
        if (!isDefaultFlag(entity.getIsDefault())) {
            return;
        }
        // 调用listEntities方法，复用统一能力并保证业务规则一致。
        List<CompanyAddress> remaining = listEntities(companyId);
        if (remaining.isEmpty()) {
            return;
        }
        // 调用getId方法，复用统一能力并保证业务规则一致。
        setDefaultInternal(companyId, remaining.get(0).getId());
    }

    /**
     * setDefault。
     *
     * @param addressId address ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setDefault(Long addressId) {
        // 说明：执行该步骤以保证业务流程正确。
        Long companyId = requireCurrentCompanyId();
        // 调用requireOwnedAddress方法，复用统一能力并保证业务规则一致。
        CompanyAddress entity = requireOwnedAddress(addressId, companyId);
        if (isDefaultFlag(entity.getIsDefault())) {
            return;
        }
        // 调用getId方法，复用统一能力并保证业务规则一致。
        setDefaultInternal(companyId, entity.getId());
    }

    /**
     * requireCurrent公司ID。
     *
     * @return 处理结果
     */
    private Long requireCurrentCompanyId() {
        // 调用resolveCompanyId方法，复用统一能力并保证业务规则一致。
        Long companyId = companyDataAccessContext.resolveCompanyId();
        if (companyId == null) {
            throw new ServiceException("缺少公司数据访问上下文");
        }
        return companyId;
    }

    /**
     * requireOwnedAddress。
     *
     * @param addressId address ID
     * @return 处理结果
     */
    private CompanyAddress requireOwnedAddress(Long addressId, Long companyId) {
        // 说明：执行该步骤以保证业务流程正确。
        CompanyAddress entity = companyAddressMapper.selectById(addressId);
        if (entity == null) {
            throw new ServiceException("地址不存在");
        }
        if (!companyId.equals(entity.getCompanyId())) {
            throw new ServiceException("无权操作该地址");
        }
        return entity;
    }

    /**
     * 分页查询Entities列表。
     *
     * @return 处理结果
     */
    private List<CompanyAddress> listEntities(Long companyId) {
        // 说明：执行该步骤以保证业务流程正确。
        List<CompanyAddress> entities = companyAddressMapper.selectList(
                new LambdaQueryWrapper<CompanyAddress>().eq(CompanyAddress::getCompanyId, companyId)
        );
        if (entities == null || entities.isEmpty()) {
            return new ArrayList<>();
        }
        return entities.stream()
                .filter(item -> companyId.equals(item.getCompanyId()))
                .sorted(this::compareAddress)
                // 调用toList方法，复用统一能力并保证业务规则一致。
                .collect(Collectors.toList());
    }

    /**
     * clearDefault。
     *
     * @param keepId keep ID
     */
    private void clearDefault(Long companyId, Long keepId) {
        // 调用listEntities方法，复用统一能力并保证业务规则一致。
        List<CompanyAddress> entities = listEntities(companyId);
        for (CompanyAddress item : entities) {
            if (!isDefaultFlag(item.getIsDefault())) {
                continue;
            }
            if (keepId != null && keepId.equals(item.getId())) {
                continue;
            }
            // 调用setIsDefault方法，复用统一能力并保证业务规则一致。
            item.setIsDefault(0);
            // 说明：执行该步骤以保证业务流程正确。
            companyAddressMapper.updateById(item);
        }
    }

    /**
     * setDefaultInternal。
     *
     * @param addressId address ID
     */
    private void setDefaultInternal(Long companyId, Long addressId) {
        // 调用clearDefault方法，复用统一能力并保证业务规则一致。
        clearDefault(companyId, addressId);
        // 说明：执行该步骤以保证业务流程正确。
        CompanyAddress entity = requireOwnedAddress(addressId, companyId);
        // 调用setIsDefault方法，复用统一能力并保证业务规则一致。
        entity.setIsDefault(1);
        // 说明：执行该步骤以保证业务流程正确。
        companyAddressMapper.updateById(entity);
    }

    /**
     * 判断是否存在OtherAddress。
     *
     * @param currentAddressId current Address ID
     */
    private boolean hasOtherAddress(Long companyId, Long currentAddressId) {
        return listEntities(companyId).stream()
                // 调用getId方法，复用统一能力并保证业务规则一致。
                .anyMatch(item -> !currentAddressId.equals(item.getId()));
    }

    /**
     * promoteLatestAddressAsDefault。
     *
     * @param excludedAddressId excluded Address ID
     */
    private void promoteLatestAddressAsDefault(Long companyId, Long excludedAddressId) {
        listEntities(companyId).stream()
                .filter(item -> !excludedAddressId.equals(item.getId()))
                .findFirst()
                // 调用getId方法，复用统一能力并保证业务规则一致。
                .ifPresent(item -> setDefaultInternal(companyId, item.getId()));
    }

    /**
     * fillAddress。
     *
     * @param entity 参数
     * @param contactName 参数
     * @param contactPhone 参数
     * @param address 参数
     */
    private void fillAddress(CompanyAddress entity, String contactName, String contactPhone, String address) {
        entity.setContactName(normalizeTextWithLength(contactName, "联系人不能为空", CONTACT_NAME_MAX_LENGTH,
                "联系人长度不能超过64个字符"));
        entity.setContactPhone(normalizeTextWithLength(contactPhone, "联系电话不能为空", CONTACT_PHONE_MAX_LENGTH,
                "联系电话长度不能超过32个字符"));
        entity.setAddress(normalizeTextWithLength(address, "详细地址不能为空", ADDRESS_MAX_LENGTH,
                "详细地址长度不能超过255个字符"));
    }

    /**
     * 构建Address视图。
     *
     * @param entity 参数
     * @return 处理结果
     */
    private CompanyAddressVO buildAddressVO(CompanyAddress entity) {
        // 调用CompanyAddressVO方法，复用统一能力并保证业务规则一致。
        CompanyAddressVO vo = new CompanyAddressVO();
        // 调用getId方法，复用统一能力并保证业务规则一致。
        vo.setId(entity.getId());
        // 调用getCompanyId方法，复用统一能力并保证业务规则一致。
        vo.setCompanyId(entity.getCompanyId());
        // 调用getContactName方法，复用统一能力并保证业务规则一致。
        vo.setContactName(entity.getContactName());
        // 调用getContactPhone方法，复用统一能力并保证业务规则一致。
        vo.setContactPhone(entity.getContactPhone());
        // 调用getAddress方法，复用统一能力并保证业务规则一致。
        vo.setAddress(entity.getAddress());
        // 调用getIsDefault方法，复用统一能力并保证业务规则一致。
        vo.setIsDefault(entity.getIsDefault());
        return vo;
    }

    /**
     * 规范化TextWithLength。
     *
     * @param value 参数
     * @param requiredMessage 参数
     * @param maxLength 参数
     * @param tooLongMessage 参数
     * @return 处理结果
     */
    private String normalizeTextWithLength(String value, String requiredMessage, int maxLength, String tooLongMessage) {
        // 调用normalizeRequiredText方法，复用统一能力并保证业务规则一致。
        String normalized = normalizeRequiredText(value, requiredMessage);
        if (normalized.length() > maxLength) {
            throw new ServiceException(tooLongMessage);
        }
        return normalized;
    }

    /**
     * 规范化RequiredText。
     *
     * @param value 参数
     * @param message 参数
     * @return 处理结果
     */
    private String normalizeRequiredText(String value, String message) {
        // 调用normalizeText方法，复用统一能力并保证业务规则一致。
        String normalized = normalizeText(value);
        if (normalized == null) {
            throw new ServiceException(message);
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
     * 判断是否DefaultFlag。
     *
     * @param value 参数
     */
    private boolean isDefaultFlag(Integer value) {
        return value != null && value == 1;
    }

    /**
     * compareAddress。
     *
     * @param left 参数
     * @param right 参数
     * @return 处理结果
     */
    private int compareAddress(CompanyAddress left, CompanyAddress right) {
        // 调用defaultSortValue方法，复用统一能力并保证业务规则一致。
        int defaultCompare = Integer.compare(defaultSortValue(right), defaultSortValue(left));
        if (defaultCompare != 0) {
            return defaultCompare;
        }
        // 调用getUpdateTime方法，复用统一能力并保证业务规则一致。
        int updateCompare = compareDateTimeDesc(left.getUpdateTime(), right.getUpdateTime());
        if (updateCompare != 0) {
            return updateCompare;
        }
        return Long.compare(nullSafeId(right.getId()), nullSafeId(left.getId()));
    }

    /**
     * defaultSort值。
     *
     * @param entity 参数
     * @return 处理结果
     */
    private int defaultSortValue(CompanyAddress entity) {
        return isDefaultFlag(entity.getIsDefault()) ? 1 : 0;
    }

    /**
     * compareDateTime描述。
     *
     * @param left 参数
     * @param right 参数
     * @return 处理结果
     */
    private int compareDateTimeDesc(LocalDateTime left, LocalDateTime right) {
        if (left == null && right == null) {
            return 0;
        }
        if (left == null) {
            return 1;
        }
        if (right == null) {
            return -1;
        }
        return right.compareTo(left);
    }

    /**
     * nullSafeID。
     *
     * @return 处理结果
     */
    private long nullSafeId(Long id) {
        return id == null ? 0L : id;
    }
}


