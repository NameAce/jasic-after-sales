package com.jasic.aftersales.customer.controller;

import com.jasic.aftersales.common.core.domain.Result;
import com.jasic.aftersales.common.enums.SysFileBizTypeEnum;
import com.jasic.aftersales.common.enums.SysFileUploadUserTypeEnum;
import com.jasic.aftersales.framework.security.StpCustomerUtil;
import com.jasic.aftersales.system.domain.dto.SysFileBizBindDTO;
import com.jasic.aftersales.system.domain.dto.SysFileBizUnbindDTO;
import com.jasic.aftersales.system.domain.vo.SysFileUploadVO;
import com.jasic.aftersales.system.service.SysFileService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
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
 * C端文件控制器
 *
 * @author Codex
 * @date 2026/04/07
 */
@Api(tags = "C端文件")
@RestController
@RequestMapping("/customer/file")
public class CustomerFileController {

    @Resource
    private SysFileService sysFileService;

    /**
     * 上传客户侧文件
     *
     * @param file 文件
     * @return 上传结果
     */
    @ApiOperation(value = "上传客户侧文件")
    @PostMapping("/upload")
    public Result<SysFileUploadVO> upload(@RequestParam("file") MultipartFile file) {
        // 说明：执行该步骤以保证业务流程正确。
        Long customerId = requireCustomerId();
        // 说明：执行该步骤以保证业务流程正确。
        return Result.ok(sysFileService.upload(
                file,
                "customer/work-order",
                customerId,
                SysFileUploadUserTypeEnum.CUSTOMER,
                null
        ));
    }

    /**
     * 按业务整组绑定文件
     *
     * @param dto 绑定参数
     * @return 操作结果
     */
    @ApiOperation(value = "按业务整组绑定文件")
    @PostMapping("/biz/bind")
    public Result<Void> bindBizFiles(@Validated @RequestBody SysFileBizBindDTO dto) {
        // 说明：执行该步骤以保证业务流程正确。
        Long customerId = requireCustomerId();
        // 说明：执行该步骤以保证业务流程正确。
        sysFileService.replaceBizFiles(
                dto.getBizType(),
                dto.getBizId(),
                dto.getFileIds(),
                null,
                customerId,
                SysFileUploadUserTypeEnum.CUSTOMER,
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
    @PostMapping("/biz/unbind")
    public Result<Void> unbindBizFile(@Validated @RequestBody SysFileBizUnbindDTO dto) {
        // 调用requireCustomerId方法，复用统一能力并保证业务规则一致。
        requireCustomerId();
        // 调用getFileId方法，复用统一能力并保证业务规则一致。
        sysFileService.unbindBizFile(dto.getBizType(), dto.getBizId(), dto.getFileId());
        return Result.ok();
    }

    /**
     * require客户ID。
     *
     * @return 处理结果
     */
    private Long requireCustomerId() {
        // 调用checkLogin方法，复用统一能力并保证业务规则一致。
        StpCustomerUtil.checkLogin();
        return StpCustomerUtil.getLoginIdAsLong();
    }
}




