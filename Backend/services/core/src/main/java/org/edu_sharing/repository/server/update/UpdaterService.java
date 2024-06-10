package org.edu_sharing.repository.server.update;

import com.typesafe.config.Optional;
import lombok.Setter;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.alfresco.service.transaction.TransactionService;
import org.apache.commons.lang3.StringUtils;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.update.Protocol;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.AbstractBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

import jakarta.transaction.UserTransaction;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
//@Log4j
@Component
public class UpdaterService implements ApplicationContextAware, ApplicationListener<ContextRefreshedEvent> {

    //TODO Cluster
    private static Set<String> currentlyRunningUpdates = Collections.synchronizedSet(new HashSet<>());

    @Setter
    private ApplicationContext applicationContext;

    private List<UpdateInfo> updateInfoList;
    protected final TransactionService transactionService;
    private final List<UpdateFactory> updateFactories;

    @Autowired
    public UpdaterService(TransactionService transactionService, @Optional List<UpdateFactory> updateFactories) {
        this.transactionService = transactionService;
        this.updateFactories = updateFactories;
    }


    @Value
    private static class RoutineUpdateInfo implements UpdateInfo {
        String beanName;
        UpdateRoutine updateRoutine;
        Method method;
        BeanFactory beanFactory;

        public void execute(boolean test) {
            Object bean = beanFactory.getBean(beanName);
            try {
                if (isTestable()) {
                    method.invoke(bean, test);
                } else {
                    if(test){
                        log.info("this updater has no test method");
                        return;
                    }
                    method.invoke(bean);
                }
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public String getId() {
            return updateRoutine.id();
        }

        @Override
        public String getDescription() {
            return updateRoutine.description();
        }

        @Override
        public boolean isNonTransactional() {
            return updateRoutine.isNonTransactional();
        }

        @Override
        public int getOrder() {
            return updateRoutine.order();
        }

        @Override
        public boolean isAuto() {
            return updateRoutine.auto();
        }

        @Override
        public boolean isTestable() {
            return method.getParameterTypes().length == 1;
        }
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        AbstractBeanFactory beanFactory = (AbstractBeanFactory) applicationContext.getAutowireCapableBeanFactory();

        updateInfoList = Arrays.stream(applicationContext.getBeanDefinitionNames())
                .flatMap(x -> {
                    Method[] methods = java.util.Optional.ofNullable((RootBeanDefinition) beanFactory.getMergedBeanDefinition(x))
                            .map(RootBeanDefinition::getTargetType)
                            .map(Class::getMethods)
                            .orElse(new Method[0]);

                    return Arrays.stream(methods)
                            .filter(y -> y.isAnnotationPresent(UpdateRoutine.class))
                            .map(y -> new RoutineUpdateInfo(x, y.getAnnotation(UpdateRoutine.class), y, beanFactory));
                })
                .sorted(Comparator.comparingInt(RoutineUpdateInfo::getOrder))
                .collect(Collectors.toList());

        updateInfoList.stream()
                .filter(x-> x instanceof RoutineUpdateInfo)
                .map(x->(RoutineUpdateInfo)x)
                .forEach(this::validateUpdateMethodSignature);

        runAutoUpdates();
    }

    private void validateUpdateMethodSignature(RoutineUpdateInfo routineUpdateInfo) {
        Class<?>[] parameterTypes = routineUpdateInfo.method.getParameterTypes();
        if (parameterTypes.length == 0) {
            return;
        }

        if (parameterTypes.length > 1) {
            throw new UpdateSignatureException("Update method " + routineUpdateInfo.getBeanName() + "." + routineUpdateInfo.method.getName() + " can only have a boolean parameter to indicate if the update should run as an test");
        }

        Class<?> parameterType = parameterTypes[0];
        if (parameterType != boolean.class && parameterType != Boolean.class) {
            throw new UpdateSignatureException("Update method " + routineUpdateInfo.getBeanName() + "." + routineUpdateInfo.method.getName() + " can only have a boolean parameter to indicate if the update should run as an test");
        }
    }

    private void runAutoUpdates() {
        ArrayList<UpdateInfo> updateInfos = getAllUpdateInfos();
        for (UpdateInfo x : updateInfos) {
            if (!x.isAuto()) {
                continue;
            }
            try {
                executeUpdate(x, false);
            }catch (Exception ex){
                log.error("Update failed {}:", x.getId(), ex);
            }
        }
    }

    @NotNull
    private ArrayList<UpdateInfo> getAllUpdateInfos() {
        ArrayList<UpdateInfo> updateInfos = new ArrayList<>(updateInfoList);
        updateInfos.addAll(updateFactories.stream().flatMap(x -> x.getUpdates().stream()).collect(Collectors.toList()));
        updateInfos.sort(Comparator.comparingInt(UpdateInfo::getOrder));
        return updateInfos;
    }

    private void executeUpdate(UpdateInfo x, boolean isTestRunner) {
        AbstractBeanFactory beanFactory = (AbstractBeanFactory) applicationContext.getAutowireCapableBeanFactory();
        if (currentlyRunningUpdates.contains(x.getId())) {
            log.error("Update {} is already running. stop processing", x.getId());
            return;
        }

        log.info("Started " + x.getId());
        currentlyRunningUpdates.add(x.getId());
        try {
            Protocol protocol = beanFactory.getBean(Protocol.class);
            Map<String, Object> updateInfo = protocol.getSysUpdateEntry(x.getId());
            if (updateInfo != null) {
                log.info("Update" + x.getId() + " already done at " + updateInfo.get(CCConstants.CCM_PROP_SYSUPDATE_DATE));
                return;
            }

            UserTransaction transaction = transactionService.getNonPropagatingUserTransaction();
            try {
                if (!x.isNonTransactional()) {
                    transaction.begin();
                }

                x.execute(isTestRunner);

                if (!x.isNonTransactional()) {
                    if (isTestRunner) {
                        transaction.rollback();
                    } else {
                        transaction.commit();
                    }
                }

                try {
                    if (!isTestRunner) {
                        protocol.writeSysUpdateEntry(x.getId());
                    }
                } catch (Throwable throwable) {
                    log.error("Error writing protocol entry", throwable);
                }
            } catch (Exception ex) {
                if(!x.isNonTransactional()) {
                    transaction.rollback();
                }
                log.error("Update failed or not completed", ex);
            }

        } catch (Throwable e) {
            throw new RuntimeException(e);
        } finally {
            currentlyRunningUpdates.remove(x.getId());
        }
    }

    public Collection<UpdateInfo> getUpdateInfo() {
        return new ArrayList<>(getAllUpdateInfos());
    }

    public void runUpdate(@NotNull String updateId) {
        if (StringUtils.isBlank(updateId)) {
            throw new IllegalArgumentException("Update id can't be null or empty");
        }

        executeUpdate(getAllUpdateInfos().stream()
                .filter(x -> x.getId().equals(updateId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown update id: " + updateId)), false);
    }

    public void testUpdate(String updateId) {
        if (StringUtils.isBlank(updateId)) {
            throw new IllegalArgumentException("Update id can't be null or empty");
        }

        executeUpdate(getAllUpdateInfos().stream()
                .filter(x -> x.getId().equals(updateId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown update id: " + updateId)), true);
    }


}




