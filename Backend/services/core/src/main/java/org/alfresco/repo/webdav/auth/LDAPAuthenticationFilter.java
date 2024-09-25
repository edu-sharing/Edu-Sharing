package org.alfresco.repo.webdav.auth;

import com.typesafe.config.Config;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.UserTransaction;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.authentication.AuthenticationComponent;
import org.alfresco.repo.security.authentication.AuthenticationException;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.web.filter.beans.DependencyInjectedFilter;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.InvalidNodeRefException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.security.AuthenticationService;
import org.alfresco.service.cmr.security.NoSuchPersonException;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.transaction.TransactionService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.edu_sharing.alfresco.lightbend.LightbendConfigLoader;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.springframework.context.ApplicationContext;

import javax.naming.CommunicationException;
import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.*;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.util.*;


/**
 * Servlet Filter implementation class CockpitAuthenticationFilter
 */
/**
 * Servlet Filter implementation class CockpitAuthenticationFilter
 */
public class LDAPAuthenticationFilter implements Filter, DependencyInjectedFilter {

	private static final String CONTEXT = LDAPAuthenticationFilter.class.getCanonicalName();
	private static final String RELEASE = "$Revision: 1422 $";

	private static final String WELCOME = "WebDAV Server";

	// init params

	private static final String INIT_CONFIG_BASE = "repository.webdav.authentication.";

	private static final String INIT_LDAP_URI      = INIT_CONFIG_BASE + "ldap.uri";
	private static final String INIT_LDAP_BASE     = INIT_CONFIG_BASE + "ldap.base";
	private static final String INIT_LDAP_SEC_AUTH = INIT_CONFIG_BASE +"ldap.sec.auth";
	private static final String INIT_LDAP_SEC_USER = INIT_CONFIG_BASE +"ldap.sec.user";
	private static final String INIT_LDAP_SEC_PWD  = INIT_CONFIG_BASE + "ldap.sec.pwd";

	private static final String INIT_LDAP_FROM     = INIT_CONFIG_BASE +"ldap.from";
	private static final String INIT_LDAP_TO       = INIT_CONFIG_BASE + "ldap.to";
	private static final String INIT_LDAP_UID 	   = INIT_CONFIG_BASE + "ldap.uid";

	/**
	 * edu-sharing customization
	 */
	private static final String INIT_USE_ALFRESCO_AUTHENTICATION_COMPONENT = INIT_CONFIG_BASE + "ldap.alfrescoAuthComponent";


	// Allow an authentication ticket to be passed as part of a request to bypass authentication

	private static final String ARG_TICKET = "ticket";
	private static final String PPT_EXTN = ".ppt";
	private static final String VTI_IGNORE = "&vtiIgnore";


	private static final Log logger = LogFactory.getLog(LDAPAuthenticationFilter.CONTEXT);

	Config eduConfig = LightbendConfigLoader.get();

	// Servlet context

	private ServletContext m_context;

	// Various services required by NTLM authenticator

	private AuthenticationService m_authService;
	private PersonService m_personService;
	private NodeService m_nodeService;
	private TransactionService m_transactionService;

	private DirContext jndi;

	private String ldapFrom;
	private String ldapTo;

	private boolean useAlfrescoAuthenticationComponent = false;
	private String ldapBase = null;

	//rember the env global
	private Properties env = null;
	private String ldapUidProp = null;
	private String ldapUrl = null;


	/**
	 * edu-sharing fix from 4.2.f
	 *
	 *
	 * ALF-13621: Due to browser inconsistencies we have to try a fallback path of encodings
	 */
	/** The password encodings to try in priority order **/
	private static final String[] ENCODINGS = new String[] {
			"UTF-8",
			System.getProperty("file.encoding"),
			"ISO-8859-1"
	};

	/** Corresponding array of CharsetDecoders with CodingErrorAction.REPORT. Duplicates removed. */
	private static final CharsetDecoder[] DECODERS;

	static
	{
        Map<String, CharsetDecoder> decoders = new LinkedHashMap<>(ENCODINGS.length * 2);
		for (String encoding : ENCODINGS)
		{
			if (!decoders.containsKey(encoding))
			{
				decoders.put(encoding, Charset.forName(encoding).newDecoder()
						.onMalformedInput(CodingErrorAction.REPORT));
			}
		}
		DECODERS = new CharsetDecoder[decoders.size()];
		decoders.values().toArray(DECODERS);
	}


	/**
	 * Initialize the filter
	 *
	 * @param config FitlerConfig
	 * @exception ServletException
	 */
	@Override
	public void init(FilterConfig config) throws ServletException
	{
		LDAPAuthenticationFilter.logger.info(LDAPAuthenticationFilter.CONTEXT + " (" + LDAPAuthenticationFilter.RELEASE + ")");

		// Save the context

		this.m_context = config.getServletContext();

		// Setup the authentication context

		//WebApplicationContext ctx = WebApplicationContextUtils.getRequiredWebApplicationContext(this.m_context);

		ApplicationContext context = AlfAppContextGate.getApplicationContext();
		ServiceRegistry serviceRegistry = (ServiceRegistry) context.getBean(ServiceRegistry.SERVICE_REGISTRY);

		//ServiceRegistry serviceRegistry = (ServiceRegistry) ctx.getBean(ServiceRegistry.SERVICE_REGISTRY);
		this.m_nodeService = serviceRegistry.getNodeService();
		this.m_authService = serviceRegistry.getAuthenticationService();
		this.m_transactionService = serviceRegistry.getTransactionService();
		//this.m_personService = (PersonService) ctx.getBean("PersonService");   // transactional and permission-checked
		this.m_personService = (PersonService) context.getBean("PersonService");   // transactional and permission-checked


		this.ldapBase = eduConfig.getString(LDAPAuthenticationFilter.INIT_LDAP_BASE);
		this.ldapUrl = eduConfig.getString(LDAPAuthenticationFilter.INIT_LDAP_URI);
		this.ldapFrom = eduConfig.getString(LDAPAuthenticationFilter.INIT_LDAP_FROM);
		this.ldapTo = eduConfig.getString(LDAPAuthenticationFilter.INIT_LDAP_TO);
		this.ldapUidProp = eduConfig.getString(LDAPAuthenticationFilter.INIT_LDAP_UID);
		if(this.ldapUidProp == null || this.ldapUidProp.trim().equals("")) this.ldapUidProp = "uid";

		env = new Properties();
		env.put(Context.INITIAL_CONTEXT_FACTORY,
				"com.sun.jndi.ldap.LdapCtxFactory");
		//edu-sharing
		useAlfrescoAuthenticationComponent = eduConfig.getBoolean(LDAPAuthenticationFilter.INIT_USE_ALFRESCO_AUTHENTICATION_COMPONENT);
		env.put(Context.PROVIDER_URL, this.ldapUrl + "/" + this.ldapBase);
		env.put(Context.SECURITY_AUTHENTICATION, eduConfig.getString(LDAPAuthenticationFilter.INIT_LDAP_SEC_AUTH));
		env.put(Context.SECURITY_PRINCIPAL, eduConfig.getString(LDAPAuthenticationFilter.INIT_LDAP_SEC_USER));
		env.put(Context.SECURITY_CREDENTIALS, eduConfig.getString(LDAPAuthenticationFilter.INIT_LDAP_SEC_PWD));

	}

	/**
	 * @see Filter#destroy()
	 */
	@Override
	public void destroy() {

		if (this.jndi != null) {

			try {

				this.jndi.close();

			} catch (NamingException e) {}

		}
	}

	@Override
	public void doFilter(ServletContext servletContext, ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
		doFilter(servletRequest, servletResponse, filterChain);
	}
	/**
	 * Run the authentication filter
	 *
	 * @param req ServletRequest
	 * @param resp ServletResponse
	 * @param chain FilterChain
	 * @exception ServletException
	 * @exception IOException
	 */
	@Override
	public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain) throws IOException,
			ServletException
	{
		if(this.jndi == null){
			try {

				this.jndi = new InitialDirContext(env);

			} catch (NamingException e) {
				logger.error(e.getMessage(), e);
				throw new ServletException(e);
			}
		}

		// Assume it's an HTTP request
		HttpServletRequest httpReq = (HttpServletRequest) req;
		HttpServletResponse httpResp = (HttpServletResponse) resp;

		// Get the user details object from the session

		WebDAVUser user = (WebDAVUser) httpReq.getSession().getAttribute(BaseAuthenticationFilter.AUTHENTICATION_USER);

		if (user == null)
		{
			// Get the authorization header


			String authHdr = httpReq.getHeader("Authorization");
			logger.debug("user == null authHdr:"+authHdr);
			if ( (authHdr != null) && (authHdr.length() > 5) && authHdr.substring(0,5).equalsIgnoreCase("BASIC"))
			{
				// Basic authentication details present
/*
				String basicAuth = new String(java.util.Base64.getDecoder().decode(authHdr.substring(5).getBytes()));

				// Split the username and password

				String username = null;
				String password = null;

				int pos = basicAuth.indexOf(":");
				if ( pos != -1)
				{
					username = basicAuth.substring(0, pos);
					password = basicAuth.substring(pos + 1);
				}
				else
				{
					username = basicAuth;
					password = "";
				}

				try{
					user = searchForUser(username,password);
				}catch(CommunicationException e){
					logger.error(e.getMessage() +" Will create new InitialDirContext and retry.");
					try{
						this.jndi = new InitialDirContext(env);
						user = searchForUser(username,password);
					}catch(CommunicationException ce){

						logger.error(e.getMessage() + " still occurs will give up. maybe restart alfresco.");

					}catch (NamingException ne) {
						logger.error(ne.getMessage(), ne);
					}
				}

				if(user != null){
					httpReq.getSession().setAttribute(BaseAuthenticationFilter.AUTHENTICATION_USER, user);
				}


*/

				/**
				 * edu-sharing fix
				 */
				if (logger.isDebugEnabled())
					logger.debug("Basic authentication details present in the header.");
				byte[] encodedString = java.util.Base64.getDecoder().decode(authHdr.substring(5).trim().getBytes());

				// ALF-13621: Due to browser inconsistencies we have to try a fallback path of encodings
				Set<String> attemptedAuths = new HashSet<String>(DECODERS.length * 2);
				for (CharsetDecoder decoder : DECODERS)
				{
					try
					{
						// Attempt to decode using this charset
						String basicAuth = decoder.decode(ByteBuffer.wrap(encodedString)).toString();



						// It decoded OK but we may already have tried this string.
						if (!attemptedAuths.add(basicAuth))
						{
							// Already tried - no need to try again
							continue;
						}


						String username = null;
						String password = null;


						// Split the username and password
						int pos = basicAuth.indexOf(":");
						if (pos != -1)
						{
							username = basicAuth.substring(0, pos);
							password = basicAuth.substring(pos + 1);
						}
						else
						{
							username = basicAuth;
							password = "";
						}

						// Authenticate the user
						try{
							logger.info("webdav ldap authentication: starting. loginName:####");
							user = searchForUser(username,password);
						}catch(CommunicationException e){
							logger.error(e.getMessage() +" Will create new InitialDirContext and retry.");
							try{
								this.jndi = new InitialDirContext(env);
								user = searchForUser(username,password);
							}catch(CommunicationException ce){
								logger.error(e.getMessage() + " still occurs will give up. maybe restart alfresco.");
							}catch (NamingException ne) {
								logger.error(ne.getMessage(), ne);
							}
						}catch(NoSuchPersonException e){
							logger.error("person does not exist in alfresco");
						}

						if(user != null){
							httpReq.getSession().setAttribute(BaseAuthenticationFilter.AUTHENTICATION_USER, user);
							// Success so break out
							break;
						}




					}
					catch (CharacterCodingException e)
					{
						if (logger.isDebugEnabled())
							logger.debug("Didn't decode using " + decoder.getClass().getName(), e);
					}
					catch (AuthenticationException ex)
					{
						if (logger.isDebugEnabled())
							logger.debug("Authentication error ", ex);
					}
					catch (NoSuchPersonException e)
					{
						if (logger.isDebugEnabled())
							logger.debug("There is no such person error ", e);
					}
				}




			}
			else
			{
				// Check if the request includes an authentication ticket

				String ticket = req.getParameter( LDAPAuthenticationFilter.ARG_TICKET);

				logger.debug("auth by ticket:"+ticket);

				if ( (ticket != null) &&  (ticket.length() > 0))
				{
					// PowerPoint bug fix
					if (ticket.endsWith(LDAPAuthenticationFilter.PPT_EXTN))
					{
						ticket = ticket.substring(0, ticket.length() - LDAPAuthenticationFilter.PPT_EXTN.length());
					}

					// vtiIgnore argument may find its way onto the ticket due to a double-encoding issue with Office
					if (ticket.endsWith(LDAPAuthenticationFilter.VTI_IGNORE))
					{
						ticket = ticket.substring(0, ticket.length() - LDAPAuthenticationFilter.VTI_IGNORE.length());
					}

					// Debug

					if ( LDAPAuthenticationFilter.logger.isDebugEnabled()) {
						LDAPAuthenticationFilter.logger.debug("Logon via ticket from " + req.getRemoteHost() + " (" +
								req.getRemoteAddr() + ":" + req.getRemotePort() + ")" + " ticket=" + ticket);
					}

					UserTransaction tx = null;
					try
					{
						// Validate the ticket

						this.m_authService.validate(ticket);

						// Need to create the User instance if not already available

						String currentUsername = this.m_authService.getCurrentUserName();

						// Start a transaction

						tx = this.m_transactionService.getUserTransaction();
						tx.begin();

						NodeRef personRef = this.m_personService.getPerson(currentUsername);
						user = new WebDAVUser( currentUsername, this.m_authService.getCurrentTicket(), personRef);
						NodeRef homeRef = (NodeRef) this.m_nodeService.getProperty(personRef, ContentModel.PROP_HOMEFOLDER);

						// Check that the home space node exists - else Login cannot proceed

						if (this.m_nodeService.exists(homeRef) == false)
						{
							throw new InvalidNodeRefException(homeRef);
						}
						user.setHomeNode(homeRef);

						tx.commit();
						tx = null;

						// Store the User object in the Session - the authentication servlet will then proceed

						httpReq.getSession().setAttribute(BaseAuthenticationFilter.AUTHENTICATION_USER, user);
					}
					catch (AuthenticationException authErr)
					{
						// Clear the user object to signal authentication failure

						user = null;
					}
					catch (Throwable e)
					{
						// Clear the user object to signal authentication failure

						user = null;
					}
					finally
					{
						try
						{
							if (tx != null)
							{
								tx.rollback();
							}
						}
						catch (Exception tex)
						{
						}
					}
				}
			}

			// Check if the user is authenticated, if not then prompt again

			if ( user == null)
			{
				// No user/ticket, force the client to prompt for logon details

				httpResp.setHeader("WWW-Authenticate", "BASIC realm=\""+ LDAPAuthenticationFilter.WELCOME +"\"");
				httpResp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

				httpResp.flushBuffer();
				return;
			}
		}
		else
		{

			logger.debug("user != null :"+user.getTicket());
			try
			{
				// Setup the authentication context
				this.m_authService.validate(user.getTicket());

				// Set the current locale

				// I18NUtil.setLocale(Application.getLanguage(httpRequest.getSession()));
			}
			catch (Exception ex)
			{
				// No user/ticket, force the client to prompt for logon details

				httpResp.setHeader("WWW-Authenticate", "BASIC realm=\""+ LDAPAuthenticationFilter.WELCOME +"\"");
				httpResp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

				httpResp.flushBuffer();
				return;
			}
		}

		// Chain other filters

		chain.doFilter(req, resp);
	}

	/**
	 * search for user encapsulated in a method to catch a potential CommunicationException and retry
	 *
	 *https://issues.apache.org/jira/browse/HADOOP-9125
	 *https://issues.apache.org/jira/secure/attachment/12560771/HADOOP-9125.patch
	 */
	WebDAVUser searchForUser(String loginName, String password) throws CommunicationException{


		String uid = null;

		String dn = null;

		String username = loginName;

		try
		{
			SearchControls ctls = new SearchControls();

			ctls.setSearchScope(SearchControls.ONELEVEL_SCOPE);
			ctls.setReturningAttributes(new String[] {this.ldapTo,this.ldapUidProp});

			String base = "";
			String query = "(" +this.ldapFrom +"="+ username + ")";
			logger.debug("query:"+query);
			NamingEnumeration<SearchResult> rs;

			rs = this.jndi.search(base, query, ctls);

			if (rs.hasMore()) {

				SearchResult r = rs.next();

				Attribute attr = r.getAttributes().get(this.ldapTo);

				if (attr != null) {
					username = (String) attr.get();
				}

				Attribute uidAttr = r.getAttributes().get(this.ldapUidProp);
				if(uidAttr != null){
					uid = (String) uidAttr.get();
				}

				dn = r.getNameInNamespace();

			}else{
				throw new AuthenticationException("webdav ldap authentication: user not found in directory. loginName:###");
			}
			rs.close();

			logger.debug("query:"+query + " new username:"+username);
			//edu-sharing customization
			if(username != null){
				final String fusername = username;
				boolean allowed = AuthenticationUtil.runAsSystem(() -> {
					NodeRef personRef = this.m_personService.getPerson(fusername, false);
					if(!LightbendConfigLoader.get().getIsNull("repository.personActiveStatus")) {
						String personActiveStatus = LightbendConfigLoader.get().getString("repository.personActiveStatus");
						String personStatus = (String)this.m_nodeService.getProperty(personRef, QName.createQName(CCConstants.CM_PROP_PERSON_ESPERSONSTATUS));
						if(!personActiveStatus.equals(personStatus)){
							logger.info("personActiveStatus mismatch. " + personStatus + " vs " + personActiveStatus );
							return false;
						}
					}
					return true;
				});
				if(!allowed){
					throw new AuthenticationException("webdav ldap authentication: USER_BLOCKED. loginName: ###  / userName: ###");
				}
			}

			if(useAlfrescoAuthenticationComponent){
				this.m_authService.authenticate(username, password.toCharArray());
			}else{

				logger.debug("using ldap auth dn:" + dn + " uid:" +uid +" username:" +username);
				this.authenticate(dn, username, password, loginName);
			}

			// Set the user name as stored by the back end
			username = this.m_authService.getCurrentUserName();

			// Get the user node and home folder

			NodeRef personNodeRef = this.m_personService.getPerson(username);
			NodeRef homeSpaceRef = (NodeRef) this.m_nodeService.getProperty(personNodeRef, ContentModel.PROP_HOMEFOLDER);

			// Setup User object and Home space ID etc.

			return new WebDAVUser(username, this.m_authService.getCurrentTicket(), homeSpaceRef);



		}catch(CommunicationException ce){
			throw ce;
		} catch (NamingException e) {
			// Do nothing, user object will be null
			logger.error(e.getMessage(),e);
		} catch (AuthenticationException ex) {
			// Do nothing, user object will be null
			if(ex.getMessage() != null && ex.getMessage().contains("Invalid Credentials")){
				logger.warn("webdav ldap authentication: failed with Invalid Credentials. loginName: ### / userName: ###");
			}
			if (ex.getMessage() != null && ex.getMessage().contains("DN with no password")) {
				logger.warn("webdav ldap authentication: no password provided. loginName: ### / userName: ###");
			}else {
				logger.warn(ex.getMsgId());
				if (logger.isDebugEnabled()) {
					logger.error(ex.getMessage(), ex);
				}
			}
		}

		return null;
	}

	/**
	 * edu-sharing customization: try to authenticate at ldap directly
	 *
	 * @param username
	 * @param password
	 * @param loginName
	 * @throws AuthenticationException
	 */
	private void authenticate(String ldapUserDn, String username, String password, String loginName) throws  AuthenticationException{

		if(env != null){
			Properties authEnv = new Properties();
			authEnv.put(Context.INITIAL_CONTEXT_FACTORY,
					"com.sun.jndi.ldap.LdapCtxFactory");
			//authEnv.put(Context.PROVIDER_URL, env.get(Context.PROVIDER_URL));
			authEnv.put(Context.PROVIDER_URL,this.ldapUrl);
			//authEnv.put(Context.SECURITY_PRINCIPAL,"uid="+ldapUid);
			//authEnv.put(Context.SECURITY_PRINCIPAL,"uid="+ldapUid+","+this.ldapBase);
			authEnv.put(Context.SECURITY_PRINCIPAL,ldapUserDn);

			authEnv.put(Context.SECURITY_AUTHENTICATION,env.get(Context.SECURITY_AUTHENTICATION));
			authEnv.put(Context.SECURITY_CREDENTIALS,password);


			try {
				new InitialDirContext(authEnv);
				ApplicationContext context = AlfAppContextGate.getApplicationContext();
				AuthenticationComponent authComp = (AuthenticationComponent)context.getBean("authenticationComponent");
				authComp.setCurrentUser(username);
				logger.info("webdav ldap authentication: sucessfull. loginName: ###  / userName: ###");
				return;
			}catch(javax.naming.AuthenticationException e){
				logger.debug(e.getMessage(), e);
				throw new AuthenticationException(e.getMessage());
			} catch (NamingException e) {
				logger.debug(e.getMessage(), e);
				throw new AuthenticationException(e.getMessage());
			}

		}
		throw new AuthenticationException("LDAPAuthenticationFilter env seems to be null");
	}

}
