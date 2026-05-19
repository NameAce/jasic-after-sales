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
 * <p>阶段三除了校验注册表默认值，还需要锁定初始化脚本的后台默认配置。
 * 该测试直接读取阶段一 SQL 种子，防止后续调整时把 `channelScene`、路由配置、
 * 固定文案或取消场景误改回旧口径。</p>
 *
 * @author Codex
 * @date 2026/05/18
 */
public class NotifyTemplateSeedSqlTest {

    /**
     * 校验阶段一 SQL 种子与当前 6 个保留场景口径一致，并且不会误接回取消的 B 端评价提醒。
     *
     * @throws IOException 读取 SQL 文件异常
     */
    @Test
    public void shouldKeepPhaseOneSeedSqlAlignedWithStageThreeBaseline() throws IOException {
        String sql = readPhaseOneSeedSql();

        Assert.assertTrue(sql.contains("'WORK_ORDER_ACCEPT'"));
        Assert.assertTrue(sql.contains("'WORK_ORDER_TRANSFER_IN'"));
        Assert.assertTrue(sql.contains("'WORK_ORDER_ASSIGNED'"));
        Assert.assertTrue(sql.contains("'WORK_ORDER_ACCEPTED'"));
        Assert.assertTrue(sql.contains("'WORK_ORDER_TRANSFER_NOTICE'"));
        Assert.assertTrue(sql.contains("'WORK_ORDER_EVALUATION_INVITE'"));

        Assert.assertTrue(sql.contains("\"channelScene\":\"B\""));
        Assert.assertTrue(sql.contains("\"channelScene\":\"C\""));
        Assert.assertTrue(sql.contains("'WORK_ORDER_DETAIL'"));
        Assert.assertTrue(sql.contains("'WORK_ORDER_EVALUATE'"));
        Assert.assertTrue(sql.contains("'${workOrderId}'"));

        Assert.assertTrue(sql.contains("新工单 ${orderNo} 已进入当前网点待派单池，请及时派单处理"));
        Assert.assertTrue(sql.contains("工单 ${orderNo} 已转入当前网点，请继续跟进处理"));
        Assert.assertTrue(sql.contains("工单 ${orderNo} 已派给您，请及时联系客户并处理"));
        Assert.assertTrue(sql.contains("您的工单 ${orderNo} 已有工程师接单，当前网点将继续为您处理"));
        Assert.assertTrue(sql.contains("您的工单 ${orderNo} 已转由其他网点继续处理，请留意后续联系。"));
        Assert.assertTrue(sql.contains("您的维修工单 ${orderNo} 已完成，欢迎对本次服务进行评价"));

        Assert.assertTrue(sql.contains("转出网点名称"));
        Assert.assertTrue(sql.contains("转入后的当前处理网点名称"));
        Assert.assertTrue(sql.contains("客户姓名 -> 客户手机号 -> 客户"));

        Assert.assertFalse("阶段三不应重新接回取消的 B 端评价提醒模板",
                sql.contains("aW97dc0OyW40-vGbO9ekIT9DFfyS6JvR9UhPkPuaW_Q"));
        Assert.assertFalse("阶段三不应把 B 端评价提醒场景写回初始化 SQL",
                sql.contains("B端评价提醒"));
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
