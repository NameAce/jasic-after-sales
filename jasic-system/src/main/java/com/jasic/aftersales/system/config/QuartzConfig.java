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
 * @author Zoro
 * @date 2026/04/12
 */
@Configuration
public class QuartzConfig {

    /**NOTIFY_EVENT_JOB_GROUP 常量，用于固定当前类内部复用的业务编码、默认值或配置边界。*/
    private static final String NOTIFY_EVENT_JOB_GROUP = "NOTIFY_EVENT";
    /**NOTIFY_EVENT_JOB_NAME 常量，用于固定当前类内部复用的业务编码、默认值或配置边界。*/
    private static final String NOTIFY_EVENT_JOB_NAME = "notify-event-consume-job";
    /**NOTIFY_EVENT_TRIGGER_NAME 常量，用于固定当前类内部复用的业务编码、默认值或配置边界。*/
    private static final String NOTIFY_EVENT_TRIGGER_NAME = "notify-event-consume-trigger";
    /**NOTIFY_DISPATCH_JOB_GROUP 常量，用于固定当前类内部复用的业务编码、默认值或配置边界。*/
    private static final String NOTIFY_DISPATCH_JOB_GROUP = "NOTIFY_DISPATCH";
    /**NOTIFY_DISPATCH_JOB_NAME 常量，用于固定当前类内部复用的业务编码、默认值或配置边界。*/
    private static final String NOTIFY_DISPATCH_JOB_NAME = "notify-dispatch-send-job";
    /**NOTIFY_DISPATCH_TRIGGER_NAME 常量，用于固定当前类内部复用的业务编码、默认值或配置边界。*/
    private static final String NOTIFY_DISPATCH_TRIGGER_NAME = "notify-dispatch-send-trigger";

    /**
     * schedulerFactoryBeanCustomizer。
     *
     * @param beanFactory beanFactory，当前业务处理所需的输入值。
     * @return 业务处理结果
     */
    @Bean
    public SchedulerFactoryBeanCustomizer schedulerFactoryBeanCustomizer(AutowireCapableBeanFactory beanFactory) {
        return schedulerFactoryBean -> schedulerFactoryBean.setJobFactory(new SpringBeanJobFactory() {
            /**
     * 创建任务Instance。
     *
     * @param bundle bundle，当前业务处理所需的输入值。
     * @return 业务处理结果
             */
            @Override
            protected Object createJobInstance(TriggerFiredBundle bundle) throws Exception {
                Object job = super.createJobInstance(bundle);
                beanFactory.autowireBean(job);
                return job;
            }
        });
    }

    /**
     * notify事件消费任务详情。
     *
     * @return 业务处理结果
     */
    @Bean
    public JobDetail notifyEventConsumeJobDetail() {
        return JobBuilder.newJob(NotifyEventConsumeJob.class)
                .withIdentity(NOTIFY_EVENT_JOB_NAME, NOTIFY_EVENT_JOB_GROUP)
                .storeDurably()
                .build();
    }

    /**
     * notify事件消费Trigger。
     *
     * @param jobDetail jobDetail，当前业务处理所需的输入值。
     * @param intervalSeconds intervalSeconds，当前业务处理所需的输入值。
     * @return 业务处理结果
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
     * @return 业务处理结果
     */
    @Bean
    public JobDetail notifyDispatchSendJobDetail() {
        return JobBuilder.newJob(NotifyDispatchSendJob.class)
                .withIdentity(NOTIFY_DISPATCH_JOB_NAME, NOTIFY_DISPATCH_JOB_GROUP)
                .storeDurably()
                .build();
    }

    /**
     * notify分发发送Trigger。
     *
     * @param jobDetail jobDetail，当前业务处理所需的输入值。
     * @param intervalSeconds intervalSeconds，当前业务处理所需的输入值。
     * @return 业务处理结果
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
     * @param jobDetail jobDetail，当前业务处理所需的输入值。
     * @param triggerName triggerName，当前业务处理所需的输入值。
     * @param triggerGroup triggerGroup，当前业务处理所需的输入值。
     * @param intervalSeconds intervalSeconds，当前业务处理所需的输入值。
     * @return 业务处理结果
     */
    private Trigger buildSimpleTrigger(JobDetail jobDetail, String triggerName, String triggerGroup, int intervalSeconds) {
        int safeIntervalSeconds = Math.max(intervalSeconds, 1);
        return TriggerBuilder.newTrigger()
                .withIdentity(triggerName, triggerGroup)
                .forJob(jobDetail)
                .startNow()
                .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                        .withIntervalInSeconds(safeIntervalSeconds)
                        .repeatForever()
                        .withMisfireHandlingInstructionNextWithRemainingCount())
                .build();
    }
}


