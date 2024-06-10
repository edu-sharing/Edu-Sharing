package org.edu_sharing.repository.server.importer;

import java.io.InputStream;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.stream.Stream;

import org.alfresco.model.ContentModel;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.dictionary.PropertyDefinition;
import org.alfresco.service.cmr.repository.*;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchParameters;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.QName;
import org.apache.log4j.Logger;
import org.apache.poi.ss.usermodel.*;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.client.tools.MimeTypes;
import org.edu_sharing.repository.client.tools.StringTool;
import org.edu_sharing.repository.server.MCAlfrescoAPIClient;
import org.edu_sharing.repository.server.tools.forms.DuplicateFinder;
import org.edu_sharing.service.clientutils.ClientUtilsService;
import org.edu_sharing.service.clientutils.WebsiteInformation;
import org.edu_sharing.service.collection.CollectionServiceFactory;


public class ExcelLOMImporter {

	Logger logger = Logger.getLogger(ExcelLOMImporter.class);
	
	Map<String,String> excelAlfMap = null;
	
	
	ServiceRegistry serviceRegistry = (ServiceRegistry)AlfAppContextGate.getApplicationContext().getBean(ServiceRegistry.SERVICE_REGISTRY);
	DictionaryService dictionaryService = serviceRegistry.getDictionaryService(); 
	NodeService nodeService = serviceRegistry.getNodeService();
	
	String targetFolder = null;
	
	InputStream is = null;
	
	int maxNodesInFolder = 100;
	
	MCAlfrescoAPIClient apiClient = new MCAlfrescoAPIClient();
	
	private Map<String, Map<String, Object>> currentLevelObjects = null;

	private int rowCount;
	
	public int getRowCount() {
		return rowCount;
	}

	QName qnameWWWUrl = QName.createQName(CCConstants.CCM_PROP_IO_WWWURL);
	QName qnameTitle = QName.createQName(CCConstants.LOM_PROP_GENERAL_TITLE);
	QName qnameLicenseKey = QName.createQName(CCConstants.CCM_PROP_IO_COMMONLICENSE_KEY);
	QName qnameThumbnail = QName.createQName(CCConstants.CCM_PROP_IO_THUMBNAILURL);


	public ExcelLOMImporter(String targetFolder, InputStream is, Boolean addToCollection) throws Exception {
		
		this.targetFolder = targetFolder;
		this.is = is;
		
		
		Map<Integer,String> IdxColumnMap = new HashMap<>();
		
		try{
			Workbook workbook = WorkbookFactory.create(this.is);
			
			Sheet sheet = workbook.getSheetAt(0);
			
			rowCount = 0;
			String parentFolder = targetFolder;
			
			NodeRef targetFolderNodeRef = new NodeRef(MCAlfrescoAPIClient.storeRef,targetFolder);
			QName assocTypeContains = QName.createQName(CCConstants.CM_ASSOC_FOLDER_CONTAINS);
			for(Row row : sheet){
				String folderName = new Integer(rowCount / maxNodesInFolder).toString();
				NodeRef currentFolder = nodeService.getChildByName(targetFolderNodeRef, assocTypeContains, folderName);
				
				if(currentFolder == null){
					Map<QName,Serializable> folderProps = new HashMap<>();
					folderProps.put(QName.createQName(CCConstants.CM_NAME), folderName);
					folderProps.put(QName.createQName(CCConstants.CM_PROP_C_TITLE), folderName);
					parentFolder = nodeService.createNode(targetFolderNodeRef,assocTypeContains, QName.createQName(folderName),  QName.createQName(CCConstants.CCM_TYPE_MAP),folderProps).getChildRef().getId();
				}
				
				try{
					currentLevelObjects = apiClient.getChildren(parentFolder);
				}catch(Throwable e){
					e.printStackTrace();
				}
				
				if(IdxColumnMap.size() > 0){
					//we got the headers
					Map<QName,Serializable> toSafe = new HashMap<>();
					
					String contentUrl = null;
					LinkedHashSet<String> collectionsToImportTo = new LinkedHashSet<>();
					for(Cell cell : row){
						
						int colIdxIdx = cell.getColumnIndex();
						
						if(CellType.STRING != cell.getCellType()){
							continue;
						}

						String columnName = IdxColumnMap.get(colIdxIdx);
						if(columnName == null){
							logger.error("no column name found for column:"+colIdxIdx);
							continue;
						}

						if(columnName.startsWith("collection")){
							String value = cell.getStringCellValue();
							if(value != null){
								collectionsToImportTo.add(value);
							}
						}

						//System.out.println(columnName + " " + toSafe.get(QName.createQName(CCConstants.CM_NAME)) + " " + cell.getStringCellValue() + " colIdx:" + colIdxIdx);
						String alfrescoProperty = null;
						String value = cell.getStringCellValue();
						if(value == null) continue;
						value = value.trim();
						if(value.equals("")) continue;
						
						if(columnName != null){
							alfrescoProperty = getExcelAlfMap().get(columnName);
						}
						
						if(alfrescoProperty != null){
							if(alfrescoProperty != null && 
									alfrescoProperty.equals(CCConstants.CM_PROP_CONTENT)){
								contentUrl = value;
							}else{
								PropertyDefinition propDef = dictionaryService.getProperty(QName.createQName(alfrescoProperty));
								
								if(propDef != null) {
									if(propDef.isMultiValued() && !alfrescoProperty.contains("contributer")){
										ArrayList<String> multival = new ArrayList<>();
										
										//String[] vals = value.split(",");   StringTool.escape(CCConstants.MULTIVALUE_SEPARATOR)
										String[] vals = value.split(StringTool.escape(CCConstants.MULTIVALUE_SEPARATOR));
										multival.addAll(Arrays.asList(vals));
										
										toSafe.put(QName.createQName(alfrescoProperty), multival);
									}else{
										toSafe.put(QName.createQName(alfrescoProperty), value);
									}
								}else {
									logger.error("unkown property: " + alfrescoProperty);
									continue;
								}
							}
						}
						
					}

					//try to get title from wwurl
					String wwwUrl = (String)toSafe.get(qnameWWWUrl);

					String nodeName = addName(toSafe, wwwUrl);
					addThumbnail(toSafe, wwwUrl);

					if(toSafe.size() > 0 && nodeName != null && !nodeName.trim().equals("")){
						
						//check for valid thumbnail url
						boolean createNode = true;
						String thumbUrl = (String)toSafe.get(qnameThumbnail);

						if((thumbUrl == null || !thumbUrl.startsWith("http")) && (contentUrl == null || contentUrl.trim().equals(""))) {
							logger.error("invalid thumbnail url:" + thumbUrl +" for:" +toSafe.get(QName.createQName(CCConstants.CM_NAME))+" will not safe object");
							createNode = false;
						}
						if(createNode) {
							ChildAssociationRef newNode = nodeService.createNode(new NodeRef(MCAlfrescoAPIClient.storeRef,parentFolder),QName.createQName(CCConstants.CM_ASSOC_FOLDER_CONTAINS), QName.createQName(nodeName),  QName.createQName(CCConstants.CCM_TYPE_IO),toSafe);
							
							if(contentUrl != null && !contentUrl.trim().equals("")){
								String mimetype = MimeTypes.guessMimetype(contentUrl);
								try {
									InputStream inputStream = new URL(contentUrl).openConnection().getInputStream();
									apiClient.writeContent(MCAlfrescoAPIClient.storeRef,
											newNode.getChildRef().getId(),
											inputStream,
											mimetype,
											null,
											CCConstants.CM_PROP_CONTENT);
								}catch (java.io.FileNotFoundException e){
									logger.error("no content found for:" + toSafe.get(QName.createQName(CCConstants.CM_NAME))+ "url:" +contentUrl);
								}
							}
						
							apiClient.createVersion(newNode.getChildRef().getId());

							logger.info("node created:" + serviceRegistry.getNodeService().getPath(newNode.getChildRef()));
							addToCollections(newNode,collectionsToImportTo,addToCollection);
						}
					}else {
						logger.error("can not determine name property for row: "+row.getRowNum());
					}
					
				}else{
				//build the headers
					for(Cell cell : row){
						if(CellType.STRING == cell.getCellType()){
							IdxColumnMap.put(cell.getColumnIndex(),cell.getStringCellValue());
						}
					}
				}
				
				rowCount++;
			}
			
			
			
		}catch(Exception e){
			throw e;
		}

	}

	private void addThumbnail(Map<QName, Serializable> toSafe, String wwwUrl) {
		String thumbnailUrl = (String) toSafe.get(qnameThumbnail);
		if(thumbnailUrl == null && wwwUrl != null && wwwUrl.contains("youtu")){
			String youtubeId = getYoutubeId(wwwUrl);
			if(youtubeId != null) {
				thumbnailUrl = "https://img.youtube.com/vi/" + youtubeId + "/0.jpg";
			}
			toSafe.put(qnameThumbnail,thumbnailUrl);
		}
	}

	/**
	 * tries to get title from website title
	 * when no name is present in toSafe map it tries to get name from titel
	 * if still not present it tries to get name from wwwurl
	 * name is cleared to get an alfresco conform name
	 *
	 * @param toSafe
	 * @param wwwUrl
	 * @return
	 */
	private String addName(Map<QName, Serializable> toSafe, String wwwUrl) {
		if(toSafe.get(qnameTitle) == null && wwwUrl != null && wwwUrl.startsWith("http")){
			WebsiteInformation websiteInfo = ClientUtilsService.getWebsiteInformation(wwwUrl);
			if(websiteInfo != null){
				String title = websiteInfo.getTitle();
				toSafe.put(qnameTitle,title);

				if(toSafe.get(qnameLicenseKey) == null){
					if(websiteInfo.getLicense() != null){
						String ccVersion = websiteInfo.getLicense().getCcVersion();
						toSafe.put(qnameLicenseKey,ccVersion);
					}
				}
			}
		}


		String nodeName = (String)toSafe.get(ContentModel.PROP_NAME);
		if(nodeName == null || nodeName.trim().isEmpty()){
			nodeName = (String) toSafe.get(qnameTitle);
		}

		if(nodeName == null || nodeName.trim().isEmpty() && (wwwUrl != null && !wwwUrl.trim().isEmpty())){
			nodeName = wwwUrl;
		}

		if(nodeName == null){
			return null;
		}

		Map<String,Object> eduProps = new HashMap<>();
		eduProps.put(CCConstants.CM_NAME, nodeName);
		eduProps.put(CCConstants.LOM_PROP_GENERAL_TITLE, nodeName);
		new DuplicateFinder().transformToSafeName(currentLevelObjects, eduProps);
		toSafe.put(ContentModel.PROP_NAME, (String)eduProps.get(CCConstants.CM_NAME));
		return (String)toSafe.get(QName.createQName(CCConstants.CM_NAME));
	}

	private void addToCollections(ChildAssociationRef newNode, LinkedHashSet<String> collectionsForNode, Boolean addToCollection){
		String wwwUrl = (String)serviceRegistry.getNodeService().getProperty(newNode.getChildRef(),QName.createQName(CCConstants.CCM_PROP_IO_WWWURL));
		String nodeName = (String)serviceRegistry.getNodeService().getProperty(newNode.getChildRef(), ContentModel.PROP_NAME);
		if(collectionsForNode != null && collectionsForNode.size() > 1){

			String parentCollection = collectionsForNode.stream().findFirst().get();
			String targetCollection = collectionsForNode.stream().skip(collectionsForNode.size() -1).findFirst().get();
			logger.info("collections for node " + String.join("/",collectionsForNode) +" p:"+parentCollection +" c:"+targetCollection);

			SearchParameters searchParameters = new SearchParameters();
			searchParameters.setLanguage(SearchService.LANGUAGE_LUCENE);
			searchParameters.setQuery("ASPECT:\"ccm:collection\" AND @cm\\:name:\"" + targetCollection + "\"");
			searchParameters.setSkipCount(0);
			searchParameters.setLimit(10);
			searchParameters.addStore(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE);

			ResultSet rs = serviceRegistry.getSearchService().query(searchParameters);

			//check if there is a parent
			NodeRef pathMatchesNodeRef = null;
			LinkedHashSet<String> pathsMatch = new LinkedHashSet<>();
			for(NodeRef nodeRef : rs.getNodeRefs()){
				Path path = serviceRegistry.getNodeService().getPath(nodeRef);
				String displayPath = path.toDisplayPath(serviceRegistry.getNodeService(),serviceRegistry.getPermissionService());
				logger.info("checking path for parent collection;\"" + parentCollection + "\";path:"+displayPath +"/"+nodeService.getProperty(nodeRef,ContentModel.PROP_NAME));
				if(displayPath.contains(parentCollection)){
					pathsMatch.add(displayPath);
					pathMatchesNodeRef = nodeRef;
				}
			}

			if(pathsMatch.size() > 1){
				logger.error("more than one path matches;"+nodeName +";"+newNode.getChildRef()+";" + String.join(";",pathsMatch));
			}else if(pathsMatch.size() == 0){
				logger.error("no path matches;"+nodeName +";"+newNode.getChildRef());
			}else{
				logger.info("adding;" + nodeName +";"+newNode.getChildRef() +";TO;" + pathsMatch.iterator().next() +"/"+nodeService.getProperty(pathMatchesNodeRef,ContentModel.PROP_NAME));
				try {
					if(addToCollection) {
						CollectionServiceFactory.getLocalService().addToCollection(pathMatchesNodeRef.getId(), newNode.getChildRef().getId(), null, false);
					}
				} catch (Throwable throwable) {

					logger.error(throwable.getMessage(),throwable);
				}
			}
		}else{
			this.logger.info("addToCollections expects two collection to be defined in excelsheet;" + wwwUrl);
		}
	}

	public static void main(String[] args) {
		String wwwUrl = "https://youtu.be/VMuKmeZCkVQ";
		System.out.println("id1:"+ getYoutubeId(wwwUrl));
		wwwUrl = "https://www.youtube.com/watch?v=VMuKmeZCkVQ&feature=youtu.be";
		System.out.println("id2:"+ getYoutubeId(wwwUrl));
	}

	private static String getYoutubeId(String wwwUrl){
		try {
			if(wwwUrl.startsWith("https://youtu.be")){
				URL url = new URL(wwwUrl);
				String id = url.getPath();
				id = id.replaceAll("/","");
				return id;
			}else if(wwwUrl.startsWith("https://www.youtube")){

				URL url = new URL(wwwUrl);
				String queryString = url.getQuery();
				String id = Stream.of(queryString.split("&")).map(kv -> kv.split("=")).filter(kv -> "v".equalsIgnoreCase(kv[0])).map(kv -> kv[1])
						.findFirst()
						.orElse("");
				return id;
			}else{
				return null;
			}

		} catch (MalformedURLException e) {
			return null;
		}
	}
	
	public Map<String, String> getExcelAlfMap() {
		if(excelAlfMap == null){
			excelAlfMap = new HashMap<>();
			excelAlfMap.put("catalog", CCConstants.CCM_PROP_IO_REPLICATIONSOURCE);
			excelAlfMap.put("identifier", CCConstants.CCM_PROP_IO_REPLICATIONSOURCEID);
			excelAlfMap.put("datestamp", CCConstants.CCM_PROP_IO_REPLICATIONSOURCETIMESTAMP);
			excelAlfMap.put("name", CCConstants.CM_NAME);
			excelAlfMap.put("title", CCConstants.LOM_PROP_GENERAL_TITLE);
			excelAlfMap.put("language", CCConstants.LOM_PROP_GENERAL_LANGUAGE);
			excelAlfMap.put("description", CCConstants.LOM_PROP_GENERAL_DESCRIPTION);
			excelAlfMap.put("keyword", CCConstants.LOM_PROP_GENERAL_KEYWORD);
			excelAlfMap.put("context", CCConstants.CCM_PROP_IO_REPL_EDUCATIONAL_CONTEXT);
			excelAlfMap.put("educationalIntendedenduserrole", CCConstants.CCM_PROP_IO_REPL_EDUCATIONAL_INTENDEDENDUSERROLE);
			excelAlfMap.put("educationalTypicalAgeRangeFrom", CCConstants.CCM_PROP_IO_REPL_EDUCATIONAL_TYPICALAGERANGEFROM);
			excelAlfMap.put("educationalTypicalAgeRangeTo", CCConstants.CCM_PROP_IO_REPL_EDUCATIONAL_TYPICALAGERANGETO);
			excelAlfMap.put("educationalTypicalAgeRange", CCConstants.CCM_PROP_IO_REPL_EDUCATIONAL_TYPICALAGERANGE);
			excelAlfMap.put("lifeCycleContributerAuthor", CCConstants.CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_AUTHOR);
			excelAlfMap.put("lifeCycleContributerPublisher", CCConstants.CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_PUBLISHER);
			excelAlfMap.put("metadataContributerProvider", CCConstants.CCM_PROP_IO_REPL_METADATACONTRIBUTER_PROVIDER);
			excelAlfMap.put("metadataContributerCreator", CCConstants.CCM_PROP_IO_REPL_METADATACONTRIBUTER_CREATOR);
			excelAlfMap.put("technicalFormat", CCConstants.LOM_PROP_TECHNICAL_FORMAT);
			excelAlfMap.put("technicalLocation", CCConstants.LOM_PROP_TECHNICAL_LOCATION);
			excelAlfMap.put("wwwurl", CCConstants.CCM_PROP_IO_WWWURL);
			excelAlfMap.put("contentUrl",CCConstants.CM_PROP_CONTENT);
			excelAlfMap.put("educationalLearningResourceType", CCConstants.CCM_PROP_IO_REPL_EDUCATIONAL_LEARNINGRESSOURCETYPE);
			excelAlfMap.put("RightsCopyrightAndOtherRestrictions", CCConstants.LOM_PROP_RIGHTS_COPY_RIGHT);
			excelAlfMap.put("RightsDescription", CCConstants.LOM_PROP_RIGHTS_RIGHTS_DESCRIPTION);
			excelAlfMap.put("thumbnailUrl", CCConstants.CCM_PROP_IO_THUMBNAILURL);
			excelAlfMap.put("taxonId",CCConstants.CCM_PROP_IO_REPL_TAXON_ID);
			excelAlfMap.put("taxonEntry",CCConstants.CCM_PROP_IO_REPL_TAXON_ENTRY);
			excelAlfMap.put("licenseKey",CCConstants.CCM_PROP_IO_COMMONLICENSE_KEY);
			excelAlfMap.put("licenseVersion",CCConstants.CCM_PROP_IO_COMMONLICENSE_CC_VERSION);
			excelAlfMap.put("licenseLocale",CCConstants.CCM_PROP_IO_COMMONLICENSE_CC_LOCALE);
			excelAlfMap.put("licenseSourceUrl",CCConstants.CCM_PROP_IO_LICENSE_SOURCE_URL);
			excelAlfMap.put("licenseTitleOfWork",CCConstants.CCM_PROP_IO_LICENSE_TITLE_OF_WORK);
			excelAlfMap.put("licenseTo",CCConstants.CCM_PROP_IO_LICENSE_TO);
			excelAlfMap.put("licenseValid",CCConstants.CCM_PROP_IO_LICENSE_VALID);
			excelAlfMap.put("originUniversity",CCConstants.CCM_PROP_IO_UNIVERSITY);
			excelAlfMap.put("metadataset",CCConstants.CM_PROP_METADATASET_EDU_METADATASET);
			excelAlfMap.put("oeh_widgets","{" + CCConstants.NAMESPACE_CCM+"}oeh_widgets");
		}
		return excelAlfMap;
	}
	
	
	
}
