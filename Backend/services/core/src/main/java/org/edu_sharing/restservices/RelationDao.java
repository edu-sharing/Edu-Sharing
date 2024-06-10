package org.edu_sharing.restservices;

import org.apache.log4j.Logger;
import org.edu_sharing.restservices.shared.NodeRelation;
import org.edu_sharing.restservices.shared.RelationData;
import org.edu_sharing.restservices.shared.User;
import org.edu_sharing.service.authority.AuthorityService;
import org.edu_sharing.service.authority.AuthorityServiceFactory;
import org.edu_sharing.service.relations.InputRelationType;
import org.edu_sharing.service.relations.RelationService;
import org.edu_sharing.service.relations.RelationServiceFactory;

import java.util.Arrays;

public class RelationDao {
    private static Logger logger = Logger.getLogger(NodeDao.class);
    private final AuthorityService authorityService;
    private final RelationService relationService;
    private final RepositoryDao repoDao;

    public RelationDao(RepositoryDao repoDao) {
        this.repoDao = repoDao;
        this.relationService = RelationServiceFactory.getRelationService(repoDao.getId());
        this.authorityService = AuthorityServiceFactory.getAuthorityService(repoDao.getId());
    }

    public void createRelation(String sourceNodeId, String targetNodeId, InputRelationType relationType) throws DAOException {
        try {
            this.relationService.createRelation(sourceNodeId, targetNodeId, relationType);
        } catch (Exception e) {
            throw DAOException.mapping(e);
        }
    }

    public void deleteRelation(String sourceNodeId, String targetNodeId, InputRelationType inputRelationType) throws DAOException {
        try {
            this.relationService.deleteRelation(sourceNodeId, targetNodeId, inputRelationType);
        } catch (Exception e) {
            throw DAOException.mapping(e);
        }
    }

    public NodeRelation getRelations(String sourceNodeId) throws DAOException {
        try {
            org.edu_sharing.service.relations.NodeRelation nodeRelation = this.relationService.getRelations(sourceNodeId);
            NodeRelation.NodeRelationBuilder nodeRelationBuilder = NodeRelation.builder();

            // TODO we can delete this cause it's unused by the frontend
            //nodeRelationBuilder.node(NodeDao.getAnyExistingNode(repoDao, Arrays.asList(NodeDao.ExistingMode.IfNotExists, NodeDao.ExistingMode.IfNoReadPermissions), nodeRelation.getNode()).asNode());
            for (org.edu_sharing.service.relations.RelationData relationData : nodeRelation.getRelations()) {
                try {
                    nodeRelationBuilder.relation(
                            RelationData.builder()
                                    // use getAnyExistingNode in case the original id it refers to has been deleted
                                    .node(NodeDao.getAnyExistingNode(repoDao, Arrays.asList(NodeDao.ExistingMode.IfNotExists, NodeDao.ExistingMode.IfNoReadPermissions), relationData.getNode()).asNode())
                                    .creator(new User(authorityService.getUser(relationData.getCreator())))
                                    .timestamp(relationData.getTimestamp())
                                    .type(relationData.getType())
                                    .build());
                } catch (DAOException e) {
                    logger.warn("Relation node not found and no published id is available for " + relationData.getNode() + ": " + e.getMessage());
                    // don't add relations if the related node doesn't exist
                }
            }
            return nodeRelationBuilder.build();
        } catch (Exception e) {
            throw DAOException.mapping(e);
        }
    }
}
