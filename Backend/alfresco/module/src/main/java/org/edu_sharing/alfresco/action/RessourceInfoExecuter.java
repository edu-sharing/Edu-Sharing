package org.edu_sharing.alfresco.action;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.action.executer.ActionExecuterAbstractBase;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ActionService;
import org.alfresco.service.cmr.action.ParameterDefinition;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorInputStream;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.tika.Tika;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RessourceInfoExecuter extends ActionExecuterAbstractBase {

	private static final long MAX_JSON_PARSE_SIZE = 1024 * 1024 * 1;
	/** The logger */
	private static Log logger = LogFactory.getLog(RessourceInfoExecuter.class);

	/** The name of the action */
	public static final String NAME = "cc-ressourceinfo-action";

	/**
	 * the node service
	 */
	private NodeService nodeService;

	private ContentService contentService;

	private ActionService actionService = null;

	public static final String CCM_ASPECT_RESSOURCEINFO = "{http://www.campuscontent.de/model/1.0}ressourceinfo";

	public static final String CCM_PROP_IO_RESSOURCETYPE = "{http://www.campuscontent.de/model/1.0}ccressourcetype";
	public static final String CCM_PROP_IO_RESSOURCEVERSION = "{http://www.campuscontent.de/model/1.0}ccressourceversion";

	public static final String CCM_PROP_IO_RESOURCESUBTYPE = "{http://www.campuscontent.de/model/1.0}ccresourcesubtype";
	public static final String CCM_RESSOURCETYPE_MOODLE = "moodle";
	public static final String CCM_RESSOURCETYPE_H5P = "h5p";
	// simple connector elements
	public static final String CCM_RESSOURCETYPE_CONNECTOR = "connector";
	public static final String CCM_RESSOURCETYPE_GEOGEBRA = "geogebra";
	public static final String CCM_RESSOURCETYPE_SERLO = "serlo";
	public static final String CCM_RESSOURCETYPE_EDUHTML = "eduhtml";

	public static ArchiveInputStream getZipInputStream(ContentReader contentreader) throws IOException {
		InputStream is = contentreader.getContentInputStream();

		Tika tika = new Tika();
		String type = tika.detect(is);
		logger.info("type:" + type);

		if(type == null) {

		}else if(type.equals("application/gzip")) {

			try {
				final InputStream bis = new BufferedInputStream(is);
				CompressorInputStream cis = null;
				cis = new CompressorStreamFactory().createCompressorInputStream(CompressorStreamFactory.GZIP,
						bis);
				return new TarArchiveInputStream(cis);

			}catch(CompressorException e) {
				logger.error(e.getMessage());
			}
		}else if(type.equals("application/zip")) {
		    // allowStoredEntriesWithDataDescriptor = true because some h5p might have this
			return new ZipArchiveInputStream(is, contentreader.getEncoding(), true, true);
		}else {
			logger.info("unknown format:" +  type);
		}
		is.close();
		return null;
	}
	protected void executeImpl(Action action, NodeRef actionedUponNodeRef) {

		ContentReader contentreader = this.contentService.getReader(actionedUponNodeRef, ContentModel.PROP_CONTENT);


		if (contentreader != null) {
			try{
				logger.info(contentreader.getMimetype());

				ArchiveInputStream zip = getZipInputStream(contentreader);
				ArchiveEntry current = null;
				if(zip!=null) {
					boolean genericHtmlFallback = false;
					while ((current = zip.getNextEntry()) != null) {
						if (current.getName().equals("imsmanifest.xml")) {

							process(zip, contentreader, actionedUponNodeRef);
							zip.close();
							return;

						}
						if (current.getName().equalsIgnoreCase("index.html") || current.getName().equalsIgnoreCase("index.htm")) {
							genericHtmlFallback = true;
						}

						if (current.getName().equals("moodle.xml")) {
							processMoodle(zip, contentreader, actionedUponNodeRef);
							zip.close();
							return;
						}
						// geogebra
						if (current.getName().endsWith("geogebra.xml")) {
							processGeogebra(zip, actionedUponNodeRef);
							zip.close();
							return;
						}

						if (current.getName().equals("moodle_backup.xml")) {
							processMoodle2_0(zip, contentreader, actionedUponNodeRef);
							zip.close();
							return;
						}
						if (current.getName().equals("h5p.json")) {
							zip.close();
							processH5P(actionedUponNodeRef);
							return;
						}
					}

					zip.close();
                    if(genericHtmlFallback){
                        proccessGenericHTML(actionedUponNodeRef);
                    }
				} else {
					if(Arrays.asList("application/json", "text/plain", "application/octet-stream").contains(contentreader.getMimetype()) &&
							contentreader.getSize() < MAX_JSON_PARSE_SIZE) {
						// stream was already consumed
						contentreader = this.contentService.getReader(actionedUponNodeRef, ContentModel.PROP_CONTENT);
						try (InputStream is = contentreader.getContentInputStream()) {
							if(processSerlo(is, actionedUponNodeRef)) {
								return;
							}
						} catch(Throwable e) {
							logger.debug(e);
						}
					}
				}
			} catch (Exception e) {
				logger.info(e);
			}
		}

	}

	boolean processSerlo(InputStream is, NodeRef nodeRef) {
		try {
			SerloStructure result = new Gson().fromJson(new InputStreamReader(is), SerloStructure.class);
			if("https://serlo.org/editor".equals(result.type)) {
				nodeService.addAspect(nodeRef, QName.createQName(CCM_ASPECT_RESSOURCEINFO),
						null);
				nodeService.setProperty(nodeRef, QName.createQName(CCM_PROP_IO_RESSOURCETYPE),
						CCM_RESSOURCETYPE_SERLO);
				nodeService.setProperty(nodeRef, QName.createQName(CCM_PROP_IO_RESOURCESUBTYPE), result.variant);
				nodeService.setProperty(nodeRef, QName.createQName(CCM_PROP_IO_RESSOURCEVERSION), result.version);
				return true;
			}
		}
		catch(JsonSyntaxException ignored) {}
		catch(Throwable e) {
			logger.debug("Serlo parsing error " + nodeRef, e);
		}
		return false;
	}

	private void proccessGenericHTML(NodeRef nodeRef) {
		nodeService.addAspect(nodeRef, QName.createQName(CCM_ASPECT_RESSOURCEINFO),
				null);
		nodeService.setProperty(nodeRef, QName.createQName(CCM_PROP_IO_RESSOURCETYPE),
				CCM_RESSOURCETYPE_EDUHTML);

		// check if file contains unity content (web gl)
		try{
			ContentReader contentreader = this.contentService.getReader(nodeRef, ContentModel.PROP_CONTENT);
			ArchiveInputStream zip = getZipInputStream(contentreader);
			while(true){
				ArchiveEntry entry = zip.getNextEntry();
				if(entry==null)
					break;
				if(entry.getName().equals("Build/UnityLoader.js")){

					nodeService.setProperty(nodeRef, QName.createQName(CCM_PROP_IO_RESOURCESUBTYPE),
						"webgl");
				}
			}
		}
		catch(Throwable t){
			logger.info(t);
		}
	}

	private void process(InputStream is, ContentReader contentreader, NodeRef actionedUponNodeRef) {
		Document doc = new RessourceInfoTool().loadFromStream(is);
		if ((contentreader.getMimetype().equals("application/zip")
				|| contentreader.getMimetype().equals("application/save-as")
				|| contentreader.getMimetype().equals("application/x-zip-compressed")) && doc != null) {
			try {
				String ressourceType = null;
				String ressourceVersion = null;
				XPathFactory pfactory = XPathFactory.newInstance();
				XPath xpath = pfactory.newXPath();
				String schemaPath = "/manifest/metadata/schema";
				String schemaVersPath = "/manifest/metadata/schemaversion";
				String schema = (String) xpath.evaluate(schemaPath, doc, XPathConstants.STRING);
				String schemaVers = (String) xpath.evaluate(schemaVersPath, doc, XPathConstants.STRING);
				if (schema == null || schema.trim().equals("") || schemaVers == null || schemaVers.trim().equals("")) {
					// scorm
					String spath = "/manifest/organizations/organization[position()=1]/metadata/schema";
					String svpath = "/manifest/organizations/organization[position()=1]/metadata/schemaversion";
					schema = (String) xpath.evaluate(spath, doc, XPathConstants.STRING);
					schemaVers = (String) xpath.evaluate(svpath, doc, XPathConstants.STRING);

					if (schema == null || schema.trim().equals("") || schemaVers == null
							|| schemaVers.trim().equals("")) {
						spath = "/manifest/resources/resource[position()=1]/metadata/schema";
						svpath = "/manifest/resources/resource[position()=1]/metadata/schemaversion";
						schema = (String) xpath.evaluate(spath, doc, XPathConstants.STRING);
						schemaVers = (String) xpath.evaluate(svpath, doc, XPathConstants.STRING);
					}
				}

				logger.info("schema:" + schema);
				logger.info("schemaVers:" + schemaVers);

				ArrayList<RessourceInfoTool.QTIInfo> isQtiList = new RessourceInfoTool().isQti(doc, xpath);
				if (isQtiList.size() > 0) {
					// take the first qtiInfo
					ressourceType = isQtiList.get(0).getType();
					ressourceVersion = isQtiList.get(0).getVersion();
				} else {
					ressourceType = schema;
					ressourceVersion = schemaVers;
				}
				
				if(ressourceType == null || ressourceType.trim().equals("")) {
					
					NodeList ns = (NodeList)xpath.evaluate("//manifest/@*", doc, XPathConstants.NODESET);
					for(int i = 0; i < ns.getLength(); i++) {
						if(ns.item(i) != null) {
							Node n = ns.item(i);
							String tc = n.getTextContent();
							if(tc.contains("http://www.imsproject.org/xsd/imscp_rootv1p1p2")) {
								ressourceType = "ADL SCORM";
								ressourceVersion ="1.2";
							}
						}
					}
					
				}
				
				logger.info("ressourceType:" + ressourceType);
				logger.info("ressourceVersion:" + ressourceVersion);

				if (ressourceType != null && !ressourceType.trim().equals("") && ressourceVersion != null
						&& !ressourceVersion.trim().equals("")) {
					if (this.nodeService.hasAspect(actionedUponNodeRef,
							QName.createQName(CCM_ASPECT_RESSOURCEINFO)) == false) {
						this.nodeService.addAspect(actionedUponNodeRef, QName.createQName(CCM_ASPECT_RESSOURCEINFO),
								null);
					}

					nodeService.setProperty(actionedUponNodeRef, QName.createQName(CCM_PROP_IO_RESSOURCETYPE),
							ressourceType);
					nodeService.setProperty(actionedUponNodeRef, QName.createQName(CCM_PROP_IO_RESSOURCEVERSION),
							ressourceVersion);
					if (isQtiList.size() > 0) {
						ArrayList<String> qtiInfoSubtypeList = new ArrayList<>();
						for (RessourceInfoTool.QTIInfo qtiInfo : isQtiList) {
							String subtype = qtiInfo.getSubtype();
							if (subtype != null && !subtype.trim().equals("")
									&& !qtiInfoSubtypeList.contains(subtype)) {
								qtiInfoSubtypeList.add(subtype);
							}
						}
						// we only need test,questonair or item
						if (qtiInfoSubtypeList.size() > 1) {
							qtiInfoSubtypeList.remove("item");
						}

						if (qtiInfoSubtypeList.size() > 0) {
							nodeService.setProperty(actionedUponNodeRef, QName.createQName(CCM_PROP_IO_RESOURCESUBTYPE),
									qtiInfoSubtypeList);
						}
					}

				}

				/**
				 * wenn qti content suche nach content file und setzte content damit es
				 * indiziert wird
				 */
				if (isQtiList.size() > 0) {

					logger.info("it is an qti. so we are doing some content indexing");

					Action action = actionService.createAction("cc-zipcontent-indexer-action");
					/*
					 * if (parameters != null) { for (Object key : parameters.keySet()) {
					 * action.setParameterValue((String) key, (Serializable) parameters.get(key)); }
					 * }
					 */
					actionService.executeAction(action, actionedUponNodeRef);
				} else {
					logger.info("thats no qti!!!!!");
				}

			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	void processGeogebra(InputStream is, NodeRef actionedUponNodeRef) {
		// thumbnail is handled @org.edu_sharing.alfresco.transformer.GeogebraTransformerWorker
		try {
			Document doc = new RessourceInfoTool().loadFromStream(is);
			XPathFactory pfactory = XPathFactory.newInstance();
			XPath xpath = pfactory.newXPath();
			String schemaVersPath = "/geogebra/@version";
			String schemaVers = (String) xpath.evaluate(schemaVersPath, doc, XPathConstants.STRING);
			if (schemaVers != null && !schemaVers.equals("")) {
				if (!this.nodeService.hasAspect(actionedUponNodeRef,
                        QName.createQName(CCM_ASPECT_RESSOURCEINFO))) {
					this.nodeService.addAspect(actionedUponNodeRef, QName.createQName(CCM_ASPECT_RESSOURCEINFO),
							null);
				}
				nodeService.setProperty(actionedUponNodeRef, QName.createQName(CCM_PROP_IO_RESSOURCETYPE),
						CCM_RESSOURCETYPE_GEOGEBRA);
				nodeService.setProperty(actionedUponNodeRef, QName.createQName(CCM_PROP_IO_RESSOURCEVERSION),
						schemaVers);
			}
		} catch(Throwable e) {
			logger.info("Could not identify if file is a geogebra element: " + e.getMessage());
		}
	}

	private void processMoodle(InputStream is, ContentReader contentreader, NodeRef actionedUponNodeRef) {
		RessourceInfoTool ressourceInfoTool = new RessourceInfoTool();
		Document doc = ressourceInfoTool.loadFromStream(is);
		if ((contentreader.getMimetype().equals("application/zip")
				|| contentreader.getMimetype().equals("application/save-as")
				|| contentreader.getMimetype().equals("application/x-zip-compressed")) && doc != null) {
			try {

				XPathFactory pfactory = XPathFactory.newInstance();
				XPath xpath = pfactory.newXPath();
				// String schemaPath =
				String schemaVersPath = "/MOODLE_BACKUP/INFO/MOODLE_RELEASE";
				String schemaVers = (String) xpath.evaluate(schemaVersPath, doc, XPathConstants.STRING);
				if (schemaVers != null && !schemaVers.equals("")) {
					if (this.nodeService.hasAspect(actionedUponNodeRef,
							QName.createQName(CCM_ASPECT_RESSOURCEINFO)) == false) {
						this.nodeService.addAspect(actionedUponNodeRef, QName.createQName(CCM_ASPECT_RESSOURCEINFO),
								null);
					}

					nodeService.setProperty(actionedUponNodeRef, QName.createQName(CCM_PROP_IO_RESSOURCETYPE),
							CCM_RESSOURCETYPE_MOODLE);
					nodeService.setProperty(actionedUponNodeRef, QName.createQName(CCM_PROP_IO_RESSOURCEVERSION),
							schemaVers);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private void processH5P(NodeRef nodeRef) {
		nodeService.setProperty(nodeRef, QName.createQName(CCM_PROP_IO_RESSOURCETYPE),
				CCM_RESSOURCETYPE_H5P);
		// thumbnail is handled @org.edu_sharing.alfresco.transformer.H5PTransformerWorker
	}

	private void processMoodle2_0(InputStream is, ContentReader contentreader, NodeRef actionedUponNodeRef) {
		Document doc = new RessourceInfoTool().loadFromStream(is);
		if ((contentreader.getMimetype().equals("application/zip")
				|| contentreader.getMimetype().equals("application/save-as")
				|| contentreader.getMimetype().equals("application/x-zip-compressed")
				|| contentreader.getMimetype().equals("application/vnd.moodle.backup")) && doc != null) {
			try {

				XPathFactory pfactory = XPathFactory.newInstance();
				XPath xpath = pfactory.newXPath();
				// String schemaPath =
				String schemaVersPath = "/moodle_backup/information/moodle_release";
				String schemaVers = (String) xpath.evaluate(schemaVersPath, doc, XPathConstants.STRING);
				if (schemaVers != null && !schemaVers.equals("")) {
					if (this.nodeService.hasAspect(actionedUponNodeRef,
							QName.createQName(CCM_ASPECT_RESSOURCEINFO)) == false) {
						this.nodeService.addAspect(actionedUponNodeRef, QName.createQName(CCM_ASPECT_RESSOURCEINFO),
								null);
					}

					nodeService.setProperty(actionedUponNodeRef, QName.createQName(CCM_PROP_IO_RESSOURCETYPE),
							CCM_RESSOURCETYPE_MOODLE);
					nodeService.setProperty(actionedUponNodeRef, QName.createQName(CCM_PROP_IO_RESSOURCEVERSION),
							schemaVers);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	protected void addParameterDefinitions(List<ParameterDefinition> arg0) {
		// TODO Auto-generated method stub

	}

	public static void main(String[] args) {

		try {
			FileInputStream fis = new FileInputStream(
					new File("/Users/mv/Downloads/sicherung-moodle2-course-2-klassenzimmer-20180219-1638-an.mbz"));
			//

			Tika tika = new Tika();
			String type = tika.detect(fis);
			System.out.println("type:" + type);
			
			final InputStream is = new BufferedInputStream(fis);
			// CompressorInputStream in = new
			// CompressorStreamFactory().createCompressorInputStream("bzip2", is);
			CompressorInputStream in = null;
			try {
				in = new CompressorStreamFactory().createCompressorInputStream(CompressorStreamFactory.GZIP, is);
			} catch (CompressorException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			if (in != null) {

				// IOUtils.copy(in, bos);

				TarArchiveInputStream tais = new TarArchiveInputStream(in);
				ArchiveEntry ae = null;
				while ((ae = tais.getNextEntry()) != null) {
					System.out.println(ae.getName());
				}

				tais.close();
				in.close();
			}
	
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public NodeService getNodeService() {
		return nodeService;
	}

	public void setNodeService(NodeService nodeService) {
		this.nodeService = nodeService;
	}

	public ContentService getContentService() {
		return contentService;
	}

	public void setContentService(ContentService contentService) {
		this.contentService = contentService;
	}

	public void setActionService(ActionService actionService) {
		this.actionService = actionService;
	}

	static class SerloStructure {
		public String type;
		public String version;
		public String variant;
	}
}
