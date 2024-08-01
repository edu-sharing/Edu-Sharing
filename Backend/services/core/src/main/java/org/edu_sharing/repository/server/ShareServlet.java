package org.edu_sharing.repository.server;

import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.apache.log4j.Logger;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.rpc.Share;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.repository.server.tools.URLTool;
import org.edu_sharing.service.nodeservice.NodeServiceHelper;
import org.edu_sharing.service.permission.PermissionServiceFactory;
import org.edu_sharing.service.share.ShareService;
import org.edu_sharing.service.share.ShareServiceImpl;
import org.springframework.context.ApplicationContext;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.Date;
import java.util.stream.Stream;
import org.edu_sharing.repository.tools.URLHelper;

public class ShareServlet extends HttpServlet {

	static Logger logger = Logger.getLogger(ShareServlet.class);

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		if(!"download".equals(req.getParameter("mode"))){
			resp.sendRedirect(URLHelper.getNgComponentsUrl()+"sharing?"+req.getQueryString());
			return;
		}
		ServletOutputStream op = resp.getOutputStream();

		String token = req.getParameter("token");
		String password = req.getParameter("password");
		String nodeId = req.getParameter("nodeId");

		if (nodeId == null) {
			op.println("missing nodeId");
			return;
		}

		if (token == null) {
			op.println("missing token");
			return;
		}
		ApplicationContext appContext = AlfAppContextGate.getApplicationContext();

		ServiceRegistry serviceRegistry = (ServiceRegistry) appContext.getBean(ServiceRegistry.SERVICE_REGISTRY);

		final NodeRef nodeRef = new NodeRef(MCAlfrescoAPIClient.storeRef, nodeId);
		final String[] childIds;
		if(req.getParameter("childIds")!=null){
			childIds=req.getParameter("childIds").split(",");
		}
		else{
		// childobject? fetch all sub children as zip
			childIds = AuthenticationUtil.runAsSystem(() -> {
				if (NodeServiceHelper.getType(nodeRef).equals(CCConstants.CCM_TYPE_IO)) {
					return AuthenticationUtil.runAsSystem(() ->
							Stream.concat(
									Stream.of(nodeRef.getId()),
									NodeServiceHelper.getChildrenChildAssociationRefType(nodeRef, CCConstants.CCM_TYPE_IO).
									stream().map((r) -> r.getChildRef().getId())
							).toArray(String[]::new)
					);
				}
				return null;
			});
		}
		if(childIds!=null && childIds.length>1){
			NodeRef finalNodeRef = nodeRef;
			AuthenticationUtil.runAsSystem(() -> {
				String fileName= (String) serviceRegistry.getNodeService().getProperty(finalNodeRef,QName.createQName(CCConstants.CM_NAME));
				DownloadServlet.downloadZip(resp, childIds,nodeId,token,password,fileName+".zip");
				return null;
			});
			return;
		}


		AuthenticationUtil.runAsSystem(() -> {
			try {

				if (!serviceRegistry.getNodeService().exists(nodeRef)) {
					resp.sendRedirect(URLTool.getNgMessageUrl("share_file_deleted"));
					//op.println("File does not longer exist!");
					return null;
				}

				ShareService shareService = new ShareServiceImpl(PermissionServiceFactory.getPermissionService(ApplicationInfoList.getHomeRepository().getAppId()));
				Share share = shareService.getShare(nodeId, token);
				if (share == null) {
					resp.sendRedirect(URLTool.getNgMessageUrl("invalid_share"));
					//op.println("no share found for this nodeid and token!");
					return null;
				}

				if (share.getExpiryDate() != ShareService.EXPIRY_DATE_UNLIMITED) {
					if (new Date(System.currentTimeMillis()).after(new Date(share.getExpiryDate()))) {
						resp.sendRedirect(URLTool.getNgMessageUrl("share_expired"));
						//op.println("share is expired!");
						return null;
					}
				}
				if (share.getPassword() != null && (!share.getPassword().equals(ShareServiceImpl.encryptPassword(password)))) {
					resp.sendRedirect(URLHelper.getNgComponentsUrl() + "sharing?" + req.getQueryString());
				}
				String wwwUrl = (String) serviceRegistry.getNodeService().getProperty(nodeRef, QName.createQName(CCConstants.CCM_PROP_IO_WWWURL));
				if (wwwUrl != null && !wwwUrl.trim().equals("")) {
					resp.sendRedirect(wwwUrl);
					return null;
				}
				NodeRef mappedNodeRef = nodeRef;
				// download child object (io) from a map
				if (childIds != null && serviceRegistry.getNodeService().getType(nodeRef).equals(QName.createQName(CCConstants.CCM_TYPE_MAP))) {
					if (!shareService.isNodeAccessibleViaShare(nodeRef, childIds[0])) {
						resp.sendRedirect(URLTool.getNgMessageUrl("invalid_share"));
						return null;
					}
					mappedNodeRef = new NodeRef(MCAlfrescoAPIClient.storeRef, childIds[0]);
				}
				String nodeName = (String) serviceRegistry.getNodeService().getProperty(mappedNodeRef, QName.createQName(CCConstants.CM_NAME));

				ContentReader reader = serviceRegistry.getContentService().getReader(mappedNodeRef,
						ContentModel.PROP_CONTENT);
				if (reader == null) {
					resp.sendRedirect(URLTool.getNgMessageUrl("share_empty"));
					//op.println("The file is empty!");
					return null;
				}
				String mimetype = reader.getMimetype();

				resp.setContentType((mimetype != null) ? mimetype : "application/octet-stream");
				resp.setContentLength((int) reader.getContentData().getSize());
				resp.setHeader("Content-Disposition", "attachment; filename=\"" + nodeName + "\"");

				int length = 0;
				//
				// Stream to the requester.
				//
				byte[] bbuf = new byte[1024];
				// DataInputStream in = new
				// DataInputStream(url.openStream());
				DataInputStream in = new DataInputStream(reader.getContentInputStream());
				while ((in != null) && ((length = in.read(bbuf)) != -1)) {
					op.write(bbuf, 0, length);
				}

				in.close();
				op.flush();
				op.close();

				share.setDownloadCount((share.getDownloadCount() + 1));
				shareService.updateDownloadCount(share);

			} catch (Throwable e) {
				logger.error(e.getMessage(), e);
			}
			return null;
		});
		return;

	}

}
