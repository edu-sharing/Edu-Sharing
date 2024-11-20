package org.edu_sharing.restservices;

import java.util.Map;

import org.apache.log4j.Logger;
import org.edu_sharing.service.rendering.RenderingDetails;
import org.edu_sharing.service.rendering.RenderingTool;

public class RenderingDao {

	RepositoryDao repoDao;
	
	public RenderingDao(RepositoryDao repoDao) {
		this.repoDao = repoDao;
	}
	
	public RenderingDetails getDetails(String nodeId, String nodeVersion, String displayMode, Map<String,String> parameters) throws DAOException{
		try{
			return repoDao.getRenderingServiceClient().getDetails(repoDao.getId(), nodeId,nodeVersion,
					displayMode==null || displayMode.isEmpty() ? RenderingTool.DISPLAY_DYNAMIC : displayMode,
					parameters);
		}catch(Throwable t){
			throw DAOException.mapping(t);
		}
	}
}
