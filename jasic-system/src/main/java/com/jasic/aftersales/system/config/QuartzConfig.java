package com.jasic.aftersales.system.config;

import com.jasic.aftersales.system.notify.job.NotifyDispatchSendJob;
import com.jasic.aftersales.system.notify.job.NotifyEventConsumeJob;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.spi.TriggerFiredBundle;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.boot.autoconfigure.quartz.SchedulerFactoryBeanCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.SpringBeanJobFactory;

/**
 * Quartz configuration.
 *
 * @author Codex
 * @date 2026/04/12
 */
@Configuration
public class QuartzConfig {

    private static final String NOTIFY_EVENT_JOB_GROUP = "NOTIFY_EVENT";
    private static final String NOTIFY_EVENT_JOB_NAME = "notify-event-consume-job";
    private static final String NOTIFY_EVENT_TRIGGER_NAME = "notify-event-consume-trigger";
    private static final String NOTIFY_DISPATCH_JOB_GROUP = "NOTIFY_DISPATCH";
    private static final String NOTIFY_DISPATCH_JOB_NAME = "notify-dispatch-send-job";
    private static final String NOTIFY_DISPATCH_TRIGGER_NAME = "notify-dispatch-send-trigger";

    /**
     * schedulerFactoryBeanCustomizer。
     *
     * @param beanFactory 参数
     * @return 处理结果
     */
    @Bean
    public SchedulerFactoryBeanCustomizer schedulerFactoryBeanCustomizer(AutowireCapableBeanFactory beanFactory) {
        return schedulerFactoryBean -> schedulerFactoryBean.setJobFactory(new SpringBeanJobFactory() {
            /**
     * 创建任务Instance。
     *
     * @param bundle 参数
     * @return 处理结果
             */
            @Override
            protected Object createJobInstance(TriggerFiredBundle bundle) throws Exception {
                // 调用createJobInstance方法，复用统一能力并保证业务规则一致。
                Object job = super.createJobInstance(bundle);
                // 调用autowireBean方法，复用统一能力并保证业务规则一致。
                beanFactory.autowireBean(job);
                return job;
            }
        });
    }

    /**
     * notify事件消费任务详情。
     *
     * @return 处理结果
     */
    @Bean
    public JobDetail notifyEventConsumeJobDetail() {
        return JobBuilder.newJob(NotifyEventConsumeJob.class)
                .withIdentity(NOTIFY_EVENT_JOB_NAME, NOTIFY_EVENT_JOB_GROUP)
                .storeDurably()
                // 调用build方法，复用统一能力并保证业务规则一致。
                .build();
    }

    /**
     * notify事件消费Trigger。
     *
     * @param jobDetail 参数
     * @param intervalSeconds 参数
     * @return 处理结果
     */
    @Bean
    public Trigger notifyEventConsumeTrigger(
            @Qualifier("notifyEventConsumeJobDetail") JobDetail jobDetail,
            @Value("${jasic.notify.consume-interval-seconds:10}") int intervalSeconds) {
        return buildSimpleTrigger(jobDetail, NOTIFY_EVENT_TRIGGER_NAME, NOTIFY_EVENT_JOB_GROUP, intervalSeconds);
    }

    /**
     * notify分发发送任务详情。
     *
     * @return 处理结果
     */
    @Bean
    public JobDetail notifyDispatchSendJobDetail() {
        return JobBuilder.newJob(NotifyDispatchSendJob.class)
                .withIdentity(NOTIFY_DISPATCH_JOB_NAME, NOTIFY_DISPATCH_JOB_GROUP)
                .storeDurably()
                // 调用build方法，复用统一能力并保证业务规则一致。
                .build();
    }

    /**
     * notify分发发送Trigger。
     *
     * @param jobDetail 参数
     * @param intervalSeconds 参数
     * @return 处理结果
     */
    @Bean
    public Trigger notifyDispatchSendTrigger(
            @Qualifier("notifyDispatchSendJobDetail") JobDetail jobDetail,
            @Value("${jasic.notify.dispatch-interval-seconds:10}") int intervalSeconds) {
        return buildSimpleTrigger(jobDetail, NOTIFY_DISPATCH_TRIGGER_NAME, NOTIFY_DISPATCH_JOB_GROUP, intervalSeconds);
    }

    /**
     * 构建SimpleTrigger。
     *
     * @param jobDetail 参数
     * @param triggerName 参数
     * @param triggerGroup 参数
     * @param intervalSeconds 参数
     * @return 处理结果
     */
    private Trigger buildSimpleTrigger(JobDetail jobDetail, String triggerName, String triggerGroup, int intervalSeconds) {
        // 调用max方法，复用统一能力并保证业务规则一致。
        int safeIntervalSeconds = Math.max(intervalSeconds, 1);
        return TriggerBuilder.newTrigger()
                .withIdentity(triggerName, triggerGroup)
                .forJob(jobDetail)
                .startNow()
                .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                        .withIntervalInSeconds(safeIntervalSeconds)
                        .repeatForever()
                        .withMisfireHandlingInstructionNextWithRemainingCount())
                // 调用build方法，复用统一能力并保证业务规则一致。
                .build();
    }
}


