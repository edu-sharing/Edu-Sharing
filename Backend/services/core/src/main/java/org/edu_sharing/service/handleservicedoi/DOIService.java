package org.edu_sharing.service.handleservicedoi;

import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.namespace.QName;
import org.apache.commons.lang3.StringUtils;
import org.edu_sharing.metadataset.v2.MetadataKey;
import org.edu_sharing.metadataset.v2.MetadataWidget;
import org.edu_sharing.metadataset.v2.tools.MetadataHelper;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.tools.VCardConverter;
import org.edu_sharing.service.handleservice.HandleService;
import org.edu_sharing.service.handleservice.HandleServiceNotConfiguredException;
import org.edu_sharing.service.handleservicedoi.model.*;
import org.edu_sharing.service.nodeservice.NodeServiceHelper;
import org.edu_sharing.spring.scope.refresh.RefreshScopeRefreshedEvent;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.event.EventListener;
import org.springframework.http.*;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class DOIService implements HandleService {

    public static final String APICITE_PREFIX = "http://api.datacite.org/";

    /**
     * if you want a custom mapping, register this as a bean
     *
     * @Service class DOIProperyMappingCustom implements DOIService.DOIPropertyMapping {
     * You might call getBasicMapping from the DOIService
     */
    interface DOIPropertyMapping {
        DOI getCustomMapping(DOIService doiService, String nodeId, Map<QName, Serializable> properties) throws DOIServiceMissingAttributeException;
    }


    //https://api.test.datacite.org/


    private final Optional<DOIPropertyMapping> customMapping;
    private final BeanFactory beanFactory;
    private DoiConfig doiConfig;

    RestTemplate template = new RestTemplate(new HttpComponentsClientHttpRequestFactory());

    public DOIService(Optional<DOIPropertyMapping> customMapping, BeanFactory beanFactory) throws HandleServiceNotConfiguredException {
        this.customMapping = customMapping;
        this.beanFactory = beanFactory;
        loadConfig();
    }

    @EventListener
    public void onRefreshScopeRefreshed(RefreshScopeRefreshedEvent event){
        loadConfig();
    }

    private void loadConfig() {
        try {
            DoiConfig doiConfig = beanFactory.getBean(DoiConfig.class);

            if(doiConfig.isEnabled()){
                Objects.requireNonNull(doiConfig.getBaseUrl(), "repository.doiservice.baseUrl not set");
                Objects.requireNonNull(doiConfig.getAccountId(), "repository.doiservice.accountId not set");
                Objects.requireNonNull(doiConfig.getPrefix(), "repository.doiservice.prefix not set");
                Objects.requireNonNull(doiConfig.getPassword(), "repository.doiservice.password not set");
            }
           this.doiConfig = doiConfig;
        }catch (Throwable t){
            log.error("Could not initialize doi service properly cause of error in config, please check config for \"repository.doiservice\"", t);
        }
    }


    @Override
    public boolean enabled() {
        return doiConfig.isEnabled();
    }

    @Override
    public boolean available() {
        HttpHeaders headers = new HttpHeaders();
        headers.put("Accept", List.of("text/plain"));
        HttpEntity<DOI> entity = new HttpEntity<>(headers);
        ResponseEntity<String> exchange = template.exchange(doiConfig.getBaseUrl() + "/heartbeat", HttpMethod.GET, entity, String.class);
        log.debug("heartbeat check response: {}", exchange.getBody());
        return exchange.getStatusCode() == HttpStatusCode.valueOf(200);
    }

    public DOI getDOI(String id) {

        HttpEntity<DOI> entity = new HttpEntity<>(getHttpHeaders());
        ResponseEntity<DOI> result = template.exchange(doiConfig.getBaseUrl() + "/dois/" + id, HttpMethod.GET, entity, DOI.class);

        return result.getBody();
    }

    @Override
    public String create(String handleId, String nodeId, Map<QName, Serializable> properties) throws Exception {
        return update(handleId, nodeId, properties);
    }


    @Override
    public String update(String handleId, String nodeId, Map<QName, Serializable> properties) throws Exception {
        log.debug("Updating DOI: {}", handleId);
        DOI doi = mapForPublishing(nodeId, properties);
        if (doi.getXmlRepresentation() != null) {
            XMLHelper xmlHelper = new XMLHelper();
            xmlHelper.setIdentifier(doi.getXmlRepresentation(), handleId);
            doi.getData().getAttributes().setXml(Base64.getEncoder().encodeToString(xmlHelper.marshal(doi.getXmlRepresentation()).getBytes()));
            doi.setXmlRepresentation(null);
        }
        return updateDOIWithMetadata(handleId, doi);
    }

    private String updateDOIWithMetadata(String handleId, DOI doi) throws DOIServiceException {
        HttpEntity<DOI> entity = new HttpEntity<>(doi, getHttpHeaders());
        doi.getData().getAttributes().setDoi(handleId);
        ResponseEntity<DOI> doiResponseEntity = template.exchange(doiConfig.getBaseUrl() + "/dois/" + handleId, HttpMethod.PUT, entity, DOI.class);
        try {
            log.info("updateDOIWithMetadata:{}", new Gson().toJson(doi));
        } catch (Throwable ignored) {
        }
        if (!doiResponseEntity.getStatusCode().is2xxSuccessful()) {
            throw new DOIServiceException("update id:" + handleId + " failed. api returned: " + doiResponseEntity.getStatusCode());
        }
        return doiResponseEntity.getBody().getData().getId();
    }

    /**
     * update the state of the given node
     * if this node does not have a doi attached, this method will do nothing
     * see https://support.datacite.org/docs/updating-metadata-with-the-rest-api#changing-the-doi-state
     */
    @Override
    public boolean updateState(String nodeId, String eventState) throws Exception {
        String handleId = (String) NodeServiceHelper.getPropertyNative(new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, nodeId), CCConstants.CCM_PROP_PUBLISHED_DOI_ID);
        if (handleId == null) {
            log.info("Node {} has no doi, will not update state", nodeId);
            return false;
        }
        DOI doi = DOI.builder()
                .data(Data.builder()
                        .type("dois")
                        .attributes(Data.Attributes.builder()
                                .event(eventState)
                                .build())
                        .build())
                .build();
        updateDOIWithMetadata(handleId, doi);
        return true;
    }

    @Override
    public String delete(String handleId, String nodeId) throws Exception {
        //only works for drafts
        HttpEntity<Void> entity = new HttpEntity<>(getHttpHeaders());
        ResponseEntity<Void> doiResponseEntity = template.exchange(doiConfig.getBaseUrl() + "/dois/" + handleId, HttpMethod.DELETE, entity, Void.class);
        if (!doiResponseEntity.getStatusCode().is2xxSuccessful()) {
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
                                .prefix(doiConfig.getPrefix()).build())
                        .build())
                .build();

        HttpEntity<DOI> entity = new HttpEntity<>(doi, getHttpHeaders());

        ResponseEntity<DOI> doiResponseEntity = template.exchange(doiConfig.getBaseUrl() + "/dois/", HttpMethod.POST, entity, DOI.class);
        if (!doiResponseEntity.getStatusCode().is2xxSuccessful()) {
            throw new Exception("generate id failed. api returned: " + doiResponseEntity.getStatusCode());
        }
        log.debug("generated DOI: {}", doiResponseEntity.getBody().getData().getId());
        return doiResponseEntity.getBody().getData().getId();
    }

    @Override
    public String getHandleIdProperty() {
        return CCConstants.CCM_PROP_PUBLISHED_DOI_ID;
    }

    private DOI mapForPublishing(String nodeId, Map<QName, Serializable> properties) throws Exception {
        if (customMapping.isPresent()) {
            return customMapping.get().getCustomMapping(this, nodeId, properties);
        } else {
            return getBasicMapping(nodeId, properties, true);
        }
    }

    @NotNull
    public DOI getBasicMapping(String nodeId, Map<QName, Serializable> properties, boolean failOnMissing) throws Exception {
        return getBasicMapping(nodeId, properties, failOnMissing, false);
    }

    public DOI getBasicMapping(String nodeId, Map<QName, Serializable> properties, boolean failOnMissing, boolean xml) throws Exception {

        Helper helper = (xml) ? new XMLHelper() : new JSONHelper();


        List<String> author = (List<String>) properties.get(QName.createQName(CCConstants.CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_AUTHOR));
        List<String> authorFreetext = ((List<String>) properties.getOrDefault(QName.createQName(CCConstants.CCM_PROP_AUTHOR_FREETEXT), (Serializable) Collections.emptyList()))
                .stream()
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toList());

        if (author == null || author.isEmpty()) {
            if (authorFreetext.isEmpty() && failOnMissing) {
                throw new DOIServiceMissingAttributeException(CCConstants.getValidLocalName(CCConstants.CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_AUTHOR), "Creator");
            }
        }
        if (author != null && !author.isEmpty()) {
            author.forEach(a -> {
                helper.addCreator(VCardConverter.getNameForVCardString(a), null);
            });
        }
        if (!authorFreetext.isEmpty()) {
            authorFreetext.forEach(a -> helper.addCreator(a, null));
        }

        //title
        String title = (String) properties.get(QName.createQName(CCConstants.LOM_PROP_GENERAL_TITLE));

        if (StringUtils.isEmpty(title) && failOnMissing) {
            throw new DOIServiceMissingAttributeException(CCConstants.getValidLocalName(CCConstants.LOM_PROP_GENERAL_TITLE), "Title");
        }
        helper.addTitle(title);

        List<String> publisherList = (List<String>) properties.get(QName.createQName(CCConstants.CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_PUBLISHER));
        String publisher;
        if ((publisherList == null || publisherList.isEmpty())) {
            if (failOnMissing) {
                throw new DOIServiceMissingAttributeException(CCConstants.getValidLocalName(CCConstants.CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_PUBLISHER), "Publisher");
            }
        } else {
            publisher = publisherList.get(0);
            helper.setPublisher(VCardConverter.getNameForVCardString(publisher));
        }

        //published year
        Date d = (Date) properties.get(QName.createQName(CCConstants.CCM_PROP_PUBLISHED_DATE));
        if (d == null) {
            if (failOnMissing) {
                throw new DOIServiceMissingAttributeException(CCConstants.getValidLocalName(CCConstants.CCM_PROP_PUBLISHED_DATE), "PublicationYear");
            }
        } else {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(d);
            helper.setPublicationYear(calendar.get(Calendar.YEAR));
        }


        //learning resourcetype
        List<String> lrts = (List<String>) properties.get(QName.createQName(CCConstants.CCM_PROP_IO_REPL_EDUCATIONAL_LEARNINGRESSOURCETYPE));
        String mapping;
        if (lrts == null || lrts.isEmpty()) {
            mapping = "Other";
            // throw new DOIServiceMissingAttributeException(CCConstants.getValidLocalName(CCConstants.CCM_PROP_IO_REPL_EDUCATIONAL_LEARNINGRESSOURCETYPE), "resourceTypeGeneral");
        } else {
            mapping = getMapping(nodeId, CCConstants.getValidLocalName(CCConstants.CCM_PROP_IO_REPL_EDUCATIONAL_LEARNINGRESSOURCETYPE), lrts);
            if (mapping == null) mapping = "Other";

        }
        helper.setResourceType(mapping);

        //url
        DOI doi = helper.getDoi();
        doi.getData().getAttributes().setUrl(getContentLink(properties));
        return doi;
    }

    private HttpHeaders getHttpHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.put("Authorization", List.of(DOIService.getBasicAuthenticationHeader(doiConfig.getAccountId(), doiConfig.getPassword())));
        headers.put("Content-Type", List.of("application/vnd.api+json"));
        headers.put("Accept", List.of("*/*"));
        return headers;
    }

    public static String getBasicAuthenticationHeader(String username, String password) {
        String valueToEncode = username + ":" + password;
        return "Basic " + Base64.getEncoder().encodeToString(valueToEncode.getBytes());
    }

    public String getMapping(String nodeId, String mdsWidgetId, List<String> key) {
        try {
            MetadataWidget widget = MetadataHelper.getMetadataset(new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, nodeId)).findWidget(mdsWidgetId);
            Map<String, Collection<MetadataKey.MetadataKeyRelated>> valuespaceMappingByRelation = widget.getValuespaceMappingByRelation(MetadataKey.MetadataKeyRelated.Relation.relatedMatch);
            // try to find any key with relation and map it
            for (String k : key) {
                Optional<MetadataKey.MetadataKeyRelated> metadataKeyRelates = valuespaceMappingByRelation.get(k).stream().filter(r -> r.getKey().startsWith(APICITE_PREFIX)).findFirst();
                if (metadataKeyRelates.isPresent()) {
                    return metadataKeyRelates.get().getKey().substring(APICITE_PREFIX.length());
                }
            }

        } catch (Exception e) {
            log.warn(e.getMessage(), e);
        }
        return null;
    }

}
