package com.jasic.aftersales.system.controller;

import com.jasic.aftersales.common.core.controller.BaseController;
import com.jasic.aftersales.common.core.domain.Result;
import com.jasic.aftersales.system.domain.entity.SysArea;
import com.jasic.aftersales.system.domain.vo.SysAreaOptionVO;
import com.jasic.aftersales.system.service.ISysAreaService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

/**
 * 行政区划接口
 *
 * @author Codex
 * @date 2026/04/17
 */
@Api(tags = "行政区划")
@RestController
@RequestMapping("/org/area")
public class SysAreaController extends BaseController {

    @Resource
    private ISysAreaService sysAreaService;

    /**
     * ???????
     *
     * @param parentCode ??
     * @return ????
     */
    @ApiOperation(value = "查询指定父级下的行政区划选项（基础参考数据，仅需登录即可访问）")
    @GetMapping("/options")
    public Result<List<SysAreaOptionVO>> listOptions(@RequestParam(required = false) String parentCode) {
        return Result.ok(sysAreaService.listOptionsByParentCode(parentCode));
    }

    /**
     * ??By Area Code?
     *
     * @param areaCode ??
     * @return ??????
     */
    @ApiOperation(value = "按编码查询行政区划（基础参考数据，仅需登录即可访问）")
    @GetMapping("/{areaCode}")
    public Result<SysArea> getByAreaCode(@PathVariable String areaCode) {
        return Result.ok(sysAreaService.getByAreaCode(areaCode));
    }
}
