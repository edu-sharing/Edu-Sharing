package org.edu_sharing.service.network;

import com.google.gson.Gson;
import com.typesafe.config.ConfigBeanFactory;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.apache.commons.codec.digest.DigestUtils;
import org.edu_sharing.alfresco.lightbend.LightbendConfigLoader;
import org.edu_sharing.alfresco.service.connector.ConnectorList;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.client.tools.UrlTool;
import org.edu_sharing.repository.server.tools.URLTool;
import org.edu_sharing.repository.server.tools.UserEnvironmentTool;
import org.edu_sharing.service.network.model.Service;
import org.edu_sharing.service.network.model.StoredService;
import org.edu_sharing.service.nodeservice.NodeService;
import org.edu_sharing.service.nodeservice.NodeServiceFactory;

import java.io.FileInputStream;
import java.io.FileReader;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;


public class NetworkServiceImpl implements NetworkService {
    NodeService nodeService;
    public NetworkServiceImpl(){
        nodeService=NodeServiceFactory.getLocalService();
    }
    @Override
    public Collection<StoredService> getServices() throws Throwable{
        return AuthenticationUtil.runAsSystem(()->{
            try{
                List<ChildAssociationRef> services = nodeService.getChildrenChildAssociationRef(
                        new UserEnvironmentTool().getEdu_SharingServiceFolder());
                Collection<StoredService> result=new ArrayList<>();
                result.add(getOwnService());
                for(ChildAssociationRef service : services){
                    Map<String, Object> props = nodeService.getProperties(service.getChildRef().getStoreRef().getProtocol(), service.getChildRef().getStoreRef().getIdentifier(), service.getChildRef().getId());
                    String data= (String) props.get(CCConstants.CCM_PROP_SERVICE_NODE_DATA);
                    StoredService serviceObject = new Gson().fromJson(data, StoredService.class);
                    serviceObject.setId((String) props.get(CCConstants.CM_NAME));
                    result.add(serviceObject);
                }
                return result;
            }
            catch(Throwable t){
                throw new RuntimeException(t);
            }
        });
    }

    @Override
    public StoredService getOwnService() {
        try{
        Service service = new Gson().fromJson(
                new Gson().toJson(LightbendConfigLoader.get().getConfig("repository.serviceDescription").root().unwrapped()),
                Service.class
        );

        service.setUrl(URLTool.getBaseUrl());
        service.setIcon(URLTool.getNgAssetsUrl()+"images/app-icon.svg");
        service.setLogo(URLTool.getNgAssetsUrl()+"images/logo.svg");

        Collection<Service.Interface> interfaces=service.getInterfaces();
        Service.Interface api=new Service.Interface();
        api.setFormat(Service.Interface.Format.Json);
        api.setType(Service.Interface.Type.Generic_Api);
        api.setUrl(URLTool.getRestServiceUrl());
        api.setDocumentation(URLTool.getBaseUrl()+"/swagger/index.html");
        interfaces.add(api);
        Service.Interface statistics=new Service.Interface();
        statistics.setFormat(Service.Interface.Format.Json);
        statistics.setType(Service.Interface.Type.Statistics);
        statistics.setUrl(URLTool.getRestServiceUrl()+"statistic/v1/public");
        interfaces.add(statistics);
        Service.Interface sitemap=new Service.Interface();
        sitemap.setFormat(Service.Interface.Format.XML);
        sitemap.setType(Service.Interface.Type.Sitemap);
        sitemap.setUrl(URLTool.getEduservletUrl()+"sitemap");
        interfaces.add(sitemap);
        service.setInterfaces(interfaces);
        return new StoredService(generateId(service),service);
        }catch(Throwable t){
            t.printStackTrace();
            return null;
        }
    }

    @Override
    public StoredService addService(Service service) throws Throwable {
        return AuthenticationUtil.runAsSystem(()->{
            try{
                String parent=new UserEnvironmentTool().getEdu_SharingServiceFolder();

                String id=generateId(service);
                Map<String, String[]> props = generateProps(id,service);
                nodeService.createNode(parent,CCConstants.CCM_TYPE_SERVICE_NODE,props);
                return new StoredService(id,service);
            }
            catch(Throwable t){
                throw new RuntimeException(t);
            }
        });
    }

    @Override
    public StoredService updateService(String id, Service service) throws Throwable {
        try{
            String parent=new UserEnvironmentTool().getEdu_SharingServiceFolder();
            String node = nodeService.findNodeByName(parent, id);
            Map<String, String[]> props = generateProps(id,service);
            nodeService.updateNode(node,props);
            return new StoredService(id,service);
        }
        catch(Throwable t){
            throw new RuntimeException(t);
        }
    }
    private String generateId(Service service) {
        return DigestUtils.shaHex(service.getUrl());
    }

    private Map<String, String[]> generateProps(String id, Service service) {
        Map<String, String[]> props = new HashMap<>();
        props.put(CCConstants.CM_NAME,new String[]{id});

        String json=new Gson().toJson(service);
        props.put(CCConstants.CCM_PROP_SERVICE_NODE_NAME,new String[]{service.getName()});
        props.put(CCConstants.CCM_PROP_SERVICE_NODE_DESCRIPTION,new String[]{service.getDescription()});
        props.put(CCConstants.CCM_PROP_SERVICE_NODE_TYPE,new String[]{service.getType()});
        props.put(CCConstants.CCM_PROP_SERVICE_NODE_DATA,new String[]{json});
        return props;
    }
}
