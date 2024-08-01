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
package org.edu_sharing.repository.server.tools;

import net.sourceforge.cardme.engine.VCardEngine;
import net.sourceforge.cardme.io.VCardWriter;
import net.sourceforge.cardme.vcard.VCard;
import net.sourceforge.cardme.vcard.VCardImpl;
import net.sourceforge.cardme.vcard.arch.VCardVersion;
import net.sourceforge.cardme.vcard.exceptions.VCardBuildException;
import net.sourceforge.cardme.vcard.types.AdrType;
import net.sourceforge.cardme.vcard.types.ExtendedType;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.client.tools.StringTool;

import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class VCardConverter {

	static Logger logger = Logger.getLogger(VCardConverter.class);
	public static ArrayList<Map<String,Object>> vcardToMap(String propPrefix, String vcardString){

		ArrayList<Map<String,Object>> result = new ArrayList<>();
		try {
			VCardEngine engine = new VCardEngine();
			// unfortunately, the multi vcard string method is only private, so we need to make it accessible
			Method method = engine.getClass().getDeclaredMethod("parseManyInOneVCard", String.class);
			method.setAccessible(true);
			for(VCard vcard : (List<VCard>)method.invoke(engine,vcardString)) {
				Map<String,Object> vcardMap = new HashMap<>();
				String fn=vcard.getFN()==null ? null : vcard.getFN().getFormattedName();
				String familyName=vcard.getN()==null ? null : vcard.getN().getFamilyName();
				String givenName=vcard.getN()==null ? null : vcard.getN().getGivenName();
				String mail=null;
				if(vcard.getEmails()!=null && vcard.getEmails().size()>0){
					mail=vcard.getEmails().get(0).getEmail();
				}
				String org=vcard.getOrg()!=null ? vcard.getOrg().getOrgName() : null;
				String title=vcard.getTitle()!=null ? vcard.getTitle().getTitle() : null;
				String tel=null;
				if(vcard.getTels()!=null && vcard.getTels().size()>0){
					tel=vcard.getTels().get(0).getTelephone();
				}
				String url=null;
				if(vcard.getUrls()!=null && vcard.getUrls().size()>0){
					url=vcard.getUrls().get(0).getRawUrl();
				}
				Map<String,String> extendedData=new HashMap<>();
				if(vcard.getExtendedTypes()!=null) {
					for (ExtendedType extended : vcard.getExtendedTypes()) {
						extendedData.put(extended.getExtendedName(), extended.getExtendedValue());
					}
				}
				AdrType adr = vcard.getAdrs()!=null && vcard.getAdrs().size()>0 ? vcard.getAdrs().get(0) : null;
				vcardMap.put(propPrefix + CCConstants.VCARD_T_FN, (fn != null) ? fn : "");
				vcardMap.put(propPrefix + CCConstants.VCARD_SURNAME, (familyName != null) ? familyName : "");
				vcardMap.put(propPrefix + CCConstants.VCARD_GIVENNAME, (givenName != null) ? givenName : "");

				vcardMap.put(propPrefix + CCConstants.VCARD_STREET, (adr != null) ? adr.getStreetAddress() : "");
				vcardMap.put(propPrefix + CCConstants.VCARD_CITY, (adr != null) ? adr.getLocality() : "");
				vcardMap.put(propPrefix + CCConstants.VCARD_PLZ, (adr != null) ? adr.getPostalCode() : "");
				vcardMap.put(propPrefix + CCConstants.VCARD_REGION, (adr != null) ? adr.getRegion() : "");
				vcardMap.put(propPrefix + CCConstants.VCARD_COUNTRY, (adr != null) ? adr.getCountryName() : "");
				vcardMap.put(propPrefix + CCConstants.VCARD_EMAIL, (mail != null) ? mail : "");
				vcardMap.put(propPrefix + CCConstants.VCARD_ORG, (org != null) ? org : "");
				vcardMap.put(propPrefix + CCConstants.VCARD_TITLE, (title != null) ? title : "");
				vcardMap.put(propPrefix + CCConstants.VCARD_TEL, (tel != null) ? tel : "");
				vcardMap.put(propPrefix + CCConstants.VCARD_URL, (url != null) ? url : "");

				/*if (extendedData.get(CCConstants.VCARD_T_X_ES_LOM_CONTRIBUTE_DATE) != null) {
					vcardMap.put(propPrefix + CCConstants.VCARD_EXT_LOM_CONTRIBUTE_DATE,extendedData.get(CCConstants.VCARD_T_X_ES_LOM_CONTRIBUTE_DATE));
				}*/

				for(Map.Entry<String,String> extended:extendedData.entrySet()){
					vcardMap.put(extended.getKey(),extended.getValue());
				}

				if(vcard.getVersion() != null && (
						VCardVersion.V3_0.equals(vcard.getVersion().getVersion()) ||
								VCardVersion.V4_0.equals(vcard.getVersion().getVersion())
				)) {
					vcardMap = new HashMap<>(vcardMap.entrySet().stream().filter(e -> ( e.getKey() != null && e.getValue() != null)).collect(Collectors.toMap(e -> e.getKey(), e-> e.getValue())));
					vcardMap = new HashMap<>(vcardMap.entrySet()
							.stream()
							.collect(Collectors.toMap(
											e -> e.getKey(),
											e -> e.getValue().toString().replace("\\\\", "\\").replace("\\,", ",")
									)
							));
				result.add(vcardMap);
				}
			}
		} catch (Exception e) {
			logger.debug(e.getMessage(),e);
			return new ArrayList<>();
		}
		return result;
	}

	public static String getNameForVCard(String prefix,Map<String,Object> data){
		if(isPersonVCard(prefix,data)){
			String name="";
			if(data.containsKey(prefix+CCConstants.VCARD_TITLE))
				name+=data.get(prefix+CCConstants.VCARD_TITLE)+" ";
			if(data.containsKey(prefix+CCConstants.VCARD_GIVENNAME))
				name+=data.get(prefix+CCConstants.VCARD_GIVENNAME)+" ";
			if(data.containsKey(prefix+CCConstants.VCARD_SURNAME))
				name+=data.get(prefix+CCConstants.VCARD_SURNAME)+" ";
			if(!name.trim().isEmpty())
				return name.trim();
		}
		if(data.containsKey(prefix+CCConstants.VCARD_ORG))
			return (String) data.get(prefix+CCConstants.VCARD_ORG);
		return null;
	}

	public static String getNameForVCardString(String vcardString) {
		ArrayList<Map<String, Object>> vcard = VCardConverter.vcardToMap(null, vcardString);
		if (vcard.size() == 0)
			return null;
		return StringUtils.join(vcard.stream().map(v ->
					VCardConverter.getNameForVCard(null, v)
		).collect(Collectors.toList()), ", ");
	}


	public static String removeEMails(String vcardString){
		return StringUtils.join(cleanupVcard(vcardString, (vcard) -> {
			if(vcard.getEmails() != null) {
				vcard.getEmails().forEach(vcard::removeEmail);
			}
			return vcard;
		}), CCConstants.MULTIVALUE_SEPARATOR);
	}

	/**
	 * removes all given vcard props in the given vcard string (can be multi value)
	 * useful to remove sensible data (e.g. email)
	 */
	public static List<String> cleanupVcard(String vcardString, Function<VCard, VCard> cleanup ){
		try{
			VCardEngine engine = new VCardEngine();
			// unfortunately, the multi vcard string method is only private, so we need to make it accessible
			Method method = engine.getClass().getDeclaredMethod("parseManyInOneVCard", String.class);
			method.setAccessible(true);
			return ((List<VCard>) method.invoke(engine, vcardString)).stream().map(cleanup).map((vCard) -> {
				VCardWriter writer = new VCardWriter();
				writer.setVCard(vCard);
				try {
					return writer.buildVCardString();
				} catch (VCardBuildException e) {
					throw new RuntimeException(e);
				}
			}).collect(Collectors.toList());
		}catch(Throwable t){
			logger.warn("Could not cleanup VCard, reading failed: " + t.getMessage(), t);
			// return empty vcard, not the original vcard, since otherwise we might expose private data
			VCardWriter writer = new VCardWriter();
			writer.setVCard(new VCardImpl());
			try {
				return Collections.singletonList(writer.buildVCardString());
			} catch (VCardBuildException e) {
				throw new RuntimeException(e);
			}
		}
	}
	public static ArrayList<Map<String,Object>> vcardToMap(String vcardString){
		return vcardToMap("", vcardString);
	}

	static ArrayList<String> vcardProps = null;

	static public boolean isVCardProp(String property) {
		if(vcardProps == null){
			vcardProps = new ArrayList<>();
			vcardProps.add(CCConstants.LOM_PROP_CONTRIBUTE_ENTITY);
			vcardProps.add(CCConstants.CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_AUTHOR);
			vcardProps.add(CCConstants.CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_PUBLISHER);
			vcardProps.add(CCConstants.CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_CONTENT_PROVIDER);
			vcardProps.add(CCConstants.CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_EDITOR);
			vcardProps.add(CCConstants.CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_EDUCATIONAL_VALIDATOR);
			vcardProps.add(CCConstants.CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_GRAPHICAL_DESIGNER);
			vcardProps.add(CCConstants.CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_INITIATOR);
			vcardProps.add(CCConstants.CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_INSTRUCTIONAL_DESIGNER);
			vcardProps.add(CCConstants.CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_SCRIPT_WRITER);
			vcardProps.add(CCConstants.CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_SUBJECT_MATTER_EXPERT);
			vcardProps.add(CCConstants.CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_TECHNICAL_IMPLEMENTER);
			vcardProps.add(CCConstants.CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_TECHNICAL_VALIDATOR);
			vcardProps.add(CCConstants.CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_TERMINATOR);
			vcardProps.add(CCConstants.CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_UNKNOWN);
			vcardProps.add(CCConstants.CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_VALIDATOR);

			vcardProps.add(CCConstants.CCM_PROP_IO_REPL_METADATACONTRIBUTER_CREATOR);
			vcardProps.add(CCConstants.CCM_PROP_IO_REPL_METADATACONTRIBUTER_PROVIDER);
			vcardProps.add(CCConstants.CCM_PROP_IO_REPL_METADATACONTRIBUTER_VALIDATOR);
		}
		if(vcardProps.contains(property)){
			return true;
		}else{
			return false;
		}
	}

	/**
	 * tests if Property is VCard Prop and converts the String to HashMap used
	 * for contributers
	 *
	 * @param type
	 * @param property
	 * @param value
	 * @return
	 */
	public static Map<String, Object> getVCardMap(String type, String property, String value) {

		if(value == null) {
			return new HashMap<>();
		}

		Map<String, Object> result =  null;

		// VCard
		if (isVCardProp(property)) {

			//split for multivalue vcards:
			String[] splitted = value.split(StringTool.escape(CCConstants.MULTIVALUE_SEPARATOR));
			for(String split:splitted){
				ArrayList<Map<String,Object>> vcards = vcardToMap(property, split);
				if (vcards != null) {
					if(vcards.size()==0)
						return new HashMap<>();
					if(result == null){
						result = vcards.get(0);
					}else{
						Map<String,Object> tmpVCardMap = vcards.get(0);
						for(Map.Entry<String,Object> vcardEntry : tmpVCardMap.entrySet()){
							//check if vcardprop already has a value in result
							String vcardValInResult = (String)result.get(vcardEntry.getKey());
							if(vcardValInResult != null && !vcardValInResult.trim().equals("")){
								result.put(vcardEntry.getKey(), vcardValInResult + CCConstants.MULTIVALUE_SEPARATOR + (String)vcardEntry.getValue());
							}else{
								result.put(vcardEntry.getKey(), vcardEntry.getValue());
							}
						}
					}

				}
			}
		}

		return result;
	}

	/**
	 * Search for all vcard properties and inflate them, returning a new property map with all inflated vcard props
	 * @param nodeType
	 * @param props
	 * @return
	 */
	public static Map<String, Object> addVCardProperties(String nodeType, Map<String, Object> props) {
		Map<String,Object> propsNew = new HashMap<>();
		propsNew.putAll(props);
		for(Map.Entry<String, Object> entry : props.entrySet()){
			if(entry == null || entry.getKey() == null || entry.getValue() == null) continue;
			Map<String,Object> vcard = getVCardMap(nodeType, entry.getKey(), entry.getValue().toString());
			if(vcard!=null)
				propsNew.putAll(vcard);
		}
		return propsNew;
	}

	public static boolean isPersonVCard(String prefix, Map<String, Object> data) {
		return data.get(prefix+CCConstants.VCARD_GIVENNAME)!=null && !data.get(prefix+CCConstants.VCARD_GIVENNAME).toString().isEmpty() ||
				data.containsKey(prefix+CCConstants.VCARD_SURNAME) && !data.get(prefix+CCConstants.VCARD_SURNAME).toString().isEmpty();
	}
}
