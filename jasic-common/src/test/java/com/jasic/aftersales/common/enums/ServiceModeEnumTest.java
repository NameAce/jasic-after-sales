package com.jasic.aftersales.common.enums;

import org.junit.Assert;
import org.junit.Test;

/**
 * 服务方式枚举测试
 *
 * @author Codex
 * @date 2026/04/08
 */
public class ServiceModeEnumTest {

    @Test
    public void shouldResolveServiceModeByCode() {
        Assert.assertEquals(ServiceModeEnum.MAIL, ServiceModeEnum.getByCode("MAIL"));
        Assert.assertEquals(ServiceModeEnum.STORE, ServiceModeEnum.getByCode("STORE"));
        Assert.assertNull(ServiceModeEnum.getByCode("寄修"));
    }

    @Test
    public void shouldResolveServiceModeLabel() {
        Assert.assertEquals("寄修", ServiceModeEnum.resolveLabel("MAIL"));
        Assert.assertEquals("到店维修", ServiceModeEnum.resolveLabel("STORE"));
        Assert.assertEquals("UNKNOWN", ServiceModeEnum.resolveLabel("UNKNOWN"));
    }
}
