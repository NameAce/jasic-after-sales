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
     * ?? schedulerFactoryBeanCustomizer ?????
     *
     * @param beanFactory ??
     * @return ????
     */
    @Bean
    public SchedulerFactoryBeanCustomizer schedulerFactoryBeanCustomizer(AutowireCapableBeanFactory beanFactory) {
        return schedulerFactoryBean -> schedulerFactoryBean.setJobFactory(new SpringBeanJobFactory() {
            /**
             * ?????
             *
             * @param bundle ??
             * @return ????
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
     * ?? notifyEventConsumeJobDetail ?????
     *
     * @return ????
     */
    @Bean
    public JobDetail notifyEventConsumeJobDetail() {
        return JobBuilder.newJob(NotifyEventConsumeJob.class)
                .withIdentity(NOTIFY_EVENT_JOB_NAME, NOTIFY_EVENT_JOB_GROUP)
                .storeDurably()
                .build();
    }

    /**
     * ?? notifyEventConsumeTrigger ?????
     *
     * @param jobDetail ??
     * @param intervalSeconds ??
     * @return ????
     */
    @Bean
    public Trigger notifyEventConsumeTrigger(
            @Qualifier("notifyEventConsumeJobDetail") JobDetail jobDetail,
            @Value("${jasic.notify.consume-interval-seconds:10}") int intervalSeconds) {
        return buildSimpleTrigger(jobDetail, NOTIFY_EVENT_TRIGGER_NAME, NOTIFY_EVENT_JOB_GROUP, intervalSeconds);
    }

    /**
     * ?? notifyDispatchSendJobDetail ?????
     *
     * @return ????
     */
    @Bean
    public JobDetail notifyDispatchSendJobDetail() {
        return JobBuilder.newJob(NotifyDispatchSendJob.class)
                .withIdentity(NOTIFY_DISPATCH_JOB_NAME, NOTIFY_DISPATCH_JOB_GROUP)
                .storeDurably()
                .build();
    }

    /**
     * ?? notifyDispatchSendTrigger ?????
     *
     * @param jobDetail ??
     * @param intervalSeconds ??
     * @return ????
     */
    @Bean
    public Trigger notifyDispatchSendTrigger(
            @Qualifier("notifyDispatchSendJobDetail") JobDetail jobDetail,
            @Value("${jasic.notify.dispatch-interval-seconds:10}") int intervalSeconds) {
        return buildSimpleTrigger(jobDetail, NOTIFY_DISPATCH_TRIGGER_NAME, NOTIFY_DISPATCH_JOB_GROUP, intervalSeconds);
    }

    /**
     * ???????
     *
     * @param jobDetail ??
     * @param triggerName ??
     * @param triggerGroup ??
     * @param intervalSeconds ??
     * @return ????
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
