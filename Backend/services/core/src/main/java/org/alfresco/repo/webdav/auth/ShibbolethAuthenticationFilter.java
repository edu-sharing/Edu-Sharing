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

import lombok.extern.slf4j.Slf4j;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.authentication.AuthenticationComponent;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.InvalidNodeRefException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.security.AuthenticationService;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.service.transaction.TransactionService;
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
@Slf4j
public class ShibbolethAuthenticationFilter implements Filter {

    private static final String RELEASE = "$Revision: 2980 $";

    private static final String WELCOME = "WebDAV Server";

    private static final String PARAM_DOMAIN = "domain";
    private static final String SEPAR_DOMAIN = "@";


    // Allow an authentication ticket to be passed as part of a request to bypass authentication

    private static final String ARG_TICKET = "ticket";
    private static final String PPT_EXTN = ".ppt";
    private static final String VTI_IGNORE = "&vtiIgnore";

    // Various services required by NTLM authenticator

    private AuthenticationService m_authService;
    private PersonService m_personService;
    private NodeService m_nodeService;
    private TransactionService m_transactionService;

    private AuthenticationComponent m_authComp;

    private String protectedURL;
    private String successContent;
    private String defaultDomain;
    private String defaultSelector;
    private String redirectPath;


    /**
     * Initialize the filter
     *
     * @param config FitlerConfig
     */
    @Override
    public void init(FilterConfig config) throws ServletException {
        ShibbolethAuthenticationFilter.log.info("{} ( {} )", ShibbolethAuthenticationFilter.class, ShibbolethAuthenticationFilter.RELEASE);

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
     * @param req   ServletRequest
     * @param resp  ServletResponse
     * @param chain FilterChain
     */
    @Override
    public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain) throws IOException,
            ServletException {

        log.debug("starting");

        HttpServletRequest httpReq = (HttpServletRequest) req;
        HttpServletResponse httpResp = (HttpServletResponse) resp;

        // Get the user details object from the session

        WebDAVUser user = (WebDAVUser) httpReq.getSession().getAttribute(BaseAuthenticationFilter.AUTHENTICATION_USER);

        if (user == null) {
            log.debug("new websession");

            // Get the authorization header

            String authHdr = httpReq.getHeader("Authorization");
            if ((authHdr != null) && (authHdr.length() > 5) && authHdr.substring(0, 5).equalsIgnoreCase("BASIC")) {
                log.debug("auth by shibboleth");

                // Basic authentication details present

                String basicAuth = new String(java.util.Base64.getDecoder().decode(authHdr.substring(5).getBytes()));

                // Split the username and password

                String username;
                String password;

                int pos = basicAuth.indexOf(":");
                if (pos != -1) {
                    username = basicAuth.substring(0, pos);
                    password = basicAuth.substring(pos + 1);
                } else {
                    username = basicAuth;
                    password = "";
                }


                try {

                    String[] usernameParts = username.split(SEPAR_DOMAIN);
                    String localname = (usernameParts.length == 2)
                            ? usernameParts[0]
                            : username;

                    log.debug("localname = <{}>", localname);

                    String defaultDomain = httpReq.getParameter(PARAM_DOMAIN);

                    String domain = (usernameParts.length == 2)
                            ? usernameParts[1]
                            : (defaultDomain != null)
                            ? defaultDomain
                            : (this.defaultDomain != null)
                            ? this.defaultDomain
                            : "";

                    log.debug("domain = <{}>", domain);
                    String scopedname = localname + (!domain.isEmpty() ? SEPAR_DOMAIN + domain : "");

                    String proxyHost = System.getProperty("https.proxyHost");
                    String proxyPort = System.getProperty("https.proxyPort");

                    String content;
                    try (final WebClient webClient = (proxyHost != null & proxyPort != null)
                            ? new WebClient(BrowserVersion.getDefault(), proxyHost, Integer.parseInt(proxyPort))
                            : new WebClient()) {

                        HtmlPage page =
                                doAutoLogin(
                                        doAutoWAYF(
                                                webClient.getPage(this.protectedURL),
                                                domain,
                                                this.defaultSelector),
                                        localname,
                                        password);

                        content = page.asNormalizedText();
                        page.cleanUp();
                    }

                    if (content.contains(this.successContent)) {
                        log.debug("auth by shibboleth successful");

                        UserTransaction tx = null;
                        try {
                            // Set security context

                            this.m_authComp.setCurrentUser(scopedname);

                            // Start a transaction

                            tx = this.m_transactionService.getUserTransaction();
                            tx.begin();

                            NodeRef personRef = this.m_personService.getPerson(scopedname);

                            NodeRef homeRef = (NodeRef) this.m_nodeService.getProperty(personRef, ContentModel.PROP_HOMEFOLDER);

                            // Check that the home space node exists - else Login cannot proceed

                            if (!this.m_nodeService.exists(homeRef)) {
                                throw new InvalidNodeRefException(homeRef);
                            }

                            user = new WebDAVUser(username, this.m_authService.getCurrentTicket(), homeRef);

                            tx.commit();
                            tx = null;

                            httpReq.getSession().setAttribute(
                                    BaseAuthenticationFilter.AUTHENTICATION_USER,
                                    user);

                        } catch (Throwable e) {

                            // Clear the user object to signal authentication failure
                            user = null;
                            log.error(e.getMessage(), e);

                        } finally {
                            try {
                                if (tx != null) {
                                    tx.rollback();
                                }
                            } catch (Exception tex) {
                                log.error(tex.getMessage(), tex);
                            }
                        }

                    } else {
                        log.debug("auth by shibboleth failed");
                    }

                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }

            } else {
                // Check if the request includes an authentication ticket

                String ticket = req.getParameter(ShibbolethAuthenticationFilter.ARG_TICKET);

                log.debug("auth by ticket: {}", ticket);

                if ((ticket != null) && (!ticket.isEmpty())) {
                    // PowerPoint bug fix
                    if (ticket.endsWith(ShibbolethAuthenticationFilter.PPT_EXTN)) {
                        ticket = ticket.substring(0, ticket.length() - ShibbolethAuthenticationFilter.PPT_EXTN.length());
                    }

                    // vtiIgnore argument may find its way onto the ticket due to a double-encoding issue with Office
                    if (ticket.endsWith(ShibbolethAuthenticationFilter.VTI_IGNORE)) {
                        ticket = ticket.substring(0, ticket.length() - ShibbolethAuthenticationFilter.VTI_IGNORE.length());
                    }

                    // Debug

                    log.debug("Logon via ticket from {} ({}:{}) ticket={}", req.getRemoteHost(), req.getRemoteAddr(), req.getRemotePort(), ticket);

                    UserTransaction tx = null;
                    try {
                        // Validate the ticket

                        this.m_authService.validate(ticket);

                        // Need to create the User instance if not already available

                        String currentUsername = this.m_authService.getCurrentUserName();

                        // Start a transaction

                        tx = this.m_transactionService.getUserTransaction();
                        tx.begin();

                        NodeRef personRef = this.m_personService.getPerson(currentUsername);
                        user = new WebDAVUser(currentUsername, this.m_authService.getCurrentTicket(), personRef);
                        NodeRef homeRef = (NodeRef) this.m_nodeService.getProperty(personRef, ContentModel.PROP_HOMEFOLDER);

                        // Check that the home space node exists - else Login cannot proceed

                        if (!this.m_nodeService.exists(homeRef)) {
                            throw new InvalidNodeRefException(homeRef);
                        }
                        user.setHomeNode(homeRef);

                        tx.commit();
                        tx = null;

                        // Store the User object in the Session - the authentication servlet will then proceed

                        httpReq.getSession().setAttribute(BaseAuthenticationFilter.AUTHENTICATION_USER, user);
                    } catch (Throwable e) {
                        // Clear the user object to signal authentication failure
                        user = null;
                    } finally {
                        try {
                            if (tx != null) {
                                tx.rollback();
                            }
                        } catch (Exception ex) {
                            log.error(ex.getMessage(), ex);
                        }
                    }
                }
            }

            // Check if the user is authenticated, if not then prompt again

            if (user == null) {
                // No user/ticket, force the client to prompt for logon details

                httpResp.setHeader("WWW-Authenticate", "BASIC realm=\"" + ShibbolethAuthenticationFilter.WELCOME + "\"");
                httpResp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

                httpResp.flushBuffer();
                return;
            }
        } else {

            log.debug("websession exists, ticket: {}", user.getTicket());
            try {
                // Setup the authentication context
                this.m_authService.validate(user.getTicket());

                // Set the current locale

                // I18NUtil.setLocale(Application.getLanguage(httpRequest.getSession()));
            } catch (Exception ex) {
                // No user/ticket, force the client to prompt for logon details

                httpResp.setHeader("WWW-Authenticate", "BASIC realm=\"" + ShibbolethAuthenticationFilter.WELCOME + "\"");
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

        log.debug(result.asXml());
        log.debug("domain: {}", domain);
        log.debug("selector: {}", selector);

        try {

            HtmlSelect select = page.getHtmlElementById("idpSelectSelector");

            boolean idpFound = false;
            for (HtmlOption option1 : select.getOptions()) {

                String idp = option1.getValueAttribute();

                boolean found = (selector != null) ? idp.equals(selector) : idp.contains(domain);

                if ((!idpFound) && found) {

                    option1.setSelected(true);
                    log.debug("idp <{}> selected", idp);

                    idpFound = true;

                } else {

                    option1.setSelected(false);
                    log.debug("idp <{}>", idp);
                }

            }

            if (idpFound) {

                result = page.getHtmlElementById("idpSelectListButton").click();

                page.cleanUp();

                log.debug(result.asXml());
            }

        } catch (Exception e) {
            log.error(e.getMessage(), e);
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

            log.debug(result.asXml());

        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

        return result;
    }

    @Override
    public void destroy() {
        // TODO Auto-generated method stub

    }
}
