package com.jasic.aftersales.customer.service;

import com.jasic.aftersales.customer.domain.dto.CustomerAddressCreateDTO;
import com.jasic.aftersales.customer.domain.dto.CustomerAddressUpdateDTO;
import com.jasic.aftersales.customer.domain.vo.CustomerAddressVO;

import java.util.List;

/**
 * C端客户地址 Service 接口
 *
 * @author Zoro
 * @date 2026/04/08
 */
public interface ICustomerAddressService {

    /**
     * 查询当前客户地址列表
     *
     * @return 地址列表
     */
    List<CustomerAddressVO> list();

    /**
     * 查询地址详情
     *
     * @param addressId 地址ID
     * @return 地址详情
     */
    CustomerAddressVO getById(Long addressId);

    /**
     * 新增地址
     *
     * @param dto 新增参数
     * @return 地址ID
     */
    Long create(CustomerAddressCreateDTO dto);

    /**
     * 修改地址
     *
     * @param dto 修改参数
     */
    void update(CustomerAddressUpdateDTO dto);

    /**
     * 删除地址
     *
     * @param addressId 地址ID
     */
    void delete(Long addressId);

    /**
     * 设为默认地址
     *
     * @param addressId 地址ID
     */
    void setDefault(Long addressId);
}
