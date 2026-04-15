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
 * @author Codex
 * @date 2026/04/01
 */
public class WorkOrderRequestValidationTest {

    private final Validator validator;

    public WorkOrderRequestValidationTest() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        this.validator = factory.getValidator();
    }

    @Test
    public void shouldRequireReviewWorkOrderId() {
        WorkOrderReviewDTO dto = new WorkOrderReviewDTO();

        Set<ConstraintViolation<WorkOrderReviewDTO>> violations = validator.validate(dto);

        Assert.assertTrue(violations.stream()
                .anyMatch(item -> "工单ID不能为空".equals(item.getMessage())));
    }

    @Test
    public void shouldRequireQuoteFaultJudge() {
        WorkOrderQuoteDTO dto = new WorkOrderQuoteDTO();
        dto.setWorkOrderId(1L);
        dto.setFaultJudge("   ");

        Set<ConstraintViolation<WorkOrderQuoteDTO>> violations = validator.validate(dto);

        Assert.assertTrue(violations.stream()
                .anyMatch(item -> "故障判定不能为空".equals(item.getMessage())));
    }

    @Test
    public void shouldRequireTechAcceptFaultJudge() {
        WorkOrderTechAcceptDTO dto = new WorkOrderTechAcceptDTO();
        dto.setWorkOrderId(1L);
        dto.setFaultJudge("   ");

        Set<ConstraintViolation<WorkOrderTechAcceptDTO>> violations = validator.validate(dto);

        Assert.assertTrue(violations.stream()
                .anyMatch(item -> "故障判定不能为空".equals(item.getMessage())));
    }

    @Test
    public void shouldRequireRepairWorkOrderId() {
        WorkOrderRepairDTO dto = new WorkOrderRepairDTO();

        Set<ConstraintViolation<WorkOrderRepairDTO>> violations = validator.validate(dto);

        Assert.assertTrue(violations.stream()
                .anyMatch(item -> "工单ID不能为空".equals(item.getMessage())));
    }
}
