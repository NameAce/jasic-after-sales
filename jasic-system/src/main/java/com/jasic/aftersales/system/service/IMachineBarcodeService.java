package com.jasic.aftersales.system.service;

import com.jasic.aftersales.common.core.domain.PageResult;
import com.jasic.aftersales.system.domain.dto.MachineBarcodeDTO;
import com.jasic.aftersales.system.domain.dto.MachineBarcodeImportItemDTO;
import com.jasic.aftersales.system.domain.query.MachineBarcodeQuery;
import com.jasic.aftersales.system.domain.vo.MachineBarcodeVO;
import com.jasic.aftersales.system.domain.vo.SysCompanySimpleVO;

import java.util.List;

/**
 * 条码档案管理 Service 接口
 *
 * @author Codex
 * @date 2026/04/01
 */
public interface IMachineBarcodeService {

    /**
     * 分页查询条码档案
     *
     * @param query 查询参数
     * @return 分页结果
     */
    PageResult<MachineBarcodeVO> listPage(MachineBarcodeQuery query);

    /**
     * 根据 ID 查询条码档案
     *
     * @param id 主键
     * @return 条码档案详情
     */
    MachineBarcodeVO getById(Long id);

    /**
     * 查询可选总部列表
     *
     * @return 总部选项
     */
    List<SysCompanySimpleVO> listHqCompanyOptions();

    /**
     * 新增条码档案
     *
     * @param dto 新增参数
     * @return 主键 ID
     */
    Long save(MachineBarcodeDTO dto);

    /**
     * 修改条码档案
     *
     * @param dto 修改参数
     */
    void update(MachineBarcodeDTO dto);

    /**
     * 删除条码档案
     *
     * @param id 主键 ID
     */
    void remove(Long id);

    /**
     * 批量导入条码档案，按条码覆盖更新
     *
     * @param items 导入列表
     * @return 导入条数
     */
    Integer importItems(List<MachineBarcodeImportItemDTO> items);
}
