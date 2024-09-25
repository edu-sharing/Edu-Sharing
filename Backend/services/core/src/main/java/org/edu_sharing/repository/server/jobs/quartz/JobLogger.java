package org.edu_sharing.repository.server.jobs.quartz;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Core;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.springframework.context.ApplicationContext;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Plugin(
        name = "JobLogger",
        category = Core.CATEGORY_NAME,
        elementType = Appender.ELEMENT_TYPE)
public class JobLogger extends AbstractAppender {
    private static final int MAX_LOG_ENTRIES = 5000;
    /**
     * logs are not shared in cluster for performance reasongs
     */
    private static Map<JobInfo, List<LogEntry>> logs = new ConcurrentHashMap<>();

    private final ApplicationContext applicationContext;

    public static final List<String> IGNORABLE_JOBS = new ArrayList<>();

    static {
        IGNORABLE_JOBS.add(SystemStatisticJob.class.getName());
        IGNORABLE_JOBS.add(TimedPermissionJob.class.getName());
        IGNORABLE_JOBS.add("org.edu_sharing.repository.server.jobs.quartz.ClusterInfoJob");
    }

    public JobLogger(String name, Filter filter, ApplicationContext applicationContext) {
        super(name, filter, null);
        this.applicationContext = applicationContext;
    }

    public static void addLog(JobInfo jobInfo, LogEntry entry) {
        List<LogEntry> log = logs.get(jobInfo);
        if (log == null) {
            log = new ArrayList<>();
        }
        log.add(entry);
        if (entry.level.isGreaterOrEqual(jobInfo.getWorstLevel())) {
            jobInfo.setWorstLevel(entry.level);
            JobHandler.refreshJobsCache(jobInfo);
        }
        if (log.size() > MAX_LOG_ENTRIES) {
            log.remove(0);
        }
        logs.put(jobInfo, log);
    }

    @PluginFactory
    public static JobLogger createAppender(
            @PluginAttribute("name") String name,
            @PluginElement("Filter") Filter filter,
            ApplicationContext applicationContext) {
        return new JobLogger(name, filter, applicationContext);
    }

    public static List<LogEntry> getLog(JobInfo jobInfo) {
        return logs.get(jobInfo);
    }

    public static void init(JobInfo info) {
        logs.put(info, new ArrayList<>());
    }

    @Override
    public void append(LogEvent event) {
        if (event.getLoggerName().equals(JobHandler.class.getName())) {
            return;
        }
        try {
            for (JobInfo job : JobHandler.getInstance(applicationContext).getAllRunningJobs()) {
                if (!job.getStatus().equals(JobInfo.Status.Running))
                    continue;
                String clazz = job.getJobClass().getName();
                String message = event.getMessage().getFormattedMessage();
                if (event.getThrown() != null) {
                    message += "\n\n" + StringUtils.join(event.getThrown().getStackTrace(), "\n");
                }


                if (job.getThreadId() == Thread.currentThread().getId()) {
                    JobLogger.addLog(job, new LogEntry(org.apache.log4j.Level.toLevel(event.getLevel().name()), event.getInstant().getEpochMillisecond(), event.getLoggerName(), message));
                } else if (clazz.equals(event.getLoggerName())) {
                    JobLogger.addLog(job, new LogEntry(org.apache.log4j.Level.toLevel(event.getLevel().name()), event.getInstant().getEpochMillisecond(), event.getLoggerName(), message));
                }
                // importer job mapping
                else if (clazz.equals(ImporterJob.class.getName()) && event.getLoggerName().startsWith("org.edu_sharing.repository.server.importer")) {
                    JobLogger.addLog(job, new LogEntry(org.apache.log4j.Level.toLevel(event.getLevel().name()), event.getInstant().getEpochMillisecond(), event.getLoggerName(), message));
                }
            }
        } catch (Exception e) {

        }
    }
}
