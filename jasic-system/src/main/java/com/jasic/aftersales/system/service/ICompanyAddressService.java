package com.jasic.aftersales.system.service;

import com.jasic.aftersales.system.domain.dto.CompanyAddressCreateDTO;
import com.jasic.aftersales.system.domain.dto.CompanyAddressUpdateDTO;
import com.jasic.aftersales.system.domain.vo.CompanyAddressVO;

import java.util.List;

/**
 * 公司地址簿 Service 接口。
 *
 * @author Codex
 * @date 2026/04/11
 */
public interface ICompanyAddressService {

    /**
     * 查询当前公司地址列表。
     *
     * @return 地址列表
     */
    List<CompanyAddressVO> list();

    /**
     * 查询地址详情。
     *
     * @param addressId 地址ID
     * @return 地址详情
     */
    CompanyAddressVO getById(Long addressId);

    /**
     * 新增地址。
     *
     * @param dto 新增参数
     * @return 地址ID
     */
    Long create(CompanyAddressCreateDTO dto);

    /**
     * 修改地址。
     *
     * @param dto 修改参数
     */
    void update(CompanyAddressUpdateDTO dto);

    /**
     * 删除地址。
     *
     * @param addressId 地址ID
     */
    void delete(Long addressId);

    /**
     * 设为默认地址。
     *
     * @param addressId 地址ID
     */
    void setDefault(Long addressId);
}
