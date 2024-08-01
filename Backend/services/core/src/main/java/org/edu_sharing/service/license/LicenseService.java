package org.edu_sharing.service.license;

import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.tools.URLHelper;

public class LicenseService {

	public String getIconUrl(String license,boolean dynamic){
		if(license==null || license.isEmpty())
			license="none";
		String ccImageName = license.toLowerCase().replace("_", "-")+".svg";
		String url = URLHelper.getBaseUrl(dynamic) + "/ccimages/licenses/" + ccImageName;

		return url;
	}
	public String getIconUrl(String license){
		return getIconUrl(license,true);
	}

	public String getLicenseUrl(String license, String locale){
		return getLicenseUrl(license, locale, null);
	}
	
	public String getLicenseUrl(String license, String locale, String version){
		if(license==null || license.isEmpty())
			return null;
		String result = null;
		if (license.equals(CCConstants.COMMON_LICENSE_CC_BY)) {
			result = CCConstants.COMMON_LICENSE_CC_BY_LINK;
		}
		if (license.equals(CCConstants.COMMON_LICENSE_PDM)) {
			result = CCConstants.COMMON_LICENSE_CC_PDM_LINK;
		}
		if (license.equals(CCConstants.COMMON_LICENSE_CC_ZERO)) {
			result = CCConstants.COMMON_LICENSE_CC_ZERO_LINK;
		}
		if (license.equals(CCConstants.COMMON_LICENSE_CC_BY_NC)) {
			result = CCConstants.COMMON_LICENSE_CC_BY_NC_LINK;
		}
		if (license.equals(CCConstants.COMMON_LICENSE_CC_BY_NC_ND)) {
			result = CCConstants.COMMON_LICENSE_CC_BY_NC_ND_LINK;
		}
		if (license.equals(CCConstants.COMMON_LICENSE_CC_BY_NC_SA)) {
			result = CCConstants.COMMON_LICENSE_CC_BY_NC_SA_LINK;
		}
		if (license.equals(CCConstants.COMMON_LICENSE_CC_BY_ND)) {
			result = CCConstants.COMMON_LICENSE_CC_BY_ND_LINK;
		}
		if (license.equals(CCConstants.COMMON_LICENSE_CC_BY_SA)) {
			result = CCConstants.COMMON_LICENSE_CC_BY_SA_LINK;
		}
		if (license.equals(CCConstants.COMMON_LICENSE_EDU_NC)) {
			result = CCConstants.COMMON_LICENSE_EDU_LINK;
		}
		
		if (license.equals(CCConstants.COMMON_LICENSE_EDU_NC_ND)) {
			result = CCConstants.COMMON_LICENSE_EDU_LINK;
		}
		
		if(result != null){
			version = (version == null) ? "3.0" : version;
			if(result.contains("${version}")){
				result = result.replace("${version}", version);
			}
			
			String country = (locale == null) ? "de" : locale.split("_")[0];
			if(result.contains("${locale}")){
				result = result.replace("${locale}", country);
			}
		}
		
		return result;
	}
		
}
