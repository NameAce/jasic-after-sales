package com.jasic.aftersales.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jasic.aftersales.system.domain.entity.FirstSecondRelation;
import com.jasic.aftersales.system.domain.query.FirstSecondRelationQuery;
import com.jasic.aftersales.system.domain.vo.FirstSecondRelationVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 一级-二级从属关系 Mapper
 *
 * @author Zoro
 * @date 2026/03/18
 */
@Mapper
public interface FirstSecondRelationMapper extends BaseMapper<FirstSecondRelation> {

    /**
     * 分页查询一级-二级从属（关联公司名称）
     *
     * @param page  分页参数
     * @param query 查询条件
     * @return 分页结果
     */
    IPage<FirstSecondRelationVO> selectFirstSecondPage(Page<FirstSecondRelationVO> page, @Param("query") FirstSecondRelationQuery query);

}
