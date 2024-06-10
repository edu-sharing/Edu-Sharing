package org.alfresco.repo.webdav;

import com.typesafe.config.Config;
import jakarta.servlet.*;
import org.alfresco.repo.web.filter.beans.DependencyInjectedFilter;
import org.edu_sharing.alfresco.lightbend.LightbendConfigLoader;
import org.edu_sharing.spring.ApplicationContextFactory;

import java.io.IOException;

public class Edu_SharingBeanProxyFilter implements Filter
{
    /**
     * Name of the init parameter that carries the proxied bean name 
     */
    private static final String INIT_PARAM_BEAN_NAME = "beanName";
    
    private DependencyInjectedFilter filter;
    private ServletContext context;

    Config eduConfig = LightbendConfigLoader.get();

    static String CONFIG_ENABLED = "repository.webdav.enabled";

    boolean enabled = true;
    
    /**
     * Initialize the filter.
     * 
     * @param args
     *            FilterConfig
     * @throws ServletException
     *             the servlet exception
     * @exception ServletException
     */
    public void init(FilterConfig args) throws ServletException
    {
        if(eduConfig.hasPath(CONFIG_ENABLED)){
            enabled = eduConfig.getBoolean(CONFIG_ENABLED);
        }
        this.context = args.getServletContext();
    	this.filter = (DependencyInjectedFilter) ApplicationContextFactory.getApplicationContext().getBean(args.getInitParameter(INIT_PARAM_BEAN_NAME));
        if(this.filter instanceof Filter){
            ((Filter)this.filter).init(args);
        }
    }

    /* (non-Javadoc)
     * @see jakarta.servlet.Filter#destroy()
     */
    public void destroy()
    {
        this.filter = null;
    }

    /* (non-Javadoc)
     * @see jakarta.servlet.Filter#doFilter(jakarta.servlet.ServletRequest, jakarta.servlet.ServletResponse, jakarta.servlet.FilterChain)
     */
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
            ServletException
    {
        if(enabled) this.filter.doFilter(this.context, request, response, chain);
    }

}
