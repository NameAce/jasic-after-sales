package com.jasic.aftersales.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jasic.aftersales.common.enums.SysFileBizTypeEnum;
import com.jasic.aftersales.common.enums.WorkOrderActionEnum;
import com.jasic.aftersales.common.exception.ServiceException;
import com.jasic.aftersales.system.domain.entity.SysFileBiz;
import com.jasic.aftersales.system.domain.entity.WorkOrder;
import com.jasic.aftersales.system.domain.entity.WorkOrderRepair;
import com.jasic.aftersales.system.mapper.SysFileBizMapper;
import com.jasic.aftersales.system.mapper.WorkOrderMapper;
import com.jasic.aftersales.system.mapper.WorkOrderRepairMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.EnumSet;

/**
 * 文件业务对象权限校验。
 *
 * @author Codex
 * @date 2026/05/05
 */
@Service
public class SysFileBizPermissionService {

    private static final EnumSet<SysFileBizTypeEnum> WORK_ORDER_TYPES = EnumSet.of(
            SysFileBizTypeEnum.WORK_ORDER_FAULT_IMAGE,
            SysFileBizTypeEnum.WORK_ORDER_FAULT_VIDEO,
            SysFileBizTypeEnum.WORK_ORDER_FAULT_VOICE,
            SysFileBizTypeEnum.WORK_ORDER_SENDER_VOUCHER,
            SysFileBizTypeEnum.WORK_ORDER_RETURN_VOUCHER
    );

    private static final EnumSet<SysFileBizTypeEnum> WORK_ORDER_REPAIR_TYPES = EnumSet.of(
            SysFileBizTypeEnum.WORK_ORDER_REPAIR_OLD_IMAGE,
            SysFileBizTypeEnum.WORK_ORDER_REPAIR_NEW_IMAGE,
            SysFileBizTypeEnum.WORK_ORDER_REPAIR_MACHINE_IMAGE,
            SysFileBizTypeEnum.WORK_ORDER_REPAIR_BARCODE_IMAGE,
            SysFileBizTypeEnum.WORK_ORDER_REPAIR_OTHER_IMAGE
    );

    /**
     * ??????????
     *
     * @param bizType ????
     * @param bizId ??ID
     */
    @Resource
    private WorkOrderMapper workOrderMapper;

    @Resource
    private WorkOrderRepairMapper workOrderRepairMapper;

    @Resource
    private SysFileBizMapper sysFileBizMapper;

    @Resource
    private WorkOrderPermissionService workOrderPermissionService;

    public void requireView(SysFileBizTypeEnum bizType, Long bizId) {
        WorkOrder workOrder = resolveWorkOrder(bizType, bizId);
        // ??????????????????????
        if (!workOrderPermissionService.canView(workOrder)) {
            throw new ServiceException("无权查看该工单");
        }
    }

    /**
     * ??????????
     *
     * @param bizType ????
     * @param bizId ??ID
     */
    public void requireExecute(SysFileBizTypeEnum bizType, Long bizId) {
        WorkOrderActionEnum action = resolveWriteAction(bizType, bizId);
        WorkOrder workOrder = resolveWorkOrder(bizType, bizId);
        // ??????????????????????
        if (!workOrderPermissionService.canExecute(workOrder, action)) {
            throw new ServiceException("无权操作该工单附件");
        }
    }

    /**
     * ??????????
     *
     * @param fileId ??ID
     * @param bizType ????
     * @param bizId ??ID
     */
    public void requireFileBoundToBiz(Long fileId, SysFileBizTypeEnum bizType, Long bizId) {
        if (fileId == null) {
            throw new ServiceException("文件ID不能为空");
        }
        LambdaQueryWrapper<SysFileBiz> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysFileBiz::getFileId, fileId)
                .eq(SysFileBiz::getBizType, bizType)
                .eq(SysFileBiz::getBizId, bizId)
                .last("limit 1");
        // ??????????????????????????
        if (sysFileBizMapper.selectOne(wrapper) == null) {
            throw new ServiceException("文件未绑定到当前业务对象");
        }
    }

    /**
     * ???????
     *
     * @param bizType ????
     * @param bizId ??ID
     * @return ????
     */
    private WorkOrder resolveWorkOrder(SysFileBizTypeEnum bizType, Long bizId) {
        if (bizType == null || bizId == null) {
            throw new ServiceException("业务类型和业务ID不能为空");
        }
        Long workOrderId;
        if (WORK_ORDER_TYPES.contains(bizType)) {
            workOrderId = bizId;
        } else if (WORK_ORDER_REPAIR_TYPES.contains(bizType)) {
            // ??????????????????????????
            WorkOrderRepair repair = workOrderRepairMapper.selectById(bizId);
            if (repair == null) {
                throw new ServiceException("维修记录不存在");
            }
            workOrderId = repair.getWorkOrderId();
        } else {
            throw new ServiceException("不支持的文件业务类型");
        }
        WorkOrder workOrder = workOrderMapper.selectById(workOrderId);
        if (workOrder == null) {
            throw new ServiceException("工单不存在");
        }
        return workOrder;
    }

    /**
     * ???????
     *
     * @param bizType ????
     * @param bizId ??ID
     * @return ????
     */
    private WorkOrderActionEnum resolveWriteAction(SysFileBizTypeEnum bizType, Long bizId) {
        if (bizType == null || bizId == null) {
            throw new ServiceException("业务类型和业务ID不能为空");
        }
        if (SysFileBizTypeEnum.WORK_ORDER_SENDER_VOUCHER == bizType) {
            return WorkOrderActionEnum.UPLOAD_SEND_EXPRESS;
        }
        if (SysFileBizTypeEnum.WORK_ORDER_RETURN_VOUCHER == bizType) {
            return WorkOrderActionEnum.CLOSE;
        }
        if (WORK_ORDER_REPAIR_TYPES.contains(bizType)) {
            // ??????????????????????????
            WorkOrderRepair repair = workOrderRepairMapper.selectById(bizId);
            if (repair == null) {
                throw new ServiceException("维修记录不存在");
            }
            return "RECHECK".equals(repair.getRegisterStage())
                    ? WorkOrderActionEnum.REVIEW
                    : WorkOrderActionEnum.REPAIR_FINISH;
        }
        if (WORK_ORDER_TYPES.contains(bizType)) {
            throw new ServiceException("该附件类型不支持通用文件入口写入，请走工单业务接口");
        }
        throw new ServiceException("不支持的文件业务类型");
    }
}
