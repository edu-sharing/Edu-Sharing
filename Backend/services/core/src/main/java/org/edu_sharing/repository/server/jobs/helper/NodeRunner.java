package org.edu_sharing.repository.server.jobs.helper;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.alfresco.repo.policy.BehaviourFilter;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.apache.commons.lang3.StringUtils;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.server.importer.OAIPMHLOMImporter;
import org.edu_sharing.repository.server.tools.cache.RepositoryCache;
import org.edu_sharing.service.nodeservice.NodeService;
import org.edu_sharing.service.nodeservice.NodeServiceFactory;
import org.edu_sharing.service.nodeservice.RecurseMode;
import org.springframework.context.ApplicationContext;

import jakarta.transaction.NotSupportedException;
import jakarta.transaction.SystemException;
import jakarta.transaction.UserTransaction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This is a class that will do a task for all nodes with a given type (or all) in a given folder
 */

@Slf4j
public class NodeRunner {

    private final NodeService nodeService = NodeServiceFactory.getLocalService();
    private final ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();
    private final ServiceRegistry serviceRegistry = applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY, ServiceRegistry.class);
    private final BehaviourFilter policyBehaviourFilter = applicationContext.getBean("policyBehaviourFilter", BehaviourFilter.class);
    /**
     * The task that will be called for each node
     */
    @Setter
    @Getter
    private Consumer<NodeRef> task;

    /**
     * A filter (optional) which should return true for all nodes where the task should actually be processed for
     */
    @Setter
    @Getter
    private Predicate<? super NodeRef> filter;

    /**
     * The start folder, defaults to company home
     */
    @Setter
    @Getter
    private String startFolder=nodeService.getCompanyHome();

    @Getter
    @Setter
    private StoreRef startFolderStore = StoreRef.STORE_REF_WORKSPACE_SPACESSTORE;

    /**
     * The types of nodes that should be processed (or null for all)
     */
    @Setter
    @Getter
    private List<String> types;

    /**
     * run as alfresco system user
     */
    @Setter
    @Getter
    private boolean runAsSystem = false;

    /**
     * Threading enabled
     * Note: Currently, only the task is threaded, not previous filter actions
     * The thread pool uses the same Thread Size as the @OAIPMHLOMImporter
     */
    @Setter
    @Getter
    private boolean threaded = true;

    /**
     * Shall the modified date of the edited nodes stay the same
     * (Enabling this will disable the behaviour filters while running)
     * Required transaction to be set to Local or Global
     */
    @Getter
    @Setter
    private boolean keepModifiedDate;

    /**
     * Shall a transaction be spawned for the task
     * This WILL NOT WORK in conjunction with threaded=true
     */
    @Getter
    @Setter
    private TransactionMode transaction=TransactionMode.None;

    /**
     * How to recurse across elements
     * Default will only recurse into sub-folders, but not sub-elements (e.g. childobjects)
     */
    @Setter
    @Getter
    private RecurseMode recurseMode = RecurseMode.Folders;

    /**
     * shall the cache for each processed node be invalidated/cleared?
     */
    @Setter
    @Getter
    private boolean invalidateCache = true;

    @Getter
    @Setter
    private String lucene = null;

    @Getter
    @Setter
    private StoreRef luceneStore = StoreRef.STORE_REF_WORKSPACE_SPACESSTORE;
    /**
     * custom nodes list to iterate over
     */
    @Setter
    @Getter
    private Collection<NodeRef> nodesList;

    public NodeRunner() {
	}

    /**
     * runs the job for each node
     * @return the number of nodes that have been processed (also if they may have been filtered in the filter expression)
     */
    public int run(){
        if(task==null)
            throw new IllegalArgumentException("No task has been set yet");

        UserTransaction userTransaction = null;
        if(transaction.equals(TransactionMode.Global)){
            userTransaction = serviceRegistry.getTransactionService().getNonPropagatingUserTransaction();
            try {
                userTransaction.begin();
            } catch (NotSupportedException|SystemException e) {
                log.error(e.getMessage(), e);
            }
        }
        try {
            List<NodeRef> nodes;

            if(nodesList != null) {
                nodes = new ArrayList<>(nodesList);
            } else if(StringUtils.isBlank(lucene)) {
                if (runAsSystem)
                    nodes = AuthenticationUtil.runAsSystem(() -> nodeService.getChildrenRecursive(startFolderStore, startFolder, types, recurseMode));
                else
                    nodes = nodeService.getChildrenRecursive(startFolderStore, startFolder, types, recurseMode);
            }else{
                log.info("collection nodes by lucene: {}", lucene);
                if (runAsSystem)
                    nodes = AuthenticationUtil.runAsSystem(() -> new NodeCollectorLucene(lucene, luceneStore).getNodes());
                else
                    nodes = new NodeCollectorLucene(lucene, luceneStore).getNodes();
            }
            Predicate<? super NodeRef> callFilter = (ref) -> {
                if (filter == null)
                    return true;
                if (runAsSystem) {
                    return AuthenticationUtil.runAsSystem(() -> filter.test(ref));
                } else {
                    return filter.test(ref);
                }
            };
            Consumer<? super NodeRef> callTask = (ref) -> {
                UserTransaction userTransactionLocal = null;
                if(transaction.equals(TransactionMode.Local)){
                    userTransactionLocal = serviceRegistry.getTransactionService().getNonPropagatingUserTransaction();
                    try {
                        userTransactionLocal.begin();
                    } catch (NotSupportedException|SystemException e) {
                        log.error(e.getMessage(), e);
                    }
                }

                if(transaction.equals(TransactionMode.LocalRetrying)){
                    serviceRegistry.getRetryingTransactionHelper().doInTransaction(() -> {
                        runTask(ref);
                        return null;
                    });
                }
                else {
                    runTask(ref);
                }
                if(transaction.equals(TransactionMode.Local) && userTransactionLocal != null) {
                    try {
                        userTransactionLocal.commit();
                    } catch (Exception e) {
                        log.error(e.getMessage(),e);
                        try {
                            userTransactionLocal.rollback();
                        } catch (SystemException e1) {
                            log.error(e1.getMessage(),e1);
                        }
                    }
                }
            };

            log.info("before distinct: {}", nodes.size());
            nodes = nodes.stream().distinct().collect(Collectors.toList());
            log.info("after distinct: {}", nodes.size());

            Stream<NodeRef> filteredStream = nodes.stream().filter(callFilter);
            if (threaded) {
                ExecutorService executor = Executors.newFixedThreadPool(OAIPMHLOMImporter.getThreadCount());
                List<Callable<Void>> threads=new ArrayList<>();
                filteredStream.forEach((ref)->{
                    threads.add(()-> {
                        callTask.accept(ref);
                        return null;
                    });
                });
                try {
                    executor.invokeAll(threads);
                    executor.shutdown();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            else
                filteredStream.forEach(callTask);
            return nodes.size();
        }
        finally{
            if(transaction.equals(TransactionMode.Global) && userTransaction != null){
                try {
                    userTransaction.commit();
                } catch (Exception e) {
                    log.error(e.getMessage(),e);
                    try {
                        userTransaction.rollback();
                    } catch (SystemException e1) {
                        log.error(e1.getMessage(),e1);
                    }
                }
            }

        }
    }

    private void runTask(NodeRef ref) {
        if(ref == null){
            log.error("nodeRef is null");
            return;
        }
        try {
            if (keepModifiedDate)
                policyBehaviourFilter.disableBehaviour(ref);
            if (runAsSystem) {
                AuthenticationUtil.runAsSystem(() -> {
                    task.accept(ref);
                    new RepositoryCache().remove(ref.getId());
                    return null;
                });
            } else {
                task.accept(ref);
                new RepositoryCache().remove(ref.getId());
            }
        }finally {
            if(keepModifiedDate || !transaction.equals(TransactionMode.None)) policyBehaviourFilter.enableBehaviour(ref);
        }
    }


    public enum TransactionMode{
        None, // no transactions
        Global, // for whole task
        Local, // per node
        LocalRetrying // per node, with retrying transaction
    }
}
