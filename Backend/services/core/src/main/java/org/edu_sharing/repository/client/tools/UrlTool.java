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
package org.edu_sharing.repository.client.tools;

import java.util.ArrayList;
import java.util.List;

import org.springframework.extensions.surf.util.URLEncoder;


public class UrlTool {
	/**
	 * Set a param for a given url
	 * @param url the whole url
	 * @param param the param name to add
	 * @param value the value to add (must be pre-encoded)
	 * @return
	 */
	public static String setParam(String url, String param, String value){
		if(url != null){
			
			/**
			 * the hash is the last part of a url
			 */
			String[] hashSplitted = url.split("#");
			if(hashSplitted.length > 1){
				url = hashSplitted[0];
			}
			String paramsep = (url.indexOf("?") > -1)? "&": "?";
			url = url + paramsep + param + "=" +value;
			
			if(hashSplitted.length > 1){
				url = url+"#"+hashSplitted[1];
			}
			
			return url;
		}else{
			return url;
		}
	}
	/**
	 * Like set param, but will encode the value using encodeUriComponent
	 * @param url
	 * @param param
	 * @param value
	 * @return
	 */
	public static String setParamEncode(String url, String param, String value){
		return setParam(url, param, URLEncoder.encodeUriComponent(value));
	}
	public static String removeParam(String url, String param){
		
		if(!url.contains("?")) return url;
		
		String baseUrl =  url.split("\\?")[0];
		String queryPart = url.split("\\?")[1];
		
		if(!queryPart.contains(param)) return url;
		
		String[] params = queryPart.split("\\&");
		List<String> newParams = new ArrayList<>();
		for(String p : params){
			if(!p.contains(param)){
				newParams.add(p);
			}
		}
		
		String newUrl = baseUrl;
		for(int i = 0; i < newParams.size(); i++){
			String p = newParams.get(i);
			if(i == 0){
				newUrl = newUrl + "?"+p;
			}else{
				newUrl = newUrl + "&"+p;
			}
		}
		
		return newUrl;
	}


	public static String replaceParam(String url, String param, String value){
		url = removeParam(url, param);
		url = setParam(url, param, value);
		return url;
	}
	
	/**
	 * if sourceUrl contains jsessionid it will be added to target url
	 * 
	 * @param sourceUrl
	 * @param targetUrl without query param!!!
	 * @return targetUrl with jsessionid if sourceUrl includes it
	 */
	public static String setJSessionId(String sourceUrl, String targetUrl){
		
		String jsessionId = UrlTool.getJessionId(sourceUrl);
		if(jsessionId != null){
			
			//String[] splitted = targetUrl.split("\\?");
			
			targetUrl = targetUrl + ";jsessionid="+jsessionId;
		}
		return targetUrl;
	}
	
	/**
	 * 
	 * @param url
	 * @return jsessionid when url contains it, else null
	 */
	public static String getJessionId(String url){
		String result = null;
		if(url.contains(";jsessionid=")){
			String[] splittedUrl = url.split(";jsessionid=");
			String partjsessionId = splittedUrl[1];
			//remove query String
			result = (partjsessionId.contains("?")) ? partjsessionId.split("\\?")[0] : partjsessionId;
		}
		return result;
	}
	
}
