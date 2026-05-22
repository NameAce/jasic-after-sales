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
 * @author Zoro
 * @date 2026/04/01
 */
public class CustomerWorkOrderCreateValidationTest {

    /**validator 字段，由接口调用方提交并参与服务层业务校验。*/
    private final Validator validator;

    /**构造 CustomerWorkOrderCreateValidationTest 实例，初始化当前对象在业务流程中需要持有的基础数据。*/
    public CustomerWorkOrderCreateValidationTest() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        this.validator = factory.getValidator();
    }

    /**验证NotRequireBarcodeAtBeanValidationLayer，保证相关业务规则在回归场景下保持稳定。*/
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

    /**验证RequireServiceCompany，保证相关业务规则在回归场景下保持稳定。*/
    @Test
    public void shouldRequireServiceCompany() {
        CustomerWorkOrderCreateDTO dto = new CustomerWorkOrderCreateDTO();
        dto.setServiceMode("STORE");
        dto.setBarcode("JASIC-001");

        Set<ConstraintViolation<CustomerWorkOrderCreateDTO>> violations = validator.validate(dto);

        Assert.assertTrue(violations.stream()
                .anyMatch(item -> "服务网点不能为空".equals(item.getMessage())));
    }

    /**验证NotRequireCustomerName，保证相关业务规则在回归场景下保持稳定。*/
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
