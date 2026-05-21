package com.jasic.aftersales.system.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.jasic.aftersales.common.enums.SubjectTypeEnum;
import com.jasic.aftersales.common.exception.ServiceException;
import com.jasic.aftersales.framework.security.SecurityContext;
import com.jasic.aftersales.system.domain.query.dashboard.DashboardOperLogQuery;
import com.jasic.aftersales.system.domain.vo.dashboard.DashboardCountByDayVO;
import com.jasic.aftersales.system.domain.vo.dashboard.DashboardOperLogTrend7dVO;
import com.jasic.aftersales.system.domain.vo.dashboard.DashboardPlatformOverviewStatsVO;
import com.jasic.aftersales.system.domain.vo.dashboard.DashboardSubjectTypeCountVO;
import com.jasic.aftersales.system.domain.vo.dashboard.DashboardSubjectTypeDistributionVO;
import com.jasic.aftersales.system.domain.vo.dashboard.PlatformDashboardHomeVO;
import com.jasic.aftersales.system.domain.vo.dashboard.PlatformDashboardOverviewVO;
import com.jasic.aftersales.system.mapper.DashboardMapper;
import com.jasic.aftersales.system.service.IPlatformDashboardService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 平台首页 Service 实现。
 *
 * <p>该实现负责把平台首页组织治理概览、主体类型分布与操作日志趋势
 * 统一收敛为首页专用结构，替代前端继续复用公司/用户/角色/日志分页接口做二次统计。</p>
 *
 * @author Codex
 * @date 2026/05/20
 */
@Service
public class PlatformDashboardServiceImpl implements IPlatformDashboardService {

    /**
     * 首页趋势固定展示最近 7 天。
     */
    private static final int TREND_DAYS = 7;

    /**
     * 首页专用聚合 Mapper，负责平台治理与日志趋势统计。
     */
    @Resource
    private DashboardMapper dashboardMapper;

    /**
     * 查询平台首页总览。
     *
     * <p>核心流程：
     * 1. 校验当前主体必须为 PLATFORM；
     * 2. 使用首页专用聚合 SQL 读取组织治理概览与主体类型分布；
     * 3. 使用首页专用聚合 SQL 读取近七天操作日志趋势与失败数；
     * 4. 对缺少权限的数据块做服务端兜底，避免新首页接口整体 403。</p>
     *
     * @return 平台首页总览
     */
    @Override
    public PlatformDashboardHomeVO getHome() {
        validateSubjectType(SubjectTypeEnum.PLATFORM);

        PlatformDashboardHomeVO home = new PlatformDashboardHomeVO();
        DashboardPlatformOverviewStatsVO rawOverview = dashboardMapper.selectPlatformOverviewStats();
        home.setOverview(buildOverview(rawOverview));
        home.setSubjectTypeDistribution(buildSubjectTypeDistribution());
        home.setOperLogTrend7d(buildOperLogTrend());
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
            throw new ServiceException("当前登录主体不支持访问平台首页");
        }
    }

    /**
     * 构建平台首页概览卡片。
     *
     * <p>平台首页允许按能力块做权限兜底。
     * 因此即使首页接口整体可访问，也需要对用户、角色、通知场景等敏感统计做服务端降级。</p>
     *
     * @param rawOverview 聚合 SQL 原始结果
     * @return 平台首页概览卡片
     */
    private PlatformDashboardOverviewVO buildOverview(DashboardPlatformOverviewStatsVO rawOverview) {
        PlatformDashboardOverviewVO overview = new PlatformDashboardOverviewVO();

        if (hasPermission("org:company:list")) {
            overview.setCompanyTotal(defaultLong(rawOverview == null ? null : rawOverview.getCompanyTotal()));
            overview.setEnabledCompanyTotal(defaultLong(rawOverview == null ? null : rawOverview.getEnabledCompanyTotal()));
        } else {
            overview.setCompanyTotal(0L);
            overview.setEnabledCompanyTotal(0L);
        }

        if (hasPermission("system:user:list")) {
            overview.setUserTotal(defaultLong(rawOverview == null ? null : rawOverview.getUserTotal()));
        } else {
            overview.setUserTotal(0L);
        }

        if (hasPermission("system:role:list")) {
            overview.setRoleTotal(defaultLong(rawOverview == null ? null : rawOverview.getRoleTotal()));
        } else {
            overview.setRoleTotal(0L);
        }

        if (hasPermission("system:notifyScene:list")) {
            overview.setNotifySceneTotal(defaultLong(rawOverview == null ? null : rawOverview.getNotifySceneTotal()));
        } else {
            overview.setNotifySceneTotal(0L);
        }
        return overview;
    }

    /**
     * 构建主体类型分布。
     *
     * @return 主体类型分布
     */
    private DashboardSubjectTypeDistributionVO buildSubjectTypeDistribution() {
        DashboardSubjectTypeDistributionVO distribution = new DashboardSubjectTypeDistributionVO();
        if (!hasPermission("org:company:list")) {
            distribution.setPlatformCount(0L);
            distribution.setHqCount(0L);
            distribution.setServiceCount(0L);
            return distribution;
        }

        List<DashboardSubjectTypeCountVO> rows = dashboardMapper.selectSubjectTypeDistribution();
        Map<String, Long> countMap = new LinkedHashMap<>();
        if (rows != null) {
            for (DashboardSubjectTypeCountVO row : rows) {
                if (row != null && row.getSubjectType() != null) {
                    countMap.put(row.getSubjectType(), defaultLong(row.getCountNum()));
                }
            }
        }
        distribution.setPlatformCount(countMap.getOrDefault(SubjectTypeEnum.PLATFORM.getCode(), 0L));
        distribution.setHqCount(countMap.getOrDefault(SubjectTypeEnum.HQ.getCode(), 0L));
        distribution.setServiceCount(countMap.getOrDefault(SubjectTypeEnum.SERVICE.getCode(), 0L));
        return distribution;
    }

    /**
     * 构建平台首页操作日志近七天趋势。
     *
     * @return 操作日志近七天趋势
     */
    private DashboardOperLogTrend7dVO buildOperLogTrend() {
        DashboardOperLogTrend7dVO trend7d = new DashboardOperLogTrend7dVO();
        List<String> dayKeys = buildRecentDayKeys();
        trend7d.setDayKeys(dayKeys);

        if (!hasPermission("log:operLog:list")) {
            trend7d.setOperLogCounts(buildZeroCounts(dayKeys.size()));
            trend7d.setFailedCount(0L);
            return trend7d;
        }

        DashboardOperLogQuery query = new DashboardOperLogQuery();
        query.setStartTime(resolveTrendStartTime());
        query.setEndTime(resolveTrendEndTime());

        Map<String, Long> countMap = buildDayCountMap(dashboardMapper.selectOperLogTrend(query));
        List<Long> counts = new ArrayList<>();
        for (String dayKey : dayKeys) {
            counts.add(countMap.getOrDefault(dayKey, 0L));
        }
        trend7d.setOperLogCounts(counts);
        trend7d.setFailedCount(defaultLong(dashboardMapper.selectOperLogFailedCount(query)));
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
     * 统一兜底空数量。
     *
     * @param value 原始数量
     * @return 非空数量
     */
    private Long defaultLong(Long value) {
        return value == null ? 0L : value;
    }

    /**
     * 判断当前人是否具备指定权限。
     *
     * @param permission 权限标识
     * @return true 表示具备权限
     */
    private boolean hasPermission(String permission) {
        return StpUtil.hasPermission(permission);
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
