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
package org.edu_sharing.service.authentication.cas;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.input.SAXBuilder;

public class PTValidation  {
	
	Logger logger = Logger.getLogger(PTValidation.class);
	String url = null;
	String callbackURL = null;
	String serviceUrl = null;
	
	public static String USER = "user";
	public static String CALLBACKURL = "callbackUrl";
	
	public PTValidation(String validatePTURL, String _serviceUrl, String validCallbackUrl){
		logger.info("Creating PTValidation Object with:");
		logger.info("validatePTURL:"+validatePTURL);
		logger.info("serviceUrl:"+_serviceUrl);
		logger.info("validCallbackUrl:"+validCallbackUrl);
		System.out.println("Creating PTValidation Object with:");
		System.out.println("validatePTURL:"+validatePTURL);
		System.out.println("validCallbackUrl:"+validCallbackUrl);
		url = validatePTURL;
		serviceUrl = _serviceUrl;
		callbackURL = validCallbackUrl;
		
	}
	/**
	 * 
	 * @param proxyTicket
	 * @return map with Key user Value usernamevalue and Key CallbackURL Value callbackurlval
	 * if no valid proxyTicket it returns null
	 * if valid than the User is included in the resultmap
	 * if one callbackurl equals the construktorParam validCallbackUrl than this callbackurl is packed in the reultMap
	 */
	public Map<String,String> getCredentials(String proxyTicket){
		Map<String,String> resultMap = null;
		try{
			url = url+"?ticket="+proxyTicket+"&service="+serviceUrl;
			logger.info("url:"+url);
			System.out.println("getCredentials: url:"+url);
			GetMethod get = new GetMethod(url);
			
			HttpClient client = new HttpClient();
			client.executeMethod(get);
			InputStream result = get.getResponseBodyAsStream();
			System.out.println("PTValidation getting back from cas:");
			//System.out.println(get.getResponseBodyAsString());
			Document doc = new SAXBuilder().build(result);
			Element rootEle = doc.getRootElement();
			if (rootEle != null) {
				System.out.println("rootEle.getName():" + rootEle.getName());
				Namespace ns = rootEle.getNamespace();
				System.out.println("ns.getPrefix():" + ns.getPrefix());
				if (rootEle.getName().equals("serviceResponse")) {
					Element succEle = rootEle.getChild("authenticationSuccess", ns);
					if (succEle != null) {
						resultMap = new HashMap<>();
						System.out.println(" succEle :" + succEle.getName());
						Element eleUser = succEle.getChild("user", ns);
						resultMap.put(PTValidation.USER, eleUser.getValue());
						System.out.println("eleUser.getName():" + eleUser.getName());
						Element eleProxies = succEle.getChild("proxies", ns);
						
						if (eleProxies != null) {
							List list = eleProxies.getChildren("proxy", ns);
							for(Object obj:list){
								Element ele = (Element)obj;
								if(ele.getValue().equals(callbackURL)){
									resultMap.put(PTValidation.CALLBACKURL, ele.getValue());
								}
							}
						}
					}
				}
			}
		}catch(Exception e){
			e.printStackTrace();
			return null;
		}
		return resultMap;
	}
	
}
