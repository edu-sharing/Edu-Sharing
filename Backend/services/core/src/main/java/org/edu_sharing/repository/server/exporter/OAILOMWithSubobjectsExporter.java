package org.edu_sharing.repository.server.exporter;

import java.io.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.MLText;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.namespace.RegexQNamePattern;
import org.apache.log4j.Logger;
import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.MCAlfrescoAPIClient;
import org.springframework.context.ApplicationContext;
import org.springframework.extensions.surf.util.ISO8601DateFormat;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * @TODO
 * 
 * source tags(fehlen generell noch)
 * 
 * 
 * @author mv
 *
 */
public class OAILOMWithSubobjectsExporter {

	class TagDef{
		
		String tag;
		
		String subTag;
		
		String subTagProperty;
		
		Map<String,String> properties = new HashMap<>();
		
		String type;
		
		String childAssociation;
		
		List<TagDef> subTags = new ArrayList<>();
		
		public Map<String, String> getProperties() {
			return properties;
		}
		
		public void setProperties(Map<String, String> properties) {
			this.properties = properties;
		}
		
		public void setTag(String tag) {
			this.tag = tag;
		}
		
		public String getTag() {
			return tag;
		}
		
		public String getType() {
			return type;
		}
		
		public void setType(String type) {
			this.type = type;
		}
		
		public void setChildAssociation(String childAssociation) {
			this.childAssociation = childAssociation;
		}
		
		public String getChildAssociation() {
			return childAssociation;
		}
		
		public void setSubTags(List<TagDef> subTags) {
			this.subTags = subTags;
		}
		
		public List<TagDef> getSubTags() {
			return subTags;
		}
		
		public String getSubTag() {
			return subTag;
		}
		
		public void setSubTag(String subTag) {
			this.subTag = subTag;
		}
		
		public String getSubTagProperty() {
			return subTagProperty;
		}
		
		public void setSubTagProperty(String subTagProperty) {
			this.subTagProperty = subTagProperty;
		}
	}

	Logger logger = Logger.getLogger(OAILOMWithSubobjectsExporter.class);

	ServiceRegistry serviceRegistry = null;

	NodeService nodeService = null;
	
	NodeRef ioNodeRef = null;

	NodeRef nodeRef = null;

	Document doc;
	
	TagDef lom = new TagDef();

	public OAILOMWithSubobjectsExporter(String ioId) throws ParserConfigurationException {
		ApplicationContext context = AlfAppContextGate.getApplicationContext();
		serviceRegistry = (ServiceRegistry) context.getBean(ServiceRegistry.SERVICE_REGISTRY);
		nodeService = serviceRegistry.getNodeService();

		nodeRef = new NodeRef(MCAlfrescoAPIClient.storeRef, ioId);
		ioNodeRef = new NodeRef(nodeRef.getStoreRef(), nodeRef.getId());

		DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

		// root element record
		doc = docBuilder.newDocument();
		
		/**
		 * lom
		 */
		
		lom.setTag("lom");
		List<TagDef> lomSubTags = new ArrayList<>();
		lom.setSubTags(lomSubTags);
		
		/**
		 * general
		 */
		TagDef general = new TagDef();
		general.setTag("general");
		general.setType(CCConstants.CCM_TYPE_IO);
		List<TagDef> generalSubtags = general.getSubTags();
		
		TagDef generalTitle = new TagDef();
		generalTitle.setTag("title");
		generalTitle.getProperties().put(CCConstants.LOM_PROP_GENERAL_TITLE, "string");
		generalSubtags.add(generalTitle);
		general.getProperties().put(CCConstants.LOM_PROP_GENERAL_LANGUAGE,"language");
		
		TagDef generalDescription = new TagDef();
		generalDescription.setTag("description");
		generalDescription.getProperties().put(CCConstants.LOM_PROP_GENERAL_DESCRIPTION,"string");
		generalSubtags.add(generalDescription);
		
		TagDef generalKeyword = new TagDef();
		generalKeyword.setTag("keyword");
		generalKeyword.setSubTag("string");
		generalKeyword.setSubTagProperty(CCConstants.LOM_PROP_GENERAL_KEYWORD);
		generalSubtags.add(generalKeyword);
		
		/**
		 * general identifier
		 */
		TagDef identifier = new TagDef();
		identifier.setTag("identifier");
		identifier.setType(CCConstants.LOM_TYPE_IDENTIFIER);
		identifier.getProperties().put(CCConstants.LOM_PROP_IDENTIFIER_CATALOG, "catalog");
		identifier.getProperties().put(CCConstants.LOM_PROP_IDENTIFIER_ENTRY, "entry");
		identifier.setChildAssociation(CCConstants.LOM_ASSOC_IDENTIFIER);
		generalSubtags.add(identifier);
		
		/**
		 * general structure
		 */
		TagDef structure = new TagDef();
		structure.setTag("structure");
		structure.getProperties().put(CCConstants.LOM_PROP_GENERAL_STRUCTURE, "value");
		generalSubtags.add(structure);
		
		/**
		 * general agregationLevel
		 */
		TagDef agregationLevel = new TagDef();
		agregationLevel.setTag("aggregationLevel");
		agregationLevel.getProperties().put(CCConstants.LOM_PROP_GENERAL_AGGREGATIONLEVEL, "value");
		generalSubtags.add(agregationLevel);

		/**
		 * lifeCycle
		 */
		TagDef lifecycle = new TagDef();
		lifecycle.setTag("lifeCycle");
		
		TagDef lcVersion = new TagDef();
		lcVersion.setTag("version");
		lcVersion.getProperties().put(CCConstants.LOM_PROP_LIFECYCLE_VERSION, "string");
		
		TagDef lcStatus = new TagDef();
		lcStatus.setTag("status");
		lcStatus.getProperties().put(CCConstants.LOM_PROP_LIFECYCLE_STATUS, "value");
		
		/**
		 * lifeCycle Contribute
		 */
		TagDef lcContribute = new TagDef();
		lcContribute.setTag("contribute");
		lcContribute.setType(CCConstants.LOM_TYPE_CONTRIBUTE);
		lcContribute.setChildAssociation(CCConstants.LOM_ASSOC_LIFECYCLE_CONTRIBUTE);
		List<TagDef> lcContributeSubTags = new ArrayList<>();
		lcContribute.setSubTags(lcContributeSubTags);
		lcContribute.getProperties().put(CCConstants.LOM_PROP_CONTRIBUTE_ENTITY, "entity");
		
		TagDef lcContributeRole = new TagDef();
		lcContributeSubTags.add(lcContributeRole);
		lcContributeRole.setTag("role");
		lcContributeRole.getProperties().put(CCConstants.LOM_PROP_CONTRIBUTE_ROLE, "value");
		
		TagDef lcContributeDate = new TagDef();
		lcContributeDate.setTag("date");
		lcContributeSubTags.add(lcContributeDate);
		lcContributeDate.getProperties().put(CCConstants.LOM_PROP_CONTRIBUTE_DATE, "dateTime");
		
		lifecycle.getSubTags().add(lcVersion);
		lifecycle.getSubTags().add(lcStatus);
		lifecycle.getSubTags().add(lcContribute);
		
		/**
		 * metadata
		 */
		TagDef metaMetadata = new TagDef();
		metaMetadata.setTag("metaMetadata");
		metaMetadata.setType(CCConstants.CCM_TYPE_IO);
		
		TagDef metaMetadataidentifier = new TagDef();
		metaMetadataidentifier.setTag("identifier");
		metaMetadataidentifier.setType(CCConstants.LOM_TYPE_IDENTIFIER);
		metaMetadataidentifier.getProperties().put(CCConstants.LOM_PROP_IDENTIFIER_CATALOG, "catalog");
		metaMetadataidentifier.getProperties().put(CCConstants.LOM_PROP_IDENTIFIER_ENTRY, "entry");
		metaMetadataidentifier.setChildAssociation(CCConstants.LOM_ASSOC_META_METADATA_IDENTIFIER);
		metaMetadata.getSubTags().add(metaMetadataidentifier);
		
		/**
		 * metadata Contribute
		 */
		TagDef mdContribute = new TagDef();
		mdContribute.setTag("contribute");
		mdContribute.setType(CCConstants.LOM_TYPE_CONTRIBUTE);
		mdContribute.setChildAssociation(CCConstants.LOM_ASSOC_META_METADATA_CONTRIBUTE);
		mdContribute.getProperties().put(CCConstants.LOM_PROP_CONTRIBUTE_ENTITY, "entity");
		
		TagDef mdContributeRole = new TagDef();
		mdContributeRole.setTag("role");
		mdContribute.getSubTags().add(lcContributeRole);
		mdContributeRole.getProperties().put(CCConstants.LOM_PROP_CONTRIBUTE_ROLE, "value");
		
		TagDef mdContributeDate = new TagDef();
		mdContributeDate.setTag("date");
		mdContribute.getSubTags().add(lcContributeDate);
		mdContributeDate.getProperties().put(CCConstants.LOM_PROP_CONTRIBUTE_DATE, "dateTime");
		
		metaMetadata.getSubTags().add(mdContribute);
		metaMetadata.getProperties().put(CCConstants.LOM_PROP_META_METADATA_LANGUAGE, "language");
		
		/**
		 * technical
		 */
		TagDef technical = new TagDef();
		technical.setTag("technical");
		technical.getProperties().put(CCConstants.LOM_PROP_TECHNICAL_FORMAT, "format");
		technical.getProperties().put(CCConstants.LOM_PROP_TECHNICAL_SIZE, "size");
		technical.getProperties().put(CCConstants.LOM_PROP_TECHNICAL_LOCATION, "location");
		technical.getProperties().put(CCConstants.LOM_PROP_TECHNICAL_DURATION, "duration");
		
		/**
		 * educational
		 */
		TagDef educational = new TagDef();
		educational.setTag("educational");
		educational.setType(CCConstants.LOM_TYPE_EDUCATIONAL);
		
		TagDef learningResourceType = new TagDef();
		learningResourceType.setTag("learningResourceType");
		learningResourceType.setSubTag("value");
		learningResourceType.setSubTagProperty(CCConstants.LOM_PROP_EDUCATIONAL_LEARNINGRESOURCETYPE);
		educational.getSubTags().add(learningResourceType);
		
		TagDef intendedEndUserRole = new TagDef();
		intendedEndUserRole.setTag("intendedEndUserRole");
		intendedEndUserRole.setSubTag("value");
		intendedEndUserRole.setSubTagProperty(CCConstants.LOM_PROP_EDUCATIONAL_INTENDED_ENDUSERROLE);
		educational.getSubTags().add(intendedEndUserRole);
		
		TagDef educationalContext = new TagDef();
		educationalContext.setTag("context");
		educationalContext.setSubTag("value");
		educationalContext.setSubTagProperty(CCConstants.LOM_PROP_EDUCATIONAL_CONTEXT);
		educational.getSubTags().add(educationalContext);
		
		
		TagDef typicalAgeRange = new TagDef();
		typicalAgeRange.setTag("typicalAgeRange");
		typicalAgeRange.getProperties().put(CCConstants.LOM_PROP_EDUCATIONAL_TYPICALAGERANGE, "string");
		educational.getSubTags().add(typicalAgeRange);

		educational.getProperties().put(CCConstants.LOM_PROP_EDUCATIONAL_LANGUAGE, "language");
		
		/**
		 * Rights
		 */
		TagDef rights = new TagDef();
		rights.setTag("rights");
		rights.setType(CCConstants.CCM_TYPE_IO);
		
		TagDef copyrightAndOtherRestrictions = new TagDef();
		copyrightAndOtherRestrictions.setTag("copyrightAndOtherRestrictions");
		copyrightAndOtherRestrictions.getProperties().put(CCConstants.LOM_PROP_RIGHTS_COPY_RIGHT, "value");
		rights.getSubTags().add(copyrightAndOtherRestrictions);
		
		TagDef rightsDescription = new TagDef();
		rightsDescription.setTag("description");
		rightsDescription.getProperties().put(CCConstants.LOM_PROP_RIGHTS_RIGHTS_DESCRIPTION, "string");
		rights.getSubTags().add(rightsDescription);
		
		/**
		 * relation
		 */
		TagDef relation = new TagDef();
		relation.setTag("relation");
		relation.setType(CCConstants.LOM_TYPE_RELATION);
		
		TagDef relationKind = new TagDef();
		relationKind.setTag("kind");
		relationKind.getProperties().put(CCConstants.LOM_PROP_RELATION_KIND, "value");
		relation.getSubTags().add(relationKind);
		
		TagDef relationResource = new TagDef();
		relationResource.setTag("resource");
		relation.getSubTags().add(relationResource);
		
		TagDef relationResourceDescription = new TagDef();
		relationResourceDescription.setTag("description");
		relationResourceDescription.getProperties().put(CCConstants.LOM_PROP_RESOURCE_DESCRIPTION, "string");
		relationResource.getSubTags().add(relationResourceDescription);
		
		TagDef relationIdentifier = new TagDef();
		relationIdentifier.setTag("identifier");
		relationIdentifier.setType(CCConstants.LOM_TYPE_IDENTIFIER);
		relationIdentifier.getProperties().put(CCConstants.LOM_PROP_IDENTIFIER_CATALOG, "catalog");
		relationIdentifier.getProperties().put(CCConstants.LOM_PROP_IDENTIFIER_ENTRY, "entry");
		
		relationIdentifier.setChildAssociation(CCConstants.LOM_ASSOC_RESOURCE_IDENTIFIER);
		
		relation.getSubTags().add(relationIdentifier);
		
		
		TagDef classification = new TagDef();
		classification.setTag("classification");
		classification.setType(CCConstants.LOM_TYPE_CLASSIFICATION);
		TagDef purpose = new TagDef();
		purpose.setTag("purpose");
		purpose.getProperties().put(CCConstants.LOM_PROP_CLASSIFICATION_PURPOSE, "value");
		classification.getSubTags().add(purpose);
		
		TagDef taxonPath = new TagDef();
		taxonPath.setTag("taxonPath");
		taxonPath.setType(CCConstants.LOM_TYPE_TAXON_PATH);
		classification.getSubTags().add(taxonPath);
		
		TagDef taxonPathSource = new TagDef();
		taxonPathSource.setTag("source");
		taxonPathSource.getProperties().put(CCConstants.LOM_PROP_TAXONPATH_SOURCE, "string");
		taxonPath.getSubTags().add(taxonPathSource);
		
		TagDef taxon = new TagDef();
		taxon.setTag("taxon");
		taxon.setType(CCConstants.LOM_TYPE_TAXON);
		taxon.getProperties().put(CCConstants.LOM_PROP_TAXON_ID, "id");
		taxonPath.getSubTags().add(taxon);
		
		TagDef taxonEntry = new TagDef();
		taxonEntry.setTag("entry");
		taxonEntry.getProperties().put(CCConstants.LOM_PROP_TAXON_ENTRY, "string");
		taxon.getSubTags().add(taxonEntry);
		
		
		lomSubTags.add(general);
		lomSubTags.add(lifecycle);
		lomSubTags.add(metaMetadata);
		lomSubTags.add(technical);
		lomSubTags.add(educational);
		lomSubTags.add(rights);
		lomSubTags.add(relation);
		lomSubTags.add(classification);
		
	}
	
	private void processTag(TagDef tagDef, Element parentXMLElement, NodeRef nodeRef, String parentNodeType){
		//System.out.println(tagDef.tag + " parentXMLElement:" + parentXMLElement.getTagName() + " parentNodeType:" + parentNodeType);
		//switch to subnode
		
		/**
		 * reset noderef
		 */
		if(CCConstants.CCM_TYPE_IO.equals(parentNodeType)){
			this.nodeRef = new NodeRef(ioNodeRef.getStoreRef(),ioNodeRef.getId());
			nodeRef = new NodeRef(ioNodeRef.getStoreRef(),ioNodeRef.getId());
		}
		
		if(tagDef.getTag().equals("metaMetadata")){
			System.out.println("metaMetadata");
		}
		
		if(tagDef.getType() != null && !tagDef.getType().equals(parentNodeType)){
			List<ChildAssociationRef> childNodes = (tagDef.getChildAssociation() != null) ? 
					nodeService.getChildAssocs(nodeRef,QName.createQName(tagDef.getChildAssociation()),RegexQNamePattern.MATCH_ALL) : 
						nodeService.getChildAssocs(nodeRef);
			
			
			for(ChildAssociationRef childRef : childNodes){
				
				
				
				if(QName.createQName(tagDef.getType()).equals(nodeService.getType(childRef.getChildRef()))){
					this.nodeRef = childRef.getChildRef();
					processNodeRef(tagDef,parentXMLElement,childRef.getChildRef(),tagDef.getType());
				}
			}
		}else{
			this.nodeRef = nodeRef;
			processNodeRef(tagDef,parentXMLElement,nodeRef,parentNodeType);
		}
	}
	
	private void processNodeRef(TagDef tagDef, Element parentXMLElement, NodeRef nodeRef, String parentNodeType){
		
		
		
		Element element = (tagDef.getSubTag() != null) ? createAndAppendElement(tagDef, parentXMLElement, false) : createAndAppendElement(tagDef.getTag(), parentXMLElement);
		
		for(Map.Entry<String,String> entry : tagDef.getProperties().entrySet()){
			createAndAppendElement(entry.getValue(), element, QName.createQName(entry.getKey()));
		}
		
		for(TagDef subTag : tagDef.getSubTags()){
			processTag(subTag, element, nodeRef, parentNodeType);
		}
		
	}
	private void serializeResult(OutputStream os) throws IOException {
		XMLSerializer serializer = new XMLSerializer(os, new OutputFormat(doc,"UTF-8", true));
		serializer.serialize(doc);
	}
	public void export(String outputDir) {

		Document doc=toXML();
		if(doc==null)
			return;

		//formatted output:
		try{
			String sourceId = nodeRef.getId();//(String)nodeService.getProperty(nodeRef, QName.createQName(CCConstants.CM_NAME));
			//sourceId = sourceId.replaceAll(":", "_");
			
			File f = new File(outputDir + "/" + sourceId +".xml");
			FileOutputStream os = new FileOutputStream(f);
			serializeResult(os);
		}catch(IOException e){
			logger.error(e.getMessage(),e);
		}

	}

	public Document toXML() {
		QName type = nodeService.getType(nodeRef);

		if (!type.equals(QName.createQName(CCConstants.CCM_TYPE_IO))) {
			logger.error("this was not an io");
			return null;
		}

		Element lom = doc.createElement("lom");
		doc.appendChild(lom);


		lom.setAttribute("xmlns", "http://ltsc.ieee.org/xsd/LOM");
		lom.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
		lom.setAttribute("xsi:schemaLocation", "http://www.w3.org/2001/XMLSchema-instance");
		lom.setAttribute("xsi:schemaLocation", "http://ltsc.ieee.org/xsd/LOM  http://ltsc.ieee.org/xsd/lomv1.0/lom.xsd");

		for(TagDef tagDef : this.lom.getSubTags()){
			processTag(tagDef, lom, nodeRef, type.toString());
		}
		return doc;
	}


	public Element createAndAppendElement(String elementName, Element parent) {
		return this.createAndAppendElement(elementName, parent, (String) null);
	}
	
	public Element createAndAppendElement(String elementName, Element parent, QName property){
		return createAndAppendElement(elementName, parent, property,false);
	}

	public Element createAndAppendElement(String elementName, Element parent, QName property, boolean cdata) {
		
		
		if (property != null) {
			Serializable repoValue = nodeService.getProperty(nodeRef, property);
			return this.createAndAppendElement(elementName, parent, repoValue,cdata);
		}
		return null;
	}
	
	public Element createAndAppendElement(TagDef tagDef, Element parent, boolean cdata){
		
		Serializable repoValue = (tagDef.getSubTag() != null) ? nodeService.getProperty(nodeRef, QName.createQName(tagDef.getSubTagProperty())) : null;
		
		if(repoValue == null){
			return null;
		}
		
		if(repoValue instanceof MLText){
			MLText mlText = (MLText)repoValue;
			Element ele = this.createAndAppendElement(tagDef.getTag(), parent);
			appendMLText(mlText, ele);
			return ele;
		}
		
		
		if(repoValue instanceof List){
			for(Object lval:(List)repoValue){
				
				if(tagDef.getSubTag() != null){
					Element element = this.createAndAppendElement(tagDef.getTag(), parent);
					this.createAndAppendElement(tagDef.getSubTag(), element,(Serializable)lval,cdata);
				}else{
					this.createAndAppendElement(tagDef.getTag(), parent, (Serializable)lval,cdata);
				}
			}
			return null;
		}
		
		String value = null;
		if(repoValue instanceof Number){
			value = ((Number)repoValue).toString();
		}else if(repoValue instanceof Date){
			value =  ISO8601DateFormat.format((Date)repoValue);
		}else if(repoValue instanceof String){
			value = (String)repoValue;
		}else if(repoValue instanceof Boolean){
			value = ((Boolean)repoValue).toString();
		}else{
			logger.warn("unknown value type:"+repoValue.getClass().getName());
			value = repoValue.toString();
		}
		
		
		
		return this.createAndAppendElement(tagDef.getTag(), parent,value,cdata);
	}
	
	/**
	 * returns null when repoValue instanceof List
	 * @param elementName
	 * @param parent
	 * @param repoValue
	 * @return
	 */
	public Element createAndAppendElement(String elementName, Element parent, Serializable repoValue, boolean cdata){
		
		if(repoValue == null){
			return null;
		}
		
		if(repoValue instanceof MLText){
			MLText mlText = (MLText)repoValue;
			Element ele = this.createAndAppendElement(elementName, parent);
			appendMLText(mlText, ele);
			return ele;
		}
		
		
		if(repoValue instanceof List){
			for(Object lval:(List)repoValue){
				this.createAndAppendElement(elementName, parent, (Serializable)lval,cdata);
			}
			return null;
		}
		
		String value = null;
		if(repoValue instanceof Number){
			value = ((Number)repoValue).toString();
		}else if(repoValue instanceof Date){
			value =  ISO8601DateFormat.format((Date)repoValue);
		}else if(repoValue instanceof String){
			value = (String)repoValue;
		}else if(repoValue instanceof Boolean){
			value = ((Boolean)repoValue).toString();
		}else{
			logger.warn("unknown value type:"+repoValue.getClass().getName());
			value = repoValue.toString();
		}
		
		
		
		return this.createAndAppendElement(elementName, parent,value,cdata);
	}
	
	
	public void appendMLText(MLText mlText, Element mlElement){
		for(Map.Entry<Locale,String> entry : mlText.entrySet()){
			Element langEle = this.createAndAppendElement("string", mlElement,entry.getValue());
			langEle.setAttribute("language", entry.getKey().getLanguage());
		}
	}
	
	public Element createAndAppendElement(String elementName, Element parent, String textContent){
		return this.createAndAppendElement(elementName, parent, textContent,false);
	}

	public Element createAndAppendElement(String elementName, Element parent, String textContent, boolean cdata) {
		try{
			Element element = doc.createElement(elementName);
			
			if (textContent != null) {
				textContent = textContent.trim();
				
				if(cdata){
					element.appendChild(doc.createCDATASection(textContent));
				}else{
					element.setTextContent(textContent);
				}
			}
			parent.appendChild(element);
			return element;
		}catch(org.w3c.dom.DOMException e){
			e.printStackTrace();
			throw e;
		}
	}
}
