package com.jasic.aftersales.system.notify.support;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 通知模板初始化 SQL 测试。
 *
 * <p>该测试直接校验阶段一初始化脚本里的场景和默认模板配置，
 * 防止后续调整时把当前基线场景、模板 ID 或字段映射改丢。</p>
 *
 * @author Codex
 * @date 2026/05/20
 */
public class NotifyTemplateSeedSqlTest {

    /**
     * 阶段一 SQL 种子应覆盖当前 7 个通知场景，并包含 B 端评价提醒模板。
     *
     * @throws IOException 读取 SQL 文件异常
     */
    @Test
    public void shouldKeepPhaseOneSeedSqlAlignedWithCurrentBaseline() throws IOException {
        String sql = readPhaseOneSeedSql();

        Assert.assertTrue(sql.contains("'WORK_ORDER_ACCEPT'"));
        Assert.assertTrue(sql.contains("'WORK_ORDER_TRANSFER_IN'"));
        Assert.assertTrue(sql.contains("'WORK_ORDER_ASSIGNED'"));
        Assert.assertTrue(sql.contains("'WORK_ORDER_EVALUATED'"));
        Assert.assertTrue(sql.contains("'WORK_ORDER_ACCEPTED'"));
        Assert.assertTrue(sql.contains("'WORK_ORDER_TRANSFER_NOTICE'"));
        Assert.assertTrue(sql.contains("'WORK_ORDER_EVALUATION_INVITE'"));

        Assert.assertTrue(sql.contains("\"channelScene\":\"B\""));
        Assert.assertTrue(sql.contains("\"channelScene\":\"C\""));
        Assert.assertTrue(sql.contains("'WORK_ORDER_DETAIL'"));
        Assert.assertTrue(sql.contains("'WORK_ORDER_EVALUATE'"));
        Assert.assertTrue(sql.contains("'${workOrderId}'"));

        Assert.assertTrue(sql.contains("aW97dc0OyW40-vGbO9ekIT9DFfyS6JvR9UhPkPuaW_Q"));
        Assert.assertTrue(sql.contains("thing11\",\"value\":\"${assignedUserName}\""));
        Assert.assertTrue(sql.contains("thing9\",\"value\":\"${customerName}\""));
        Assert.assertTrue(sql.contains("phone_number10\",\"value\":\"${customerMobile}\""));
        Assert.assertTrue(sql.contains("character_string8\",\"value\":\"${orderNo}\""));
    }

    /**
     * 读取阶段一初始化 SQL。
     *
     * @return SQL 全文
     * @throws IOException 文件读取异常
     */
    private String readPhaseOneSeedSql() throws IOException {
        Path sqlPath = Paths.get("..", "sql", "upgrade-20260515-notify-template-config-phase1.sql").normalize();
        Assert.assertTrue("阶段一初始化 SQL 文件不存在: " + sqlPath.toAbsolutePath(), Files.exists(sqlPath));
        return new String(Files.readAllBytes(sqlPath), StandardCharsets.UTF_8);
    }
}
