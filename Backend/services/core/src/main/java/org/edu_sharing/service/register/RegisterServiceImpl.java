package org.edu_sharing.service.register;

import net.sf.acegisecurity.AuthenticationCredentialsNotFoundException;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.cache.SimpleCache;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.security.MutableAuthenticationService;
import org.alfresco.service.cmr.security.NoSuchPersonException;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.service.namespace.QName;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.log4j.Logger;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.AuthenticationTool;
import org.edu_sharing.repository.server.AuthenticationToolAPI;
import org.edu_sharing.alfresco.repository.server.authentication.Context;
import org.edu_sharing.repository.server.tools.Mail;
import org.edu_sharing.repository.server.tools.mailtemplates.MailTemplate;
import org.edu_sharing.restservices.register.v1.model.RegisterInformation;
import org.edu_sharing.service.authority.AuthorityServiceHelper;
import org.springframework.context.ApplicationContext;

import jakarta.servlet.ServletContext;
import java.io.Serializable;
import java.net.URLEncoder;
import java.security.InvalidKeyException;
import java.util.HashMap;
import java.util.Map;

import org.edu_sharing.repository.tools.URLHelper;

public class RegisterServiceImpl implements RegisterService {
    static Logger logger = Logger.getLogger(RegisterServiceImpl.class);
    private static int KEY_LENGTH = 16;
    public final static SimpleCache<String,RegisterInformation> registerUserCache = (SimpleCache)  AlfAppContextGate.getApplicationContext().getBean("eduSharingRegisterUserCache");
    public final static SimpleCache<String,RegisterInformation> recoverPasswordCache = (SimpleCache)  AlfAppContextGate.getApplicationContext().getBean("eduSharingRecoverPasswordCache");

    private final PersonService personService;
    private final NodeService nodeService;
    private final MutableAuthenticationService authService;

    public RegisterServiceImpl() {
        ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();
        ServiceRegistry serviceRegistry = (ServiceRegistry) applicationContext
                .getBean(ServiceRegistry.SERVICE_REGISTRY);

        personService = serviceRegistry.getPersonService();
        nodeService = serviceRegistry.getNodeService();
        authService = serviceRegistry.getAuthenticationService();
    }
    protected void sendRegisterMail(RegisterInformation info, String key) throws Exception {
        String currentLocale = new AuthenticationToolAPI().getCurrentLocale();

        String subject=MailTemplate.getSubject("userRegister", currentLocale);
        String content=MailTemplate.getContent("userRegister", currentLocale,true);
        Map<String,String> replace=new HashMap<>();
        replace.put("link", URLHelper.getNgComponentsUrl()+"register/done/"+URLEncoder.encode(key)+"/"+ URLEncoder.encode(info.getEmail()));
        replace.put("key",key);
        addMailRegisterInfo(info, replace);
        Mail mail=new Mail();
        ServletContext context = Context.getCurrentInstance().getRequest().getSession().getServletContext();
        mail.sendMailHtml(
                context,
                info.getEmail(),
                subject,content,replace);

    }

    private void addMailRegisterInfo(RegisterInformation info, Map<String, String> replace) {
        replace.put("firstName",info.getFirstName());
        replace.put("lastName",info.getLastName());
        replace.put("email",info.getEmail());
        replace.put("organization",info.getOrganization());
    }

    public boolean userExists(String id) throws Exception {
        return personService.personExists(id);
    }
    public boolean userExists(RegisterInformation info) throws Exception {
        return userExists(info.getEmail());
    }
    @Override
    public void resetPassword(String key, String newPassword) throws Exception {
        RegisterInformation info = recoverPasswordCache.get(key);
        if(info==null)
            throw new InvalidKeyException();
        AuthenticationUtil.runAsSystem(()-> {
            setPassword(info,newPassword);
            info.setPassword(newPassword);
            recoverPasswordCache.remove(key);
            authenticate(info);
            return null;
        });
    }

    protected void setPassword(RegisterInformation info, String newPassword) throws Exception{
        authService.setAuthentication(info.getAuthorityName(), newPassword.toCharArray());
    }

    @Override
    public boolean recoverPassword(String id) throws Exception {
        return AuthenticationUtil.runAsSystem(()-> {
            try {
                RegisterInformation info = getPersonById(id);
                if(AuthorityServiceHelper.isAdmin(info.getAuthorityName())) {
                    throw new SecurityException("Recovering passwords is forbidden for admin users");
                }
                String key = addToCacheNoDuplicate(info,recoverPasswordCache,false);

                String currentLocale = new AuthenticationToolAPI().getCurrentLocale();
                String subject = MailTemplate.getSubject("userRecoverPassword", currentLocale);
                String content = MailTemplate.getContent("userRecoverPassword", currentLocale, true);
                Map<String, String> replace = new HashMap<>();
                replace.put("link", URLHelper.getNgComponentsUrl() + "register/reset-password/" + URLEncoder.encode(key));
                replace.put("link.static", URLHelper.getNgComponentsUrl(false) + "register/reset-password/" + URLEncoder.encode(key));
                replace.put("key", key);
                addMailRegisterInfo(info, replace);
                Mail mail = new Mail();
                ServletContext context = Context.getCurrentInstance().getRequest().getSession().getServletContext();
                mail.sendMailHtml(
                        context,
                        info.getEmail(),
                        subject, content, replace);
                return true;
            } catch (NoSuchPersonException|AuthenticationCredentialsNotFoundException e) {
                return false;
            }
        });
    }

    private RegisterInformation getPersonById(String id) {
        NodeRef ref = personService.getPerson(id,false);
        Map<QName, Serializable> props = nodeService.getProperties(ref);
        RegisterInformation info = new RegisterInformation();
        info.setAuthorityName((String) props.get(ContentModel.PROP_USERNAME));
        info.setFirstName((String) props.get(ContentModel.PROP_FIRSTNAME));
        info.setLastName((String) props.get(ContentModel.PROP_LASTNAME));
        info.setEmail((String) props.get(ContentModel.PROP_EMAIL));
        info.setOrganization((String) props.get(ContentModel.PROP_ORGANIZATION));
        if(props.containsKey(QName.createQName(CCConstants.CM_PROP_PERSON_ALLOW_NOTIFICATIONS)))
            info.setAllowNotifications((Boolean) props.get(QName.createQName(CCConstants.CM_PROP_PERSON_ALLOW_NOTIFICATIONS)));
        return info;
    }

    @Override
    public String activate(String key) throws Throwable {
        RegisterInformation info = registerUserCache.get(key);
        if(info==null)
            throw new InvalidKeyException();
        String result = storeUser(info);
        registerUserCache.remove(key);
        // authenticate the newly activated user automatically
        authenticate(info);

        try{
            Map<String, String> replace = new HashMap<>();
            replace.put("link", URLHelper.getNgComponentsUrl()+"permissions");
            addMailRegisterInfo(info, replace);
            Mail mail = new Mail();
            ServletContext context = Context.getCurrentInstance().getRequest().getSession().getServletContext();
            String currentLocale = new AuthenticationToolAPI().getPrimaryLocale();
            String subject = MailTemplate.getSubject("userRegisterInformation", currentLocale);
            String content = MailTemplate.getContent("userRegisterInformation", currentLocale, true);
            String receiver=mail.getConfig().getString("register.receiver");
            mail.sendMailHtml(
                    context,
                    receiver,
                    subject,content, replace);
        }catch(Throwable t){
            logger.warn("Error sending register confirmation to admin",t);
        }

        return result;

    }

    private void authenticate(RegisterInformation info) throws Exception {
        AuthenticationTool authTool=new AuthenticationToolAPI();
        Map<String, String> validatedAuth = authTool.createNewSession(info.getAuthorityName(), info.getPassword());
        authTool.storeAuthInfoInSession(info.getAuthorityName(), validatedAuth.get(CCConstants.AUTH_TICKET),CCConstants.AUTH_TYPE_DEFAULT, Context.getCurrentInstance().getRequest().getSession(true));
        authService.authenticate(info.getAuthorityName(),info.getPassword().toCharArray());
    }

    protected String storeUser(RegisterInformation info) throws Exception {
        return AuthenticationUtil.runAsSystem(()-> {
            Map<QName, Serializable> map = new HashMap<>();
            String authority = info.getEmail();
            map.put(ContentModel.PROP_USERNAME, authority);
            map.put(ContentModel.PROP_FIRSTNAME, info.getFirstName());
            map.put(ContentModel.PROP_LASTNAME, info.getLastName());
            map.put(ContentModel.PROP_ORGANIZATION, info.getOrganization());
            map.put(ContentModel.PROP_EMAIL, info.getEmail());
            map.put(QName.createQName(CCConstants.CM_PROP_PERSON_VCARD), info.getVcard());
            map.put(QName.createQName(CCConstants.CM_PROP_PERSON_ALLOW_NOTIFICATIONS), info.isAllowNotifications());
            try {
                authService.createAuthentication(authority, info.getPassword().toCharArray());
                NodeRef result = personService.createPerson(map);
                return result.getId();
            } catch (Throwable t) {
                // check for duplicates
                throw t;
            }
        });
    }
    @Override
    public void register(RegisterInformation info) throws DuplicateAuthorityException, Throwable{
        try {
            AuthenticationUtil.runAsSystem(() -> {
                // on register, authority name equlas the user name
                info.setAuthorityName(info.getEmail());
                if (userExists(info))
                    throw new DuplicateAuthorityException();
                String value = addToCacheNoDuplicate(info, registerUserCache,true);
                sendRegisterMail(info, value);
                return null;
            });
        }catch(RuntimeException e){
            throw e.getCause();
        }

    }
    @Override
    public boolean resendRegisterMail(String mail) throws Exception {
        String key=getKeyForMail(mail,registerUserCache);
        if(key!=null){
            sendRegisterMail(registerUserCache.get(key),key);
            return true;
        }
        return false;
    }
    private String getKeyForMail(String mail,SimpleCache<String,RegisterInformation> cache){
        for (String cacheKey : cache.getKeys()) {
            try {
                if (cache.get(cacheKey).getAuthorityName().equals(mail))
                    return cacheKey;
            }catch(Throwable t){
                // it's possible to get class cast exceptions when hot deploying
            }
        }
        return null;
    }
    private String addToCacheNoDuplicate(RegisterInformation info,SimpleCache cache,boolean override) {
        String existing=getKeyForMail(info.getAuthorityName(),cache);
        if(existing!=null) {
            cache.put(existing,info);
            return existing;
        }
        return addToCache(info, cache);
    }

    private String addToCache(Object data, SimpleCache cache) {
        while(true){
            String id=RandomStringUtils.random(KEY_LENGTH,true,true);
            if(cache.contains(id))
                continue;
            cache.put(id,data);
            return id;
        }
    }
}
