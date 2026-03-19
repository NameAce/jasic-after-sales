package com.jasic.aftersales.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jasic.aftersales.system.domain.entity.HqFirstContract;
import com.jasic.aftersales.system.domain.query.HqFirstContractQuery;
import com.jasic.aftersales.system.domain.vo.HqFirstContractVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 总部-一级签约关系 Mapper
 *
 * @author Zoro
 * @date 2026/03/18
 */
@Mapper
public interface HqFirstContractMapper extends BaseMapper<HqFirstContract> {

    /**
     * 分页查询总部-一级签约（关联公司、大区名称）
     *
     * @param page  分页参数
     * @param query 查询条件
     * @return 分页结果
     */
    IPage<HqFirstContractVO> selectHqFirstPage(Page<HqFirstContractVO> page, @Param("query") HqFirstContractQuery query);

}
