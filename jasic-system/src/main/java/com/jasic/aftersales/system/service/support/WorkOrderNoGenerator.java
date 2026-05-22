package com.jasic.aftersales.system.service.support;

import com.jasic.aftersales.common.exception.ServiceException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/*** 工单号生成器。
 *
 * 规则：JSWX + yyyyMMdd + 五位日流水号

@author Zoro*/
@Component
public class WorkOrderNoGenerator {

    /**ORDER_NO_PREFIX 常量，用于固定当前类内部复用的业务编码、默认值或配置边界。*/
    private static final String ORDER_NO_PREFIX = "JSWX";
    /**REDIS_KEY_PREFIX 常量，用于固定当前类内部复用的业务编码、默认值或配置边界。*/
    private static final String REDIS_KEY_PREFIX = "workorder:order-no:";
    /**DATE_FORMATTER 常量，用于固定当前类内部复用的业务编码、默认值或配置边界。*/
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    /**MAX_DAILY_SEQUENCE 常量，用于固定当前类内部复用的业务编码、默认值或配置边界。*/
    private static final long MAX_DAILY_SEQUENCE = 99999L;

    /**
     * StringRedis模板模板依赖。
     *
     * @return 业务处理结果
     */
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 处理nextOrderNo业务逻辑。
     *
     * <p>说明：该方法用于执行业务流程编排，确保调用链路清晰可维护。</p>
     * @return 业务处理结果
     */
    public String nextOrderNo() {
        LocalDate today = LocalDate.now();
        String datePart = today.format(DATE_FORMATTER);
        long sequence = nextDailySequence(datePart);
        return ORDER_NO_PREFIX + datePart + String.format("%05d", sequence);
    }

    /**
     * nextDailySequence。
     *
     * @param datePart 时间值，用于业务节点记录或时效判断。
     * @return 业务处理结果
     */
    private long nextDailySequence(String datePart) {
        String redisKey = REDIS_KEY_PREFIX + datePart;
        Long sequence = stringRedisTemplate.opsForValue().increment(redisKey);
        if (sequence == null) {
            throw new ServiceException("工单号生成失败，请稍后重试");
        }
        if (sequence > MAX_DAILY_SEQUENCE) {
            throw new ServiceException("当日工单号流水已达上限，请联系管理员处理");
        }

        Duration ttl = Duration.between(LocalDateTime.now(), LocalDate.now().plusDays(1).atStartOfDay());
        if (!ttl.isNegative() && !ttl.isZero()) {
            stringRedisTemplate.expire(redisKey, ttl);
        }
        return sequence;
    }
}


