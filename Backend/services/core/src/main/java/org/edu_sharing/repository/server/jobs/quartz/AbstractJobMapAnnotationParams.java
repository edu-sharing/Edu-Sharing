/**
 *
 */
package org.edu_sharing.repository.server.jobs.quartz;

import org.edu_sharing.repository.server.jobs.quartz.annotation.JobFieldDescription;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public abstract class AbstractJobMapAnnotationParams extends AbstractJob {
    @Override
    public final void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        super.execute(jobExecutionContext);
        Class<? extends AbstractJobMapAnnotationParams> clazz = this.getClass();
        for (Field field : clazz.getDeclaredFields()) {
            if (field.isAnnotationPresent(JobFieldDescription.class)) {
                try {
                    field.setAccessible(true);
                    Object value = jobExecutionContext.getJobDetail().getJobDataMap().get(field.getName());
                    if (field.getAnnotation(JobFieldDescription.class).file()) {
                        field.set(this, jobExecutionContext.getJobDetail().getJobDataMap().get(JobHandler.FILE_DATA));
                    } else if (field.getType().isEnum()) {
                        field.set(this, mapEnum(field.getType(), jobExecutionContext.getJobDetail().getJobDataMap().getString(field.getName())));
                    } else if (field.getType().isPrimitive()) {
                        field.set(this, value);
                    } else if (field.getType().isAssignableFrom(Collection.class) || field.getType().equals(List.class)) {
                        if (value != null) {
                            Type abstractType = ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0];
                            if (abstractType instanceof ParameterizedType) {
                                field.set(this,
                                        new ArrayList<>(((Collection<?>) value))
                                );
                            } else {
                                Class<?> type = (Class<?>) abstractType;
                                if (jobExecutionContext.getJobDetail().getJobDataMap().get(field.getName()) != null) {
                                    field.set(this,
                                            ((Collection<?>) jobExecutionContext.getJobDetail().getJobDataMap().get(field.getName())).stream().map(
                                                    (v) -> {
                                                        if (type.isEnum()) {
                                                            return mapEnum(type,
                                                                    (String) v);
                                                        } else {
                                                            return v;
                                                        }
                                                    }
                                            ).collect(Collectors.toList()));
                                }
                            }
                        }
                    } else if (field.getType().equals(Date.class) && value != null) {
                        String formatPattern = "yyyy-MM-dd";
                        SimpleDateFormat dateFormat = new SimpleDateFormat(formatPattern);
                        field.set(this, dateFormat.parse((String) jobExecutionContext.getJobDetail().getJobDataMap().get(field.getName())));
                    } else if (field.getType().getName().equals(Integer.class.getName())) {
                        Object data = jobExecutionContext.getJobDetail().getJobDataMap().get(field.getName());
                        if (data instanceof String) {
                            field.set(this, Integer.parseInt((String) data));
                        } else {
                            field.set(this, data);
                        }
                    } else {
                        field.set(this, jobExecutionContext.getJobDetail().getJobDataMap().get(field.getName()));
                    }
                } catch (IllegalAccessException | ParseException e) {
                    logger.warn("Could not map annotated field " + field.getName());
                }
            }
        }
        this.executeInternal(jobExecutionContext);
    }

    private <T> T mapEnum(Class<T> type, String value) {
        try {
            return (T) type.getMethod("valueOf", String.class).invoke(null,
                    value
            );
        } catch (Exception e) {
            logger.warn(e.getMessage(), e);
            return null;
        }
    }

    protected abstract void executeInternal(JobExecutionContext jobExecutionContext) throws JobExecutionException;
}
