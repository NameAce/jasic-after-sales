package com.jasic.aftersales.customer.domain.dto;

import org.junit.Assert;
import org.junit.Test;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import java.util.Set;

/**
 * C端建单参数校验测试
 *
 * @author Codex
 * @date 2026/04/01
 */
public class CustomerWorkOrderCreateValidationTest {

    private final Validator validator;

    public CustomerWorkOrderCreateValidationTest() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        this.validator = factory.getValidator();
    }

    @Test
    public void shouldNotRequireBarcodeAtBeanValidationLayer() {
        CustomerWorkOrderCreateDTO dto = new CustomerWorkOrderCreateDTO();
        dto.setServiceMode("MAIL");
        dto.setServiceCompanyId(1L);
        dto.setBarcode("   ");
        dto.setBrandType(com.jasic.aftersales.common.enums.BrandTypeEnum.NON_JASIC);

        Set<ConstraintViolation<CustomerWorkOrderCreateDTO>> violations = validator.validate(dto);

        Assert.assertFalse(violations.stream()
                .anyMatch(item -> "机器条码不能为空".equals(item.getMessage())));
    }

    @Test
    public void shouldRequireServiceCompany() {
        CustomerWorkOrderCreateDTO dto = new CustomerWorkOrderCreateDTO();
        dto.setServiceMode("STORE");
        dto.setBarcode("JASIC-001");

        Set<ConstraintViolation<CustomerWorkOrderCreateDTO>> violations = validator.validate(dto);

        Assert.assertTrue(violations.stream()
                .anyMatch(item -> "服务网点不能为空".equals(item.getMessage())));
    }

    @Test
    public void shouldNotRequireCustomerName() {
        CustomerWorkOrderCreateDTO dto = new CustomerWorkOrderCreateDTO();
        dto.setServiceMode("STORE");
        dto.setBarcode("JASIC-001");
        dto.setServiceCompanyId(1L);

        Set<ConstraintViolation<CustomerWorkOrderCreateDTO>> violations = validator.validate(dto);

        Assert.assertFalse(violations.stream()
                .anyMatch(item -> "customerName".equals(item.getPropertyPath().toString())));
    }
}
