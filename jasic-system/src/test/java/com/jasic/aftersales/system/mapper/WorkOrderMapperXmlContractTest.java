package com.jasic.aftersales.system.mapper;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/*** WorkOrder Mapper XML 合同测试。

@author Zoro*/
public class WorkOrderMapperXmlContractTest {

    /**shouldAllowSelfAllScopeToIncludeAssignedAndHistoryParticipation 数据访问操作，为服务层提供数据库查询或写入结果。*/
    @Test
    public void shouldAllowSelfAllScopeToIncludeAssignedAndHistoryParticipation() throws IOException {
        String xml = new String(Files.readAllBytes(resolveMapperPath()), StandardCharsets.UTF_8);
        String normalized = xml.replace("\r\n", "\n");

        Assert.assertTrue(normalized.contains("<sql id=\"CurrentUserAllVisibilityCondition\">"));
        Assert.assertTrue(normalized.contains("w.assigned_user_id = #{query.accessContext.currentUserId}"));
        Assert.assertTrue(normalized.contains("OR <include refid=\"CurrentUserHistoryParticipationCondition\" />"));
        Assert.assertEquals(2, countOccurrences(normalized,
                "<when test=\"query.viewScope == 'ALL'\">\n" +
                        "                        AND <include refid=\"CurrentUserAllVisibilityCondition\" />"));
    }

    /**shouldRestrictSelfScopedVisibilityCondition 数据访问操作，为服务层提供数据库查询或写入结果。*/
    @Test
    public void shouldRestrictSelfScopedVisibilityCondition() throws IOException {
        String xml = new String(Files.readAllBytes(resolveMapperPath()), StandardCharsets.UTF_8);
        String normalized = xml.replace("\r\n", "\n");

        Assert.assertEquals(6, countOccurrences(normalized, "<if test=\"query.accessContext.dataScope == 'SELF'\">"));
        Assert.assertEquals(4, countOccurrences(normalized,
                "AND <include refid=\"CurrentUserHistoryParticipationCondition\" />"));
        Assert.assertEquals(3, countOccurrences(normalized,
                "OR <include refid=\"CurrentUserHistoryParticipationCondition\" />"));
    }

    /**shouldDefineHqSiteSummaryReadonlyScope 数据访问操作，为服务层提供数据库查询或写入结果。*/
    @Test
    public void shouldDefineHqSiteSummaryReadonlyScope() throws IOException {
        String xml = new String(Files.readAllBytes(resolveMapperPath()), StandardCharsets.UTF_8);
        String normalized = xml.replace("\r\n", "\n");

        Assert.assertTrue(normalized.contains("<select id=\"selectHqSiteSummary\""));
        Assert.assertTrue(normalized.contains("<select id=\"selectHqSiteOrderPage\""));
        Assert.assertTrue(normalized.contains("AND w.current_accept_subject_type = 'SERVICE'"));
        Assert.assertTrue(normalized.contains("AND w.current_accept_company_id &lt;&gt; #{query.accessContext.currentCompanyId}"));
        Assert.assertTrue(normalized.contains("w.main_status IN ('PENDING_ASSIGN', 'PENDING_TECH_ACCEPT')"));
        Assert.assertTrue(normalized.contains("SUM(CASE WHEN w.main_status = 'COMPLETED' THEN 1 ELSE 0 END) AS completedCount"));
    }

    /**shouldReturnWarrantyFieldsInWorkOrderListQueries 数据访问操作，为服务层提供数据库查询或写入结果。*/
    @Test
    public void shouldReturnWarrantyFieldsInWorkOrderListQueries() throws IOException {
        String xml = new String(Files.readAllBytes(resolveMapperPath()), StandardCharsets.UTF_8);
        String normalized = xml.replace("\r\n", "\n");

        Assert.assertEquals(3, countOccurrences(normalized, "w.last_out_date AS lastOutDate"));
        Assert.assertEquals(3, countOccurrences(normalized, "w.warranty_status AS warrantyStatus"));
    }

    /**shouldUseTransferDirectionOutAsTransferOutScope 数据访问操作，为服务层提供数据库查询或写入结果。*/
    @Test
    public void shouldUseTransferDirectionOutAsTransferOutScope() throws IOException {
        String xml = new String(Files.readAllBytes(resolveMapperPath()), StandardCharsets.UTF_8);
        String normalized = xml.replace("\r\n", "\n");

        Assert.assertEquals(2, countOccurrences(normalized, "<when test=\"query.transferDirection == 'OUT'\">"));
        Assert.assertEquals(2, countOccurrences(normalized, "AND tf.action_type = 'TRANSFER'"));
        Assert.assertEquals(2, countOccurrences(normalized,
                "AND tf.from_company_id = #{query.accessContext.currentCompanyId}"));
        Assert.assertEquals(2, countOccurrences(normalized,
                "<if test=\"query.transferDirection != 'OUT' and query.accessContext.dataScope == 'SELF' and query.accessContext.currentUserId != null\">"));
    }

    /**shouldSupportMiniProgramSingleTextFuzzySearch 校验小程序单框搜索在列表和统计中保持同源。*/
    @Test
    public void shouldSupportMiniProgramSingleTextFuzzySearch() throws IOException {
        String xml = new String(Files.readAllBytes(resolveMapperPath()), StandardCharsets.UTF_8);
        String normalized = xml.replace("\r\n", "\n");

        Assert.assertTrue(normalized.contains("<sql id=\"WorkOrderKeywordCondition\">"));
        Assert.assertTrue(normalized.contains("<sql id=\"WorkOrderSingleTextOrderNoCondition\">"));
        Assert.assertTrue(normalized.contains("w.customer_mobile LIKE CONCAT('%', #{query.keyword}, '%')"));
        Assert.assertTrue(normalized.contains("w.barcode LIKE CONCAT('%', #{query.keyword}, '%')"));
        Assert.assertTrue(normalized.contains("w.product_model LIKE CONCAT('%', #{query.keyword}, '%')"));
        Assert.assertTrue(normalized.contains("w.customer_mobile LIKE CONCAT('%', #{query.orderNo}, '%')"));
        Assert.assertTrue(normalized.contains("w.barcode LIKE CONCAT('%', #{query.orderNo}, '%')"));
        Assert.assertTrue(normalized.contains("w.product_model LIKE CONCAT('%', #{query.orderNo}, '%')"));
        Assert.assertEquals(2, countOccurrences(normalized, "<include refid=\"WorkOrderSingleTextOrderNoCondition\" />"));
        Assert.assertEquals(2, countOccurrences(normalized, "<include refid=\"WorkOrderKeywordCondition\" />"));
    }

    /**resolveMapperPath 数据访问操作，为服务层提供数据库查询或写入结果。
@return 查询或解析得到的业务对象。*/
    private Path resolveMapperPath() {
        Path direct = Paths.get("jasic-admin", "src", "main", "resources", "mapper", "system", "WorkOrderMapper.xml");
        if (Files.exists(direct)) {
            return direct;
        }
        Path sibling = Paths.get("..", "jasic-admin", "src", "main", "resources", "mapper", "system", "WorkOrderMapper.xml");
        if (Files.exists(sibling)) {
            return sibling.normalize();
        }
        throw new IllegalStateException("未找到 WorkOrderMapper.xml");
    }

    /**countOccurrences 数据访问操作，为服务层提供数据库查询或写入结果。
@param content content 字段参数。
@param needle needle 字段参数。
@return 处理后的业务结果。*/
    private int countOccurrences(String content, String needle) {
        int count = 0;
        int index = 0;
        while ((index = content.indexOf(needle, index)) >= 0) {
            count++;
            index += needle.length();
        }
        return count;
    }
}


