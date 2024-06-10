package org.edu_sharing.repository.server.jobs.quartz;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.log4j.Level;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.impl.JobDetailImpl;
import org.quartz.utils.Key;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class JobInfo implements Serializable {
    private static final Logger log = LoggerFactory.getLogger(JobInfo.class);
    private final String uniqueId;
    private Class jobClass;
    private Key jobKey;
    private JobDataMap jobDataMap;
    private String jobName;
    private String jobGroup;
    private long threadId = -1;

    public JobInfo(JobDetail jobDetail) {
        uniqueId = UUID.randomUUID().toString();
        setJobDetail(jobDetail);
        setStartTime(System.currentTimeMillis());
        setStatus(Status.Running);
    }

    public JobDataMap getJobDataMap() {
        return jobDataMap;
    }

    public Class getJobClass() {
        return jobClass;
    }

    public boolean equalsDetail(JobDetail other) {
        return
                Objects.equals(((JobDetailImpl)other).getName(), jobName) &&
                        Objects.equals(((JobDetailImpl)other).getGroup(), jobGroup);
    }

    @Override
    public boolean equals(Object other) {
        if(other instanceof JobInfo) {
            return Objects.equals(((JobInfo) other).uniqueId, uniqueId);
        }
        return super.equals(other);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uniqueId);
    }

    public void setWorstLevel(Level level) {
        this.worstLevel = level;
    }

    public long getThreadId() {
        return threadId;
    }

    public void setThreadId(long threadId) {
        this.threadId = threadId;
    }

    public enum Status{
        Running,
        Failed,
        Aborted,
        Finished
    }
    private long startTime,finishTime;
    private Status status;
    private Level worstLevel=Level.ALL;

    public void setJobDetail(JobDetail jobDetail) {
        jobClass = jobDetail.getJobClass();
        jobName = ((JobDetailImpl)jobDetail).getName();
        jobGroup = ((JobDetailImpl)jobDetail).getGroup();
        jobDataMap = jobDetail.getJobDataMap();
    }

    public Level getWorstLevel() {
        return worstLevel;
    }

    public long getFinishTime() {
        return finishTime;
    }

    public void setFinishTime(long finishTime) {
        this.finishTime = finishTime;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }


    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public String getJobName() {
        return jobName;
    }

    public String getJobGroup() {
        return jobGroup;
    }

    @JsonProperty
    List<LogEntry> getLog() {
        return JobLogger.getLog(this);
    }

}
