package org.edu_sharing.restservices;

import org.edu_sharing.repository.client.rpc.Share;
import org.edu_sharing.restservices.shared.*;
import org.edu_sharing.restservices.sharing.v1.model.SharingInfo;

import org.edu_sharing.service.InsufficientPermissionException;
import org.edu_sharing.service.permission.PermissionServiceFactory;
import org.edu_sharing.service.share.ShareService;
import org.edu_sharing.service.share.ShareServiceImpl;

import java.util.*;

public class SharingDao {
	public static SharingInfo getInfo(RepositoryDao repositoryDao, String node, String token,String password) throws DAOException{
		try {
			Share share = getShare(repositoryDao, node, token);
			return new SharingInfo(share, NodeDao.getNode(repositoryDao, node),password);
		}catch(Throwable t){
			throw DAOException.mapping(t);

		}
	}

	private static Share getShare(RepositoryDao repoDao, String node, String token) {
		ShareService service = new ShareServiceImpl(PermissionServiceFactory.getPermissionService(repoDao.getId()));
		Share share = service.getShare(node, token);
		if (share == null)
			throw new IllegalArgumentException("Share with token " + token + " does not exist");
		return share;
	}

	public static List<NodeRef> getChildren(RepositoryDao repositoryDao, String node, String token, String password) throws DAOException {
		try {
			Share share = getShare(repositoryDao, node, token);
			if(share.getPassword()!=null && !share.getPassword().equals(ShareServiceImpl.encryptPassword(password))){
				throw new InsufficientPermissionException("Invalid password supplied");
			}
			NodeDao nodeDao = NodeDao.getNode(repositoryDao, node);
			if(!nodeDao.isDirectory())
				throw new IllegalArgumentException("Node "+node+" is not a directory");
			return nodeDao.getChildren();
		}catch(Throwable t){
			throw DAOException.mapping(t);
		}
	}
}
