package com.jasic.aftersales.system.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jasic.aftersales.common.enums.SysFileBizTypeEnum;
import com.jasic.aftersales.system.domain.entity.SysFileBiz;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 文件业务关系 Mapper
 *
 * @author Codex
 * @date 2026/04/07
 */
public interface SysFileBizMapper extends BaseMapper<SysFileBiz> {

    /**
     * 按业务类型查询附件关系，忽略公司隔离，确保工单详情可见跨来源附件。
     *
     * @param bizType 业务类型
     * @param bizId 业务ID
     * @return 附件关系列表
     */
    @InterceptorIgnore(tenantLine = "true")
    @Select({
            "<script>",
            "SELECT *",
            "FROM sys_file_biz",
            "WHERE biz_type = #{bizType}",
            "  AND biz_id = #{bizId}",
            "ORDER BY sort_num ASC, id ASC",
            "</script>"
    })
    List<SysFileBiz> selectVisibleBizRelations(@Param("bizType") SysFileBizTypeEnum bizType,
                                               @Param("bizId") Long bizId);

    /**
     * 按业务类型列表批量查询附件关系，忽略公司隔离，统一用于工单附件聚合。
     *
     * @param bizTypes 业务类型列表
     * @param bizId 业务ID
     * @return 附件关系列表
     */
    @InterceptorIgnore(tenantLine = "true")
    @Select({
            "<script>",
            "SELECT *",
            "FROM sys_file_biz",
            "WHERE biz_id = #{bizId}",
            "  AND biz_type IN",
            "  <foreach collection='bizTypes' item='bizType' open='(' separator=',' close=')'>",
            "    #{bizType}",
            "  </foreach>",
            "ORDER BY biz_type ASC, sort_num ASC, id ASC",
            "</script>"
    })
    List<SysFileBiz> selectVisibleBizRelationsByTypes(@Param("bizTypes") List<SysFileBizTypeEnum> bizTypes,
                                                      @Param("bizId") Long bizId);
}
