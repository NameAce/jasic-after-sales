package com.jasic.aftersales.system.service;

import com.jasic.aftersales.common.enums.SysFileBizTypeEnum;
import com.jasic.aftersales.common.enums.WorkOrderActionEnum;
import com.jasic.aftersales.common.exception.ServiceException;
import com.jasic.aftersales.system.domain.entity.SysFileBiz;
import com.jasic.aftersales.system.domain.entity.WorkOrder;
import com.jasic.aftersales.system.domain.entity.WorkOrderRepair;
import com.jasic.aftersales.system.mapper.SysFileBizMapper;
import com.jasic.aftersales.system.mapper.WorkOrderMapper;
import com.jasic.aftersales.system.mapper.WorkOrderRepairMapper;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**SysFileBizPermissionServiceTest 测试类，用于验证对应业务规则、边界条件和回归场景。

@author Zoro*/
public class SysFileBizPermissionServiceTest {

    /**service 字段，用于当前类内部业务处理。*/
    private SysFileBizPermissionService service;
    /**permissionService 字段，用于当前类内部业务处理。*/
    private StubWorkOrderPermissionService permissionService;

    /**setUp 处理逻辑，服务于当前类的业务编排和数据转换。*/
    @Before
    public void setUp() throws Exception {
        service = new SysFileBizPermissionService();
        permissionService = new StubWorkOrderPermissionService();
        setField(service, "workOrderMapper", mapperProxy(WorkOrderMapper.class, buildWorkOrder(1L), null));
        setField(service, "workOrderRepairMapper", mapperProxy(WorkOrderRepairMapper.class, null, null));
        setField(service, "sysFileBizMapper", mapperProxy(SysFileBizMapper.class, null, null));
        setField(service, "workOrderPermissionService", permissionService);
    }

    /**验证RejectViewWhenWorkOrderPermissionDenied，保证相关业务规则在回归场景下保持稳定。*/
    @Test
    public void shouldRejectViewWhenWorkOrderPermissionDenied() {
        permissionService.allowView = false;

        assertServiceException(new ThrowingRunnable() {
            /**run 处理逻辑，服务于当前类的业务编排和数据转换。*/
            @Override
            public void run() {
                service.requireView(SysFileBizTypeEnum.WORK_ORDER_FAULT_IMAGE, 1L);
            }
        });
    }

    /**验证RejectGenericWriteForFaultFiles，保证相关业务规则在回归场景下保持稳定。*/
    @Test
    public void shouldRejectGenericWriteForFaultFiles() {
        permissionService.allowExecute = true;

        assertServiceException(new ThrowingRunnable() {
            /**run 处理逻辑，服务于当前类的业务编排和数据转换。*/
            @Override
            public void run() {
                service.requireExecute(SysFileBizTypeEnum.WORK_ORDER_FAULT_IMAGE, 1L);
            }
        });
    }

    /**验证UseSendExpressActionForSenderVoucherWrite，保证相关业务规则在回归场景下保持稳定。*/
    @Test
    public void shouldUseSendExpressActionForSenderVoucherWrite() {
        permissionService.allowExecute = false;

        assertServiceException(new ThrowingRunnable() {
            /**run 处理逻辑，服务于当前类的业务编排和数据转换。*/
            @Override
            public void run() {
                service.requireExecute(SysFileBizTypeEnum.WORK_ORDER_SENDER_VOUCHER, 1L);
            }
        });

        Assert.assertEquals(WorkOrderActionEnum.UPLOAD_SEND_EXPRESS, permissionService.lastAction);
    }

    /**验证UseReviewActionForRecheckRepairFiles，保证相关业务规则在回归场景下保持稳定。*/
    @Test
    public void shouldUseReviewActionForRecheckRepairFiles() throws Exception {
        permissionService.allowExecute = true;
        setField(service, "workOrderRepairMapper", mapperProxy(WorkOrderRepairMapper.class, buildRepair(9L, 1L, "RECHECK"), null));

        service.requireExecute(SysFileBizTypeEnum.WORK_ORDER_REPAIR_NEW_IMAGE, 9L);

        Assert.assertEquals(WorkOrderActionEnum.REVIEW, permissionService.lastAction);
    }

    /**验证RejectPreviewWhenFileIsNotBoundToBiz，保证相关业务规则在回归场景下保持稳定。*/
    @Test
    public void shouldRejectPreviewWhenFileIsNotBoundToBiz() {
        assertServiceException(new ThrowingRunnable() {
            /**run 处理逻辑，服务于当前类的业务编排和数据转换。*/
            @Override
            public void run() {
                service.requireFileBoundToBiz(99L, SysFileBizTypeEnum.WORK_ORDER_FAULT_IMAGE, 1L);
            }
        });
    }

    /**验证AllowPreviewWhenFileIsBoundToBiz，保证相关业务规则在回归场景下保持稳定。*/
    @Test
    public void shouldAllowPreviewWhenFileIsBoundToBiz() throws Exception {
        setField(service, "sysFileBizMapper", mapperProxy(SysFileBizMapper.class, null, new SysFileBiz()));

        service.requireFileBoundToBiz(99L, SysFileBizTypeEnum.WORK_ORDER_FAULT_IMAGE, 1L);
    }

    /**buildWorkOrder 业务数据，统一收口字段清洗、默认值处理和返回对象组装规则。
@param id 主键ID。
@return 处理后的业务结果。*/
    private WorkOrder buildWorkOrder(Long id) {
        WorkOrder workOrder = new WorkOrder();
        workOrder.setId(id);
        return workOrder;
    }

    /**buildRepair 业务数据，统一收口字段清洗、默认值处理和返回对象组装规则。
@param id 主键ID。
@param workOrderId 工单ID。
@param registerStage registerStage 字段参数。
@return 处理后的业务结果。*/
    private WorkOrderRepair buildRepair(Long id, Long workOrderId, String registerStage) {
        WorkOrderRepair repair = new WorkOrderRepair();
        repair.setId(id);
        repair.setWorkOrderId(workOrderId);
        repair.setRegisterStage(registerStage);
        return repair;
    }

    /**mapperProxy 处理逻辑，服务于当前类的业务编排和数据转换。
@param type type 字段参数。
@param selectByIdResult selectByIdResult 字段参数。
@param selectOneResult selectOneResult 字段参数。
@return 处理后的业务结果。*/
    private <T> T mapperProxy(Class<T> type, Object selectByIdResult, Object selectOneResult) {
        InvocationHandler handler = new InvocationHandler() {
            /**invoke 处理逻辑，服务于当前类的业务编排和数据转换。
@param proxy proxy 字段参数。
@param method method 字段参数。
@param args args 字段参数。
@return 处理后的业务结果。*/
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) {
                if ("selectById".equals(method.getName())) {
                    return selectByIdResult;
                }
                if ("selectOne".equals(method.getName())) {
                    return selectOneResult;
                }
                return defaultValue(method.getReturnType());
            }
        };
        Object proxy = Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler);
        return type.cast(proxy);
    }

    /**setField 处理逻辑，服务于当前类的业务编排和数据转换。
@param target target 字段参数。
@param fieldName 名称文本，用于展示、匹配或保存业务对象名称。
@param value value 字段参数。*/
    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = SysFileBizPermissionService.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    /**defaultValue 处理逻辑，服务于当前类的业务编排和数据转换。
@param returnType returnType 字段参数。
@return 处理后的业务结果。*/
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
        return 0;
    }

    /**assertServiceException 处理逻辑，服务于当前类的业务编排和数据转换。
@param runnable runnable 字段参数。*/
    private void assertServiceException(ThrowingRunnable runnable) {
        try {
            runnable.run();
            Assert.fail("Expected ServiceException");
        } catch (ServiceException expected) {
            // expected
        }
    }

    /**ThrowingRunnable 测试类，用于验证对应业务规则、边界条件和回归场景。

@author Zoro*/
    private interface ThrowingRunnable {
        /**run 处理逻辑，服务于当前类的业务编排和数据转换。*/
        void run();
    }

    /**StubWorkOrderPermissionService 测试类，用于验证对应业务规则、边界条件和回归场景。

@author Zoro*/
    private static class StubWorkOrderPermissionService extends WorkOrderPermissionService {
        /**allowView 字段，用于当前类内部业务处理。*/
        private boolean allowView = true;
        /**allowExecute 字段，用于当前类内部业务处理。*/
        private boolean allowExecute;
        /**lastAction 字段，用于当前类内部业务处理。*/
        private WorkOrderActionEnum lastAction;

        /**canView 业务条件，用于决定后续流程是否允许继续执行。
@param workOrder workOrder 字段参数。
@return true 表示满足业务条件，false 表示不满足。*/
        @Override
        public boolean canView(WorkOrder workOrder) {
            return allowView;
        }

        /**canExecute 业务条件，用于决定后续流程是否允许继续执行。
@param workOrder workOrder 字段参数。
@param action action 字段参数。
@return true 表示满足业务条件，false 表示不满足。*/
        @Override
        public boolean canExecute(WorkOrder workOrder, WorkOrderActionEnum action) {
            lastAction = action;
            return allowExecute;
        }
    }
}


