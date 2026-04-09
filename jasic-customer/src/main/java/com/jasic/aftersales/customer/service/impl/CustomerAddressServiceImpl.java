package com.jasic.aftersales.customer.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jasic.aftersales.common.exception.ServiceException;
import com.jasic.aftersales.customer.domain.dto.CustomerAddressCreateDTO;
import com.jasic.aftersales.customer.domain.dto.CustomerAddressUpdateDTO;
import com.jasic.aftersales.customer.domain.entity.CUser;
import com.jasic.aftersales.customer.domain.entity.CustomerAddress;
import com.jasic.aftersales.customer.domain.vo.CustomerAddressVO;
import com.jasic.aftersales.customer.mapper.CustomerAddressMapper;
import com.jasic.aftersales.customer.service.ICustomerAddressService;
import com.jasic.aftersales.customer.service.ICUserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * C端客户地址 Service 实现
 *
 * @author Codex
 * @date 2026/04/08
 */
@Service
public class CustomerAddressServiceImpl implements ICustomerAddressService {

    private static final int MAX_ADDRESS_COUNT = 20;
    private static final Pattern MOBILE_PATTERN = Pattern.compile("^1\\d{10}$");
    private static final int CONTACT_NAME_MAX_LENGTH = 64;
    private static final int REGION_MAX_LENGTH = 64;
    private static final int DETAIL_ADDRESS_MAX_LENGTH = 255;

    @Resource
    private CustomerAddressMapper customerAddressMapper;

    @Resource
    private ICUserService cUserService;

    /**
     * 查询当前客户地址列表
     *
     * @return 地址列表
     */
    @Override
    public List<CustomerAddressVO> list() {
        Long customerId = requireCurrentCustomerId();
        return listEntities(customerId).stream().map(this::buildAddressVO).collect(Collectors.toList());
    }

    /**
     * 查询地址详情
     *
     * @param addressId 地址ID
     * @return 地址详情
     */
    @Override
    public CustomerAddressVO getById(Long addressId) {
        return buildAddressVO(requireOwnedAddress(addressId, requireCurrentCustomerId()));
    }

    /**
     * 新增地址
     *
     * @param dto 新增参数
     * @return 地址ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(CustomerAddressCreateDTO dto) {
        Long customerId = requireCurrentCustomerId();
        List<CustomerAddress> currentAddresses = listEntities(customerId);
        if (currentAddresses.size() >= MAX_ADDRESS_COUNT) {
            throw new ServiceException("最多只能保存20条地址，请先删除一条后再新增");
        }

        CustomerAddress entity = new CustomerAddress();
        entity.setCustomerId(customerId);
        fillAddress(entity, dto.getContactName(), dto.getContactMobile(), dto.getProvince(), dto.getCity(),
                dto.getCounty(), dto.getDetailAddress());

        boolean shouldSetDefault = currentAddresses.isEmpty() || isDefaultFlag(dto.getIsDefault());
        entity.setIsDefault(shouldSetDefault ? 1 : 0);
        if (shouldSetDefault) {
            clearDefault(customerId, null);
        }
        customerAddressMapper.insert(entity);
        return entity.getId();
    }

    /**
     * 修改地址
     *
     * @param dto 修改参数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(CustomerAddressUpdateDTO dto) {
        Long customerId = requireCurrentCustomerId();
        CustomerAddress entity = requireOwnedAddress(dto.getId(), customerId);
        fillAddress(entity, dto.getContactName(), dto.getContactMobile(), dto.getProvince(), dto.getCity(),
                dto.getCounty(), dto.getDetailAddress());
        customerAddressMapper.updateById(entity);
    }

    /**
     * 删除地址
     *
     * @param addressId 地址ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long addressId) {
        Long customerId = requireCurrentCustomerId();
        CustomerAddress entity = requireOwnedAddress(addressId, customerId);
        customerAddressMapper.deleteById(entity.getId());
        if (!isDefaultFlag(entity.getIsDefault())) {
            return;
        }
        List<CustomerAddress> remaining = listEntities(customerId);
        if (remaining.isEmpty()) {
            return;
        }
        setDefaultInternal(customerId, remaining.get(0).getId());
    }

    /**
     * 设为默认地址
     *
     * @param addressId 地址ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setDefault(Long addressId) {
        Long customerId = requireCurrentCustomerId();
        CustomerAddress entity = requireOwnedAddress(addressId, customerId);
        if (isDefaultFlag(entity.getIsDefault())) {
            return;
        }
        setDefaultInternal(customerId, entity.getId());
    }

    /**
     * 校验当前登录客户并返回客户ID
     *
     * @return 客户ID
     */
    private Long requireCurrentCustomerId() {
        CUser user = cUserService.getCurrentUser();
        return user.getId();
    }

    /**
     * 查询并校验地址归属
     *
     * @param addressId 地址ID
     * @param customerId 当前客户ID
     * @return 地址实体
     */
    private CustomerAddress requireOwnedAddress(Long addressId, Long customerId) {
        CustomerAddress entity = customerAddressMapper.selectById(addressId);
        if (entity == null) {
            throw new ServiceException("地址不存在");
        }
        if (!customerId.equals(entity.getCustomerId())) {
            throw new ServiceException("无权操作该地址");
        }
        return entity;
    }

    /**
     * 查询当前客户全部地址并统一排序，避免不同承载端出现顺序漂移。
     *
     * @param customerId 客户ID
     * @return 地址列表
     */
    private List<CustomerAddress> listEntities(Long customerId) {
        List<CustomerAddress> entities = customerAddressMapper.selectList(
                new LambdaQueryWrapper<CustomerAddress>().eq(CustomerAddress::getCustomerId, customerId)
        );
        if (entities == null || entities.isEmpty()) {
            return new ArrayList<>();
        }
        return entities.stream()
                .filter(item -> customerId.equals(item.getCustomerId()))
                .sorted(this::compareAddress)
                .collect(Collectors.toList());
    }

    /**
     * 清理当前客户已有默认地址。
     *
     * @param customerId 客户ID
     * @param keepId 保留为默认的地址ID
     */
    private void clearDefault(Long customerId, Long keepId) {
        List<CustomerAddress> entities = listEntities(customerId);
        for (CustomerAddress item : entities) {
            if (!isDefaultFlag(item.getIsDefault())) {
                continue;
            }
            if (keepId != null && keepId.equals(item.getId())) {
                continue;
            }
            item.setIsDefault(0);
            customerAddressMapper.updateById(item);
        }
    }

    /**
     * 切换默认地址并确保同一客户最多一条默认记录。
     *
     * @param customerId 客户ID
     * @param addressId 地址ID
     */
    private void setDefaultInternal(Long customerId, Long addressId) {
        clearDefault(customerId, addressId);
        CustomerAddress entity = requireOwnedAddress(addressId, customerId);
        entity.setIsDefault(1);
        customerAddressMapper.updateById(entity);
    }

    /**
     * 填充地址字段并做统一校验。
     *
     * @param entity 地址实体
     * @param contactName 联系人
     * @param contactMobile 联系手机号
     * @param province 省
     * @param city 市
     * @param county 区县
     * @param detailAddress 详细地址
     */
    private void fillAddress(CustomerAddress entity, String contactName, String contactMobile, String province,
                             String city, String county, String detailAddress) {
        entity.setContactName(normalizeTextWithLength(contactName, "联系人不能为空", CONTACT_NAME_MAX_LENGTH,
                "联系人长度不能超过64个字符"));
        entity.setContactMobile(normalizeMobile(contactMobile));
        entity.setProvince(normalizeTextWithLength(province, "省不能为空", REGION_MAX_LENGTH, "省长度不能超过64个字符"));
        entity.setCity(normalizeTextWithLength(city, "市不能为空", REGION_MAX_LENGTH, "市长度不能超过64个字符"));
        entity.setCounty(normalizeOptionalText(county, REGION_MAX_LENGTH, "区县长度不能超过64个字符"));
        entity.setDetailAddress(normalizeTextWithLength(detailAddress, "详细地址不能为空", DETAIL_ADDRESS_MAX_LENGTH,
                "详细地址长度不能超过255个字符"));
    }

    /**
     * 构建地址视图
     *
     * @param entity 地址实体
     * @return 地址视图
     */
    private CustomerAddressVO buildAddressVO(CustomerAddress entity) {
        CustomerAddressVO vo = new CustomerAddressVO();
        vo.setId(entity.getId());
        vo.setContactName(entity.getContactName());
        vo.setContactMobile(entity.getContactMobile());
        vo.setProvince(entity.getProvince());
        vo.setCity(entity.getCity());
        vo.setCounty(entity.getCounty());
        vo.setDetailAddress(entity.getDetailAddress());
        vo.setIsDefault(entity.getIsDefault());
        vo.setFullAddress(buildFullAddress(entity));
        return vo;
    }

    /**
     * 组装完整地址文本
     *
     * @param entity 地址实体
     * @return 完整地址
     */
    private String buildFullAddress(CustomerAddress entity) {
        StringBuilder builder = new StringBuilder();
        appendIfHasText(builder, entity.getProvince());
        appendIfHasText(builder, entity.getCity());
        appendIfHasText(builder, entity.getCounty());
        appendIfHasText(builder, entity.getDetailAddress());
        return builder.toString();
    }

    /**
     * 追加非空地址片段
     *
     * @param builder 字符串构建器
     * @param value 地址片段
     */
    private void appendIfHasText(StringBuilder builder, String value) {
        if (StringUtils.hasText(value)) {
            builder.append(value.trim());
        }
    }

    /**
     * 统一清洗必填文本并校验长度。
     *
     * @param value 原始值
     * @param requiredMessage 必填提示
     * @param maxLength 最大长度
     * @param tooLongMessage 超长提示
     * @return 清洗后的文本
     */
    private String normalizeTextWithLength(String value, String requiredMessage, int maxLength, String tooLongMessage) {
        String normalized = normalizeRequiredText(value, requiredMessage);
        if (normalized.length() > maxLength) {
            throw new ServiceException(tooLongMessage);
        }
        return normalized;
    }

    /**
     * 统一清洗可选文本并校验长度。
     *
     * @param value 原始值
     * @param maxLength 最大长度
     * @param tooLongMessage 超长提示
     * @return 清洗后的文本
     */
    private String normalizeOptionalText(String value, int maxLength, String tooLongMessage) {
        String normalized = normalizeText(value);
        if (normalized != null && normalized.length() > maxLength) {
            throw new ServiceException(tooLongMessage);
        }
        return normalized;
    }

    /**
     * 统一清洗必填文本。
     *
     * @param value 原始值
     * @param message 必填提示
     * @return 清洗后的文本
     */
    private String normalizeRequiredText(String value, String message) {
        String normalized = normalizeText(value);
        if (normalized == null) {
            throw new ServiceException(message);
        }
        return normalized;
    }

    /**
     * 校验手机号格式。
     *
     * @param mobile 原始手机号
     * @return 清洗后的手机号
     */
    private String normalizeMobile(String mobile) {
        String normalized = normalizeRequiredText(mobile, "联系手机号不能为空");
        if (!MOBILE_PATTERN.matcher(normalized).matches()) {
            throw new ServiceException("请输入正确的手机号");
        }
        return normalized;
    }

    /**
     * 清洗文本，空白按 null 处理。
     *
     * @param value 原始文本
     * @return 清洗后的文本
     */
    private String normalizeText(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    /**
     * 判断是否默认标记。
     *
     * @param value 标记值
     * @return 是否默认
     */
    private boolean isDefaultFlag(Integer value) {
        return value != null && value == 1;
    }

    /**
     * 统一地址排序：默认地址优先，其次按最近更新时间倒序。
     *
     * @param left 左侧地址
     * @param right 右侧地址
     * @return 比较结果
     */
    private int compareAddress(CustomerAddress left, CustomerAddress right) {
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
     * 默认标记排序值
     *
     * @param entity 地址实体
     * @return 排序值
     */
    private int defaultSortValue(CustomerAddress entity) {
        return isDefaultFlag(entity.getIsDefault()) ? 1 : 0;
    }

    /**
     * 按时间倒序比较
     *
     * @param left 左侧时间
     * @param right 右侧时间
     * @return 比较结果
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
     * 兜底处理空ID
     *
     * @param id 主键
     * @return 非空排序值
     */
    private long nullSafeId(Long id) {
        return id == null ? 0L : id;
    }
}
