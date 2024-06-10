package org.edu_sharing.service.rating;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.namespace.QName;
import org.apache.commons.lang.NotImplementedException;
import org.apache.log4j.Logger;
import org.edu_sharing.alfresco.policy.GuestCagePolicy;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.repository.server.tools.cache.EduSharingRatingCache;
import org.edu_sharing.service.InsufficientPermissionException;
import org.edu_sharing.service.authority.AuthorityService;
import org.edu_sharing.service.authority.AuthorityServiceFactory;
import org.edu_sharing.service.model.NodeRefImpl;
import org.edu_sharing.service.nodeservice.NodeService;
import org.edu_sharing.service.nodeservice.NodeServiceFactory;
import org.edu_sharing.service.nodeservice.NodeServiceHelper;
import org.edu_sharing.service.notification.NotificationService;
import org.edu_sharing.service.notification.NotificationServiceFactoryUtility;
import org.edu_sharing.service.notification.Status;
import org.edu_sharing.service.permission.PermissionService;
import org.edu_sharing.service.permission.PermissionServiceFactory;
import org.edu_sharing.service.toolpermission.ToolPermissionHelper;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.ApplicationContext;

import java.lang.reflect.Array;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Deprecated, use the new mongo service instead!
 */
@Deprecated
public class RatingServiceImpl extends RatingServiceAdapter {

    private final Logger logger = Logger.getLogger(RatingServiceImpl.class);

    ApplicationContext alfApplicationContext = AlfAppContextGate.getApplicationContext();
    ServiceRegistry serviceRegistry = (ServiceRegistry) alfApplicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);
    org.alfresco.service.cmr.repository.NodeService nodeServiceAlfresco = serviceRegistry.getNodeService();
    private PermissionService permissionService;
    private AuthorityService authorityService;
    private NodeService nodeService;
    private NotificationService notificationService;

    public RatingServiceImpl() {
        super(ApplicationInfoList.getHomeRepository().getAppId());
    }

    public void init() {
        this.nodeService = NodeServiceFactory.getLocalService();
        this.authorityService = AuthorityServiceFactory.getLocalService();
        this.permissionService = PermissionServiceFactory.getLocalService();
        this.notificationService = NotificationServiceFactoryUtility.getLocalService();
    }

    @Override
    public void addOrUpdateRating(String nodeId, Double rating, String text) throws Exception {
        checkPreconditions(nodeId);
        AuthenticationUtil.runAsSystem(() -> {
            Rating currentRating = getRatingForUser(nodeId);
            Map<String, Object> props = new HashMap<>();
            props.put(CCConstants.CCM_PROP_RATING_VALUE, rating);
            props.put(CCConstants.CCM_PROP_RATING_TEXT, text);
            if (currentRating == null) {
                nodeService.createNodeBasic(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, nodeId, CCConstants.CCM_TYPE_RATING, CCConstants.CCM_ASSOC_RATING, props);
            } else {
                nodeService.updateNodeNative(currentRating.getRef().getId(), props);
            }
            invalidateCache(nodeId);


            String nodeType = null;
            List<String> nodeAspects;
            Map<String, Object> nodeProps;
            try {
                nodeType = nodeService.getType(StoreRef.PROTOCOL_WORKSPACE, StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier(), nodeId);
                nodeAspects = Arrays.asList(nodeService.getAspects(StoreRef.PROTOCOL_WORKSPACE, StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier(), nodeId));
                nodeProps = nodeService.getProperties(StoreRef.PROTOCOL_WORKSPACE, StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier(), nodeId);
            } catch (Throwable e) {
                nodeAspects = new ArrayList<>();
                nodeProps = new HashMap<>();
            }
            RatingDetails accumulatedRatings = getAccumulatedRatings(new NodeRefImpl(nodeId), null);
            notificationService.notifyRatingChanged(nodeId, nodeType, nodeAspects, nodeProps, rating, accumulatedRatings, Status.ADDED);
            return null;
        });
    }

    private void invalidateCache(String nodeId) {
        EduSharingRatingCache.delete(new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, nodeId));
    }

    @Override
    public List<Rating> getRatings(String nodeId, Date after) {
        return this.nodeService.getChildrenChildAssociationRefType(nodeId, CCConstants.CCM_TYPE_RATING).stream().
                filter((ref) -> {
                    if (after == null)
                        return true;
                    try {
                        Date date = (Date) nodeServiceAlfresco.getProperty(ref.getChildRef(), QName.createQName(CCConstants.CM_PROP_C_MODIFIED));
                        return date.after(after);
                    } catch (Exception e) {
                        logger.warn(e.getMessage(), e);
                        return false;
                    }
                }).
                map((ref) -> {
                    Rating rating = new Rating();
                    rating.setRef(ref.getChildRef());
                    rating.setRating(Double.parseDouble(NodeServiceHelper.getProperty(ref.getChildRef(), CCConstants.CCM_PROP_RATING_VALUE)));
                    rating.setText(NodeServiceHelper.getProperty(ref.getChildRef(), CCConstants.CCM_PROP_RATING_TEXT));
                    rating.setAuthority(NodeServiceHelper.getProperty(ref.getChildRef(), CCConstants.CM_PROP_C_CREATOR));
                    return rating;
                }).collect(Collectors.toList());
    }

    private Rating getRatingForUser(String nodeId) {
        return getRatings(nodeId, null).stream().filter((r) -> r.getAuthority().equals(AuthenticationUtil.getFullyAuthenticatedUser())).findFirst().orElse(null);
    }

    @Override
    public void deleteRating(String nodeId) throws Exception {
        checkPreconditions(nodeId);
        AuthenticationUtil.runAsSystem(() -> {
            Rating rating = getRatingForUser(nodeId);
            if (rating != null) {
                nodeService.removeNode(rating.getRef().getId(), nodeId, false);

                String nodeType = null;
                List<String> nodeAspects;
                Map<String, Object> nodeProps;
                try {
                    nodeType = nodeService.getType(StoreRef.PROTOCOL_WORKSPACE, StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier(), nodeId);
                    nodeAspects = Arrays.asList(nodeService.getAspects(StoreRef.PROTOCOL_WORKSPACE, StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier(), nodeId));
                    nodeProps = nodeService.getProperties(StoreRef.PROTOCOL_WORKSPACE, StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier(), nodeId);
                } catch (Throwable e) {
                    nodeAspects = new ArrayList<>();
                    nodeProps = new HashMap<>();
                }
                RatingDetails accumulatedRatings = getAccumulatedRatings(new NodeRefImpl(nodeId), null);
                notificationService.notifyRatingChanged(nodeId, nodeType,  nodeAspects, nodeProps, rating.getRating(), accumulatedRatings, Status.REMOVED);
            } else {
                throw new IllegalArgumentException("No rating for current user found for the given node");
            }
            invalidateCache(nodeId);
            return null;
        });
    }

    /**
     * Get the accumulated ratings data
     * @param after  the date which the ratings should have at least. Use null (default) to use ratings of all times and also use the cache
     */
    @Override
    public RatingDetails getAccumulatedRatings(org.edu_sharing.service.model.NodeRef nodeRef, Date after){
        String nodeId = nodeRef.getNodeId();
        if (after == null) {
            try {
                RatingsCache accumulated = EduSharingRatingCache.get(new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, nodeId));
                if (accumulated != null) {
                    return cacheToDetail(accumulated);
                }
            } catch (ClassCastException e) {
                // ignore
            } catch (Exception e) {
                logger.warn("Failed to resolve rating cache for node " + nodeId + ": " + e.getMessage());
            }
        }
        List<Rating> ratings = this.getRatings(nodeId, after);
        //@TODO: Duplicated call for getRatings
        Rating userRating = this.getRatingForUser(nodeId);

        RatingsCache accumulated = new RatingsCache();
        accumulated.setOverall(new RatingsCache.RatingData(ratings.stream().map(Rating::getRating).reduce(Double::sum).orElse(0.), ratings.size()));
        accumulated.setUsers(new HashMap<>(ratings.stream().collect(Collectors.toMap(Rating::getAuthority, Rating::getRating))));
        Map<String, RatingsCache.RatingData> affiliation = new HashMap<>();
        // collect counts for each affiliation group
        ratings.forEach((r) -> {
            String authorityAffiliation = (String) authorityService.getAuthorityProperty(r.getAuthority(), CCConstants.CM_PROP_PERSON_EDU_SCHOOL_PRIMARY_AFFILIATION);
            if (authorityAffiliation == null)
                authorityAffiliation = "none";
            RatingsCache.RatingData entry = affiliation.getOrDefault(authorityAffiliation, new RatingsCache.RatingData(0, 0));
            entry.setCount(entry.getCount() + 1);
            entry.setSum(entry.getSum() + r.getRating());
            affiliation.put(authorityAffiliation, entry);
        });
        accumulated.setAffiliation(affiliation);
        if (after == null) {
            EduSharingRatingCache.put(new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, nodeId), accumulated);
        }
        return cacheToDetail(accumulated);
    }

    @Override
    public List<String> getAlteredNodeIds(@NotNull Date after) {
        throw new NotImplementedException();
    }

    @Override
    public List<RatingHistory> getAccumulatedRatingHistory(org.edu_sharing.service.model.NodeRef nodeRef, Date after) {
        throw new NotImplementedException();
    }

    private RatingDetails cacheToDetail(RatingsCache cache) {
        RatingDetails details = new RatingDetails();
        details.setAffiliation(cache.getAffiliation());
        details.setOverall(cache.getOverall());
        if (cache.getUsers() != null && cache.getUsers().containsKey(AuthenticationUtil.getFullyAuthenticatedUser())) {
            details.setUser(cache.getUsers().get(AuthenticationUtil.getFullyAuthenticatedUser()));
        }
        return details;
    }

    private void checkPreconditions(String nodeId) throws Exception {
        if (authorityService.isGuest()) {
            throw new GuestCagePolicy.GuestPermissionDeniedException("guests can not use ratings");
        }
        ToolPermissionHelper.throwIfToolpermissionMissing(CCConstants.CCM_VALUE_TOOLPERMISSION_RATE_WRITE);
        if (!NodeServiceHelper.getType(new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, nodeId)).equals(CCConstants.CCM_TYPE_IO)) {
            throw new IllegalArgumentException("Ratings only supported for nodes of type " + CCConstants.CCM_TYPE_IO);
        }
        List<String> permissions = permissionService.getPermissionsForAuthority(nodeId, AuthenticationUtil.getFullyAuthenticatedUser());
        if (!permissions.contains(CCConstants.PERMISSION_RATE)) {
            throw new InsufficientPermissionException("No permission '" + CCConstants.PERMISSION_RATE + "' to add ratings to node " + nodeId);
        }
    }
}
