package org.edu_sharing.repository.update;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alfresco.model.ContentModel;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.repository.MLText;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchParameters;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.namespace.QName;
import org.apache.log4j.Logger;
import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.*;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.MCAlfrescoAPIClient;
import org.edu_sharing.repository.server.tools.UserEnvironmentTool;

public class SchoolContext {

	Logger logger = Logger.getLogger(SchoolContext.class);

	ServiceRegistry serviceRegistry = (ServiceRegistry) AlfAppContextGate.getApplicationContext().getBean(
			ServiceRegistry.SERVICE_REGISTRY);
	DictionaryService dictionaryService = serviceRegistry.getDictionaryService();
	NodeService nodeService = serviceRegistry.getNodeService();

	PermissionService permissionService = serviceRegistry.getPermissionService();
	
	Workbook workbook;

	public SchoolContext(String file) {
		try {

			if (!new MCAlfrescoAPIClient().isAdmin()) {
				throw new Exception("You should be an admin!!!");
			}

			if (file != null && !file.trim().equals("")) {
				FileInputStream fis = new FileInputStream(new File(file));
				init(fis);
			}
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}
	
	public SchoolContext(InputStream in) {
		init(in);
	}
	
	public void init(InputStream in) {
		try {
			workbook = WorkbookFactory.create(in);
		}catch(EncryptedDocumentException e) {
			logger.error(e.getMessage(), e);
		}catch(IOException e) {
			logger.error(e.getMessage(), e);
		}
	}
	
	public static String TYPE_FEDERALSTATE = "{http://www.edu-sharing.net/model/essc/1.0}federalstate";
	public static String PROP_FEDERALSTATE_KEY = "{http://www.edu-sharing.net/model/essc/1.0}federalstate_key";

	public static String TYPE_TYPEOFSCHOOL = "{http://www.edu-sharing.net/model/essc/1.0}typeofschool";
	public static String PROP_TYPEOFSCHOOL_KEY = "{http://www.edu-sharing.net/model/essc/1.0}typeofschool_key";

	public static String TYPE_DISCIPLINE = "{http://www.edu-sharing.net/model/essc/1.0}discipline";
	public static String PROP_DISCIPLINE_KEY = "{http://www.edu-sharing.net/model/essc/1.0}discipline_key";

	public static String TYPE_AGEGROUP = "{http://www.edu-sharing.net/model/essc/1.0}agegroup";

	public static String TYPE_SCHOOLCONTEXT = "{http://www.edu-sharing.net/model/essc/1.0}schoolcontext";
	public static String PROP_REF_FEDERALSTATE = "{http://www.edu-sharing.net/model/essc/1.0}ref_federalstate";
	public static String PROP_REF_TYPEOFSCHOOL = "{http://www.edu-sharing.net/model/essc/1.0}ref_typeofschool";
	public static String PROP_REF_DISCIPLINE = "{http://www.edu-sharing.net/model/essc/1.0}ref_discipline";
	public static String PROP_REF_AGEGROUP = "{http://www.edu-sharing.net/model/essc/1.0}ref_agegroup";

	public void createEntities() {

		createEntities(0, 0, 1, TYPE_FEDERALSTATE, PROP_FEDERALSTATE_KEY);
		createEntities(1, 0, 1, TYPE_TYPEOFSCHOOL, PROP_TYPEOFSCHOOL_KEY);
		createEntities(2, 0, 1, TYPE_DISCIPLINE, PROP_DISCIPLINE_KEY);
		createEntities(3, 0, 1, TYPE_AGEGROUP, null);
	
		logger.info("finished");
	}

	NodeRef getSchoolContextFolder() throws Throwable {
		String sysFolder = new UserEnvironmentTool("admin").getEdu_SharingSystemFolderBase();

		String sysFolderName = "Edu_Sharing_SchoolContext";

		NodeRef sysFolderParent = new NodeRef(MCAlfrescoAPIClient.storeRef, sysFolder);

		// sysfolder
		NodeRef sysFolderNodeRef = nodeService.getChildByName(sysFolderParent, ContentModel.ASSOC_CONTAINS,
				sysFolderName);

		if (sysFolderNodeRef == null) {
			Map<QName, Serializable> rootFolderProps = new HashMap<>();
			rootFolderProps.put(ContentModel.PROP_NAME, sysFolderName);
			sysFolderNodeRef = nodeService.createNode(sysFolderParent, ContentModel.ASSOC_CONTAINS,
					QName.createQName(sysFolderName), QName.createQName(CCConstants.CCM_TYPE_MAP), rootFolderProps)
					.getChildRef();
			
			permissionService.setPermission(sysFolderNodeRef, PermissionService.ALL_AUTHORITIES, PermissionService.READ, true);
		}
		return sysFolderNodeRef;
	}

	NodeRef getEntityFolder(String entityFolderName) throws Throwable {
		NodeRef sysFolderNodeRef = getSchoolContextFolder();
		NodeRef entityFolderNodeRef = nodeService.getChildByName(sysFolderNodeRef, ContentModel.ASSOC_CONTAINS,
				entityFolderName);
		if (entityFolderNodeRef == null) {
			Map<QName, Serializable> props = new HashMap<>();
			props.put(ContentModel.PROP_NAME, entityFolderName);
			entityFolderNodeRef = nodeService.createNode(sysFolderNodeRef, ContentModel.ASSOC_CONTAINS,
					QName.createQName(entityFolderName), QName.createQName(CCConstants.CCM_TYPE_MAP), props)
					.getChildRef();
			permissionService.setPermission(entityFolderNodeRef, PermissionService.ALL_AUTHORITIES, PermissionService.READ, true);
		}
		return entityFolderNodeRef;
	}

	NodeRef getRelationsFolder() throws Throwable {

		NodeRef sysFolderNodeRef = getSchoolContextFolder();
		
		// relation folder
		String relationsFolderName = "relations";
		NodeRef relationsFolderNodeRef = nodeService.getChildByName(sysFolderNodeRef, ContentModel.ASSOC_CONTAINS,
				relationsFolderName);
		if (relationsFolderNodeRef == null) {
			Map<QName, Serializable> props = new HashMap<>();
			props.put(ContentModel.PROP_NAME, relationsFolderName);
			relationsFolderNodeRef = nodeService.createNode(sysFolderNodeRef, ContentModel.ASSOC_CONTAINS,
					QName.createQName(relationsFolderName), QName.createQName(CCConstants.CCM_TYPE_MAP), props)
					.getChildRef();
			permissionService.setPermission(relationsFolderNodeRef, PermissionService.ALL_AUTHORITIES, PermissionService.READ, true);
		}
		return relationsFolderNodeRef;
	}

	public String createEntities(int exceltab, int keyCol, int nameCol, String type, String keyProp) {

		try {

			Map<String, String> keyNamesMap = new HashMap<>();
			Sheet sheetBL = workbook.getSheetAt(exceltab);
			logger.info("nr:" + exceltab + " name:" + sheetBL.getSheetName());

			for (Row row : sheetBL) {

				Cell keyCell = row.getCell(keyCol);
				
				//end of content
				if(keyCell == null){
					break;
				}
				
				if ((CellType.STRING != keyCell.getCellType())
						&& (CellType.NUMERIC != keyCell.getCellType())) {
					continue;
				}

				Cell nameCell = row.getCell(nameCol);
				if ((CellType.STRING != nameCell.getCellType())
						&& (CellType.NUMERIC != nameCell.getCellType())) {
					continue;
				}

				String key = null;
				if (CellType.STRING == keyCell.getCellType()) {
					key = keyCell.getStringCellValue();
				} else if (CellType.NUMERIC == keyCell.getCellType()) {
					key = new Integer(new Double(keyCell.getNumericCellValue()).intValue()).toString();
				}

				String name = null;
				if (CellType.STRING == nameCell.getCellType()) {
					name = nameCell.getStringCellValue();
				} else if (CellType.NUMERIC == nameCell.getCellType()) {
					name = new Integer(new Double(nameCell.getNumericCellValue()).intValue()).toString();
				}

				// dont import
				if (type.equals(TYPE_DISCIPLINE) && key.matches("[A-Z][0-9]")) {
					logger.info("will not import:" + key + " " + name);
					continue;
				}

				keyNamesMap.put(key.trim(), name.trim());

			}

			// entityfolder
			String entityFolderName = type.split("}")[1];
			NodeRef entityFolderNodeRef = getEntityFolder(entityFolderName);

			for (Map.Entry<String, String> keyName : keyNamesMap.entrySet()) {

				Map<QName, Serializable> props = new HashMap<>();

				if (keyProp != null) {
					props.put(QName.createQName(keyProp), keyName.getKey());
				}

				props.put(ContentModel.PROP_TITLE, keyName.getValue());

				String name = keyName.getKey();
			
				props.put(ContentModel.PROP_NAME, name);

				NodeRef entityNodeRef = nodeService.getChildByName(entityFolderNodeRef, ContentModel.ASSOC_CONTAINS,
						name);

				if (entityNodeRef == null) {
					logger.info("will create Entity:" + name + " " + keyName.getValue());
					NodeRef entityObjNodeRef = nodeService.createNode(entityFolderNodeRef, ContentModel.ASSOC_CONTAINS, QName.createQName(name),
							QName.createQName(type), props).getChildRef();
					permissionService.setPermission(entityObjNodeRef, PermissionService.ALL_AUTHORITIES, PermissionService.READ, true);
					
				} else {
					String oldTitle = getString(nodeService.getProperty(entityNodeRef, ContentModel.PROP_TITLE));
					if (!oldTitle.equals(keyName.getValue())) {
						logger.info("will update Entity:" + name + " " + keyName.getValue());
						nodeService.setProperty(entityNodeRef, ContentModel.PROP_TITLE, keyName.getValue());
					}
				}

			}

		} catch (Throwable e) {
			e.printStackTrace();
		}

		return null;
	}

	public void createRelations(int relationTab, int typeOfSchoolKeyCol, int disciplineKeyCol) {
		logger.info("start creating relations");
		// Prepare
		Map<String, NodeRef> typeOfSchoolsMap = new HashMap<>();
		List<NodeRef> typeOfSchools = getNodeRefs(TYPE_TYPEOFSCHOOL);
		for (NodeRef nodeRef : typeOfSchools) {
			typeOfSchoolsMap.put((String) nodeService.getProperty(nodeRef, QName.createQName(PROP_TYPEOFSCHOOL_KEY)),
					nodeRef);
		}

		Map<String, NodeRef> disciplinesMap = new HashMap<>();
		List<NodeRef> disciplines = getNodeRefs(TYPE_DISCIPLINE);
		for (NodeRef nodeRef : disciplines) {
			disciplinesMap.put((String) nodeService.getProperty(nodeRef, QName.createQName(PROP_DISCIPLINE_KEY)),
					nodeRef);
		}

		Sheet relationSheet = workbook.getSheetAt(relationTab);
		int rowCounter = 0;

		try {
			NodeRef relationsFolderNodeRef = getRelationsFolder();

			ArrayList<String> processedTosDis = new ArrayList<>();
			for (Row row : relationSheet) {
				// 0 is the header row
				if (rowCounter == 0){
					rowCounter++;
					continue;
				}
					
				String tosKey = row.getCell(typeOfSchoolKeyCol).getStringCellValue();
				String dKey = row.getCell(disciplineKeyCol).getStringCellValue();

				NodeRef tosNodeRef = typeOfSchoolsMap.get(tosKey);
				NodeRef dNodeRef = disciplinesMap.get(dKey);
				
				if(tosNodeRef == null || dNodeRef == null){
					logger.info("found no nodeRef for typeOfSchoolKey:"+tosKey+" nodeRef:"+tosNodeRef+" or disciplineKey:"+dKey+" nodeRef:"+dNodeRef);
					continue;
				}

				String baseQuery = "TYPE:\"" + SchoolContext.TYPE_SCHOOLCONTEXT + "\"";

				String query = baseQuery;

				query += " AND @essc\\:ref_typeofschool:\"" + tosNodeRef.toString() + "\"";
				query += " AND @essc\\:ref_discipline:\"" + dNodeRef.toString() + "\"";

				SearchService searchService = serviceRegistry.getSearchService();

				ResultSet rs = searchService.query(MCAlfrescoAPIClient.storeRef, SearchService.LANGUAGE_SOLR_ALFRESCO,
						query);

				String checkProcessed = tosNodeRef.toString() + dNodeRef.toString();

				if (rs.length() == 0 && !processedTosDis.contains(checkProcessed)) {

					Map<QName, Serializable> props = new HashMap<>();

					props.put(QName.createQName(PROP_REF_TYPEOFSCHOOL), tosNodeRef);
					props.put(QName.createQName(PROP_REF_DISCIPLINE), dNodeRef);

					String name = tosNodeRef.toString();
					name = name + dNodeRef.toString();
					String hash = new Integer(name.hashCode()).toString();
					props.put(ContentModel.PROP_NAME, hash);

					String assocName = hash;

					logger.info("creating typeOfSchool:" + tosKey + " and discipline:" + dKey);
					NodeRef relNodeRef = nodeService.createNode(getRelationContainer(relationsFolderNodeRef, processedTosDis.size()),
							ContentModel.ASSOC_CONTAINS, QName.createQName(assocName),
							QName.createQName(TYPE_SCHOOLCONTEXT), props).getChildRef();
					permissionService.setPermission(relNodeRef, PermissionService.ALL_AUTHORITIES, PermissionService.READ, true);
					

					processedTosDis.add(checkProcessed);
				} else {
					logger.info("typeOfSchool:" + tosKey + " and discipline:" + dKey + " combination already done");
				}

				rowCounter++;
			}
			logger.info(processedTosDis.size() + " combinations created!");
		} catch (Throwable e) {
			logger.error(e.getMessage(),e);
		}

	}

	/**
	 * @deprecated
	 * 
	 * @param typeOfSchoolDisciplineInclusion
	 */
	public void combineTypeOfSchool(Map<String, String[]> typeOfSchoolDisciplineInclusion) {
		logger.info("starting");
		try {
			NodeRef relationsFolderNodeRef = getRelationsFolder();
			List<NodeRef> typeOfSchools = getNodeRefs(TYPE_TYPEOFSCHOOL);
			List<NodeRef> disciplines = getNodeRefs(TYPE_DISCIPLINE);

			ArrayList<String> exclusionsForCommonSchoolTypes = new ArrayList<>();
			for (Map.Entry<String, String[]> entry : typeOfSchoolDisciplineInclusion.entrySet()) {
				exclusionsForCommonSchoolTypes.addAll(Arrays.asList(entry.getValue()));
			}

			int counter = 0;
			for (NodeRef typeOfSchool : typeOfSchools) {

				String schoolTypeName = getString(nodeService.getProperty(typeOfSchool, ContentModel.PROP_TITLE));

				String[] disciplineInclusion = typeOfSchoolDisciplineInclusion.get(schoolTypeName);

				for (NodeRef discipline : disciplines) {

					String disciplineName = getString(nodeService.getProperty(discipline, ContentModel.PROP_TITLE));

					if (exclusionsForCommonSchoolTypes.contains(disciplineName)
							&& (disciplineInclusion == null || !Arrays.asList(disciplineInclusion).contains(
									disciplineName))) {
						logger.info("ignoring discipline " + disciplineName + " for " + schoolTypeName);
						continue;
					}

					Map<QName, Serializable> props = new HashMap<>();

					props.put(QName.createQName(PROP_REF_TYPEOFSCHOOL), typeOfSchool);
					props.put(QName.createQName(PROP_REF_DISCIPLINE), discipline);

					String name = ""; 

					if (typeOfSchool != null) {
						name = name + typeOfSchool.toString();
					} else {
						name = name + "null";
					}

					if (discipline != null) {
						name = name + discipline.toString();
					} else {
						name = name + "null";
					}

					String hash = new Integer(name.hashCode()).toString();

					props.put(ContentModel.PROP_NAME, hash);

					String assocName = hash;

					NodeRef relNodeRef = nodeService.createNode(getRelationContainer(relationsFolderNodeRef, counter),
							ContentModel.ASSOC_CONTAINS, QName.createQName(assocName),
							QName.createQName(TYPE_SCHOOLCONTEXT), props).getChildRef();
					
					permissionService.setPermission(relNodeRef, PermissionService.ALL_AUTHORITIES, PermissionService.READ, true);

					counter++;
					logger.info("counter:" + counter);
				}
			}

		} catch (Throwable e) {
			e.printStackTrace();
		}
		logger.info("finished");

	}

	String getString(Serializable value) {
		if (value instanceof MLText) {
			return ((MLText) value).getDefaultValue();
		} else {
			return value.toString();
		}
	}

	/**
	 * @deprecated
	 * 
	 * @param federalStatesParam
	 */
	public void combine(NodeRef[] federalStatesParam) {

		try {
			NodeRef relationsFolderNodeRef = getRelationsFolder();

			List<NodeRef> federalStates = getNodeRefs(TYPE_FEDERALSTATE);
			federalStates.add(null);
			List<NodeRef> typeOfSchools = getNodeRefs(TYPE_TYPEOFSCHOOL);
			typeOfSchools.add(null);
			List<NodeRef> disciplines = getNodeRefs(TYPE_DISCIPLINE);
			disciplines.add(null);
			List<NodeRef> ageGroups = getNodeRefs(TYPE_AGEGROUP);
			ageGroups.add(null);

			logger.info("federalStates: " + federalStates.size() + " typeOfSchools:" + typeOfSchools.size()
					+ " disciplines:" + disciplines.size() + " ageGroups:" + ageGroups.size());

			int counter = 0;

			for (NodeRef federalState : federalStates) {

				if (federalStatesParam != null && !Arrays.asList(federalStatesParam).contains(federalState)) {
					continue;
				}

				Serializable federalStateName = (federalState != null) ? nodeService.getProperty(federalState,
						ContentModel.PROP_TITLE) : "";

				for (NodeRef typeOfSchool : typeOfSchools) {
					for (NodeRef discipline : disciplines) {
						for (NodeRef ageGroup : ageGroups) {

							if ((counter % 1000) != 0) {
								counter++;
								continue;
							}

							Map<QName, Serializable> props = new HashMap<>();
							props.put(QName.createQName(PROP_REF_FEDERALSTATE), federalState);
							props.put(QName.createQName(PROP_REF_TYPEOFSCHOOL), typeOfSchool);
							props.put(QName.createQName(PROP_REF_DISCIPLINE), discipline);
							props.put(QName.createQName(PROP_REF_AGEGROUP), ageGroup);

							String name = ""; 

							if (federalState != null) {
								name = name + federalState.toString();
							} else {
								name = name + "null";
							}

							if (typeOfSchool != null) {
								name = name + typeOfSchool.toString();
							} else {
								name = name + "null";
							}

							if (discipline != null) {
								name = name + discipline.toString();
							} else {
								name = name + "null";
							}

							if (ageGroup != null) {
								name = name + ageGroup.toString();
							} else {
								name = name + "null";
							}

							String hash = new Integer(name.hashCode()).toString();

							props.put(ContentModel.PROP_NAME, hash);

							String assocName = hash;

							NodeRef relNodeRef = nodeService.createNode(getRelationContainer(relationsFolderNodeRef, counter),
									ContentModel.ASSOC_CONTAINS, QName.createQName(assocName),
									QName.createQName(TYPE_SCHOOLCONTEXT), props).getChildRef();
							
							permissionService.setPermission(relNodeRef, PermissionService.ALL_AUTHORITIES, PermissionService.READ, true);

							counter++;
							String federalStateAsString = (federalStateName instanceof MLText) ? ((MLText) federalStateName)
									.getDefaultValue() : (String) federalStateName;
							logger.info("created " + counter + " relations.federal state:" + federalStateAsString);
						}
					}
				}
			}
		} catch (Throwable e) {
			e.printStackTrace();
		}

	}

	NodeRef getRelationContainer(NodeRef relationRoot, int counter) {

		String foldername = new Integer(counter / 1000).toString();

		NodeRef result = nodeService.getChildByName(relationRoot, ContentModel.ASSOC_CONTAINS, foldername);
		if (result == null) {
			Map<QName, Serializable> props = new HashMap<>();
			props.put(ContentModel.PROP_NAME, foldername);

			result = nodeService.createNode(relationRoot, ContentModel.ASSOC_CONTAINS,
					QName.createQName(QName.createValidLocalName(foldername)),
					QName.createQName(CCConstants.CCM_TYPE_MAP), props).getChildRef();
			
			permissionService.setPermission(result, PermissionService.ALL_AUTHORITIES, PermissionService.READ, true);

		}
		return result;
	}

	public List<NodeRef> getNodeRefs(String type) {

		SearchService searchService = serviceRegistry.getSearchService();

		SearchParameters sp = new SearchParameters();
		sp.setQuery("TYPE:\"" + type + "\"");
		sp.addStore(MCAlfrescoAPIClient.storeRef);
		sp.setLanguage(SearchService.LANGUAGE_LUCENE);
		ResultSet rs = searchService.query(sp);

		return rs.getNodeRefs();
	}

}
