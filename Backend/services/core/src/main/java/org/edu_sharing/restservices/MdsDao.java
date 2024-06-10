package org.edu_sharing.restservices;

import java.util.*;
import java.util.stream.Collectors;

import org.alfresco.service.cmr.repository.StoreRef;
import org.edu_sharing.metadataset.v2.*;
import org.edu_sharing.metadataset.v2.tools.MetadataHelper;
import org.edu_sharing.metadataset.v2.tools.MetadataSearchHelper;
import org.edu_sharing.repository.server.RepoFactory;
import org.edu_sharing.restservices.mds.v1.model.*;
import org.edu_sharing.restservices.shared.MdsQueryCriteria;
import org.edu_sharing.restservices.shared.Mds;
import org.edu_sharing.service.nodeservice.NodeService;
import org.edu_sharing.service.nodeservice.NodeServiceFactory;
import org.edu_sharing.service.notification.NotificationService;
import org.edu_sharing.service.notification.NotificationServiceFactoryUtility;
import org.edu_sharing.service.search.Suggestion;

public class MdsDao {

    public static final String DEFAULT = "-default-";
    private final NotificationService notificationService;
    private final NodeService nodeService;

    public static List<MetadataSetInfo> getAllMdsDesc(RepositoryDao repoDao) throws Exception {
        return RepoFactory.getMetadataSetsForRepository(repoDao.getId());
    }

    public Suggestions getSuggestions(String queryId, String parameter, String value, List<MdsQueryCriteria> criterias) throws DAOException {
        Suggestions result = new Suggestions();
        ArrayList<Suggestions.Suggestion> suggestionsResult = new ArrayList<>();
        result.setValues(suggestionsResult);
        try {
            List<? extends Suggestion> suggestions =
                    MetadataSearchHelper.getSuggestions(this.repoDao.getId(), mds, queryId, parameter, value, criterias);

            for (Suggestion suggest : suggestions) {
                Suggestions.Suggestion suggestion = new Suggestions.Suggestion();
                suggestion.setDisplayString(suggest.getDisplayString());
                //suggestion.setReplacementString(suggest.getReplacementString());
                suggestion.setKey(suggest.getKey());
                suggestionsResult.add(suggestion);
            }

        } catch (Exception e) {
            throw DAOException.mapping(e);
        }
        return result;
    }

    public static MdsDao getMds(RepositoryDao repoDao, String mdsId) throws DAOException {

        try {

            MetadataSet mds = MetadataHelper.getMetadataset(repoDao.getApplicationInfo(), mdsId);

            if (mds == null) {
                throw new DAOMissingException(new IllegalArgumentException(mdsId));
            }

            return new MdsDao(repoDao, mds);

        } catch (Throwable t) {
            throw DAOException.mapping(t);
        }

    }

    private final RepositoryDao repoDao;
    private final MetadataSet mds;

    private MdsDao(RepositoryDao repoDao, MetadataSet mds) {
        this.repoDao = repoDao;
        this.mds = mds;
        this.nodeService = NodeServiceFactory.getNodeService(repoDao.getApplicationInfo().getAppId());
        this.notificationService = NotificationServiceFactoryUtility.getLocalService();
    }

    public Mds asMds() {

        Mds data = new Mds();

        data.setName(mds.getName());
        data.setCreate(mds.getCreate() != null ? new Mds.Create(mds.getCreate()) : null);
        data.setWidgets(getWidgets());
        data.setViews(getViews());
        data.setGroups(getGroups());
        data.setLists(getLists());
        data.setSorts(getSorts());

        return data;
    }

    private List<MdsWidget> getWidgets() {
        List<MdsWidget> result = new ArrayList<>();
        for (MetadataWidget type : this.mds.getWidgets()) {
            result.add(new MdsWidget(type));
        }
        return result;
    }

    private List<MdsView> getViews() {
        List<MdsView> result = new ArrayList<>();
        for (MetadataTemplate type : this.mds.getTemplates()) {
            result.add(new MdsView(type));
        }
        return result;
    }

    private List<MdsGroup> getGroups() {
        List<MdsGroup> result = new ArrayList<>();
        for (MetadataGroup type : this.mds.getGroups()) {
            result.add(new MdsGroup(type));
        }
        return result;
    }

    private List<MdsList> getLists() {
        List<MdsList> result = new ArrayList<>();
        for (MetadataList type : this.mds.getLists()) {
            result.add(new MdsList(type));
        }
        return result;
    }

    private List<MdsSort> getSorts() {
        List<MdsSort> result = new ArrayList<>();
        for (MetadataSort type : this.mds.getSorts()) {
            result.add(new MdsSort(type));
        }
        return result;
    }

    public MetadataSet getMds() {
        return mds;
    }

    public MdsValue suggestValue(String widget, String valueCaption, String parent, List<String> nodes) throws DAOException {
        MetadataWidget widgetDefinition = mds.findAllWidgets(widget).stream().filter((w) -> w.getSuggestionReceiver() != null && !w.getSuggestionReceiver().isEmpty()).findFirst()
                .orElseThrow(() -> new DAOValidationException(new IllegalArgumentException("No widget definition found which can receive suggestion data")));


        MdsValue result = new MdsValue();
        result.setId(UUID.randomUUID().toString());
        result.setParent(parent);
        result.setCaption(valueCaption);
        try {
            if (nodes == null) {
                nodes = new ArrayList<>();
            }

            List<String> nodeTypes = nodes.stream()
                    .map(x -> {
                        try {
                            return nodeService.getType(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getProtocol(), StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier(), x);
                        } catch (Throwable e) {
                            return null;
                        }
                    })
                    .collect(Collectors.toList());

            List<Map<String, Object>> nodeProperties = nodes.stream()
                    .map(x -> {
                        try {
                            return nodeService.getProperties(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getProtocol(), StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier(), x);
                        } catch (Throwable e) {
                            return new HashMap<String, Object>();
                        }
                    })
                    .collect(Collectors.toList());

            List<List<String>> nodeAspects = nodes.stream()
                    .map(x -> {
                        try {
                            return Arrays.asList(nodeService.getAspects(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getProtocol(), StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier(), x));
                        } catch (Throwable e) {
                            return new ArrayList<String>();
                        }
                    })
                    .collect(Collectors.toList());

            notificationService.notifyMetadataSetSuggestion(result, widgetDefinition, nodes, nodeTypes, nodeAspects, nodeProperties);
        } catch (Throwable t) {
            throw DAOException.mapping(t);
        }
        return result;
    }
}
