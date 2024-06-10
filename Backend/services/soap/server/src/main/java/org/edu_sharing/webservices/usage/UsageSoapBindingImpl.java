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

package org.edu_sharing.webservices.usage;

import java.util.ArrayList;

import org.apache.axis.Constants;
import org.apache.axis.MessageContext;
import org.apache.log4j.Logger;
import org.edu_sharing.webservices.authentication.AuthenticationException;

public class UsageSoapBindingImpl implements org.edu_sharing.webservices.usage.Usage {

	public static Logger logger = Logger.getLogger(UsageSoapBindingImpl.class);

	public org.edu_sharing.webservices.usage.UsageResult getUsage(java.lang.String repositoryTicket, java.lang.String repositoryUsername,
			java.lang.String lmsId, java.lang.String courseId, java.lang.String parentNodeId, java.lang.String appUser,
			java.lang.String ressourceId) throws java.rmi.RemoteException, org.edu_sharing.webservices.usage.UsageException,
			org.edu_sharing.webservices.authentication.AuthenticationException {

		try {
			org.edu_sharing.service.usage.UsageService usageService = new org.edu_sharing.service.usage.UsageService(getRequestIp(), lmsId,
					repositoryTicket);
			org.edu_sharing.service.usage.Usage serviceResult = usageService.getUsage(courseId, parentNodeId, appUser, ressourceId);
			return transform(serviceResult);
		} catch (org.edu_sharing.service.usage.AuthenticationException e) {
			throw new AuthenticationException(null,e.getMessage());
		} catch (org.edu_sharing.service.usage.UsageException e) {
			UsageException uE = new UsageException();
			uE.setFaultString(e.getMessage());
			throw uE;
		}

	}

	public org.edu_sharing.webservices.usage.UsageResult[] getUsageByParentNodeId(java.lang.String repositoryTicket,
			java.lang.String repositoryUsername, java.lang.String parentNodeId) throws java.rmi.RemoteException,
			org.edu_sharing.webservices.usage.UsageException, org.edu_sharing.webservices.authentication.AuthenticationException {
		try {
			org.edu_sharing.service.usage.UsageService usageService = new org.edu_sharing.service.usage.UsageService(getRequestIp(), null,
					repositoryTicket);
			ArrayList<org.edu_sharing.service.usage.Usage> serviceResults = usageService.getUsageByParentNodeId(parentNodeId);
			ArrayList<UsageResult> result = new ArrayList<>();

			for (org.edu_sharing.service.usage.Usage serviceResult : serviceResults) {
				result.add(transform(serviceResult));
			}

			return result.toArray(new UsageResult[result.size()]);
		} catch (org.edu_sharing.service.usage.AuthenticationException e) {
			throw new AuthenticationException(null,e.getMessage());
		}
	}

	public boolean deleteUsage(java.lang.String repositoryTicket, java.lang.String repositoryUsername, java.lang.String appSessionId,
			java.lang.String appCurrentUserId, java.lang.String lmsId, java.lang.String courseId, java.lang.String parentNodeId,
			java.lang.String resourceId) throws java.rmi.RemoteException, org.edu_sharing.webservices.usage.UsageException,
			org.edu_sharing.webservices.authentication.AuthenticationException {

		try {
			org.edu_sharing.service.usage.UsageService usageService = new org.edu_sharing.service.usage.UsageService(getRequestIp(), lmsId,
					repositoryTicket);
			return usageService.deleteUsage(appSessionId, appCurrentUserId, courseId, parentNodeId, resourceId);
		} catch (org.edu_sharing.service.usage.AuthenticationException e) {
			throw new AuthenticationException(null,e.getMessage());
		} catch (org.edu_sharing.service.usage.UsageException e) {
			UsageException uE = new UsageException();
			uE.setFaultString(e.getMessage());
			throw uE;
		}
	}

	public boolean usageAllowed(java.lang.String repositoryTicket, java.lang.String repositoryUsername, java.lang.String nodeId,
			java.lang.String lmsId, java.lang.String courseId) throws java.rmi.RemoteException,
			org.edu_sharing.webservices.usage.UsageException, org.edu_sharing.webservices.authentication.AuthenticationException {
		try {
			org.edu_sharing.service.usage.UsageService usageService = new org.edu_sharing.service.usage.UsageService(getRequestIp(), lmsId,
					repositoryTicket);
			return usageService.usageAllowed(nodeId, courseId);
		} catch (org.edu_sharing.service.usage.AuthenticationException e) {
			throw new AuthenticationException(null,e.getMessage());
		}
	}

	public void setUsage(java.lang.String repositoryTicket, java.lang.String repositoryUsername, java.lang.String lmsId,
			java.lang.String courseId, java.lang.String parentNodeId, java.lang.String appUser, java.lang.String appUserMail,
			java.util.Calendar fromUsed, java.util.Calendar toUsed, int distinctPersons, java.lang.String version,
			java.lang.String ressourceId, java.lang.String xmlParams) throws java.rmi.RemoteException,
			org.edu_sharing.webservices.usage.UsageException, org.edu_sharing.webservices.authentication.AuthenticationException {

		try {
			org.edu_sharing.service.usage.UsageService usageService = new org.edu_sharing.service.usage.UsageService(getRequestIp(), lmsId,
					repositoryTicket);
			usageService.setUsage(courseId, parentNodeId, appUser, appUserMail, fromUsed, toUsed, distinctPersons, version, ressourceId,
					xmlParams);
		} catch (org.edu_sharing.service.usage.AuthenticationException e) {
			throw new AuthenticationException(null,e.getMessage());
		} catch (org.edu_sharing.service.usage.UsageException e) {
			UsageException uE = new UsageException();
			uE.setFaultString(e.getMessage());
			throw uE;
		}

	}

	public boolean deleteUsages(java.lang.String repositoryTicket, java.lang.String repositoryUsername, java.lang.String appSessionId,
			java.lang.String appCurrentUserId, java.lang.String lmsId, java.lang.String courseId) throws java.rmi.RemoteException,
			org.edu_sharing.webservices.usage.UsageException, org.edu_sharing.webservices.authentication.AuthenticationException {
		try {
			org.edu_sharing.service.usage.UsageService usageService = new org.edu_sharing.service.usage.UsageService(getRequestIp(), lmsId,
					repositoryTicket);
			return usageService.deleteUsages(appSessionId, appCurrentUserId, courseId);
		} catch (org.edu_sharing.service.usage.AuthenticationException e) {
			throw new AuthenticationException(null,e.getMessage());
		} catch (org.edu_sharing.service.usage.UsageException e) {
			UsageException uE = new UsageException();
			uE.setFaultString(e.getMessage());
			throw uE;
		}
	}

	private String getRequestIp() {
		MessageContext messageContext = MessageContext.getCurrentContext();
		return messageContext.getStrProp(Constants.MC_REMOTE_ADDR);
	}

	private UsageResult transform(org.edu_sharing.service.usage.Usage serviceUsage) {
		UsageResult result = new UsageResult();

		result.setAppUser(serviceUsage.getAppUser());
		result.setAppUserMail(serviceUsage.getAppUserMail());
		result.setCourseId(serviceUsage.getCourseId());
		result.setDistinctPersons(serviceUsage.getDistinctPersons());
		result.setFromUsed(serviceUsage.getFromUsed());
		result.setLmsId(serviceUsage.getLmsId());
		result.setNodeId(serviceUsage.getNodeId());
		result.setParentNodeId(serviceUsage.getParentNodeId());
		result.setResourceId(serviceUsage.getResourceId());
		result.setToUsed(serviceUsage.getToUsed());
		result.setUsageCounter(serviceUsage.getUsageCounter());
		result.setUsageVersion(serviceUsage.getUsageVersion());
		result.setUsageXmlParams(serviceUsage.getUsageXmlParams());
		return result;
	}
}
