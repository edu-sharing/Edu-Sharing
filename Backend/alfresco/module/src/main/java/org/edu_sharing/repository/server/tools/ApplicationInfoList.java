/**
 *
 *  
 * 
 * 
 *	
 *
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 *
 */
package org.edu_sharing.repository.server.tools;

import org.alfresco.repo.cache.SimpleCache;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;

import java.util.*;


public class ApplicationInfoList {
	
	private static Log logger = LogFactory.getLog(ApplicationInfoList.class);
	
	private static SimpleCache<String, ApplicationInfo> appInfos =  (SimpleCache<String, ApplicationInfo>) AlfAppContextGate.getApplicationContext().getBean("eduSharingApplicationInfoCache");
	private static ApplicationInfo appInfoHome;

	public static ApplicationInfo getRepositoryInfo(String file){
		return getApplicationInfoByProperty(file, ApplicationInfoProperty.APPFILE);
	}

	public static boolean isLocalRepository(String repositoryId) {
		if(repositoryId == null){
			return true;
		}
		ApplicationInfo repo = getRepositoryInfoById(repositoryId);
		return repo.getAppId().equals(getHomeRepository().getAppId()) || ApplicationInfo.REPOSITORY_TYPE_LOCAL.equals(repo.getRepositoryType());
	}

	static enum ApplicationInfoProperty {TYPE, REPOSITORYTYPE, APPID, HOME, APPFILE};
	private static ApplicationInfo getApplicationInfoByProperty(String value, ApplicationInfoProperty prop){
		if(appInfos == null || appInfos.getKeys().size() < 1){
			getApplicationInfos();
		}
		for(String key : appInfos.getKeys()){
			ApplicationInfo appInfo = appInfos.get(key);
			if(prop.equals(ApplicationInfoProperty.TYPE)){
				if(value.equals(appInfo.getType())){
					return appInfo;
				}
			}
			if(prop.equals(ApplicationInfoProperty.REPOSITORYTYPE)){
				if(value.equals(appInfo.getRepositoryType())){
					return appInfo;
				}
			}
			if(prop.equals(ApplicationInfoProperty.APPID)){
				if(value.equals(appInfo.getAppId())){
					return appInfo;
				}
			}
			if(prop.equals(ApplicationInfoProperty.HOME)){
				if(appInfo.ishomeNode()) return appInfo;
			}
			if(prop.equals(ApplicationInfoProperty.APPFILE)){
				if(value.equals(appInfo.getAppFile())){
					return appInfo;
				}
			}

		}
		return null;
	}

	public static ApplicationInfo getRenderService() {
		return getApplicationInfoByProperty(ApplicationInfo.TYPE_RENDERSERVICE, ApplicationInfoProperty.TYPE);
	}

	public static ApplicationInfo getLearningLocker() {
		return getApplicationInfoByProperty(ApplicationInfo.TYPE_LEARNING_LOCKER, ApplicationInfoProperty.TYPE);
	}
	
	public static ApplicationInfo getRepositoryInfoById(String repId){
		return getApplicationInfoByProperty(repId, ApplicationInfoProperty.APPID);
	}
	public static ApplicationInfo getRepositoryInfoByType(String type){
		return getApplicationInfoByProperty(type, ApplicationInfoProperty.TYPE);
	}
	public static ApplicationInfo getRepositoryInfoByRepositoryType(String type){
		return getApplicationInfoByProperty(type, ApplicationInfoProperty.REPOSITORYTYPE);
	}
	/**
	 *
	 * @return Map with AppId, ApplicationInfo
	 */
	public static Collection<String> getAppInfoIds(){
		initAppInfoCache();
		return Collections.unmodifiableCollection(appInfos.getKeys());
	}
	/**
	 *
	 * @return Map with AppId, ApplicationInfo
	 */
	public static Map<String, ApplicationInfo> getApplicationInfos(){
		initAppInfoCache();
		/**
		 * @TODO refactor calls to this methode so that hashmap building is not needed
		 */
		Map<String, ApplicationInfo> result = new HashMap<>();
		appInfos.getKeys().forEach(appId -> result.put(appId,appInfos.get(appId)));
		return Collections.synchronizedMap(result);
	}

	private static void initAppInfoCache() {
		if(appInfos.getKeys().size() == 0){
			logger.debug("appInfos size is 0");
			synchronized(appInfos) {
				initAppInfos();
			}
		}else{
			logger.debug("appInfos size not 0");
		}
	}

	/**
	 * synchronized to prevent errors when multithreads call this static method
	 */
	private static synchronized void initAppInfos(){
		String[] appFileArray = null;
		try{
			ApplicationInfo registry = new ApplicationInfo("ccapp-registry.properties.xml");
			String repStr = registry.getString("applicationfiles", null);
			if(repStr == null || repStr.trim().isEmpty()){
				logger.error("Repository Registry config is undefined or empty");
				return;
			}
			appFileArray = repStr.split(",");
			if(appFileArray.length == 0){
				logger.error("Repository Registry config is empty");
				return;
			}
		}catch(Exception e){
			logger.error("Could not find Repository Registry",e);
			return;
		}

		List<ApplicationInfo> tmpAppInfos = new ArrayList<>();
		for(String appFile: appFileArray){
			logger.debug("appFile:"+appFile);
			appFile = appFile.trim();
			if(appFile.isEmpty()){
				logger.error("found empty value in Repository Registry");
				continue;
			}

			try{
				ApplicationInfo repInfo = new ApplicationInfo(appFile);
				logger.debug("put:"+appFile+" "+repInfo);
				synchronized(tmpAppInfos) {
					tmpAppInfos.add(repInfo);
					if(repInfo.ishomeNode()) {
						appInfoHome = repInfo;
					}
				}
			}catch(Exception e){
				e.printStackTrace();
			}

		}

		for (ApplicationInfo appInfo : tmpAppInfos) {
			if(logger.isDebugEnabled()) {
				logger.debug("put:" + appInfo.getAppFile() + " " + appInfo + " size:" + appInfos.getKeys().size());
			}
			appInfos.put(appInfo.getAppId(), appInfo);
		}
	}
	
	/**
	 * 
	 * @return List of RepositoryInfos with local Repository as the first one
	 */
	public static ArrayList<ApplicationInfo> getRepositoryInfosOrdered(){
		//set home reporsitory as the first one
		ArrayList<ApplicationInfo> appInfoList = new ArrayList<>();
		for(String key : ApplicationInfoList.getApplicationInfos().keySet()){
			ApplicationInfo repInfo = ApplicationInfoList.getApplicationInfos().get(key);
			appInfoList.add(repInfo);
		}
		Collections.sort(appInfoList);
		return appInfoList;
	}
	
	public static ApplicationInfo getHomeRepository(){
		if(appInfos.getKeys().isEmpty()) {
			initAppInfos();
		}
		if(appInfoHome == null) logger.error("no home repository found. check your application files");
		return appInfoHome;
	}
	public static ApplicationInfo getHomeRepositoryObeyConfig(String[] allowedRepos){

		ApplicationInfo realHome = getHomeRepository();

		if(allowedRepos == null || allowedRepos.length == 0 ){
			return realHome;
		}
		List<String> reposList = Arrays.asList(allowedRepos);
		if(reposList.contains("-home-") || reposList.contains(realHome.getAppId())) {
			return realHome;
		}
		ApplicationInfo configHome = getRepositoryInfoById(allowedRepos[0]);
		if(configHome==null){
			logger.error("Config does not allow home, and fallback repo " + allowedRepos[0]+" does not exist! Please check your client.config.xml!");
		} else {
			logger.info("Switching -home- repo to " + allowedRepos[0] + " because of current config");
			return configHome;
		}
		return getHomeRepository();
	}
	
	public static void refresh(){
		logger.debug("calling");
		appInfos.clear();
		getApplicationInfos();
		logger.debug("returning");
	}
}
