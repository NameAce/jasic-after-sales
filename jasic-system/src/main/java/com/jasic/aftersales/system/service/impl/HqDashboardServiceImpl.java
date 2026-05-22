package com.jasic.aftersales.system.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.jasic.aftersales.common.constant.WorkOrderStatusConstants;
import com.jasic.aftersales.common.enums.SubjectTypeEnum;
import com.jasic.aftersales.common.exception.ServiceException;
import com.jasic.aftersales.framework.security.SecurityContext;
import com.jasic.aftersales.system.domain.access.WorkOrderAccessContext;
import com.jasic.aftersales.system.domain.query.WorkOrderQuery;
import com.jasic.aftersales.system.domain.query.dashboard.DashboardWorkOrderTrendQuery;
import com.jasic.aftersales.system.domain.vo.WorkOrderStatusCountVO;
import com.jasic.aftersales.system.domain.vo.dashboard.DashboardCountByDayVO;
import com.jasic.aftersales.system.domain.vo.dashboard.HomeMetricVO;
import com.jasic.aftersales.system.domain.vo.dashboard.HomeRouteTargetVO;
import com.jasic.aftersales.system.domain.vo.dashboard.HomeSectionVO;
import com.jasic.aftersales.system.domain.vo.dashboard.HomeTrendSeriesVO;
import com.jasic.aftersales.system.domain.vo.dashboard.HomeTrendVO;
import com.jasic.aftersales.system.domain.vo.dashboard.HqDashboardHomeVO;
import com.jasic.aftersales.system.mapper.DashboardMapper;
import com.jasic.aftersales.system.service.IHqDashboardService;
import com.jasic.aftersales.system.service.IWorkOrderService;
import com.jasic.aftersales.system.service.WorkOrderAccessContextResolver;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 总部主体首页 Service 实现。
 *
 * <p>本轮总部首页只实现“调度看板”：当前总部承接工单池、当前总部作为转出方的已转出指标、
 * 以及近七天工单事件趋势。该实现不再读取待办、消息、网点履约、SLA 或风险指标。</p>
 *
 * <p>工单卡片数字统一调用 IWorkOrderService.countByStatus，
 * 由工单领域服务补齐与列表一致的权限上下文和查询条件，避免首页另写存量 SQL 造成数字与列表 total 不一致。</p>
 *
 * @author Zoro
 * @date 2026/05/21
 */
@Service
public class HqDashboardServiceImpl implements IHqDashboardService {

    /**
     * 首页趋势固定展示最近七天。
     */
    private static final int TREND_DAYS = 7;

    /**
     * 工单列表路由名称。
     *
     * <p>首页接口返回 routeTarget 后，前端只按该路由名跳转，不再在卡片组件中拼筛选口径。</p>
     */
    private static final String WORK_ORDER_ROUTE_NAME = "after-sales_work-order";

    /**
     * 复用工单列表同源统计能力。
     */
    @Resource
    private IWorkOrderService workOrderService;

    /**
     * 复用工单访问上下文解析器。
     *
     * <p>趋势统计虽然不要求与列表 total 对齐，但仍必须按当前主体和数据范围限制事件可见性。</p>
     */
    @Resource
    private WorkOrderAccessContextResolver workOrderAccessContextResolver;

    /**
     * 首页专用 Mapper。
     *
     * <p>这里只用于近七天事件趋势，工单卡片存量统计不走 DashboardMapper。</p>
     */
    @Resource
    private DashboardMapper dashboardMapper;

    /**
     * 查询总部主体首页。
     *
     * <p>核心流程：
     * 1. 校验当前登录主体必须为总部；
     * 2. 复用工单列表统计查询当前承接工单池；
     * 3. 使用 transferDirection=OUT 查询当前总部作为转出方的已转出数量；
     * 4. 按工单流水事件发生时间聚合近七天流入、完成、转出趋势。</p>
     *
     * @return 总部主体首页数据
     */
    @Override
    public HqDashboardHomeVO getHome() {
        validateSubjectType(SubjectTypeEnum.HQ);

        HqDashboardHomeVO home = new HqDashboardHomeVO();
        home.setTitle("调度看板");
        if (!hasWorkOrderListPermission()) {
            home.setWorkOrderPool(buildEmptyCurrentPoolSection("当前总部承接工单池"));
            home.setTransfer(buildTransferSection(0L, "当前总部曾经承接且由当前总部转出的工单"));
            home.setTrend(buildEmptyTrend("近 7 天事件趋势"));
            return home;
        }

        home.setWorkOrderPool(buildCurrentPoolSection("当前总部承接工单池", "当前总部承接中"));
        home.setTransfer(buildTransferSection(countTransferOut(), "当前总部曾经承接且由当前总部转出的工单"));
        home.setTrend(buildHqTrend());
        return home;
    }

    /**
     * 校验当前登录主体类型。
     *
     * @param expected 期望主体类型
     */
    private void validateSubjectType(SubjectTypeEnum expected) {
        String currentSubjectType = SecurityContext.getCurrentSubjectType();
        if (!expected.getCode().equals(currentSubjectType)) {
            throw new ServiceException("当前登录主体不支持访问总部首页");
        }
    }

    /**
     * 构建当前承接工单池分区。
     *
     * <p>该分区每个卡片都使用 viewScope=CURRENT，并按需附加 mainStatus。
     * 这些参数与工单列表接口接收的参数完全一致，点击后列表 total 应与卡片数字对齐。</p>
     *
     * @param sectionTitle 分区标题
     * @param subjectPhrase 当前主体描述
     * @return 当前承接工单池分区
     */
    private HomeSectionVO buildCurrentPoolSection(String sectionTitle, String subjectPhrase) {
        Map<String, Long> countMap = countStatusMap(buildCurrentQuery(null));
        HomeSectionVO section = buildSection(sectionTitle);
        section.getMetrics().add(buildWorkOrderMetric(
                "CURRENT_TOTAL",
                "当前工单总量",
                countMap.get("ALL"),
                subjectPhrase + "的全部工单",
                query("viewScope", "CURRENT")
        ));
        appendStatusMetric(section, countMap, "PENDING_ASSIGN", WorkOrderStatusConstants.MainStatus.PENDING_ASSIGN, subjectPhrase);
        appendStatusMetric(section, countMap, "PENDING_TECH_ACCEPT", WorkOrderStatusConstants.MainStatus.PENDING_TECH_ACCEPT, subjectPhrase);
        appendStatusMetric(section, countMap, "IN_PROGRESS", WorkOrderStatusConstants.MainStatus.IN_PROGRESS, subjectPhrase);
        appendStatusMetric(section, countMap, "COMPLETED", WorkOrderStatusConstants.MainStatus.COMPLETED, subjectPhrase);
        appendStatusMetric(section, countMap, "CLOSED", WorkOrderStatusConstants.MainStatus.CLOSED, subjectPhrase);
        return section;
    }

    /**
     * 构建无权限时的空工单池分区。
     *
     * <p>如果当前人没有工单列表权限，首页不能通过聚合接口泄露任何工单数量。</p>
     *
     * @param sectionTitle 分区标题
     * @return 空工单池分区
     */
    private HomeSectionVO buildEmptyCurrentPoolSection(String sectionTitle) {
        HomeSectionVO section = buildSection(sectionTitle);
        section.getMetrics().add(buildWorkOrderMetric("CURRENT_TOTAL", "当前工单总量", 0L, "无工单列表权限", query("viewScope", "CURRENT")));
        section.getMetrics().add(buildWorkOrderMetric("PENDING_ASSIGN", "待派单", 0L, "无工单列表权限", query("viewScope", "CURRENT", "mainStatus", WorkOrderStatusConstants.MainStatus.PENDING_ASSIGN)));
        section.getMetrics().add(buildWorkOrderMetric("PENDING_TECH_ACCEPT", "待接单", 0L, "无工单列表权限", query("viewScope", "CURRENT", "mainStatus", WorkOrderStatusConstants.MainStatus.PENDING_TECH_ACCEPT)));
        section.getMetrics().add(buildWorkOrderMetric("IN_PROGRESS", "维修中", 0L, "无工单列表权限", query("viewScope", "CURRENT", "mainStatus", WorkOrderStatusConstants.MainStatus.IN_PROGRESS)));
        section.getMetrics().add(buildWorkOrderMetric("COMPLETED", "已完成", 0L, "无工单列表权限", query("viewScope", "CURRENT", "mainStatus", WorkOrderStatusConstants.MainStatus.COMPLETED)));
        section.getMetrics().add(buildWorkOrderMetric("CLOSED", "已关闭", 0L, "无工单列表权限", query("viewScope", "CURRENT", "mainStatus", WorkOrderStatusConstants.MainStatus.CLOSED)));
        return section;
    }

    /**
     * 添加主状态指标。
     *
     * @param section 首页分区
     * @param countMap 状态数量 Map
     * @param code 指标编码
     * @param mainStatus 主状态编码
     * @param subjectPhrase 当前主体描述
     */
    private void appendStatusMetric(HomeSectionVO section, Map<String, Long> countMap, String code,
                                    String mainStatus, String subjectPhrase) {
        String label = WorkOrderStatusConstants.resolveMainStatusLabel(mainStatus);
        section.getMetrics().add(buildWorkOrderMetric(
                code,
                label,
                countMap.get(mainStatus),
                subjectPhrase + "且主状态为“" + label + "”的工单",
                query("viewScope", "CURRENT", "mainStatus", mainStatus)
        ));
    }

    /**
     * 构建已转出分区。
     *
     * <p>已转出只使用 transferDirection=OUT，表示当前登录公司作为转出方。
     * 这里不使用 hasTransfer=1，因为 hasTransfer 只能表达工单发生过转单，不能证明当前主体是转出方。</p>
     *
     * @param count 已转出数量
     * @param statCondition 统计条件说明
     * @return 已转出分区
     */
    private HomeSectionVO buildTransferSection(Long count, String statCondition) {
        HomeSectionVO section = buildSection("已转出");
        section.getMetrics().add(buildWorkOrderMetric(
                "TRANSFER_OUT",
                "已转出",
                count,
                statCondition,
                query("transferDirection", "OUT")
        ));
        return section;
    }

    /**
     * 统计当前主体作为转出方的工单数量。
     *
     * <p>这里复用 WorkOrderService.countByStatus，并传入 transferDirection=OUT。
     * 因此该数字与列表接口使用同一后端条件。</p>
     *
     * @return 已转出数量
     */
    private Long countTransferOut() {
        WorkOrderQuery query = new WorkOrderQuery();
        query.setTransferDirection("OUT");
        return countStatusMap(query).get("ALL");
    }

    /**
     * 构建总部近七天事件趋势。
     *
     * <p>趋势按 work_order_flow.create_time 聚合，不按当前状态存量统计。
     * 因此趋势数据用于观察事件发生节奏，不要求点击后与某个状态列表 total 对齐。</p>
     *
     * @return 总部近七天事件趋势
     */
    private HomeTrendVO buildHqTrend() {
        DashboardWorkOrderTrendQuery query = buildTrendQuery();
        List<String> days = buildRecentDayKeys();
        HomeTrendVO trend = buildEmptyTrend("近 7 天事件趋势");
        trend.setDays(days);
        trend.getSeries().add(buildTrendSeries("FLOW_IN", "流入总部", days, dashboardMapper.selectFlowInTrend(query)));
        trend.getSeries().add(buildTrendSeries("REPAIR_FINISH", "总部完成", days, dashboardMapper.selectRepairFinishTrend(query)));
        trend.getSeries().add(buildTrendSeries("TRANSFER_OUT", "总部转出", days, dashboardMapper.selectTransferOutTrend(query)));
        return trend;
    }

    /**
     * 构建趋势查询参数。
     *
     * @return 趋势查询参数
     */
    private DashboardWorkOrderTrendQuery buildTrendQuery() {
        DashboardWorkOrderTrendQuery query = new DashboardWorkOrderTrendQuery();
        query.setStartTime(resolveTrendStartTime());
        query.setEndTime(resolveTrendEndTime());

        // 趋势 SQL 需要登录主体、公司和数据范围，否则会绕开工单领域权限边界。
        WorkOrderAccessContext accessContext = workOrderAccessContextResolver.resolve();
        query.setAccessContext(accessContext);
        return query;
    }

    /**
     * 构建当前承接工单统计查询。
     *
     * @param mainStatus 主状态编码；为空时统计全部当前承接工单
     * @return 工单查询参数
     */
    private WorkOrderQuery buildCurrentQuery(String mainStatus) {
        WorkOrderQuery query = new WorkOrderQuery();
        query.setViewScope("CURRENT");
        query.setMainStatus(mainStatus);
        return query;
    }

    /**
     * 调用工单领域状态统计并转为 Map。
     *
     * <p>调用工单 Service 而不是 DashboardMapper，是为了让首页卡片与列表接口共享同一套权限和筛选构造。</p>
     *
     * @param query 工单查询参数
     * @return 状态编码到数量的 Map
     */
    private Map<String, Long> countStatusMap(WorkOrderQuery query) {
        List<WorkOrderStatusCountVO> rows = workOrderService.countByStatus(query);
        Map<String, Long> result = new LinkedHashMap<>();
        result.put("ALL", 0L);
        result.put(WorkOrderStatusConstants.MainStatus.PENDING_ASSIGN, 0L);
        result.put(WorkOrderStatusConstants.MainStatus.PENDING_TECH_ACCEPT, 0L);
        result.put(WorkOrderStatusConstants.MainStatus.IN_PROGRESS, 0L);
        result.put(WorkOrderStatusConstants.MainStatus.COMPLETED, 0L);
        result.put(WorkOrderStatusConstants.MainStatus.CLOSED, 0L);
        if (rows == null) {
            return result;
        }
        for (WorkOrderStatusCountVO row : rows) {
            if (row == null || row.getMainStatus() == null) {
                continue;
            }
            result.put(row.getMainStatus(), defaultLong(row.getCountNum()));
        }
        return result;
    }

    /**
     * 创建工单指标卡片。
     *
     * @param code 指标编码
     * @param title 指标标题
     * @param value 指标值
     * @param statCondition 统计条件说明
     * @param query 点击列表查询参数
     * @return 首页指标
     */
    private HomeMetricVO buildWorkOrderMetric(String code, String title, Long value,
                                              String statCondition, Map<String, Object> query) {
        HomeMetricVO metric = new HomeMetricVO();
        metric.setCode(code);
        metric.setTitle(title);
        metric.setValue(defaultLong(value));
        metric.setUnit("单");
        metric.setStatCondition(statCondition);
        metric.setListQueryCondition(buildListQueryCondition(query));
        metric.setRouteTarget(route(WORK_ORDER_ROUTE_NAME, query));
        return metric;
    }

    /**
     * 创建趋势序列。
     *
     * @param code 序列编码
     * @param name 序列名称
     * @param days 最近七天日期
     * @param rows 数据库按天聚合结果
     * @return 趋势序列
     */
    private HomeTrendSeriesVO buildTrendSeries(String code, String name, List<String> days, List<DashboardCountByDayVO> rows) {
        Map<String, Long> countMap = buildDayCountMap(rows);
        HomeTrendSeriesVO series = new HomeTrendSeriesVO();
        series.setCode(code);
        series.setName(name);
        List<Long> values = new ArrayList<>();
        for (String day : days) {
            values.add(countMap.getOrDefault(day, 0L));
        }
        series.setValues(values);
        return series;
    }

    /**
     * 创建空趋势结构。
     *
     * @param title 趋势标题
     * @return 空趋势结构
     */
    private HomeTrendVO buildEmptyTrend(String title) {
        HomeTrendVO trend = new HomeTrendVO();
        trend.setTitle(title);
        trend.setDays(buildRecentDayKeys());
        return trend;
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
     * 创建跳转目标。
     *
     * @param routeName 前端路由名称
     * @param query 路由查询参数
     * @return 跳转目标
     */
    private HomeRouteTargetVO route(String routeName, Map<String, Object> query) {
        HomeRouteTargetVO target = new HomeRouteTargetVO();
        target.setRouteName(routeName);
        target.setQuery(query);
        return target;
    }

    /**
     * 创建有序查询参数。
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
     * 把路由查询参数转成接口说明文本。
     *
     * @param query 路由查询参数
     * @return 查询说明
     */
    private String buildListQueryCondition(Map<String, Object> query) {
        if (query == null || query.isEmpty()) {
            return "工单列表无额外筛选";
        }
        StringBuilder builder = new StringBuilder("工单列表查询参数：");
        boolean first = true;
        for (Map.Entry<String, Object> entry : query.entrySet()) {
            if (!first) {
                builder.append("&");
            }
            builder.append(entry.getKey()).append("=").append(entry.getValue());
            first = false;
        }
        return builder.toString();
    }

    /**
     * 生成最近七天日期键。
     *
     * @return 最近七天日期键
     */
    private List<String> buildRecentDayKeys() {
        List<String> dayKeys = new ArrayList<>();
        LocalDate today = LocalDate.now();
        for (int i = TREND_DAYS - 1; i >= 0; i--) {
            dayKeys.add(today.minusDays(i).toString());
        }
        return dayKeys;
    }

    /**
     * 把按天聚合结果转换成 Map。
     *
     * @param rows 按天聚合结果
     * @return 日期到数量的映射
     */
    private Map<String, Long> buildDayCountMap(List<DashboardCountByDayVO> rows) {
        Map<String, Long> result = new LinkedHashMap<>();
        if (rows == null) {
            return result;
        }
        for (DashboardCountByDayVO row : rows) {
            if (row == null || row.getDayKey() == null) {
                continue;
            }
            result.put(row.getDayKey(), defaultLong(row.getCountNum()));
        }
        return result;
    }

    /**
     * 判断当前人是否具备工单列表权限。
     *
     * @return true 表示可以读取首页工单统计
     */
    private boolean hasWorkOrderListPermission() {
        return StpUtil.hasPermission("workorder:list");
    }

    /**
     * 解析首页趋势开始时间。
     *
     * @return 最近七天开始时间，含边界
     */
    private LocalDateTime resolveTrendStartTime() {
        return LocalDate.now().minusDays(TREND_DAYS - 1L).atStartOfDay();
    }

    /**
     * 解析首页趋势结束时间。
     *
     * @return 明天零点，不含边界
     */
    private LocalDateTime resolveTrendEndTime() {
        return LocalDate.now().plusDays(1L).atStartOfDay();
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
