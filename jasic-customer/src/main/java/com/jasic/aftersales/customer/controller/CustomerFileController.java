package com.jasic.aftersales.customer.controller;

import com.jasic.aftersales.common.core.domain.Result;
import com.jasic.aftersales.common.enums.SysFileBizTypeEnum;
import com.jasic.aftersales.common.enums.SysFileUploadUserTypeEnum;
import com.jasic.aftersales.framework.security.StpCustomerUtil;
import com.jasic.aftersales.system.domain.dto.SysFileBizBindDTO;
import com.jasic.aftersales.system.domain.dto.SysFileBizUnbindDTO;
import com.jasic.aftersales.system.domain.vo.SysFileItemVO;
import com.jasic.aftersales.system.domain.vo.SysFilePreviewVO;
import com.jasic.aftersales.system.domain.vo.SysFileUploadVO;
import com.jasic.aftersales.system.service.SysFileService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.util.List;
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
        Long customerId = requireCustomerId();
        return Result.ok(sysFileService.upload(
                file,
                "customer/work-order",
                customerId,
                SysFileUploadUserTypeEnum.CUSTOMER,
                null
        ));
    }

    /**
     * 生成文件预览地址
     *
     * @param fileId 文件ID
     * @return 预览地址
     */
    @ApiOperation(value = "生成文件预览地址")
    @GetMapping("/{fileId}/preview-url")
    public Result<SysFilePreviewVO> getPreviewUrl(@PathVariable Long fileId) {
        requireCustomerId();
        return Result.ok(sysFileService.getPreviewUrl(fileId));
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
        Long customerId = requireCustomerId();
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
        requireCustomerId();
        sysFileService.unbindBizFile(dto.getBizType(), dto.getBizId(), dto.getFileId());
        return Result.ok();
    }

    /**
     * 查询业务文件列表
     *
     * @param bizType 业务类型
     * @param bizId   业务ID
     * @return 文件列表
     */
    @ApiOperation(value = "查询业务文件列表")
    @GetMapping("/biz/list")
    public Result<List<SysFileItemVO>> listBizFiles(@RequestParam SysFileBizTypeEnum bizType,
                                                    @RequestParam Long bizId) {
        requireCustomerId();
        return Result.ok(sysFileService.listBizFiles(bizType, bizId));
    }

    private Long requireCustomerId() {
        StpCustomerUtil.checkLogin();
        return StpCustomerUtil.getLoginIdAsLong();
    }
}
