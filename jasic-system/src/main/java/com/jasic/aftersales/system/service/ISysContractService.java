package com.jasic.aftersales.system.service;

import com.jasic.aftersales.common.core.domain.PageResult;
import com.jasic.aftersales.system.domain.dto.FirstSecondRelationDTO;
import com.jasic.aftersales.system.domain.dto.HqFirstContractDTO;
import com.jasic.aftersales.system.domain.entity.FirstSecondRelation;
import com.jasic.aftersales.system.domain.entity.HqFirstContract;
import com.jasic.aftersales.system.domain.query.FirstSecondRelationQuery;
import com.jasic.aftersales.system.domain.query.HqFirstContractQuery;
import com.jasic.aftersales.system.domain.vo.FirstSecondRelationVO;
import com.jasic.aftersales.system.domain.vo.HqFirstContractVO;

/**
 * 签约管理 Service 接口
 *
 * @author Zoro
 * @date 2026/03/18
 */
public interface ISysContractService {

    /**
     * 总部-一级签约分页列表
     *
     * @param query 查询参数
     * @return 分页结果
     */
    PageResult<HqFirstContractVO> listHqFirstPage(HqFirstContractQuery query);

    /**
     * 新增总部-一级签约
     *
     * @param dto 签约参数
     * @return 主键ID
     */
    Long saveHqFirst(HqFirstContractDTO dto);

    /**
     * 修改总部-一级签约
     *
     * @param dto 签约参数
     */
    void updateHqFirst(HqFirstContractDTO dto);

    /**
     * 删除总部-一级签约
     *
     * @param id 主键ID
     */
    void removeHqFirst(Long id);

    /**
     * 一级-二级从属分页列表
     *
     * @param query 查询参数
     * @return 分页结果
     */
    PageResult<FirstSecondRelationVO> listFirstSecondPage(FirstSecondRelationQuery query);

    /**
     * 新增一级-二级从属
     *
     * @param dto 从属关系参数
     * @return 主键ID
     */
    Long saveFirstSecond(FirstSecondRelationDTO dto);

    /**
     * 删除一级-二级从属
     *
     * @param id 主键ID
     */
    void removeFirstSecond(Long id);
}
