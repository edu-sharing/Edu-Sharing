package org.alfresco.repo.webdav.auth;

import java.io.IOException;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.UserTransaction;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.authentication.AuthenticationComponent;
import org.alfresco.repo.security.authentication.AuthenticationException;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.InvalidNodeRefException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.security.AuthenticationService;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.service.transaction.TransactionService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.springframework.context.ApplicationContext;

import org.htmlunit.BrowserVersion;
import org.htmlunit.WebClient;
import org.htmlunit.html.HtmlButton;
import org.htmlunit.html.HtmlForm;
import org.htmlunit.html.HtmlOption;
import org.htmlunit.html.HtmlPage;
import org.htmlunit.html.HtmlSelect;


/**
 * Servlet Filter for "Headless Shibboleth Auth"
 */
public class ShibbolethAuthenticationFilter implements Filter {

	private static final String RELEASE = "$Revision: 2980 $";

	private static final String WELCOME = "WebDAV Server";

	private static final String PARAM_DOMAIN = "domain";
	private static final String SEPAR_DOMAIN = "@";

	private static final Log logger = LogFactory.getLog(ShibbolethAuthenticationFilter.class);

	// Allow an authentication ticket to be passed as part of a request to bypass authentication

	private static final String ARG_TICKET = "ticket";
	private static final String PPT_EXTN = ".ppt";
	private static final String VTI_IGNORE = "&vtiIgnore";

	// Various services required by NTLM authenticator

	private AuthenticationService m_authService;
	private PersonService m_personService;
	private NodeService m_nodeService;
	private TransactionService m_transactionService;

	private AuthenticationComponent  m_authComp;

	private String protectedURL;
	private String successContent;
	private String defaultDomain;
	private String defaultSelector;
	private String redirectPath;


	/**
	 * Initialize the filter
	 *
	 * @param config FitlerConfig
	 * @exception ServletException
	 */
	@Override
	public void init(FilterConfig config) throws ServletException
	{
		ShibbolethAuthenticationFilter.logger.info(ShibbolethAuthenticationFilter.class + " (" + ShibbolethAuthenticationFilter.RELEASE + ")");

		// Save the context

		this.protectedURL = config.getInitParameter("protectedURL");
		this.successContent = config.getInitParameter("successContent");
		this.defaultDomain = config.getInitParameter("defaultDomain");
		this.defaultSelector = config.getInitParameter("defaultSelector");
		this.redirectPath = config.getInitParameter("redirectPath");

		// Setup the authentication context

		ApplicationContext context = AlfAppContextGate.getApplicationContext();
		ServiceRegistry serviceRegistry = (ServiceRegistry) context.getBean(ServiceRegistry.SERVICE_REGISTRY);

		this.m_nodeService = serviceRegistry.getNodeService();
		this.m_authService = serviceRegistry.getAuthenticationService();
		this.m_transactionService = serviceRegistry.getTransactionService();
		this.m_personService = serviceRegistry.getPersonService();

		this.m_authComp = (AuthenticationComponent) context.getBean("AuthenticationComponent");

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

		logger.debug("starting");

		HttpServletRequest httpReq = (HttpServletRequest) req;
		HttpServletResponse httpResp = (HttpServletResponse) resp;

		// Get the user details object from the session

		WebDAVUser user = (WebDAVUser) httpReq.getSession().getAttribute(BaseAuthenticationFilter.AUTHENTICATION_USER);

		if (user == null)
		{
			logger.debug("new websession");

			// Get the authorization header

			String authHdr = httpReq.getHeader("Authorization");
			if ( (authHdr != null) && (authHdr.length() > 5) && authHdr.substring(0,5).equalsIgnoreCase("BASIC"))
			{
				logger.debug("auth by shibboleth");

				// Basic authentication details present

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


				try {

					String[] usernameParts = username.split(SEPAR_DOMAIN);

					String localname =
							  (usernameParts.length == 2)
							? usernameParts[0]
							: username;

					logger.debug("localname = <"+ localname +">");

					String defaultDomain = httpReq.getParameter(PARAM_DOMAIN);

					String domain =
							 (usernameParts.length == 2)
							? usernameParts[1]
							:    (defaultDomain != null)
								? defaultDomain
								:    (this.defaultDomain != null)
									? this.defaultDomain
									: "";

					logger.debug("domain = <"+ domain +">");

					String scopedname = localname + (0 < domain.length() ? SEPAR_DOMAIN + domain : "");

					String proxyHost = System.getProperty("https.proxyHost");
					String proxyPort = System.getProperty("https.proxyPort");

					final WebClient webClient =
							  (proxyHost != null & proxyPort != null)
							? new WebClient(BrowserVersion.getDefault(), proxyHost, Integer.parseInt(proxyPort))
							: new WebClient();

					HtmlPage page =
							doAutoLogin(
									doAutoWAYF(
											(HtmlPage) webClient.getPage(this.protectedURL),
											domain,
											this.defaultSelector),
									localname,
									password);

					String content = page.asNormalizedText();
					page.cleanUp();

					webClient.close();

					if (content.contains(this.successContent)) {

						logger.debug("auth by shibboleth successful");

						UserTransaction tx = null;
						try
						{
							// Set security context

							this.m_authComp.setCurrentUser(scopedname);

							// Start a transaction

							tx = this.m_transactionService.getUserTransaction();
							tx.begin();

							NodeRef personRef = this.m_personService.getPerson(scopedname);

							NodeRef homeRef = (NodeRef) this.m_nodeService.getProperty(personRef, ContentModel.PROP_HOMEFOLDER);

							// Check that the home space node exists - else Login cannot proceed

							if (this.m_nodeService.exists(homeRef) == false) {
								throw new InvalidNodeRefException(homeRef);
							}

							user = new WebDAVUser( username, this.m_authService.getCurrentTicket(), homeRef);

							tx.commit();
							tx = null;

							httpReq.getSession().setAttribute(
									BaseAuthenticationFilter.AUTHENTICATION_USER,
									user);

						} catch (Throwable e) {

							// Clear the user object to signal authentication failure
							user = null;
							logger.error(e);

						} finally {

							try
							{
								if (tx != null) {
									tx.rollback();
								}

							} catch (Exception tex) {
								logger.error(tex);
							}

						}

					} else {

						logger.debug("auth by shibboleth failed");

					}

				} catch (Exception e) {
					logger.error(e);
				}

			}
			else
			{
				// Check if the request includes an authentication ticket

				String ticket = req.getParameter( ShibbolethAuthenticationFilter.ARG_TICKET);

				logger.debug("auth by ticket:"+ticket);

				if ( (ticket != null) &&  (ticket.length() > 0))
				{
					// PowerPoint bug fix
					if (ticket.endsWith(ShibbolethAuthenticationFilter.PPT_EXTN))
					{
						ticket = ticket.substring(0, ticket.length() - ShibbolethAuthenticationFilter.PPT_EXTN.length());
					}

					// vtiIgnore argument may find its way onto the ticket due to a double-encoding issue with Office
					if (ticket.endsWith(ShibbolethAuthenticationFilter.VTI_IGNORE))
					{
						ticket = ticket.substring(0, ticket.length() - ShibbolethAuthenticationFilter.VTI_IGNORE.length());
					}

					// Debug

					logger.debug(
							"Logon via ticket from " + req.getRemoteHost() + " (" + req.getRemoteAddr() + ":" + req.getRemotePort() + ")" +
							" ticket=" + ticket);

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

				httpResp.setHeader("WWW-Authenticate", "BASIC realm=\""+ ShibbolethAuthenticationFilter.WELCOME +"\"");
				httpResp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

				httpResp.flushBuffer();
				return;
			}
		}
		else
		{

			logger.debug("websession exists, ticket: "+user.getTicket());
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

				httpResp.setHeader("WWW-Authenticate", "BASIC realm=\""+ ShibbolethAuthenticationFilter.WELCOME +"\"");
				httpResp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

				httpResp.flushBuffer();
				return;
			}
		}

		// Chain other filters

		if (this.redirectPath != null) {

			req.getRequestDispatcher(this.redirectPath).forward(req, resp);

		} else {

			chain.doFilter(req, resp);

		}
	}

	private HtmlPage doAutoWAYF(HtmlPage page, String domain, String selector) {

		HtmlPage result = page;

		logger.debug(result.asXml());
		logger.debug("domain: " + domain);
		logger.debug("selector: " + selector);

		try {

			HtmlSelect select = page.getHtmlElementById("idpSelectSelector");

			boolean idpFound = false;
			for (HtmlOption option1 : select.getOptions()) {

				String idp = option1.getValueAttribute();

				boolean found = (selector != null) ? idp.equals(selector) : idp.contains(domain);

				if ((! idpFound) && found) {

					option1.setSelected(true);
					logger.debug("idp <"+ idp +"> selected");

					idpFound = true;

				} else {

					option1.setSelected(false);
					logger.debug("idp <"+ idp +">");
				}

			}

			if (idpFound) {

				result = page.getHtmlElementById("idpSelectListButton").click();

				page.cleanUp();

				logger.debug(result.asXml());
			}

		} catch (Exception e) {
			logger.error(e);
		}

		return result;
	}

	private HtmlPage doAutoLogin(HtmlPage page, String localname, String password) {

		HtmlPage result = page;

		try {

			final HtmlForm form2 = (HtmlForm) page.getByXPath("//form").get(0);

			form2.getInputByName("j_username").setValueAttribute(localname);
			form2.getInputByName("j_password").setValueAttribute(password);

			HtmlButton button2 =
					(HtmlButton) form2.getByXPath("//button[@type='submit']").get(0);

			result = button2.click();

			page.cleanUp();

			logger.debug(result.asXml());

		} catch (Exception e) {
			logger.error(e);
		}

		return result;
	}

	@Override
	public void destroy() {
		// TODO Auto-generated method stub

	}
}
