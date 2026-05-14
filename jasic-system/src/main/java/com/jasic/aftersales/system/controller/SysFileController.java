package com.jasic.aftersales.system.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.jasic.aftersales.common.annotation.OperLog;
import com.jasic.aftersales.common.core.domain.Result;
import com.jasic.aftersales.common.enums.OperTypeEnum;
import com.jasic.aftersales.common.enums.SysFileUploadUserTypeEnum;
import com.jasic.aftersales.framework.security.SecurityContext;
import com.jasic.aftersales.system.domain.dto.SysFileBizBindDTO;
import com.jasic.aftersales.system.domain.dto.SysFileBizUnbindDTO;
import com.jasic.aftersales.system.domain.vo.SysFileUploadVO;
import com.jasic.aftersales.system.service.SysFileBizPermissionService;
import com.jasic.aftersales.system.service.SysFileService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

/**
 * 系统侧文件控制器
 *
 * @author Codex
 * @date 2026/04/07
 */
@Api(tags = "系统侧文件")
@RestController
@SaCheckLogin
@RequestMapping("/system/file")
public class SysFileController {

    @Resource
    private SysFileService sysFileService;

    @Resource
    private SysFileBizPermissionService sysFileBizPermissionService;

    /**
     * 上传系统侧文件
     *
     * @param file 文件
     * @return 上传结果
     */
    @ApiOperation(value = "上传系统侧文件")
    @OperLog(title = "文件中心", operType = OperTypeEnum.INSERT)
    @PostMapping("/upload")
    public Result<SysFileUploadVO> upload(@RequestParam("file") MultipartFile file) {
        // 说明：执行该步骤以保证业务流程正确。
        return Result.ok(sysFileService.upload(
                file,
                "system/work-order",
                SecurityContext.getCurrentUserId(),
                SysFileUploadUserTypeEnum.SYSTEM,
                SecurityContext.getCurrentCompanyId()
        ));
    }

    /**
     * 按业务整组绑定文件
     *
     * @param dto 绑定参数
     * @return 操作结果
     */
    @ApiOperation(value = "按业务整组绑定文件")
    @OperLog(title = "文件中心", operType = OperTypeEnum.UPDATE)
    @PostMapping("/biz/bind")
    public Result<Void> bindBizFiles(@Validated @RequestBody SysFileBizBindDTO dto) {
        // 说明：执行该步骤以保证业务流程正确。
        sysFileBizPermissionService.requireExecute(dto.getBizType(), dto.getBizId());
        // 说明：执行该步骤以保证业务流程正确。
        sysFileService.replaceBizFiles(
                dto.getBizType(),
                dto.getBizId(),
                dto.getFileIds(),
                SecurityContext.getCurrentCompanyId(),
                SecurityContext.getCurrentUserId(),
                SysFileUploadUserTypeEnum.SYSTEM,
                dto.getRemark()
        );
        return Result.ok();
    }

    /**
     * 解绑单个业务文件
     *
     * @param dto 解绑参数
     * @return 操作结果
     */
    @ApiOperation(value = "解绑单个业务文件")
    @OperLog(title = "文件中心", operType = OperTypeEnum.UPDATE)
    @PostMapping("/biz/unbind")
    public Result<Void> unbindBizFile(@Validated @RequestBody SysFileBizUnbindDTO dto) {
        // 调用getBizId方法，复用统一能力并保证业务规则一致。
        sysFileBizPermissionService.requireExecute(dto.getBizType(), dto.getBizId());
        // 调用getFileId方法，复用统一能力并保证业务规则一致。
        sysFileService.unbindBizFile(dto.getBizType(), dto.getBizId(), dto.getFileId());
        return Result.ok();
    }

}



