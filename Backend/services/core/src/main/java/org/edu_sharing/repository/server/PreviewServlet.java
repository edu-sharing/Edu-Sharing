package org.edu_sharing.repository.server;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.repo.security.permissions.AccessDeniedException;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ActionStatus;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.InvalidNodeRefException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.version.VersionService;
import org.alfresco.service.namespace.QName;
import org.apache.log4j.Logger;
import org.edu_sharing.alfresco.RestrictedAccessException;
import org.edu_sharing.alfresco.lightbend.LightbendConfigLoader;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.tools.*;
import org.edu_sharing.repository.server.tools.cache.PreviewCache;
import org.edu_sharing.service.mime.MimeTypesV2;
import org.edu_sharing.service.nodeservice.NodeService;
import org.edu_sharing.service.nodeservice.NodeServiceFactory;
import org.edu_sharing.service.nodeservice.NodeServiceHelper;
import org.edu_sharing.service.permission.PermissionServiceFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.util.StreamUtils;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.plugins.jpeg.JPEGImageWriteParam;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.List;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class PreviewServlet extends HttpServlet {

	private static Logger logger = Logger.getLogger(PreviewServlet.class);

	public static final String RESULT_TYPE_MIME_ICON = "mime_type";

	private static final int MAX_IMAGE_SIZE = PreviewCache.MAX_IMAGE_SIZE;

	private static final float DEFAULT_QUALITY = 0.8f;

	ServiceRegistry serviceRegistry;

	@Override
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		ApplicationContext appContext = AlfAppContextGate.getApplicationContext();
		serviceRegistry = (ServiceRegistry) appContext.getBean(ServiceRegistry.SERVICE_REGISTRY);
	}

	private boolean isCacheable(int width,int height, int maxWidth, int maxHeight){
		if(width==-1)
			return true;
		if(maxWidth>0 && maxHeight>0) {
			for(int i=0;i<PreviewCache.CACHE_SIZES_MAX_WIDTH.length;i++){
				if(PreviewCache.CACHE_SIZES_MAX_WIDTH[i]==maxWidth && PreviewCache.CACHE_SIZES_MAX_HEIGHT[i]==maxHeight)
					return true;
			}
		}
		for(int i=0;i<PreviewCache.CACHE_SIZES_WIDTH.length;i++){
			if(PreviewCache.CACHE_SIZES_WIDTH[i]==width && PreviewCache.CACHE_SIZES_HEIGHT[i]==height)
				return true;
		}
		return false;
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		fetchNodeData(req,resp);
	}

	class UnsupportedTypeException extends Exception{

	}

	private void fetchNodeData(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		ServletOutputStream op = resp.getOutputStream();
		String nodeId=null;
		MimeTypesV2 mime=new MimeTypesV2(ApplicationInfoList.getHomeRepository(), MimeTypesV2.PathType.Relative);
		mime.setPreferredFormat(
				"png".equals(req.getParameter("format")) ?
						MimeTypesV2.Format.Png : MimeTypesV2.Format.Svg);
		boolean isCollection=false;

		final StoreRef storeRef;
		String storeProtocol =  req.getParameter("storeProtocol");
		String storeId = req.getParameter("storeId");
		if(storeProtocol != null && storeId != null){
			storeRef = new StoreRef(storeProtocol,storeId);
		}
		else{
			storeRef = StoreRef.STORE_REF_WORKSPACE_SPACESSTORE;
		}
		final NodeService nodeService;
		String repository = req.getParameter("repository");
		boolean remoteNode = false;
		if(repository!=null){
			nodeService = NodeServiceFactory.getNodeService(repository);
			remoteNode = true;
		} else {
			nodeService = NodeServiceFactory.getLocalService();
		}
		try {
			nodeId = req.getParameter("nodeId");
			String type = req.getParameter("type");
			String version = req.getParameter("version");
			NodeRef nodeRef = new NodeRef(storeRef, nodeId);
			String nodeType = nodeService.getType(nodeId);

			// check nodetype for security reasons
			String inNodeId=nodeId;
			Map<String,Object> props;
			if (nodeId != null) {
				try {
					props = nodeService.getProperties(storeRef.getProtocol(),storeRef.getIdentifier(),nodeId);
					String[] aspectsArray = nodeService.getAspects(storeRef.getProtocol(), storeRef.getIdentifier(), nodeId);
					List<String> aspects;
					if(aspectsArray == null){
						aspects = new ArrayList<>();
					} else {
						aspects = Arrays.asList(aspectsArray);
					}
					if (remoteNode || nodeType.equals(CCConstants.CCM_TYPE_REMOTEOBJECT) || aspects.contains(CCConstants.CCM_ASPECT_REMOTEREPOSITORY)) {
						if(aspects.contains(CCConstants.CCM_ASPECT_REMOTEREPOSITORY)){
							// just fetch dynamic data which needs to be fetched, because the local io already has metadata
							props.putAll(NodeServiceFactory.getNodeService(
									(String) props.get(CCConstants.CCM_PROP_REMOTEOBJECT_REPOSITORYID)
							).getPropertiesDynamic(storeProtocol, storeId, (String) props.get(CCConstants.CCM_PROP_REMOTEOBJECT_NODEID)));
						}
						// if its local stored, load the url directly
						String thumbnail = (String)props.get(CCConstants.CCM_PROP_IO_THUMBNAILURL);
						if(thumbnail != null && !thumbnail.trim().equals("")){
							handleExternalThumbnail(nodeId,req, resp, thumbnail);
							return;
						}
						if(nodeType.equals(CCConstants.CCM_TYPE_REMOTEOBJECT) || aspects.contains(CCConstants.CCM_ASPECT_REMOTEREPOSITORY)) {
							props = NodeServiceFactory.getNodeService((String) props.get(CCConstants.CCM_PROP_REMOTEOBJECT_REPOSITORYID))
									.getProperties(storeProtocol, storeId, (String) props.get(CCConstants.CCM_PROP_REMOTEOBJECT_NODEID));
						}
						if(props != null){
							thumbnail = (String)props.get(CCConstants.CCM_PROP_IO_THUMBNAILURL);
							if(thumbnail != null && !thumbnail.trim().equals("")){
								handleExternalThumbnail(nodeId,req, resp, thumbnail);
								return;
							}
						}
						throw new UnsupportedTypeException();
						//deliverContentAsSystem(nodeRef, CCConstants.CM_PROP_CONTENT, req, resp);
						//return;
					}

					// For collections: Fetch the original object for preview
					if(aspects.contains(CCConstants.CCM_ASPECT_COLLECTION_IO_REFERENCE) && props.containsKey(CCConstants.CCM_PROP_IO_ORIGINAL)){

						String original = (String) props.get(CCConstants.CCM_PROP_IO_ORIGINAL);

						if(!nodeId.equals(original)){
							nodeId=(String) props.get(CCConstants.CCM_PROP_IO_ORIGINAL);
							isCollection=true;
						}
					}

					validateScope(req, props);
					// we need to check permissions and allow or deny access by using the READ_PREVIEW permission
					validatePermissions(storeRef,inNodeId);

					if (!nodeType.equals(CCConstants.CCM_TYPE_IO)
							&& !nodeType.equals(CCConstants.CCM_TYPE_MAP)
							&& !nodeType.equals(CCConstants.CCM_TYPE_SAVED_SEARCH)) {
						throw new UnsupportedTypeException();
					}
				} catch (InvalidNodeRefException e) {
					resp.sendError(HttpServletResponse.SC_NOT_ACCEPTABLE, e.getMessage());
					return;
				}
			} else {
				resp.sendError(HttpServletResponse.SC_NOT_ACCEPTABLE, "nodeId is null!");
				return;
			}

			if(CCConstants.CCM_TYPE_MAP.equals(nodeType)){
				if(deliverContentAsSystem(nodeRef, CCConstants.CCM_PROP_MAP_ICON, req, resp))
					return;
			}


			//get previewurl to find out type (generated/userdefined)
			//Attention the url of GetPreviewResult of generated/userdefined previews points on this servlet so don't use it

			PreviewDetail getPrevResult = null;
			// check if version is requested and version seems to be NOT the current node version
			if(version != null && !version.trim().equals("") && !isCollection && !version.equals(props.get(CCConstants.LOM_PROP_LIFECYCLE_VERSION))){
				Map<String, Map<String,Object>> versionHistory = nodeService.getVersionHistory(nodeId);
				if(versionHistory != null){
					for(Map.Entry<String, Map<String,Object>> entry : versionHistory.entrySet()){
						String tmpVers = (String)entry.getValue().get(CCConstants.LOM_PROP_LIFECYCLE_VERSION);
						if(version.equals(tmpVers)){

							String vNodeId = (String)entry.getValue().get(CCConstants.SYS_PROP_NODE_UID);
							System.out.println("vNodeId:"+vNodeId+ " entry key:"+entry.getKey());
							try {
								getPrevResult = getPreview(nodeService,VersionService.VERSION_STORE_PROTOCOL, "version2Store", entry.getKey());
							}catch(InvalidNodeRefException e){
								// ignoring this error since versioned files don't have a preview
								// converting it to an other exception so that the mime type handler will take care
								throw new Exception("Versioned files don't have a preview",e);
							}

						}

					}
				}
			}
			final String nodeIdFinal=nodeId;

			if(getPrevResult == null){
				// we need to access the actual object as admin
				// for collections, this is required
				// and since may there is no right to access binary content (but READ_PREVIEW is present and validated before)
				getPrevResult = AuthenticationUtil.runAsSystem(() -> getPreview(nodeService,storeRef.getProtocol(), storeRef.getIdentifier(), nodeIdFinal));
			}

			if(isCollection){
				final PreviewDetail getPrevResultFinal=getPrevResult;
				NodeServiceHelper.validatePermissionRestrictedAccess(new NodeRef(new StoreRef(storeProtocol, storeId), inNodeId), CCConstants.PERMISSION_READ_PREVIEW);
				if(AuthenticationUtil.runAsSystem(new RunAsWork<Boolean>() {
					@Override
					public Boolean doWork() throws IOException  {
						return loadPreview(req, resp, op, storeRef, nodeIdFinal, nodeService, getPrevResultFinal);
					}
				})){
					return;
				}
			}
			else{
				if(loadPreview(req, resp, op, storeRef, nodeId, nodeService, getPrevResult)){
					return;
				}
			}

			/**
			 * generated or userdefined
			 */
			if (getPrevResult != null && (getPrevResult.getType().equals(PreviewDetail.TYPE_USERDEFINED) || getPrevResult.getType().equals(PreviewDetail.TYPE_GENERATED))) {
				NodeRef prevNodeRef = null;
				String property = CCConstants.CM_PROP_CONTENT;
				if (getPrevResult.getType().equals(PreviewDetail.TYPE_USERDEFINED)) {
					prevNodeRef = new NodeRef(MCAlfrescoAPIClient.storeRef, nodeId);
					property = CCConstants.CCM_PROP_IO_USERDEFINED_PREVIEW;
				}
				if (getPrevResult.getType().equals(PreviewDetail.TYPE_GENERATED)) {


					Map<String, Object> previewProps = null;
					final String fnodeId = nodeId;
					if(isCollection) {
						prevNodeRef = AuthenticationUtil.runAsSystem(
								() -> nodeService.getChild(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE,fnodeId, CCConstants.CM_TYPE_THUMBNAIL, CCConstants.CM_NAME,
										CCConstants.CM_VALUE_THUMBNAIL_NAME_imgpreview_png));
					}else {
						prevNodeRef = nodeService.getChild(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE,nodeId, CCConstants.CM_TYPE_THUMBNAIL, CCConstants.CM_NAME,
								CCConstants.CM_VALUE_THUMBNAIL_NAME_imgpreview_png);
					}
				}
				if (prevNodeRef != null) {

					if(isCollection) {
						NodeRef fprevNodeRef = prevNodeRef;
						String fproperty = property;
						boolean result = deliverContentAsSystem(fprevNodeRef, fproperty, req, resp);
						if(result) {
							return;
						}
						op.close();
					}else {
						if(deliverContentAsSystem(prevNodeRef, property, req, resp)) {

							return;
						}
						op.close();


					}

				}
			}



		} catch (org.alfresco.repo.security.permissions.AccessDeniedException | RestrictedAccessException e) {
			logger.debug(e.getMessage(),e);
			resp.sendRedirect(mime.getNoPermissionsPreview());
			return;
		}  catch (InvalidNodeRefException e) {
			logger.debug(e.getMessage(),e);
			resp.sendRedirect(mime.getNodeDeletedPreview());
			return;
		}
		catch (UnsupportedTypeException e){
			// ignore, the node type ist not supported for image previews
		}  catch (Throwable e) {
			// smaller logging for collection ref (i.e. original may deleted, that occurs often)
			if(isCollection)
				logger.warn(e.getMessage());
			else
				logger.warn(e);
		}
		/**
		 * fallback to mime first, then default
		 */
		try{
			Map<String,Object> props;
			String[] aspects=new String[]{};
			String type=null;
			if(isCollection){
				final String nodeIdFinal=nodeId;
				props=AuthenticationUtil.runAsSystem(() -> {
                    try{
                        return NodeServiceFactory.getLocalService().getProperties(storeRef.getProtocol(), storeRef.getIdentifier(),nodeIdFinal);
                    }catch(Throwable t){
                        throw new Exception(t);
                    }
                });
			}
			else{
				props=nodeService.getProperties(storeRef.getProtocol(), storeRef.getIdentifier(),nodeId);
				aspects=nodeService.getAspects(storeRef.getProtocol(), storeRef.getIdentifier(),nodeId);
				type = nodeService.getType(nodeId);
			}
			resp.sendRedirect(mime.getPreview(type,props,Arrays.asList(aspects)));
			return;
		}
		catch(Throwable t){
			resp.sendRedirect(mime.getDefaultPreview());
		}
	}

	private void validatePermissions(StoreRef storeRef, String nodeId) {
		boolean result = PermissionServiceFactory.getLocalService().hasPermission(storeRef.getProtocol(),storeRef.getIdentifier(),nodeId,CCConstants.PERMISSION_READ_PREVIEW);
		if(!result)
			throw new AccessDeniedException("No "+CCConstants.PERMISSION_READ_PREVIEW+" on "+nodeId);
	}

	private void validateScope(HttpServletRequest req, Map<String, Object> props) {
		String scope=(String) req.getSession().getAttribute(CCConstants.AUTH_SCOPE);
		// Allow only valid scope
		if(props.containsKey(CCConstants.CCM_PROP_EDUSCOPE_NAME)){
			String nodeScope=(String) props.get(CCConstants.CCM_PROP_EDUSCOPE_NAME);
			if(!nodeScope.equals(scope)){
				throw new AccessDeniedException("Node has an other scope");
			}
		}
		// This happens if the user tries to access a non-scoped node from a scope
		else if(scope!=null){
			throw new AccessDeniedException("Node does not have a scope");
		}
	}

	/**
	 * returns true wenn a redirect was done
	 * @param req
	 * @param resp
	 * @param op
	 * @param nodeId
	 * @param getPrevResult
	 * @return
	 * @throws IOException
	 */
	private boolean loadPreview(HttpServletRequest req, HttpServletResponse resp, ServletOutputStream op,StoreRef storeRef, String nodeId,
								NodeService nodeService, PreviewDetail getPrevResult) throws IOException {

		if(getPrevResult == null){
			return false;
		}

		/**
		 * external URL
		 */
		if (getPrevResult.getType().equals(PreviewDetail.TYPE_EXTERNAL)) {
			String extThumbUrl = getPrevResult.getUrl();
			if (extThumbUrl != null && !extThumbUrl.trim().equals("")) {
				return handleExternalThumbnail(nodeId,req, resp, extThumbUrl);
			}
		}

		/**
		 * generated or user defined
		 */
		if (getPrevResult.getType().equals(PreviewDetail.TYPE_USERDEFINED) || getPrevResult.getType().equals(PreviewDetail.TYPE_GENERATED)) {
			NodeRef prevNodeRef = null;
			String property = CCConstants.CM_PROP_CONTENT;
			if (getPrevResult.getType().equals(PreviewDetail.TYPE_USERDEFINED)) {
				prevNodeRef = new NodeRef(MCAlfrescoAPIClient.storeRef, nodeId);
				property = CCConstants.CCM_PROP_IO_USERDEFINED_PREVIEW;
			}
			if (getPrevResult.getType().equals(PreviewDetail.TYPE_GENERATED)) {
				prevNodeRef = nodeService.getChild(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE,nodeId, CCConstants.CM_TYPE_THUMBNAIL, CCConstants.CM_NAME,
						CCConstants.CM_VALUE_THUMBNAIL_NAME_imgpreview_png);
			}
			if (prevNodeRef != null) {
				if(deliverContentAsSystem(prevNodeRef, property, req, resp))
					return true;
				op.close();
			}
		}
		if (getPrevResult.getType().equals(PreviewDetail.TYPE_DEFAULT)) {
			NodeRef nodeRef = new NodeRef(MCAlfrescoAPIClient.storeRef, nodeId);
			String mimetype=NodeServiceFactory.getLocalService().getContentMimetype(storeRef.getProtocol(), storeRef.getIdentifier(),nodeId);
			if(mimetype!=null && mimetype.startsWith("image")) {
				if(deliverContentAsSystem(nodeRef,  CCConstants.CM_PROP_CONTENT, req, resp))
					return true;
			}
		}
		return false;
	}

	private boolean handleExternalThumbnail(String nodeId, HttpServletRequest req, HttpServletResponse resp, String url) throws IOException {
		if ("false".equalsIgnoreCase(req.getParameter("allowRedirect")) &&
				LightbendConfigLoader.get().getStringList("repository.communication.preview.remoteAllowList").stream().anyMatch((reg) -> {
					Pattern pattern = Pattern.compile(reg);
					Matcher matched = pattern.matcher(url);
					return matched.matches();
				})) {
			logger.debug("Follow redirect allowed for " + url);
			try{
				new HttpQueryTool().queryStream(url, new HttpQueryTool.Callback<Void>() {
					@Override
					public void handle(InputStream httpResult) {
						resp.setHeader("Content-Type", "image/jpeg");
						try {
							DataInputStream extImgTransformed = postProcessImage(nodeId, new DataInputStream(httpResult), req, null);
							StreamUtils.copy(extImgTransformed, resp.getOutputStream());
						} catch (IOException e) {
							throw new RuntimeException(e);
						}
					}
				});
				return true;
			} catch(Throwable t) {
				logger.info("Fetching preview via http failed for: " + url, t);
			}
		}
		resp.sendRedirect(url);
		return true;
	}

	private DataInputStream postProcessImage(String nodeId, DataInputStream in, HttpServletRequest req, String mimetype){
		float quality=DEFAULT_QUALITY;

		int width=0,height=0,maxHeight=0,maxWidth=0;
		boolean crop=false;
		boolean hasAnyValue=false;
		try{
			quality=Integer.parseInt(req.getParameter("quality"))/100.f;
			hasAnyValue=true;
		}catch(Throwable t){}

		try{
			crop=req.getParameter("crop").equals("true");
			hasAnyValue=true;
		}catch(Throwable t){}
		try{
			width=Integer.parseInt(req.getParameter("width"));
		}catch(Throwable t){}
		try{
			height=Integer.parseInt(req.getParameter("height"));
		}catch(Throwable t){}
		try{
			maxWidth=Integer.parseInt(req.getParameter("maxWidth"));
		}catch(Throwable t){}
		try{
			maxHeight=Integer.parseInt(req.getParameter("maxHeight"));
		}catch(Throwable t){}
		boolean fullsize=false;
		if(!hasAnyValue) {
			width=-1;
			height=-1;
			crop=true;
			fullsize=true;
			maxWidth=MAX_IMAGE_SIZE;
			maxHeight=MAX_IMAGE_SIZE;
		}

		boolean fromCache=false;
		if(fullsize || isCacheable(width, height,maxWidth,maxHeight)){
			File file=PreviewCache.getFileForNode(nodeId,fullsize ? -1 : width,height,maxWidth,maxHeight,false);
			if(file!=null && file.exists()){
				try{
					in.close();
					in=new DataInputStream(new FileInputStream(file));
					fromCache=true;
				}catch(Throwable t){}
			}
		}
		// if the image is cached, allow only 70% quality because it's useless to compress in higher ratio
		quality=Math.min(Math.max(quality, 0), fromCache ? DEFAULT_QUALITY : 1);
		width=Math.min(Math.max(width, 0), MAX_IMAGE_SIZE);
		height=Math.min(Math.max(height, 0), MAX_IMAGE_SIZE);
		maxWidth=Math.min(Math.max(maxWidth, 0), MAX_IMAGE_SIZE);
		maxHeight=Math.min(Math.max(maxHeight, 0), MAX_IMAGE_SIZE);

		try{
			// cache optimization, if no other tasks, just return the cached preview
			if(fromCache && Math.abs(quality-DEFAULT_QUALITY)<0.1){
				logger.debug("Sending direct image cache to client: "+nodeId);
				byte[] img=StreamUtils.copyToByteArray(in);
				return new DataInputStream(new ByteArrayInputStream(img));
			}
			BufferedImage img;
			boolean svg = false;
			if(Objects.equals("image/svg+xml", mimetype)) {
				svg = true;
				img = ImageIO.read(new ByteArrayInputStream(ImageTool.convertSvgToPng(in)));
			} else {
				img = ImageIO.read(in);
			}

			try{
				float aspect=Float.parseFloat(req.getParameter("aspect"));
				if(aspect>1){
					width=img.getWidth();
					height=(int) (width/aspect);
				}
				else{
					height=img.getHeight();
					width=(int) (height*aspect);
				}
			}
			catch(Throwable t){}
			boolean scale=true;
			if(crop && !fromCache){
				float aspectOriginal=(float)img.getWidth()/(float)img.getHeight();
				if(maxWidth>0){
					width=(int) Math.min(height*aspectOriginal,maxWidth);
				}
				if(maxHeight>0){
					height=(int) Math.min(width/aspectOriginal,maxHeight);
				}
				if(maxWidth>0 && maxHeight>0) {
					if(aspectOriginal>1) {
						width=maxWidth;
						height=(int) (width/aspectOriginal);
					}
					else {
						height=maxHeight;
						width=(int) (height*aspectOriginal);
					}
					if(width>img.getWidth() || height>img.getHeight()) {
						scale=false;
					}
				}
				if(!scale && !svg)
					return null;
				BufferedImage cropped=new BufferedImage(width,height, BufferedImage.TYPE_INT_ARGB); // getType() sometimes return 0
				float aspectCrop=(float)width/(float)height;
				Graphics g=cropped.getGraphics();
				if(aspectCrop>aspectOriginal){
					float scaledHeight=cropped.getWidth()/aspectOriginal;
					Image scaled=img.getScaledInstance((int)(scaledHeight*aspectOriginal),(int)scaledHeight, BufferedImage.SCALE_SMOOTH);
					g.drawImage(scaled, 0,(int)( -(scaledHeight-cropped.getHeight())/2), cropped.getWidth(),(int)scaledHeight, null);
				}
				else{
					float scaledWidth=cropped.getHeight()*aspectOriginal;
					Image scaled=img.getScaledInstance((int)scaledWidth,(int)(scaledWidth/aspectOriginal), BufferedImage.SCALE_SMOOTH);
					g.drawImage(scaled, (int)( -(scaledWidth-cropped.getWidth())/2),0,(int)scaledWidth, cropped.getHeight(), null);
				}
				img=cropped;

			}
			BufferedImage imgOut=new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
			imgOut.getGraphics().setColor(java.awt.Color.WHITE);
			imgOut.getGraphics().fillRect(0, 0,imgOut.getWidth(),imgOut.getHeight());
			imgOut.getGraphics().drawImage(img, 0, 0, null);

			if(!fromCache && (isCacheable(width, height,maxWidth,maxHeight) || fullsize)){
				// Drop alpha (weird colors in jpg otherwise)
				ImageIO.write(imgOut, "JPG",PreviewCache.getFileForNode(nodeId,fullsize ? -1 : width, height,maxWidth,maxHeight,true));
			}

			JPEGImageWriteParam jpegParams = new JPEGImageWriteParam(null);
			jpegParams.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
			jpegParams.setCompressionQuality(quality);
			ImageWriter writer = ImageIO.getImageWritersByFormatName("jpg").next();
			ByteArrayOutputStream os=new ByteArrayOutputStream();
			MemoryCacheImageOutputStream imgOutStream = new MemoryCacheImageOutputStream(os);

			writer.setOutput(imgOutStream);
			writer.write(null,new IIOImage(imgOut,null,null),jpegParams);
			imgOutStream.close();
			in.close();
			return new DataInputStream(new ByteArrayInputStream(os.toByteArray()));
		} catch(OutOfMemoryError e) {
			throw e;
		}catch(Throwable t){
			if(t.getCause() instanceof OutOfMemoryError) {
				throw (OutOfMemoryError)t.getCause();
			}
			return null;
		}
		finally {
			try {
				in.close();
			} catch (IOException e) {}
		}

	}
	private boolean deliverContentAsSystem(NodeRef nodeRef, String contentProp,HttpServletRequest req, HttpServletResponse resp) throws IOException{
		return AuthenticationUtil.runAsSystem(new RunAsWork<Boolean>() {

			@Override
			public Boolean doWork() throws Exception {
				ContentReader reader = serviceRegistry.getContentService().getReader(nodeRef,
						QName.createQName(contentProp));

				if(reader == null){
					return false;
				}

				String mimetype = reader.getMimetype();


				//resp.setContentLength((int) reader.getContentData().getSize());
				// resp.setHeader("Content-Disposition",
				// "attachment; filename=\" preview.png \"");

				int length = 0;
				//
				// Stream to the requester.
				//

				// DataInputStream in = new
				// DataInputStream(url.openStream());
				InputStream is=reader.getContentInputStream();
				DataInputStream in = new DataInputStream(is);
				if(mimetype.startsWith("image")) {
					try {
						DataInputStream tmp = postProcessImage(nodeRef.getId(), in, req, mimetype);
						if (tmp != null) {
							in = tmp;
							mimetype = "image/jpeg";
						} else {
						// image was broken but stream is consumed, open a new one
						reader = serviceRegistry.getContentService().getReader(nodeRef,
								QName.createQName(contentProp));
						is=reader.getContentInputStream();
						in = new DataInputStream(is);
					}

					} catch (OutOfMemoryError e) {
						logger.debug("Image too large for memory, falling back to icon " + nodeRef);
						throw e;
					}
				}
				deliverImage(mimetype, in, resp);
				return true;
			}
		});

	}

	private void deliverImage(String mimetype, DataInputStream in, HttpServletResponse resp) throws IOException {
		ServletOutputStream op = resp.getOutputStream();
		int length;
		// fix to proper mimetype (usually comes at "image/svg xml" which is not valid)
		if(mimetype.startsWith("image/svg")){
			mimetype = "image/svg+xml";
		}
		if(mimetype.equals("image/svg+xml")) {
			throw new RuntimeException("svg is not supported and could not be converted");
		}
		resp.setContentType(mimetype);

		resp.setContentLength((int) in.available());

		byte[] bbuf = new byte[1024];
		while ((in != null) && ((length = in.read(bbuf)) != -1)) {
			op.write(bbuf, 0, length);
		}

		in.close();

		op.flush();
		op.close();
	}


	public static PreviewDetail getPreview(NodeService nodeService,String storeProtocol, String storeIdentifier, String nodeId){
		return getPreview(nodeService,storeProtocol,storeIdentifier,nodeId,null);
	}
	public static PreviewDetail getPreview(NodeService nodeService,String storeProtocol, String storeIdentifier, String nodeId,Map<String, Object> nodeProps){
		StoreRef storeRef = new StoreRef(storeProtocol,storeIdentifier);
		NodeRef nodeRef = new NodeRef(storeRef,nodeId);
		if(!nodeService.getType(nodeId).equals(CCConstants.CCM_TYPE_IO)){
			return null;
		}

		String extThumbnail =(nodeProps == null) ? nodeService.getProperty(storeProtocol,storeIdentifier,nodeId,CCConstants.CCM_PROP_IO_THUMBNAILURL)
				: (String) nodeProps.get(CCConstants.CCM_PROP_IO_THUMBNAILURL);
		if (extThumbnail != null && !extThumbnail.trim().equals("")) {
			return new PreviewDetail(extThumbnail, PreviewDetail.TYPE_EXTERNAL, false);
		}

		String defaultImageUrl = URLTool.getBaseUrl() + "/"
				+ CCConstants.DEFAULT_PREVIEW_IMG;

		try (InputStream crUserDefinedPreview=nodeService.getContent(storeProtocol,storeIdentifier,nodeId,null,CCConstants.CCM_PROP_IO_USERDEFINED_PREVIEW)) {
			/**
			 * userdefined
			 */
			if (crUserDefinedPreview != null && crUserDefinedPreview.available() > 0) {
				String url = nodeService.getPreview(storeProtocol,storeIdentifier,nodeId, null, null).getUrl();
				return new PreviewDetail(url, PreviewDetail.TYPE_USERDEFINED, false);
			}

		}catch(Throwable t){
			// may fails if the user does not has access for content
		}


		/**
		 * generated and action active
		 */
		Action action = ActionObserver.getInstance().getAction(nodeRef, CCConstants.ACTION_NAME_CREATE_THUMBNAIL);
		if (action != null && action.getExecutionStatus().equals(ActionStatus.Running)) {
			return new PreviewDetail(defaultImageUrl, PreviewDetail.TYPE_DEFAULT, true);
		}

		/**
		 * generated and no action active
		 */
		NodeRef previewProps = nodeService.getChild(storeRef, nodeId, CCConstants.CM_TYPE_THUMBNAIL, CCConstants.CM_NAME,
				CCConstants.CM_VALUE_THUMBNAIL_NAME_imgpreview_png);
		if (previewProps != null) {
			String url = NodeServiceHelper.getPreview(new NodeRef(storeRef, nodeId)).getUrl();
			return new PreviewDetail(url, PreviewDetail.TYPE_GENERATED, false);
		}

		return new PreviewDetail(defaultImageUrl, PreviewDetail.TYPE_DEFAULT, false);
	}
	public static class PreviewDetail{
		private String url;

		private String type;

		private boolean createActionIsRunning = true;

		public static final String TYPE_EXTERNAL = "TYPE_EXTERNAL";
		public static final String TYPE_USERDEFINED = "TYPE_USERDEFINED";
		public static final String TYPE_GENERATED = "TYPE_GENERATED";
		public static final String TYPE_DEFAULT = "TYPE_DEFAULT";
		public PreviewDetail(String url, String type, boolean createActionIsRunning) {
			this.url = url;
			this.type = type;
			this.createActionIsRunning = createActionIsRunning;
		}

		public String getUrl() {
			return url;
		}

		public String getType() {
			return type;
		}

		public boolean isCreateActionIsRunning() {
			return createActionIsRunning;
		}
	}
}
