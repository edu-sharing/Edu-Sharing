package org.edu_sharing.alfresco.service.config.model;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import jakarta.xml.bind.annotation.XmlElement;
import java.io.Serializable;

public class Values implements Serializable {
	@XmlElement public String[] supportedLanguages;
	@XmlElement public String extension;
	@XmlElement public String loginUrl;
	@XmlElement public Boolean loginAllowLocal;
	@XmlElement public String loginProvidersUrl;
	@XmlElement public String loginProviderTargetUrl;
	//none,iframe,redirect
	@XmlElement public LoginSilentMode loginSilentMode;
	@XmlElement public Register register;
	@XmlElement public String recoverPasswordUrl;
	@XmlElement	public String imprintUrl;
	@XmlElement	public String privacyInformationUrl;
	@XmlElement	public String helpUrl;
	@XmlElement	public String whatsNewUrl;
	@XmlElement	public String editProfileUrl;
	@XmlElement	public Boolean editProfile;
	@XmlElement	public String[] workspaceColumns;
	@XmlElement	public boolean workspaceSharedToMeDefaultAll;
	@XmlElement	public String[] hideMainMenu;
	@XmlElement	public LogoutInfo logout;
	@XmlElement	public MenuEntry[] menuEntries;
	@XmlElement	public ContextMenuEntry[] customOptions;
	@XmlElement	public ContextMenuEntry[] userMenuOverrides;
	@XmlElement	public String[] allowedLicenses;
	@XmlElement	public License[] customLicenses;
	@XmlElement	public ConfigWorkflow workflow;
	@XmlElement	public Boolean licenseDialogOnUpload;
	@XmlElement	public Boolean nodeReport;
	@XmlElement	public Boolean branding;
	@XmlElement	public ConfigRating rating;
	@XmlElement	public Boolean publishingNotice;
	@XmlElement	public String siteTitle;
	@XmlElement	public String userDisplayName;
	@XmlElement	public String userSecondaryDisplayName;
	@XmlElement	public Boolean userAffiliation;
	@XmlElement	public String defaultUsername;
	@XmlElement	public String defaultPassword;
	@XmlElement	public Banner banner;
	@XmlElement	public AvailableMds[] availableMds;
	@XmlElement	public String[] availableRepositories;
	@XmlElement	public Integer searchViewType;
	@XmlElement	public Integer workspaceViewType;
	@XmlElement	public Integer itemsPerRequest;
	@XmlElement	public Rendering rendering;
	@XmlElement	public SessionExpiredDialog sessionExpiredDialog;
	@XmlElement	public String defaultLocation;
	@XmlElement	public String loginDefaultLocation;
	@XmlElement	public Boolean searchGroupResults;
	@XmlElement	public Mainnav mainnav;
	@XmlElement	public String searchSidenavMode;
	@XmlElement	public Collections collections;
	@XmlElement	public LicenseAgreement licenseAgreement;
	@XmlElement	public Services services;
	@XmlElement	public HelpMenuOptions[] helpMenuOptions;
	@XmlElement	public Image[] images;
	@XmlElement	public FontIcon[] icons;
	@XmlElement	public Stream stream;
	@XmlElement	public Admin admin;
	@XmlElement	public SimpleEdit simpleEdit;
	@XmlElement	public ConfigFrontpage frontpage;
	@XmlElement	public ConfigUpload upload;
	@XmlElement	public ConfigPublish publish;
	@XmlElement	public ConfigRemote remote;
	@XmlElement public ConfigReportProblem reportProblem;
	@XmlElement	public String customCSS;
	@XmlElement	public ConfigThemeColors themeColors;
	@XmlElement	public ConfigPrivacy privacy;

	@JsonPropertyDescription("Config for frontend tutorial (darkened area with highlighted element)")
	@XmlElement	public ConfigTutorial tutorial;

}
