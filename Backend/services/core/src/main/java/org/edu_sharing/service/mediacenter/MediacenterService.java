package org.edu_sharing.service.mediacenter;

import org.edu_sharing.service.search.model.SortDefinition;

import java.io.InputStream;
import java.util.Date;
import java.util.List;
import java.util.Map;

public interface MediacenterService {
	
	public int importMediacenters(InputStream csv);

	public int importOrganisations(InputStream csv);

	public int importOrgMcConnections(InputStream csv, boolean removeSchoolsFromMC);

	/**
	 * adjustment of licenses for mediacenters on nodes given by an {@link MediacenterLicenseProvider}
	 * does a full sync, compares LicensProvider nodes with local nodes and adds or removes permission
	 *
	 * @deprecated
	 */
	public void manageNodeLicenses();

	/**
	 * adjustment of licenses for mediacenters on nodes given by an {@link MediacenterLicenseProvider}
	 * full sync done by using null for from/until
	 * this method only get's changes of {@link MediacenterLicenseProvider}
	 * @param from
	 * @param until
	 */
	public void manageNodeLicenses(Date from, Date until);

	List<String> getMediacenterAuthoritiesByNode(String nodeId) throws Exception;

	public boolean isActive(String authorityName);

	public void setActive(boolean active, String authorityName);

	public String getMediacenterAdminGroup(String authorityName);

	public String getMediacenterProxyGroup(String authorityName);

	public void isAllowedToManage(String authorityName);

	public String createMediacenter(String id, String displayName, String postalCode, String city) throws Exception;

	public void updateMediacenter(String id, String displayName, String postalCode, String city, String districtAbbreviation, String mainUrl, String mediacenterCatalogs, boolean active) throws Exception;

	public void deleteMediacenter(String authorityName);

	public List<org.edu_sharing.service.model.NodeRef> getAllLicensedNodes(String mediacenter, Map<String, String[]> criteria, SortDefinition sortDefinition) throws Throwable;
}
