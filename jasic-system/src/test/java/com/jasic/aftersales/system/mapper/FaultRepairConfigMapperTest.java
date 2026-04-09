package com.jasic.aftersales.system.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import org.junit.Assert;
import org.junit.Test;

/**
 * 故障与维修配置 Mapper 约束测试。
 *
 * @author Codex
 * @date 2026/04/08
 */
public class FaultRepairConfigMapperTest {

    @Test
    public void shouldIgnoreTenantLineForFaultRepairConfig() {
        InterceptorIgnore annotation = FaultRepairConfigMapper.class.getAnnotation(InterceptorIgnore.class);

        Assert.assertNotNull(annotation);
        Assert.assertEquals("true", annotation.tenantLine());
    }
}
