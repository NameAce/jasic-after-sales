package com.jasic.aftersales.system.domain.dto;

import org.junit.Assert;
import org.junit.Test;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import java.util.Collections;
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
    public void shouldRequireReviewResult() {
        WorkOrderReviewDTO dto = new WorkOrderReviewDTO();
        dto.setWorkOrderId(1L);
        dto.setReviewResult("   ");

        Set<ConstraintViolation<WorkOrderReviewDTO>> violations = validator.validate(dto);

        Assert.assertTrue(violations.stream()
                .anyMatch(item -> "复检结果不能为空".equals(item.getMessage())));
    }

    @Test
    public void shouldCascadeValidateFaultItems() {
        WorkOrderFaultItemDTO faultItem = new WorkOrderFaultItemDTO();
        faultItem.setFaultDesc("   ");

        WorkOrderRepairDTO dto = new WorkOrderRepairDTO();
        dto.setWorkOrderId(1L);
        dto.setFaults(Collections.singletonList(faultItem));

        Set<ConstraintViolation<WorkOrderRepairDTO>> violations = validator.validate(dto);

        Assert.assertTrue(violations.stream()
                .anyMatch(item -> "故障描述不能为空".equals(item.getMessage())));
    }
}
