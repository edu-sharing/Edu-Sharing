package org.edu_sharing.alfresco.transformer;

public class GeogebraTransformerWorker {}
/*
public class GeogebraTransformerWorker extends ContentTransformerHelper implements ContentTransformerWorker  {

	static Logger logger = Logger.getLogger(GeogebraTransformerWorker.class);

	NodeService nodeService = null;

	@Override
	public String getComments(boolean available) {
		return "geogebra image and fulltext converter";
	}

	@Override
	public void transform(ContentReader reader, ContentWriter writer, TransformationOptions options) throws Exception {
		// check if file contains an image to use as thumbnail
		try {
			ArchiveInputStream zip = RessourceInfoExecuter.getZipInputStream(reader);
			while (true) {
				ArchiveEntry entry = zip.getNextEntry();
				if(entry==null)
					break;
				String name=entry.getName().toLowerCase();
				if("text/plain".equals(writer.getMimetype())) {
					if(name.endsWith("geogebra.xml")) {
						OutputStream os = writer.getContentOutputStream();
						extractTextContent(zip, os);
						os.close();
						return;
					}
				} else if(name.endsWith("geogebra_thumbnail.png")) {
					OutputStream os = writer.getContentOutputStream();
					InputStream is = ImageTool.autoRotateImage(zip, ImageTool.MAX_THUMB_SIZE);
					StreamUtils.copy(is,os);
					os.close();
					return;
				}
			}
		}
		catch(Throwable t){
			logger.debug(t.getMessage(),t);
		}
		throw new AlfrescoRuntimeException("No image found in geogebra file");
	}

	void extractTextContent(InputStream is, OutputStream os) throws Exception {
		PrintWriter pw = new PrintWriter(os);
		Document doc = new RessourceInfoTool().loadFromStream(is);
		XPathFactory pfactory = XPathFactory.newInstance();
		XPath xpath = pfactory.newXPath();
		String path = "/geogebra//element[@type='inlinetext']/content/@val";
		NodeList text = (NodeList) xpath.evaluate(path, doc, XPathConstants.NODESET);
		boolean hasContent = false;
		for(int i = 0; i < text.getLength(); i++) {Node node = text.item(i);
			List<Object> jsonData = new Gson().fromJson(node.getTextContent(), List.class);
			if(!jsonData.isEmpty()) {
				Map<Object, Object> map = (Map<Object, Object>) jsonData.get(0);
				if (map.containsKey("text")) {
					if (hasContent) {
						pw.write(" ");
					}
					pw.write((String) map.get("text"));
					hasContent = true;
				}
			}
		}
		pw.close();
	}

	public String getVersionString() {
		return "1.0";
	};

	public boolean isAvailable() {
		return true;
	};

	@Override
	public boolean isTransformable(String sourceMimetype, String targetMimetype, TransformationOptions options) {
		return AuthenticationUtil.runAsSystem(() ->
				(sourceMimetype.equals("application/zip") || sourceMimetype.equals("application/octet-stream"))
						&& (MimetypeMap.MIMETYPE_IMAGE_PNG.equals(targetMimetype) || MimetypeMap.MIMETYPE_IMAGE_JPEG.equals(targetMimetype) || "text/plain".equals(targetMimetype))
						&& options.getSourceNodeRef() != null
						&& RessourceInfoExecuter.CCM_RESSOURCETYPE_GEOGEBRA.equals(nodeService.getProperty(options.getSourceNodeRef(), QName.createQName(RessourceInfoExecuter.CCM_PROP_IO_RESSOURCETYPE)))
		);
	}

	public void setNodeService(NodeService nodeService) {
		this.nodeService = nodeService;
	}
}
*/