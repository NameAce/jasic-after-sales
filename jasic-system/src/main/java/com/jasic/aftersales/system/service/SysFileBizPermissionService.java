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
 * @author Zoro
 * @date 2026/05/05
 */
@Service
public class SysFileBizPermissionService {

    /**WORK_ORDER_TYPES 常量，用于固定当前类内部复用的业务编码、默认值或配置边界。*/
    private static final EnumSet<SysFileBizTypeEnum> WORK_ORDER_TYPES = EnumSet.of(
            SysFileBizTypeEnum.WORK_ORDER_FAULT_IMAGE,
            SysFileBizTypeEnum.WORK_ORDER_FAULT_VIDEO,
            SysFileBizTypeEnum.WORK_ORDER_FAULT_VOICE,
            SysFileBizTypeEnum.WORK_ORDER_SENDER_VOUCHER,
            SysFileBizTypeEnum.WORK_ORDER_RETURN_VOUCHER
    );

    /**WORK_ORDER_REPAIR_TYPES 常量，用于固定当前类内部复用的业务编码、默认值或配置边界。*/
    private static final EnumSet<SysFileBizTypeEnum> WORK_ORDER_REPAIR_TYPES = EnumSet.of(
            SysFileBizTypeEnum.WORK_ORDER_REPAIR_OLD_IMAGE,
            SysFileBizTypeEnum.WORK_ORDER_REPAIR_NEW_IMAGE,
            SysFileBizTypeEnum.WORK_ORDER_REPAIR_MACHINE_IMAGE,
            SysFileBizTypeEnum.WORK_ORDER_REPAIR_BARCODE_IMAGE,
            SysFileBizTypeEnum.WORK_ORDER_REPAIR_OTHER_IMAGE
    );

    /**
     * 工单数据访问接口。
     */
    @Resource
    private WorkOrderMapper workOrderMapper;

    /**workOrderRepairMapper 依赖，用于协同完成当前业务流程中的数据访问、规则校验或状态处理。*/
    @Resource
    private WorkOrderRepairMapper workOrderRepairMapper;

    /**sysFileBizMapper 依赖，用于协同完成当前业务流程中的数据访问、规则校验或状态处理。*/
    @Resource
    private SysFileBizMapper sysFileBizMapper;

    /**workOrderPermissionService 依赖，用于协同完成当前业务流程中的数据访问、规则校验或状态处理。*/
    @Resource
    private WorkOrderPermissionService workOrderPermissionService;

    /**
     * 处理requireView业务逻辑。
     *
     * <p>说明：该方法用于执行业务流程编排，确保调用链路清晰可维护。</p>
     * @param bizType bizType，当前业务处理所需的输入值。
     * @param bizId 业务主键或关联对象ID。
     */
    public void requireView(SysFileBizTypeEnum bizType, Long bizId) {
        WorkOrder workOrder = resolveWorkOrder(bizType, bizId);
        if (!workOrderPermissionService.canView(workOrder)) {
            throw new ServiceException("无权查看该工单");
        }
    }

    /**
     * requireExecute。
     *
     * @param bizType bizType，当前业务处理所需的输入值。
     */
    public void requireExecute(SysFileBizTypeEnum bizType, Long bizId) {
        WorkOrderActionEnum action = resolveWriteAction(bizType, bizId);
        WorkOrder workOrder = resolveWorkOrder(bizType, bizId);
        if (!workOrderPermissionService.canExecute(workOrder, action)) {
            throw new ServiceException("无权操作该工单附件");
        }
    }

    /**
     * require文件BoundTo业务。
     *
     * @param bizType bizType，当前业务处理所需的输入值。
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
        if (sysFileBizMapper.selectOne(wrapper) == null) {
            throw new ServiceException("文件未绑定到当前业务对象");
        }
    }

    /**
     * 解析工单。
     *
     * @param bizType bizType，当前业务处理所需的输入值。
     * @return 业务处理结果
     */
    private WorkOrder resolveWorkOrder(SysFileBizTypeEnum bizType, Long bizId) {
        if (bizType == null || bizId == null) {
            throw new ServiceException("业务类型和业务ID不能为空");
        }
        Long workOrderId;
        if (WORK_ORDER_TYPES.contains(bizType)) {
            workOrderId = bizId;
        } else if (WORK_ORDER_REPAIR_TYPES.contains(bizType)) {
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
     * 解析Write动作。
     *
     * @param bizType bizType，当前业务处理所需的输入值。
     * @return 业务处理结果
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




