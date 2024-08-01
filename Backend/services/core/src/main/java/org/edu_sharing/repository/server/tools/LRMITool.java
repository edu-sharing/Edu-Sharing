package org.edu_sharing.repository.server.tools;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigValue;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.log4j.Logger;
import org.edu_sharing.alfresco.lightbend.LightbendConfigLoader;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.client.tools.forms.VCardTool;
import org.edu_sharing.repository.client.tools.metadata.ValueTool;
import org.edu_sharing.repository.server.NodeRefVersion;
import org.edu_sharing.repository.tools.URLHelper;
import org.edu_sharing.service.license.LicenseService;
import org.edu_sharing.service.nodeservice.NodeServiceHelper;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.*;
import java.util.stream.Collectors;

public class LRMITool {

    private static final List<String> IGNORED_PROPERTIES = Arrays.asList(CCConstants.SYS_PROP_NODE_UID);
    private static final Map<String,String> VCARD_MAPPING=new HashMap<>();
    static{
        VCARD_MAPPING.put("contributor",CCConstants.CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_MITWIRKENDE);
        VCARD_MAPPING.put("creator",CCConstants.CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_AUTHOR);
        VCARD_MAPPING.put("publisher",CCConstants.CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_PUBLISHER);
    }
    private static Logger logger=Logger.getLogger(LRMITool.class);

    private static final String OR_SEPERATOR = ",";
    private static final String AND_SEPERATOR = "+";

    public static JSONObject getLRMIJson(NodeRefVersion node) throws Throwable {
        JSONObject lrmi=new JSONObject();
        // TODO: This probably has to work for remote repos in future
        Map<String, Object> props = NodeServiceHelper.getPropertiesVersion(node.getNodeRef(), node.getVersion());
        Config lrmiProps = getMappingFile();
        lrmi.put("@context","http://schema.org/");
        lrmi.put("@type",new String[]{"CreativeWork","MediaObject"});
        for(Map.Entry<String,String> vcard : VCARD_MAPPING.entrySet()){
            Object data = getFromVCard(vcard.getValue(), props);
            if(data != null) {
                lrmi.put(vcard.getKey(), data);
            }
        }
        lrmi.put("url", URLHelper.getNgRenderNodeUrl(node.getNodeRef().getId(),node.getVersion()));
        lrmi.put("thumbnailUrl",NodeServiceHelper.getPreview(new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE,node.getNodeRef().getId())).getUrl());
        lrmi.put("dateCreated",getDate(getProperty(props, Collections.singletonList(CCConstants.CM_PROP_C_CREATED+CCConstants.LONG_DATE_SUFFIX))));
        lrmi.put("dateModified",getDate(getProperty(props, Collections.singletonList(CCConstants.CM_PROP_C_MODIFIED+CCConstants.LONG_DATE_SUFFIX))));
        lrmi.put("datePublished",getDate(getProperty(props, Collections.singletonList(CCConstants.CCM_PROP_PUBLISHED_DATE+CCConstants.LONG_DATE_SUFFIX))));
        lrmi.put("license",new LicenseService().getLicenseUrl(
                (String)props.get(CCConstants.CCM_PROP_IO_COMMONLICENSE_KEY),
                (String)props.get(CCConstants.CCM_PROP_IO_COMMONLICENSE_CC_LOCALE),
                (String)props.get(CCConstants.CCM_PROP_IO_COMMONLICENSE_CC_VERSION)
        ));
        for(Map.Entry<String, ConfigValue> prop : lrmiProps.entrySet()){
            // split by "," (or) first, than combine all the "+" concats (and)
            // finally, convert them to global names
            List<List<String>> propsListAnd = Arrays.stream(prop.getValue().unwrapped().toString().split(OR_SEPERATOR)).map((f) ->
                    Arrays.stream(f.split("\\"+AND_SEPERATOR)).
                            map(CCConstants::getValidGlobalName).
                            collect(Collectors.toList())).
                    collect(Collectors.toList()
            );
            lrmi.put(prop.getKey(),getPropertyCombined(props,propsListAnd));
        }
        /*
        lrmi.put("name",getProperty(props,CCConstants.LOM_PROP_GENERAL_TITLE,CCConstants.CM_NAME));
        lrmi.put("about",getProperty(props,CCConstants.LOM_PROP_GENERAL_DESCRIPTION));
        lrmi.put("learningResourceType",getProperty(props,CCConstants.LOM_PROP_EDUCATIONAL_LEARNINGRESOURCETYPE));
        lrmi.put("typicalAgeRange",getProperty(props,CCConstants.CCM_PROP_IO_REPL_EDUCATIONAL_TYPICALAGERANGE));
        lrmi.put("timeRequired",getProperty(props,CCConstants.CCM_PROP_IO_REPL_EDUCATIONAL_TYPICALLEARNINGTIME));
        */
        return lrmi;
    }

    private static Config getMappingFile() throws Exception {
        return LightbendConfigLoader.get().getConfig("exporter.lrmi.properties");
    }

    private static String getDate(Object property) {
        if(property==null)
            return null;
        try {
            return DateFormatUtils.ISO_DATETIME_TIME_ZONE_FORMAT.format(new Date(Long.parseLong((String) property)));

        }
        catch(Throwable t){
            return property.toString();
        }
    }

    private static Object getFromVCard(String key,Map<String,Object> props) throws JSONException {
        if(props.get(key)==null)
            return null;
        String[] values=new ValueTool().getMultivalue(props.get(key).toString());
        JSONArray result=new JSONArray();
        for(String v: values) {
            Map<String, Object> vcard = VCardConverter.getVCardMap(null, key, v);
            if (vcard == null || vcard.size() == 0)
                return null;

            JSONObject json = new JSONObject();
            if (VCardConverter.isPersonVCard(key, vcard)) {
                json.put("@type", "Person");
                json.put("givenName", vcard.get(key + CCConstants.VCARD_GIVENNAME));
                json.put("familyName", vcard.get(key + CCConstants.VCARD_SURNAME));
            } else {
                json.put("@type", "Organization");
                json.put("legalName", vcard.get(key + CCConstants.VCARD_ORG));
            }
            String name = VCardConverter.getNameForVCard(key, vcard);
            if(name!=null && !name.isEmpty()) {
                result.put(json);
            }
        }
        if(result.length()>1) {
            return result;
        } else if (result.length() == 1) {
            return result.getJSONObject(0);
        } else {
            return null;
        }
    }

    /**
     * The outer list defines the first hit
     * The first outer list hit will be returned (basically "or" combined)
     * All inner list properties are concated
     * @param props
     * @param keys
     * @return
     */
    private static Object getPropertyCombined(Map<String, Object> props, List<List<String>> keys) {
        for(List<String> keyArray : keys){
            List<Object> result=new ArrayList<>();
            for(String key : keyArray) {
                if (!props.containsKey(key) || props.get(key) == null)
                    continue;
                if (props.get(key) instanceof String) {
                    if (((String) props.get(key)).isEmpty()) {
                        continue;
                    }
                    String[] data = StringUtils.splitByWholeSeparator((String) props.get(key), CCConstants.MULTIVALUE_SEPARATOR);
                    if (data.length > 1)
                        result.addAll(Arrays.asList(data));
                    else
                        result.add(data[0]);
                }
                else
                    result.add(props.get(key));
            }
            if(result.size()==1)
                return result.get(0);
            else if(result.size()>1)
                return result;
            // else continue until a filled field is found
        }
        return null;
    }
    private static Object getProperty(Map<String, Object> props, List<String> keys) {
        return getPropertyCombined(props,Collections.singletonList(keys));
    }

    public static Map<String, String[]> fromLRMIJsonToProperties(JSONObject jsonObject) throws Exception {
        Map<String,String[]> props=new HashMap<>();
        Config lrmiProps = getMappingFile();
        for(Map.Entry<String,String> vcard : VCARD_MAPPING.entrySet()){
            try {
                props.put(vcard.getValue(), new String[]{asVCard(jsonObject.getJSONObject(vcard.getKey()))});
            }catch(Exception e){
                logger.info("Can not map vcard lrmi value for "+vcard.getKey()+": "+e.getMessage());
            }
        }
        for(Map.Entry<String, ConfigValue> prop : lrmiProps.entrySet()){
            try{
                if(!jsonObject.has(prop.getKey())){
                    logger.debug("Can not map lrmi field "+prop.getKey()+" because the json is missing it.");
                    continue;
                }
                Object data=jsonObject.get(prop.getKey());
                String property=prop.getValue().unwrapped().toString();
                // if there are multiple targets, use the first for reverse mapping
                property=property.split(OR_SEPERATOR)[0].split("\\"+AND_SEPERATOR)[0];
                if(IGNORED_PROPERTIES.contains(property)){
                    continue;
                }
                if(data instanceof JSONArray){
                    JSONArray jsonArray = (JSONArray) data;
                    props.put(property,new String[jsonArray.length()]);
                    for(int i=0;i<jsonArray.length();i++){
                        props.get(property)[i]=jsonArray.getString(i);
                    }
                }
                else if(data instanceof String){
                    props.put(property,new String[]{(String)data});
                }
                else{
                    logger.warn("Can not map lrmi attribute "+prop.getKey()+" to "+property+" because the object type is unsupported: "+data.getClass().getSimpleName());
                }
            }catch(Throwable t){
                logger.info("Error mapping lrmi: "+t.getMessage(),t);
            }
        }
        return props;
    }

    /**
     * map the givven lrmi json person entry into a vcard string
     * "publisher": {
     *     "@type": "Person",
     *     "givenName": "A",
     *     "familyName": "B"
     *   },
     */
    private static String asVCard(JSONObject object) throws JSONException {
        Map<String, String> map = new HashMap<>();
        String type = object.getString("@type");
        if(type.equals("Person")){
            map.put(CCConstants.VCARD_GIVENNAME, object.getString("givenName"));
            map.put(CCConstants.VCARD_SURNAME, object.getString("familyName"));
        }
        else if(type.equals("Organization")){
            map.put(CCConstants.VCARD_ORG, object.getString("legalName"));
        }
        return VCardTool.hashMap2VCard(map);
    }
}
