package com.jasic.aftersales.system.service.support;

import com.jasic.aftersales.common.exception.ServiceException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 工单号生成器。
 *
 * 规则：JSWX + yyyyMMdd + 五位日流水号
 */
@Component
public class WorkOrderNoGenerator {

    private static final String ORDER_NO_PREFIX = "JSWX";
    private static final String REDIS_KEY_PREFIX = "workorder:order-no:";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final long MAX_DAILY_SEQUENCE = 99999L;

    /**
     * ?? nextOrderNo ?????
     *
     * @return ?????
     */
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    public String nextOrderNo() {
        LocalDate today = LocalDate.now();
        String datePart = today.format(DATE_FORMATTER);
        long sequence = nextDailySequence(datePart);
        return ORDER_NO_PREFIX + datePart + String.format("%05d", sequence);
    }

    /**
     * ?? nextDailySequence ?????
     *
     * @param datePart ??
     * @return ????
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
