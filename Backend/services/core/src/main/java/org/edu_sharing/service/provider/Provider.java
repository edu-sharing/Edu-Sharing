package org.edu_sharing.service.provider;

import org.apache.commons.lang.NotImplementedException;
import org.edu_sharing.repository.server.AuthenticationTool;
import org.edu_sharing.repository.server.AuthenticationToolAPI;
import org.edu_sharing.repository.server.MCAlfrescoAPIClient;
import org.edu_sharing.repository.server.MCBaseClient;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.service.collection.CollectionService;
import org.edu_sharing.service.collection.CollectionServiceImpl;
import org.edu_sharing.service.comment.CommentService;
import org.edu_sharing.service.comment.CommentServiceAdapter;
import org.edu_sharing.service.feedback.FeedbackService;
import org.edu_sharing.service.nodeservice.NodeService;
import org.edu_sharing.service.nodeservice.NodeServiceAdapter;
import org.edu_sharing.service.permission.PermissionService;
import org.edu_sharing.service.permission.PermissionServiceReadOnly;
import org.edu_sharing.service.rating.RatingService;
import org.edu_sharing.service.rating.RatingServiceAdapter;
import org.edu_sharing.service.relations.RelationService;
import org.edu_sharing.service.relations.RelationServiceAdadapter;
import org.edu_sharing.service.rendering.RenderingService;
import org.edu_sharing.service.rendering.RenderingServiceFactory;
import org.edu_sharing.service.rendering.RenderingServiceImpl;
import org.edu_sharing.service.search.SearchService;
import org.edu_sharing.spring.ApplicationContextFactory;

import java.util.HashMap;
import java.util.Map;

public abstract class Provider {
    protected final String appId;

    public Provider(String appId){
        this.appId = appId;
    }

    public NodeService getNodeService(){
        return new NodeServiceAdapter(appId);
    }
    public RatingService getRatingService(){
        return new RatingServiceAdapter(appId);
    }
    public RelationService getRelationService(){
        return new RelationServiceAdadapter(appId);
    }
    public CommentService getCommentService(){
        return new CommentServiceAdapter(appId);
    }
    public PermissionService getPermissionService(){
        return new PermissionServiceReadOnly(appId);
    }
    public AuthenticationTool getAuthenticationTool(){
        return new AuthenticationToolAPI(appId);
    }
    public RenderingService getRenderingService(){
        RenderingService service = (RenderingService) ApplicationContextFactory.getApplicationContext().getBean("renderingService");
        service.setAppId(appId);
        return service;
    }
    public CollectionService getCollectionService(){
        return CollectionServiceImpl.build(appId);
    }

    /**
     * @Deprecated
     */
    public MCBaseClient getApiClient(Map<String, String> auth){
        return new MCAlfrescoAPIClient(appId, auth);
    }
    public abstract SearchService getSearchService();

    public FeedbackService getFeedbackService() {
        throw new NotImplementedException("remote FeedbackService not implemented");
    }
}
