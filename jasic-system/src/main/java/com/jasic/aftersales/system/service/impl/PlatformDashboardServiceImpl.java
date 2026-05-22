package com.jasic.aftersales.system.service.impl;

import com.jasic.aftersales.common.enums.SubjectTypeEnum;
import com.jasic.aftersales.common.exception.ServiceException;
import com.jasic.aftersales.framework.security.SecurityContext;
import com.jasic.aftersales.system.domain.vo.dashboard.DashboardPlatformGovernanceStatsVO;
import com.jasic.aftersales.system.domain.vo.dashboard.HomeMetricVO;
import com.jasic.aftersales.system.domain.vo.dashboard.HomeRouteTargetVO;
import com.jasic.aftersales.system.domain.vo.dashboard.HomeSectionVO;
import com.jasic.aftersales.system.domain.vo.dashboard.PlatformDashboardHomeVO;
import com.jasic.aftersales.system.mapper.DashboardMapper;
import com.jasic.aftersales.system.service.IPlatformDashboardService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 平台主体首页 Service 实现。
 *
 * <p>本轮平台首页只实现“治理看板”，范围限定为组织、账号和基础配置三块。
 * 该实现不会调用工单 Service 或工单 Mapper，也不会读取 CRM 同步、消息治理、操作日志趋势或完整度评分数据，
 * 以保证平台主体首页不再混入售后业务数据。</p>
 *
 * @author Codex
 * @date 2026/05/21
 */
@Service
public class PlatformDashboardServiceImpl implements IPlatformDashboardService {

    /**
     * 首页专用聚合 Mapper。
     *
     * <p>平台治理统计统一走 DashboardMapper.selectPlatformGovernanceStats，
     * 这样可以明确审查平台首页 SQL 不包含工单表。</p>
     */
    @Resource
    private DashboardMapper dashboardMapper;

    /**
     * 查询平台主体首页。
     *
     * <p>核心流程：
     * 1. 校验当前登录主体必须为平台主体；
     * 2. 读取平台治理原始统计；
     * 3. 分别组装组织治理、账号治理、基础配置三个 section；
     * 4. 返回固定标题“治理看板”。</p>
     *
     * @return 平台主体首页数据
     */
    @Override
    public PlatformDashboardHomeVO getHome() {
        validateSubjectType(SubjectTypeEnum.PLATFORM);

        DashboardPlatformGovernanceStatsVO stats = dashboardMapper.selectPlatformGovernanceStats();
        PlatformDashboardHomeVO home = new PlatformDashboardHomeVO();
        home.setTitle("治理看板");
        home.setOrganization(buildOrganizationSection(stats));
        home.setAccount(buildAccountSection(stats));
        home.setBasicConfig(buildBasicConfigSection(stats));
        return home;
    }

    /**
     * 校验当前登录主体类型。
     *
     * <p>平台治理看板只允许平台主体访问；总部和服务网点应分别进入自己的首页接口，
     * 否则不同主体看到的首页语义会混淆。</p>
     *
     * @param expected 期望主体类型
     */
    private void validateSubjectType(SubjectTypeEnum expected) {
        String currentSubjectType = SecurityContext.getCurrentSubjectType();
        if (!expected.getCode().equals(currentSubjectType)) {
            throw new ServiceException("当前登录主体不支持访问平台首页");
        }
    }

    /**
     * 组装组织治理分区。
     *
     * <p>组织治理只统计总部和服务网点，不把客户主体纳入本轮首页。</p>
     *
     * @param stats 平台治理原始统计
     * @return 组织治理分区
     */
    private HomeSectionVO buildOrganizationSection(DashboardPlatformGovernanceStatsVO stats) {
        HomeSectionVO section = buildSection("组织治理");
        section.getMetrics().add(buildMetric(
                "HQ_COUNT",
                "总部数",
                defaultLong(stats == null ? null : stats.getHqCompanyCount()),
                "个",
                "当前系统内主体类型为总部的公司数量",
                "组织列表筛选总部主体",
                route("org", query("activeTab", "company", "subjectType", "HQ"))
        ));
        section.getMetrics().add(buildMetric(
                "SERVICE_COUNT",
                "服务网点数",
                defaultLong(stats == null ? null : stats.getServiceCompanyCount()),
                "个",
                "当前系统内主体类型为服务网点的公司数量，一级和二级服务网点统一纳入",
                "组织列表筛选服务网点主体",
                route("org", query("activeTab", "company", "subjectType", "SERVICE"))
        ));
        section.getMetrics().add(buildMetric(
                "ENABLED_SUBJECT_COUNT",
                "启用主体数",
                defaultLong(stats == null ? null : stats.getEnabledSubjectCount()),
                "个",
                "当前启用状态的总部和服务网点主体数量",
                "组织列表筛选启用主体",
                route("org", query("activeTab", "company", "status", 1))
        ));
        section.getMetrics().add(buildMetric(
                "DISABLED_SUBJECT_COUNT",
                "停用主体数",
                defaultLong(stats == null ? null : stats.getDisabledSubjectCount()),
                "个",
                "当前停用状态的总部和服务网点主体数量",
                "组织列表筛选停用主体",
                route("org", query("activeTab", "company", "status", 0))
        ));
        return section;
    }

    /**
     * 组装账号治理分区。
     *
     * <p>账号治理只展示用户和角色规模，不展示我的事项、个人待办或消息治理数据。</p>
     *
     * @param stats 平台治理原始统计
     * @return 账号治理分区
     */
    private HomeSectionVO buildAccountSection(DashboardPlatformGovernanceStatsVO stats) {
        HomeSectionVO section = buildSection("账号治理");
        section.getMetrics().add(buildMetric(
                "USER_TOTAL",
                "用户总数",
                defaultLong(stats == null ? null : stats.getUserTotal()),
                "个",
                "当前系统未删除的 B 端用户总数",
                "用户列表同口径总数",
                route("system_user", null)
        ));
        section.getMetrics().add(buildMetric(
                "ENABLED_USER_COUNT",
                "启用用户数",
                defaultLong(stats == null ? null : stats.getEnabledUserCount()),
                "个",
                "当前系统未删除且状态为启用的 B 端用户数量",
                "用户列表筛选 status=1",
                route("system_user", query("status", 1))
        ));
        section.getMetrics().add(buildMetric(
                "DISABLED_USER_COUNT",
                "停用用户数",
                defaultLong(stats == null ? null : stats.getDisabledUserCount()),
                "个",
                "当前系统未删除且状态为停用的 B 端用户数量",
                "用户列表筛选 status=0",
                route("system_user", query("status", 0))
        ));
        section.getMetrics().add(buildMetric(
                "ROLE_COUNT",
                "角色数",
                defaultLong(stats == null ? null : stats.getRoleCount()),
                "个",
                "当前系统角色数量",
                "角色列表同口径总数",
                route("system_role", null)
        ));
        return section;
    }

    /**
     * 组装基础配置分区。
     *
     * <p>基础配置只展示规模和稳定入口，不计算完整度、评分或健康度。</p>
     *
     * @param stats 平台治理原始统计
     * @return 基础配置分区
     */
    private HomeSectionVO buildBasicConfigSection(DashboardPlatformGovernanceStatsVO stats) {
        HomeSectionVO section = buildSection("基础配置");
        section.getMetrics().add(buildMetric(
                "PRODUCT_COUNT",
                "产品资料数",
                defaultLong(stats == null ? null : stats.getProductCount()),
                "个",
                "机器条码档案中已沉淀的产品编码去重数量",
                "高级功能中的机器条码档案入口",
                route("advanced-modules", query("module", "barcode"))
        ));
        section.getMetrics().add(buildMetric(
                "SERVICE_TYPE_COUNT",
                "服务类型数",
                defaultLong(stats == null ? null : stats.getServiceTypeCount()),
                "个",
                "字典类型 service_mode 下的服务方式配置项数量",
                "高级功能中的字典管理入口",
                route("advanced-modules", query("module", "dict"))
        ));
        section.getMetrics().add(buildMetric(
                "DICT_ITEM_COUNT",
                "字典配置项数",
                defaultLong(stats == null ? null : stats.getDictItemCount()),
                "个",
                "当前系统字典数据项总数",
                "高级功能中的字典管理入口",
                route("advanced-modules", query("module", "dict"))
        ));
        section.getMetrics().add(buildMetric(
                "REGION_COUNT",
                "区域配置数",
                defaultLong(stats == null ? null : stats.getRegionCount()),
                "个",
                "当前系统大区配置数量",
                "高级功能中的系统大区入口",
                route("advanced-modules", query("module", "region"))
        ));
        return section;
    }

    /**
     * 创建首页分区。
     *
     * @param title 分区标题
     * @return 首页分区
     */
    private HomeSectionVO buildSection(String title) {
        HomeSectionVO section = new HomeSectionVO();
        section.setTitle(title);
        return section;
    }

    /**
     * 创建首页指标。
     *
     * @param code 指标编码
     * @param title 指标标题
     * @param value 指标数值
     * @param unit 指标单位
     * @param statCondition 统计条件说明
     * @param listQueryCondition 列表查询条件说明
     * @param routeTarget 点击跳转目标
     * @return 首页指标
     */
    private HomeMetricVO buildMetric(String code, String title, Long value, String unit,
                                     String statCondition, String listQueryCondition,
                                     HomeRouteTargetVO routeTarget) {
        HomeMetricVO metric = new HomeMetricVO();
        metric.setCode(code);
        metric.setTitle(title);
        metric.setValue(defaultLong(value));
        metric.setUnit(unit);
        metric.setStatCondition(statCondition);
        metric.setListQueryCondition(listQueryCondition);
        metric.setRouteTarget(routeTarget);
        return metric;
    }

    /**
     * 创建跳转目标。
     *
     * @param routeName 前端路由名称
     * @param query 路由查询参数
     * @return 跳转目标
     */
    private HomeRouteTargetVO route(String routeName, Map<String, Object> query) {
        HomeRouteTargetVO target = new HomeRouteTargetVO();
        target.setRouteName(routeName);
        if (query != null) {
            target.setQuery(query);
        }
        return target;
    }

    /**
     * 创建有序查询参数。
     *
     * <p>首页返回 query 时使用有序 Map，便于调试日志和接口验收时稳定观察。</p>
     *
     * @param keyValues 交替传入 key 与 value
     * @return 查询参数 Map
     */
    private Map<String, Object> query(Object... keyValues) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (keyValues == null) {
            return result;
        }
        for (int i = 0; i + 1 < keyValues.length; i += 2) {
            result.put(String.valueOf(keyValues[i]), keyValues[i + 1]);
        }
        return result;
    }

    /**
     * 统一兜底空数量。
     *
     * @param value 原始数量
     * @return 非空数量
     */
    private Long defaultLong(Long value) {
        return value == null ? 0L : value;
    }
}
