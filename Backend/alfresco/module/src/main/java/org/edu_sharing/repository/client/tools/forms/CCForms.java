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
package org.edu_sharing.repository.client.tools.forms;

import java.util.HashMap;
import java.util.Map;

import org.edu_sharing.repository.client.tools.CCConstants;


public class CCForms {
	/**
	 * Property, Formtype, Formlength, Asssociation , value, create, update
	 * ------------------------------------------------------------------------- 
	 * Property:
	 * - the Property of the Node
	 * Formtype:
	 * - gives Information how to handle this Property
	 * Formlength:
	 * - length of an String Field
	 * Association:
	 * - source or target
	 * - if Formtype == CC_FROM_OBJECT than it's the ChildAssociationName 
	 * value:
	 * - when when Formtype is CCConstants.CC_FORM_STRING or DATE than it's taken as defaultvalue(TODO try to get it out of request if not in, than take it as default value)
	 * - when value==CCConstants.CC_FORM_DEFAULTVALUE_PARENTID then the ID of the parentNode is taken
	 * - when Formtype is CCConstants.CC_FORM_AUTOMATIC than the value of this field is an property of the 
	 *   main node(will be set after the main node was created/updated) 
	 * - when FormType is CCConstants.CC_FORM_COPYFROMREQUEST the value is the name of the requestParam from which its copied
	 * create:
	 * - will be used by create
	 * update
	 * - will be used by update 
	 * idcontenturl 
	 * - Konstante über die man in der clientschicht die Contenturl bekommt die Property CCConstants.CM_PROP_CONTENT
	 *   kann nicht verwendet werden da die bei allen objekten gleich sind die von cm:content erben
	 */
	
	/**
	 * property variable for upload is an concatinated string (type + # + property)
	 * cause it can be more than one upload elements in the same form
	 */
	
	public static String[][] io = new String[][]{
		{CCConstants.CM_NAME, 							CCConstants.CC_FORM_STRING						,"250", null, null,CCConstants.CC_FORM_TRUE,CCConstants.CC_FORM_TRUE, null},
		{CCConstants.LOM_PROP_GENERAL_TITLE, 			CCConstants.CC_FORM_STRING						,"250", null, null,CCConstants.CC_FORM_TRUE,CCConstants.CC_FORM_TRUE, null},
		{CCConstants.CM_PROP_CONTENT,					CCConstants.CC_FORM_UPLOAD						,"250", null, null,CCConstants.CC_FORM_TRUE,CCConstants.CC_FORM_TRUE, CCConstants.CONTENTURL},
		{CCConstants.CCM_PROP_IO_WWWURL, 				CCConstants.CC_FORM_STRING						,"250", null, null,CCConstants.CC_FORM_TRUE,CCConstants.CC_FORM_TRUE, null},		
		{CCConstants.LOM_PROP_GENERAL_DESCRIPTION, 	    CCConstants.CC_FORM_STRING						,"250", null, null,CCConstants.CC_FORM_TRUE,CCConstants.CC_FORM_TRUE, null},
		{CCConstants.LOM_PROP_GENERAL_KEYWORD, 	    	CCConstants.CC_FORM_STRING						,"250", null, null,CCConstants.CC_FORM_TRUE,CCConstants.CC_FORM_TRUE, null},
		
	
		{CCConstants.CM_PROP_C_TITLE, 					CCConstants.CC_FORM_AUTOMATIC					,"250", null, CCConstants.LOM_PROP_GENERAL_TITLE,CCConstants.CC_FORM_TRUE,CCConstants.CC_FORM_TRUE, null},
		/*{CCConstants.CM_PROP_C_MODIFIED,				CCConstants.CC_FORM_DATE						,"250", null, null,CCConstants.CC_FORM_TRUE,CCConstants.CC_FORM_TRUE},*/
		/*wird automatisch gestzt{CCConstants.CM_PROP_C_CREATOR, 				CCConstants.CC_FORM_STRING						,"250", null, null,CCConstants.CC_FORM_TRUE,CCConstants.CC_FORM_TRUE, null},*/
		/*changed to LOM_PROP_EDUCATIONAL_LEARNINGRESOURCETYPE {CCConstants.CCM_PROP_IO_EDUCATIONKIND, 		CCConstants.CC_FORM_STRING						,"250", null, null,CCConstants.CC_FORM_TRUE,CCConstants.CC_FORM_TRUE, null},*/
		{CCConstants.CCM_PROP_IO_MEDIATYPE, 			CCConstants.CC_FORM_STRING						,"250", null, null,CCConstants.CC_FORM_TRUE,CCConstants.CC_FORM_TRUE, null},
		{CCConstants.CCM_PROP_IO_AUDIENCE, 				CCConstants.CC_FORM_STRING						,"250", null, null,CCConstants.CC_FORM_TRUE,CCConstants.CC_FORM_TRUE, null},
		{CCConstants.CCM_PROP_IO_SEMANTICTYPE, 			CCConstants.CC_FORM_STRING						,"250", null, null,CCConstants.CC_FORM_TRUE,CCConstants.CC_FORM_TRUE, null},
		{CCConstants.CCM_PROP_IO_PUBLISHER, 			CCConstants.CC_FORM_STRING						,"250", null, null,CCConstants.CC_FORM_TRUE,CCConstants.CC_FORM_TRUE, null},
		{CCConstants.CCM_PROP_IO_LICENSE, 				CCConstants.CC_FORM_STRING						,"250", null, null,CCConstants.CC_FORM_TRUE,CCConstants.CC_FORM_TRUE, null},
		{CCConstants.CCM_PROP_IO_ORIGINAL, 				CCConstants.CC_FORM_STRING						,"250", null, null,CCConstants.CC_FORM_TRUE,CCConstants.CC_FORM_TRUE, null},
		{CCConstants.CCM_PROP_IO_TOPIC, 				CCConstants.CC_FORM_STRING						,"250", null, null,CCConstants.CC_FORM_TRUE,CCConstants.CC_FORM_TRUE, null},
		{CCConstants.LOM_PROP_TECHNICAL_FORMAT, 		CCConstants.CC_FORM_AUTOMATIC					,"250", null, CCConstants.ALFRESCO_MIMETYPE,CCConstants.CC_FORM_TRUE,CCConstants.CC_FORM_TRUE, null},
		{CCConstants.CCM_PROP_IO_GROUPSIZE, 			CCConstants.CC_FORM_INT							,"250", null, null,CCConstants.CC_FORM_TRUE,CCConstants.CC_FORM_TRUE, null},
		{CCConstants.LOM_PROP_GENERAL_LANGUAGE, 		CCConstants.CC_FORM_STRING						,"250", null, null,CCConstants.CC_FORM_TRUE,CCConstants.CC_FORM_TRUE, null},
		{CCConstants.LOM_TYPE_IDENTIFIER, 	    		CCConstants.CC_FORM_OBJECT						,"250", CCConstants.LOM_ASSOC_IDENTIFIER, null,CCConstants.CC_FORM_TRUE,CCConstants.CC_FORM_TRUE, null},
		{CCConstants.LOM_TYPE_EDUCATIONAL, 	    		CCConstants.CC_FORM_OBJECT						,"250", CCConstants.LOM_ASSOC_EDUCATIONAL, null,CCConstants.CC_FORM_TRUE,CCConstants.CC_FORM_TRUE, null},
		{CCConstants.CM_TYPE_THUMBNAIL, 	    		CCConstants.CC_FORM_DONOTHING					,"250", CCConstants.CM_ASSOC_THUMBNAILS, null,CCConstants.CC_FORM_TRUE,CCConstants.CC_FORM_TRUE, null},
		{CCConstants.CCM_PROP_IO_LEARNINGGOAL, 			CCConstants.CC_FORM_STRING						,"250", null, null,CCConstants.CC_FORM_TRUE,CCConstants.CC_FORM_TRUE, null},
		{CCConstants.CCM_PROP_IO_GUIDANCESTUDENTS, 			CCConstants.CC_FORM_STRING					,"250", null, null,CCConstants.CC_FORM_TRUE,CCConstants.CC_FORM_TRUE, null},
		{CCConstants.CCM_PROP_IO_GUIDANCETEACHERS, 			CCConstants.CC_FORM_STRING					,"250", null, null,CCConstants.CC_FORM_TRUE,CCConstants.CC_FORM_TRUE, null},
		{CCConstants.CCM_PROP_IO_POINTSFROM, 			CCConstants.CC_FORM_INT							,"250", null, null,CCConstants.CC_FORM_TRUE,CCConstants.CC_FORM_TRUE, null},
		{CCConstants.CCM_PROP_IO_POINTSTO, 			CCConstants.CC_FORM_INT								,"250", null, null,CCConstants.CC_FORM_TRUE,CCConstants.CC_FORM_TRUE, null},
		{CCConstants.CCM_PROP_IO_POINTSDEFAULT, 			CCConstants.CC_FORM_INT						,"250", null, null,CCConstants.CC_FORM_TRUE,CCConstants.CC_FORM_TRUE, null},
		{CCConstants.CM_ASPECT_VERSIONABLE, 	    	CCConstants.CC_FORM_ASPECT						,"250", null, null,null,CCConstants.CC_FORM_TRUE, null},
		{CCConstants.CCM_PROP_IO_OBJECTTYPE, 			CCConstants.CC_FORM_INT							,"250", null, null,CCConstants.CC_FORM_TRUE,null, null},
		{CCConstants.CCM_PROP_IO_REPL_EDUCATIONAL_CONTEXT, CCConstants.CC_FORM_AUTOMATIC				,"250", null, CCConstants.LOM_PROP_EDUCATIONAL_CONTEXT,CCConstants.CC_FORM_TRUE,CCConstants.CC_FORM_TRUE, null},
		{CCConstants.CCM_PROP_IO_REPL_EDUCATIONAL_LEARNINGRESSOURCETYPE, CCConstants.CC_FORM_AUTOMATIC	,"250", null, CCConstants.LOM_PROP_EDUCATIONAL_LEARNINGRESOURCETYPE,CCConstants.CC_FORM_TRUE,CCConstants.CC_FORM_TRUE, null},
		{CCConstants.CCM_PROP_IO_REPL_EDUCATIONAL_TYPICALLEARNINGTIME, CCConstants.CC_FORM_AUTOMATIC	,"250", null, CCConstants.LOM_PROP_EDUCATIONAL_TYPICALLEARNINGTIME,CCConstants.CC_FORM_TRUE,CCConstants.CC_FORM_TRUE, null},
		{CCConstants.CCM_PROP_IO_REPL_TAXON_ENTRY, 		CCConstants.CC_FORM_AUTOMATIC						,"250", null, CCConstants.LOM_PROP_TAXON_ENTRY,CCConstants.CC_FORM_TRUE,CCConstants.CC_FORM_TRUE, null},
		{CCConstants.CCM_PROP_IO_REPL_CLASSIFICATION_KEYWORD, CCConstants.CC_FORM_AUTOMATIC				,"250", null, CCConstants.LOM_PROP_CLASSIFICATION_KEYWORD,CCConstants.CC_FORM_TRUE,CCConstants.CC_FORM_TRUE, null},
		{CCConstants.CCM_PROP_IO_COMMONLICENSE_KEY, 	CCConstants.CC_FORM_STRING						,"250", null, null,CCConstants.CC_FORM_TRUE,CCConstants.CC_FORM_TRUE, null},
		{CCConstants.CCM_PROP_IO_COMMONLICENSE_QUESTIONSALLOWED, 	    CCConstants.CC_FORM_STRING		,"250", null, null,CCConstants.CC_FORM_TRUE,CCConstants.CC_FORM_TRUE, null},
		{CCConstants.LOM_PROP_RIGHTS_RIGHTS_DESCRIPTION, 		CCConstants.CC_FORM_AUTOMATIC					,"250", null, CCConstants.CCM_PROP_IO_COMMONLICENSE_KEY,CCConstants.CC_FORM_TRUE,CCConstants.CC_FORM_TRUE, null}
	}; 
	
	
	public static String[][] preview = new String[][]{
		
		{CCConstants.CM_PROP_CONTENT,					CCConstants.CC_FORM_UPLOAD						,"250", null, null,CCConstants.CC_FORM_TRUE,CCConstants.CC_FORM_TRUE, CCConstants.CM_ASSOC_THUMBNAILS}
		
	};
	
	public static String[][] map = new String[][]{
		//{CCConstants.CM_NAME, 							CCConstants.CC_FORM_STRING						,"250", null, null,CCConstants.CC_FORM_TRUE,CCConstants.CC_FORM_TRUE, null},
		{CCConstants.CM_PROP_C_TITLE, 					CCConstants.CC_FORM_STRING						,"250", null, null,CCConstants.CC_FORM_TRUE,CCConstants.CC_FORM_TRUE, null},
		{CCConstants.CCM_PROP_MAP_ICON,					CCConstants.CC_FORM_UPLOAD						,"250", null, null,CCConstants.CC_FORM_TRUE,CCConstants.CC_FORM_TRUE, CCConstants.CCM_PROP_MAP_ICON}
		/*{CCConstants.CCM_PROP_MAP_X, 					CCConstants.CC_FORM_STRING						,"250", null,CCConstants.CC_FORM_TRUE,CCConstants.CC_FORM_TRUE, null},
		{CCConstants.CCM_PROP_MAP_Y, 					CCConstants.CC_FORM_STRING						,"250", null,CCConstants.CC_FORM_TRUE,CCConstants.CC_FORM_TRUE, null},*/
	};
	
	/*public static String[][] maplink = new String[][]{
		{CCConstants.CM_NAME, 							CCConstants.CC_FORM_STRING						,"250", null, null,CCConstants.CC_FORM_TRUE,CCConstants.CC_FORM_TRUE, null},
		{CCConstants.CCM_PROP_MAP_ICON,					CCConstants.CC_FORM_UPLOAD						,"250", null, null,CCConstants.CC_FORM_TRUE,CCConstants.CC_FORM_TRUE, CCConstants.CCM_PROP_MAP_ICON},
		{CCConstants.CCM_PROP_MAP_LINKTARGET,			CCConstants.CC_FORM_ASSOC						,"250", CCConstants.CC_ASSOC_FROM, null,CCConstants.CC_FORM_TRUE,CCConstants.CC_FORM_TRUE, null}
	};*/
	
	public static String[][] folder = new String[][]{
		//{CCConstants.CM_NAME, 							CCConstants.CC_FORM_STRING						,"250", null, null,CCConstants.CC_FORM_TRUE,CCConstants.CC_FORM_TRUE, null},
		{CCConstants.CM_PROP_C_TITLE, 					CCConstants.CC_FORM_STRING						,"250", null, null,CCConstants.CC_FORM_TRUE,CCConstants.CC_FORM_TRUE, null}
	};
	
	public static String[][] content = new String[][]{
		{CCConstants.CM_NAME, 							CCConstants.CC_FORM_STRING						,"250", null, null,CCConstants.CC_FORM_TRUE,CCConstants.CC_FORM_TRUE, null},
		{CCConstants.CM_PROP_CONTENT,					CCConstants.CC_FORM_UPLOAD						,"250", null, null,CCConstants.CC_FORM_TRUE,CCConstants.CC_FORM_TRUE, CCConstants.CONTENTURL}
	};
	
	public static String[][] lomEducational = new String[][]{
		{CCConstants.LOM_PROP_EDUCATIONAL_INTERACTIVITYTYPE, CCConstants.CC_FORM_STRING					,"250", null, null,CCConstants.CC_FORM_TRUE,CCConstants.CC_FORM_TRUE, null},
		{CCConstants.LOM_PROP_EDUCATIONAL_LEARNINGRESOURCETYPE, CCConstants.CC_FORM_STRING				,"250", null, null,CCConstants.CC_FORM_TRUE,CCConstants.CC_FORM_TRUE, null},
		{CCConstants.LOM_PROP_EDUCATIONAL_CONTEXT, CCConstants.CC_FORM_STRING							,"250", null, null,CCConstants.CC_FORM_TRUE,CCConstants.CC_FORM_TRUE, null},
		{CCConstants.LOM_PROP_EDUCATIONAL_LANGUAGE, CCConstants.CC_FORM_STRING							,"250", null, null,CCConstants.CC_FORM_TRUE,CCConstants.CC_FORM_TRUE, null},
		{CCConstants.LOM_PROP_EDUCATIONAL_DESCRIPTION, CCConstants.CC_FORM_STRING						,"250", null, null,CCConstants.CC_FORM_TRUE,CCConstants.CC_FORM_TRUE, null},
		{CCConstants.LOM_PROP_EDUCATIONAL_TYPICALLEARNINGTIME, CCConstants.CC_FORM_STRING				,"162", null, null,CCConstants.CC_FORM_TRUE,CCConstants.CC_FORM_TRUE, null},
		{CCConstants.LOM_PROP_EDUCATIONAL_TYPICALAGERANGE, CCConstants.CC_FORM_STRING					,"162", null, null,CCConstants.CC_FORM_TRUE,CCConstants.CC_FORM_TRUE, null}
		
	};
	
	//only consists of default value fields so it will not be shown
	public static String[][] lomIdentifier = new String[][]{
		{CCConstants.LOM_PROP_IDENTIFIER_CATALOG, CCConstants.CC_FORM_DEFAULT	,"250", null, CCConstants.NAMESPACE_CCM,CCConstants.CC_FORM_TRUE,CCConstants.CC_FORM_TRUE, null},
		{CCConstants.LOM_PROP_IDENTIFIER_ENTRY, CCConstants.CC_FORM_DEFAULT		,"250", null, CCConstants.CC_FORM_DEFAULTVALUE_PARENTID,CCConstants.CC_FORM_TRUE,CCConstants.CC_FORM_TRUE, null}
	};
	
	/*public static String[][] lomContribute = new String[][]{
		
		{CCConstants.LOM_PROP_CONTRIBUTE_ENTITY, 	CCConstants.CC_FORM_AUTOMATIC						,"250", null, CCConstants.CM_PROP_C_MODIFIER,CCConstants.CC_FORM_TRUE,CCConstants.CC_FORM_TRUE, null}
	};*/
	
	public static String[][] lomContribute = new String[][]{	
		{CCConstants.LOM_PROP_CONTRIBUTE_ROLE, 	CCConstants.CC_FORM_STRING		,"250", null, null,CCConstants.CC_FORM_TRUE,CCConstants.CC_FORM_TRUE, null},
		{CCConstants.LOM_PROP_CONTRIBUTE_ENTITY, 	CCConstants.CC_FORM_VCARD	,"250", null, null,CCConstants.CC_FORM_TRUE,CCConstants.CC_FORM_TRUE, null},
		{CCConstants.LOM_PROP_CONTRIBUTE_DATE, 	CCConstants.CC_FORM_DATE	,"250", null, null,CCConstants.CC_FORM_TRUE,CCConstants.CC_FORM_TRUE, null}
	};
	
	public static String[][] lomRelation = new String[][]{
		//7.2:Relation.Resource when the value of 7.1:Relation.Kind is "IsBasedOn".
		//{CCConstants.LOM_PROP_RELATION_KIND, 		CCConstants.CC_FORM_DEFAULT			,"250", null, "IsBasedOn",CCConstants.CC_FORM_TRUE, CCConstants.CC_FORM_TRUE, null},
		{CCConstants.LOM_PROP_RELATION_KIND, 		CCConstants.CC_FORM_STRING			,"250", null, null,CCConstants.CC_FORM_TRUE, CCConstants.CC_FORM_TRUE, null},
		{CCConstants.LOM_PROP_RESOURCE_DESCRIPTION, CCConstants.CC_FORM_STRING			,"250", null, null,CCConstants.CC_FORM_TRUE, CCConstants.CC_FORM_TRUE, null}
	};
	

	public static String[][] remoteobject = new String[][]{
		{CCConstants.CCM_PROP_REMOTEOBJECT_REPOSITORYID, CCConstants.CC_FORM_STRING			,"250", null, null,CCConstants.CC_FORM_TRUE,CCConstants.CC_FORM_TRUE, null},
		{CCConstants.CCM_PROP_REMOTEOBJECT_NODEID, CCConstants.CC_FORM_STRING				,"250", null, null,CCConstants.CC_FORM_TRUE,CCConstants.CC_FORM_TRUE, null},
		{CCConstants.CCM_PROP_REMOTEOBJECT_REPOSITORY_TYPE, CCConstants.CC_FORM_STRING		,"250", null, null,CCConstants.CC_FORM_TRUE,CCConstants.CC_FORM_TRUE, null}
	};
	
	public static String[][] taxon = new String[][]{
		{CCConstants.LOM_PROP_TAXON_ENTRY, CCConstants.CC_FORM_STRING						,"250", null, null,CCConstants.CC_FORM_TRUE,CCConstants.CC_FORM_TRUE, null},
		{CCConstants.LOM_PROP_TAXON_ID,    CCConstants.CC_FORM_STRING					    ,"250", null, null,CCConstants.CC_FORM_TRUE,CCConstants.CC_FORM_TRUE, null}
	};
	
	public static String[][] organisation = new String[][]{
		{CCConstants.CCM_PROP_ORGANISATION_CONTACT_FIRSTNAME, CCConstants.CC_FORM_STRING ,"250", null, null,CCConstants.CC_FORM_TRUE,CCConstants.CC_FORM_TRUE, null},
		{CCConstants.CCM_PROP_ORGANISATION_CONTACT_MAIL,    CCConstants.CC_FORM_STRING	 ,"250", null, null,CCConstants.CC_FORM_TRUE,CCConstants.CC_FORM_TRUE, null},
		{CCConstants.CCM_PROP_ORGANISATION_CONTACT_NAME,    CCConstants.CC_FORM_STRING	 ,"250", null, null,CCConstants.CC_FORM_TRUE,CCConstants.CC_FORM_TRUE, null},
		{CCConstants.CCM_PROP_ORGANISATION_DEFAULT,    		CCConstants.CC_FORM_BOOLEAN	 ,"250", null, null,CCConstants.CC_FORM_TRUE,CCConstants.CC_FORM_TRUE, null},
		{CCConstants.CCM_PROP_ORGANISATION_DESCRIPTION,    CCConstants.CC_FORM_STRING	 ,"250", null, null,CCConstants.CC_FORM_TRUE,CCConstants.CC_FORM_TRUE, null},
		{CCConstants.CCM_PROP_ORGANISATION_MAIL,    CCConstants.CC_FORM_STRING	 ,"250", null, null,CCConstants.CC_FORM_TRUE,CCConstants.CC_FORM_TRUE, null},
		{CCConstants.CCM_PROP_ORGANISATION_NAME,    CCConstants.CC_FORM_STRING	 ,"250", null, null,CCConstants.CC_FORM_TRUE,CCConstants.CC_FORM_TRUE, null},
		{CCConstants.CCM_PROP_ORGANISATION_URL,    CCConstants.CC_FORM_STRING	 ,"250", null, null,CCConstants.CC_FORM_TRUE,CCConstants.CC_FORM_TRUE, null}
	};
	
	
	public static int IDX_PROPERTY = 0;
	public static int IDX_FORM_TYPE = 1;
	public static int IDX_FORM_LENGTH = 2;
	public static int IDX_ASSOC_TYPE = 3;
	public static int IDX_FORM_VALUE = 4;
	public static int IDX_FORM_CREATE = 5;
	public static int IDX_FORM_UPDATE = 6;
	public static int IDX_IDCONTENTURL = 7;
	
	public static HashMap classes = null;
	
	public static HashMap getClasses(){
		if(classes == null){
			classes = new HashMap<>();
			classes.put(CCConstants.CCM_TYPE_IO, io);
			classes.put(CCConstants.CCM_TYPE_MAP, map);
			//classes.put(CCConstants.CCM_TYPE_MAPLINK, maplink);
			classes.put(CCConstants.CM_TYPE_FOLDER, folder);
			classes.put(CCConstants.CM_TYPE_CONTENT, content);
			classes.put(CCConstants.LOM_TYPE_EDUCATIONAL, lomEducational);
			classes.put(CCConstants.LOM_TYPE_IDENTIFIER, lomIdentifier);
			classes.put(CCConstants.LOM_TYPE_CONTRIBUTE, lomContribute);
			classes.put(CCConstants.LOM_TYPE_RELATION, lomRelation);
			classes.put(CCConstants.CCM_TYPE_REMOTEOBJECT, remoteobject);
			classes.put(CCConstants.CM_TYPE_THUMBNAIL, preview);
			classes.put(CCConstants.LOM_TYPE_TAXON, taxon);
			classes.put(CCConstants.CCM_TYPE_ORGANISATION, organisation);
		}
		return classes;
	}
	/**
	 * 
	 * @param type
	 * @param property
	 * @return Row Description
	 */
	
	public static String[] getRow(String type, String property){
		String[][] metadata = (String[][])getClasses().get(type);
		if(metadata != null){
			for(int i = 0; i < metadata.length; i++){
				if(metadata[i][IDX_PROPERTY].equals(property)){
					return metadata[i];
				}
			}
		}else{
			//MessageHelper.showDialogBox("No metadata found for type:"+type+" prop:"+property);
		}
		return null;
	}
	
	private static Map<String,String> propFormMapper = null;
	
	private static void initFormMapper(){
		if(propFormMapper == null){
			propFormMapper = new HashMap<>();
			propFormMapper.put(CCConstants.CCM_TYPE_IO+"#"+CCConstants.CM_PROP_CONTENT, CCConstants.CCM_TYPE_IO+"#"+CCConstants.CM_PROP_CONTENT);
			propFormMapper.put(CCConstants.CM_TYPE_THUMBNAIL+"#"+CCConstants.CM_PROP_CONTENT, CCConstants.CM_TYPE_THUMBNAIL+"#"+CCConstants.CM_PROP_CONTENT);
			propFormMapper.put(CCConstants.CCM_TYPE_MAP+"#"+CCConstants.CCM_PROP_MAP_ICON, CCConstants.CCM_TYPE_MAP+"#"+CCConstants.CCM_PROP_MAP_ICON);	
		}
	}
	/**
	 * normaly the property is also the form name
	 * inherited Node properties like CM_CONTENT can appear in > 1 Classes
	 * to make sure that an form name is unique this method delivers an unique form name
	 * 
	 * it's an concatinated String TYPE+"#"+PROPERTY
	 * 
	 * if the property is unique(only appears in one Class)
	 * ->it don't appears in propFormMapper -> the property is returned
	 *
	 * @TODO CCConstants.CC_FORM_AUTOMATIC copied Props musst be unique in one form
	 * so we must get the Type Information from the object where its copied from (maybe 
	 *  put type#property String instead only the property)
	 *
	 * 
	 * @param type
	 * @param property
	 * @return
	 */
	public static String getFormEleNameByProp(String type, String property){
		String result = null;
		initFormMapper();
		String inPropFormMapper = propFormMapper.get(type +"#"+property);
		if(inPropFormMapper == null){
			result = property;
		}else{
			result = inPropFormMapper;
		}
		return result;
	}
	/**
	 * form name can be an concatinated string (type + # + property)
	 * cause it can be more than one upload elements in the same form.
	 * 
	 * @param type
	 * @param formElementName
	 * @return property
	 */
	public static String getPropByFormEle(String type, String formElementName){
		
		initFormMapper();
		String tmpFormElementName = propFormMapper.get(formElementName);
		if(tmpFormElementName != null){
			
			String[] splitted = tmpFormElementName.split("#");
			return splitted[1];
		}else{
			//formEleName = property
			return formElementName;
		}
	}
	
	
	/**
	 * Child Assocition the ParentNode gots to it's childs
	 */
	private static Map<String,String> childAssocs = null;
	
	public static String getChildAssoc(String type){
		if(childAssocs == null){
			childAssocs = new HashMap<>();
			childAssocs.put(CCConstants.LOM_TYPE_CONTRIBUTE, CCConstants.LOM_ASSOC_LIFECYCLE_CONTRIBUTE);
			childAssocs.put(CCConstants.LOM_TYPE_RELATION, CCConstants.LOM_ASSOC_SCHEMA_RELATION);
		}
		return childAssocs.get(type);
	}
	
	
	/**
	 * is multivalue
	 */
	private static Map<String,Boolean> isMultiValue = null;
	
	public static Boolean isMultiValue(String property){
		if(isMultiValue == null){
			isMultiValue = new HashMap<>();
			isMultiValue.put(CCConstants.LOM_PROP_GENERAL_LANGUAGE, new Boolean(true));
			isMultiValue.put(CCConstants.LOM_PROP_EDUCATIONAL_LEARNINGRESOURCETYPE, new Boolean(true));
		}
		boolean result = (isMultiValue.get(property) == null) ? false : isMultiValue.get(property);
		return result;
	}
	
}
