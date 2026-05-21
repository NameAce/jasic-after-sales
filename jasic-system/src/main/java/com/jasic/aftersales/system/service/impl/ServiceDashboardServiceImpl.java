package com.jasic.aftersales.system.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.jasic.aftersales.common.core.domain.PageResult;
import com.jasic.aftersales.common.enums.SubjectTypeEnum;
import com.jasic.aftersales.common.exception.ServiceException;
import com.jasic.aftersales.framework.security.SecurityContext;
import com.jasic.aftersales.system.domain.access.WorkOrderAccessContext;
import com.jasic.aftersales.system.domain.query.WorkOrderQuery;
import com.jasic.aftersales.system.domain.query.dashboard.DashboardTodoQuery;
import com.jasic.aftersales.system.domain.query.dashboard.DashboardWorkOrderTrendQuery;
import com.jasic.aftersales.system.domain.vo.WorkOrderStatusCountVO;
import com.jasic.aftersales.system.domain.vo.dashboard.DashboardCountByDayVO;
import com.jasic.aftersales.system.domain.vo.dashboard.DashboardHistoryTodoVO;
import com.jasic.aftersales.system.domain.vo.dashboard.DashboardTodoStatsVO;
import com.jasic.aftersales.system.domain.vo.dashboard.DashboardTrend7dVO;
import com.jasic.aftersales.system.domain.vo.dashboard.DashboardWorkOrderStatusVO;
import com.jasic.aftersales.system.domain.vo.dashboard.ServiceDashboardHomeVO;
import com.jasic.aftersales.system.domain.vo.dashboard.ServiceDashboardOverviewVO;
import com.jasic.aftersales.system.mapper.DashboardMapper;
import com.jasic.aftersales.system.notify.domain.query.NotifyMessageQuery;
import com.jasic.aftersales.system.notify.domain.vo.NotifyMessagePageVO;
import com.jasic.aftersales.system.notify.service.NotifyMessageService;
import com.jasic.aftersales.system.service.IServiceDashboardService;
import com.jasic.aftersales.system.service.IWorkOrderService;
import com.jasic.aftersales.system.service.WorkOrderAccessContextResolver;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 服务主体首页 Service 实现。
 *
 * <p>该实现负责把当前登录服务主体首页所需的待办概览、工单概览、
 * 工单状态分布、近七天趋势和最新动态统一收敛为专用返回结构。
 * 其中工单状态统计复用既有 `status-count` 能力，趋势统计走首页专用聚合 SQL。</p>
 *
 * @author Codex
 * @date 2026/05/20
 */
@Service
public class ServiceDashboardServiceImpl implements IServiceDashboardService {

    /**
     * 首页最新动态最多返回 10 条，避免首页列表过长影响加载与阅读。
     */
    private static final int LATEST_HISTORY_TODO_LIMIT = 10;

    /**
     * 首页趋势固定展示最近 7 天。
     */
    private static final int TREND_DAYS = 7;

    /**
     * 复用现有工单聚合能力，保证首页统计口径与列表页权限模型一致。
     */
    @Resource
    private IWorkOrderService workOrderService;

    /**
     * 复用现有工单访问上下文解析器，避免首页自行绕开既有数据权限。
     */
    @Resource
    private WorkOrderAccessContextResolver workOrderAccessContextResolver;

    /**
     * 首页专用聚合 Mapper，负责待办趋势与工单建单趋势等专用 SQL。
     */
    @Resource
    private DashboardMapper dashboardMapper;

    /**
     * 复用现有通知消息 Service，读取首页最新历史待办列表。
     */
    @Resource
    private NotifyMessageService notifyMessageService;

    /**
     * 查询服务主体首页总览。
     *
     * <p>核心流程：
     * 1. 校验当前登录主体必须为 SERVICE；
     * 2. 基于当前登录用户与公司聚合待办概览、待办趋势和最新历史待办；
     * 3. 若具备工单列表权限，则复用既有工单聚合能力与首页专用工单趋势 SQL；
     * 4. 统一输出首页专用返回结构，不再要求前端二次拼装分页列表结果。</p>
     *
     * @return 服务主体首页总览
     */
    @Override
    public ServiceDashboardHomeVO getHome() {
        validateSubjectType(SubjectTypeEnum.SERVICE);

        ServiceDashboardHomeVO home = new ServiceDashboardHomeVO();
        ServiceDashboardOverviewVO overview = new ServiceDashboardOverviewVO();
        DashboardTrend7dVO trend7d = buildEmptyWorkOrderTrend();

        // 首页待办完全以当前登录用户和当前登录公司为准，不允许前端另传口径。
        DashboardTodoQuery todoQuery = buildTodoQuery();
        DashboardTodoStatsVO todoStats = dashboardMapper.selectTodoStats(todoQuery);
        overview.setActiveTodoCount(defaultLong(todoStats == null ? null : todoStats.getActiveTodoCount()));
        overview.setHistoryTodoCount(defaultLong(todoStats == null ? null : todoStats.getHistoryTodoCount()));

        // 首页最新动态仍复用稳定的 HISTORY 列表能力，但只取最近若干条，不再拿分页总数做统计。
        home.setLatestHistoryTodos(listLatestHistoryTodos());

        // 先补齐待办趋势。即使当前人没有工单权限，也应该能看到自己待办近七天变化。
        fillTodoTrend(trend7d, dashboardMapper.selectActiveTodoTrend(todoQuery));

        if (hasWorkOrderListPermission()) {
            // 复用既有 status-count 统计能力，保证首页工单口径与现有工单权限模型保持一致。
            List<WorkOrderStatusCountVO> currentStatusRows = workOrderService.countByStatus(buildStatusQuery("CURRENT", null));
            List<WorkOrderStatusCountVO> allStatusRows = workOrderService.countByStatus(buildStatusQuery("ALL", null));
            overview.setWorkOrderTotal(resolveAllCount(currentStatusRows));
            home.setWorkOrderStatus(buildDashboardWorkOrderStatus(allStatusRows));

            // 工单趋势必须走首页专用聚合 SQL，否则继续复用分页列表会被 pageSize 截断。
            fillWorkOrderTrend(trend7d, dashboardMapper.selectCreatedWorkOrderTrend(buildWorkOrderTrendQuery("ALL")));
        } else {
            overview.setWorkOrderTotal(0L);
            home.setWorkOrderStatus(buildEmptyWorkOrderStatus());
            fillWorkOrderTrend(trend7d, Collections.emptyList());
        }

        home.setOverview(overview);
        home.setTrend7d(trend7d);
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
            throw new ServiceException("当前登录主体不支持访问服务主体首页");
        }
    }

    /**
     * 构建首页待办聚合查询参数。
     *
     * <p>首页待办统计只能使用当前登录用户和公司，
     * 否则会破坏“首页统计以后端当前登录上下文为准”的约束。</p>
     *
     * @return 待办聚合查询参数
     */
    private DashboardTodoQuery buildTodoQuery() {
        Long currentUserId = SecurityContext.getCurrentUserId();
        Long currentCompanyId = SecurityContext.getCurrentCompanyId();
        if (currentUserId == null) {
            throw new ServiceException("缺少当前登录用户上下文");
        }
        if (currentCompanyId == null) {
            throw new ServiceException("缺少当前登录公司上下文");
        }
        DashboardTodoQuery query = new DashboardTodoQuery();
        query.setReceiverId(currentUserId);
        query.setReceiverCompanyId(currentCompanyId);
        query.setStartTime(resolveTrendStartTime());
        query.setEndTime(resolveTrendEndTime());
        return query;
    }

    /**
     * 构建首页工单状态统计查询参数。
     *
     * <p>这里继续复用既有 `WorkOrderQuery + WorkOrderAccessContextResolver`，
     * 避免首页工单统计与列表页权限口径脱节。</p>
     *
     * @param viewScope 首页需要的工单视角
     * @param hasTransfer 是否仅统计转单工单
     * @return 工单状态统计查询参数
     */
    private WorkOrderQuery buildStatusQuery(String viewScope, Integer hasTransfer) {
        WorkOrderQuery query = new WorkOrderQuery();
        query.setViewScope(viewScope);
        query.setHasTransfer(hasTransfer);
        return query;
    }

    /**
     * 构建首页工单趋势查询参数。
     *
     * <p>趋势查询除了复用工单访问上下文，还需要固定最近七天时间窗口。
     * 如果不补这个对象，首页就只能继续拿分页列表结果做不准确的前端聚合。</p>
     *
     * @param viewScope 工单视角
     * @return 工单趋势聚合查询参数
     */
    private DashboardWorkOrderTrendQuery buildWorkOrderTrendQuery(String viewScope) {
        DashboardWorkOrderTrendQuery query = new DashboardWorkOrderTrendQuery();
        query.setViewScope(viewScope);
        query.setHasTransfer(null);
        query.setStartTime(resolveTrendStartTime());
        query.setEndTime(resolveTrendEndTime());

        // 工单趋势 SQL 必须复用现有工单访问上下文，否则首页趋势会出现“列表页看不见、首页看得见”的越权风险。
        WorkOrderAccessContext accessContext = workOrderAccessContextResolver.resolve();
        query.setAccessContext(accessContext);
        return query;
    }

    /**
     * 读取首页最新历史待办列表。
     *
     * <p>这里有意继续复用现有 HISTORY 分页能力，因为首页只展示“最近若干条列表项”，
     * 并不会再拿分页总数或分页 records 做趋势统计。</p>
     *
     * @return 首页最新历史待办列表
     */
    private List<DashboardHistoryTodoVO> listLatestHistoryTodos() {
        NotifyMessageQuery query = new NotifyMessageQuery();
        query.setBox("HISTORY");
        query.setPageNum(1);
        query.setPageSize(LATEST_HISTORY_TODO_LIMIT);
        query.setReceiverId(SecurityContext.getCurrentUserId());
        query.setReceiverCompanyId(SecurityContext.getCurrentCompanyId());

        PageResult<NotifyMessagePageVO> pageResult = notifyMessageService.listPage(query);
        List<NotifyMessagePageVO> records = pageResult == null ? Collections.emptyList() : pageResult.getRecords();
        if (records == null || records.isEmpty()) {
            return Collections.emptyList();
        }
        List<DashboardHistoryTodoVO> result = new ArrayList<>();
        for (NotifyMessagePageVO record : records) {
            if (record == null) {
                continue;
            }
            DashboardHistoryTodoVO item = new DashboardHistoryTodoVO();
            item.setId(record.getId());
            item.setTitle(record.getTitle());
            item.setSummary(record.getSummary());
            item.setBizType(record.getBizType());
            item.setBizId(record.getBizId());
            item.setRouteType(record.getRouteType());
            item.setRouteValue(record.getRouteValue());
            item.setTodoStatus(record.getTodoStatus());
            item.setCreateTime(record.getCreateTime());
            result.add(item);
        }
        return result;
    }

    /**
     * 把待办趋势按最近七天完整补齐。
     *
     * @param trend7d 首页趋势对象
     * @param rows 待办按天聚合结果
     */
    private void fillTodoTrend(DashboardTrend7dVO trend7d, List<DashboardCountByDayVO> rows) {
        List<String> dayKeys = buildRecentDayKeys();
        Map<String, Long> countMap = buildDayCountMap(rows);
        trend7d.setDayKeys(dayKeys);
        List<Long> counts = new ArrayList<>();
        for (String dayKey : dayKeys) {
            counts.add(countMap.getOrDefault(dayKey, 0L));
        }
        trend7d.setActiveTodoCounts(counts);
    }

    /**
     * 把工单趋势按最近七天完整补齐。
     *
     * @param trend7d 首页趋势对象
     * @param rows 工单按天聚合结果
     */
    private void fillWorkOrderTrend(DashboardTrend7dVO trend7d, List<DashboardCountByDayVO> rows) {
        List<String> dayKeys = trend7d.getDayKeys();
        if (dayKeys == null || dayKeys.isEmpty()) {
            dayKeys = buildRecentDayKeys();
            trend7d.setDayKeys(dayKeys);
        }
        Map<String, Long> countMap = buildDayCountMap(rows);
        List<Long> counts = new ArrayList<>();
        for (String dayKey : dayKeys) {
            counts.add(countMap.getOrDefault(dayKey, 0L));
        }
        trend7d.setCreatedWorkOrderCounts(counts);
    }

    /**
     * 将工单状态计数列表收敛成首页专用固定字段。
     *
     * @param rows 既有状态计数列表
     * @return 首页工单状态结构
     */
    private DashboardWorkOrderStatusVO buildDashboardWorkOrderStatus(List<WorkOrderStatusCountVO> rows) {
        Map<String, Long> statusCountMap = new LinkedHashMap<>();
        if (rows != null) {
            for (WorkOrderStatusCountVO row : rows) {
                if (row != null && row.getMainStatus() != null) {
                    statusCountMap.put(row.getMainStatus(), defaultLong(row.getCountNum()));
                }
            }
        }
        DashboardWorkOrderStatusVO status = new DashboardWorkOrderStatusVO();
        status.setAll(statusCountMap.getOrDefault("ALL", 0L));
        status.setPendingAssign(statusCountMap.getOrDefault("PENDING_ASSIGN", 0L));
        status.setPendingTechAccept(statusCountMap.getOrDefault("PENDING_TECH_ACCEPT", 0L));
        status.setInProgress(statusCountMap.getOrDefault("IN_PROGRESS", 0L));
        status.setCompleted(statusCountMap.getOrDefault("COMPLETED", 0L));
        status.setClosed(statusCountMap.getOrDefault("CLOSED", 0L));
        return status;
    }

    /**
     * 创建空的工单状态结构。
     *
     * @return 空状态结构
     */
    private DashboardWorkOrderStatusVO buildEmptyWorkOrderStatus() {
        DashboardWorkOrderStatusVO status = new DashboardWorkOrderStatusVO();
        status.setAll(0L);
        status.setPendingAssign(0L);
        status.setPendingTechAccept(0L);
        status.setInProgress(0L);
        status.setCompleted(0L);
        status.setClosed(0L);
        return status;
    }

    /**
     * 创建空的近七天趋势结构。
     *
     * @return 空趋势结构
     */
    private DashboardTrend7dVO buildEmptyWorkOrderTrend() {
        DashboardTrend7dVO trend7d = new DashboardTrend7dVO();
        List<String> dayKeys = buildRecentDayKeys();
        trend7d.setDayKeys(dayKeys);
        trend7d.setCreatedWorkOrderCounts(buildZeroCounts(dayKeys.size()));
        trend7d.setActiveTodoCounts(buildZeroCounts(dayKeys.size()));
        return trend7d;
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
     * 把按天聚合结果转换成便于补齐缺失日期的 Map。
     *
     * @param rows 按天聚合结果
     * @return 日期键到数量的映射
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
     * 创建指定长度的零值数组。
     *
     * @param size 数组长度
     * @return 零值数组
     */
    private List<Long> buildZeroCounts(int size) {
        List<Long> result = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            result.add(0L);
        }
        return result;
    }

    /**
     * 读取状态计数列表中的 ALL 数量。
     *
     * @param rows 状态计数列表
     * @return ALL 数量
     */
    private Long resolveAllCount(List<WorkOrderStatusCountVO> rows) {
        if (rows == null) {
            return 0L;
        }
        for (WorkOrderStatusCountVO row : rows) {
            if (row != null && "ALL".equals(row.getMainStatus())) {
                return defaultLong(row.getCountNum());
            }
        }
        return 0L;
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

    /**
     * 判断当前人是否具备工单统计能力。
     *
     * @return true 表示可读取工单首页统计
     */
    private boolean hasWorkOrderListPermission() {
        return StpUtil.hasPermission("workorder:list");
    }

    /**
     * 解析首页趋势开始时间。
     *
     * @return 最近七天的开始时间，含边界
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
}
