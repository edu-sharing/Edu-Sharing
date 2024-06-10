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
package org.edu_sharing.service.authentication;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.directory.Attribute;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import org.alfresco.service.namespace.QName;
import org.apache.log4j.Logger;
import org.edu_sharing.repository.client.tools.CCConstants;

public class UserDataServiceLDAPImpl extends UserDataServiceAbstract {

	Logger logger = Logger.getLogger(UserDataServiceLDAPImpl.class);

	/**
	 * ldap.initial_context_factory=com.sun.jndi.ldap.LdapCtxFactory
	 * ldap.provider_url=ldap://localhost:12345/dc=produktion,dc=schulcockpit
	 * ldap.security_authentication=simple
	 * ldap.security_principal=cn=Manager,dc=schulcockpit
	 * ldap.security_credentials=secret
	 */

	/**
	 * LDAP propetries as Spring beans
	 */
	String initialContextFacory = "com.sun.jndi.ldap.LdapCtxFactory";
	String providerUrl = null;
	String securityAuthentication = null;
	String securityPrincipal = null;
	String securityCredentials = null;

	String firstNameProp = null;
	String lastNameProp = null;
	String emailProp = null;
	String guidProp = null;
	String uidProp = "uid";

	Map<String, String> attributMapping = null;

	@Override
	public Map<QName, Serializable> getRepositoryUserProperties(String directoryUsername) throws Exception {
		return this.getRepositoryUserProperties(uidProp, directoryUsername);
	}

	
	public Map<QName, Serializable> getRepositoryUserProperties(String directoryUsernameProp, String directoryUsernameValue) throws Exception {
		Map<String, String> attMap = getAttributeMapping();
		String[] propsToReturn = attMap.keySet().toArray(new String[attMap.keySet().size()]);
		Map<String, String> userDirectoryRequestResult = getDirectoryUserProperties(directoryUsernameProp, directoryUsernameValue, propsToReturn);

		Map<QName, Serializable> result = new HashMap<>();

		for (String ldapProp : propsToReturn) {
			String repProp = attMap.get(ldapProp);

			String repValue = userDirectoryRequestResult.get(ldapProp);
			if (repValue != null) {
				result.put(QName.createQName(repProp), repValue);
			}
		}

		return result;

	}

	/**
	 * 
	 * @param directoryUsernameProp
	 * @param directoryUsernameValue
	 * @param propsToReturn
	 * @return ldapProps
	 * @throws Exception
	 */
	public Map<String, String> getDirectoryUserProperties(String directoryUsernameProp, String directoryUsernameValue, String[] propsToReturn) throws Exception {
		Properties env = new Properties();
		env.put(Context.INITIAL_CONTEXT_FACTORY, initialContextFacory);
		env.put(Context.PROVIDER_URL, providerUrl);
		env.put(Context.SECURITY_AUTHENTICATION, securityAuthentication);
		env.put(Context.SECURITY_PRINCIPAL, securityPrincipal);
		env.put(Context.SECURITY_CREDENTIALS, securityCredentials);

		DirContext ctx = new InitialDirContext(env);

		SearchControls ctls = new SearchControls();

		ctls.setSearchScope(SearchControls.SUBTREE_SCOPE);

		ctls.setReturningAttributes(propsToReturn);

		NamingEnumeration<SearchResult> rs = ctx.search("" // wo (leer = root)
				, "(" + directoryUsernameProp + "=" + directoryUsernameValue + ")" // query
				, ctls); //

		Map<String, String> result = new HashMap<>();

		while (rs.hasMore()) {

			SearchResult r = rs.next();
			for (String ldapAtt : propsToReturn) {

				logger.info("ldapAtt:" + ldapAtt);

				Attribute att = r.getAttributes().get(ldapAtt);
				if (att != null) {
					Object value = att.get();
					if (value != null) {
						String stringVal = null;
						if (value instanceof byte[]) {
							stringVal = new String((byte[]) value);
						} else if (value instanceof String) {
							stringVal = (String) value;
						} else {
							logger.error("unknown value Class " + value.getClass().getName() + " for " + ldapAtt);
						}

						if (stringVal != null) {
							logger.info(ldapAtt + " value:" + stringVal);
							// result.put(QName.createQName(attMap.get(ldapAtt)),
							// stringVal);
							result.put(ldapAtt, stringVal);
						}
					}
				} else {
					logger.info("att for " + ldapAtt + " is null");
				}

			}

		}
		return result;
	}

	Map<String, String> getAttributeMapping() {
		if (attributMapping == null) {
			attributMapping = new HashMap<>();
			attributMapping.put(firstNameProp, CCConstants.CM_PROP_PERSON_FIRSTNAME);
			attributMapping.put(lastNameProp, CCConstants.CM_PROP_PERSON_LASTNAME);
			attributMapping.put(emailProp, CCConstants.CM_PROP_PERSON_EMAIL);
			attributMapping.put(guidProp, CCConstants.CM_PROP_PERSON_GUID);
		}
		return attributMapping;
	}

	public String getInitialContextFacory() {
		return initialContextFacory;
	}

	public void setInitialContextFacory(String initialContextFacory) {
		this.initialContextFacory = initialContextFacory;
	}

	public String getProviderUrl() {
		return providerUrl;
	}

	public void setProviderUrl(String providerUrl) {
		this.providerUrl = providerUrl;
	}

	public String getSecurityAuthentication() {
		return securityAuthentication;
	}

	public void setSecurityAuthentication(String securityAuthentication) {
		this.securityAuthentication = securityAuthentication;
	}

	public String getSecurityPrincipal() {
		return securityPrincipal;
	}

	public void setSecurityPrincipal(String securityPrincipal) {
		this.securityPrincipal = securityPrincipal;
	}

	public String getSecurityCredentials() {
		return securityCredentials;
	}

	public void setSecurityCredentials(String securityCredentials) {
		this.securityCredentials = securityCredentials;
	}

	public String getFirstNameProp() {
		return firstNameProp;
	}

	public void setFirstNameProp(String firstNameProp) {
		this.firstNameProp = firstNameProp;
	}

	public String getLastNameProp() {
		return lastNameProp;
	}

	public void setLastNameProp(String lastNameProp) {
		this.lastNameProp = lastNameProp;
	}

	public String getEmailProp() {
		return emailProp;
	}

	public void setEmailProp(String emailProp) {
		this.emailProp = emailProp;
	}

	public String getGuidProp() {
		return guidProp;
	}

	public void setGuidProp(String guidProp) {
		this.guidProp = guidProp;
	}

	public String getUidProp() {
		return uidProp;
	}

	public void setUidProp(String uidProp) {
		this.uidProp = uidProp;
	}

}
