package com.jasic.aftersales.common.constant;

import org.junit.Assert;
import org.junit.Test;

/**
 * 工单状态常量测试。
 *
 * @author Zoro
 * @date 2026/03/31
 */
public class WorkOrderStatusConstantsTest {

    /**验证ResolveMainStatusLabel，保证相关业务规则在回归场景下保持稳定。*/
    @Test
    public void shouldResolveMainStatusLabel() {
        Assert.assertEquals("待派单",
                WorkOrderStatusConstants.resolveMainStatusLabel(WorkOrderStatusConstants.MainStatus.PENDING_ASSIGN));
        Assert.assertEquals("待接单",
                WorkOrderStatusConstants.resolveMainStatusLabel(WorkOrderStatusConstants.MainStatus.PENDING_TECH_ACCEPT));
        Assert.assertEquals("维修中",
                WorkOrderStatusConstants.resolveMainStatusLabel(WorkOrderStatusConstants.MainStatus.IN_PROGRESS));
        Assert.assertEquals("已完成",
                WorkOrderStatusConstants.resolveMainStatusLabel(WorkOrderStatusConstants.MainStatus.COMPLETED));
        Assert.assertEquals("已关闭",
                WorkOrderStatusConstants.resolveMainStatusLabel(WorkOrderStatusConstants.MainStatus.CLOSED));
    }

    /**验证ResolveDisplayStatusForWaitAccept，保证相关业务规则在回归场景下保持稳定。*/
    @Test
    public void shouldResolveDisplayStatusForWaitAccept() {
        Assert.assertTrue(WorkOrderStatusConstants.isWaitAcceptMainStatus(WorkOrderStatusConstants.MainStatus.PENDING_ASSIGN));
        Assert.assertTrue(WorkOrderStatusConstants.isWaitAcceptMainStatus(WorkOrderStatusConstants.MainStatus.PENDING_TECH_ACCEPT));
        Assert.assertEquals(WorkOrderStatusConstants.DisplayStatus.WAIT_ACCEPT,
                WorkOrderStatusConstants.resolveDisplayStatus(WorkOrderStatusConstants.MainStatus.PENDING_ASSIGN));
        Assert.assertEquals(WorkOrderStatusConstants.DisplayStatus.WAIT_ACCEPT,
                WorkOrderStatusConstants.resolveDisplayStatus(WorkOrderStatusConstants.MainStatus.PENDING_TECH_ACCEPT));
        Assert.assertEquals("待接单",
                WorkOrderStatusConstants.resolveDisplayStatusLabel(WorkOrderStatusConstants.MainStatus.PENDING_ASSIGN));
        Assert.assertEquals("待接单",
                WorkOrderStatusConstants.resolveDisplayStatusLabel(WorkOrderStatusConstants.MainStatus.PENDING_TECH_ACCEPT));
    }

    /**验证ResolveEvaluateStatusLabel，保证相关业务规则在回归场景下保持稳定。*/
    @Test
    public void shouldResolveEvaluateStatusLabel() {
        Assert.assertEquals("未开启评价",
                WorkOrderStatusConstants.resolveEvaluateStatusLabel(WorkOrderStatusConstants.EvaluateStatus.NOT_OPEN));
        Assert.assertEquals("待评价",
                WorkOrderStatusConstants.resolveEvaluateStatusLabel(WorkOrderStatusConstants.EvaluateStatus.PENDING_EVALUATE));
        Assert.assertEquals("已评价",
                WorkOrderStatusConstants.resolveEvaluateStatusLabel(WorkOrderStatusConstants.EvaluateStatus.EVALUATED));
    }

    /**验证JudgeTransferableStatus，保证相关业务规则在回归场景下保持稳定。*/
    @Test
    public void shouldJudgeTransferableStatus() {
        Assert.assertFalse(WorkOrderStatusConstants.canTransfer(WorkOrderStatusConstants.MainStatus.PENDING_ASSIGN));
        Assert.assertFalse(WorkOrderStatusConstants.canTransfer(WorkOrderStatusConstants.MainStatus.PENDING_TECH_ACCEPT));
        Assert.assertTrue(WorkOrderStatusConstants.canTransfer(WorkOrderStatusConstants.MainStatus.IN_PROGRESS));
        Assert.assertTrue(WorkOrderStatusConstants.canTransfer(WorkOrderStatusConstants.MainStatus.COMPLETED));
        Assert.assertFalse(WorkOrderStatusConstants.canTransfer(WorkOrderStatusConstants.MainStatus.CLOSED));
    }

    /**验证FollowFrozenStatusFlow，保证相关业务规则在回归场景下保持稳定。*/
    @Test
    public void shouldFollowFrozenStatusFlow() {
        Assert.assertEquals(WorkOrderStatusConstants.MainStatus.PENDING_ASSIGN, WorkOrderStatusFlow.afterCreate());
        Assert.assertEquals(WorkOrderStatusConstants.EvaluateStatus.NOT_OPEN, WorkOrderStatusFlow.afterCreateEvaluateStatus());
        Assert.assertEquals(WorkOrderStatusConstants.MainStatus.PENDING_TECH_ACCEPT, WorkOrderStatusFlow.afterAssign());
        Assert.assertEquals(WorkOrderStatusConstants.MainStatus.IN_PROGRESS, WorkOrderStatusFlow.afterTechAccept());
        Assert.assertEquals(WorkOrderStatusConstants.MainStatus.PENDING_ASSIGN, WorkOrderStatusFlow.afterTransfer());
        Assert.assertEquals(WorkOrderStatusConstants.MainStatus.COMPLETED, WorkOrderStatusFlow.afterRepairFinish());
/*        Assert.assertEquals(WorkOrderStatusConstants.MainStatus.IN_PROGRESS, WorkOrderStatusFlow.afterReview(true));
        Assert.assertEquals(WorkOrderStatusConstants.MainStatus.COMPLETED, WorkOrderStatusFlow.afterReview(false));*/
        Assert.assertEquals(WorkOrderStatusConstants.MainStatus.CLOSED, WorkOrderStatusFlow.afterClose());
        Assert.assertEquals(WorkOrderStatusConstants.EvaluateStatus.PENDING_EVALUATE, WorkOrderStatusFlow.afterCloseEvaluateStatus());
        Assert.assertEquals(WorkOrderStatusConstants.EvaluateStatus.PENDING_EVALUATE, WorkOrderStatusFlow.afterCloseEvaluateStatus(true));
        Assert.assertEquals(WorkOrderStatusConstants.EvaluateStatus.NOT_OPEN, WorkOrderStatusFlow.afterCloseEvaluateStatus(false));
        Assert.assertEquals(WorkOrderStatusConstants.EvaluateStatus.EVALUATED, WorkOrderStatusFlow.afterEvaluate());
    }
}


