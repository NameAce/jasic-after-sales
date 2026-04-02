package com.jasic.aftersales.system.service.impl;

import com.jasic.aftersales.common.constant.WorkOrderStatusConstants;
import com.jasic.aftersales.common.exception.ServiceException;
import com.jasic.aftersales.system.domain.dto.WorkOrderFaultItemDTO;
import com.jasic.aftersales.system.domain.dto.WorkOrderRepairDTO;
import com.jasic.aftersales.system.domain.entity.WorkOrder;
import com.jasic.aftersales.system.domain.entity.WorkOrderQuote;
import com.jasic.aftersales.system.domain.vo.WorkOrderRepairFaultOptionVO;
import com.jasic.aftersales.system.mapper.WorkOrderMapper;
import com.jasic.aftersales.system.mapper.WorkOrderQuoteMapper;
import com.jasic.aftersales.system.service.IFaultRepairConfigService;
import com.jasic.aftersales.system.service.WorkOrderPermissionService;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
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

    @Test
    public void shouldRequirePartDescWhenFaultItemHasRepairContent() throws Exception {
        WorkOrderServiceImpl service = new WorkOrderServiceImpl();
        setField(service, "faultRepairConfigService", createFaultRepairConfigServiceProxy(Collections.emptyList()));

        WorkOrderFaultItemDTO faultItem = new WorkOrderFaultItemDTO();
        faultItem.setFaultDesc("电源故障");
        faultItem.setRepairDesc("更换电源板");

        try {
            invokeNormalizeRepairFaults(service, buildRepairWorkOrder(), Collections.singletonList(faultItem));
            Assert.fail("预期应拒绝缺少配件信息的故障点");
        } catch (ServiceException ex) {
            Assert.assertEquals("配件信息不能为空", ex.getMessage());
        }
    }

    @Test
    public void shouldRequireOtherDescWhenOtherRepairOptionSelected() throws Exception {
        WorkOrderServiceImpl service = new WorkOrderServiceImpl();
        setField(service, "faultRepairConfigService", createFaultRepairConfigServiceProxy(Collections.singletonList(
                buildRepairFaultOption("电源故障", Collections.singletonList("更换电源板"))
        )));

        WorkOrderFaultItemDTO faultItem = new WorkOrderFaultItemDTO();
        faultItem.setFaultDesc("电源故障");
        faultItem.setPartDesc("电源板");
        faultItem.setRepairItems(Arrays.asList("更换电源板", "其它维修说明"));

        try {
            invokeNormalizeRepairFaults(service, buildRepairWorkOrder(), Collections.singletonList(faultItem));
            Assert.fail("预期应拒绝缺少其他维修说明的维修登记");
        } catch (ServiceException ex) {
            Assert.assertEquals("选择其它维修说明时，其他维修说明不能为空", ex.getMessage());
        }
    }

    @Test
    public void shouldSnapshotRepairItemsWhenRepairConfigMatched() throws Exception {
        WorkOrderServiceImpl service = new WorkOrderServiceImpl();
        setField(service, "faultRepairConfigService", createFaultRepairConfigServiceProxy(Collections.singletonList(
                buildRepairFaultOption("电源故障", Arrays.asList("更换电源板", "清洁接线"))
        )));

        WorkOrderFaultItemDTO faultItem = new WorkOrderFaultItemDTO();
        faultItem.setFaultDesc("电源故障");
        faultItem.setPartDesc("电源板");
        faultItem.setRepairItems(Collections.singletonList("更换电源板"));

        List<WorkOrderFaultItemDTO> result = invokeNormalizeRepairFaults(
                service,
                buildRepairWorkOrder(),
                Collections.singletonList(faultItem)
        );

        Assert.assertEquals(1, result.size());
        Assert.assertEquals("电源故障", result.get(0).getFaultDesc());
        Assert.assertEquals("更换电源板", result.get(0).getRepairDesc());
        Assert.assertEquals(Collections.singletonList("更换电源板"), result.get(0).getRepairItems());
    }

    @Test
    public void shouldRejectManualRepairDescWhenFaultConfigMatched() throws Exception {
        WorkOrderServiceImpl service = new WorkOrderServiceImpl();
        setField(service, "faultRepairConfigService", createFaultRepairConfigServiceProxy(Collections.singletonList(
                buildRepairFaultOption("电源故障", Arrays.asList("更换电源板", "清洁接线"))
        )));

        WorkOrderFaultItemDTO faultItem = new WorkOrderFaultItemDTO();
        faultItem.setFaultDesc("电源故障");
        faultItem.setPartDesc("电源板");
        faultItem.setRepairDesc("手工输入维修说明");

        try {
            invokeNormalizeRepairFaults(service, buildRepairWorkOrder(), Collections.singletonList(faultItem));
            Assert.fail("预期应拒绝绕过配置的手工维修说明");
        } catch (ServiceException ex) {
            Assert.assertEquals("请选择配置内的维修说明", ex.getMessage());
        }
    }

    @Test
    public void shouldRejectFaultDescOutsideConfiguredRange() throws Exception {
        WorkOrderServiceImpl service = new WorkOrderServiceImpl();
        setField(service, "faultRepairConfigService", createFaultRepairConfigServiceProxy(Collections.singletonList(
                buildRepairFaultOption("电源故障", Collections.singletonList("更换电源板"))
        )));

        WorkOrderFaultItemDTO faultItem = new WorkOrderFaultItemDTO();
        faultItem.setFaultDesc("非配置故障");
        faultItem.setPartDesc("电源板");
        faultItem.setRepairDesc("手工输入维修说明");

        try {
            invokeNormalizeRepairFaults(service, buildRepairWorkOrder(), Collections.singletonList(faultItem));
            Assert.fail("预期应拒绝配置范围外的故障描述");
        } catch (ServiceException ex) {
            Assert.assertEquals("故障描述不在当前配置范围内", ex.getMessage());
        }
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

    private IFaultRepairConfigService createFaultRepairConfigServiceProxy(List<WorkOrderRepairFaultOptionVO> options) {
        InvocationHandler handler = new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) {
                if ("listRepairFaultOptions".equals(method.getName())) {
                    return options;
                }
                return defaultValue(method.getReturnType());
            }
        };
        return (IFaultRepairConfigService) Proxy.newProxyInstance(
                IFaultRepairConfigService.class.getClassLoader(),
                new Class<?>[]{IFaultRepairConfigService.class},
                handler
        );
    }

    @SuppressWarnings("unchecked")
    private List<WorkOrderFaultItemDTO> invokeNormalizeRepairFaults(WorkOrderServiceImpl service,
                                                                    WorkOrder workOrder,
                                                                    List<WorkOrderFaultItemDTO> faults) throws Exception {
        Method method = WorkOrderServiceImpl.class
                .getDeclaredMethod("normalizeRepairFaults", WorkOrder.class, List.class);
        method.setAccessible(true);
        try {
            return (List<WorkOrderFaultItemDTO>) method.invoke(service, workOrder, faults);
        } catch (InvocationTargetException ex) {
            if (ex.getCause() instanceof ServiceException) {
                throw (ServiceException) ex.getCause();
            }
            throw ex;
        }
    }

    private WorkOrderRepairFaultOptionVO buildRepairFaultOption(String faultDesc, List<String> repairOptions) {
        WorkOrderRepairFaultOptionVO option = new WorkOrderRepairFaultOptionVO();
        option.setFaultDesc(faultDesc);
        option.setRepairOptions(repairOptions);
        return option;
    }

    private WorkOrder buildRepairWorkOrder() {
        WorkOrder workOrder = new WorkOrder();
        workOrder.setId(5L);
        workOrder.setHqCompanyId(9L);
        workOrder.setProductCode("P-100");
        workOrder.setProductModel("M-200");
        return workOrder;
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
