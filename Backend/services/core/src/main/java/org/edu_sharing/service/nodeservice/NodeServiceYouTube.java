package org.edu_sharing.service.nodeservice;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.client.tools.forms.VCardTool;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.repository.server.tools.URLTool;

import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.GenericJson;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.YouTube.Videos;
import com.google.api.services.youtube.model.SearchResult;
import com.google.api.services.youtube.model.Thumbnail;
import com.google.api.services.youtube.model.Video;
import com.google.api.services.youtube.model.VideoListResponse;

public class NodeServiceYouTube extends NodeServiceAdapter{

	private String repositoryId;
	private String googleAPIKey;
	public NodeServiceYouTube(String appId) {
		super(appId);
		ApplicationInfo appInfo = ApplicationInfoList.getRepositoryInfoById(appId);
		this.repositoryId = appInfo.getAppId();
		googleAPIKey = appInfo.getApiKey(); 
	}

	@Override
	public Map<String, Object> getProperties(String storeProtocol, String storeId, String nodeId) throws Throwable {
		return getProperties(nodeId);
	}
	private Map<String, Object> getProperties(String nodeId) throws Throwable {
		YouTube youtube = new YouTube.Builder(new NetHttpTransport(), new JacksonFactory(), arg -> {
        }).setApplicationName("youtube-cmdline-search-sample").build();
		Videos.List list = youtube.videos().list("id,snippet");
		list.setId(nodeId);
		list.setKey(googleAPIKey);
		VideoListResponse vlr = list.execute();
		
		List<Video> result = vlr.getItems();
		
		if(result.size() > 0){
			
			Video video = result.get(0);
			return getPropsByVideoEntry(repositoryId,video);
		}
		
		return null;
	}

	public static Map<String, Object> getPropsByVideoEntry(String repositoryId,GenericJson searchResult) {
		Map<String, Object> properties = new HashMap<>();
		
		String esrId = null;
		String esrTitle = null;
		String esrContentUrl = null;
		String esrTechLoc = null;
		long esrModified = 0;
		String esrDescr = null;
		
		String thumbnail = null;
		
		String kind = null;
		
		String keywords = null;
		
		String user = null;
		
		String esrDuration = null;
		String esrAuthor = null;
		
		if(searchResult instanceof SearchResult){
			SearchResult sr = (SearchResult)searchResult;
			esrId = sr.getId().getVideoId();
			esrTitle = sr.getSnippet().getTitle();
			esrContentUrl = URLTool.getRedirectServletLink(repositoryId, esrId);
			esrTechLoc = "https://www.youtube.com/watch?v=" + esrId;
			if(sr.getSnippet().getPublishedAt() != null){
				esrModified = sr.getSnippet().getPublishedAt().getValue();
			}
			esrDescr = sr.getSnippet().getDescription();
			Thumbnail tn = sr.getSnippet().getThumbnails().getDefault();
			thumbnail = tn.getUrl();
			kind = sr.getId().getKind();
			
			user = sr.getSnippet().getChannelTitle();
			
		}else if(searchResult instanceof Video){
			
			Video sr = (Video)searchResult;
			esrId = sr.getId();
			try{
				esrDuration=sr.getFileDetails().getDurationMs().toString();
			}catch(Throwable e){
				esrDuration="0";
			}
			esrTitle = sr.getSnippet().getTitle();
			esrAuthor=sr.getSnippet().getChannelTitle();
			esrContentUrl = URLTool.getRedirectServletLink(repositoryId, esrId);
			esrTechLoc = "https://www.youtube.com/watch?v=" + esrId;
			if(sr.getSnippet().getPublishedAt() != null){
				esrModified  = sr.getSnippet().getPublishedAt().getValue();
			}
			esrDescr = sr.getSnippet().getDescription();
			Thumbnail tn = sr.getSnippet().getThumbnails().getDefault();
			thumbnail = tn.getUrl();
			kind = sr.getKind();
			if( sr.getSnippet().getTags() != null){
				
				for(String kw : sr.getSnippet().getTags()){
					keywords = (keywords == null) ? kw : keywords+CCConstants.MULTIVALUE_SEPARATOR+kw;
				}
			
			}
			
			user = sr.getSnippet().getChannelTitle();
			
		}
		
		if (kind.equals("youtube#video")) {
			
			properties.put(CCConstants.CM_NAME, esrTitle);
			properties.put(CCConstants.LOM_PROP_GENERAL_TITLE, esrTitle);
			properties.put(CCConstants.SYS_PROP_NODE_UID, esrId);
			properties.put(CCConstants.CONTENTURL, esrContentUrl);
			properties.put(CCConstants.VIRT_PROP_PERMALINK,esrTechLoc);
			properties.put(CCConstants.LOM_PROP_TECHNICAL_LOCATION, esrTechLoc);
			properties.put(CCConstants.LOM_PROP_TECHNICAL_DURATION, esrDuration);
			properties.put(CCConstants.CM_PROP_C_MODIFIED, new Date(esrModified));
			properties.put(CCConstants.CM_PROP_C_CREATED, new Date(esrModified));
			properties.put(CCConstants.CM_PROP_C_CREATOR, esrAuthor);
			properties.put(CCConstants.CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_AUTHOR, VCardTool.nameToVCard(esrAuthor));
			
			if(user != null){
				properties.put(CCConstants.CM_PROP_C_MODIFIER, user);
			}
			
			if(keywords != null){
				properties.put(CCConstants.LOM_PROP_GENERAL_KEYWORD,keywords);
			}

			properties.put(CCConstants.LOM_PROP_GENERAL_DESCRIPTION, esrDescr);

			properties.put(CCConstants.LOM_PROP_TECHNICAL_FORMAT, "video/x-flash");
			properties.put(CCConstants.NODETYPE, CCConstants.CCM_TYPE_IO);

			properties.put(CCConstants.CCM_PROP_IO_THUMBNAILURL, thumbnail);
			properties.put(CCConstants.REPOSITORY_ID, repositoryId );
			properties.put(CCConstants.CCM_PROP_IO_COMMONLICENSE_KEY, CCConstants.COMMON_LICENSE_CC_BY);
			properties.put(CCConstants.CCM_PROP_IO_REPLICATIONSOURCE,"youtube");

		}

		return properties;
	}
	public static YouTube getYoutube() {
		return new YouTube.Builder(new NetHttpTransport(), new JacksonFactory(), new HttpRequestInitializer() {
			@Override
			public void initialize(com.google.api.client.http.HttpRequest request) throws IOException {
			}
		}).setApplicationName("youtube-cmdline-search-sample").build();
	}
	
	@Override
	public InputStream getContent(String nodeId) throws Throwable {
		return null;
	}
}
