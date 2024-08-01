package org.edu_sharing.repository.server.sitemap;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Marshaller;

import org.apache.log4j.Logger;
import org.edu_sharing.metadataset.v2.MetadataReader;
import org.edu_sharing.metadataset.v2.MetadataSet;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.MCAlfrescoAPIClient;
import org.edu_sharing.repository.server.SearchResultNodeRef;
import org.edu_sharing.repository.server.sitemap.xml.Sitemapindex;
import org.edu_sharing.repository.server.sitemap.xml.Urlset;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.repository.server.tools.URLTool;
import org.edu_sharing.repository.tools.URLHelper;
import org.edu_sharing.service.mime.MimeTypesV2;
import org.edu_sharing.service.nodeservice.NodeService;
import org.edu_sharing.service.nodeservice.NodeServiceFactory;
import org.edu_sharing.service.nodeservice.NodeServiceHelper;
import org.edu_sharing.service.search.SearchService;
import org.edu_sharing.service.search.SearchServiceFactory;
import org.edu_sharing.service.search.model.SearchToken;
import org.edu_sharing.service.search.model.SortDefinition;


public class SitemapServlet extends HttpServlet{
    public final static String NS_SITEMAP="http://www.sitemaps.org/schemas/sitemap/0.9";
    public final static SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
    private static Logger logger = Logger.getLogger(SitemapServlet.class);
    private static final int NODES_PER_MAP = 500;
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        try {
            String from = req.getParameter("from");
            String type = req.getParameter("type");

            if(from==null){
                Sitemapindex index = getAll(req);
                toXml(index,resp);
            }
            else{
                Urlset set = getNodes(req,type,Integer.parseInt(from));
                toXml(set,resp);
            }
        }
        catch(Throwable t){
            t.printStackTrace();
            resp.sendError(500,t.toString());
        }
    }

    private Sitemapindex getAll(HttpServletRequest request) throws Throwable {
        Sitemapindex index = new Sitemapindex();
        SearchService search = SearchServiceFactory.getLocalService();

        SearchToken token=new SearchToken();
        token.setContentType(SearchService.ContentType.FILES);
        token.setMaxResult(0);
        token.setExcludes(Arrays.asList(new String[]{"preview"}));
        SearchResultNodeRef resultFiles = search.search(getMds(request), MetadataSet.DEFAULT_CLIENT_QUERY, getSearchAllCriterias(), token);
        token.setContentType(SearchService.ContentType.COLLECTIONS);
        SearchResultNodeRef resultCollections = search.search(getMds(request), MetadataSet.DEFAULT_CLIENT_QUERY, getSearchAllCriterias(), token);
        for(int i=0;i<resultFiles.getNodeCount();i+=NODES_PER_MAP){
            Sitemapindex.Sitemap map = new Sitemapindex.Sitemap();
            map.loc=request.getRequestURL()+"?type=io&from="+i;
            index.sitemap.add(map);
        }
        for(int i=0;i<resultCollections.getNodeCount();i+=NODES_PER_MAP){
            Sitemapindex.Sitemap map = new Sitemapindex.Sitemap();
            map.loc=request.getRequestURL()+"?type=collection&from="+i;
            index.sitemap.add(map);
        }
        return index;
    }

    private void toXml(Object obj, HttpServletResponse resp) throws Exception {
        JAXBContext jaxbContext = JAXBContext.newInstance(obj.getClass());
        Marshaller jaxbMarshaller = jaxbContext.createMarshaller();

        resp.setContentType("application/xml");
        // output pretty printed
        jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        jaxbMarshaller.setProperty(Marshaller.JAXB_SCHEMA_LOCATION, NS_SITEMAP);
        jaxbMarshaller.setProperty(Marshaller.JAXB_FRAGMENT,false);
        jaxbMarshaller.marshal(obj, resp.getOutputStream());
    }

    private Urlset getNodes(HttpServletRequest request, String type, int from) throws Throwable {
        Urlset set = new Urlset();
        SearchService search = SearchServiceFactory.getLocalService();
        NodeService nodeService = NodeServiceFactory.getLocalService();

        SearchToken token=new SearchToken();
        if("collection".equals(type))
            token.setContentType(SearchService.ContentType.COLLECTIONS);
        else
            token.setContentType(SearchService.ContentType.FILES);
        token.setMaxResult(NODES_PER_MAP);
        token.setFrom(from);
        token.setExcludes(Arrays.asList(new String[]{"preview"}));
        SortDefinition sort = new SortDefinition();
        sort.addSortDefinitionEntry(new SortDefinition.SortDefinitionEntry(CCConstants.getValidLocalName(CCConstants.CM_PROP_C_CREATED),true));
        token.setSortDefinition(sort);
        SearchResultNodeRef result = search.search(getMds(request), MetadataSet.DEFAULT_CLIENT_QUERY, getSearchAllCriterias(), token);
        for(org.edu_sharing.service.model.NodeRef ref : result.getData()){
            logger.info("node:"+ref.getNodeId() +" "+ref.getStoreId());
            Urlset.Url url=new Urlset.Url();
            String[] aspects = nodeService.getAspects(ref.getStoreProtocol(),ref.getStoreId(),ref.getNodeId());
            Date property = (ref.getProperties() != null)
                    ? new Date((long)ref.getProperties().get(CCConstants.CM_PROP_C_MODIFIED))
                    : (Date) nodeService.getPropertyNative(ref.getStoreProtocol(), ref.getStoreId(), ref.getNodeId(), CCConstants.CM_PROP_C_MODIFIED);
            url.lastmod = DATE_FORMAT.format(property);
            if(Arrays.asList(aspects).contains(CCConstants.CCM_ASPECT_COLLECTION)){
                url.loc=URLTool.getNgCollectionUrl(ref.getNodeId());
            }
            else {
                url.loc = URLHelper.getNgRenderNodeUrl(ref.getNodeId(), null);
            }
            String mimetype=nodeService.getContentMimetype(ref.getStoreProtocol(),ref.getStoreId(),ref.getNodeId());
            if(MimeTypesV2.getTypeFromMimetype(mimetype).equals("file-video")){
                Urlset.Url.Video video = new Urlset.Url.Video();
                video.thumbnail_loc = NodeServiceHelper.getPreview(ref).getUrl();
                try {
                    video.content_loc = new MCAlfrescoAPIClient().getDownloadUrl(ref.getNodeId());
                }catch(Throwable t){
                    logger.warn("Can not read download url: "+t.getMessage());
                }                video.title = nodeService.getProperty(ref.getStoreProtocol(),ref.getStoreId(),ref.getNodeId(),CCConstants.CM_NAME);
                url.video.add(video);
            }
            else {
                Urlset.Url.Image image = new Urlset.Url.Image();
                image.loc = NodeServiceHelper.getPreview(ref).getUrl();
                //url.image.add(image);
            }
            //getPreviewServletUrl
            set.url.add(url);
        }
        return set;
    }

    private Map<String, String[]> getSearchAllCriterias() {
        Map<String, String[]> criterias=new HashMap<>();
        criterias.put(MetadataSet.DEFAULT_CLIENT_QUERY_CRITERIA,new String[]{""});
        return criterias;
    }

    private MetadataSet getMds(HttpServletRequest request)throws Throwable {
        String locale;
        try {
            locale = request.getLocale().toString();
        }catch(Throwable t){
            locale="de_DE";
        }
        return MetadataReader.getMetadataset(ApplicationInfoList.getHomeRepository(), CCConstants.metadatasetdefault_id, locale);
    }

}
