package com.jasic.aftersales.system.config;

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
 * Quartz 配置
 *
 * @author Codex
 * @date 2026/04/12
 */
@Configuration
public class QuartzConfig {

    private static final String NOTIFY_JOB_GROUP = "NOTIFY_EVENT";
    private static final String NOTIFY_JOB_NAME = "notify-event-consume-job";
    private static final String NOTIFY_TRIGGER_NAME = "notify-event-consume-trigger";

    @Bean
    public SchedulerFactoryBeanCustomizer schedulerFactoryBeanCustomizer(AutowireCapableBeanFactory beanFactory) {
        return schedulerFactoryBean -> schedulerFactoryBean.setJobFactory(new SpringBeanJobFactory() {
            @Override
            protected Object createJobInstance(TriggerFiredBundle bundle) throws Exception {
                Object job = super.createJobInstance(bundle);
                beanFactory.autowireBean(job);
                return job;
            }
        });
    }

    @Bean
    public JobDetail notifyEventConsumeJobDetail() {
        return JobBuilder.newJob(NotifyEventConsumeJob.class)
                .withIdentity(NOTIFY_JOB_NAME, NOTIFY_JOB_GROUP)
                .storeDurably()
                .build();
    }

    @Bean
    public Trigger notifyEventConsumeTrigger(
            @Qualifier("notifyEventConsumeJobDetail") JobDetail jobDetail,
            @Value("${jasic.notify.consume-interval-seconds:10}") int intervalSeconds) {
        int safeIntervalSeconds = Math.max(intervalSeconds, 1);
        return TriggerBuilder.newTrigger()
                .withIdentity(NOTIFY_TRIGGER_NAME, NOTIFY_JOB_GROUP)
                .forJob(jobDetail)
                .startNow()
                .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                        .withIntervalInSeconds(safeIntervalSeconds)
                        .repeatForever()
                        .withMisfireHandlingInstructionNextWithRemainingCount())
                .build();
    }
}
