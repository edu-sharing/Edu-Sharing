package org.edu_sharing.service.foldertemplates;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.edu_sharing.repository.client.rpc.EduGroup;
import org.edu_sharing.repository.client.rpc.Group;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.MCAlfrescoAPIClient;
import org.edu_sharing.repository.server.MCAlfrescoBaseClient;
import org.edu_sharing.alfresco.repository.server.authentication.Context;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.repository.server.tools.UserEnvironmentTool;
import org.edu_sharing.service.permission.PermissionServiceFactory;
import org.edu_sharing.service.authority.AuthorityServiceFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * @author hupfer
 */
public class FolderTemplatesImpl implements FolderTemplates {

	MCAlfrescoBaseClient repoClient;
	EduGroup eduGroup;
	String folderNodeId;
	String templatePropertiesFile;
	Properties properties;
	Logger logger = Logger.getLogger(FolderTemplatesImpl.class);
	Map<String, Group> subGroupList = new HashMap<>();
	
	LoggingErrorHandler loggingErrorHandler = new LoggingErrorHandler();
	
	org.edu_sharing.service.permission.PermissionService permissionService = PermissionServiceFactory.getPermissionService(ApplicationInfoList.getHomeRepository().getAppId());

	public FolderTemplatesImpl(MCAlfrescoBaseClient repoClient) {
		this.repoClient = repoClient;
	}

	public FolderTemplatesImpl() {
	}

	private boolean checkAuth() throws Exception {

		if (this.repoClient.isAdmin()) {
			return true;
		};

		String AdminGroup = this.properties.getProperty("admingroup");
		if (AdminGroup.isEmpty()){
			logger.error("admingroup is not set in properties file");
			return false;
		}

		String mappedGroup = getMappedGroup(AdminGroup, this.eduGroup.getGroupDisplayName());
		if (mappedGroup.isEmpty()){
			logger.error("mapped admingroup empty ");
			return false;
		}
		mappedGroup = mappedGroup.replace("GROUP_", "");

		String[] member = AuthorityServiceFactory.getLocalService().getMembershipsOfGroup(mappedGroup);

		String user = (String) Context.getCurrentInstance().getRequest().getSession()
				.getAttribute(CCConstants.AUTH_USERNAME);

		int i;
		for (i = 0; i < member.length; i++) {
			if (member[i].equals(user)) {
				return true;
			}
		}
		
		return false;
	}

	public boolean validateTemplate(InputStream is) throws Throwable {

		boolean nameSpaceAware = false;
		InputStream schemaStream;
		
		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

		try {
			schemaStream =  classLoader.getResourceAsStream("template.xsd");
		} catch (Exception e) {
			loggingErrorHandler.getMessage().add(e.getMessage());
			//e.printStackTrace();
			return false;
		}

		try {
			TemplateValidate.validateXML(is, schemaStream, nameSpaceAware, loggingErrorHandler);
		} catch (Exception e) {
			loggingErrorHandler.getMessage().add(e.getMessage());
			//e.printStackTrace();
			return false;
		}
		
		return true;
	}

	/**
	 * read properties from template.properties file located in Edu_Sharing_Sys_Template folder
	 * @author hupfer
	 */
	private void readProperies() {
		try {
			InputStream i = ((MCAlfrescoAPIClient) this.repoClient).getContent(this.templatePropertiesFile);
			this.properties = new Properties();
			this.properties.load(i);

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * set edugroup
	 * @author hupfer
	 */
	private boolean setEduGroup(String eduGroupname) throws Throwable {
		this.eduGroup = AuthorityServiceFactory.getAuthorityService((ApplicationInfoList.getHomeRepository().getAppId())).getEduGroup(eduGroupname);
		if (this.eduGroup == null || this.eduGroup.getGroupId().isEmpty()) {
			logger.error("no eduGroup found");
			loggingErrorHandler.getMessage().add("no eduGroup found");
			return false;
		}
		this.folderNodeId = eduGroup.getFolderId();
		return true;
	}

	public void setTemplate(String templateName, String eduGroupname, String folder) throws Throwable {
		
		loggingErrorHandler.getMessage().clear();
		
		if (templateName.isEmpty()) {
			logger.fatal("template Name is empty ");
			loggingErrorHandler.getMessage().add("template Name is empty");
			return;
		}
		
		if (eduGroupname.isEmpty()) {
			logger.fatal("eduGroup Name is empty");
			loggingErrorHandler.getMessage().add("eduGroup Name is empty");
			return;
		}

		if (!setEduGroup(eduGroupname)) {
			return;
		};
		
		if (folder != null && !folder.contentEquals("")) {
			this.folderNodeId = folder;
			logger.info("folderId was set ");
			loggingErrorHandler.getMessage().add("folderId was set ");

		}

		String groupNodeId = eduGroup.getGroupId();

		UserEnvironmentTool uit = new UserEnvironmentTool(ApplicationInfoList.getHomeRepository().getAppId(),
				this.repoClient.getAuthenticationInfo());
		String Sysf = uit.getEdu_SharingTemplateFolder();

		Map<String, Map<String, Object>> templatelist = this.repoClient.getChildren(Sysf);
		if (templatelist.isEmpty()) {
			logger.fatal("no Templates  found");
			loggingErrorHandler.getMessage().add("no Templates found");
			return;
		}
		String filename = "";
		String fileformat = "";
		String fileNodeId = "";

		for (Map<String, Object> tl : templatelist.values()) {

			filename = (String) tl.get(CCConstants.CM_NAME);
			fileformat = (String) tl.get(CCConstants.LOM_PROP_TECHNICAL_FORMAT);

			if (filename.equals(templateName) && fileformat.equals("text/xml")) {
				fileNodeId = (String) tl.get(CCConstants.SYS_PROP_NODE_UID);
			}
			if (filename.equals("template.properties") && fileformat.equals("text/plain")) {
				this.templatePropertiesFile = (String) tl.get(CCConstants.SYS_PROP_NODE_UID);
			}
		}
		if(fileNodeId.isEmpty()){
			logger.fatal("Template "+templateName+" not found");
			loggingErrorHandler.getMessage().add("Template "+templateName+" not found");
			return;
		}
		if (filename.isEmpty()) {
			logger.fatal("no Templates found");
			loggingErrorHandler.getMessage().add("no Templates  found");
			return;
		}

		// get SubGroups
		Map<String, Map<String, Object>> subList = this.repoClient.getChildren(groupNodeId);
		if (subList.isEmpty()) {
			logger.fatal("no Subgroups for eduGroup found");
			this.loggingErrorHandler.getMessage().add("no Subgroups for eduGroup found");
			return;
		}
		
		for (Map<String, Object> gd : subList.values()) {
			String nodeType = (String) gd.get("NodeType");

			if (nodeType.equals("{http://www.alfresco.org/model/content/1.0}authorityContainer")) {
				Group lgroup = new Group();
				lgroup.setAuthorityName((String) gd.get("{http://www.alfresco.org/model/content/1.0}authorityName"));
				lgroup.setDisplayName(
						(String) gd.get("{http://www.alfresco.org/model/content/1.0}authorityDisplayName"));
				lgroup.setNodeId((String) gd.get("{http://www.alfresco.org/model/system/1.0}node-uuid"));
				String groupType = (String)gd.get(CCConstants.CCM_PROP_GROUPEXTENSION_GROUPTYPE);
				if(groupType != null) {
					lgroup.setGroupType(groupType);
				}
				this.subGroupList.put(
						(String) gd.get("{http://www.alfresco.org/model/content/1.0}authorityDisplayName"), lgroup);
			}

		}

		//read properties file 
		readProperies();

		// check right to execute foldertemplate function
		if (!checkAuth()) {
			logger.info("No Rights to use FolderTemplate function !");
			loggingErrorHandler.getMessage().add("No Rights to use FolderTemplate function !");
			return;
		}

		Document doc = null;
		try {

			InputStream check = ((MCAlfrescoAPIClient) this.repoClient).getContent(fileNodeId);
			if (!validateTemplate(check)){
				return;
			};

			InputStream i = ((MCAlfrescoAPIClient) this.repoClient).getContent(fileNodeId);
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder;
			builder = factory.newDocumentBuilder();

			try {
				doc = builder.parse(i);

			} catch (IOException | SAXException e) {
				logger.fatal("XML SAX Exception, check XML Temlate");
				loggingErrorHandler.getMessage().add("XML SAX Exception, check XML Temlate");
			}
			Element root = doc.getDocumentElement();

			// call recursive create folder
			createFolder(root, this.folderNodeId);

		} catch (ParserConfigurationException e) {
			loggingErrorHandler.getMessage().add("XML Parse SAX Exception, check XML Temlate");
			logger.fatal("XML Parse SAX Exception, check XML Temlate");
			e.printStackTrace();
		}

		return;
	}

	/**
	 * getMappedGroup
	 * @author hupfer
	 */
	private String getMappedGroup(String groupname, String eduGroup) {

		String tgroupname;
		tgroupname = groupname.replace("ä", "ae");
		tgroupname = tgroupname.replace("ö", "oe");
		tgroupname = tgroupname.replace("ü", "ue");

		String GroupDisplayName = "";

		String grtempl = this.properties.getProperty(tgroupname);
		if (grtempl == null){
			String message="no Org-Admin Group found in template.properties file that matches \""+tgroupname+"\"";
			logger.error(message);
			loggingErrorHandler.getMessage().add(message);
			return "";
		}
		
		String s = grtempl.replace("$GROUPNAME$", groupname);
		
		String eduGroupSuffix = (String)this.properties.getProperty("edugroupsuffix");
		if(eduGroupSuffix == null) eduGroupSuffix = "";
		String orgName = StringUtils.removeEnd(eduGroup, eduGroupSuffix);
		orgName = orgName.trim();
		GroupDisplayName = s.replace("$ORGNAME$", orgName);

		if (GroupDisplayName.isEmpty()) {
			logger.info("GroupDisplayName is empty");
			loggingErrorHandler.getMessage().add("GroupDisplayName is empty");
			return "";
		}

		Group n = this.subGroupList.get(GroupDisplayName);
		if (n == null) {
			logger.info("no Group to map found : " + GroupDisplayName);
			loggingErrorHandler.getMessage().add("no Group to map found : " + GroupDisplayName);
			return "";
		}
		logger.info("found Group: " + n.getDisplayName());
		return n.getAuthorityName();

	}

	/*
	 * createFolder 
	 */
	private void createFolder(Node node, String parentid) {
		
		String pnid = parentid;
		
		String uid = "";
		NodeList nodeList = node.getChildNodes();
		for (int i = 0, len = nodeList.getLength(); i < len; i++) {
			Node currentNode = nodeList.item(i);

			if (currentNode.getNodeType() == Node.ELEMENT_NODE) {

				if (currentNode.getNodeName() == "folder") {
					String fname = currentNode.getAttributes().getNamedItem("name").getNodeValue();
					if (fname.isEmpty()) {
						logger.error("folder without name found");
						return;
					}
					String strInherited = currentNode.getAttributes().getNamedItem("inherited").getNodeValue();
					boolean boolInherited = false;
					if (strInherited.equals("yes")) {
						boolInherited = true;
					}

					TmplGroup tg = new TmplGroup(currentNode);
					List<TGroup> lg = tg.getGroups();

					System.out.println(node.getNodeName() + " " + fname);
					logger.info(node.getNodeName() + " " + fname);

					Map<String, Object> eduProps = new HashMap<>();
					String name = fname;
					eduProps.put(CCConstants.CM_NAME, name);
					eduProps.put(CCConstants.CM_PROP_C_TITLE, name);
					uid = "";
					try {
						Map<String, Object> d = this.repoClient.getChild(parentid, CCConstants.CCM_TYPE_MAP,
								CCConstants.CM_NAME, name);

						if (d != null) {
							uid = (String) d.get(CCConstants.SYS_PROP_NODE_UID);
							logger.info(name + " already exists " + uid);
						}

					} catch (Throwable e1) {
						logger.info(e1.getMessage());
						logger.error(e1.getMessage(),e1);
					}

					if (uid.isEmpty()) {
						try {
							pnid = this.repoClient.createNode(parentid, CCConstants.CCM_TYPE_MAP, eduProps);
							logger.info("folder: "+ name + " created ");

							for (TGroup gr : lg) {
								gr.getName();

								String[] Rightarray = gr.getRight().split(",", -1);
								String gname = getMappedGroup(gr.getName(), eduGroup.getGroupDisplayName());

								if (!gname.isEmpty()) {
									permissionService.setPermissions(pnid, gname, Rightarray, boolInherited);
								} else {
									logger.error("no Group found to set ");
								}
							}

						} catch (Exception e) {
							e.printStackTrace();
						}
					} else {
						pnid = uid;
					}
				}
			} // calls this method for all the children which is Element
			createFolder(currentNode, pnid);
		}

	}

	public List<String> getMessage(){
		return loggingErrorHandler.getMessage();
	}
}
