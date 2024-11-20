package org.edu_sharing.service.rendering;

import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.service.feedback.FeedbackService;
import org.edu_sharing.service.provider.ProviderHelper;
import org.edu_sharing.spring.ApplicationContextFactory;

public class RenderingServiceFactory {
	
	public static RenderingService getRenderingService(String appId){
		ApplicationInfo appInfo = (appId == null) ? ApplicationInfoList.getHomeRepository() : ApplicationInfoList.getRepositoryInfoById(appId);
		if(!ProviderHelper.hasProvider(appInfo)) {
			RenderingService service = (RenderingService) ApplicationContextFactory.getApplicationContext().getBean("renderingService");
			return service;
		} else {
			return ProviderHelper.getProviderByApp(appInfo).getRenderingService();
		}
	}
	public static RenderingService getLocalService(){
		RenderingService service = (RenderingService) ApplicationContextFactory.getApplicationContext().getBean("renderingService");
		return service;
	}
}
