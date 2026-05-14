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

public class SysFileBizPermissionServiceTest {

    private SysFileBizPermissionService service;
    private StubWorkOrderPermissionService permissionService;

    @Before
    public void setUp() throws Exception {
        service = new SysFileBizPermissionService();
        permissionService = new StubWorkOrderPermissionService();
        setField(service, "workOrderMapper", mapperProxy(WorkOrderMapper.class, buildWorkOrder(1L), null));
        setField(service, "workOrderRepairMapper", mapperProxy(WorkOrderRepairMapper.class, null, null));
        setField(service, "sysFileBizMapper", mapperProxy(SysFileBizMapper.class, null, null));
        setField(service, "workOrderPermissionService", permissionService);
    }

    @Test
    public void shouldRejectViewWhenWorkOrderPermissionDenied() {
        permissionService.allowView = false;

        assertServiceException(new ThrowingRunnable() {
            @Override
            public void run() {
                service.requireView(SysFileBizTypeEnum.WORK_ORDER_FAULT_IMAGE, 1L);
            }
        });
    }

    @Test
    public void shouldRejectGenericWriteForFaultFiles() {
        permissionService.allowExecute = true;

        assertServiceException(new ThrowingRunnable() {
            @Override
            public void run() {
                service.requireExecute(SysFileBizTypeEnum.WORK_ORDER_FAULT_IMAGE, 1L);
            }
        });
    }

    @Test
    public void shouldUseSendExpressActionForSenderVoucherWrite() {
        permissionService.allowExecute = false;

        assertServiceException(new ThrowingRunnable() {
            @Override
            public void run() {
                service.requireExecute(SysFileBizTypeEnum.WORK_ORDER_SENDER_VOUCHER, 1L);
            }
        });

        Assert.assertEquals(WorkOrderActionEnum.UPLOAD_SEND_EXPRESS, permissionService.lastAction);
    }

    @Test
    public void shouldUseReviewActionForRecheckRepairFiles() throws Exception {
        permissionService.allowExecute = true;
        setField(service, "workOrderRepairMapper", mapperProxy(WorkOrderRepairMapper.class, buildRepair(9L, 1L, "RECHECK"), null));

        service.requireExecute(SysFileBizTypeEnum.WORK_ORDER_REPAIR_NEW_IMAGE, 9L);

        Assert.assertEquals(WorkOrderActionEnum.REVIEW, permissionService.lastAction);
    }

    @Test
    public void shouldRejectPreviewWhenFileIsNotBoundToBiz() {
        assertServiceException(new ThrowingRunnable() {
            @Override
            public void run() {
                service.requireFileBoundToBiz(99L, SysFileBizTypeEnum.WORK_ORDER_FAULT_IMAGE, 1L);
            }
        });
    }

    @Test
    public void shouldAllowPreviewWhenFileIsBoundToBiz() throws Exception {
        setField(service, "sysFileBizMapper", mapperProxy(SysFileBizMapper.class, null, new SysFileBiz()));

        service.requireFileBoundToBiz(99L, SysFileBizTypeEnum.WORK_ORDER_FAULT_IMAGE, 1L);
    }

    private WorkOrder buildWorkOrder(Long id) {
        WorkOrder workOrder = new WorkOrder();
        workOrder.setId(id);
        return workOrder;
    }

    private WorkOrderRepair buildRepair(Long id, Long workOrderId, String registerStage) {
        WorkOrderRepair repair = new WorkOrderRepair();
        repair.setId(id);
        repair.setWorkOrderId(workOrderId);
        repair.setRegisterStage(registerStage);
        return repair;
    }

    private <T> T mapperProxy(Class<T> type, Object selectByIdResult, Object selectOneResult) {
        InvocationHandler handler = new InvocationHandler() {
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

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = SysFileBizPermissionService.class.getDeclaredField(fieldName);
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
        return 0;
    }

    private void assertServiceException(ThrowingRunnable runnable) {
        try {
            runnable.run();
            Assert.fail("Expected ServiceException");
        } catch (ServiceException expected) {
            // expected
        }
    }

    private interface ThrowingRunnable {
        void run();
    }

    private static class StubWorkOrderPermissionService extends WorkOrderPermissionService {
        private boolean allowView = true;
        private boolean allowExecute;
        private WorkOrderActionEnum lastAction;

        @Override
        public boolean canView(WorkOrder workOrder) {
            return allowView;
        }

        @Override
        public boolean canExecute(WorkOrder workOrder, WorkOrderActionEnum action) {
            lastAction = action;
            return allowExecute;
        }
    }
}
