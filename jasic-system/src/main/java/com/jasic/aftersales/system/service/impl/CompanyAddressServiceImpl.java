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
     * ???????
     *
     * @return ????
     */
    @Resource
    private CompanyAddressMapper companyAddressMapper;

    @Resource
    private CompanyDataAccessContext companyDataAccessContext;

    @Override
    public List<CompanyAddressVO> list() {
        Long companyId = requireCurrentCompanyId();
        return listEntities(companyId).stream().map(this::buildAddressVO).collect(Collectors.toList());
    }

    /**
     * ??By Id?
     *
     * @param addressId address ID
     * @return ????
     */
    @Override
    public CompanyAddressVO getById(Long addressId) {
        return buildAddressVO(requireOwnedAddress(addressId, requireCurrentCompanyId()));
    }

    /**
     * ?????
     *
     * @param dto ????
     * @return ????
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(CompanyAddressCreateDTO dto) {
        // ?????????????????????????????
        Long companyId = requireCurrentCompanyId();
        List<CompanyAddress> currentAddresses = listEntities(companyId);
        if (currentAddresses.size() >= MAX_ADDRESS_COUNT) {
            throw new ServiceException("最多只能保存20条地址，请先删除一条后再新增");
        }
        CompanyAddress entity = new CompanyAddress();
        entity.setCompanyId(companyId);
        fillAddress(entity, dto.getContactName(), dto.getContactPhone(), dto.getAddress());
        boolean shouldSetDefault = currentAddresses.isEmpty() || isDefaultFlag(dto.getIsDefault());
        entity.setIsDefault(shouldSetDefault ? 1 : 0);
        if (shouldSetDefault) {
            clearDefault(companyId, null);
        }
        // ???????????????????????
        companyAddressMapper.insert(entity);
        return entity.getId();
    }

    /**
     * ?????
     *
     * @param dto ????
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(CompanyAddressUpdateDTO dto) {
        // ?????????????????????????????
        Long companyId = requireCurrentCompanyId();
        CompanyAddress entity = requireOwnedAddress(dto.getId(), companyId);
        fillAddress(entity, dto.getContactName(), dto.getContactPhone(), dto.getAddress());
        boolean setDefault = isDefaultFlag(dto.getIsDefault());
        boolean wasDefault = isDefaultFlag(entity.getIsDefault());
        if (setDefault) {
            clearDefault(companyId, entity.getId());
            entity.setIsDefault(1);
        } else if (wasDefault) {
            entity.setIsDefault(hasOtherAddress(companyId, entity.getId()) ? 0 : 1);
        } else {
            entity.setIsDefault(0);
        }
        // ???????????????????????
        companyAddressMapper.updateById(entity);
        if (!setDefault && wasDefault && entity.getIsDefault() == 0) {
            promoteLatestAddressAsDefault(companyId, entity.getId());
        }
    }

    /**
     * ?????
     *
     * @param addressId address ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long addressId) {
        // ?????????????????????????????
        Long companyId = requireCurrentCompanyId();
        CompanyAddress entity = requireOwnedAddress(addressId, companyId);
        // ???????????????????????
        companyAddressMapper.deleteById(entity.getId());
        if (!isDefaultFlag(entity.getIsDefault())) {
            return;
        }
        List<CompanyAddress> remaining = listEntities(companyId);
        if (remaining.isEmpty()) {
            return;
        }
        setDefaultInternal(companyId, remaining.get(0).getId());
    }

    /**
     * ??Default?
     *
     * @param addressId address ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setDefault(Long addressId) {
        // ?????????????????????????????
        Long companyId = requireCurrentCompanyId();
        CompanyAddress entity = requireOwnedAddress(addressId, companyId);
        if (isDefaultFlag(entity.getIsDefault())) {
            return;
        }
        setDefaultInternal(companyId, entity.getId());
    }

    /**
     * ??????????
     *
     * @return ????
     */
    private Long requireCurrentCompanyId() {
        Long companyId = companyDataAccessContext.resolveCompanyId();
        if (companyId == null) {
            throw new ServiceException("缺少公司数据访问上下文");
        }
        return companyId;
    }

    /**
     * ??????????
     *
     * @param addressId address ID
     * @param companyId ??ID
     * @return ????
     */
    private CompanyAddress requireOwnedAddress(Long addressId, Long companyId) {
        // ??????????????????????????
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
     * ???????
     *
     * @param companyId ??ID
     * @return ????
     */
    private List<CompanyAddress> listEntities(Long companyId) {
        // ??????????????????????????
        List<CompanyAddress> entities = companyAddressMapper.selectList(
                new LambdaQueryWrapper<CompanyAddress>().eq(CompanyAddress::getCompanyId, companyId)
        );
        if (entities == null || entities.isEmpty()) {
            return new ArrayList<>();
        }
        return entities.stream()
                .filter(item -> companyId.equals(item.getCompanyId()))
                .sorted(this::compareAddress)
                .collect(Collectors.toList());
    }

    /**
     * ?????
     *
     * @param companyId ??ID
     * @param keepId keep ID
     */
    private void clearDefault(Long companyId, Long keepId) {
        List<CompanyAddress> entities = listEntities(companyId);
        for (CompanyAddress item : entities) {
            if (!isDefaultFlag(item.getIsDefault())) {
                continue;
            }
            if (keepId != null && keepId.equals(item.getId())) {
                continue;
            }
            item.setIsDefault(0);
            // ???????????????????????
            companyAddressMapper.updateById(item);
        }
    }

    /**
     * ??Default Internal?
     *
     * @param companyId ??ID
     * @param addressId address ID
     */
    private void setDefaultInternal(Long companyId, Long addressId) {
        clearDefault(companyId, addressId);
        // ?????????????????????????????
        CompanyAddress entity = requireOwnedAddress(addressId, companyId);
        entity.setIsDefault(1);
        // ???????????????????????
        companyAddressMapper.updateById(entity);
    }

    /**
     * ??????Other Address?
     *
     * @param companyId ??ID
     * @param currentAddressId current Address ID
     * @return true ??????
     */
    private boolean hasOtherAddress(Long companyId, Long currentAddressId) {
        return listEntities(companyId).stream()
                .anyMatch(item -> !currentAddressId.equals(item.getId()));
    }

    /**
     * ?? promoteLatestAddressAsDefault ?????
     *
     * @param companyId ??ID
     * @param excludedAddressId excluded Address ID
     */
    private void promoteLatestAddressAsDefault(Long companyId, Long excludedAddressId) {
        listEntities(companyId).stream()
                .filter(item -> !excludedAddressId.equals(item.getId()))
                .findFirst()
                .ifPresent(item -> setDefaultInternal(companyId, item.getId()));
    }

    /**
     * ???????
     *
     * @param entity ????
     * @param contactName ??
     * @param contactPhone ??
     * @param address ??
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
     * ???????
     *
     * @param entity ????
     * @return ????
     */
    private CompanyAddressVO buildAddressVO(CompanyAddress entity) {
        CompanyAddressVO vo = new CompanyAddressVO();
        vo.setId(entity.getId());
        vo.setCompanyId(entity.getCompanyId());
        vo.setContactName(entity.getContactName());
        vo.setContactPhone(entity.getContactPhone());
        vo.setAddress(entity.getAddress());
        vo.setIsDefault(entity.getIsDefault());
        return vo;
    }

    /**
     * ????????
     *
     * @param value ???
     * @param requiredMessage ??
     * @param maxLength ??
     * @param tooLongMessage ??
     * @return ?????
     */
    private String normalizeTextWithLength(String value, String requiredMessage, int maxLength, String tooLongMessage) {
        String normalized = normalizeRequiredText(value, requiredMessage);
        if (normalized.length() > maxLength) {
            throw new ServiceException(tooLongMessage);
        }
        return normalized;
    }

    /**
     * ????????
     *
     * @param value ???
     * @param message ??
     * @return ?????
     */
    private String normalizeRequiredText(String value, String message) {
        String normalized = normalizeText(value);
        if (normalized == null) {
            throw new ServiceException(message);
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
     * ????Default Flag?
     *
     * @param value ???
     * @return true ??????
     */
    private boolean isDefaultFlag(Integer value) {
        return value != null && value == 1;
    }

    /**
     * ???????
     *
     * @param left ????
     * @param right ????
     * @return ????
     */
    private int compareAddress(CompanyAddress left, CompanyAddress right) {
        int defaultCompare = Integer.compare(defaultSortValue(right), defaultSortValue(left));
        if (defaultCompare != 0) {
            return defaultCompare;
        }
        int updateCompare = compareDateTimeDesc(left.getUpdateTime(), right.getUpdateTime());
        if (updateCompare != 0) {
            return updateCompare;
        }
        return Long.compare(nullSafeId(right.getId()), nullSafeId(left.getId()));
    }

    /**
     * ??????
     *
     * @param entity ????
     * @return ????
     */
    private int defaultSortValue(CompanyAddress entity) {
        return isDefaultFlag(entity.getIsDefault()) ? 1 : 0;
    }

    /**
     * ???????
     *
     * @param left ????
     * @param right ????
     * @return ????
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
     * ?? nullSafeId ?????
     *
     * @param id ??ID
     * @return ????
     */
    private long nullSafeId(Long id) {
        return id == null ? 0L : id;
    }
}
