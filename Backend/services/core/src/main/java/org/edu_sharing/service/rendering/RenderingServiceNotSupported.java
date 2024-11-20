package org.edu_sharing.service.rendering;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.commons.lang.NotImplementedException;
import org.edu_sharing.repository.server.tools.*;
import org.edu_sharing.service.InsufficientPermissionException;

import java.io.UnsupportedEncodingException;
import java.util.Map;

public class RenderingServiceNotSupported implements RenderingService{

	public RenderingServiceNotSupported(){
	}
	
	@Override
	public RenderingDetails getDetails(String appId, String nodeId,String nodeVersion,String displayMode,Map<String,String> parameters) throws InsufficientPermissionException, Exception{
		throw new NotImplementedException();
	}

	@Override
	public String getDetails(String renderingServiceUrl, RenderingServiceData data) throws JsonProcessingException, UnsupportedEncodingException {
		throw new NotImplementedException();
	}
	@Override
	public RenderingServiceData getData(ApplicationInfo appInfo, String nodeId, String nodeVersion, String user, RenderingServiceOptions options) throws Exception {
		throw new NotImplementedException();
	}

	@Override
	public boolean renderingSupported() {
		return false;
	}

}
