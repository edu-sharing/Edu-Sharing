/**
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 */
package org.edu_sharing.repository.server.importer;

import java.text.SimpleDateFormat;
import java.util.*;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.alfresco.service.cmr.security.AuthorityType;
import org.apache.log4j.Logger;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.client.tools.forms.VCardTool;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.repository.server.tools.VCardConverter;
import org.edu_sharing.service.nodeservice.NodeServiceHelper;
import org.joda.time.DateTime;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class RecordHandlerLOM implements RecordHandlerInterface {

	Logger logger = Logger.getLogger(RecordHandlerLOM.class);
	XPathFactory pfactory = XPathFactory.newInstance();
	XPath xpath = pfactory.newXPath();

	DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

	boolean askElixier = false;

	String metadataSetId = null;

	HashMap toSafeMap = new HashMap();
	List<String> toRemoveList = new ArrayList<>();

	String metadataPrefix = null;

	@Override
	public boolean createSubobjects() {
		return false;
	}

	public RecordHandlerLOM(String metadataSetId) {
		this(metadataSetId,null);
	}

	public RecordHandlerLOM(String metadataSetId, String metadataPrefix) {

		this.askElixier = askElixier;

		this.metadataPrefix = metadataPrefix;

		if(metadataSetId == null || metadataSetId.trim().equals("")){
			metadataSetId = "default";
		}

		this.metadataSetId = metadataSetId;
	}


	public void handleRecord(Node nodeRecord, String cursor, String set) throws Throwable {

		xpath.reset();

		toSafeMap.clear();

		String replicationTimeStamp = (String) xpath.evaluate("header/datestamp", nodeRecord, XPathConstants.STRING);

		String lomNamespace = (String)xpath.evaluate("metadata/lom/@schemaLocation",nodeRecord,XPathConstants.STRING);

		/**
		 * general
		 */

		String replicationId = null;
		String lomCatalogId = null;

		List<Map<String, Object>> generalIdentifierList = new ArrayList<>();
		NodeList identifierNodeList = (NodeList) xpath.evaluate("metadata/lom/general/identifier", nodeRecord, XPathConstants.NODESET);
		for(int i = 0; i < identifierNodeList.getLength(); i++){
			// general identifier
			String tmpReplicationId = (String) xpath.evaluate("entry", identifierNodeList.item(i), XPathConstants.STRING);
			String tmpLomCatalogId = (String) xpath.evaluate("catalog", identifierNodeList.item(i), XPathConstants.STRING);

			if(i == 0){
				replicationId = tmpReplicationId;
				lomCatalogId = tmpLomCatalogId;
			}

			Map<String, Object> generalIdentifierToSafeMap = new HashMap<>();
			generalIdentifierToSafeMap.put(CCConstants.LOM_PROP_IDENTIFIER_ENTRY, tmpReplicationId);
			generalIdentifierToSafeMap.put(CCConstants.LOM_PROP_IDENTIFIER_CATALOG, tmpLomCatalogId);

			generalIdentifierList.add(generalIdentifierToSafeMap);
		}

		//fallback to dini-ag dialect
		String oerbwNs = "https://www.oerbw.de/hsoerlom";
		if(lomNamespace != null && lomNamespace.trim().contains(oerbwNs)){

			if(replicationId == null || replicationId.trim().equals("")){
				replicationId = (String) xpath.evaluate("metadata/lom/general/identifier", nodeRecord, XPathConstants.STRING);
			}
			if(lomCatalogId == null || lomCatalogId.trim().equals("")){
				lomCatalogId = oerbwNs;
			}
		}

		ArrayList<String> generalTitleI18n = getMultiLangValueNew((Node) xpath.evaluate("metadata/lom/general/title", nodeRecord, XPathConstants.NODE));

		if(generalTitleI18n == null || generalTitleI18n.size() == 0){
			logger.warn("No title for " + replicationId +", will use id as name");
			generalTitleI18n = new ArrayList<>(Collections.singletonList(replicationId));
		}

		List generallanguage = convertListToString((NodeList)xpath.evaluate("metadata/lom/general/language", nodeRecord, XPathConstants.NODESET));

		Node generalNode = (Node) xpath.evaluate("metadata/lom/general", nodeRecord, XPathConstants.NODE);

		List generalKeywords = getMultivalue(generalNode, "keyword");

		// time period when license is valid (not original LOM fields)
		String licenseFromString = (String)xpath.evaluate("header/license/datefrom", nodeRecord, XPathConstants.STRING);
		String licenseToString = (String)xpath.evaluate("header/license/dateto", nodeRecord, XPathConstants.STRING);
		String licenseDescriptionString = (String)xpath.evaluate("header/license/context", nodeRecord, XPathConstants.STRING);
		if(licenseFromString != null && !licenseFromString.trim().equals("")){
			Date licenseFrom = null;
			Date licenseTo = null;
			try {
				SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
				licenseFrom = df.parse(licenseFromString);
				licenseTo = df.parse(licenseToString);

				if (licenseFrom != null) toSafeMap.put(CCConstants.CCM_PROP_IO_LICENSE_FROM, licenseFrom);
				if (licenseTo != null) toSafeMap.put(CCConstants.CCM_PROP_IO_LICENSE_TO, licenseTo);
				if (licenseDescriptionString != null) toSafeMap.put(CCConstants.CCM_PROP_IO_LICENSE_DESCRIPTION, licenseDescriptionString);

				if(licenseFrom != null && licenseTo != null){
					Date today = new Date();
					if(licenseFrom.getTime() > today.getTime() || licenseTo.getTime() < today.getTime() ){
						toSafeMap.put(CCConstants.CCM_PROP_IO_LICENSE_VALID, "false");
					}else{
						toSafeMap.put(CCConstants.CCM_PROP_IO_LICENSE_VALID, "true");
					}
				}
			} catch (Exception e) {
				logger.debug("error wrong lizense date:"+e.getMessage());
			}

		}

		String structureSrc = (String) xpath.evaluate("metadata/lom/general/structure/source", nodeRecord, XPathConstants.STRING);
		String structureVal = (String) xpath.evaluate("metadata/lom/general/structure/value", nodeRecord, XPathConstants.STRING);

		String aggregationLevelSrc = (String) xpath.evaluate("metadata/lom/general/aggregationLevel/source", nodeRecord, XPathConstants.STRING);
		String aggregationLevelVal = (String) xpath.evaluate("metadata/lom/general/aggregationLevel/value", nodeRecord, XPathConstants.STRING);

		// SAFE PART
		// so that it can be found in search
		toSafeMap.put(CCConstants.CCM_PROP_IO_OBJECTTYPE, "0");
		toSafeMap.put("TYPE#" + CCConstants.LOM_TYPE_IDENTIFIER + "#" + CCConstants.LOM_ASSOC_IDENTIFIER, generalIdentifierList);
		toSafeMap.put(CCConstants.CCM_PROP_IO_REPLICATIONSOURCE, lomCatalogId);
		toSafeMap.put(CCConstants.CCM_PROP_IO_REPLICATIONSOURCEID, replicationId);
		toSafeMap.put(CCConstants.CCM_PROP_IO_REPLICATIONSOURCETIMESTAMP, replicationTimeStamp);

		String titleEntry = generalTitleI18n.get(0);
		toSafeMap.put(CCConstants.LOM_PROP_GENERAL_TITLE, titleEntry);
		String name = new String(titleEntry);
		name = NodeServiceHelper.cleanupCmName(name);

		//replace ending dot with nothing
		name = name.replaceAll("[\\.]*$", "");
		name = name.trim();

		toSafeMap.put(CCConstants.CM_NAME, name);
		toSafeMap.put(CCConstants.CM_PROP_C_TITLE, titleEntry);

		toSafeMap.put(CCConstants.LOM_PROP_GENERAL_LANGUAGE, generallanguage);

		toSafeMap.put(CCConstants.CM_PROP_METADATASET_EDU_METADATASET, this.metadataSetId);


		ArrayList<String> generalDescriptionI18n = getMultiLangValueNew((Node) xpath.evaluate("metadata/lom/general/description", nodeRecord, XPathConstants.NODE));

		if(generalDescriptionI18n != null && generalDescriptionI18n.size() > 0){
			toSafeMap.put(CCConstants.LOM_PROP_GENERAL_DESCRIPTION, generalDescriptionI18n.get(0));
		}

		if (generalKeywords != null && generalKeywords.size() > 0) {
			toSafeMap.put(CCConstants.LOM_PROP_GENERAL_KEYWORD, generalKeywords);
		}

		// @TODO structureSrc
		// toSafeMap.put(CCConstants.LOM_PROP_GENERAL_STRUCTURE_SRC,
		// structureSrc);
		if(structureVal != null && !structureVal.equals("")){
			toSafeMap.put(CCConstants.LOM_PROP_GENERAL_STRUCTURE, structureVal);
		}
		// @TODO aggregationLevelVal
		// toSafeMap.put(CCConstants.LOM_PROP_GENERAL_AGGREGATIONLEVEL_SRC,
		// aggregationLevelSrc);
		if(aggregationLevelVal != null && !aggregationLevelVal.trim().equals("")){
			toSafeMap.put(CCConstants.LOM_PROP_GENERAL_AGGREGATIONLEVEL, aggregationLevelVal);
		}

		/**
		 * lifecycle
		 */
		Node lifecycleNode = (Node) xpath.evaluate("metadata/lom/lifeCycle", nodeRecord, XPathConstants.NODE);
		if (lifecycleNode != null) {
			Node versionNode = (Node) xpath.evaluate("version", lifecycleNode, XPathConstants.NODE);

			String lifecycleStatusSource = (String) xpath.evaluate("status/source", lifecycleNode, XPathConstants.STRING);
			String lifecycleStatusValue = (String) xpath.evaluate("status/value", lifecycleNode, XPathConstants.STRING);

			// lifecycleContribute
			ArrayList<Map<String, Object>> lifecycleContributes;

			/**
			 * search replication preferred from LOM
			 */

			Map<String,ArrayList<String>> replLifecycleContributer = new HashMap<>();
			lifecycleContributes = getContributes(lifecycleNode);
			if(lifecycleContributes != null){

				ArrayList<Map<String, Object>> assignedLicenses = new ArrayList<>();
				List<String> assignedLicensesAuthorities = new ArrayList<>();
				Date assignedLicenseExpiryDate = null;

				for (Map<String, Object> contr : lifecycleContributes) {

					String role = (String) contr.get(CCConstants.LOM_PROP_CONTRIBUTE_ROLE);
					String entity = (String) contr.get(CCConstants.LOM_PROP_CONTRIBUTE_ENTITY);
					Date dateObj = (Date) contr.get(CCConstants.LOM_PROP_CONTRIBUTE_DATE);
					SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
					String date = (dateObj != null) ? sdf.format(dateObj) : null;
					if(role != null){
						String lc_property = CCConstants.getLifecycleContributerProp(role.trim());
						if(lc_property != null && !lc_property.trim().equals("")){
							ArrayList<String> tmpLCList = replLifecycleContributer.get(lc_property);
							if(tmpLCList == null) tmpLCList = new ArrayList<>();

							/**
							 * add Date
							 */

							if(date != null){
								ArrayList<Map<String, Object>> vards = VCardConverter.vcardToMap(entity);
								if(vards != null && vards.size() > 0) {
									vards.get(0).put(CCConstants.VCARD_EXT_LOM_CONTRIBUTE_DATE, date);
									entity = VCardTool.hashMap2VCard((HashMap)vards.get(0));
								}

								if(role.equals("terminator") && toSafeMap.get(CCConstants.CCM_PROP_IO_LICENSE_TO) == null){
									toSafeMap.put(CCConstants.CCM_PROP_IO_LICENSE_TO, dateObj);
								}
							}

							tmpLCList.add(entity);
							replLifecycleContributer.put(lc_property, tmpLCList);

						}else{
							logger.warn("can not map lifecycle contributer role "+role+" to edu-sharing property");
						}
					}

					if (role != null && role.equals("terminator")) {
						Date terminatorDate =  (Date)contr.get(CCConstants.LOM_PROP_CONTRIBUTE_DATE);
						if(terminatorDate != null) {
							toSafeMap.put(CCConstants.CCM_PROP_IO_LICENSE_TO, terminatorDate);
						}
					}


					/***
					 * not longer used there will be a separate job to get the expiry date
					 */
				/*if (role != null && role.equals("terminator")) {
						assignedLicenseExpiryDate =  (Date)contr.get(CCConstants.LOM_PROP_CONTRIBUTE_DATE);
					}
					if (role != null && role.equals("distributor")) {
						String vcard =  (String)contr.get(CCConstants.LOM_PROP_CONTRIBUTE_ENTITY);
						ArrayList<Map<String, Object>> vcards =VCardConverter.vcardToHashMap(vcard);
						if(vcards != null){
							String mediaCenter = (String)vcards.get(0).get("FN");
							if(mediaCenter != null && !mediaCenter.trim().equals("")){
								assignedLicensesAuthorities.add(mediaCenter);
							}
						}
					}*/
				}

				if(assignedLicenseExpiryDate != null){
					ArrayList<Map<String, Object>> assignedLicensesNodes = new ArrayList<Map<String, Object>>();
					for(String authority : assignedLicensesAuthorities){
						Map<String, Object> props = new HashMap<>();
						props.put(CCConstants.CCM_PROP_ASSIGNED_LICENSE_AUTHORITY, AuthorityType.GROUP.getPrefixString() + authority);
						props.put(CCConstants.CCM_PROP_ASSIGNED_LICENSE_ASSIGNEDLICENSE_EXPIRY, assignedLicenseExpiryDate);
						//props.put(CCConstants.CCM_PROP_ASSIGNED_LICENSE_ASSIGNEDLICENSE_EXPIRY, new Date(System.currentTimeMillis() + 600000));
						assignedLicensesNodes.add(props);
					}

					if(assignedLicensesNodes.size() > 0){
						toSafeMap.put(CCConstants.CCM_PROP_IO_MEDIACENTER, assignedLicensesAuthorities);
						toSafeMap.put("TYPE#" + CCConstants.CCM_TYPE_ASSIGNED_LICENSE + "#" + CCConstants.CCM_ASSOC_ASSIGNEDLICENSES, assignedLicensesNodes);
					}
				}

			}

			for(Map.Entry<String,String> entry : CCConstants.getLifecycleContributerPropsMap().entrySet()){
				ArrayList<String> entityListForLifecycleContrProp = replLifecycleContributer.get(entry.getValue());

				//alfresco dont likes to safe empty lists
				if(entityListForLifecycleContrProp == null || entityListForLifecycleContrProp.size() == 0){
					toRemoveList.add(entry.getValue());
				}else{
					toSafeMap.put(entry.getValue(), entityListForLifecycleContrProp);
				}
			}

			// SAFE PART

			String mlAsString = getMultiLangAsString(".", versionNode);
			if(mlAsString != null) toSafeMap.put(CCConstants.LOM_PROP_LIFECYCLE_VERSION, mlAsString);


			// @TODO toSafeMap.put(CCConstants.LOM_PROP_LIFECYCLE_STATUS_SOURCE,lifecycleStatusSource);
			if(lifecycleStatusValue != null) toSafeMap.put(CCConstants.LOM_PROP_LIFECYCLE_STATUS, lifecycleStatusValue);
			if (lifecycleContributes != null && lifecycleContributes.size() > 0) {
				toSafeMap.put("TYPE#" + CCConstants.LOM_TYPE_CONTRIBUTE + "#" + CCConstants.LOM_ASSOC_LIFECYCLE_CONTRIBUTE, lifecycleContributes);
			}

		}
		/**
		 * metaMetadata
		 */
		Node nodeMetaMetadata = (Node) xpath.evaluate("metadata/lom/metaMetadata", nodeRecord, XPathConstants.NODE);
		if (nodeMetaMetadata != null) {
			String identifierCatalog = (String) xpath.evaluate("identifier/catalog", nodeMetaMetadata, XPathConstants.STRING);
			String identifierEntry = (String) xpath.evaluate("identifier/entry", nodeMetaMetadata, XPathConstants.STRING);
			String language = (String) xpath.evaluate("language", nodeMetaMetadata, XPathConstants.STRING);

			ArrayList<Map<String, Object>> metaMetadataContributes = getContributes(nodeMetaMetadata);

			// metadata contributer creator als replication attribut
			ArrayList<String> creatorList = new ArrayList<>();

			//metadata contribuiter provider
			ArrayList<String> providerList = new ArrayList<>();
			if (metaMetadataContributes != null) {
				for (Map<String, Object> map : metaMetadataContributes) {

					String role = (String) map.get(CCConstants.LOM_PROP_CONTRIBUTE_ROLE);
					if (role != null && role.trim().equals("creator")) {
						creatorList.add((String) map.get(CCConstants.LOM_PROP_CONTRIBUTE_ENTITY));
					}

					if(role != null && role.trim().equals("provider")){
						providerList.add((String) map.get(CCConstants.LOM_PROP_CONTRIBUTE_ENTITY));
					}
				}
			}

			if (creatorList.size() > 0) {
				toSafeMap.put(CCConstants.CCM_PROP_IO_REPL_METADATACONTRIBUTER_CREATOR, creatorList);
			}else{
				toRemoveList.add(CCConstants.CCM_PROP_IO_REPL_METADATACONTRIBUTER_CREATOR);
			}

			if(providerList.size() > 0){
				toSafeMap.put(CCConstants.CCM_PROP_IO_REPL_METADATACONTRIBUTER_PROVIDER, providerList);
			}else{
				toRemoveList.add(CCConstants.CCM_PROP_IO_REPL_METADATACONTRIBUTER_PROVIDER);
			}

			// SAFE PART
			HashMap metadataIdentifierProps = new HashMap();
			metadataIdentifierProps.put(CCConstants.LOM_PROP_IDENTIFIER_CATALOG, identifierCatalog);
			metadataIdentifierProps.put(CCConstants.LOM_PROP_IDENTIFIER_ENTRY, identifierEntry);

			// @TODO identifier language ist nicht im lom schema aber im oai lom
			// result

			toSafeMap.put("TYPE#" + CCConstants.LOM_TYPE_IDENTIFIER + "#" + CCConstants.LOM_ASSOC_META_METADATA_IDENTIFIER, metadataIdentifierProps);
			if (metaMetadataContributes != null && metaMetadataContributes.size() > 0) {
				toSafeMap.put("TYPE#" + CCConstants.LOM_TYPE_CONTRIBUTE + "#" + CCConstants.LOM_ASSOC_META_METADATA_CONTRIBUTE, metaMetadataContributes);
			}
		}

		/**
		 * technical
		 */
		Node nodeTechnical = (Node) xpath.evaluate("metadata/lom/technical", nodeRecord, XPathConstants.NODE);
		if(nodeTechnical != null){
			String format = (String) xpath.evaluate("format", nodeTechnical, XPathConstants.STRING);
			String size = (String) xpath.evaluate("size", nodeTechnical, XPathConstants.STRING);
			String location = (String) xpath.evaluate("location", nodeTechnical, XPathConstants.STRING);
			String duration = (String) xpath.evaluate("duration", nodeTechnical, XPathConstants.STRING);

			// SAFE PART
			if(format != null && !format.trim().equals("")){

				if(format.contains(";")){
					format = format.split(";")[0];
				}

				toSafeMap.put(CCConstants.LOM_PROP_TECHNICAL_FORMAT, format);
			}
			if(size != null && !size.equals("")) toSafeMap.put(CCConstants.LOM_PROP_TECHNICAL_SIZE, size);
			if(location != null && !location.trim().equals("")){
				toSafeMap.put(CCConstants.LOM_PROP_TECHNICAL_LOCATION, location);
				toSafeMap.put(CCConstants.CCM_PROP_IO_WWWURL, location);
			}

			if(duration != null && !duration.equals("")){
				toSafeMap.put(CCConstants.LOM_PROP_TECHNICAL_DURATION, duration);
			}

		}

		/**
		 * rights
		 */
		Node lomNode = (Node) xpath.evaluate("metadata/lom", nodeRecord, XPathConstants.NODE);
		if(lomNode != null){
			String rightsCostSrc = (String) xpath.evaluate("rights/cost/source", lomNode, XPathConstants.STRING);
			String rightsCostValue = (String) xpath.evaluate("rights/cost/value", lomNode, XPathConstants.STRING);
			String copyRightSrc = (String) xpath.evaluate("rights/copyrightAndOtherRestrictions/source", lomNode, XPathConstants.STRING);
			String copyRightVal = (String) xpath.evaluate("rights/copyrightAndOtherRestrictions/value", lomNode, XPathConstants.STRING);

			// @TODO was passiert mit description lang=x-t-cc-url wert z.B. =
			// http://creativecommons.org/licenses/by-nc/2.0/de/

			// SAFE PART
			if (copyRightVal != null && !copyRightVal.trim().equals("")) {

				if (copyRightVal.equals("yes") || copyRightVal.equals("no")) {
					Boolean copyRightValBool = (copyRightVal.equals("yes")) ? true : false;
					toSafeMap.put(CCConstants.LOM_PROP_RIGHTS_COPY_RIGHT, copyRightValBool);
				} else {
					logger.info("unknown value for copyRightVal:" + copyRightVal);
				}
			}
			if (rightsCostValue != null && !rightsCostValue.trim().equals("")) {
				if (rightsCostValue.equals("yes") || rightsCostValue.equals("no")) {
					Boolean rightsCostValueBool = (rightsCostValue.equals("yes")) ? true : false;
					toSafeMap.put(CCConstants.LOM_PROP_RIGHTS_COST, rightsCostValueBool);
				}
			}

			putMultiLangValue(toSafeMap, CCConstants.LOM_PROP_RIGHTS_RIGHTS_DESCRIPTION, "rights/description", lomNode);

			String lomrights = (String)toSafeMap.get(CCConstants.LOM_PROP_RIGHTS_RIGHTS_DESCRIPTION);
			if(lomrights != null && !lomrights.trim().equals("")){

				if(lomrights.startsWith("http")) {
					String[] splitted = lomrights.split("/");
					String version = splitted[splitted.length - 1];
					toSafeMap.put(CCConstants.CCM_PROP_IO_COMMONLICENSE_CC_VERSION, version);
				}

				if(lomrights.toLowerCase().contains("CC-by") || lomrights.toLowerCase().contains("by")){
					toSafeMap.put(CCConstants.CCM_PROP_IO_COMMONLICENSE_KEY, CCConstants.COMMON_LICENSE_CC_BY);
				}

				if(lomrights.toLowerCase().contains("CC-by-sa") || lomrights.toLowerCase().contains("by-sa")){
					toSafeMap.put(CCConstants.CCM_PROP_IO_COMMONLICENSE_KEY, CCConstants.COMMON_LICENSE_CC_BY_SA);
				}

				if(lomrights.toLowerCase().contains("CC-by-nc") || lomrights.toLowerCase().contains("by-nc")){
					toSafeMap.put(CCConstants.CCM_PROP_IO_COMMONLICENSE_KEY, CCConstants.COMMON_LICENSE_CC_BY_NC);
				}

				if(lomrights.toLowerCase().contains("CC-by-nc-nd") || lomrights.toLowerCase().contains("by-nc-nd")){
					toSafeMap.put(CCConstants.CCM_PROP_IO_COMMONLICENSE_KEY, CCConstants.COMMON_LICENSE_CC_BY_NC_ND);
				}

				if(lomrights.toLowerCase().contains("CC-by-nc-sa") || lomrights.toLowerCase().contains("by-nc-sa")){
					toSafeMap.put(CCConstants.CCM_PROP_IO_COMMONLICENSE_KEY, CCConstants.COMMON_LICENSE_CC_BY_NC_SA);
				}

				if(lomrights.toLowerCase().contains("CC-by-nd") || lomrights.toLowerCase().contains("by-nd")){
					toSafeMap.put(CCConstants.CCM_PROP_IO_COMMONLICENSE_KEY, CCConstants.COMMON_LICENSE_CC_BY_ND);
				}
			}

		}

		// @TODO rightsCostSrc, copyRightSrc

		/**
		 * educational
		 *
		 * @todo typicallearningtime
		 */
		NodeList nodeEducationalList = (NodeList) xpath.evaluate("metadata/lom/educational", nodeRecord, XPathConstants.NODESET);
		List educationalToSafeList = new ArrayList();
		// LOM Replication lists
		List<String> lomReplicationLearningresourceTypeList = new ArrayList<>();
		List lomReplicationEducationalContextList = new ArrayList();
		List lomReplicationIntendedEndUserList = new ArrayList();
		List lomReplicationTypicalAgeRangeList = new ArrayList();

		for (int eduIdx = 0; eduIdx < nodeEducationalList.getLength(); eduIdx++) {
			HashMap eduCationalToSafe = new HashMap();
			Node nodeEducational = nodeEducationalList.item(eduIdx);
			NodeList learningresourceTypeList = (NodeList) xpath.evaluate("learningResourceType", nodeEducational, XPathConstants.NODESET);
			List learningResourceTypeToSafeList = new ArrayList();
			for (int i = 0; i < learningresourceTypeList.getLength(); i++) {
				Node learningresourceTypeNode = learningresourceTypeList.item(i);
				String learningResourceTypeSource = (String) xpath.evaluate("source", learningresourceTypeNode, XPathConstants.STRING);
				String learningResourceTypeValue = (String) xpath.evaluate("value", learningresourceTypeNode, XPathConstants.STRING);
				learningResourceTypeToSafeList.add(learningResourceTypeValue);
				lomReplicationLearningresourceTypeList.add(learningResourceTypeValue);
				// @TODO learningResourceTypeSource
			}

			NodeList intendedEndUserRoleList = (NodeList) xpath.evaluate("intendedEndUserRole", nodeEducational, XPathConstants.NODESET);
			List intendedEndUserRoleToSafeList = new ArrayList();
			for (int i = 0; i < intendedEndUserRoleList.getLength(); i++) {
				Node intendedEndUserNode = intendedEndUserRoleList.item(i);
				String intendedEndUserRoleSource = (String) xpath.evaluate("source", intendedEndUserNode, XPathConstants.STRING);
				String intendedEndUserRoleValue = (String) xpath.evaluate("value", intendedEndUserNode, XPathConstants.STRING);
				intendedEndUserRoleToSafeList.add(intendedEndUserRoleValue);
				// @TODO intendedEndUserRoleSource
			}

			lomReplicationIntendedEndUserList.addAll(intendedEndUserRoleToSafeList);

			NodeList contextList = (NodeList) xpath.evaluate("context", nodeEducational, XPathConstants.NODESET);
			List contextToSafeList = new ArrayList();
			for (int i = 0; i < contextList.getLength(); i++) {
				Node contextNode = contextList.item(i);
				String contexteSource = (String) xpath.evaluate("source", contextNode, XPathConstants.STRING);
				String contextValue = (String) xpath.evaluate("value", contextNode, XPathConstants.STRING);
				contextToSafeList.add(contextValue);
				lomReplicationEducationalContextList.add(contextValue);
				// @TODO contexteSource
			}

			lomReplicationTypicalAgeRangeList = getMultivalue(nodeEducational, "typicalAgeRange");
			List educationalLanguage = convertListToString((NodeList)xpath.evaluate("language", nodeEducational, XPathConstants.NODESET));
			eduCationalToSafe.put(CCConstants.LOM_PROP_EDUCATIONAL_LANGUAGE, educationalLanguage);

			// SAFE PART
			eduCationalToSafe.put(CCConstants.LOM_PROP_EDUCATIONAL_LEARNINGRESOURCETYPE, learningResourceTypeToSafeList);
			eduCationalToSafe.put(CCConstants.LOM_PROP_EDUCATIONAL_INTENDED_ENDUSERROLE, intendedEndUserRoleToSafeList);
			eduCationalToSafe.put(CCConstants.LOM_PROP_EDUCATIONAL_CONTEXT, contextToSafeList);

			eduCationalToSafe.put(CCConstants.LOM_PROP_EDUCATIONAL_TYPICALAGERANGE, lomReplicationTypicalAgeRangeList);

			educationalToSafeList.add(eduCationalToSafe);
		}
		// SAFE PART
		toSafeMap.put("TYPE#" + CCConstants.LOM_TYPE_EDUCATIONAL + "#" + CCConstants.LOM_ASSOC_EDUCATIONAL, educationalToSafeList);

		// alfreco don't likes to safe empty lists when the prop is multilang and multivalue
		if (lomReplicationLearningresourceTypeList.size() > 0){
			toSafeMap.put(CCConstants.CCM_PROP_IO_REPL_EDUCATIONAL_LEARNINGRESSOURCETYPE, lomReplicationLearningresourceTypeList);
		}else{
			//we need the to overwrite with empty values when the values disappear from the source
			toRemoveList.add(CCConstants.CCM_PROP_IO_REPL_EDUCATIONAL_LEARNINGRESSOURCETYPE);
		}
		if (lomReplicationEducationalContextList.size() > 0){
			toSafeMap.put(CCConstants.CCM_PROP_IO_REPL_EDUCATIONAL_CONTEXT, lomReplicationEducationalContextList);
		}else{
			toRemoveList.add(CCConstants.CCM_PROP_IO_REPL_EDUCATIONAL_CONTEXT);
		}

		if(lomReplicationIntendedEndUserList.size() > 0){
			toSafeMap.put(CCConstants.CCM_PROP_IO_REPL_EDUCATIONAL_INTENDEDENDUSERROLE, lomReplicationIntendedEndUserList);
		}else{
			toRemoveList.add(CCConstants.CCM_PROP_IO_REPL_EDUCATIONAL_INTENDEDENDUSERROLE);
		}

		if(lomReplicationTypicalAgeRangeList != null && lomReplicationTypicalAgeRangeList.size() > 0) {
			toSafeMap.put(CCConstants.CCM_PROP_IO_REPL_EDUCATIONAL_TYPICALAGERANGE, lomReplicationTypicalAgeRangeList);
			String tar = (String)lomReplicationTypicalAgeRangeList.get(0);
			if(tar != null && tar.matches("[0-9]?[0-9]-[0-9][0-9]?")){
				String[] splitted = tar.split("-");
				toSafeMap.put(CCConstants.CCM_PROP_IO_REPL_EDUCATIONAL_TYPICALAGERANGEFROM,splitted[0]);
				toSafeMap.put(CCConstants.CCM_PROP_IO_REPL_EDUCATIONAL_TYPICALAGERANGETO,splitted[1]);
			}

		}else {
			toRemoveList.add(CCConstants.CCM_PROP_IO_REPL_EDUCATIONAL_TYPICALAGERANGE);
		}

		/**
		 * classification
		 */
		//for replication ArrayList<String>
		NodeList nodeClassificationList = (NodeList) xpath.evaluate("metadata/lom/classification", nodeRecord, XPathConstants.NODESET);
		List classificationToSafeList = new ArrayList();

		// LOM Replication
		List lomReplicationTaxonEntry = new ArrayList();
		List lomReplicationTaxonId = new ArrayList();

		List<String> classificationKeywords = new ArrayList<>();
		for (int i = 0; i < nodeClassificationList.getLength(); i++) {
			Node classificationNode = nodeClassificationList.item(i);
			String purposeSource = (String) xpath.evaluate("purpose/source", classificationNode, XPathConstants.STRING);
			String purposeValue = (String) xpath.evaluate("purpose/value", classificationNode, XPathConstants.STRING);

			NodeList taxonPathList = (NodeList) xpath.evaluate("taxonPath", classificationNode, XPathConstants.NODESET);
			List taxonPathToSafeList = new ArrayList();
			for (int taxonPathIdx = 0; taxonPathIdx < taxonPathList.getLength(); taxonPathIdx++) {
				Node taxonPathNode = taxonPathList.item(taxonPathIdx);

				NodeList taxonList = (NodeList) xpath.evaluate("taxon", taxonPathNode, XPathConstants.NODESET);
				List taxonToSafeList = new ArrayList();
				for (int taxonIdx = 0; taxonIdx < taxonList.getLength(); taxonIdx++) {
					Node taxonNode = taxonList.item(taxonIdx);
					String taxonId = (String) xpath.evaluate("id", taxonNode, XPathConstants.STRING);
					if(taxonId != null) taxonId = taxonId.trim();


					HashMap taxonProps = new HashMap();
					taxonProps.put(CCConstants.LOM_PROP_TAXON_ID, taxonId);

					putMultiLangValue(taxonProps, CCConstants.LOM_PROP_TAXON_ENTRY, "entry", taxonNode);
					putMultiLangValue(lomReplicationTaxonEntry, CCConstants.LOM_PROP_TAXON_ENTRY, "entry", taxonNode);

					/**
					 * only put discipline taxonId's to main object
					 */
					if(purposeValue != null && purposeValue.trim().equals("discipline")){
						lomReplicationTaxonId.add(taxonId);
					}

					taxonToSafeList.add(taxonProps);

				}


				HashMap taxonPathProps = new HashMap();
				putMultiLangValue(taxonPathProps, CCConstants.LOM_PROP_TAXONPATH_SOURCE, "source", taxonPathNode);

				taxonPathProps.put("TYPE#" + CCConstants.LOM_TYPE_TAXON + "#" + CCConstants.LOM_ASSOC_TAXONPATH_TAXON, taxonToSafeList);
				taxonPathToSafeList.add(taxonPathProps);
			}
			HashMap classificationProps = new HashMap();
			classificationProps.put(CCConstants.LOM_PROP_CLASSIFICATION_PURPOSE, purposeValue);

			List classificationKeyword = getMultivalue(classificationNode, "keyword");
			if(classificationKeyword != null && classificationKeyword.size() > 0){
				classificationProps.put(CCConstants.LOM_PROP_CLASSIFICATION_KEYWORD, classificationKeyword);
				classificationKeywords.addAll(classificationKeyword);
			}

			// @TODO purposeSource
			classificationProps.put("TYPE#" + CCConstants.LOM_TYPE_TAXON_PATH + "#" + CCConstants.LOM_ASSOC_CLASSIFICATION_TAXONPATH, taxonPathToSafeList);

			// @TODO classification keyword (not found in data)
			classificationToSafeList.add(classificationProps);
		}

		if(classificationKeywords.size() > 0){
			toSafeMap.put(CCConstants.CCM_PROP_IO_REPL_CLASSIFICATION_KEYWORD, classificationKeywords);
		}

		toSafeMap.put("TYPE#" + CCConstants.LOM_TYPE_CLASSIFICATION + "#" + CCConstants.LOM_ASSOC_SCHEMA_CLASSIFICATION, classificationToSafeList);

		// TO SAFE LOM Replication
		// alfreco don't likes to safe empty lists
		if (classificationToSafeList.size() > 0){

			toSafeMap.put(CCConstants.CCM_PROP_IO_REPL_TAXON_ENTRY, lomReplicationTaxonEntry);
			toSafeMap.put(CCConstants.CCM_PROP_IO_REPL_TAXON_ID, lomReplicationTaxonId);

		} else {

			/*
			//
			//alf5 solr4 don't likes to get null for mltext properties (solr tracking failed). So we put an empty list here
			//
			toSafeMap.put(CCConstants.CCM_PROP_IO_REPL_TAXON_ENTRY, new ArrayList<>());
			toSafeMap.put(CCConstants.CCM_PROP_IO_REPL_TAXON_ID, new ArrayList<>());
			*/
			/**
			 * add them to the remove list, this is better than empty lists
			 */
			toRemoveList.add(CCConstants.CCM_PROP_IO_REPL_TAXON_ENTRY);
			toRemoveList.add(CCConstants.CCM_PROP_IO_REPL_TAXON_ID);

		}


		/**
		 * Annotation
		 *
		 * @TODO
		 */

		/**
		 * Requirement
		 *
		 * @TODO
		 */

		/**
		 * @TODO replicate nested lom objects for search
		 */

		String thumbnailUrl = null;

		/**
		 * custom elements
		 */
		String customElementsSource = (String) xpath.evaluate("metadata/lom/customElements/source_id", nodeRecord, XPathConstants.STRING);
		String customElementsThumbnail = (String) xpath.evaluate("metadata/lom/customElements/thumbnail", nodeRecord, XPathConstants.STRING);

		if(customElementsSource != null && !customElementsSource.trim().equals("")
				&& customElementsThumbnail != null && !customElementsThumbnail.trim().equals("")){
			ApplicationInfo appInfo = ApplicationInfoList.getHomeRepository();
			thumbnailUrl = appInfo.getClientprotocol() +"://"+ appInfo.getDomain() +"/"+customElementsSource+"/images/" + customElementsThumbnail;
		}

		/**
		 * additional data from elixier
		 */
		// getting Thumbnail thru elixier dialect

		ArrayList<String> fach_sachgebietListOriginal = new ArrayList<>();
		String serientitel = null;

		List relationToSafeList = new ArrayList();
		List relationToSafeListProperty = new ArrayList();
		NodeList nodeRelationList = (NodeList) xpath.evaluate("metadata/lom/relation", nodeRecord, XPathConstants.NODESET);
		if(nodeRelationList != null){

			for(int i = 0; i < nodeRelationList.getLength(); i++){
				Node nodeRelation = nodeRelationList.item(i);
				String relationKind = (String)  xpath.evaluate("kind/value", nodeRelation, XPathConstants.STRING);
				if(relationKind != null && relationKind.equals("hasthumbnail")){
					String tmpThumbUrl = (String)  xpath.evaluate("resource/description/string", nodeRelation, XPathConstants.STRING);
					if(tmpThumbUrl != null && !tmpThumbUrl.trim().equals("")){
						thumbnailUrl = tmpThumbUrl.trim();
					}
				}

				if(relationKind != null){
					String description = (String)  xpath.evaluate("resource/description/string", nodeRelation, XPathConstants.STRING);

					HashMap relationProps = new HashMap();
					relationProps.put(CCConstants.LOM_PROP_RELATION_KIND, relationKind);
					relationProps.put(CCConstants.LOM_PROP_RESOURCE_DESCRIPTION, description);

					String catalog = (String) xpath.evaluate("resource/identifier/catalog", nodeRelation, XPathConstants.STRING);
					String entry = (String) xpath.evaluate("resource/identifier/entry", nodeRelation, XPathConstants.STRING);
					HashMap identifierProps = new HashMap();
					identifierProps.put(CCConstants.LOM_PROP_IDENTIFIER_CATALOG, catalog);
					identifierProps.put(CCConstants.LOM_PROP_IDENTIFIER_ENTRY, entry);
					relationProps.put("TYPE#"+ CCConstants.LOM_TYPE_IDENTIFIER + "#" + CCConstants.LOM_ASSOC_RESOURCE_IDENTIFIER, identifierProps);

					relationToSafeList.add(relationProps);
					if(!relationKind.equals("hasthumbnail") && entry!=null && !entry.isEmpty())
						relationToSafeListProperty.add(relationKind+"#"+entry);
				}
			}
		}

		if(relationToSafeList.size() > 0){
			toSafeMap.put("TYPE#" + CCConstants.LOM_TYPE_RELATION + "#" + CCConstants.LOM_ASSOC_SCHEMA_RELATION, relationToSafeList);
			// store as flat property for solr search
			if(relationToSafeListProperty.size() > 0)
				toSafeMap.put(CCConstants.CCM_PROP_IO_REPL_EDUCATIONAL_SCHEMA_RELATION,relationToSafeListProperty);
		}

		ApplicationInfo homeApplication = ApplicationInfoList.getHomeRepository();


		// TO SAFE thumbnail:
		if (thumbnailUrl != null && !thumbnailUrl.trim().equals("")) {
			toSafeMap.put(CCConstants.CCM_PROP_IO_THUMBNAILURL, thumbnailUrl);
		}
		if (serientitel != null && !serientitel.trim().equals("")) {
			toSafeMap.put(CCConstants.CCM_PROP_IO_TITLE_SERIES, serientitel);
		}
	}

	private static List<String> convertListToString(NodeList nodes) {
		List<String> result=new ArrayList<>(nodes.getLength());
		for(int i=0;i<nodes.getLength();i++){
			result.add(nodes.item(i).getTextContent());
		}
		return result;
	}


	/**
	 * //since we deactivated the multilang = true global alf prop we can not use the MLText anymore
	 * this method returns a list of strings including all values of all languages
	 *
	 * @param node
	 * @return
	 * @throws XPathExpressionException
	 */
	public ArrayList<String> getMultiLangValueNew(Node node) throws XPathExpressionException {
		ArrayList<String> result = new ArrayList<>();
		if (node != null) {
			NodeList langList = (NodeList) xpath.evaluate("string", node, XPathConstants.NODESET);
			if(langList.getLength() == 0) langList = (NodeList) xpath.evaluate("langstring", node, XPathConstants.NODESET);
			for (int i = 0; i < langList.getLength(); i++) {
				Node langValNode = langList.item(i);
				String language = (String) xpath.evaluate("@language", langValNode, XPathConstants.STRING);
				language = (language == null || language.trim().equals("")) ? (String) xpath.evaluate("@lang", langValNode, XPathConstants.STRING) : language;
				String value = (String) xpath.evaluate(".", langValNode, XPathConstants.STRING);
				if (language != null && !language.trim().equals("") && value != null && !value.trim().equals("")) {
					// wir brauchen das LAND damit i18n richtig funktioniert
					if (language.length() == 2) {
						language = language + "_" + language.toUpperCase();
					}

					value = adaptValue(value);
					result.add(value);
				}
			}
		}
		return result;
	}

	public List getMultivalue(Node parent, String nodeName) throws XPathExpressionException {
		return getMultivalue(parent,nodeName,true);
	}

	public List getMultivalue(Node parent, String nodeName,boolean ignoreEmpty) throws XPathExpressionException {
		if (parent == null){
			logger.debug("returning null cause parent == null");
			return null;
		}

		List result = new ArrayList();

		NodeList nodelist = (NodeList) xpath.evaluate(nodeName, parent, XPathConstants.NODESET);
		for (int i = 0; i < nodelist.getLength(); i++) {
			Node node = nodelist.item(i);

			// test if multilang
			Double countStringTag = (Double) xpath.evaluate("count(string)", node, XPathConstants.NUMBER);
			if (countStringTag != null && countStringTag > 0) {

				//since we deactivated the multilang = true global alf prop we can not use the MLText anymore
				for(Object value : getMultiLangValueNew(node)){
					result.add(value);
				}

			}else {
				String value = (String) xpath.evaluate(".", node, XPathConstants.STRING);
				if (!ignoreEmpty || (value != null && !value.trim().isEmpty())) {
					value = adaptValue(value);
					result.add(value);
				}
			}
		}
		if (result.size() > 0)
			return result;
		else
			return null;
	}

	public ArrayList<Map<String, Object>> getContributes(Node parent) throws XPathExpressionException {
		ArrayList<Map<String, Object>> result = new ArrayList<>();
		NodeList contributes = (NodeList) xpath.evaluate("contribute", parent, XPathConstants.NODESET);
		for (int i = 0; i < contributes.getLength(); i++) {
			Map<String, Object> map = new HashMap<>();
			Node nodeContribute = contributes.item(i);
			String contributeRoleSource = (String) xpath.evaluate("role/source", nodeContribute, XPathConstants.STRING);
			String contributeRoleValue = (String) xpath.evaluate("role/value", nodeContribute, XPathConstants.STRING);
			String contributeEntity = (String) xpath.evaluate("entity", nodeContribute, XPathConstants.STRING);
			String contributeDate = (String) xpath.evaluate("date", nodeContribute, XPathConstants.STRING);

			Date date = convertToDate(contributeDate);
			if (date != null){
				map.put(CCConstants.LOM_PROP_CONTRIBUTE_DATE, date);
			}
			if (contributeEntity != null){
				//replace chars from vcard:
				contributeEntity = contributeEntity.replace("&quot;", "");
				map.put(CCConstants.LOM_PROP_CONTRIBUTE_ENTITY, contributeEntity);
			}
			if (contributeRoleValue != null){
				map.put(CCConstants.LOM_PROP_CONTRIBUTE_ROLE, contributeRoleValue);
			}
			// @TODO contributeRoleSource
			result.add(map);
		}
		if (result.size() > 0)
			return result;
		else
			return null;
	}

	private Date convertToDate(String date) {
		try {
			SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SS");
			return df.parse(date);
		} catch (Exception e) {
			try {
				return new DateTime(date).toDate();
			}catch(Exception e2) {
				logger.debug("error parsing date:" + date);
				return null;
			}
		}
	}

	private void putMultiLangValue(HashMap map, String property, String xmlTag, Node node ) throws XPathExpressionException{
		ArrayList<String>  multilangValue = getMultiLangValueNew((Node) xpath.evaluate(xmlTag, node, XPathConstants.NODE));

		/**
		 * check if null cause if we set a multilang value to null indexing of the node does not work
		 */
		if(multilangValue != null && multilangValue.size() > 0){

			String value = null;
			for(String v : multilangValue){
				if(value == null){
					value = v;
				}else{
					value = value + ", " + v;
				}
			}
			map.put(property, value);
		}
	}

	private void putMultiLangValue(List list, String property, String xmlTag, Node node ) throws XPathExpressionException{
		ArrayList<String> multilangValue = getMultiLangValueNew((Node) xpath.evaluate(xmlTag, node, XPathConstants.NODE));

		/**
		 * check if null cause if we set a multilang value to null indexing of the node does not work
		 */
		if(multilangValue != null && multilangValue.size() > 0){
			list.addAll(multilangValue);
		}
	}

	private String getMultiLangAsString(String xmlTag, Node node) throws XPathExpressionException{
		ArrayList<String> multilangValue = getMultiLangValueNew((Node) xpath.evaluate(xmlTag, node, XPathConstants.NODE));

		String value = null;
		/**
		 * check if null cause if we set a multilang value to null indexing of the node does not work
		 */
		if(multilangValue != null && multilangValue.size() > 0){

			for(String v : multilangValue){
				if(value == null){
					value = v;
				}else{
					value = value + ", " + v;
				}
			}
		}

		return value;
	}

	private String adaptValue(String value) {
		String result = org.htmlparser.util.Translate.decode(value);
		return result;
	}

	@Override
	public Map<String, Object> getProperties() {
		return toSafeMap;
	}

	@Override
	public List<String> getPropertiesToRemove() {
		return toRemoveList;
	}
}
