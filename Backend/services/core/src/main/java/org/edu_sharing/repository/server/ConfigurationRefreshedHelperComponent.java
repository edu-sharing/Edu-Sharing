package org.edu_sharing.repository.server;

import org.edu_sharing.metadataset.v2.MetadataReader;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.repository.server.tools.HttpQueryTool;
import org.edu_sharing.service.nodeservice.PropertiesInterceptorFactory;
import org.edu_sharing.service.provider.ProviderHelper;
import org.edu_sharing.spring.scope.refresh.RefreshScopeRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Helper bean that invalidates caches of static references on RefreshScopeRefreshedEvent
 */
@Component
public class ConfigurationRefreshedHelperComponent {

	@EventListener
	public void onConfigurationChangedEvent(RefreshScopeRefreshedEvent event) {
		RepoFactory.refresh();
		ApplicationInfoList.refresh();
		HttpQueryTool.invalidateProxySettings(); // reinit proxy settings
		MetadataReader.refresh();
		PropertiesInterceptorFactory.refresh();
		ProviderHelper.clearCache();
	}
}
