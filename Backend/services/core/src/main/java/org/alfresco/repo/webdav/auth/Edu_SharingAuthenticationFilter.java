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
import jakarta.servlet.http.HttpSession;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.SessionUser;
import org.alfresco.repo.security.authentication.AuthenticationException;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.security.AuthenticationService;
import org.alfresco.service.cmr.security.NoSuchPersonException;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.service.transaction.TransactionService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.springframework.context.ApplicationContext;

public class Edu_SharingAuthenticationFilter implements Filter {

    private static final Log logger = LogFactory.getLog(Edu_SharingAuthenticationFilter.class);

    private AuthenticationService authenticationService;

    private TransactionService transactionService;

    private PersonService personService;

    private NodeService nodeService;

    /**
     * The default session attribute used to cache the user. Subclasses may
     * override this with {@link #setUserAttributeName(String)}.
     */
    public static final String AUTHENTICATION_USER = "_alfDAVAuthTicket";

    /**
     * The configured user attribute name.
     */
    public static String userAttributeName = AUTHENTICATION_USER;

    /**
     * The session attribute that indicates external authentication.
     */
    private static final String LOGIN_EXTERNAL_AUTH = "_alfExternalAuth";

    private static final String WELCOME = "WebDAV Server";

    @Override
    public void destroy() {
        logger.info("shutdown");

    }


    @Override
    public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain) throws IOException, ServletException {

        HttpServletRequest httpReq = (HttpServletRequest) req;


        HttpServletResponse httpResp = (HttpServletResponse) resp;

        SessionUser user = (SessionUser) httpReq.getSession().getAttribute(getUserAttributeName());

        if (user == null) {

            String authHdr = httpReq.getHeader("Authorization");


            if (authHdr != null && authHdr.length() > 5 && authHdr.substring(0, 5).equalsIgnoreCase("BASIC")) {

                logger.info("auth is BASIC");
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
                    // Authenticate the user


                    authenticationService.authenticate(username, password.toCharArray());

                    user = this.createUserEnvironment(httpReq.getSession(), authenticationService.getCurrentUserName(), authenticationService.getCurrentTicket(), false);

                } catch (AuthenticationException | NoSuchPersonException ex) {
                    // Do nothing, user object will be null
                    logger.error(ex.getMessage(), ex);
                }

            } else {
                // Check if the request includes an authentication ticket

                String ticket = req.getParameter("ticket");
                if (ticket != null && !ticket.isEmpty()) {
                    // PowerPoint bug fix
                    if (ticket.endsWith(".ppt")) {
                        ticket = ticket.substring(0, ticket.length() - ".ppt".length());
                    }

                    // Debug

                    if (logger.isDebugEnabled())
                        logger.debug("Logon via ticket from " + req.getRemoteHost() + " (" + req.getRemoteAddr() + ":"
                                + req.getRemotePort() + ")" + " ticket=" + ticket);

                    // Validate the ticket

                    authenticationService.validate(ticket);

                    // Need to create the User instance if not already available

                    String currentUsername = authenticationService.getCurrentUserName();

                    user = createUserEnvironment(httpReq.getSession(), currentUsername, ticket, false);
                }
            }

            if (user == null) {
                // No user/ticket, force the client to prompt for logon details
                //httpResp.setHeader("WWW-Authenticate", "BASIC realm=\"Alfresco DAV Server\"");
                httpResp.setHeader("WWW-Authenticate", "BASIC realm=\"Edu-Sharing DAV Server\"");
                httpResp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                httpResp.flushBuffer();
                return;
            }
        } else {
            logger.debug("user != null :" + user.getTicket() + " uh:" + ((WebDAVUser) user).getHomeNode());
            try {
                // Setup the authentication context
                authenticationService.validate(user.getTicket());

                // Set the current locale

                // I18NUtil.setLocale(Application.getLanguage(httpRequest.getSession()));
            } catch (Exception ex) {
                // No user/ticket, force the client to prompt for logon details

                httpResp.setHeader("WWW-Authenticate", "BASIC realm=\"" + WELCOME + "\"");
                httpResp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

                httpResp.flushBuffer();
                return;
            }
        }

        // Chain other filters
        chain.doFilter(req, resp);

    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        logger.info("startup");

        ApplicationContext context = AlfAppContextGate.getApplicationContext();
        ServiceRegistry serviceRegistry = (ServiceRegistry) context.getBean(ServiceRegistry.SERVICE_REGISTRY);
        authenticationService = serviceRegistry.getAuthenticationService();
        transactionService = serviceRegistry.getTransactionService();
        personService = serviceRegistry.getPersonService();
        nodeService = serviceRegistry.getNodeService();

    }

    protected SessionUser createUserEnvironment(HttpSession session, final String userName, final String ticket, boolean externalAuth) {
        SessionUser user = doInSystemTransaction(() -> {
            // Setup User object and Home space ID etc.
            final NodeRef personNodeRef = personService.getPerson(userName);

            String name = (String) nodeService.getProperty(personNodeRef, ContentModel.PROP_USERNAME);

            NodeRef homeSpaceRef = (NodeRef) nodeService.getProperty(personNodeRef, ContentModel.PROP_HOMEFOLDER);

            return createUserObject(name, ticket, personNodeRef, homeSpaceRef);
        });

        // Store the user on the session
        session.setAttribute(getUserAttributeName(), user);
        setExternalAuth(session, externalAuth);
        return user;
    }

    protected SessionUser createUserObject(String userName, String ticket, NodeRef personNode, NodeRef homeSpaceRef) {
        return new WebDAVUser(userName, ticket, homeSpaceRef);
    }

    /**
     * Executes a callback in a transaction as the system user
     *
     * @param callback the callback
     * @return the return value from the callback
     */
    protected <T> T doInSystemTransaction(final RetryingTransactionHelper.RetryingTransactionCallback<T> callback) {
        return AuthenticationUtil.runAs(() -> transactionService.getRetryingTransactionHelper().doInTransaction(callback), AuthenticationUtil.SYSTEM_USER_NAME);
    }

    /**
     * Return the user object session attribute name.
     *
     * @return the user object session attribute name
     */
    protected final String getUserAttributeName() {
        return userAttributeName;
    }

    private void setExternalAuth(HttpSession session, boolean externalAuth) {
        if (externalAuth) {
            session.setAttribute(LOGIN_EXTERNAL_AUTH, Boolean.TRUE);
        } else {
            session.removeAttribute(LOGIN_EXTERNAL_AUTH);
        }
    }

}
