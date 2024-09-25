package org.edu_sharing.repository.server.jobs.quartz;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.apache.log4j.Logger;
import org.edu_sharing.alfresco.service.OrganisationService;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.context.ApplicationContext;

public class SetPermissionsOrgAdminGroup extends AbstractJob {
	
	Logger logger = Logger.getLogger(SetPermissionsOrgAdminGroup.class);

	public static final String PARAM_ORGANISATIONS = "ORGANISATIONS";
	public static final String PARAM_EXECUTE = "EXECUTE";
	
	ApplicationContext appContext = AlfAppContextGate.getApplicationContext();
	
	OrganisationService organisationService = (OrganisationService)appContext.getBean("eduOrganisationService");

	public static String DESCRIPTION = "runs recursive over all/specified Organisations folder and sets coordinator permissions";

	@Override
	public void execute(JobExecutionContext context) throws JobExecutionException {
		
		String orgs = context.getJobDetail().getJobDataMap().getString(PARAM_ORGANISATIONS);
		Boolean execute = new Boolean(context.getJobDetail().getJobDataMap().getString(PARAM_EXECUTE));
		
		RunAsWork<Void> runAs = new RunAsWork<Void>() {
			@Override
			public Void doWork() throws Exception {
				run((orgs != null) ? orgs.split(",") : null, execute);
				return null;
			}
		};
		
		AuthenticationUtil.runAsSystem(runAs);
	}
	
	private void run(String[] orgs, boolean execute) {
		if(orgs == null) {
			orgs = organisationService.getOrganisations().stream().map(o -> o.get(ContentModel.PROP_AUTHORITY_NAME)).collect(Collectors.toList()).toArray(new String[0]);
		}
		
		logger.info("running for " + orgs.length + " orgs");
		
		int i = 0;
		for(String org : orgs) {
			logger.info("org nr" + i);
			organisationService.setOrgAdminPermissions(org,execute);
			i++;
		}
	}
	
	
	
	@Override
	public Class[] getJobClasses() {
		//addJobClass(SetPermissionsOrgAdminGroup.class);
		
		ArrayList<Class> al = new ArrayList<>(Arrays.asList(allJobs));
		al.add(SetPermissionsOrgAdminGroup.class);
		return al.toArray(new Class[0]) ;
	}
	
}
