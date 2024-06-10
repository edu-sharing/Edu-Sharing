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


public class VCardTool {
	public static String nameToVCard(String name){
		Map<String, String> map = new HashMap<>();
		map.put(CCConstants.VCARD_GIVENNAME, name);
		return hashMap2VCard(map);
	}
	public static String hashMap2VCard(Map<String,String> map){
		
		String uid = getValid(map.get(CCConstants.VCARD_URN_UID));
		String surname = getValid(map.get(CCConstants.VCARD_SURNAME));
		String givenname = getValid(map.get(CCConstants.VCARD_GIVENNAME));
		String city = getValid(map.get(CCConstants.VCARD_CITY));
		String country = getValid(map.get(CCConstants.VCARD_COUNTRY));
		String email = getValid(map.get(CCConstants.VCARD_EMAIL));
		String org = getValid(map.get(CCConstants.VCARD_ORG));
		String plz = getValid(map.get(CCConstants.VCARD_PLZ));
		String region = getValid(map.get(CCConstants.VCARD_REGION));
		String street = getValid(map.get(CCConstants.VCARD_STREET));
		String phone = getValid(map.get(CCConstants.VCARD_TEL));
		String title = getValid(map.get(CCConstants.VCARD_TITLE));
		String url = getValid(map.get(CCConstants.VCARD_URL));
		
		String xESLomContributeDate = getValid(map.get(CCConstants.VCARD_EXT_LOM_CONTRIBUTE_DATE));
		
		String vCard = "BEGIN:VCARD\n"
		      + "VERSION:3.0\n"
		      + "UID:urn:uuid:" + uid + "\n"
		      + "N:" + surname+";"+givenname + "\n"
		      + "FN:" + givenname+" "+surname + "\n"
		      + "ORG:" + org + "\n"
		      + "URL:" + url + "\n"
		      + "TITLE:" + title + "\n"
		      + "TEL;TYPE=WORK,VOICE:" + phone + "\n"
		      + "ADR;TYPE=intl,postal,parcel,work:;;" + street+";"+city+";"+region+";"+plz+";"+country + "\n"
		      	 //ADR;TYPE=intl,work,postal,parcel:;;Musterstraße 1;Musterstadt;;12345;Germany
		      + "EMAIL;TYPE=PREF,INTERNET:" + email + "\n";
		      
		
		if(!xESLomContributeDate.equals("")){
			vCard += CCConstants.VCARD_T_X_ES_LOM_CONTRIBUTE_DATE+":"+xESLomContributeDate+ "\n";
		}

		//extended atts
		for(Map.Entry<String,String> entry: map.entrySet()){
			if(entry.getKey().startsWith("X-")){
				String validValue = getValid(entry.getKey());
				if(!validValue.trim().equals("")){
					vCard += entry.getKey()+":"+entry.getValue()+ "\n";
				}
			}
		}

		//close vcard
		vCard += "END:VCARD\n";
		
		return vCard;
	}
	
	private static String getValid(Object value){
		if(value == null) value = "";
		return (String)value;
	}
}
