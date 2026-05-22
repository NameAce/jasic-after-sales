package com.jasic.aftersales.system.domain.dto;

import org.junit.Assert;
import org.junit.Test;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import java.util.Set;

/**
 * 工单请求参数校验测试。
 *
 * @author Zoro
 * @date 2026/04/01
 */
public class WorkOrderRequestValidationTest {

    /**validator 字段，由接口调用方提交并参与服务层业务校验。*/
    private final Validator validator;

    /**构造 WorkOrderRequestValidationTest 实例，初始化当前对象在业务流程中需要持有的基础数据。*/
    public WorkOrderRequestValidationTest() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        this.validator = factory.getValidator();
    }

    /**验证RequireReviewWorkOrderId，保证相关业务规则在回归场景下保持稳定。*/
    @Test
    public void shouldRequireReviewWorkOrderId() {
        WorkOrderReviewDTO dto = new WorkOrderReviewDTO();

        Set<ConstraintViolation<WorkOrderReviewDTO>> violations = validator.validate(dto);

        Assert.assertTrue(violations.stream()
                .anyMatch(item -> "工单ID不能为空".equals(item.getMessage())));
    }

    /**验证RequireTechAcceptFaultJudge，保证相关业务规则在回归场景下保持稳定。*/
    @Test
    public void shouldRequireTechAcceptFaultJudge() {
        WorkOrderTechAcceptDTO dto = new WorkOrderTechAcceptDTO();
        dto.setWorkOrderId(1L);
        dto.setFaultJudge("   ");

        Set<ConstraintViolation<WorkOrderTechAcceptDTO>> violations = validator.validate(dto);

        Assert.assertTrue(violations.stream()
                .anyMatch(item -> "故障判定不能为空".equals(item.getMessage())));
    }

    /**验证RequireRepairWorkOrderId，保证相关业务规则在回归场景下保持稳定。*/
    @Test
    public void shouldRequireRepairWorkOrderId() {
        WorkOrderRepairDTO dto = new WorkOrderRepairDTO();

        Set<ConstraintViolation<WorkOrderRepairDTO>> violations = validator.validate(dto);

        Assert.assertTrue(violations.stream()
                .anyMatch(item -> "工单ID不能为空".equals(item.getMessage())));
    }
}
