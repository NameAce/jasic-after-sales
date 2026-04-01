package com.jasic.aftersales.system.service.impl;

import com.jasic.aftersales.common.constant.WorkOrderStatusConstants;
import com.jasic.aftersales.common.exception.ServiceException;
import com.jasic.aftersales.system.domain.dto.WorkOrderRepairDTO;
import com.jasic.aftersales.system.domain.entity.WorkOrder;
import com.jasic.aftersales.system.domain.entity.WorkOrderQuote;
import com.jasic.aftersales.system.mapper.WorkOrderMapper;
import com.jasic.aftersales.system.mapper.WorkOrderQuoteMapper;
import com.jasic.aftersales.system.service.WorkOrderPermissionService;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.List;

/**
 * 工单业务服务测试。
 *
 * @author Codex
 * @date 2026/04/01
 */
public class WorkOrderServiceImplTest {

    @Test
    public void shouldRejectTransferTargetQueryWhenTransferNotAllowed() throws Exception {
        WorkOrder workOrder = new WorkOrder();
        workOrder.setId(1L);
        workOrder.setMainStatus(WorkOrderStatusConstants.MainStatus.IN_PROGRESS);
        workOrder.setCurrentAcceptCompanyId(2L);

        WorkOrderServiceImpl service = new WorkOrderServiceImpl();
        setField(service, "workOrderMapper", createWorkOrderMapperProxy(workOrder));
        setField(service, "workOrderPermissionService", new WorkOrderPermissionService() {
            @Override
            public boolean canView(WorkOrder target) {
                return true;
            }

            @Override
            public boolean canTransfer(WorkOrder target) {
                return false;
            }
        });

        try {
            service.listTransferTargetOptions(workOrder.getId());
            Assert.fail("预期应拒绝查询转单目标");
        } catch (ServiceException ex) {
            Assert.assertEquals("当前工单不允许转单", ex.getMessage());
        }
    }

    @Test
    public void shouldRejectEmptyRepairSubmission() throws Exception {
        WorkOrder workOrder = new WorkOrder();
        workOrder.setId(2L);
        workOrder.setMainStatus(WorkOrderStatusConstants.MainStatus.IN_PROGRESS);
        workOrder.setCurrentAcceptCompanyId(3L);

        WorkOrderServiceImpl service = new WorkOrderServiceImpl();
        setField(service, "workOrderMapper", createWorkOrderMapperProxy(workOrder));
        setField(service, "workOrderPermissionService", new WorkOrderPermissionService() {
            @Override
            public boolean canSaveRepair(WorkOrder target) {
                return true;
            }
        });

        WorkOrderRepairDTO dto = new WorkOrderRepairDTO();
        dto.setWorkOrderId(workOrder.getId());
        dto.setFaults(Collections.emptyList());

        try {
            service.saveRepair(dto);
            Assert.fail("预期应拒绝空维修登记");
        } catch (ServiceException ex) {
            Assert.assertEquals("请至少填写一项维修内容", ex.getMessage());
        }
    }

    @Test
    public void shouldDeriveContinueRepairFromReviewResult() throws Exception {
        WorkOrderServiceImpl service = new WorkOrderServiceImpl();
        Method normalizeReviewResult = WorkOrderServiceImpl.class
                .getDeclaredMethod("normalizeReviewResult", String.class);
        normalizeReviewResult.setAccessible(true);
        Method resolveContinueRepair = WorkOrderServiceImpl.class
                .getDeclaredMethod("resolveContinueRepair", String.class);
        resolveContinueRepair.setAccessible(true);

        String reviewResult = (String) normalizeReviewResult.invoke(service, "继续维修");
        Integer continueRepair = (Integer) resolveContinueRepair.invoke(service, reviewResult);

        Assert.assertEquals("继续维修", reviewResult);
        Assert.assertEquals(Integer.valueOf(1), continueRepair);
    }

    @Test
    public void shouldSkipEvaluationInviteWhenCurrentQuoteIsNoFault() throws Exception {
        WorkOrderQuote currentQuote = new WorkOrderQuote();
        currentQuote.setWorkOrderId(4L);
        currentQuote.setFaultJudge("无故障");
        currentQuote.setIsCurrentValid(1);

        WorkOrderServiceImpl service = new WorkOrderServiceImpl();
        setField(service, "workOrderQuoteMapper", createQuoteMapperProxy(Collections.singletonList(currentQuote)));
        Method isNoFaultWorkOrder = WorkOrderServiceImpl.class
                .getDeclaredMethod("isNoFaultWorkOrder", Long.class);
        isNoFaultWorkOrder.setAccessible(true);

        Boolean noFault = (Boolean) isNoFaultWorkOrder.invoke(service, 4L);

        Assert.assertTrue(noFault);
    }

    private WorkOrderMapper createWorkOrderMapperProxy(WorkOrder workOrder) {
        InvocationHandler handler = new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) {
                if ("selectById".equals(method.getName())) {
                    return workOrder;
                }
                return defaultValue(method.getReturnType());
            }
        };
        return (WorkOrderMapper) Proxy.newProxyInstance(
                WorkOrderMapper.class.getClassLoader(),
                new Class<?>[]{WorkOrderMapper.class},
                handler
        );
    }

    private WorkOrderQuoteMapper createQuoteMapperProxy(List<WorkOrderQuote> quotes) {
        InvocationHandler handler = new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) {
                if ("selectList".equals(method.getName())) {
                    return quotes;
                }
                if ("update".equals(method.getName())) {
                    return 1;
                }
                return defaultValue(method.getReturnType());
            }
        };
        return (WorkOrderQuoteMapper) Proxy.newProxyInstance(
                WorkOrderQuoteMapper.class.getClassLoader(),
                new Class<?>[]{WorkOrderQuoteMapper.class},
                handler
        );
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = WorkOrderServiceImpl.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private Object defaultValue(Class<?> returnType) {
        if (!returnType.isPrimitive()) {
            return null;
        }
        if (boolean.class.equals(returnType)) {
            return false;
        }
        if (char.class.equals(returnType)) {
            return '\0';
        }
        if (byte.class.equals(returnType) || short.class.equals(returnType)
                || int.class.equals(returnType) || long.class.equals(returnType)) {
            return 0;
        }
        if (float.class.equals(returnType)) {
            return 0F;
        }
        if (double.class.equals(returnType)) {
            return 0D;
        }
        return null;
    }
}
