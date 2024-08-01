package org.edu_sharing.service.handleservicedoi;

import com.google.gson.Gson;
import com.typesafe.config.Config;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.namespace.QName;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.edu_sharing.alfresco.lightbend.LightbendConfigLoader;
import org.edu_sharing.metadataset.v2.MetadataKey;
import org.edu_sharing.metadataset.v2.MetadataWidget;
import org.edu_sharing.metadataset.v2.tools.MetadataHelper;
import org.edu_sharing.service.handleservice.HandleService;
import org.edu_sharing.service.handleservice.HandleServiceNotConfiguredException;
import org.edu_sharing.service.handleservicedoi.model.DOI;
import org.edu_sharing.service.handleservicedoi.model.Data;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.tools.VCardConverter;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.*;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.Serializable;
import java.util.*;

@Service
public class DOIService implements HandleService {

    public static final String APICITE_PREFIX = "http://api.datacite.org/";

    /**
     * if you want a custom mapping, register this as a bean
     * @Service
     * class DOIProperyMappingCustom implements DOIService.DOIPropertyMapping {
     * You might call getBasicMapping from the DOIService
     */
    interface DOIPropertyMapping {
        DOI getCustomMapping(DOIService doiService, String nodeId, Map<QName, Serializable> properties) throws DOIServiceMissingAttributeException;
    }

    Logger logger = Logger.getLogger(DOIService.class);

    //https://api.test.datacite.org/
    String baseUrl = "https://api.datacite.org";
    String accountId;
    String prefix;
    String password;
    boolean enabled = false;

    private final Optional<DOIPropertyMapping> customMapping;

    RestTemplate template = new RestTemplate(new HttpComponentsClientHttpRequestFactory());

    public DOIService(Optional<DOIPropertyMapping> customMapping) throws HandleServiceNotConfiguredException {
        this.customMapping = customMapping;

        Config config = LightbendConfigLoader.get().getConfig("repository.doiservice");
        enabled = config.getBoolean("enabled");
        if(enabled) {
            baseUrl = config.getString("baseUrl");
            accountId = config.getString("accountId");
            prefix = config.getString("prefix");
            password = config.getString("password");
        }
    }

    @Override
    public boolean enabled() {
        return enabled;
    }

    @Override
    public boolean available() {
        HttpHeaders headers = new HttpHeaders();
        headers.put("Accept", Arrays.asList("text/plain"));
        HttpEntity<DOI> entity = new HttpEntity<>(headers);
        ResponseEntity<String> exchange = template.exchange(baseUrl + "/heartbeat", HttpMethod.GET, entity, String.class);
        logger.debug("heartbeat check response: " + exchange.getBody());
        return exchange.getStatusCode() == HttpStatusCode.valueOf(200) ? true : false;
    }

    public DOI getDOI(String id){

        HttpEntity<DOI> entity = new HttpEntity<>(getHttpHeaders());
        ResponseEntity<DOI> result = template.exchange(baseUrl+"/dois/" + id, HttpMethod.GET, entity, DOI.class);

        return result.getBody();
    }

    @Override
    public String create(String handleId, String nodeId, Map<QName, Serializable> properties) throws Exception {
        return update(handleId, nodeId,properties);
    }


    @Override
    public String update(String handleId, String nodeId, Map<QName, Serializable> properties) throws Exception {
        logger.debug("Updating DOI: " + handleId);
        DOI doi = mapForPublishing(nodeId, properties);
        HttpEntity<DOI> entity = new HttpEntity<>(doi,getHttpHeaders());
        ResponseEntity<DOI> doiResponseEntity = template.exchange(baseUrl+"/dois/"+handleId, HttpMethod.PUT, entity, DOI.class);
        try {
            logger.info("Create doi:" + new Gson().toJson(doi));
        }catch(Throwable ignored) {}
        if(!doiResponseEntity.getStatusCode().is2xxSuccessful()){
            throw new DOIServiceException("update id:" + handleId + " failed. api returned: " + doiResponseEntity.getStatusCode());
        }
        return doiResponseEntity.getBody().getData().getId();
    }

    @Override
    public String delete(String handleId, String nodeId) throws Exception {
        //only works for drafts
        HttpEntity<Void> entity = new HttpEntity<>(getHttpHeaders());
        ResponseEntity<Void> doiResponseEntity = template.exchange(baseUrl+"/dois/"+handleId, HttpMethod.DELETE, entity,Void.class);
        if(!doiResponseEntity.getStatusCode().is2xxSuccessful()){
            throw new DOIServiceException("delete id:" + handleId + " failed. api returned: " + doiResponseEntity.getStatusCode());
        }
        return handleId;
    }



    @Override
    public String generateId() throws Exception {
        DOI doi = DOI.builder()
                .data(Data.builder()
                        .type("dois")
                        .attributes(Data.Attributes.builder()
                                .prefix(prefix).build())
                        .build())
                .build();

        HttpEntity<DOI> entity = new HttpEntity<>(doi,getHttpHeaders());

        ResponseEntity<DOI> doiResponseEntity = template.exchange(baseUrl+"/dois/", HttpMethod.POST, entity, DOI.class);
        if(!doiResponseEntity.getStatusCode().is2xxSuccessful()){
            throw new Exception("generate id failed. api returned: " + doiResponseEntity.getStatusCode());
        }
        logger.debug("generated DOI: " + doiResponseEntity.getBody().getData().getId());
        return doiResponseEntity.getBody().getData().getId();
    }

    @Override
    public String getHandleIdProperty() {
        return CCConstants.CCM_PROP_PUBLISHED_DOI_ID;
    }

    private DOI mapForPublishing(String nodeId, Map<QName, Serializable> properties) throws Exception {
        if(customMapping.isPresent()) {
            return customMapping.get().getCustomMapping(this, nodeId, properties);
        } else {
            return getBasicMapping(nodeId, properties, true);
        }
    }

    @NotNull
    public DOI getBasicMapping(String nodeId, Map<QName, Serializable> properties, boolean failOnMissing) throws Exception {
        DOI doi = DOI.builder()
                .data(Data.builder()
                        .type("dois")
                        .attributes(Data.Attributes.builder()
                                .creators(new ArrayList<>())
                                .titles(new ArrayList<>())
                                .event("publish")
                                .build())
                        .build())
                .build();

        //creator
        //the main researchers involved working on the data, or the authors of the publication in priority order. May be a corporate/institutional or personal name.
        List<String> author = (List<String>) properties.get(QName.createQName(CCConstants.CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_AUTHOR));
        List<String> authorFreetext = (List<String>) properties.get(QName.createQName(CCConstants.CCM_PROP_AUTHOR_FREETEXT));
        if(author == null || author.isEmpty()){
            if((authorFreetext == null || authorFreetext.isEmpty()) && failOnMissing){
                throw new DOIServiceMissingAttributeException(CCConstants.getValidLocalName(CCConstants.CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_AUTHOR),"Creator");
            }
        }
        if(author != null && !author.isEmpty()) {
            author.stream().forEach(a -> doi.getData().getAttributes().getCreators()
                    .add(Data.Creator.builder().name(VCardConverter.getNameForVCardString(a)).build()));
        }
        if(authorFreetext != null && !authorFreetext.isEmpty()){
            authorFreetext.stream().forEach(a -> doi.getData().getAttributes().getCreators()
                    .add(Data.Creator.builder().name(a).build()));
        }


        //title
        String title = (String) properties.get(QName.createQName(CCConstants.LOM_PROP_GENERAL_TITLE));

        if(StringUtils.isEmpty(title) && failOnMissing){
            throw new DOIServiceMissingAttributeException(CCConstants.getValidLocalName(CCConstants.LOM_PROP_GENERAL_TITLE),"Title");
        }
        doi.getData().getAttributes().getTitles().add(Data.Title.builder().title(title).build());

        //publisher
        List<String> publisherList = (List<String>) properties.get(QName.createQName(CCConstants.CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_PUBLISHER));
        String publisher;
        if((publisherList == null || publisherList.isEmpty())){
            if(failOnMissing) {
                throw new DOIServiceMissingAttributeException(CCConstants.getValidLocalName(CCConstants.CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_PUBLISHER), "Publisher");
            }
        } else {
            publisher = publisherList.get(0);
            doi.getData().getAttributes().setPublisher(VCardConverter.getNameForVCardString(publisher));
        }

        //published year
        Date d = (Date) properties.get(QName.createQName(CCConstants.CCM_PROP_PUBLISHED_DATE));
        if(d == null){
            if(failOnMissing) {
                throw new DOIServiceMissingAttributeException(CCConstants.getValidLocalName(CCConstants.CCM_PROP_PUBLISHED_DATE), "PublicationYear");
            }
        } else {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(d);
            doi.getData().getAttributes().setPublicationYear(calendar.get(Calendar.YEAR));
        }


        //learning resourcetype
        List<String> lrts = (List<String>) properties.get(QName.createQName(CCConstants.CCM_PROP_IO_REPL_EDUCATIONAL_LEARNINGRESSOURCETYPE));
        Data.Types lrt;
        if(lrts == null || lrts.isEmpty()){
            lrt = Data.Types.builder().resourceTypeGeneral("Other").build();
            // throw new DOIServiceMissingAttributeException(CCConstants.getValidLocalName(CCConstants.CCM_PROP_IO_REPL_EDUCATIONAL_LEARNINGRESSOURCETYPE), "resourceTypeGeneral");
        } else {
            String mapping = getMapping(nodeId,CCConstants.getValidLocalName(CCConstants.CCM_PROP_IO_REPL_EDUCATIONAL_LEARNINGRESSOURCETYPE),lrts);
            if(mapping == null) mapping = "Other";
            lrt = Data.Types.builder().resourceTypeGeneral(mapping).build();
        }
        doi.getData().getAttributes().setTypes(lrt);


        //url
        doi.getData().getAttributes().setUrl(getContentLink(properties));
        return doi;
    }

    private HttpHeaders getHttpHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.put("Authorization",Arrays.asList(DOIService.getBasicAuthenticationHeader(accountId,password)));
        headers.put("Content-Type", Arrays.asList("application/vnd.api+json"));
        headers.put("Accept", Arrays.asList("*/*"));
        return headers;
    }

    public static final String getBasicAuthenticationHeader(String username, String password) {
        String valueToEncode = username + ":" + password;
        return "Basic " + Base64.getEncoder().encodeToString(valueToEncode.getBytes());
    }

    public String getMapping(String nodeId, String mdsWidgetId, List<String> key){
        try {
            MetadataWidget widget = MetadataHelper.getMetadataset(new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, nodeId)).findWidget(mdsWidgetId);
            Map<String, Collection<MetadataKey.MetadataKeyRelated>> valuespaceMappingByRelation = widget.getValuespaceMappingByRelation(MetadataKey.MetadataKeyRelated.Relation.relatedMatch);
            // try to find any key with relation and map it
            for (String k : key) {
                Optional<MetadataKey.MetadataKeyRelated> metadataKeyRelates = valuespaceMappingByRelation.get(k).stream().filter(r -> r.getKey().startsWith(APICITE_PREFIX)).findFirst();
                if(metadataKeyRelates.isPresent()){
                    return metadataKeyRelates.get().getKey().substring(APICITE_PREFIX.length());
                }
            }

        }catch (Exception e){
            logger.warn(e.getMessage(),e);
        }
        return null;
    }

}
