package com.jasic.aftersales.system.mapper;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * WorkOrder Mapper XML 合同测试。
 */
public class WorkOrderMapperXmlContractTest {

    @Test
    public void shouldAllowSelfAllScopeToIncludeAssignedAndHistoryParticipation() throws IOException {
        String xml = new String(Files.readAllBytes(resolveMapperPath()), StandardCharsets.UTF_8);
        String normalized = xml.replace("\r\n", "\n");

        Assert.assertTrue(normalized.contains("<sql id=\"CurrentUserAllVisibilityCondition\">"));
        Assert.assertTrue(normalized.contains("w.assigned_user_id = #{query.currentUserId}"));
        Assert.assertTrue(normalized.contains("OR <include refid=\"CurrentUserHistoryParticipationCondition\" />"));
        Assert.assertEquals(2, countOccurrences(normalized,
                "<when test=\"query.viewScope == 'ALL'\">\n" +
                        "                        AND <include refid=\"CurrentUserAllVisibilityCondition\" />"));
    }

    @Test
    public void shouldRestrictHistoryParticipationConditionToSelfScope() throws IOException {
        String xml = new String(Files.readAllBytes(resolveMapperPath()), StandardCharsets.UTF_8);
        String normalized = xml.replace("\r\n", "\n");

        Assert.assertEquals(4, countOccurrences(normalized, "<if test=\"query.dataScope == 'SELF'\">"));
        Assert.assertEquals(4, countOccurrences(normalized,
                "AND <include refid=\"CurrentUserHistoryParticipationCondition\" />"));
    }

    @Test
    public void shouldDefineHqSiteSummaryReadonlyScope() throws IOException {
        String xml = new String(Files.readAllBytes(resolveMapperPath()), StandardCharsets.UTF_8);
        String normalized = xml.replace("\r\n", "\n");

        Assert.assertTrue(normalized.contains("<select id=\"selectHqSiteSummary\""));
        Assert.assertTrue(normalized.contains("<select id=\"selectHqSiteOrderPage\""));
        Assert.assertTrue(normalized.contains("AND w.current_accept_subject_type = 'SERVICE'"));
        Assert.assertTrue(normalized.contains("AND w.current_accept_company_id &lt;&gt; #{query.companyId}"));
        Assert.assertTrue(normalized.contains("w.main_status IN ('PENDING_ASSIGN', 'PENDING_TECH_ACCEPT')"));
        Assert.assertTrue(normalized.contains("SUM(CASE WHEN w.main_status = 'COMPLETED' THEN 1 ELSE 0 END) AS completedCount"));
    }

    @Test
    public void shouldReturnWarrantyFieldsInWorkOrderListQueries() throws IOException {
        String xml = new String(Files.readAllBytes(resolveMapperPath()), StandardCharsets.UTF_8);
        String normalized = xml.replace("\r\n", "\n");

        Assert.assertEquals(3, countOccurrences(normalized, "w.last_out_date AS lastOutDate"));
        Assert.assertEquals(3, countOccurrences(normalized, "w.warranty_status AS warrantyStatus"));
    }

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
