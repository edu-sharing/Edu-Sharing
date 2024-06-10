package org.edu_sharing.service.license;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.edu_sharing.repository.client.rpc.AssignedLicense;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.AuthenticationToolAPI;
import org.edu_sharing.repository.server.MCAlfrescoBaseClient;
import org.edu_sharing.repository.server.RepoFactory;
import org.edu_sharing.alfresco.repository.server.authentication.Context;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;

public class AssignedLicenseDAOSubTypeImpl implements AssignedLicenseDAO {

	@Override
	public List<AssignedLicense> getAssignedLicenses(String repositoryId, String nodeId) throws Throwable {

		//remote repositories need a valid homeAuth to make the AuthByApp
		Map<String, String> homeAuthInfo = null;
		if(!ApplicationInfoList.getRepositoryInfoById(repositoryId).ishomeNode()){
			homeAuthInfo = new AuthenticationToolAPI().getAuthentication(Context.getCurrentInstance().getRequest().getSession());
		}
		
		MCAlfrescoBaseClient mcAlfrescoBaseClient = (MCAlfrescoBaseClient) RepoFactory.getInstance(repositoryId, homeAuthInfo);
		Map<String, Map<String, Object>> assignedLicenses = mcAlfrescoBaseClient.getChildrenByType(nodeId,
				CCConstants.CCM_TYPE_ASSIGNED_LICENSE);

		List<AssignedLicense> licenses = new ArrayList<>();
		for (Map.Entry<String, Map<String, Object>> entryLicense : assignedLicenses.entrySet()) {
			String assignedLicenseNodeId = entryLicense.getKey();
			String[] assignedLicenseLicense = new String[] { (String) entryLicense.getValue().get(
					CCConstants.CCM_PROP_ASSIGNED_LICENSE_ASSIGNEDLICENSE) };
			String assignedLicenseAuthority = (String) entryLicense.getValue().get(CCConstants.CCM_PROP_ASSIGNED_LICENSE_AUTHORITY);
			licenses.add(new AssignedLicense(assignedLicenseNodeId, assignedLicenseAuthority, assignedLicenseLicense));
		}

		return licenses;
	}

	@Override
	public void setAssignedLicenses(String repositoryId, String nodeId, List<AssignedLicense> assignedLicenses) throws Throwable {

		//remote repositories need a valid homeAuth to make the AuthByApp
		Map<String, String> homeAuthInfo = null;
		if(repositoryId != null && !ApplicationInfoList.getRepositoryInfoById(repositoryId).ishomeNode()){
			homeAuthInfo = new AuthenticationToolAPI().getAuthentication(Context.getCurrentInstance().getRequest().getSession());
		}
		
		MCAlfrescoBaseClient mcAlfrescoBaseClient = (MCAlfrescoBaseClient) RepoFactory.getInstance(repositoryId,homeAuthInfo);

		ArrayList<String> newAuthorities = new ArrayList<>();
		for (AssignedLicense assignedLicense : assignedLicenses) {
			newAuthorities.add(assignedLicense.getAuthority());
		}
		// remove old licenses for authorities that are in
		// newAuthorities List
		Map<String, Map<String, Object>> assignedLicensesHashMap = mcAlfrescoBaseClient.getChildrenByType(nodeId,
				CCConstants.CCM_TYPE_ASSIGNED_LICENSE);
		for (Map.Entry<String, Map<String, Object>> assignedLicenseEntry : assignedLicensesHashMap.entrySet()) {
			String oldAuthority = (String) assignedLicenseEntry.getValue().get(CCConstants.CCM_PROP_ASSIGNED_LICENSE_AUTHORITY);
			if (newAuthorities.contains(oldAuthority)) {
				mcAlfrescoBaseClient.removeChild(nodeId, assignedLicenseEntry.getKey(), CCConstants.CCM_ASSOC_ASSIGNEDLICENSES);
			}
		}

		// addAspect
		mcAlfrescoBaseClient.addAspect(nodeId, CCConstants.CCM_ASPECT_LICENSES);
		// set new licenses
		for (AssignedLicense assignedLicense : assignedLicenses) {
			if (assignedLicense.getLicenses() != null) {
				Map<String, Object> properties = new HashMap<>();
				properties.put(CCConstants.CCM_PROP_ASSIGNED_LICENSE_AUTHORITY, assignedLicense.getAuthority());
				properties.put(CCConstants.CCM_PROP_ASSIGNED_LICENSE_ASSIGNEDLICENSE, assignedLicense.getLicenses()[0]);

				mcAlfrescoBaseClient.createNode(nodeId, CCConstants.CCM_TYPE_ASSIGNED_LICENSE, CCConstants.CCM_ASSOC_ASSIGNEDLICENSES,
						properties);
			}
		}

	}

}
