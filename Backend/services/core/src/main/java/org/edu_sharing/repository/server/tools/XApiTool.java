package org.edu_sharing.repository.server.tools;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.namespace.QName;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.log4j.Logger;
import org.edu_sharing.metadataset.v2.MetadataWidget;
import org.edu_sharing.metadataset.v2.tools.MetadataHelper;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.tools.URLHelper;
import org.edu_sharing.service.authority.AuthorityServiceFactory;
import org.edu_sharing.service.nodeservice.NodeServiceHelper;
import org.edu_sharing.service.tracking.TrackingService;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray;

import java.io.Serializable;
import java.net.URLEncoder;
import java.util.*;
import java.util.stream.Collectors;

public class XApiTool {
    public static final String EDU_SHARING_XAPI_EXTENSION = "http://edu-sharing.net/x-api";
    private static Logger logger = Logger.getLogger(XApiTool.class);

    public static String VERB_OPEN="http://adlnet.gov/expapi/verbs/open";
    public static String VERB_DOWNLOADED="http://adlnet.gov/expapi/verbs/downloaded";

    public static String sendToXApi(String nodeId, JSONObject xApiData) throws Throwable {
        if(AuthorityServiceFactory.getLocalService().isGuest()){
            throw new IllegalArgumentException("user is guest, will not track xApi data");
        }
        JSONObject data = addData(nodeId, xApiData);
        return sendToStore(data);
    }
    public static void trackActivity(String activityVerb,String nodeId,String nodeVersion){
        ApplicationInfo learningLocker = ApplicationInfoList.getLearningLocker();
        if(learningLocker==null)
            return;
        try {
            JSONObject data = getTemplate(nodeId, nodeVersion);
            data.getJSONObject("verb").put("id", activityVerb);
            sendToXApi(nodeId, data);
        }catch(IllegalArgumentException e){
            logger.info(e);
        }catch(Throwable t){
            logger.warn("Tracking via xApi failed",t);
        }
    }

    private static JSONObject getTemplate(String nodeId, String nodeVersion) throws JSONException {
        return new JSONObject()
            .put("actor",new JSONObject())
            .put("verb",new JSONObject())
            .put("object",new JSONObject()
                .put("definition",new JSONObject())
                .put("id", URLHelper.getNgRenderNodeUrl(nodeId,nodeVersion)));
    }
    private static String queryStore(JSONArray query) throws Exception {
        ApplicationInfo learningLocker = ApplicationInfoList.getLearningLocker();
        if(learningLocker==null)
            throw new Exception("No application with type LEARNING_LOCKER registered. Please register a xApi storage first");
        String uuid = UUID.randomUUID().toString();
        String url = learningLocker.getBaseUrl();
        url += "/api/statements/aggregate?pipeline="+ URLEncoder.encode(query.toString());
        HttpGet method = new HttpGet(url);
        return storeHTTP(method);
    }
    private static String sendToStore(JSONObject data) throws Exception {
        ApplicationInfo learningLocker = ApplicationInfoList.getLearningLocker();
        if(learningLocker==null)
            throw new Exception("No application with type LEARNING_LOCKER registered. Please register a xApi storage first");
        String uuid = UUID.randomUUID().toString();
        String url = learningLocker.getBaseUrl();
        url += "/data/xAPI/statements?statementId="+ URLEncoder.encode(uuid);
        HttpPost method=new HttpPost(url);
        method.setEntity(new ByteArrayEntity(data.toString().getBytes()));
        return storeHTTP(method);
    }

    private static String storeHTTP(HttpUriRequest data) throws Exception {
        ApplicationInfo learningLocker = ApplicationInfoList.getLearningLocker();
        Map<String, String> header=new HashMap<>();
        header.put("X-Experience-API-Version","1.0.3");
        header.put("Content-Type","application/json");
        header.put("Authorization","Basic "+learningLocker.getApiKey());
        return new HttpQueryTool().query(null, header, data);
    }

    /**
     *
     * @param authority if null, do not filter by authority and fetch all facets
     * @param fieldName
     * @return
     */
    public static List<String> getFacetsFromStore(String authority, String fieldName, int limit) throws Exception {
        /**
         * [
         *   {
         *     "$match": {
         *       "statement.verb.id": "http://adlnet.gov/expapi/verbs/open",
         *       "statement.actor.account.name": "admin"
         *     }
         *   },
         *   {
         *     "$group": {
         *       "_id": "$statement.actor.account.name",
         * 	  "data": {
         * 		"$addToSet": "$statement.object.definition.extensions.http://edu-sharing&46;net/x-api.<field>"
         *            }
         *     }
         *   }
         * ]
         */
        //@TODO: Limit to a fixed number of last elements to prevent unlimited results for search
        JSONArray array = new JSONArray();
        JSONObject matchWrapper = new JSONObject();
        JSONObject match = new JSONObject();
        JSONObject groupWrapper = new JSONObject();
        JSONObject limitWrapper = new JSONObject();
        JSONObject group = new JSONObject();
        JSONObject groupAggregator = new JSONObject();
        // maybe not required
        match.put("statement.verb.id", VERB_OPEN);
        if(authority!=null)
            match.put("statement.actor.account.name", authority);

        group.put("_id","$statement.actor.account.name");
        groupAggregator.put("$addToSet","$statement.object.definition.extensions."+EDU_SHARING_XAPI_EXTENSION.replace(".","&46;")+"."+fieldName);
        group.put("data", groupAggregator);

        matchWrapper.put("$match",match);
        groupWrapper.put("$group",group);
        array.put(matchWrapper);
        if(limit>0) {
            limitWrapper.put("$limit", limit);
            array.put(limitWrapper);
        }
        array.put(groupWrapper);
        try {
            JSONArray result = new JSONArray(queryStore(array));
            JSONArray data = result.getJSONObject(0).getJSONArray("data");
            List<String> mapped = new ArrayList<>();
            for (int i = 0; i < data.length(); i++) {
                mapped.add(data.getJSONArray(i).getString(0));
            }
            return mapped;
        }catch(Exception e){
            logger.info("Could not fetch facets from xapi store: "+e.getMessage());
            return null;
        }
    }

    private static JSONObject addData(String nodeId, JSONObject xApiData) throws Throwable {
        NodeRef nodeRef = new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, nodeId);
        Map<QName, Serializable> props = NodeServiceHelper.getPropertiesNative(nodeRef);
        //xApiData.getJSONObject("object").getJSONObject("definition").put("name","");
        if(!xApiData.getJSONObject("object").getJSONObject("definition").has("extensions"))
            xApiData.getJSONObject("object").getJSONObject("definition").put("extensions",new JSONObject());
        JSONObject propsData = new JSONObject();
        Collection<MetadataWidget> widgets = MetadataHelper.getWidgetsByNode(nodeRef, true);
        // widgets objects to unique id set
        Set<String> storedProperties = widgets.stream().map((w) -> CCConstants.getValidGlobalName(w.getId())).collect(Collectors.toSet());
        // fixed properties that always should be added
        storedProperties.add(CCConstants.SYS_PROP_NODE_UID);
        props.entrySet().stream().
                filter((entry)->storedProperties.contains(entry.getKey().toString())).
                forEach((entry)->{
                    try {
                        propsData.put(CCConstants.getValidLocalName(entry.getKey().toString()),entry.getValue());
                    } catch (JSONException e) {}
        });
        xApiData.getJSONObject("object").getJSONObject("definition").getJSONObject("extensions").put(EDU_SHARING_XAPI_EXTENSION,propsData);

        // add the user account data
        xApiData.getJSONObject("actor").put("account",new JSONObject()
                .put("name",AuthenticationUtil.getFullyAuthenticatedUser())
                .put("homePage",ApplicationInfoList.getHomeRepository().getClientBaseUrl())
        );
        return xApiData;
    }

    public static String mapActivityVerb(TrackingService.EventType type) {
        switch(type){
            case DOWNLOAD_MATERIAL:
                return VERB_DOWNLOADED;
            case VIEW_MATERIAL:
                return VERB_OPEN;
        }
        return null;
    }
}
