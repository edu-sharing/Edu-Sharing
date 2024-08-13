package org.edu_sharing.repository.client.tools;

import java.util.*;

import static java.util.Collections.unmodifiableSet;

public class CCConstants {

	public final static String DIV = "CC_SEARCH";

	public final static String HTML_ID_MENUBAR = "cc-menubar-wrapper";

	public final static String CC_EXCEPTION_CYCLE_CHILDRELATION = "Übergeordneter Ordner kann nicht Kind eines untergeordneten Ordners werden!";

	public final static String CC_EXCEPTION_DUPLICATE_CHILD = "Kindknoten/Name des Knotens bereits vorhanden!";

	/**
	 * servlet mapping
	 */
	public final static String EDU_SHARING_SERVLET_PATH_PREFIX = "eduservlet/";

	public final static String EDU_SHARING_SERVLET_PATH_GWT_RPC = EDU_SHARING_SERVLET_PATH_PREFIX + "service";

	public final static String EDU_SHARING_SERVLET_PATH_CREATE = EDU_SHARING_SERVLET_PATH_PREFIX + "create";

	public final static String EDU_SHARING_SERVLET_PATH_UPDATE = EDU_SHARING_SERVLET_PATH_PREFIX + "update";

	public final static String EDU_SHARING_SERVLET_PATH_REDIRECT = EDU_SHARING_SERVLET_PATH_PREFIX + "redirect";

	public final static String EDU_SHARING_SERVLET_PATH_SERVERUPDATE = EDU_SHARING_SERVLET_PATH_PREFIX + "serverupdate";

	public final static String EDU_SHARING_SERVLET_PATH_REMOVEUSERNODES = EDU_SHARING_SERVLET_PATH_PREFIX + "removeusernodes";

	/**
	 * this is an dynamic prop that delivers the url to the content that means:
	 * if renderservice is configured it contains the render display url
	 * if no renderservice it contains the alfresco contenturl
	 * if node is a link and has the CCM_PROP_IO_WWWURL prop than content url has this value
	 */
	public final static String CONTENTURL = "ContentURL";

	/**
	 * this is an dynamic prop that delivers the url to download the content of an node
	 * it is only set when there is an ALFRESCO_MIMETYPE set (which means it has content)
	 * and the renderservice is configured
	 */
	public final static String DOWNLOADURL = "DownloadURL";


	public final static String CCREP_PROTOCOL = "ccrep://";

	/**
	 * will be used when there is an Alfresco content
	 */
	public final static String ALFRESCO_MIMETYPE = "AlfrescoMimeType";

	public final static String ASSOCIATION = "Association";

	public final static String CHILD_ASSOCIATION = "CHILD_ASSOCIATION";

	/**
	 * will be used as property for client when type got's no readable name like container type
	 */
	public final static String CHILD_ASSOCIATION_NAME = "CHILD_ASSOCIATION_NAME";

	public final static String NODETYPE = "NodeType";

	public final static String NODEID = "NodeID";

	public final static String NODECREATOR_FIRSTNAME  ="NodeCreator_FirstName";
	public final static String NODECREATOR_LASTNAME   ="NodeCreator_LastName";
	public final static String NODECREATOR_EMAIL 	  ="NodeCreator_EMail";

	public final static String NODEMODIFIER_FIRSTNAME ="NodeModifier_FirstName";
	public final static String NODEMODIFIER_LASTNAME  ="NodeModifier_LastName";
	public final static String NODEMODIFIER_EMAIL 	  ="NodeModifier_EMail";

	public final static String DETAILS_PANEL_HTML = "DETAILS_PANEL_HTML";

	public final static String VERSION_STORE_NODEID = "VERSION_STORE_NODEID";

	public final static String ARCHIVE_STOREREF = "archive://SpacesStore";

	public final static String VERSION_STOREREF = "workspace://version2Store";

	public final static String STORE_WORKSPACES_SPACES = "workspace://SpacesStore";

	public final static String PARENTID = "ParentID";

	public final static String BASKETS_PARENT_PREFIX = "CC_FAVORITES_";

	public final static String AUTH_USERNAME = "UserName";

	public final static String AUTH_USERNAME_CAPTION = "AUTH_USERNAME_CAPTION";

	public final static String AUTH_USER_HOMEDIR = "AUTH_USER_HOMEDIR";

	/**
	 * use this carefully only for non security related decisions. better use the isAdmin rpc method
	 */
	public final static String AUTH_USER_ISADMIN = "AUTH_USER_ISADMIN";

	public final static String AUTH_TICKET = "Ticket";

	public final static String AUTH_SESSIONID = "SessionID";

	public final static String AUTH_SSO_SESSIONID = "SSOSessionID";

	public final static String AUTH_REPOSITORY = "AUTH_REPOSITORY";

	public final static String AUTH_LOCALE = "AUTH_LOCALE";

	public final static String AUTH_SCOPE = "AUTH_SCOPE";

	public final static String AUTH_TYPE = "AUTH_TYPE";

	public final static String AUTH_TYPE_DEFAULT = "AUTH_TYPE_DEFAULT";

	public final static String AUTH_TYPE_SIGNATURE = "AUTH_TYPE_SIGNATURE";

	public final static String AUTH_TYPE_SHIBBOLETH = "AUTH_TYPE_SHIBBOLETH";

	public final static String AUTH_TYPE_CAS = "AUTH_TYPE_CAS";

	public final static String AUTH_TYPE_OAUTH = "AUTH_TYPE_OAUTH";
	
	public final static String AUTH_TYPE_TICKET = "AUTH_TYPE_TICKET";

	public final static String AUTH_TYPE_LTI = "AUTH_TYPE_LTI";

	public final static String AUTH_ACCESS_TOKEN = "AUTH_ACCESS_TOKEN";

	public final static String AUTH_HEADER_EDU_TICKET = "EDU-TICKET";

	/**
	 * access on a node validated by lms
	 */
	public final static String AUTH_SINGLE_USE_NODEID = "AUTH_SINGLE_USE_NODEID";

	public final static String AUTH_SINGLE_USE_TIMESTAMP = "AUTH_SINGLE_USE_TIMESTAMP";

	public final static String REPOSITORY_ID = "REPOSITORY_ID";

	public final static String REPOSITORY_CAPTION = "REPOSITORY_CAPTION";

	public final static String REPOSITORY_FILE_HOME = "homeApplication.properties.xml";

	public final static String REPOSITORY_TYPE = "REPOSITORY_TYPE";

	public final static String APPLICATIONTYPE_LMS = "LMS";

	public final static String APPLICATIONTYPE_REPOSITORY = "REPOSITORY";

	public final static String APPLICATIONTYPE_SERVICE = "SERVICE";

	public static final String PROP_USERNAME =  "{http://www.alfresco.org/model/content/1.0}userName";
	public static final String PROP_USER_HOMEFOLDER = "{http://www.alfresco.org/model/content/1.0}homeFolder";
	public static final String PROP_USER_FIRSTNAME = "{http://www.alfresco.org/model/content/1.0}firstName";
	public static final String PROP_USER_MIDDLENAME = "{http://www.alfresco.org/model/content/1.0}middleName";
	public static final String PROP_USER_LASTNAME = "{http://www.alfresco.org/model/content/1.0}lastName";
	public static final String PROP_USER_EMAIL = "{http://www.alfresco.org/model/content/1.0}email";
	public static final String PROP_USER_ORGID = "{http://www.alfresco.org/model/content/1.0}organizationId";
	public static final String PROP_USER_ISSHADOWUSER = "{http://www.alfresco.org/model/content/1.0}isShadowUser";
	public static final String PROP_USER_REPOSITORYID = "{http://www.alfresco.org/model/content/1.0}repositoryId";
	public static final String PROP_USER_ESUID = "{http://www.alfresco.org/model/content/1.0}esuid";
	public final static String PROP_USER_SECONDARY_IDS = "{http://www.alfresco.org/model/content/1.0}essecondaryIds";
	public static final String PROP_USER_ESREMOTEROLES = "{http://www.alfresco.org/model/content/1.0}esremoteroles";
	public static final String PROP_USER_ESSSOTYPE = "{http://www.alfresco.org/model/content/1.0}esssotype";
	/**
	 * the first time this user authenticated
	 * Note: This will only be set when the user authenticates itself, not if i.e. he is only getting provisioned via api
	 */
	public static final String PROP_USER_ESFIRSTLOGIN = "{http://www.alfresco.org/model/content/1.0}esFirstLogin";
	public static final String PROP_USER_ESLASTLOGIN = "{http://www.alfresco.org/model/content/1.0}esLastLogin";
	public static final String ASSOC_USER_AVATAR = "{http://www.alfresco.org/model/content/1.0}avatar";
	public static final String ASSOC_USER_PREFERENCEIMAGE = "{http://www.alfresco.org/model/content/1.0}preferenceImage";

	//public final static String NODETYPE_BASKET = "{campus.content.model}basketfolder";

	public final static String NAMESPACE_CCM ="http://www.campuscontent.de/model/1.0";

	public final static String NAMESPACE_LOM = "http://www.campuscontent.de/model/lom/1.0";

	public final static String NAMESPACE_CM = "http://www.alfresco.org/model/content/1.0";

	public final static String NAMESPACE_SYS = "http://www.alfresco.org/model/system/1.0";

	public final static String NAMESPACE_VIRTUAL = "virtualproperty";

	public final static String NAMESPACE_EXIF = "http://www.alfresco.org/model/exif/1.0";

	public final static String NAMESPACE_SHORT_CCM = "ccm";

	public final static String NAMESPACE_SHORT_LOM = "cclom";

	public final static String NAMESPACE_SHORT_CM = "cm";

	public final static String NAMESPACE_SHORT_SYS = "sys";

	public final static String NAMESPACE_SHORT_VIRTUAL = "virtual";

	public final static String NAMESPACE_SHORT_EXIF = "exif";

	public final static String CCM_TYPE_BASKET = "{http://www.campuscontent.de/model/1.0}basketfolder";

	public final static String CM_TYPE_THUMBNAIL = "{http://www.alfresco.org/model/content/1.0}thumbnail";

	public final static String ACTION_NAME_CREATE_THUMBNAIL = "create-thumbnail";

	public final static String ACTION_NAME_RESOURCEINFO = "cc-ressourceinfo-action";


	public final static String CCM_TYPE_IO = "{http://www.campuscontent.de/model/1.0}io";

	public final static String CCM_TYPE_CONFIGOBJECT = "{http://www.campuscontent.de/model/1.0}configobject";

	public final static String CCM_TYPE_COLLECTION_PROPOSAL = "{http://www.campuscontent.de/model/1.0}collection_proposal";

	public final static String CCM_TYPE_SAVED_SEARCH = "{http://www.campuscontent.de/model/1.0}saved_search";

	public final static String CCM_TYPE_COMMENT = "{http://www.campuscontent.de/model/1.0}comment";

	public final static String CCM_TYPE_COLLECTION_FEEDBACK = "{http://www.campuscontent.de/model/1.0}collection_feedback";

	public final static String CCM_TYPE_MATERIAL_FEEDBACK = "{http://www.campuscontent.de/model/1.0}material_feedback";

	public final static String CCM_TYPE_RATING = "{http://www.campuscontent.de/model/1.0}rating";

	public final static String CCM_TYPE_LO = "{http://www.campuscontent.de/model/1.0}lo";

	public final static String CCM_TYPE_EO = "{http://www.campuscontent.de/model/1.0}eo";

	public final static String CCM_TYPE_ES = "{http://www.campuscontent.de/model/1.0}es";

	public final static String CCM_TYPE_MAP = "{http://www.campuscontent.de/model/1.0}map";

	public final static String CCM_TYPE_SYSUPDATE = "{http://www.campuscontent.de/model/1.0}sysupdate";

	public final static String CCM_TYPE_MAPRELATION = "{http://www.campuscontent.de/model/1.0}maprelation";

	public final static String CCM_TYPE_REMOTEOBJECT = "{http://www.campuscontent.de/model/1.0}remoteobject";

	public final static String CCM_TYPE_ASSIGNED_LICENSE = "{http://www.campuscontent.de/model/1.0}assignedlicense";

	public final static String CCM_TYPE_ORGANISATION ="{http://www.campuscontent.de/model/1.0}organisation";

	public final static String CCM_TYPE_NOTIFY = "{http://www.campuscontent.de/model/1.0}notify";

	public final static String CCM_TYPE_SHARE = "{http://www.campuscontent.de/model/1.0}share";

	public final static String CCM_TYPE_TOOLPERMISSION = "{http://www.campuscontent.de/model/1.0}toolpermission";

	public final static String CCM_TYPE_TOOL_INSTANCE = "{http://www.campuscontent.de/model/1.0}tool_instance";

	public final static String CCM_TYPE_SERVICE_NODE = "{http://www.campuscontent.de/model/1.0}service_node";

	public final static String LOM_TYPE_IDENTIFIER ="{http://www.campuscontent.de/model/lom/1.0}identifier";

	public final static String LOM_TYPE_EDUCATIONAL = "{http://www.campuscontent.de/model/lom/1.0}educational";

	public final static String LOM_TYPE_CONTRIBUTE = "{http://www.campuscontent.de/model/lom/1.0}contribute";

	public final static String LOM_TYPE_RELATION = "{http://www.campuscontent.de/model/lom/1.0}relation";

	public final static String LOM_TYPE_CLASSIFICATION = "{http://www.campuscontent.de/model/lom/1.0}classification";

	public final static String LOM_TYPE_TAXON_PATH = "{http://www.campuscontent.de/model/lom/1.0}taxonpath";

	public final static String LOM_TYPE_TAXON = "{http://www.campuscontent.de/model/lom/1.0}taxon";

	public final static String SYS_PROP_NODE_UID = "{http://www.alfresco.org/model/system/1.0}node-uuid";

	public final static String SYS_PROP_NODE_DBID = "{http://www.alfresco.org/model/system/1.0}node-dbid";

	public final static String SYS_PROP_STORE_PROTOCOL = "{http://www.alfresco.org/model/system/1.0}store-protocol";

	public final static String SYS_PROP_STORE_IDENTIFIER = "{http://www.alfresco.org/model/system/1.0}store-identifier";

	public final static String SYS_TYPE_CONTAINER = "{http://www.alfresco.org/model/system/1.0}container";

	public final static String SYS_PROP_ARCHIVED_ORIGINAL_PARENT_ASSOC = "{http://www.alfresco.org/model/system/1.0}archivedOriginalParentAssoc";

	public final static String CM_ASPECT_VERSIONABLE = "{http://www.alfresco.org/model/content/1.0}versionable";

	public final static String CM_NAME = "{http://www.alfresco.org/model/content/1.0}name";

	public final static String CM_PROP_TITLE = "{http://www.alfresco.org/model/content/1.0}title";

	public final static String CM_PROP_DESCRIPTION = "{http://www.alfresco.org/model/content/1.0}description";

	public final static String CM_PROP_C_CREATED = "{http://www.alfresco.org/model/content/1.0}created";

	public final static String CM_PROP_C_TITLE ="{http://www.alfresco.org/model/content/1.0}title";

	public final static String CM_PROP_C_MODIFIED ="{http://www.alfresco.org/model/content/1.0}modified";

	public final static String CM_PROP_C_CREATOR ="{http://www.alfresco.org/model/content/1.0}creator";

	public final static String CM_PROP_C_MODIFIER ="{http://www.alfresco.org/model/content/1.0}modifier";

	public final static String CM_PROP_CONTENT = "{http://www.alfresco.org/model/content/1.0}content";

	public final static String CM_ASSOC_ORIGINAL = "{http://www.alfresco.org/model/content/1.0}original";

	public final static String CM_TYPE_FOLDER = "{http://www.alfresco.org/model/content/1.0}folder";

	public final static String SYS_STORE_ROOT = "{http://www.alfresco.org/model/system/1.0}store_root";

	public final static String CM_TYPE_PERSON = "{http://www.alfresco.org/model/content/1.0}person";

	public final static String CM_TYPE_CONTENT = "{http://www.alfresco.org/model/content/1.0}content";

	public final static String CM_TYPE_OBJECT = "{http://www.alfresco.org/model/content/1.0}cmobject";

	public final static String CM_TYPE_CONTAINER = "{http://www.alfresco.org/model/system/1.0}container";

	public final static String CM_TYPE_AUTHORITY_CONTAINER = "{http://www.alfresco.org/model/content/1.0}authorityContainer";

	public final static String CM_PROP_PERSON_EMAIL = "{http://www.alfresco.org/model/content/1.0}email";

	public final static String CM_PROP_PERSON_FIRSTNAME = "{http://www.alfresco.org/model/content/1.0}firstName";

	public final static String CM_PROP_PERSON_LASTNAME = "{http://www.alfresco.org/model/content/1.0}lastName";

	public final static String CM_PROP_PERSON_HOME_FOLDER = "{http://www.alfresco.org/model/content/1.0}homeFolder";

	public final static String CM_PROP_PERSON_USERNAME = "{http://www.alfresco.org/model/content/1.0}userName";

	public final static String CM_PROP_AUTHORITY_NAME = "{http://www.alfresco.org/model/content/1.0}authorityName";

	public final static String CM_PROP_PERSON_SIZE_CURRENT = "{http://www.alfresco.org/model/content/1.0}sizeCurrent";

	public final static String CM_PROP_PERSON_SIZE_QUOTA = "{http://www.alfresco.org/model/content/1.0}sizeQuota";

	public final static String CM_PROP_PERSON_ESORIGINALUID = "{http://www.alfresco.org/model/content/1.0}esoriginaluid";

	public final static String CM_PROP_PERSON_ESAFFILIATIONS = "{http://www.alfresco.org/model/content/1.0}esaffiliations";

	public final static String CM_PROP_PERSON_ESPERSONSTATUS = "{http://www.alfresco.org/model/content/1.0}espersonstatus";
	public final static String CM_PROP_PERSON_ESPERSONSTATUSDATE = "{http://www.alfresco.org/model/content/1.0}espersonstatusdate";

	public final static String CM_PROP_PERSON_EDU_SCHOOL_PRIMARY_AFFILIATION = "{http://www.alfresco.org/model/content/1.0}eduSchoolPrimaryAffiliation";

	public final static String CM_PROP_OWNER = "{http://www.alfresco.org/model/content/1.0}owner";

	public final static String CCM_PROP_PERSON_PREFERENCES = "{http://www.campuscontent.de/model/1.0}userPreferences";

	public final static String CCM_PROP_PERSON_NOTIFICATION_PREFERENCES = "{http://www.campuscontent.de/model/1.0}userNotificationPreferences";

	public final static String CCM_PROP_PERSON_SHOW_EMAIL = "{http://www.campuscontent.de/model/1.0}showEmail";

	public final static String CCM_PROP_PERSON_RECENTLY_INVITED = "{http://www.campuscontent.de/model/1.0}userRecentlyInvited";

	public final static String CCM_PROP_PERSON_RECENT_COLLECTIONS = "{http://www.campuscontent.de/model/1.0}userRecentCollections";

	public final static String CCM_PROP_PERSON_NODE_LISTS = "{http://www.campuscontent.de/model/1.0}userNodeLists";

	public final static String CCM_PROP_PERSON_EDUCATIONAL_AGERANGE_FROM = "{http://www.campuscontent.de/model/1.0}userEducationalAgeRangeFrom";

	public final static String CCM_PROP_PERSON_EDUCATIONAL_AGERANGE_TO = "{http://www.campuscontent.de/model/1.0}userEducationalAgeRangeTo";

	public final static String CCM_PROP_PH_MODIFIED = "{http://www.campuscontent.de/model/1.0}ph_modified";

	public final static String CCM_PROP_PH_HISTORY = "{http://www.campuscontent.de/model/1.0}ph_history";

	public final static String CCM_PROP_PH_INVITED = "{http://www.campuscontent.de/model/1.0}ph_invited";

	public final static String CCM_PROP_PH_USERS = "{http://www.campuscontent.de/model/1.0}ph_users";

	public final static String CCM_PROP_PH_ACTION = "{http://www.campuscontent.de/model/1.0}ph_action";

	/**
	 * custom edu-sharing person attribute
	 */
	public final static String CM_PROP_PERSON_GUID = "{http://www.alfresco.org/model/content/1.0}guid";

	public final static String CM_ASSOC_FOLDER_CONTAINS = "{http://www.alfresco.org/model/content/1.0}contains";

	public final static String CM_ASSOC_MEMBER = "{http://www.alfresco.org/model/content/1.0}member";

	public final static String CM_PROP_VERSIONABLELABEL = "{http://www.alfresco.org/model/content/1.0}versionLabel";


	/*Person*/

	public final static String CM_TYPE_PERSONACCESSELEMENT = "{http://www.alfresco.org/model/content/1.0}personaccesselement";

	public final static String CM_PROP_PERSONACCESSELEMENT_CCAPPID ="{http://www.alfresco.org/model/content/1.0}ccappid";

	public final static String CM_PROP_PERSONACCESSELEMENT_CCUSERID ="{http://www.alfresco.org/model/content/1.0}ccuserid";

	public final static String CM_PROP_PERSONACCESSELEMENT_CCACCESS ="{http://www.alfresco.org/model/content/1.0}ccaccess";

	public final static String CM_PROP_PERSONACCESSELEMENT_CCACTIVATEKEY ="{http://www.alfresco.org/model/content/1.0}ccactivatekey";


	public final static String CM_PROP_THUMBNAIL_THUMBNAILNAME = "{http://www.alfresco.org/model/content/1.0}thumbnailName";

	public final static String CM_PROP_THUMBNAIL_CONTENT_PROPERTY_NAME = "{http://www.alfresco.org/model/content/1.0}contentPropertyName";

	public final static String CM_PROP_AUTHORITY_AUTHORITYNAME = "{http://www.alfresco.org/model/content/1.0}authorityName";

	public final static String CM_PROP_AUTHORITY_AUTHORITYDISPLAYNAME = "{http://www.alfresco.org/model/content/1.0}authorityDisplayName";

	public final static String CM_ASSOC_PERSON_ACCESSLIST = "{http://www.alfresco.org/model/content/1.0}accesslist";


	public final static String CCM_PROP_SAVED_SEARCH_REPOSITORY = "{http://www.campuscontent.de/model/1.0}saved_search_repository";
	public final static String CCM_PROP_SAVED_SEARCH_MDS = "{http://www.campuscontent.de/model/1.0}saved_search_mds";
	public final static String CCM_PROP_SAVED_SEARCH_QUERY = "{http://www.campuscontent.de/model/1.0}saved_search_query";
	public final static String CCM_PROP_SAVED_SEARCH_PARAMETERS = "{http://www.campuscontent.de/model/1.0}saved_search_parameters";

	public final static String CCM_PROP_COMMENT_REPLY = "{http://www.campuscontent.de/model/1.0}comment_reply";
	public final static String CCM_PROP_COMMENT_CONTENT = "{http://www.campuscontent.de/model/1.0}comment_content";

	public final static String CCM_PROP_COLLECTION_FEEDBACK_AUTHORITY = "{http://www.campuscontent.de/model/1.0}collection_feedback_authority";

	public final static String CCM_PROP_COLLECTION_FEEDBACK_DATA = "{http://www.campuscontent.de/model/1.0}collection_feedback_data";


	public final static String CCM_PROP_MATERIAL_FEEDBACK_AUTHORITY = "{http://www.campuscontent.de/model/1.0}material_feedback_authority";

	public final static String CCM_PROP_MATERIAL_FEEDBACK_DATA = "{http://www.campuscontent.de/model/1.0}material_feedback_data";

	public final static String CCM_PROP_RATING_VALUE = "{http://www.campuscontent.de/model/1.0}rating_value";
	public final static String CCM_PROP_RATING_TEXT = "{http://www.campuscontent.de/model/1.0}rating_text";

	public final static String CCM_PROP_FORKED_ORIGIN = "{http://www.campuscontent.de/model/1.0}forked_origin";
	public final static String CCM_PROP_FORKED_ORIGIN_VERSION = "{http://www.campuscontent.de/model/1.0}forked_origin_version";

	/*CCMODEL*/
	public final static String CCM_ASPECT_USER_EXTENSION ="{http://www.campuscontent.de/model/1.0}userExtension";

	public final static String CCM_ASPECT_LICENSES ="{http://www.campuscontent.de/model/1.0}licenses";

	public final static String CCM_MEDIACENTER_STATISTICS ="{http://www.campuscontent.de/model/1.0}mediacenter_statistics";

	public final static String CCM_ASPECT_FORKED ="{http://www.campuscontent.de/model/1.0}forked";

	public final static String CCM_ASPECT_COLLECTION = "{http://www.campuscontent.de/model/1.0}collection";

	public final static String CCM_ASPECT_COLLECTION_PINNED = "{http://www.campuscontent.de/model/1.0}collection_pinned";

	public final static String CCM_ASPECT_COLLECTION_ORDERED = "{http://www.campuscontent.de/model/1.0}collection_ordered";

	public final static String CCM_ASPECT_COLLECTION_IO_REFERENCE = "{http://www.campuscontent.de/model/1.0}collection_io_reference";

	public final static String CCM_ASPECT_TOOL_DEFINITION = "{http://www.campuscontent.de/model/1.0}tool_definition";

	public final static String CCM_ASPECT_TOOL_OBJECT = "{http://www.campuscontent.de/model/1.0}tool_object";

	public final static String CCM_ASPECT_PERMISSION_HISTORY = "{http://www.campuscontent.de/model/1.0}permission_history";

	public final static String CCM_ASPECT_TRACKING = "{http://www.campuscontent.de/model/1.0}tracking";

	public final static String CCM_ASPECT_EDUCONTEXT = "{http://www.campuscontent.de/model/1.0}educontext";

	public final static String CCM_PROP_TOOL_OBJECT_TOOLINSTANCEREF = "{http://www.campuscontent.de/model/1.0}tool_instance_ref";

	public final static String CCM_PROP_IO_REF_TITLE = "{http://www.campuscontent.de/model/1.0}ref_title";

	public final static String CCM_PROP_IO_REF_DESCRIPTION = "{http://www.campuscontent.de/model/1.0}ref_description";

	public final static String CCM_PROP_IO_REF_VERSION = "{http://www.campuscontent.de/model/1.0}ref_version";

	public final static String CCM_PROP_IO_REF_VIDEO_VTT = "{http://www.campuscontent.de/model/1.0}ref_video_vtt";

	public final static String CCM_PROP_MAP_REF_TARGET = "{http://www.campuscontent.de/model/1.0}map_ref_target";

	public final static String CCM_PROP_ADDRESS_POSTALCODE = "{http://www.campuscontent.de/model/1.0}postalCode";

	public final static String CCM_PROP_ADDRESS_CITY = "{http://www.campuscontent.de/model/1.0}city";

	public final static String CCM_ASPECT_POSITIONABLE = "{http://www.campuscontent.de/model/1.0}positionable";

	public final static String CCM_ASPECT_COMMON_LICENSES ="{http://www.campuscontent.de/model/1.0}commonlicenses";

	public final static String CCM_ASPECT_EDUGROUP = "{http://www.campuscontent.de/model/1.0}edugroup";

	public final static String CCM_ASPECT_MAP_REF = "{http://www.campuscontent.de/model/1.0}map_ref";

	public final static String CCM_ASPECT_GROUPEXTENSION = "{http://www.campuscontent.de/model/1.0}groupExtension";

	public final static String CCM_ASPECT_MEDIACENTER = "{http://www.campuscontent.de/model/1.0}mediacenter";

	public final static String CCM_ASPECT_ADDRESS = "{http://www.campuscontent.de/model/1.0}address";

	// describes that the node points onto a remote node, e.g. youtube
	public final static String CCM_ASPECT_REMOTEREPOSITORY = "{http://www.campuscontent.de/model/1.0}remoterepository";

	public final static String CCM_PROP_GROUPEXTENSION_GROUPTYPE = "{http://www.campuscontent.de/model/1.0}groupType";

	public final static String CCM_PROP_GROUPEXTENSION_GROUPEMAIL = "{http://www.campuscontent.de/model/1.0}groupEmail";

	public final static String CCM_PROP_GROUPEXTENSION_GROUPSOURCE = "{http://www.campuscontent.de/model/1.0}groupSource";

	public final static String CCM_PROP_COLLECTION_PROPOSAL_TARGET = "{http://www.campuscontent.de/model/1.0}collection_proposal_target";
	public final static String CCM_PROP_COLLECTION_PROPOSAL_STATUS = "{http://www.campuscontent.de/model/1.0}collection_proposal_status";
	public final static String CCM_PROP_COLLECTION_PROPOSAL_COMMENT = "{http://www.campuscontent.de/model/1.0}collection_proposal_comment";

	public final static String CCM_PROP_MEDIACENTER_ID = "{http://www.campuscontent.de/model/1.0}mediacenterId";
	public final static String CCM_PROP_MEDIACENTER_DISTRICT_ABBREVIATION = "{http://www.campuscontent.de/model/1.0}mediacenterDistrictAbbreviation";
	public final static String CCM_PROP_MEDIACENTER_MAIN_URL = "{http://www.campuscontent.de/model/1.0}mediacenterMainUrl";
	public final static String CCM_PROP_MEDIACENTER_CATALOGS = "{http://www.campuscontent.de/model/1.0}mediacenterCatalogs";

	public final static String CCM_PROP_AUTHOR_FREETEXT = "{http://www.campuscontent.de/model/1.0}author_freetext";

	public final static String CCM_PROP_LINKTYPE = "{http://www.campuscontent.de/model/1.0}linktype";

	public final static String CCM_PROP_TOOLPERMISSION_SYSTEM_MANAGED = "{http://www.campuscontent.de/model/1.0}toolpermission_system_managed";

	public final static String CCM_PROP_TOOL_INSTANCE_KEY = "{http://www.campuscontent.de/model/1.0}tool_instance_key";

	public final static String CCM_PROP_TOOL_INSTANCE_SECRET = "{http://www.campuscontent.de/model/1.0}tool_instance_secret";

    public final static String CCM_PROP_SERVICE_NODE_NAME = "{http://www.campuscontent.de/model/1.0}service_node_name";
    public final static String CCM_PROP_SERVICE_NODE_DESCRIPTION = "{http://www.campuscontent.de/model/1.0}service_node_description";
    public final static String CCM_PROP_SERVICE_NODE_TYPE = "{http://www.campuscontent.de/model/1.0}service_node_type";
    public final static String CCM_PROP_SERVICE_NODE_DATA = "{http://www.campuscontent.de/model/1.0}service_node_data";

    public final static String CCM_PROP_EDUGROUP_EDU_HOMEDIR = "{http://www.campuscontent.de/model/1.0}edu_homedir";

	public final static String CCM_ASPECT_SHARES = "{http://www.campuscontent.de/model/1.0}shares";

	public final static String CCM_ASPECT_WORKFLOW = "{http://www.campuscontent.de/model/1.0}workflow";

	public static final String CCM_PROP_WF_RECEIVER = "{http://www.campuscontent.de/model/1.0}wf_receiver";

	public static final String CCM_PROP_WF_STATUS = "{http://www.campuscontent.de/model/1.0}wf_status";

	public static final String CCM_PROP_TRACKING_DOWNLOADS = "{http://www.campuscontent.de/model/1.0}tracking_downloads";

	public static final String CCM_PROP_TRACKING_VIEWS = "{http://www.campuscontent.de/model/1.0}tracking_views";

	public static final String CCM_ASPECT_LTITOOL_NODE = "{http://www.campuscontent.de/model/1.0}ltitool_node";

	public static final String CCM_PROP_LTITOOL_NODE_RESOURCELINK = "{http://www.campuscontent.de/model/1.0}ltitool_resourcelink";

	public static final String CCM_PROP_LTITOOL_NODE_TOOLURL = "{http://www.campuscontent.de/model/1.0}ltitool_url";

	public static final int HTTP_INSUFFICIENT_STORAGE = 503;

    public static final List<String> CHILDOBJECT_IGNORED_PARENT_PROPERTIES = Arrays.asList(
    		CCConstants.CM_NAME,
			CCConstants.CCM_PROP_IO_REPL_EDUCATIONAL_LEARNINGRESSOURCETYPE, // Materialart
			CCConstants.CCM_PROP_IO_COMMONLICENSE_KEY,
			CCConstants.CCM_PROP_IO_LICENSE_DESCRIPTION,
			CCConstants.CCM_PROP_IO_COMMONLICENSE_CC_LOCALE,
			CCConstants.CCM_PROP_IO_COMMONLICENSE_CC_VERSION,
			CCConstants.LOM_PROP_LIFECYCLE_VERSION,
			CCConstants.CCM_PROP_IO_WWWURL,
			CCConstants.ALFRESCO_MIMETYPE,
			CCConstants.LOM_PROP_TECHNICAL_FORMAT,
			CCConstants.LOM_PROP_TECHNICAL_SIZE
	);
    public static final String AUTHORITY_DELETED_USER = "DELETED_USER";
    /**
     * authority needed for appAuth authentication and read only tasks
     */
    public static final String PROXY_USER = "edu_proxy";

    public enum PROPOSAL_STATUS {
		PENDING,
		ACCEPTED,
		DECLINED
	};

	public static String CCM_WF_STATUS_VALUE_UNCHECKED="100_unchecked";
	public static String CCM_WF_STATUS_VALUE_TO_CHECK="200_tocheck";
	public static String CCM_WF_STATUS_VALUE_HASFLAWS="300_hasflaws";
	public static String CCM_WF_STATUS_VALUE_CHECKED="400_checked";


	public static final String CCM_PROP_WF_INSTRUCTIONS = "{http://www.campuscontent.de/model/1.0}wf_instructions";

	public static final String CCM_PROP_WF_PROTOCOL = "{http://www.campuscontent.de/model/1.0}wf_protocol";

	public final static String CCM_ASSOC_RELSOURCE = "{http://www.campuscontent.de/model/1.0}relsource";

	public final static String CCM_ASSOC_RELTARGET = "{http://www.campuscontent.de/model/1.0}reltarget";

	public final static String CCM_ASSOC_RELDIRECTION = "{http://www.campuscontent.de/model/1.0}reldirection";

	public final static String CCM_ASSOC_BASKETCONTENT = "{http://www.campuscontent.de/model/1.0}basketcontent";

	public final static String CCM_ASSOC_ASSIGNEDLICENSES = "{http://www.campuscontent.de/model/1.0}assignedlicenses";

	public final static String CCM_ASSOC_TOOL_INSTANCES = "{http://www.campuscontent.de/model/1.0}tool_instances";

	/* Templates */

	public final static String CCM_ASPECT_METADATA_PRESETTING ="{http://www.campuscontent.de/model/1.0}metadataPresetting";
	public final static String CCM_ASPECT_METADATA_PRESETTING_TEMPLATE ="{http://www.campuscontent.de/model/1.0}metadataPresettingTemplate";


	/**
	 * edu scopes (default, safe)
	 */
	public final static String CCM_ASPECT_EDUSCOPE = "{http://www.campuscontent.de/model/1.0}eduscope";
	public final static String CCM_PROP_EDUSCOPE_NAME = "{http://www.campuscontent.de/model/1.0}eduscopename";

	public final static String CCM_PROP_EDUCONTEXT_NAME = "{http://www.campuscontent.de/model/1.0}educontextname";
	public final static String EDUCONTEXT_DEFAULT = "default";
	public final static List<String> EDUCONTEXT_TYPES=Arrays.asList(CCConstants.CCM_TYPE_IO,CCConstants.CCM_TYPE_MAP,CCConstants.CCM_TYPE_TOOL_INSTANCE);

	//public final static String CCM_VALUE_SCOPE_DEFAULT = "default";
	public final static String CCM_VALUE_SCOPE_SAFE = "safe";


	/* federated groups */
	public final static String CCM_ASPECT_SCOPE = "{http://www.campuscontent.de/model/1.0}scope";
	public final static String CCM_PROP_SCOPE_TYPE = "{http://www.campuscontent.de/model/1.0}scopetype";
	public final static String CCM_ASPECT_IO_CHILDOBJECT = "{http://www.campuscontent.de/model/1.0}io_childobject";

	public final static String CCM_VALUE_SCOPETYPE_GLOBAL = "global";

	public final static String CCM_PROP_METADATA_PRESETTING_STATUS ="{http://www.campuscontent.de/model/1.0}metadataPresettingStatus";

	public final static String CCM_PROP_METADATA_PRESETTING_PROPERTIES ="{http://www.campuscontent.de/model/1.0}metadataPresettingProperties";

	public final static String CCM_ASSOC_METADATA_PRESETTING_TEMPLATE ="{http://www.campuscontent.de/model/1.0}metadataPresettingTemplate";

	public final static String CCM_ASSOC_NOTIFY = "{http://www.campuscontent.de/model/1.0}childnotify";

	public final static String CCM_ASSOC_NOTIFY_NODES = "{http://www.campuscontent.de/model/1.0}notify_nodes";

	public final static String CCM_ASSOC_ASSIGNED_SHARES = "{http://www.campuscontent.de/model/1.0}assignedshares";

	public final static String CCM_ASSOC_COMMENT = "{http://www.campuscontent.de/model/1.0}childcomment";

	public final static String CCM_ASSOC_RATING = "{http://www.campuscontent.de/model/1.0}childrating";

	public final static String CCM_ASSOC_COLLECTION_FEEDBACK = "{http://www.campuscontent.de/model/1.0}childcollectionfeedback";

	public final static String CCM_ASSOC_MATERIAL_FEEDBACK = "{http://www.campuscontent.de/model/1.0}childmaterialfeedback";
	/**
	 * update alfresco3stable auf alfresco34e
	 *
	 * http://wiki.alfresco.com/wiki/Upgrading_to_the_Rendition_Service
	 */

	public final static String CM_ASSOC_THUMBNAILS = "{http://www.alfresco.org/model/rendition/1.0}rendition";

	public final static String LOM_ASSOC_IDENTIFIER = "{http://www.campuscontent.de/model/lom/1.0}general_identifier";

	public final static String LOM_ASSOC_RESOURCE_IDENTIFIER ="{http://www.campuscontent.de/model/lom/1.0}resource_identifier";

	public final static String LOM_ASSOC_META_METADATA_IDENTIFIER = "{http://www.campuscontent.de/model/lom/1.0}schema_metametadata_identifier";

	public final static String LOM_ASSOC_EDUCATIONAL = "{http://www.campuscontent.de/model/lom/1.0}schema_educational";

	public final static String LOM_ASSOC_LIFECYCLE_CONTRIBUTE = "{http://www.campuscontent.de/model/lom/1.0}lifecycle_contribute";

	public final static String LOM_ASSOC_META_METADATA_CONTRIBUTE = "{http://www.campuscontent.de/model/lom/1.0}meta-metadata_contribute";

	public final static String LOM_PROP_META_METADATA_LANGUAGE = "{http://www.campuscontent.de/model/lom/1.0}metadata_language";

	public final static String LOM_ASSOC_RELATION_RESOURCE = "{http://www.campuscontent.de/model/lom/1.0}relation_resource";

	public final static String LOM_ASSOC_SCHEMA_RELATION ="{http://www.campuscontent.de/model/lom/1.0}schema_relation";

	public final static String LOM_ASSOC_CLASSIFICATION_TAXONPATH ="{http://www.campuscontent.de/model/lom/1.0}classification_taxonpath";

	public final static String LOM_ASSOC_TAXONPATH_TAXON ="{http://www.campuscontent.de/model/lom/1.0}taxonpath_taxon";

	public final static String LOM_ASSOC_SCHEMA_CLASSIFICATION = "{http://www.campuscontent.de/model/lom/1.0}schema_classification";

	public final static String CCM_PROP_IO_EDUCATIONKIND = "{http://www.campuscontent.de/model/1.0}educationkind";

	public final static String CCM_PROP_IO_MEDIATYPE = "{http://www.campuscontent.de/model/1.0}mediatype";

	public final static String CCM_PROP_IO_AUDIENCE = "{http://www.campuscontent.de/model/1.0}audience";

	public final static String CCM_PROP_IO_SEMANTICTYPE = "{http://www.campuscontent.de/model/1.0}semantictype";

	public final static String CCM_PROP_IO_AUTHOR = "{http://www.campuscontent.de/model/1.0}author";

	public final static String CCM_PROP_IO_PUBLISHER = "{http://www.campuscontent.de/model/1.0}publisher";

	public final static String CCM_PROP_IO_MEDIACENTER = "{http://www.campuscontent.de/model/1.0}mediacenter";

	public final static String CCM_PROP_IO_LEARNINGTIMEUNIT = "{http://www.campuscontent.de/model/1.0}learningtime_unit";
	public final static String CCM_PROP_IO_LICENSE = "{http://www.campuscontent.de/model/1.0}license";
	public final static String CCM_PROP_IO_DESCRIPTION = "{http://www.campuscontent.de/model/1.0}description";
	public final static String CCM_PROP_IO_ORIGINAL = "{http://www.campuscontent.de/model/1.0}original";
	public final static String CCM_PROP_IO_TOPIC = "{http://www.campuscontent.de/model/1.0}topic";
	public final static String CCM_PROP_IO_LEARNINGGOAL = "{http://www.campuscontent.de/model/1.0}learninggoal";
	public final static String CCM_PROP_IO_GUIDANCETEACHERS = "{http://www.campuscontent.de/model/1.0}guidanceteachers";
	public final static String CCM_PROP_IO_GUIDANCESTUDENTS = "{http://www.campuscontent.de/model/1.0}guidancestudents";
	public final static String CCM_PROP_IO_POINTSFROM = "{http://www.campuscontent.de/model/1.0}pointsfrom";
	public final static String CCM_PROP_IO_POINTSTO = "{http://www.campuscontent.de/model/1.0}pointsto";
	public final static String CCM_PROP_IO_POINTSDEFAULT = "{http://www.campuscontent.de/model/1.0}pointsdefault";
	public final static String CCM_PROP_IO_GROUPSIZE = "{http://www.campuscontent.de/model/1.0}groupsize";

	public final static String CCM_PROP_IO_WWWURL = "{http://www.campuscontent.de/model/1.0}wwwurl";

	public final static String CCM_PROP_IO_OBJECTTYPE = "{http://www.campuscontent.de/model/1.0}objecttype";

	public final static String CCM_PROP_IO_WIDTH = "{http://www.campuscontent.de/model/1.0}width";
	public final static String CCM_PROP_IO_HEIGHT = "{http://www.campuscontent.de/model/1.0}height";

	//LOM Replication:
	public final static String CCM_PROP_IO_REPL_EDUCATIONAL_LEARNINGRESSOURCETYPE  = "{http://www.campuscontent.de/model/1.0}educationallearningresourcetype";
	public final static String CCM_PROP_IO_REPL_EDUCATIONAL_LEARNINGRESSOURCETYPE_AGG  = "{http://www.campuscontent.de/model/1.0}educationallearningresourcetype_agg";
	public final static String CCM_PROP_IO_REPL_EDUCATIONAL_CONTEXT  = "{http://www.campuscontent.de/model/1.0}educationalcontext";
	public final static String CCM_PROP_IO_REPL_EDUCATIONAL_TYPICALLEARNINGTIME  = "{http://www.campuscontent.de/model/1.0}educationaltypicallearningtime";
	public final static String CCM_PROP_IO_REPL_EDUCATIONAL_TYPICALAGERANGEFROM  = "{http://www.campuscontent.de/model/1.0}educationaltypicalagerange_from";
	public final static String CCM_PROP_IO_REPL_EDUCATIONAL_TYPICALAGERANGETO  = "{http://www.campuscontent.de/model/1.0}educationaltypicalagerange_to";
	public final static String CCM_PROP_IO_REPL_EDUCATIONAL_INTERACTIVITYTYPE = "{http://www.campuscontent.de/model/1.0}educationalinteractivitytype";
	public final static String CCM_PROP_IO_REPL_EDUCATIONAL_TYPICALAGERANGE = "{http://www.campuscontent.de/model/1.0}educationaltypicalagerange";
	public final static String CCM_PROP_IO_REPL_EDUCATIONAL_INTENDEDENDUSERROLE = "{http://www.campuscontent.de/model/1.0}educationalintendedenduserrole";
	public final static String CCM_PROP_IO_REPL_EDUCATIONAL_SCHEMA_RELATION = "{http://www.campuscontent.de/model/1.0}schema_relation";
	public final static String CCM_PROP_IO_REPL_EDUCATIONAL_LANGUAGE = "{http://www.campuscontent.de/model/1.0}educationallanguage";

	public final static String CCM_PROP_IO_REPL_TAXON_ENTRY  = "{http://www.campuscontent.de/model/1.0}taxonentry";
	public final static String CCM_PROP_IO_REPL_TAXON_ID  = "{http://www.campuscontent.de/model/1.0}taxonid";
	public final static String CCM_PROP_IO_REPL_TAXON_ID_PATH  = "{http://www.campuscontent.de/model/1.0}taxonid_path";
	public final static String CCM_PROP_IO_REPL_TAXONPATH_XML = "{http://www.campuscontent.de/model/1.0}taxonpath_xml";

	public final static String CCM_PROP_IO_REPL_CLASSIFICATION_KEYWORD  = "{http://www.campuscontent.de/model/1.0}classification_keyword";
	public final static String CCM_PROP_IO_REPL_CLASSIFICATION_KEYWORD_DISPLAY  = "{http://www.campuscontent.de/model/1.0}classification_keyword_display";
	public final static String CCM_PROP_IO_REPL_CLASSIFICATION_PURPOSE  = "{http://www.campuscontent.de/model/1.0}classification_purpose";
	public final static String CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_PUBLISHER = "{http://www.campuscontent.de/model/1.0}lifecyclecontributer_publisher";
	public final static String CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_UNKNOWN = "{http://www.campuscontent.de/model/1.0}lifecyclecontributer_unknown";
	public final static String CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_INITIATOR = "{http://www.campuscontent.de/model/1.0}lifecyclecontributer_initiator";
	public final static String CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_TERMINATOR = "{http://www.campuscontent.de/model/1.0}lifecyclecontributer_terminator";
	public final static String CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_VALIDATOR = "{http://www.campuscontent.de/model/1.0}lifecyclecontributer_validator";
	public final static String CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_EDITOR = "{http://www.campuscontent.de/model/1.0}lifecyclecontributer_editor";
	public final static String CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_AUTHOR = "{http://www.campuscontent.de/model/1.0}lifecyclecontributer_author";
	public final static String CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_GRAPHICAL_DESIGNER = "{http://www.campuscontent.de/model/1.0}lifecyclecontributer_graphical_designer";
	public final static String CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_TECHNICAL_IMPLEMENTER = "{http://www.campuscontent.de/model/1.0}lifecyclecontributer_technical_implementer";
	public final static String CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_CONTENT_PROVIDER = "{http://www.campuscontent.de/model/1.0}lifecyclecontributer_content_provider";
	public final static String CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_TECHNICAL_VALIDATOR = "{http://www.campuscontent.de/model/1.0}lifecyclecontributer_technical_validator";
	public final static String CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_EDUCATIONAL_VALIDATOR = "{http://www.campuscontent.de/model/1.0}lifecyclecontributer_educational_validator";
	public final static String CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_SCRIPT_WRITER = "{http://www.campuscontent.de/model/1.0}lifecyclecontributer_script_writer";
	public final static String CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_INSTRUCTIONAL_DESIGNER = "{http://www.campuscontent.de/model/1.0}lifecyclecontributer_instructional_designer";
	public final static String CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_SUBJECT_MATTER_EXPERT = "{http://www.campuscontent.de/model/1.0}lifecyclecontributer_subject_matter_expert";
	public final static String CCM_PROP_IO_REPL_METADATACONTRIBUTER_CREATOR = "{http://www.campuscontent.de/model/1.0}metadatacontributer_creator";
	public final static String CCM_PROP_IO_REPL_METADATACONTRIBUTER_PROVIDER = "{http://www.campuscontent.de/model/1.0}metadatacontributer_provider";
	public final static String CCM_PROP_IO_REPL_METADATACONTRIBUTER_VALIDATOR = "{http://www.campuscontent.de/model/1.0}metadatacontributer_validator";

	public final static String CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_ANIMATION= "{http://www.campuscontent.de/model/1.0}lifecyclecontributer_animation";
	public final static String CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_ARCHIV= "{http://www.campuscontent.de/model/1.0}lifecyclecontributer_archiv";
	public final static String CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_AUFNAHMELEITUNG= "{http://www.campuscontent.de/model/1.0}lifecyclecontributer_aufnahmeleitung";
	public final static String CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_AUFNAHMETEAM= "{http://www.campuscontent.de/model/1.0}lifecyclecontributer_aufnahmeteam";
	public final static String CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_AUSSTATTUNG= "{http://www.campuscontent.de/model/1.0}lifecyclecontributer_ausstattung";
	public final static String CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_AUTOR= "{http://www.campuscontent.de/model/1.0}lifecyclecontributer_autor";
	public final static String CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_BALLETT= "{http://www.campuscontent.de/model/1.0}lifecyclecontributer_ballett";
	public final static String CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_BEARBEITETE_FASSUNG= "{http://www.campuscontent.de/model/1.0}lifecyclecontributer_bearbeitete_fassung";
	public final static String CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_BILDENDE_KUNST= "{http://www.campuscontent.de/model/1.0}lifecyclecontributer_bildende_kunst";
	public final static String CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_BILDSCHNITT= "{http://www.campuscontent.de/model/1.0}lifecyclecontributer_bildschnitt";
	public final static String CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_BUCH= "{http://www.campuscontent.de/model/1.0}lifecyclecontributer_buch";
	public final static String CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_CHOR= "{http://www.campuscontent.de/model/1.0}lifecyclecontributer_chor";
	public final static String CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_CHOREOGRAPHIE= "{http://www.campuscontent.de/model/1.0}lifecyclecontributer_choreographie";
	public final static String CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_DARSTELLER= "{http://www.campuscontent.de/model/1.0}lifecyclecontributer_darsteller";
	public final static String CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_DESIGN= "{http://www.campuscontent.de/model/1.0}lifecyclecontributer_design";
	public final static String CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_DIRIGENT= "{http://www.campuscontent.de/model/1.0}lifecyclecontributer_dirigent";
	public final static String CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_DVD_GRAFIK_UND_DESIGN= "{http://www.campuscontent.de/model/1.0}lifecyclecontributer_dvd-grafik_und_design";
	public final static String CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_DVD_PREMASTERING= "{http://www.campuscontent.de/model/1.0}lifecyclecontributer_dvd-premastering";
	public final static String CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_ENSEMBLE= "{http://www.campuscontent.de/model/1.0}lifecyclecontributer_ensemble";
	public final static String CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_FACHBERATUNG= "{http://www.campuscontent.de/model/1.0}lifecyclecontributer_fachberatung";
	public final static String CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_FOTO= "{http://www.campuscontent.de/model/1.0}lifecyclecontributer_foto";
	public final static String CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_GRAFIK= "{http://www.campuscontent.de/model/1.0}lifecyclecontributer_grafik";
	public final static String CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_IDEE= "{http://www.campuscontent.de/model/1.0}lifecyclecontributer_idee";
	public final static String CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_INTERPRET= "{http://www.campuscontent.de/model/1.0}lifecyclecontributer_interpret";
	public final static String CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_INTERVIEW= "{http://www.campuscontent.de/model/1.0}lifecyclecontributer_interview";
	public final static String CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_KAMERA= "{http://www.campuscontent.de/model/1.0}lifecyclecontributer_kamera";
	public final static String CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_KOMMENTAR= "{http://www.campuscontent.de/model/1.0}lifecyclecontributer_kommentar";
	public final static String CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_KOMPONIST= "{http://www.campuscontent.de/model/1.0}lifecyclecontributer_komponist";
	public final static String CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_KONZEPTION= "{http://www.campuscontent.de/model/1.0}lifecyclecontributer_konzeption";
	public final static String CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_LIBRETTO= "{http://www.campuscontent.de/model/1.0}lifecyclecontributer_libretto";
	public final static String CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_LITERARISCHE_VORLAGE= "{http://www.campuscontent.de/model/1.0}lifecyclecontributer_literarische_vorlage";
	public final static String CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_MAZ_BEARBEITUNG= "{http://www.campuscontent.de/model/1.0}lifecyclecontributer_maz-bearbeitung";
	public final static String CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_MITWIRKENDE= "{http://www.campuscontent.de/model/1.0}lifecyclecontributer_mitwirkende";
	public final static String CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_MODERATION= "{http://www.campuscontent.de/model/1.0}lifecyclecontributer_moderation";
	public final static String CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_MUSIK= "{http://www.campuscontent.de/model/1.0}lifecyclecontributer_musik";
	public final static String CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_MUSIKALISCHE_LEITUNG= "{http://www.campuscontent.de/model/1.0}lifecyclecontributer_musikalische_leitung";
	public final static String CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_MUSIKALISCHE_VORLAGE= "{http://www.campuscontent.de/model/1.0}lifecyclecontributer_musikalische_vorlage";
	public final static String CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_MUSIKGRUPPE= "{http://www.campuscontent.de/model/1.0}lifecyclecontributer_musikgruppe";
	public final static String CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_ORCHESTER= "{http://www.campuscontent.de/model/1.0}lifecyclecontributer_orchester";
	public final static String CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_PAEDAGOGISCHER_SACHBEARBEITER_EXTERN= "{http://www.campuscontent.de/model/1.0}lifecyclecontributer_paedagogischer_sachbearbeiter_extern";
	public final static String CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_PRODUKTIONSLEITUNG= "{http://www.campuscontent.de/model/1.0}lifecyclecontributer_produktionsleitung";
	public final static String CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_PROJEKTGRUPPE= "{http://www.campuscontent.de/model/1.0}lifecyclecontributer_projektgruppe";
	public final static String CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_PROJEKTLEITUNG= "{http://www.campuscontent.de/model/1.0}lifecyclecontributer_projektleitung";
	public final static String CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_REALISATION= "{http://www.campuscontent.de/model/1.0}lifecyclecontributer_realisation";
	public final static String CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_REDAKTION= "{http://www.campuscontent.de/model/1.0}lifecyclecontributer_redaktion";
	public final static String CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_REGIE= "{http://www.campuscontent.de/model/1.0}lifecyclecontributer_regie";
	public final static String CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_SCHNITT= "{http://www.campuscontent.de/model/1.0}lifecyclecontributer_schnitt";
	public final static String CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_SCREEN_DESIGN= "{http://www.campuscontent.de/model/1.0}lifecyclecontributer_screen-design";
	public final static String CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_SPEZIALEFFEKTE= "{http://www.campuscontent.de/model/1.0}lifecyclecontributer_spezialeffekte";
	public final static String CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_SPRECHER= "{http://www.campuscontent.de/model/1.0}lifecyclecontributer_sprecher";
	public final static String CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_STUDIO= "{http://www.campuscontent.de/model/1.0}lifecyclecontributer_studio";
	public final static String CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_SYNCHRONISATION= "{http://www.campuscontent.de/model/1.0}lifecyclecontributer_synchronisation";
	public final static String CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_SYNCHRONREGIE= "{http://www.campuscontent.de/model/1.0}lifecyclecontributer_synchronregie";
	public final static String CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_SYNCHRONSPRECHER= "{http://www.campuscontent.de/model/1.0}lifecyclecontributer_synchronsprecher";
	public final static String CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_TANZ= "{http://www.campuscontent.de/model/1.0}lifecyclecontributer_tanz";
	public final static String CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_TEXT= "{http://www.campuscontent.de/model/1.0}lifecyclecontributer_text";
	public final static String CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_TON= "{http://www.campuscontent.de/model/1.0}lifecyclecontributer_ton";
	public final static String CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_TRICK= "{http://www.campuscontent.de/model/1.0}lifecyclecontributer_trick";
	public final static String CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_VIDEOTECHNIK= "{http://www.campuscontent.de/model/1.0}lifecyclecontributer_videotechnik";
	public final static String CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_UEBERSETZUNG= "{http://www.campuscontent.de/model/1.0}lifecyclecontributer_uebersetzung";
	public final static String CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_UEBERTRAGUNG= "{http://www.campuscontent.de/model/1.0}lifecyclecontributer_uebertragung";


	public final static String CCM_PROP_IO_REPL_GENERAL_IDENTIFIER = "{http://www.campuscontent.de/model/1.0}general_identifier";

	public final static String CCM_PROP_IO_SUBSOURCE_ID = "{http://www.campuscontent.de/model/1.0}subsource_id";

	public final static String CCM_PROP_IO_CREATE_VERSION = "{http://www.campuscontent.de/model/1.0}create_version";

	public final static String CCM_PROP_IO_VERSION_COMMENT = "{http://www.campuscontent.de/model/1.0}version_comment";

	public final static String CCM_PROP_IO_GENERALKEYWORD_CAPTION = "{http://www.campuscontent.de/model/1.0}generalkeyword_caption";

	public final static String CCM_PROP_IO_COMPETENCE = "{http://www.campuscontent.de/model/1.0}competence";

	public final static String CCM_PROP_IO_COMPETENCE_DIGITAL = "{http://www.campuscontent.de/model/1.0}competence_digital";

	public final static String CCM_PROP_IO_COMPETENCE_DIGITAL2 = "{http://www.campuscontent.de/model/1.0}competence_digital2";

	public final static String CCM_PROP_IO_SEARCH_CONTEXT = "{http://www.campuscontent.de/model/1.0}search_context";
	
	public final static String CCM_PROP_IO_EDITORIAL_STATE = "{http://www.campuscontent.de/model/1.0}editorial_state";

	public final static String CCM_PROP_IO_TECHNICAL_STATE = "{http://www.campuscontent.de/model/1.0}technical_state";

	public final static String CCM_PROP_IO_LOCATION_STATUS = "{http://www.campuscontent.de/model/1.0}location_status";

	public final static String CCM_PROP_IO_LOCATION_STATUS_FAIL_COUNT = "{http://www.campuscontent.de/model/1.0}location_status_fail_count";

	public final static String CCM_PROP_IO_EDITORIAL_CHECKLIST = "{http://www.campuscontent.de/model/1.0}editorial_checklist";

	public final static String CCM_PROP_IO_PUBLISHED_DATE = "{http://www.campuscontent.de/model/1.0}published_date";

	public final static String CCM_PROP_IO_PUBLISHED_MODE = "{http://www.campuscontent.de/model/1.0}published_mode";

	public final static String CCM_PROP_IO_PUBLISHED_ORIGINAL = "{http://www.campuscontent.de/model/1.0}published_original";

	/**
	 * published
	 */
	public final static String CCM_ASPECT_PUBLISHED = "{http://www.campuscontent.de/model/1.0}published";

	public final static String CCM_PROP_PUBLISHED_DATE = "{http://www.campuscontent.de/model/1.0}published_date";

	public final static String CCM_PROP_PUBLISHED_HANDLE_ID = "{http://www.campuscontent.de/model/1.0}published_handle_id";

	public final static String CCM_PROP_PUBLISHED_DOI_ID = "{http://www.campuscontent.de/model/1.0}published_doi_id";

	/**
	 * share props
	 */
	public final static String CCM_PROP_SHARE_EXPIRYDATE = "{http://www.campuscontent.de/model/1.0}share_expirydate";
	public final static String CCM_PROP_SHARE_PASSWORD = "{http://www.campuscontent.de/model/1.0}share_password";
	public final static String CCM_PROP_SHARE_MAIL = "{http://www.campuscontent.de/model/1.0}share_mail";
	public final static String CCM_PROP_SHARE_TOKEN = "{http://www.campuscontent.de/model/1.0}share_token";
	public final static String CCM_PROP_SHARE_DOWNLOAD_COUNTER = "{http://www.campuscontent.de/model/1.0}share_download_counter";

	public final static String CCM_PROP_SYSUPDATE_ID = "{http://www.campuscontent.de/model/1.0}sysupdate_id";
	public final static String CCM_PROP_SYSUPDATE_DATE = "{http://www.campuscontent.de/model/1.0}sysupdate_date";

	public final static String CCM_PROP_AUTHORITYCONTAINER_EDUHOMEDIR = "{http://www.campuscontent.de/model/1.0}edu_homedir";

	public final static String CCM_PROP_NOTIFY_EVENT = "{http://www.campuscontent.de/model/1.0}notify_event";

	public final static String CCM_PROP_NOTIFY_ACTION = "{http://www.campuscontent.de/model/1.0}notify_action";

	public final static String CCM_PROP_NOTIFY_USER = "{http://www.campuscontent.de/model/1.0}notify_user";

	public final static String EXIF_PROP_PIXELXDIMENSION = "{http://www.alfresco.org/model/exif/1.0}pixelXDimension";
	public final static String EXIF_PROP_PIXELYDIMENSION = "{http://www.alfresco.org/model/exif/1.0}pixelYDimension";

	public final static String CCM_VALUE_NOTIFY_ACTION_PERMISSION_ADD = "PERMISSION_ADD";

	public final static String CCM_VALUE_NOTIFY_ACTION_PERMISSION_CHANGE = "PERMISSION_CHANGE";

	public final static String CCM_VALUE_NOTIFY_ACTION_PERMISSION_CHANGE_INHERIT  = "PERMISSION_CHANGE_INHERIT";

	public final static String CCM_VALUE_TOOLPERMISSION_INVITE = "TOOLPERMISSION_INVITE";

	public final static String CCM_VALUE_TOOLPERMISSION_INVITE_STREAM = "TOOLPERMISSION_INVITE_STREAM";

	public final static String CCM_VALUE_TOOLPERMISSION_INVITE_LINK = "TOOLPERMISSION_INVITE_LINK";

	public final static String CCM_VALUE_TOOLPERMISSION_INVITE_SHARE = "TOOLPERMISSION_INVITE_SHARE";

	public final static String CCM_VALUE_TOOLPERMISSION_INVITE_SAFE = "TOOLPERMISSION_INVITE_SAFE";

	public final static String CCM_VALUE_TOOLPERMISSION_INVITE_SHARE_SAFE = "TOOLPERMISSION_INVITE_SHARE_SAFE";

	public final static String CCM_VALUE_TOOLPERMISSION_INVITE_HISTORY = "TOOLPERMISSION_INVITE_HISTORY";

	public final static String CCM_VALUE_TOOLPERMISSION_LICENSE = "TOOLPERMISSION_LICENSE";

	public final static String CCM_VALUE_TOOLPERMISSION_UNCHECKEDCONTENT = "TOOLPERMISSION_UNCHECKEDCONTENT";

	public final static String CCM_VALUE_TOOLPERMISSION_INVITE_ALLAUTHORITIES = "TOOLPERMISSION_INVITE_ALLAUTHORITIES";

	public final static String CCM_VALUE_TOOLPERMISSION_WORKSPACE = "TOOLPERMISSION_WORKSPACE";

	public final static String CCM_VALUE_TOOLPERMISSION_CREATE_ELEMENTS_FOLDERS = "TOOLPERMISSION_CREATE_ELEMENTS_FOLDERS";
	public final static String CCM_VALUE_TOOLPERMISSION_CREATE_ELEMENTS_FILES = "TOOLPERMISSION_CREATE_ELEMENTS_FILES";
	public final static String CCM_VALUE_TOOLPERMISSION_CREATE_ELEMENTS_COLLECTIONS = "TOOLPERMISSION_CREATE_ELEMENTS_COLLECTIONS";

	public final static String CCM_VALUE_TOOLPERMISSION_GLOBAL_AUTHORITY_SEARCH = "TOOLPERMISSION_GLOBAL_AUTHORITY_SEARCH";

	public final static String CCM_VALUE_TOOLPERMISSION_GLOBAL_AUTHORITY_SEARCH_FUZZY = "TOOLPERMISSION_GLOBAL_AUTHORITY_SEARCH_FUZZY";

	public final static String CCM_VALUE_TOOLPERMISSION_GLOBAL_AUTHORITY_SEARCH_SHARE = "TOOLPERMISSION_GLOBAL_AUTHORITY_SEARCH_SHARE";

	public final static String CCM_VALUE_TOOLPERMISSION_GLOBAL_AUTHORITY_SEARCH_SAFE = "TOOLPERMISSION_GLOBAL_AUTHORITY_SEARCH_SAFE";

	public final static String CCM_VALUE_TOOLPERMISSION_GLOBAL_AUTHORITY_SEARCH_SHARE_SAFE = "TOOLPERMISSION_GLOBAL_AUTHORITY_SEARCH_SHARE_SAFE";

	public final static String CCM_VALUE_TOOLPERMISSION_CONNECTOR_PREFIX = "TOOLPERMISSION_CONNECTOR_";

	public final static String CCM_VALUE_TOOLPERMISSION_REPOSITORY_PREFIX = "TOOLPERMISSION_REPOSITORY_";

	public final static String CCM_VALUE_TOOLPERMISSION_COLLECTION_CHANGE_OWNER = "TOOLPERMISSION_COLLECTION_CHANGE_OWNER";

	public final static String CCM_VALUE_TOOLPERMISSION_COLLECTION_PROPOSAL = "TOOLPERMISSION_COLLECTION_PROPOSAL";

	public final static String CCM_VALUE_TOOLPERMISSION_COLLECTION_EDITORIAL = "TOOLPERMISSION_COLLECTION_EDITORIAL";

	public final static String CCM_VALUE_TOOLPERMISSION_COLLECTION_CURRICULUM = "TOOLPERMISSION_COLLECTION_CURRICULUM";

	public final static String CCM_VALUE_TOOLPERMISSION_COLLECTION_PINNING = "TOOLPERMISSION_COLLECTION_PINNING";

	public final static String CCM_VALUE_TOOLPERMISSION_CONFIDENTAL = "TOOLPERMISSION_CONFIDENTAL";

	public final static String CCM_VALUE_TOOLPERMISSION_MEDIACENTER_EDIT = "TOOLPERMISSION_MEDIACENTER_EDIT";

	public final static String CCM_VALUE_TOOLPERMISSION_HANDLESERVICE = "TOOLPERMISSION_HANDLESERVICE";

	public final static String CCM_VALUE_TOOLPERMISSION_COLLECTION_FEEDBACK = "TOOLPERMISSION_COLLECTION_FEEDBACK";

	public final static String CCM_VALUE_TOOLPERMISSION_MATERIAL_FEEDBACK = "TOOLPERMISSION_MATERIAL_FEEDBACK";

	public final static String CCM_VALUE_TOOLPERMISSION_USAGE_STATISTIC = "TOOLPERMISSION_USAGE_STATISTIC";

	public final static String CCM_VALUE_TOOLPERMISSION_COMMENT_WRITE = "TOOLPERMISSION_COMMENT_WRITE";

	public final static String CCM_VALUE_TOOLPERMISSION_GLOBAL_STATISTICS_USER = "TOOLPERMISSION_GLOBAL_STATISTICS_USER";

	public final static String CCM_VALUE_TOOLPERMISSION_GLOBAL_STATISTICS_NODES = "TOOLPERMISSION_GLOBAL_STATISTICS_NODES";

	public final static String CCM_VALUE_TOOLPERMISSION_RATE = "TOOLPERMISSION_RATE";
	public final static String CCM_VALUE_TOOLPERMISSION_RATE_WRITE = "TOOLPERMISSION_RATE_WRITE";
	public final static String CCM_VALUE_TOOLPERMISSION_RATE_READ = "TOOLPERMISSION_RATE_READ";


	public final static String CCM_VALUE_TOOLPERMISSION_SUGGESTION_WRITE = "TOOLPERMISSION_SUGGESTION_WRITE";
	public final static String CCM_VALUE_TOOLPERMISSION_SUGGESTION_READ = "TOOLPERMISSION_SUGGESTION_READ";


	public final static String CCM_VALUE_TOOLPERMISSION_MANAGE_RELATIONS = "TOOLPERMISSION_MANAGE_RELATIONS";

	public final static String CCM_VALUE_TOOLPERMISSION_VIDEO_AUDIO_CUT = "TOOLPERMISSION_VIDEO_AUDIO_CUT";

	public final static String CCM_VALUE_TOOLPERMISSION_MEDIACENTER_MANAGE = "TOOLPERMISSION_MEDIACENTER_MANAGE";

	public final static String CCM_VALUE_TOOLPERMISSION_CONTROL_RESTRICTED_ACCESS = "TOOLPERMISSION_CONTROL_RESTRICTED_ACCESS";

	public final static String CCM_VALUE_TOOLPERMISSION_PUBLISH_COPY = "TOOLPERMISSION_PUBLISH_COPY";

	public final static String CCM_VALUE_TOOLPERMISSION_CREATE_MAP_LINK = "TOOLPERMISSION_CREATE_MAP_LINK";

	public final static String CCM_VALUE_TOOLPERMISSION_SIGNUP_GROUP = "TOOLPERMISSION_SIGNUP_GROUP";

	public final static String CM_VALUE_PERSON_EDU_SCHOOL_PRIMARY_AFFILIATION_TEACHER = "teacher";

	public final static String CM_VALUE_PERSON_EDU_SCHOOL_PRIMARY_AFFILIATION_STUDENT = "student";

	public final static String CM_VALUE_PERSON_EDU_SCHOOL_PRIMARY_AFFILIATION_EMPLOYEE = "employee";

	public final static String CM_VALUE_PERSON_EDU_SCHOOL_PRIMARY_AFFILIATION_EXTERN = "extern";

	public final static String CM_VALUE_PERSON_EDU_SCHOOL_PRIMARY_AFFILIATION_SYSTEM = "system";

	public final static String CM_VALUE_PERSON_EDU_SCHOOL_PRIMARY_AFFILIATION_FUNCTION = "function";

	/**
	 * dynamic generated properties
	 */
	public final static String VIRT_PROP_TYPE = "{virtualproperty}type";

	public final static String VIRT_PROP_MEDIATYPE = "{virtualproperty}mediatype";

	public final static String VIRT_PROP_USAGECOUNT = "{virtualproperty}usagecount";

	public final static String VIRT_PROP_COMMENTCOUNT = "{virtualproperty}commentcount";

	public final static String VIRT_PROP_CHILDOBJECTCOUNT = "{virtualproperty}childobjectcount";

	public final static String VIRT_PROP_MEDIACENTERS = "{virtualproperty}mediacenters";

	/**
	 * says if this set of properties is from an remote repository that was linked in the local repo by an remoteobject
	 * values are all that can be used in new Boolean(value)
	 */
	public final static String VIRT_PROP_ISREMOTE_OBJECT = "{virtualproperty}isremoteobject";

	/**
	 * is the id of the local remote object
	 */
	public final static String VIRT_PROP_REMOTE_OBJECT_NODEID = "{virtualproperty}remoteobject_nodeid";


	/**
	 * timestamp (long) of notfy object create date for sorting
	 */
	public final static String VIRT_PROP_NOTIFY_CREATEDATE = "{virtualproperty}notify_createdate";

	public final static String VIRT_PROP_PRIMARYPARENT_NODEID = "{virtualproperty}primaryparent_nodeid";

	public final static String VIRT_PROP_PERMALINK = "{virtualproperty}permalink";

	/**
	 * license panel: formelement for IOLicenseHelper
	 */
	public final static String VIRT_PROP_CONTROLRELEASE = "{virtualproperty}controlrelease";

	public final static String VIRT_PROP_LICENSE_URL = "{virtualproperty}licenseurl";

	public final static String VIRT_PROP_LICENSE_ICON = "{virtualproperty}licenseicon";

	public final static String VIRT_PROP_ORIGINAL_DELETED = "{virtualproperty}originaldeleted";

	public final static String TOOL_HOMEFOLDER = "EDU_TOOL";

    public static final String TEMPLATE_NODE_NAME = ".METADATA_TEMPLATE";


    private static Map<String,String> lifecycleContributerPropsMap = new HashMap<>();

	public static Map<String, String> getLifecycleContributerPropsMap() {
		if(lifecycleContributerPropsMap.size() == 0){
			lifecycleContributerPropsMap.put("publisher", CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_PUBLISHER);
			lifecycleContributerPropsMap.put("unknown", CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_UNKNOWN);
			lifecycleContributerPropsMap.put("initiator", CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_INITIATOR);
			lifecycleContributerPropsMap.put("terminator", CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_TERMINATOR);
			lifecycleContributerPropsMap.put("validator", CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_VALIDATOR);
			lifecycleContributerPropsMap.put("editor", CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_EDITOR);
			lifecycleContributerPropsMap.put("author", CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_AUTHOR);
			lifecycleContributerPropsMap.put("graphical_designer", CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_GRAPHICAL_DESIGNER);
			lifecycleContributerPropsMap.put("technical_implementer", CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_TECHNICAL_IMPLEMENTER);
			lifecycleContributerPropsMap.put("content_provider", CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_CONTENT_PROVIDER);
			lifecycleContributerPropsMap.put("technical_validator", CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_TECHNICAL_VALIDATOR);
			lifecycleContributerPropsMap.put("educational_validator", CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_EDUCATIONAL_VALIDATOR);
			lifecycleContributerPropsMap.put("script_writer", CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_SCRIPT_WRITER);
			lifecycleContributerPropsMap.put("instructional_designer", CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_INSTRUCTIONAL_DESIGNER);
			lifecycleContributerPropsMap.put("subject_matter_expert", CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_SUBJECT_MATTER_EXPERT);

			lifecycleContributerPropsMap.put("Animation", CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_ANIMATION);
			lifecycleContributerPropsMap.put("Archiv", CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_ARCHIV);
			lifecycleContributerPropsMap.put("Aufnahmeleitung", CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_AUFNAHMELEITUNG);
			lifecycleContributerPropsMap.put("Aufnahmeteam", CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_AUFNAHMETEAM);
			lifecycleContributerPropsMap.put("Ausstattung", CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_AUSSTATTUNG);
			lifecycleContributerPropsMap.put("Autor", CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_AUTOR);
			lifecycleContributerPropsMap.put("Ballett", CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_BALLETT);
			lifecycleContributerPropsMap.put("Bearbeitete Fassung", CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_BEARBEITETE_FASSUNG);
			lifecycleContributerPropsMap.put("Bildende Kunst", CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_BILDENDE_KUNST);
			lifecycleContributerPropsMap.put("Bildschnitt", CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_BILDSCHNITT);
			lifecycleContributerPropsMap.put("Buch", CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_BUCH);
			lifecycleContributerPropsMap.put("Chor", CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_CHOR);
			lifecycleContributerPropsMap.put("Choreographie", CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_CHOREOGRAPHIE);
			lifecycleContributerPropsMap.put("Darsteller", CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_DARSTELLER);
			lifecycleContributerPropsMap.put("Design", CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_DESIGN);
			lifecycleContributerPropsMap.put("Dirigent", CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_DIRIGENT);
			lifecycleContributerPropsMap.put("DVD-Grafik und Design", CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_DVD_GRAFIK_UND_DESIGN);
			lifecycleContributerPropsMap.put("DVD-Premastering", CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_DVD_PREMASTERING);
			lifecycleContributerPropsMap.put("Ensemble", CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_ENSEMBLE);
			lifecycleContributerPropsMap.put("Fachberatung", CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_FACHBERATUNG);
			lifecycleContributerPropsMap.put("Foto", CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_FOTO);
			lifecycleContributerPropsMap.put("Grafik", CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_GRAFIK);
			lifecycleContributerPropsMap.put("Idee", CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_IDEE);
			lifecycleContributerPropsMap.put("Interpret (...)", CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_INTERPRET);
			lifecycleContributerPropsMap.put("Interview", CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_INTERVIEW);
			lifecycleContributerPropsMap.put("Kamera", CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_KAMERA);
			lifecycleContributerPropsMap.put("Kommentar", CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_KOMMENTAR);
			lifecycleContributerPropsMap.put("Komponist", CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_KOMPONIST);
			lifecycleContributerPropsMap.put("Konzeption", CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_KONZEPTION);
			lifecycleContributerPropsMap.put("Libretto", CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_LIBRETTO);
			lifecycleContributerPropsMap.put("Literarische Vorlage", CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_LITERARISCHE_VORLAGE);
			lifecycleContributerPropsMap.put("MAZ-Bearbeitung", CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_MAZ_BEARBEITUNG);
			lifecycleContributerPropsMap.put("Mitwirkende", CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_MITWIRKENDE);
			lifecycleContributerPropsMap.put("Moderation", CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_MODERATION);
			lifecycleContributerPropsMap.put("Musik", CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_MUSIK);
			lifecycleContributerPropsMap.put("Musikalische Leitung", CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_MUSIKALISCHE_LEITUNG);
			lifecycleContributerPropsMap.put("Musikalische Vorlage", CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_MUSIKALISCHE_VORLAGE);
			lifecycleContributerPropsMap.put("Musikgruppe", CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_MUSIKGRUPPE);
			lifecycleContributerPropsMap.put("Orchester", CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_ORCHESTER);
			lifecycleContributerPropsMap.put("Pädagogischer Sachbearbeiter (extern)", CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_PAEDAGOGISCHER_SACHBEARBEITER_EXTERN);
			lifecycleContributerPropsMap.put("Produktionsleitung", CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_PRODUKTIONSLEITUNG);
			lifecycleContributerPropsMap.put("Projektgruppe", CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_PROJEKTGRUPPE);
			lifecycleContributerPropsMap.put("Projektleitung", CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_PROJEKTLEITUNG);
			lifecycleContributerPropsMap.put("Realisation", CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_REALISATION);
			lifecycleContributerPropsMap.put("Redaktion", CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_REDAKTION);
			lifecycleContributerPropsMap.put("Regie", CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_REGIE);
			lifecycleContributerPropsMap.put("Schnitt", CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_SCHNITT);
			lifecycleContributerPropsMap.put("Screen-Design", CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_SCREEN_DESIGN);
			lifecycleContributerPropsMap.put("Spezialeffekte", CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_SPEZIALEFFEKTE);
			lifecycleContributerPropsMap.put("Sprecher", CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_SPRECHER);
			lifecycleContributerPropsMap.put("Studio", CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_STUDIO);
			lifecycleContributerPropsMap.put("Synchronisation", CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_SYNCHRONISATION);
			lifecycleContributerPropsMap.put("Synchronregie", CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_SYNCHRONREGIE);
			lifecycleContributerPropsMap.put("Synchronsprecher", CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_SYNCHRONSPRECHER);
			lifecycleContributerPropsMap.put("Tanz", CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_TANZ);
			lifecycleContributerPropsMap.put("Text", CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_TEXT);
			lifecycleContributerPropsMap.put("Ton", CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_TON);
			lifecycleContributerPropsMap.put("Trick", CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_TRICK);
			lifecycleContributerPropsMap.put("Videotechnik", CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_VIDEOTECHNIK);
			lifecycleContributerPropsMap.put("Übersetzung", CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_UEBERSETZUNG);
			lifecycleContributerPropsMap.put("Übertragung", CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_UEBERTRAGUNG);


			}
		return lifecycleContributerPropsMap;
	}

	public static String getLifecycleContributerProp(String role){

		return  getLifecycleContributerPropsMap().get(role);
	}

	private static Map<String,String> metadataContributerPropsMap = new HashMap<>();

	public static Map<String, String> getMetadataContributerPropsMap() {
		if(metadataContributerPropsMap.size() == 0){
			metadataContributerPropsMap.put("creator", CCM_PROP_IO_REPL_METADATACONTRIBUTER_CREATOR);
			metadataContributerPropsMap.put("provider", CCM_PROP_IO_REPL_METADATACONTRIBUTER_PROVIDER);
			metadataContributerPropsMap.put("validator", CCM_PROP_IO_REPL_METADATACONTRIBUTER_VALIDATOR);
		}
		return metadataContributerPropsMap;
	}

	public static String getMetadataContributerProp(String role){
		return getMetadataContributerPropsMap().get(role);
	}

	//commonlicense
	public final static String CCM_PROP_IO_COMMONLICENSE_KEY = "{http://www.campuscontent.de/model/1.0}commonlicense_key";
	public final static String CCM_PROP_IO_CUSTOM_LICENSE_KEY = "{http://www.campuscontent.de/model/1.0}customlicense_key";
	public final static String CCM_PROP_IO_COMMONLICENSE_QUESTIONSALLOWED = "{http://www.campuscontent.de/model/1.0}questionsallowed";
	public final static String CCM_PROP_IO_COMMONLICENSE_CC_VERSION = "{http://www.campuscontent.de/model/1.0}commonlicense_cc_version";
	public final static String CCM_PROP_IO_COMMONLICENSE_CC_LOCALE = "{http://www.campuscontent.de/model/1.0}commonlicense_cc_locale";
	//time of license
	public final static String CCM_PROP_IO_LICENSE_FROM = "{http://www.campuscontent.de/model/1.0}license_from";
	public final static String CCM_PROP_IO_LICENSE_TO = "{http://www.campuscontent.de/model/1.0}license_to";
	public final static String CCM_PROP_IO_LICENSE_VALID = "{http://www.campuscontent.de/model/1.0}license_valid";
	public final static String CCM_PROP_IO_LICENSE_DESCRIPTION = "{http://www.campuscontent.de/model/1.0}license_description";
	//creative commons source info
	public final static String CCM_PROP_IO_LICENSE_TITLE_OF_WORK = "{http://www.campuscontent.de/model/1.0}license_title_of_work";
	public final static String CCM_PROP_IO_LICENSE_SOURCE_URL = "{http://www.campuscontent.de/model/1.0}license_source_url";
	public final static String CCM_PROP_IO_LICENSE_PROFILE_URL = "{http://www.campuscontent.de/model/1.0}license_profile_url";

	//filename
	public final static String CCM_PROP_IO_FILENAME = "{http://www.campuscontent.de/model/1.0}filename";

	public final static String CCM_PROP_IO_USERDEFINED_PREVIEW = "{http://www.campuscontent.de/model/1.0}userdefined_preview";

	//replicated object
	public final static String CCM_PROP_IO_REPLICATIONSOURCE = "{http://www.campuscontent.de/model/1.0}replicationsource";
	public final static String CCM_PROP_IO_REPLICATIONSOURCEID = "{http://www.campuscontent.de/model/1.0}replicationsourceid";
	public final static String CCM_PROP_IO_REPLICATIONSOURCEUUID = "{http://www.campuscontent.de/model/1.0}replicationsourceuuid";
	public final static String CCM_PROP_IO_REPLICATIONSOURCETIMESTAMP = "{http://www.campuscontent.de/model/1.0}replicationsourcetimestamp";

	public final static String CCM_PROP_IO_REPLICATIONFOLDERUPDATE = "{http://www.campuscontent.de/model/1.0}replicationfolderupdate";
	public final static String CCM_PROP_IO_REPLICATION_MODIFIED = "{http://www.campuscontent.de/model/1.0}replicationmodified";
	public final static String CCM_PROP_IO_IMPORT_MODIFIED = "{http://www.campuscontent.de/model/1.0}importmodified";
	public final static String CCM_PROP_IO_IMPORT_BLOCKED = "{http://www.campuscontent.de/model/1.0}importblocked";

	//replicationsourcetimestampFORMATED is an generated prop, to keep the original
	public final static String CCM_PROP_IO_REPLICATIONSOURCETIMESTAMPFORMATED = "{http://www.campuscontent.de/model/1.0}replicationsourcetimestampFORMATED";

	/*elixier*/
	public final static String CCM_PROP_IO_THUMBNAILURL ="{http://www.campuscontent.de/model/1.0}thumbnailurl";

	public final static String CCM_PROP_IO_TITLE_SERIES ="{http://www.campuscontent.de/model/1.0}title_series";

	public static final String CCM_PROP_IO_SCHOOLCONTEXT = "{http://www.campuscontent.de/model/1.0}schoolcontext";
	public static final String CCM_PROP_IO_SCHOOLTOPIC = "{http://www.campuscontent.de/model/1.0}schooltopic";

	public static final String CCM_PROP_IO_UNIVERSITY = "{http://www.campuscontent.de/model/1.0}university";

	public static final String SCHOOLCONTEXT_PATH_SEPARATOR = "#";

	public final static String CCM_PROP_CONFIGOBJECT_VALUE = "{http://www.campuscontent.de/model/1.0}configvalue";

	public final static String CCM_PROP_REMOTEOBJECT_REPOSITORYID = "{http://www.campuscontent.de/model/1.0}remoterepositoryid";
	public final static String CCM_PROP_REMOTEOBJECT_NODEID = "{http://www.campuscontent.de/model/1.0}remotenodeid";
	public final static String CCM_PROP_REMOTEOBJECT_REPOSITORY_TYPE = "{http://www.campuscontent.de/model/1.0}remoterepositorytype";

	public final static String CCM_PROP_IMPORTED_OBJECT_NODEID = "{http://www.campuscontent.de/model/1.0}imported_object_nodeid";
	public final static String CCM_PROP_IMPORTED_OBJECT_APPID = "{http://www.campuscontent.de/model/1.0}imported_object_appid";
	public final static String CCM_PROP_IMPORTED_OBJECT_APPNAME = "{http://www.campuscontent.de/model/1.0}imported_object_appname";

	public final static String CCM_PROP_EDITOR_TYPE ="{http://www.campuscontent.de/model/1.0}editorType";

	public final static String CCM_PROP_LEARNUNIT_ID ="{http://www.campuscontent.de/model/1.0}learnunit_id";

	//Additional ressource Information (comes with aspect)

	public final static String CCM_PROP_CCRESSOURCETYPE ="{http://www.campuscontent.de/model/1.0}ccressourcetype";
	public final static String CCM_PROP_CCRESSOURCESUBTYPE ="{http://www.campuscontent.de/model/1.0}ccresourcesubtype";

	public final static String CCM_PROP_CCRESSOURCEVERSION ="{http://www.campuscontent.de/model/1.0}ccressourceversion";

	public final static String CCM_PROP_ASSIGNED_LICENSE_AUTHORITY ="{http://www.campuscontent.de/model/1.0}authority";

	public final static String CCM_PROP_ASSIGNED_LICENSE_ASSIGNEDLICENSE ="{http://www.campuscontent.de/model/1.0}assignedlicense";

	public final static String CCM_PROP_ASSIGNED_LICENSE_ASSIGNEDLICENSE_EXPIRY ="{http://www.campuscontent.de/model/1.0}assignedlicenseExpiry";

	//Organisation
	public final static String CCM_PROP_ORGANISATION_NAME = "{http://www.campuscontent.de/model/1.0}org_name";

	public final static String CCM_PROP_ORGANISATION_DESCRIPTION = "{http://www.campuscontent.de/model/1.0}org_description";

	public final static String CCM_PROP_ORGANISATION_URL = "{http://www.campuscontent.de/model/1.0}org_url";

	public final static String CCM_PROP_ORGANISATION_MAIL = "{http://www.campuscontent.de/model/1.0}org_mail";

	public final static String CCM_PROP_ORGANISATION_DEFAULT = "{http://www.campuscontent.de/model/1.0}org_default";

	public final static String CCM_PROP_ORGANISATION_CONTACT_NAME = "{http://www.campuscontent.de/model/1.0}org_contact_name";

	public final static String CCM_PROP_ORGANISATION_CONTACT_FIRSTNAME = "{http://www.campuscontent.de/model/1.0}org_contact_firstname";

	public final static String CCM_PROP_ORGANISATION_CONTACT_MAIL = "{http://www.campuscontent.de/model/1.0}org_contact_mail";

	public final static String CM_PROP_METADATASET_EDU_METADATASET = "{http://www.alfresco.org/model/content/1.0}edu_metadataset";

	public final static String CM_PROP_METADATASET_EDU_FORCEMETADATASET = "{http://www.alfresco.org/model/content/1.0}edu_forcemetadataset";

	/**
	 * Marks an original object as a licensed object
	 * This means that access to content is denied even if there should be access via a collection ref, only the original node permissions are working
	 */
	public final static String CCM_PROP_RESTRICTED_ACCESS ="{http://www.campuscontent.de/model/1.0}restricted_access";
	/**
	 * permissions that are still be granted via a ref, even if restricted access is enabled (default: none)
	 * currently allowed values in this field:
	 * CCConstants.PERMISSION_READ_ALL
	 * CCConstants.PERMISSION_DOWNLOAD_CONTENT
	 */
	public final static String CCM_PROP_RESTRICTED_ACCESS_PERMISSIONS ="{http://www.campuscontent.de/model/1.0}restricted_access_permissions";


	/**
	 * LOM Props
	 */
	//Aspect General
	public final static String LOM_PROP_GENERAL_TITLE = "{http://www.campuscontent.de/model/lom/1.0}title";
	public final static String LOM_PROP_GENERAL_LANGUAGE = "{http://www.campuscontent.de/model/lom/1.0}general_language";
	public final static String LOM_PROP_GENERAL_DESCRIPTION = "{http://www.campuscontent.de/model/lom/1.0}general_description";
	//equals DC-SUBJECT
	public final static String LOM_PROP_GENERAL_KEYWORD = "{http://www.campuscontent.de/model/lom/1.0}general_keyword";

	public final static String LOM_PROP_GENERAL_STRUCTURE = "{http://www.campuscontent.de/model/lom/1.0}structure";
	public final static String LOM_PROP_GENERAL_AGGREGATIONLEVEL = "{http://www.campuscontent.de/model/lom/1.0}aggregationlevel";

	public final static String LOM_PROP_LIFECYCLE_VERSION = "{http://www.campuscontent.de/model/lom/1.0}version";
	public final static String LOM_PROP_LIFECYCLE_STATUS = "{http://www.campuscontent.de/model/lom/1.0}status";

	//Type Identifier
	public final static String LOM_PROP_IDENTIFIER_CATALOG = "{http://www.campuscontent.de/model/lom/1.0}catalog";
	public final static String LOM_PROP_IDENTIFIER_ENTRY = "{http://www.campuscontent.de/model/lom/1.0}identifier_entry";
	//Type Educational
	public final static String LOM_PROP_EDUCATIONAL_INTERACTIVITYTYPE = "{http://www.campuscontent.de/model/lom/1.0}interactivitytype";
	public final static String LOM_PROP_EDUCATIONAL_LEARNINGRESOURCETYPE = "{http://www.campuscontent.de/model/lom/1.0}learningresourcetype";
	public final static String LOM_PROP_EDUCATIONAL_TYPICALLEARNINGTIME = "{http://www.campuscontent.de/model/lom/1.0}typicallearningtime";
	public final static String LOM_PROP_EDUCATIONAL_DESCRIPTION = "{http://www.campuscontent.de/model/lom/1.0}educational_description";
	public final static String LOM_PROP_EDUCATIONAL_LANGUAGE = "{http://www.campuscontent.de/model/lom/1.0}educational_language";
	public final static String LOM_PROP_EDUCATIONAL_CONTEXT = "{http://www.campuscontent.de/model/lom/1.0}context";
	public final static String LOM_PROP_EDUCATIONAL_TYPICALAGERANGE = "{http://www.campuscontent.de/model/lom/1.0}typicalagerange";
	public final static String LOM_PROP_EDUCATIONAL_INTENDED_ENDUSERROLE = "{http://www.campuscontent.de/model/lom/1.0}intendedenduserrole";
	//Type Contribute
	public final static String LOM_PROP_CONTRIBUTE_ENTITY = "{http://www.campuscontent.de/model/lom/1.0}contribute_entity";
	public final static String LOM_PROP_CONTRIBUTE_ROLE = "{http://www.campuscontent.de/model/lom/1.0}role";
	public final static String LOM_PROP_CONTRIBUTE_DATE = "{http://www.campuscontent.de/model/lom/1.0}contribute_date";

	public final static String LOM_PROP_TECHNICAL_FORMAT = "{http://www.campuscontent.de/model/lom/1.0}format";

	public final static String LOM_PROP_TECHNICAL_LOCATION = "{http://www.campuscontent.de/model/lom/1.0}location";

	public final static String LOM_PROP_TECHNICAL_DURATION = "{http://www.campuscontent.de/model/lom/1.0}duration";

	public final static String LOM_PROP_TECHNICAL_SIZE = "{http://www.campuscontent.de/model/lom/1.0}size";

	public final static String LOM_PROP_TECHNICAL_OTHERPLATFORMREQUIREMENTS = "{http://www.campuscontent.de/model/lom/1.0}otherplatformrequirements";

	public final static String LOM_PROP_RELATION_KIND = "{http://www.campuscontent.de/model/lom/1.0}kind";

	public final static String LOM_PROP_RESOURCE_DESCRIPTION = "{http://www.campuscontent.de/model/lom/1.0}resource_description";

	public final static String LOM_PROP_CLASSIFICATION_PURPOSE = "{http://www.campuscontent.de/model/lom/1.0}purpose";
	public final static String LOM_PROP_CLASSIFICATION_DESCRIPTION = "{http://www.campuscontent.de/model/lom/1.0}classification_description";
	public final static String LOM_PROP_CLASSIFICATION_KEYWORD = "{http://www.campuscontent.de/model/lom/1.0}classification_keyword";

	public final static String LOM_PROP_TAXONPATH_SOURCE = "{http://www.campuscontent.de/model/lom/1.0}source";

	public final static String LOM_PROP_TAXON_ID = "{http://www.campuscontent.de/model/lom/1.0}id";
	public final static String LOM_PROP_TAXON_ENTRY = "{http://www.campuscontent.de/model/lom/1.0}taxon_entry";

	public final static String LOM_PROP_RIGHTS_COPY_RIGHT = "{http://www.campuscontent.de/model/lom/1.0}copyright_and_other_restrictions";

	public final static String LOM_PROP_RIGHTS_RIGHTS_DESCRIPTION = "{http://www.campuscontent.de/model/lom/1.0}rights_description";
	public final static String LOM_PROP_RIGHTS_COST = "{http://www.campuscontent.de/model/lom/1.0}cost";

	//public final static String CCM_PROP_MAP_MAPCHILDS = "{http://www.campuscontent.de/model/1.0}mapchilds";

	public final static String CCM_PROP_MAP_X = "{http://www.campuscontent.de/model/1.0}x";

	public final static String CCM_PROP_MAP_Y = "{http://www.campuscontent.de/model/1.0}y";

	public final static String CCM_PROP_MAP_Z = "{http://www.campuscontent.de/model/1.0}z";

	public final static String CCM_PROP_MAP_ICON = "{http://www.campuscontent.de/model/1.0}mapicon";

	public final static String CCM_PROP_MAP_LINKTARGET = "{http://www.campuscontent.de/model/1.0}linktarget";

	/**
	 * Aspect collection props
	 */
	public final static String CCM_PROP_MAP_COLLECTIONCOLOR = "{http://www.campuscontent.de/model/1.0}collectioncolor";

	public final static String CCM_PROP_MAP_COLLECTIONVIEWTYPE = "{http://www.campuscontent.de/model/1.0}collectionviewtype";

	public final static String CCM_PROP_MAP_COLLECTION_AUTHOR_FREETEXT = "{http://www.campuscontent.de/model/1.0}collection_author_freetext";

	public final static String CCM_PROP_MAP_COLLECTIONTYPE = "{http://www.campuscontent.de/model/1.0}collectiontype";
	public final static String CCM_PROP_MAP_COLLECTIONSCOPE = "{http://www.campuscontent.de/model/1.0}collectionscope";
	public final static String CCM_PROP_MAP_COLLECTIONREMOTEID = "{http://www.campuscontent.de/model/1.0}collectionremoteid";
	public final static String CCM_PROP_MAP_COLLECTIONREMOTESOURCE = "{http://www.campuscontent.de/model/1.0}collectionremotesource";

	public final static String CCM_PROP_MAP_COLLECTIONLEVEL0 = "{http://www.campuscontent.de/model/1.0}collectionlevel0";

	public final static String CCM_PROP_MAP_COLLECTION_ORDER_MODE = "{http://www.campuscontent.de/model/1.0}collectionordermode";

	public final static String CCM_PROP_COLLECTION_PINNED_STATUS = "{http://www.campuscontent.de/model/1.0}collection_pinned_status";

	public final static String CCM_PROP_COLLECTION_PINNED_ORDER = "{http://www.campuscontent.de/model/1.0}collection_pinned_order";

	public final static String CCM_PROP_COLLECTION_ORDERED_POSITION = "{http://www.campuscontent.de/model/1.0}collection_ordered_position";

	public final static String CCM_PROP_CHILDOBJECT_ORDER = "{http://www.campuscontent.de/model/1.0}childobject_order";

	public final static String CCM_VALUE_LINK_LINKTYPE_CHAMELEON = "CMchameleon";
	public final static String CCM_VALUE_LINK_LINKTYPE_USER_GENERATED = "USER_GENERATED";

	public final static String CCM_PROP_MAP_TYPE = "{http://www.campuscontent.de/model/1.0}maptype";
	public final static String CCM_VALUE_MAP_TYPE_FAVORITE = "FAVORITE";
	public final static String CCM_VALUE_MAP_TYPE_EDUGROUP = "EDUGROUP";
	public final static String CCM_VALUE_MAP_TYPE_DOCUMENTS = "USERDATAFOLDER";
	public final static String CCM_VALUE_MAP_TYPE_USERINBOX = "USERINBOX";
	public final static String CCM_VALUE_MAP_TYPE_USERSAVEDSEARCH = "USERSAVEDSEARCH";

	public final static String CCM_VALUE_MAP_TYPE_IMAGES = "IMAGES";

	public final static String CCM_VALUE_MAP_TYPE_EDU_SHARING_SYSTEM = "EDUSYSTEM";

	public final static String CCM_VALUE_MAP_TYPE_EDU_SHARING_SYSTEM_UPDATE = "EDUSYSTEM_UPDATE";

	public final static String CCM_VALUE_MAP_TYPE_EDU_SHARING_SYSTEM_NOTIFY = "EDUSYSTEM_NOTIFY";

	public final static String CCM_VALUE_MAP_TYPE_EDU_SHARING_SYSTEM_TEMPLATE = "EDUSYSTEM_TEMPLATE";

	public final static String CCM_VALUE_MAP_TYPE_EDU_SHARING_SYSTEM_SERVICE = "EDUSYSTEM_SERVICE";

	public final static String CCM_VALUE_MAP_TYPE_EDU_SHARING_SYSTEM_MEDIACENTER = "EDUSYSTEM_MEDIACENTER";

	public final static String CCM_VALUE_MAP_TYPE_EDU_SHARING_SYSTEM_REPORTS = "EDUSYSTEM_REPORTS";

	public final static String CCM_VALUE_MAP_TYPE_EDU_SHARING_SYSTEM_TOOLPERMISSIONS = "EDUSYSTEM_TOOLPERMISSIONS";

	public final static String CCM_VALUE_MAP_TYPE_EDU_SHARING_SYSTEM_VALUESPACE = "EDUSYSTEM_VALUESPACE";


	public final static String CCM_ASPECT_GROUP_SIGNUP = "{http://www.campuscontent.de/model/1.0}group_signup";

	public final static String CCM_PROP_GROUP_SIGNUP_METHOD = "{http://www.campuscontent.de/model/1.0}group_signup_method";

	public final static String CCM_PROP_GROUP_SIGNUP_PASSWORD = "{http://www.campuscontent.de/model/1.0}group_signup_password";

	public final static String CCM_PROP_GROUP_SIGNUP_LIST = "{http://www.campuscontent.de/model/1.0}group_signup_list";


	public final static String CCM_VALUE_IO_NAME_CONFIG_NODE_NAME = "RepositoryConfig";

	public final static String CCM_VALUE_IO_NAME_LTI_REGISTRATION_NODE_NAME = "LTIRegistration";


	/**
	 * this property comes from alfresco when calling queryChildren
	 */
	public final static String CCM_PROP_PRIMARY_PARENT = "isPrimary";

	public static final String CCM_TYPE_USAGE ="{http://www.campuscontent.de/model/1.0}usage";
	public static final String CCM_PROP_USAGE_APPID = "{http://www.campuscontent.de/model/1.0}usageappid";
	public static final String CCM_PROP_USAGE_COURSEID = "{http://www.campuscontent.de/model/1.0}usagecourseid";
	public static final String CCM_PROP_USAGE_PARENTNODEID = "{http://www.campuscontent.de/model/1.0}usageparentnodeid";
	public static final String CCM_PROP_USAGE_APPUSER = "{http://www.campuscontent.de/model/1.0}usageappuser";
	public static final String CCM_PROP_USAGE_APPUSERMAIL = "{http://www.campuscontent.de/model/1.0}usageappusermail";
	public static final String CCM_PROP_USAGE_FROM = "{http://www.campuscontent.de/model/1.0}usagefrom";
	public static final String CCM_PROP_USAGE_TO = "{http://www.campuscontent.de/model/1.0}usageto";
	public static final String CCM_PROP_USAGE_MAXPERSONS = "{http://www.campuscontent.de/model/1.0}usagemaxpersons";
	public static final String CCM_PROP_USAGE_COUNTER = "{http://www.campuscontent.de/model/1.0}usagecounter";
	public static final String CCM_PROP_USAGE_VERSION = "{http://www.campuscontent.de/model/1.0}usageversion";
	public static final String CCM_PROP_USAGE_XMLPARAMS = "{http://www.campuscontent.de/model/1.0}usagexmlparams";
	public static final String CCM_PROP_USAGE_RESSOURCEID = "{http://www.campuscontent.de/model/1.0}usageressourceid";
	public static final String CCM_PROP_USAGE_GUID = "{http://www.campuscontent.de/model/1.0}usageguid";

	public static final String CCM_ASSOC_USAGEASPECT_USAGES = "{http://www.campuscontent.de/model/1.0}usages";

	public static final String CCM_ASSOC_CHILDIO = "{http://www.campuscontent.de/model/1.0}childio";

	public static final String CCM_ASSOC_FORKIO = "{http://www.campuscontent.de/model/1.0}forkio";


	public static final String CM_PROP_PERSON_ALLOW_NOTIFICATIONS = "{http://www.alfresco.org/model/content/1.0}personallownotifications";
	public static final String CM_PROP_PERSON_ABOUT = "{http://www.alfresco.org/model/content/1.0}personabout";
	public static final String CM_PROP_PERSON_SKILLS = "{http://www.alfresco.org/model/content/1.0}personskills";
	public static final String CM_PROP_PERSON_VCARD = "{http://www.alfresco.org/model/content/1.0}personvcard";

	/**
	 * tempory, non persistent properties
	 */
	public static final String TMP_PROP_TAXONPATH_SOURCE = "{http://www.edu-sharing.net/temp/1.0}taxonpath_source";

	public final static String CC_SESSION_EXPIRED_ID = "org.apache.ws.security.WSSecurityException";

	/**
	 * UI
	 */
	//LOM Learning reassource Type:

	public final static String UI_LB_EK_SELFSTUDY_KEY = "selfstudy";

	public final static String UI_LB_EK_EXERCISE_KEY = "exercise";

	public final static String UI_LB_EK_SIMULATION_KEY = "simulation";

	public final static String UI_LB_EK_QUESTIONNAIRE_KEY = "questionaire";

	public final static String UI_LB_EK_DIAGRAM_KEY = "diagram";

	public final static String UI_LB_EK_FIGURE_KEY = "figure";

	public final static String UI_LB_EK_GRAPH_KEY = "graph";

	public final static String UI_LB_EK_INDEX_KEY = "index";

	public final static String UI_LB_EK_SLIDE_KEY = "table";

	public final static String UI_LB_EK_NARRATIVETEXT_KEY = "narrativetext";

	public final static String UI_LB_EK_EXAM_KEY = "exam";

	public final static String UI_LB_EK_EXPERIMENT_KEY = "experiment";

	public final static String UI_LB_EK_PROBLEMSTATEMENT_KEY = "problemstatement";

	public final static String UI_LB_EK_SELFASSESSMENT_KEY = "selfassessment";

	public final static String UI_LB_EK_LECTURE_KEY = "lecture";


	//LOM typical learning Time
	public final static String UI_LB_LT_MINUTES_KEY = "minutes";

	public final static String UI_LB_LT_HOURS_KEY = "hours";

	public final static String UI_LB_LT_DAYS_KEY = "days";


	//LOM Contribute Role as LifecycleChild
	public final static String UI_LB_ROLE_author = "author";
	public final static String UI_LB_ROLE_publisher ="publisher";
	public final static String UI_LB_ROLE_unknown ="unknown";
	public final static String UI_LB_ROLE_initiator ="initiator";
	public final static String UI_LB_ROLE_terminator ="terminator";
	public final static String UI_LB_ROLE_validator ="validator";
	public final static String UI_LB_ROLE_editor ="editor";
	public final static String UI_LB_ROLE_graphical_designer ="graphical designer";
	public final static String UI_LB_ROLE_technical_implementer = "technical implementer";
	public final static String UI_LB_ROLE_content_provider ="content provider";
	public final static String UI_LB_ROLE_technical_validator ="technical validator";
	public final static String UI_LB_ROLE_educational_validator ="educational validator";
	public final static String UI_LB_ROLE_script_writer ="script writer";
	public final static String UI_LB_ROLE_instructional_designer ="instructional designer";
	public final static String UI_LB_ROLE_subject_matter_expert ="subject matter expert";

	//LOM Contribute Role as MetadataChild
	public final static String UI_LB_ROLE_creator = "creator";


	//LOM Relation Kind
	public final static String UI_LB_RELKIND_ispartof ="ispartof";
	public final static String UI_LB_RELKIND_haspart ="haspart";
	public final static String UI_LB_RELKIND_isversionof ="isversionof";
	public final static String UI_LB_RELKIND_hasversion ="hasversion";
	public final static String UI_LB_RELKIND_isformatof ="isformatof";
	public final static String UI_LB_RELKIND_hasformat ="hasformat";
	public final static String UI_LB_RELKIND_references ="references";
	public final static String UI_LB_RELKIND_isreferencedby ="isreferencedby";
	public final static String UI_LB_RELKIND_isbasedon ="isbasedon";
	public final static String UI_LB_RELKIND_isbasisfor ="isbasisfor";
	public final static String UI_LB_RELKIND_requires ="requires";
	public final static String UI_LB_RELKIND_isrequiredby ="isrequiredby";

	public final static String UI_LB_EDUCATIONAL_IATYPE_active = "active";
	public final static String UI_LB_EDUCATIONAL_IATYPE_expositive = "expositive";
	public final static String UI_LB_EDUCATIONAL_IATYPE_mixed = "mixed";

	public final static String UI_LB_EDUCATIONAL_CONTEXT_school = "school";
	public final static String UI_LB_EDUCATIONAL_CONTEXT_highereducation = "higher education";
	public final static String UI_LB_EDUCATIONAL_CONTEXT_training = "training";
	public final static String UI_LB_EDUCATIONAL_CONTEXT_other = "other";


	//LOM Classification
	public final static String UI_LB_CLASSIFICATION_PURPOSE_discipline ="discipline";

	/**
	 * Form Types
	 */
	public final static String CC_FORM_INT = "int";

	public final static String CC_FORM_ASPECT = "CC_FORM_ASPECT";

	public final static String CC_FORM_STRING = "string";

	public final static String CC_FORM_BOOLEAN = "boolean";

	public final static String CC_FORM_DATE = "date";

	public final static String CC_FORM_ASSOC = "assoc";

	public final static String CC_FORM_CHILDASSOC = "childassoc";

	public final static String CC_FORM_UPLOAD = "upload";

	public final static String CC_FORM_MULTIVALUE = "multivalue";

	public final static String CC_FORM_OBJECT = "object";

	public final static String CC_FORM_DEFAULT = "ccFormdefault";

	public final static String CC_FORM_AUTOMATIC = "ccFormAutomatic";

	public final static String CC_FORM_VCARD = "CCFORMVCARD";


	/**
	 * used for example when helper is doing ths work
	 */
	public final static String CC_FORM_DONOTHING = "CC_FORM_DONOTHING";


	//other form
	//take the parentID as default value for an property
	public final static String CC_FORM_DEFAULTVALUE_PARENTID = "ccFormDefaultValueParentID";

	//boolean config
	public final static String CC_FORM_TRUE = "CC_FORM_TRUE";

	public final static String CC_ASSOC_FROM = "from";

	public final static String CC_ASSOC_TO = "to";

	private static ArrayList<String> detailsProps = null;

	//lucene don't like underscores
	//these are the old names, only used by updater SytemfolderNameToDisplayName
	public final static String CC_DEFAULT_USER_DATA_FOLDER_NAME = "DEFAULTUSERDATAFOLDER";

	public final static String CC_DEFAULT_REMOTEOBJECT_FOLDER_NAME = "DEFAULTREMOTEOBJECTFOLDERNAME";

	public final static String CC_DEFAULT_ORGANISATION_FOLDER_NAME = "DEFAULTORGANISATIONFOLDERNAME";


	//userfolder i18n

	public final static String I18n_USERFOLDER_DOCUMENTS = "userdatafolder_files";
	public final static String I18n_USERFOLDER_IMAGES = "userdatafolder_images";
	public final static String I18n_USERFOLDER_FAVORITES = "userdatafolder_favorites";
	public final static String I18n_USERFOLDER_GROUPS = "userdatafolder_groups";

	public final static String I18n_SYSTEMFOLDER_BASE = "systemfolder_base";

	public final static String I18n_SYSTEMFOLDER_UPDATE = "systemfolder_update";

	public final static String I18n_SYSTEMFOLDER_CONFIG = "systemfolder_config";

	public final static String I18n_SYSTEMFOLDER_MEDIACENTER = "systemfolder_mediacenter";

	public final static String I18n_SYSTEMFOLDER_REPORTS = "systemfolder_reports";

	public final static String I18n_SYSTEMFOLDER_NOTIFY = "systemfolder_notify";

	public final static String I18n_SYSTEMFOLDER_TOOLPERMISSIONS = "systemfolder_toolpermissions";

	public final static String I18n_SYSTEMFOLDER_TEMPLATE = "systemfolder_template";

	public final static String I18n_SYSTEMFOLDER_SERVICE = "systemfolder_service";

	public final static String I18n_SYSTEMFOLDER_VALUESPACE = "systemfolder_valuespace";


	//Gruppen root Folder
	//public final static String CC_DEFAULT_GROUPS_FOLDERNAME = "DEFAULTGROUPSFOLDER";

	//Authorities Alfresco
	public final static String AUTHORITY_ROLE_OWNER = "ROLE_OWNER";

	public final static String AUTHORITY_GROUP_ALFRESCO_ADMINISTRATORS = "GROUP_ALFRESCO_ADMINISTRATORS";

	public final static String AUTHORITY_GROUP_EMAIL_CONTRIBUTORS = "GROUP_EMAIL_CONTRIBUTORS";

	public final static String AUTHORITY_GROUP_EVERYONE = "GROUP_EVERYONE";

	public final static String AUTHORITY_ROLE_ADMINISTRATOR = "ROLE_ADMINISTRATOR";

	//Authorities edu sharing
	public final static String AUTHORITY_GROUP_Edu_Sharing_ALL = "GROUP_Edu-Sharing_ALL";

	public static ArrayList<String> getDetailPropList(){
		if(detailsProps == null){
			detailsProps = new ArrayList<>();
			detailsProps.add(CM_PROP_C_TITLE);
			detailsProps.add(CM_PROP_C_CREATOR);
			detailsProps.add(CM_PROP_C_CREATED);
			detailsProps.add(CM_PROP_C_MODIFIED);
			detailsProps.add(CCConstants.CCM_PROP_IO_COMMONLICENSE_KEY);
			detailsProps.add(CCConstants.CCM_PROP_IO_COMMONLICENSE_QUESTIONSALLOWED);
			detailsProps.add(CM_PROP_VERSIONABLELABEL);
			detailsProps.add(CCM_PROP_IO_MEDIATYPE);

			detailsProps.add(CCM_PROP_IO_ORIGINAL);
			detailsProps.add(CCM_PROP_IO_TOPIC);
			detailsProps.add(LOM_PROP_TECHNICAL_FORMAT);
			detailsProps.add(CCM_PROP_IO_GROUPSIZE);

			detailsProps.add(LOM_PROP_CLASSIFICATION_DESCRIPTION);
			detailsProps.add(LOM_PROP_CLASSIFICATION_KEYWORD);
			detailsProps.add(LOM_TYPE_CONTRIBUTE);
			//detailsProps.add(LOM_PROP_CONTRIBUTE_ROLE);
			detailsProps.add(LOM_PROP_EDUCATIONAL_CONTEXT);
			detailsProps.add(LOM_PROP_EDUCATIONAL_DESCRIPTION);
			detailsProps.add(LOM_PROP_EDUCATIONAL_INTERACTIVITYTYPE);
			detailsProps.add(LOM_PROP_EDUCATIONAL_LANGUAGE);
			detailsProps.add(LOM_PROP_EDUCATIONAL_LEARNINGRESOURCETYPE);
			detailsProps.add(LOM_PROP_EDUCATIONAL_TYPICALAGERANGE);
			detailsProps.add(LOM_PROP_EDUCATIONAL_TYPICALLEARNINGTIME);
			detailsProps.add(LOM_PROP_GENERAL_DESCRIPTION);
			detailsProps.add(LOM_PROP_GENERAL_KEYWORD);
			detailsProps.add(LOM_PROP_GENERAL_LANGUAGE);
			detailsProps.add(LOM_PROP_GENERAL_TITLE);
			//detailsProps.add(LOM_PROP_IDENTIFIER_CATALOG);
			//detailsProps.add(LOM_PROP_IDENTIFIER_ENTRY);
			detailsProps.add(LOM_PROP_RELATION_KIND);
			detailsProps.add(LOM_PROP_RESOURCE_DESCRIPTION);
			detailsProps.add(LOM_PROP_TAXON_ENTRY);
			//detailsProps.add(LOM_PROP_TAXON_ID);
			//detailsProps.add(LOM_PROP_TAXONPATH_SOURCE);
			//detailsProps.add(LOM_PROP_TECHNICAL_FORMAT);
			detailsProps.add(LOM_PROP_EDUCATIONAL_LEARNINGRESOURCETYPE);
			detailsProps.add(REPOSITORY_CAPTION);
			detailsProps.add(REPOSITORY_ID);

			detailsProps.add(CCConstants.LOM_PROP_TECHNICAL_LOCATION);
			detailsProps.add(CM_NAME);

		}
		return detailsProps;
	}


	public final static String PROP_FILE_SEARCHCLASS = "searchclass";

	/**
	 * Permission:
	 * see Alfresco config file: permissionDefinitions.xml
	 */

	/**
	 * Read Permission
	 */
	public final static String PERMISSION_READ = "Read";

	/**
	 * Deny Permission (if set, will revoke all other permissions)
	 */
	public final static String PERMISSION_DENY = "Deny";
	/**
	 * Write Permission
	 */
	public final static String PERMISSION_WRITE = "Write";

	/**
	 * Delete Permission
	 */
	public final static String PERMISSION_DELETE = "Delete";

	public final static String PERMISSION_DELETE_NODE ="DeleteNode";

	public final static String PERMISSION_DELETE_CHILDREN = "DeleteChildren";

	public final static String PERMISSION_ADD_CHILDREN = "AddChildren";

	/**
	 * Read Permission
	 */
	public final static String PERMISSION_CONSUMER = "Consumer";
	public final static String PERMISSION_CONSUMER_METADATA = "ConsumerMetadata";

	/**
	 * Read Permission and Write Permission to the Object
	 */
	public final static String PERMISSION_EDITOR = "Editor";

	/**
	 * Read Permission and Add Content Permission (maybe just for an Folder Object
	 */
	public final static String PERMISSION_CONTRIBUTER = "Contributor";

	/**
	 * A coordinator can do anything to the object or its childeren unless the
     * permissions are set not to inherit or permission is denied.
	 */
	public final static String PERMISSION_COORDINATOR = "Coordinator";

	/**
	 * A coordinator can do anything to the object or its childeren unless the
     * permissions are set not to inherit or permission is denied.
	 */
	public final static String PERMISSION_COLLABORATOR = "Collaborator";

	/**
	 * Campus Content Publish Permission
	 */
	public final static String PERMISSION_CC_PUBLISH = "CCPublish";
	public final static String PERMISSION_READ_ALL = "ReadAll";

	public final static String PERMISSION_DOWNLOAD_CONTENT = "DownloadContent";
	public final static String PERMISSION_READ_PREVIEW = "ReadPreview";

	public final static String PERMISSION_COMMENT = "Comment";

	public final static String PERMISSION_RATE = "Rate";
	public final static String PERMISSION_RATE_READ = "RateRead";
	public final static String PERMISSION_RELATION = "Relation";

	// collection feedback permission
	public final static String PERMISSION_FEEDBACK = "Feedback";

	public final static String PERMISSION_ES_CHILD_MANAGER = "ESChildManager";

	/**
	 * this is used for the strange alfresco behavior:
	 *
	 * http://forums.alfresco.com/en/viewtopic.php?f=4&t=20335&p=66165&hilit=permission+problem#p66165
	 *
	 * read permission to make a node a child of a folder but need write permission to remove it
	 */
	public final static String PERMISSION_CC_REMOVEFROMBASKET = "CCRemoveFromBasket";

	public final static String PERMISSION_CHANGEPERMISSIONS = "ChangePermissions";

	public final static String PERMISSION_READPERMISSIONS = "ReadPermissions";

	public final static String PERMISSION_ALL =	"All";

	public final static String LICENSE_NO_LICENSE = "no-license";

	public final static String LICENSE_SCHULFUNK = "SCHULFUNK";

	public final static String LICENSE_PDM = "PDM";

	public final static String LICENSE_COPYRIGHT_FREE = "COPYRIGHT_FREE";

	public final static String LICENSE_COPYRIGHT_LICENSE = "COPYRIGHT_LICENSE";

	// a missing license is just stored as an empy value
	public final static String LICENSE_NONE = "";


	/**
	 * Creative Commons Licenses (all lead to publish right)
	 */

	public final static String COMMON_LICENSE_CC_ZERO = "CC_0";


	/**
	 * Public Domain Mark
	 */
	public final static String COMMON_LICENSE_PDM = "PDM";


	/******************************************************************
	 * Common Licenses
	 ******************************************************************/

	//Creative Commons:
	/**
	 * - the authors have to be credited in a manner specified by these.
	 */
	public final static String COMMON_LICENSE_CC_BY = "CC_BY";

	/**
	 * - The authors have to be credited in a manner specified by these.
	 * - Licensees may copy, distribute, display, and perform the work and make
	 * derivative works and remixes based on it, as long as they distribute
	 * derivative works only under a license identical ("not more restrictive")
	 * to the license that governs the original work.
	 */
	public final static String COMMON_LICENSE_CC_BY_SA = "CC_BY_SA";

	/**
	 * - The authors have to be credited in a manner specified by these.
	 * - Licensees may copy, distribute, display and perform only verbatim
	 * copies of the work, not derivative works and remixes based on it.
	 */
	public final static String COMMON_LICENSE_CC_BY_ND = "CC_BY_ND";

	/**
	 * - The authors have to be credited in a manner specified by these.
	 * - Licensees may copy, distribute, display, and perform the work and make
	 * derivative works and remixes based on it only for non-commercial purposes.
	 */
	public final static String COMMON_LICENSE_CC_BY_NC = "CC_BY_NC";

	/**
	 * - The authors have to be credited in a manner specified by these.
	 * - Licensees may copy, distribute, display, and perform the work and make
	 * derivative works and remixes based on it, as long as they distribute derivative
	 * works only under a license identical ("not more restrictive") to the license that
	 * governs the original work.
	 * - Licensees may copy, distribute, display, and perform the work and make derivative
	 * works and remixes based on it only for non-commercial purposes.
	 */
	public final static String COMMON_LICENSE_CC_BY_NC_SA = "CC_BY_NC_SA";

	/**
	 * - The authors have to be credited in a manner specified by these.
	 * - Licensees may copy, distribute, display and perform only verbatim
	 * copies of the work, not derivative works and remixes based on it.
	 * - Licensees may copy, distribute, display, and perform the work and make
	 * derivative works and remixes based on it only for non-commercial purposes.
	 */
	public final static String COMMON_LICENSE_CC_BY_NC_ND = "CC_BY_NC_ND";

	/**
	 * edu licenses:
	 * ND: no download
	 * NR: no reuse and distribution
	 * P: personal use
	 * NC: no commercial reuse and distribution
	 */


	/**
	 * - personal use
	 * - no reuse and distribution
	 * - download allowed
	 */
	public final static String COMMON_LICENSE_EDU_P_NR= "EDU_P_NR";

	/**
	 * - personal use
	 * - no reuse and distribution
	 * - no download
	 */
	public final static String COMMON_LICENSE_EDU_P_NR_ND= "EDU_P_NR_ND";

	/**
	 * - no commercial reuse and distribution
	 * - no download
	 */
	public final static String COMMON_LICENSE_EDU_NC_ND= "EDU_NC_ND";

	/**
	 * - no commercial reuse and distribution
	 * - download allowed
	 */
	public final static String COMMON_LICENSE_EDU_NC= "EDU_NC";


	//custom licenses
	/**
	 * the user can set an own license in a custom field
	 */
	public final static String COMMON_LICENSE_CUSTOM = "CUSTOM";
	
	public static List<String> getAllLicenseKeys(){
		List<String> list=new ArrayList<>();
		list.add(COMMON_LICENSE_CC_BY);
		list.add(COMMON_LICENSE_CC_BY_SA);
		list.add(COMMON_LICENSE_CC_BY_ND);
		list.add(COMMON_LICENSE_CC_BY_NC);
		list.add(COMMON_LICENSE_CC_BY_NC_SA);
		list.add(COMMON_LICENSE_CC_BY_NC_ND);
		list.add(COMMON_LICENSE_CC_ZERO);
		list.add(COMMON_LICENSE_PDM);
		list.add(COMMON_LICENSE_EDU_P_NR);
		list.add(COMMON_LICENSE_EDU_P_NR_ND);
		list.add(COMMON_LICENSE_EDU_NC_ND);
		list.add(COMMON_LICENSE_EDU_NC);
		list.add(COMMON_LICENSE_CUSTOM);
		return list;
	}
	public static List<String> getAllCCLicenseKeys(){
		List<String> list=new ArrayList<>();
		list.add(COMMON_LICENSE_CC_BY);
		list.add(COMMON_LICENSE_CC_BY_SA);
		list.add(COMMON_LICENSE_CC_BY_ND);
		list.add(COMMON_LICENSE_CC_BY_NC);
		list.add(COMMON_LICENSE_CC_BY_NC_SA);
		list.add(COMMON_LICENSE_CC_BY_NC_ND);
		list.add(COMMON_LICENSE_CC_ZERO);
		list.add(COMMON_LICENSE_PDM);
		return list;
	}

	public final static String COMMON_LICENSE_CC_ZERO_LINK = "https://creativecommons.org/publicdomain/zero/1.0/deed.${locale}";
	public final static String COMMON_LICENSE_CC_PDM_LINK = "http://creativecommons.org/publicdomain/mark/1.0/deed.${locale}";
	public final static String COMMON_LICENSE_CC_BY_LINK = "https://creativecommons.org/licenses/by/${version}/deed.${locale}";
	public final static String COMMON_LICENSE_CC_BY_SA_LINK = "https://creativecommons.org/licenses/by-sa/${version}/deed.${locale}";
	public final static String COMMON_LICENSE_CC_BY_ND_LINK = "https://creativecommons.org/licenses/by-nd/${version}/deed.${locale}";
	public final static String COMMON_LICENSE_CC_BY_NC_LINK = "https://creativecommons.org/licenses/by-nc/${version}/deed.${locale}";
	public final static String COMMON_LICENSE_CC_BY_NC_SA_LINK = "https://creativecommons.org/licenses/by-nc-sa/${version}/deed.${locale}";
	public final static String COMMON_LICENSE_CC_BY_NC_ND_LINK = "https://creativecommons.org/licenses/by-nc-nd/${version}/deed.${locale}";
	public final static String COMMON_LICENSE_EDU_LINK = "http://edu-sharing.net/licenses/edu-nc-nd/1.0/de";
	public final static String COMMON_LICENSE_CUSTOM_LINK = "http://edu-sharing.net/licenses/custom-licence/1.0/de";


	private static Map<String,String> licenseMap = null;

	public static Map<String,String> getLicenseMap(){
		if(licenseMap == null) {
			licenseMap = new HashMap<>();
			licenseMap.put(CCConstants.COMMON_LICENSE_CC_BY_SA_LINK.split("\\$")[0],CCConstants.COMMON_LICENSE_CC_BY_SA);
			licenseMap.put(CCConstants.COMMON_LICENSE_CC_BY_LINK.split("\\$")[0],CCConstants.COMMON_LICENSE_CC_BY);
			licenseMap.put(CCConstants.COMMON_LICENSE_CC_BY_ND_LINK.split("\\$")[0],CCConstants.COMMON_LICENSE_CC_BY_ND);
			licenseMap.put(CCConstants.COMMON_LICENSE_CC_BY_NC_LINK.split("\\$")[0],CCConstants.COMMON_LICENSE_CC_BY_NC);
			licenseMap.put(CCConstants.COMMON_LICENSE_CC_BY_NC_ND_LINK.split("\\$")[0],CCConstants.COMMON_LICENSE_CC_BY_NC_ND);
			licenseMap.put(CCConstants.COMMON_LICENSE_CC_BY_NC_SA_LINK.split("\\$")[0],CCConstants.COMMON_LICENSE_CC_BY_NC_SA);
			licenseMap.put(CCConstants.COMMON_LICENSE_CC_ZERO_LINK.split("\\$")[0],CCConstants.COMMON_LICENSE_CC_ZERO);
			licenseMap.put(CCConstants.COMMON_LICENSE_CC_PDM_LINK.split("\\$")[0],CCConstants.COMMON_LICENSE_PDM);
		}
		return licenseMap;
	}



	private static Set<String> permission = null;

	public static Set<String> getPermissionList(){
		if(permission == null){
			permission = new HashSet<>();
			permission.add(PERMISSION_ALL);
			permission.add(PERMISSION_READ);
			permission.add(PERMISSION_READ_PREVIEW);
			permission.add(PERMISSION_READ_ALL);
			permission.add(PERMISSION_DOWNLOAD_CONTENT);
			permission.add(PERMISSION_COMMENT);
			permission.add(PERMISSION_FEEDBACK);
			permission.add(PERMISSION_RATE);
			permission.add(PERMISSION_RATE_READ);
			permission.add(PERMISSION_RELATION);
			permission.add(PERMISSION_WRITE);
			permission.add(PERMISSION_DELETE);
			permission.add(PERMISSION_DELETE_CHILDREN);
			permission.add(PERMISSION_DELETE_NODE);
			permission.add(PERMISSION_ADD_CHILDREN);
			permission.add(PERMISSION_CONSUMER);
			permission.add(PERMISSION_CONSUMER_METADATA);
			permission.add(PERMISSION_EDITOR);
			permission.add(PERMISSION_CONTRIBUTER);
			permission.add(PERMISSION_COLLABORATOR);
			permission.add(PERMISSION_COORDINATOR);
			permission.add(PERMISSION_CC_PUBLISH);
			permission.add(PERMISSION_READPERMISSIONS);
			permission.add(PERMISSION_CHANGEPERMISSIONS);
			permission.add(PERMISSION_DENY);
		}
		return permission;
	}

	private static HashSet<String> usagePermissions = null;
	/**
	 * Permissions allowed if the node was opened via usage (lms) or signature
	 * @return
	 */
	public static synchronized Set<String> getUsagePermissions(){
		if(usagePermissions == null){
			usagePermissions = new HashSet<>();
				usagePermissions.add(PERMISSION_READ);
				usagePermissions.add(PERMISSION_READ_PREVIEW);
				usagePermissions.add(PERMISSION_READ_ALL);
				usagePermissions.add(PERMISSION_CONSUMER);
				usagePermissions.add(PERMISSION_COMMENT);
				usagePermissions.add(PERMISSION_RATE);
				usagePermissions.add(PERMISSION_RATE_READ);
		}
		return unmodifiableSet(usagePermissions);
	}

	//AuthorityTypeKey
	public static final String PERM_AUTHORITYTYPE_KEY = "AUTHORITYTYPE_KEY";
	//AuthorityName
	public static final String PERM_AUTHORITY_NAME = "PERM_AUTHORITY_NAME";

	//Authority Types
	public static final String PERM_AUTHORITY_TYPE_USER = "USER";
	public static final String PERM_AUTHORITY_TYPE_ROLE = "ROLE";
	public static final String PERM_AUTHORITY_TYPE_GROUP = "GROUP";
	public static final String PERM_AUTHORITY_TYPE_GUEST = "GUEST";

	public static final String PERM_AUTHORITY_TYPE_EVERYONE = "EVERYONE";
	public static final String PERM_AUTHORITY_TYPE_ADMIN = "ADMIN";
	public static final String PERM_AUTHORITY_TYPE_OWNER = "OWNER";

	/**
	 * VCARD
	 */
	public final static String VCARD_URN_UID = "VCARD_URN_UID";
	public final static String VCARD_GIVENNAME = "VCARD_GIVENNAME";
	public final static String VCARD_SURNAME = "VCARD_SURNAME";
	public final static String VCARD_ORG = "VCARD_ORG";
	public final static String VCARD_TITLE = "VCARD_TITLE";
	public final static String VCARD_TEL = "VCARD_TEL";
	public final static String VCARD_STREET = "VCARD_STREET";
	public final static String VCARD_CITY = "VCARD_CITY";
	public final static String VCARD_PLZ = "VCARD_PLZ";
	public final static String VCARD_COUNTRY = "VCARD_COUNTRY";
	public final static String VCARD_REGION = "VCARD_REGION";
	public final static String VCARD_EMAIL = "VCARD_EMAIL";
	public final static String VCARD_URL = "VCARD_URL";

	public final static String VCARD_EXT_LOM_CONTRIBUTE_DATE = "VCARD_EXT_LOM_CONTRIBUTE_DATE";

	public final static String VCARD_T_FN = "FN";
	public final static String VCARD_T_N = "N";
	public final static String VCARD_T_ORG = "ORG";
	public final static String VCARD_T_TITLE = "TITLE";
	public final static String VCARD_T_TEL = "TEL";
	public final static String VCARD_T_ADR = "ADR";
	public final static String VCARD_T_EMAIL = "EMAIL";
	public final static String VCARD_T_URL = "URL";

	public final static String VCARD_T_X_ES_LOM_CONTRIBUTE_DATE = "X-ES-LOM-CONTRIBUTE-DATE";

	public final static String VCARD_T_X_ORCID = "X-ORCID";
	public final static String VCARD_T_X_GND_URI = "X-GND-URI";
	public final static String VCARD_T_X_ROR = "X-ROR";
	public final static String VCARD_T_X_WIKIDATA = "X-Wikidata";

	public final static String CCM_PROPS_IO_OBJECTTYPE_IO = "0";

	public final static String CCM_PROPS_IO_OBJECTTYPE_LS = "1";

	public final static String CM_VALUE_THUMBNAIL_NAME_CCUSERDEFINED = "CM_VALUE_THUMBNAIL_NAME_CCUSERDEFINED";

	public final static String CM_VALUE_THUMBNAIL_NAME_imgpreview_png = "imgpreview";

	public final static String KEY_CM_PROP_THUMBNAIL_NODEID = "KEY_CM_PROP_THUMBNAIL_NODEID";
	public final static String KEY_PREVIEWTYPE = "KEY_PREVIEWTYPE";
	public final static String KEY_PREVIEW_GENERATION_RUNS = "KEY_PREVIEW_GENERATION_RUNS";


	//AppInfo
	public final static String APPLICATIONINFO_APPID = "appid";
	public final static String APPLICATIONINFO_APPCAPTION = "appcaption";
	public final static String APPLICATIONINFO_ISHOMENODE = "is_home_node";
	public final static String APPLICATIONINFO_REPOSITORYTYPE = "repositorytype";
	public final static String APPLICATIONINFO_RECOMMENDOBJECTS_QUERY = "recommend_objects_query";
	public final static String APPLICATIONINFO_CUSTOM_CSS = "custom_css";
	public final static String APPLICATIONINFO_LOGO = "logo";
	public final static String APPLICATIONINFO_LOGOUTURL = "logouturl";

	//Mail
	public final static String SEND_ACTIVATION_REQUESTMAIL_TEXT = "Der Benutzer \"${username}, " +
			"(email: ${receivermail} )\" der Applikation \"${appCaption}\" möchte Zugriff auf " +
			"das Repository: \"${repCaption}\".\n\n" +
			"Wenn sie sich sicher sind, dass es sich bei dem Account: \"${username}\" der Applikation: \"" +
			"${appCaption}\" um Ihren account handelt," +
			"können Sie den Zugriff über die Applikation \"${appCaption}\" auf das Repository \"" +
			"${repCaption}\" mit einem Klick auf folgenden Link freischalten:";

	public static String getSendActivationRequestMailText(String username, String usermail, String appCaption, String repCaption, String activateApplicationLink){
		String messageText = new String(CCConstants.SEND_ACTIVATION_REQUESTMAIL_TEXT);
		messageText = messageText.replace("${username}", username);
		messageText = messageText.replace("${receivermail}", usermail);
		messageText = messageText.replace("${appCaption}", appCaption);
		messageText = messageText.replace("${repCaption}", repCaption);
		messageText += " \n\n\n" + activateApplicationLink;
		return messageText;
	}

	public static final String MULTIVALUE_SEPARATOR = "[#]";

	public static final String metadatasetsdefault = "/org/edu_sharing/metadataset/metadatasets_default.xml";

	public static final String metadataseStandaloneFileTemplate = "/org/edu_sharing/metadataset/metadataset_${name}.xml";

	public static final String metadataseStandaloneNameFilterRegex = "[^a-zA-Z]";

	public static final String metadatasetdefault_id = "default";
	public static final String metadatasetsystem_id = "system";

	public static final String metadatasetsearch_valuekey = "${value}";

	//@ToDo allow configuration in metadata xml files
	public static final String[] allowedLocale = {"de_DE","en_US","en_EN","ch_CN","fr_FR"};

	public static final String defaultLocale = "default";

	public static final String CC_EXCEPTIONPARAM_REPOSITORY_CAPTION = "{http://www.campuscontent.de/exceptionparam}repositorycap";

	public static final String CC_CACHE_MILLISECONDS_KEY = "CC_CACHE_MILLISECONDS_KEY";

	private static final Map<String, String> nameSpaceMap = Map.of(
			NAMESPACE_CCM, NAMESPACE_SHORT_CCM,
			NAMESPACE_CM, NAMESPACE_SHORT_CM,
			NAMESPACE_LOM, NAMESPACE_SHORT_LOM,
			NAMESPACE_SYS,  NAMESPACE_SHORT_SYS,
			NAMESPACE_VIRTUAL, NAMESPACE_SHORT_VIRTUAL,
			NAMESPACE_EXIF, NAMESPACE_SHORT_EXIF
	);

    /**
	 * @return <namespace,localnamespace>
	 */
	public static Map<String, String> getNameSpaceMap() {
		return nameSpaceMap;
	}

	/**
	 * get locale name for a namespace value
	 * @param value
	 * @return
	 */
	public static String getValidLocalName(String value){

		if(value == null) return null;

		for(Map.Entry<String,String> entry: getNameSpaceMap().entrySet()){
			if(value.contains(entry.getKey())){
				String valMinusNamespace =  value.replace("{"+entry.getKey()+"}","");
				return entry.getValue()+":"+valMinusNamespace;
			}
		}
		return null;
	}

	/**
	 * get globale name for a namespace value
	 * @param value
	 * @return
	 */
	public static String getValidGlobalName(String value){
		for(Map.Entry<String,String> entry: getNameSpaceMap().entrySet()){
			if(value.startsWith(entry.getValue())){
				return "{" + entry.getKey() + "}" + value.substring(entry.getValue().length() + 1);
			}
		}
		return null;
	}

	public static final String NAMESPACE_PREFIX_REGEX_LONG = "\\{.+\\}.+";
	public static final String NAMESPACE_PREFIX_REGEX_SHORT = "^[^\\{].+:.+";

	public static final String	EDUSEARCH_FLAG_GLOBALRESULT 	= "GLOBALRESULT";
	public static final String	EDUSEARCH_FLAG_VALUE_FALSE 		= "false";
	public static final String	EDUSEARCH_FLAG_VALUE_TRUE 		= "true";
	public static final String	EDUSEARCH_PARENTPATH 			= "PARENTPATH";

	public static final String REQUEST_PARAM_START_SEARCH = "p_startsearch";

	public static final String REQUEST_PARAM_DISABLE_GUESTFILTER = "guestfilteroff";

	public static final String REQUEST_PARAM_ACCESSTOKEN = "accessToken";

	public final static String THEME_DEFAULT_ID = "default";

	public static final String ISODATE_SUFFIX = "ISO8601";

	public static final String LONG_DATE_SUFFIX = "_LONG";
	public static final String DISPLAYNAME_SUFFIX = "_DISPLAYNAME";

	public static final String SESSION_FEDERATED_AUTH = "SESSION_FEDERATED_AUTH";

	public static final String SESSION_ARIX_CONTEXT_PREFIX = "ARIX_CONTEXTPATH_";

	public static final String DEFAULT_PREVIEW_IMG = "/images/preview_default/ksnapshot-file.svg";

	public static final String FILEUPLOAD_HIDDEN_DIV = "file_upload_hidden_div";

	public static final String MODE_SEARCH = "0";
	public static final String MODE_WORKSPACE = "1";
	public static final String MODE_UPLOAD = "2";

	public static final String WORKSPACE_INVITED_ANCHOR = "#workspace@INVITED@INVITED@/INVITED";

	public static final String SECURITY_KEY_ALGORITHM = "RSA";
	public static final String SECURITY_SIGN_ALGORITHM = "SHA1withRSA";

	public static final String EDU_SHARING_GLOBAL_GROUPS = "EDU_SHARING_GLOBAL_GROUPS";

	public static final String ADMINISTRATORS_GROUP_TYPE = "ORG_ADMINISTRATORS";
	
	public static final String EDITORIAL_GROUP_TYPE = "EDITORIAL";
	public static final String COLLECTIONTYPE_DEFAULT = "default";
	public static final String COLLECTIONTYPE_EDITORIAL = "EDITORIAL";
	public static final String COLLECTIONTYPE_MEDIA_CENTER = "MEDIA_CENTER";

	public static final String COLLECTION_COLOR_DEFAULT = "#975B5D";

	public static final String SESSION_LAST_SEARCH_TOKENS = "LAST_SEARCH_TOKENS";

	public static final String SESSION_REMOTE_AUTHENTICATIONS = "SESSION_REMOTE_AUTHENTICATIONS";

	// Stores the last "window" based rendering data request including information about the lms
	public static final String SESSION_RENDERING_DETAILS = "SESSION_RENDERING_DETAILS";

	public static final String COLLECTION_ORDER_MODE_CUSTOM = "custom";

	public static final String ISO8601_SUFFIX = "ISO8601";

	public static final String VERSION_COMMENT_PREVIEW_CHANGED = "PREVIEW_CHANGED";

	public static final String VERSION_COMMENT_BULK_CREATE = "BULK_CREATE";
	public static final String VERSION_COMMENT_BULK_UPDATE = "BULK_UPDATE";

	public static final String VERSION_COMMENT_BULK_MIGRATION = "BULK_MIGRATION";
	public static final String VERSION_COMMENT_BULK_UPDATE_RESYNC = "BULK_UPDATE_RESYNC";
	public static final String VERSION_COMMENT_REMOTE_OBJECT_INIT = "REMOTE_OBJECT_INIT";

	/**
	 * Methos that set all the Properties for ProfileSettings
	 * @return (List) list of all properties we want to be in ProfileSettings
	 */
	public static List<String> getAllPropertiesOfProfileSettings(){
		List<String> listOfProperties=new ArrayList<>();
		listOfProperties.add(CCM_PROP_PERSON_SHOW_EMAIL);
		return listOfProperties;
	}

}
