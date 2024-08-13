package org.edu_sharing.service.notification;

import lombok.RequiredArgsConstructor;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.service.factory.ServiceFactory;

@RequiredArgsConstructor
public class NotificationServiceFactoryImpl implements ServiceFactory {


	private final NotificationService notificationService;

	@Override
	public NotificationService getServiceByAppId(String appId) {
		return notificationService;
	}

	@Override
	public NotificationService getLocalService() {
		return notificationService;
	}
}
