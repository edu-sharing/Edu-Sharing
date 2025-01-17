package org.edu_sharing.service.search;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.log4j.Logger;
import org.edu_sharing.repository.server.SearchResultNodeRef;
import org.edu_sharing.alfresco.repository.server.authentication.Context;
import org.edu_sharing.service.search.model.SearchToken;
import org.json.JSONException;
import org.json.JSONObject;

public class SearchLogger {
    static Logger logger = Logger.getLogger(SearchLogger.class);
    public static void logSearch(SearchToken searchToken, SearchResultNodeRef result){
        try {
            if(searchToken.getSearchCriterias() == null) {
                return;
            }
            logger.info(
                    new JSONObject()
                    .put("metadataset",
                            new JSONObject().put("id",searchToken.getSearchCriterias().getMetadataSetId())
                                            .put("query",searchToken.getSearchCriterias().getMetadataSetQuery()))
                    .put("contentType",searchToken.getContentType())
                    .put("criterias",searchToken.getParameters())
                    .put("pagination",
                            new JSONObject().put("offset",searchToken.getFrom())
                                            .put("count",searchToken.getMaxResult()))
                    .put("session", DigestUtils.shaHex(Context.getCurrentInstance().getRequest().getSession().getId()))
                    .put("results",new JSONObject().put("count",result.getNodeCount())
            ).toString());
        } catch (JSONException e) {
            logger.error(e.getMessage(),e);
        }
        catch (Exception e){
            logger.error("Could not track search request: "+e.getMessage());
        }
    }
}
