package org.edu_sharing.restservices.shared;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.alfresco.service.cmr.security.PermissionService;

import java.io.Serializable;
import java.util.List;

public class Mediacenter extends ManagableGroup {
	public Mediacenter(String authorityName, Type authorityType) {
		this.setAuthorityName(authorityName);
		this.setAuthorityType(authorityType);
		this.setGroupName(authorityName.substring(PermissionService.GROUP_PREFIX.length()));
	}

	@Data
	@EqualsAndHashCode(callSuper = true)
	public static class Profile extends GroupProfile {
		private MediacenterProfileExtension mediacenter;

		public Profile() {
		}

		public Profile(GroupProfile profile){
			super(profile);
		}
	}

	@Data
	public static class MediacenterProfileExtension implements Serializable {
		// id, Standort, Kreiskürzel, Kurzbezeichnung (not here, equals the displayName), URL/Link zur Startseite / Kataloge (JSON)
		private String id;
		private String location;
		private String districtAbbreviation;
		private String mainUrl;
		private List<Catalog> catalogs;
		private ContentStatus contentStatus;

		public enum ContentStatus{
			Activated,
			Deactivated,
		}
	}
	@Data
	public static class Catalog{
		private String name;
		private String url;
	}
}
