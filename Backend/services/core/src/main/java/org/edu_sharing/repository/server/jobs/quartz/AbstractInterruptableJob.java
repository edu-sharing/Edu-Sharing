package org.edu_sharing.repository.server.jobs.quartz;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.UnableToInterruptJobException;

public abstract class AbstractInterruptableJob extends AbstractJobMapAnnotationParams{
    private Thread thread;
    private boolean forceStop;
    private Runnable onInterruptedRunnable;

    @Override
    public final void executeInternal(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        thread = new Thread(() -> {
            try {
                updateJobInfo(jobExecutionContext);
                executeInterruptable(jobExecutionContext);
            } catch (JobExecutionException e) {
                logger.error(e);
                throw new RuntimeException(e);
            }
        });
        thread.start();
        try {
            thread.join();
        } catch (Throwable t) {
            logger.warn("Job " + jobExecutionContext.getJobDetail().getKey().getName() + " interrupted or crashed", t);
            throw new JobExecutionException(t);
        }
    }
    @Override
    public void interrupt() throws UnableToInterruptJobException {
        super.interrupt();
        thread.interrupt();
        if(onInterruptedRunnable != null) {
            onInterruptedRunnable.run();
        }
        if(forceStop) {
            thread.stop();
        }
    }

    /**
     * register custom runnable that is called when the job was interrupted
     * you may implement shutdown methods in this case
     * @param run
     */
    public void onInterrupted(Runnable run) {
        this.onInterruptedRunnable = run;
    }

    public void setForceStop(boolean forceStop) {
        this.forceStop = forceStop;
    }

    public boolean isForceStop() {
        return forceStop;
    }

    protected abstract void executeInterruptable(JobExecutionContext jobExecutionContext) throws JobExecutionException;
}
