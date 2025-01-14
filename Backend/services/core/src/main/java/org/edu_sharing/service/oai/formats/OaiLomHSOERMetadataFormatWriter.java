package org.edu_sharing.service.oai.formats;

import io.gdcc.xoai.dataprovider.model.MetadataFormat;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sourceforge.cardme.vcard.VCard;
import net.sourceforge.cardme.vcard.types.ExtendedType;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.security.AccessStatus;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.namespace.QName;
import org.apache.commons.lang3.StringUtils;
import org.edu_sharing.alfresco.service.guest.GuestConfig;
import org.edu_sharing.alfresco.service.guest.GuestService;
import org.edu_sharing.metadataset.v2.MetadataKey;
import org.edu_sharing.metadataset.v2.MetadataReader;
import org.edu_sharing.metadataset.v2.MetadataSet;
import org.edu_sharing.metadataset.v2.MetadataWidget;
import org.edu_sharing.metadataset.v2.tools.MetadataHelper;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.client.tools.forms.VCardTool;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.repository.server.tools.VCardConverter;
import org.edu_sharing.repository.tools.URLHelper;
import org.edu_sharing.service.license.LicenseService;
import org.edu_sharing.service.util.PropertyMapper;
import org.edu_sharing.spring.scope.refresh.annotations.RefreshScope;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.extensions.surf.util.ISO8601DateFormat;
import org.springframework.stereotype.Component;
import org.w3c.dom.Element;

import javax.xml.parsers.ParserConfigurationException;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

// please refer to
// https://dini-ag-kim.github.io/hs-oer-lom-profil/latest/

@Lazy
@Slf4j
@Component
@RefreshScope
@RequiredArgsConstructor
public class OaiLomHSOERMetadataFormatWriter extends AbstractMetadataFormatWriter {
    private final static String LANG_ATTRIBUTE = "xml:lang";

    private final NodeService nodeService;
    private final GuestService guestService;
    private final PermissionService permissionService;

    @Value("${exporter.oai.lom.identifier.catalog:#{T(org.edu_sharing.repository.server.tools.ApplicationInfoList).getHomeRepository().getAppId()}}")
    protected String identifierCatalog;


    @Value("${repository.privacy.filterVCardEmail:true}")
    protected boolean filterVCardEmail;


    private final MetadataFormat metadataFormat = MetadataFormat.metadataFormat("hs_oer_lom")
            .withSchemaLocation("http://www.w3.org/2001/XMLSchema-instance https://www.oerbw.de/hsoerlom https://w3id.org/dini-ag-kim/hs-oer-lom-profil/latest/schemas/hs-oer-lom.xsd")
            .withNamespace("https://www.oerbw.de/hsoerlom")
            .withTransformer(MetadataFormat.identity());

    @NotNull
    @Override
    public MetadataFormat getFormat() {
        return metadataFormat;
    }


    @Override
    public void build(Context context) throws ParserConfigurationException {
        Element lom = context.createAndAppendElement("lom");
        addNamespace(lom);
        addContent(context, lom);
    }

    protected void addLanguageAttribute(Context context, Element element) {
        addLanguageAttribute(element, context.getLanguage());
    }

    protected void addLanguageAttribute(Element title, String locale) {
        if (title == null) {
            return;
        }
        title.setAttribute(LANG_ATTRIBUTE, locale);
    }

    protected void addNamespace(Element lom) {
        lom.setAttribute("xmlns", "https://www.oerbw.de/hsoerlom");
        lom.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
        lom.setAttribute("xsi:schemaLocation", "http://www.w3.org/2001/XMLSchema-instance");
        lom.setAttribute("xsi:schemaLocation", "https://www.oerbw.de/hsoerlom https://w3id.org/dini-ag-kim/hs-oer-lom-profil/latest/schemas/hs-oer-lom.xsd");
    }

    protected void addContent(Context context, Element parent) {
        createGeneral(context, parent);
        createLifecycle(context, parent);
        createMetadata(context, parent);
        createTechnical(context, parent);
        createEducational(context, parent);
        createRights(context, parent);
        createClassification(context, parent);
    }

    protected void createGeneral(Context context, Element lom) {
        Element general = context.createAndAppendElement("general", lom);
        PropertyMapper propertyMapper = context.getPropertyMapper();

        createIdentifier(context, general, identifierCatalog, propertyMapper.getString(CCConstants.SYS_PROP_NODE_UID));
        createIdentifier(context, general, "HDL", propertyMapper.getString(CCConstants.CCM_PROP_PUBLISHED_HANDLE_ID));

        createTitle(context, general);
        createLanguage(context, general, propertyMapper);

        createDescription(context, general);
        createKeyword(context, general);
        context.createAndAppendElement("structure", general, propertyMapper.getString(CCConstants.LOM_PROP_GENERAL_STRUCTURE));
        createAggregationLevel(context, general);
    }

    public void createAggregationLevel(Context context, Element general) {
        NodeRef nodeRef = new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, context.getNodeId());
        String val = "1";

        List<ChildAssociationRef> childAssocs = nodeService.getChildAssocs(nodeRef, QName.createQName(CCConstants.CCM_ASSOC_CHILDIO), null);
        if (childAssocs != null && !childAssocs.isEmpty()) {
            val = "2";
        }

        createSourceValue(context, "aggregationLevel", general, val);
    }

    private void createLanguage(Context context, Element general, PropertyMapper propertyMapper) {
        String language = propertyMapper.getString(CCConstants.LOM_PROP_GENERAL_LANGUAGE);
        if (Objects.equals(language, "unknown")) {
            return;
        }

        context.createAndAppendElement("language", general, language);
    }


    private void createKeyword(Context context, Element general) {
        PropertyMapper propertyMapper = context.getPropertyMapper();

        List<String> keywords = new ArrayList<>(propertyMapper.getStringList(CCConstants.LOM_PROP_GENERAL_KEYWORD));
        keywords.addAll(propertyMapper.getStringList(CCConstants.CCM_PROP_IO_REPL_CLASSIFICATION_KEYWORD));

        for (String keyword : keywords) {
            addLanguageAttribute(context, context.createAndAppendElement("langstring", context.createAndAppendElement("keyword", general), keyword));
        }

    }

    private void createDescription(Context context, Element general) {
        PropertyMapper propertyMapper = context.getPropertyMapper();
        Element description = context.createAndAppendElement("langstring", context.createAndAppendElement("description", general), propertyMapper.getString(CCConstants.LOM_PROP_GENERAL_DESCRIPTION));
        addLanguageAttribute(context, description);
    }

    protected void createTitle(Context context, Element general) {
        PropertyMapper propertyMapper = context.getPropertyMapper();
        String titleText = propertyMapper.getString(CCConstants.LOM_PROP_GENERAL_TITLE);
        if (StringUtils.isBlank(titleText)) {
            titleText = propertyMapper.getString(CCConstants.CM_NAME);
        }

        Element title = context.createAndAppendElement("langstring", context.createAndAppendElement("title", general), titleText);
        addLanguageAttribute(context, title);
    }

    protected void createIdentifier(Context context, @NonNull Element general, @NonNull String catalogText, String entryText) {
        if (StringUtils.isBlank(entryText)) {
            return;
        }

        Element identifier = context.createAndAppendElement("identifier", general);
        context.createAndAppendElement("catalog", identifier, catalogText);
        context.createAndAppendElement("langstring", context.createAndAppendElement("entry", identifier), entryText);
    }

    private List<String> defaultProviderContributor(Context context) {
        PropertyMapper propertyMapper = context.getPropertyMapper();
        String university = propertyMapper.getString(CCConstants.CCM_PROP_IO_UNIVERSITY);
        if (StringUtils.isBlank(university)) {
            return null;
        }
        try {
            Map<String, MetadataKey> knownUniversities = MetadataHelper.getLocalDefaultMetadataset()
                    .findWidget("ccm:university")
                    .getValuesAsMap();

            MetadataKey metadataKey = knownUniversities.get(university);
            return List.of(
                    VCardTool.hashMap2VCard(
                            Map.of(
                                    CCConstants.VCARD_ORG, metadataKey.getCaption(),
                                    CCConstants.VCARD_URL, "https://" + university
                            )));


        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

    protected void createLifecycle(Context context, Element lom) {
        PropertyMapper propertyMapper = context.getPropertyMapper();
        Element lifeCycle = context.createAndAppendElement("lifecycle", lom);
        context.createAndAppendElement("langstring", context.createAndAppendElement("version", lifeCycle), propertyMapper.getString(CCConstants.LOM_PROP_LIFECYCLE_VERSION));


        String status = "Draft";
        String unGuest = null;
        GuestConfig guestConfig = guestService.getCurrentGuestConfig();
        if (guestConfig.isEnabled()) {
            unGuest = guestConfig.getUsername();
        }
        if (unGuest == null) {
            unGuest = PermissionService.GUEST_AUTHORITY;
        }
        AccessStatus sharedForEveryOne = AuthenticationUtil.runAs(() -> permissionService.hasPermission(new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, context.getNodeId()), PermissionService.CONSUMER), unGuest);
        if (sharedForEveryOne.equals(AccessStatus.ALLOWED)) {
            status = "Final";
        }

        createSourceValue(context, "status", lifeCycle, status);

        addContributor(context, lifeCycle, CCConstants.CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_AUTHOR, "Author");
        addContributor(context, lifeCycle, CCConstants.CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_CONTENT_PROVIDER, "Content Provider");
        addContributor(context, lifeCycle, CCConstants.CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_EDITOR, "Editor");
        addContributor(context, lifeCycle, CCConstants.CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_EDUCATIONAL_VALIDATOR, "Educational Validator");
        addContributor(context, lifeCycle, CCConstants.CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_GRAPHICAL_DESIGNER, "Graphical Designer");
        addContributor(context, lifeCycle, CCConstants.CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_INITIATOR, "Initiator");
        addContributor(context, lifeCycle, CCConstants.CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_INSTRUCTIONAL_DESIGNER, "Instructional Designer");
        addContributor(context, lifeCycle, CCConstants.CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_PUBLISHER, "Publisher");
        addContributor(context, lifeCycle, CCConstants.CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_SCRIPT_WRITER, "Script Writer");
        addContributor(context, lifeCycle, CCConstants.CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_TECHNICAL_IMPLEMENTER, "Technical Implementer");
        addContributor(context, lifeCycle, CCConstants.CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_TECHNICAL_VALIDATOR, "Technical Validator");
        addContributor(context, lifeCycle, CCConstants.CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_TERMINATOR, "Terminator");
        addContributor(context, lifeCycle, CCConstants.CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_UNKNOWN, "Unknown");
    }


    private void addContributor(Context context, Element eleParent, String contributorProperty, String role, String datePropertyId, String description) {
        Element contributor = addContributor(context, eleParent, contributorProperty, role);
        addDateTime(context, "date", contributor, datePropertyId, description);
    }

    private void addDateTime(Context context, String name, Element contributor, String datePropertyId, String description) {
        if (contributor == null) {
            return;
        }

        String dateValue = null;
        PropertyMapper propertyMapper = context.getPropertyMapper();
        try {
            String date = propertyMapper.getString(datePropertyId, "");
            if (date.startsWith("P")) {
                Duration duration = Duration.parse(date);
                long seconds = duration.getSeconds();
                dateValue = String.format("%02d:%02d:%02d", (seconds / 3600) % 24, (seconds / 60) % 60, seconds % 60);
            }
        } catch (Exception e) {
            Long date = propertyMapper.getLong(datePropertyId);
            if(date != null) {
                dateValue = ISO8601DateFormat.format(new Date(date));
            }
        }

        if (StringUtils.isBlank(dateValue)) {
            return;
        }

        Element element = context.createAndAppendElement(name, contributor);
        context.createAndAppendElement("datetime", element, dateValue);
        context.createAndAppendElement("langstring", context.createAndAppendElement("description", element), description);
    }

    protected Element addContributor(Context context, Element eleParent, String contributorProperty, String role) {
        return addContributor(context, eleParent, contributorProperty, role, null);
    }

    protected Element addContributor(Context context, Element eleParent, String contributorProperty, String role, List<String> defaultVCards) {
        PropertyMapper propertyMapper = context.getPropertyMapper();
        List<String> contributor = propertyMapper.getStringList(contributorProperty, defaultVCards);
        contributor = contributor.stream()
                .map(x -> VCardConverter.cleanupVcard(x, vcard -> {
                    Optional<ExtendedType> contributeDate = Optional.of(vcard)
                            .map(VCard::getExtendedTypes)
                            .map(Collection::stream)
                            .stream()
                            .flatMap(y -> y)
                            .filter((type) -> CCConstants.VCARD_T_X_ES_LOM_CONTRIBUTE_DATE.equals(type.getExtendedName()))
                            .findFirst();
                    contributeDate.ifPresent(vcard::removeExtendedType);
                    return vcard;
                }))
                .flatMap(Collection::stream)
                .collect(Collectors.toList());

        contributor = prepareContributor(context, contributor);

        if (!contributor.isEmpty()) {
            Element eleContribute = context.createAndAppendElement("contribute", eleParent);
            createSourceValue(context, "role", eleContribute, role);
            for (String x : contributor) {
                context.createAndAppendElementAsCData("vcard", context.createAndAppendElement("centity", eleContribute), x);
            }
            return eleContribute;
        }
        return null;
    }

    protected List<String> prepareContributor(Context context, List<String> contrib) {
        return contrib.stream()
                //sometimes there are empty values in list
                .filter(StringUtils::isNotBlank)
                // validate email policy
                .map(x -> cleanupVCardEMail(context, x))
                .collect(Collectors.toList());
    }

    private String cleanupVCardEMail(Context context, String vCard) {
        if (!filterVCardEmail) {
            return vCard;
        }

        if (context.hasWritePermission()) {
            return vCard;
        }

        return VCardConverter.removeEMails(vCard);
    }

    protected void createMetadata(Context context, Element lom) {
        //metametadata
        Element metaMetadata = context.createElement("metametadata");
        addContributor(context, metaMetadata, CCConstants.CCM_PROP_IO_REPL_METADATACONTRIBUTER_CREATOR, "Creator", CCConstants.CM_PROP_C_MODIFIED, "Modified");
        addContributor(context, metaMetadata, CCConstants.CCM_PROP_IO_REPL_METADATACONTRIBUTER_PROVIDER, "Provider", defaultProviderContributor(context));
        addContributor(context, metaMetadata, CCConstants.CCM_PROP_IO_REPL_METADATACONTRIBUTER_VALIDATOR, "Validator", CCConstants.CCM_PROP_IO_PUBLISHED_DATE, "Published");
        if (metaMetadata.hasChildNodes()) {
            lom.appendChild(metaMetadata);
        }

    }


    protected void createTechnical(Context context, Element lom) {
        //technical -> first is the "real" binary data
        PropertyMapper propertyMapper = context.getPropertyMapper();
        Element technical = context.createElement("technical");

        context.createAndAppendElement("format", technical, propertyMapper.getString(CCConstants.LOM_PROP_TECHNICAL_FORMAT));
        context.createAndAppendElement("size", technical, propertyMapper.getString(CCConstants.LOM_PROP_TECHNICAL_SIZE));
        context.createAndAppendElement("location", technical, URLHelper.getNgRenderNodeUrl(context.getNodeId(), null))
                .setAttribute("type", "URI");

        context.createAndAppendElement("otherplatformrequirements", technical, propertyMapper.getStringList(CCConstants.LOM_PROP_TECHNICAL_OTHERPLATFORMREQUIREMENTS));
        addDateTime(context, "duration", technical, CCConstants.LOM_PROP_TECHNICAL_DURATION, "Playing time");

        if (technical.hasChildNodes()) {
            lom.appendChild(technical);
        }
    }

    protected void createEducational(Context context, Element lom) {
        PropertyMapper propertyMapper = context.getPropertyMapper();
        //educational
        Element educational = context.createElement("educational");

        List<String> learningResourceTypes = propertyMapper.getStringList(CCConstants.CCM_PROP_IO_REPL_EDUCATIONAL_LEARNINGRESSOURCETYPE);

        String metadataSet = propertyMapper.getString(CCConstants.CM_PROP_METADATASET_EDU_METADATASET, CCConstants.metadatasetdefault_id);

        learningResourceTypes.forEach(x -> {
            Element element = context.createAndAppendElement("learningResourceType", educational);
            addLanguageAttribute(
                    context.createAndAppendElement("langstring",
                            context.createAndAppendElement("source",
                                    element),
                            "https://w3id.org/kim/hcrt/scheme"),
                    "x-none");
            context.createAndAppendElement("id", element, x);

            Element entry = context.createAndAppendElement("entry", element);
            for (String language : List.of("de", "en")) {
                try {
                    MetadataSet mds = MetadataReader.getMetadataset(ApplicationInfoList.getHomeRepository(), metadataSet, language);
                    MetadataWidget widget = mds.findWidget(CCConstants.getValidLocalName(CCConstants.CCM_PROP_IO_REPL_EDUCATIONAL_LEARNINGRESSOURCETYPE));
                    MetadataKey metadataKey = widget.getValuesAsMap().get(x);
                    if (metadataKey != null && metadataKey.getLocale().equals(language)) {
                        addLanguageAttribute(context.createAndAppendElement("langstring", entry, metadataKey.getCaption()), language);
                    }
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }

            }

        });
//        createSourceValue(context, "learningResourceType", educational, learningResourceTypes);

        if (educational.hasChildNodes()) {
            lom.appendChild(educational);
        }
    }


    protected void createRights(Context context, Element lom) {
        PropertyMapper propertyMapper = context.getPropertyMapper();
        Element rights = context.createAndAppendElement("rights", lom);

        String commonLicenceKey = propertyMapper.getString(CCConstants.CCM_PROP_IO_COMMONLICENSE_KEY);
        if (StringUtils.isNotBlank(commonLicenceKey)) {
            String commonLicenseLocale = propertyMapper.getString(CCConstants.CCM_PROP_IO_COMMONLICENSE_CC_LOCALE, Locale.getDefault().getLanguage());
            String commonLicenseVersion = propertyMapper.getString(CCConstants.CCM_PROP_IO_COMMONLICENSE_CC_VERSION);
            String url = new LicenseService().getLicenseUrl(commonLicenceKey, commonLicenseLocale, commonLicenseVersion);
            boolean hasCopyright = !commonLicenceKey.equals(CCConstants.COMMON_LICENSE_CC_ZERO);

            createSourceValue(context, "copyrightandotherrestrictions", rights, hasCopyright ? "yes" : "no");
            Element rightsDescStrEle = context.createAndAppendElement("langstring", context.createAndAppendElement("description", rights), url);
            addLanguageAttribute(rightsDescStrEle, "x-t-cc-url");
        } else {

            createSourceValue(context, "copyrightandotherrestrictions", rights, propertyMapper.getBoolean(CCConstants.LOM_PROP_RIGHTS_COPY_RIGHT));
            String description = propertyMapper.getString(CCConstants.LOM_PROP_RIGHTS_RIGHTS_DESCRIPTION);
            if (StringUtils.isNotBlank(description)) {
                context.createAndAppendElement("description", rights, description);
            } else {
                addLanguageAttribute(context.createAndAppendElement("langstring",
                                context.createAndAppendElement("description", rights),
                                "Keine Lizenz festgelegt"),
                        Locale.GERMAN.getLanguage());
            }
        }
    }


    protected void createClassification(Context context, Element lom) {
        PropertyMapper propertyMapper = context.getPropertyMapper();
        List<String> taxonIds = propertyMapper.getStringList(CCConstants.CCM_PROP_IO_REPL_TAXON_ID);
        if (taxonIds.isEmpty()) {
            return;
        }

        Element classification = context.createAndAppendElement("classification", lom);
        Element purpose = context.createAndAppendElement("purpose", classification);
        context.createAndAppendElement("source", purpose, "LOMv1.0");
        context.createAndAppendElement("value", purpose, "discipline");

        if (taxonIds.isEmpty()) {
            return;
        }

//
        try {
            MetadataWidget widget = MetadataHelper.getLocalDefaultMetadataset().findWidget("ccm:taxonid");
            Map<String, MetadataKey> values = widget.getValuesAsMap();
            for (String taxonId : taxonIds) {
                MetadataKey metadataKey = values.get(taxonId);
                if (metadataKey == null) {
                    continue;
                }

                List<String> path = new LinkedList<>();
                path.add(taxonId);

                while (metadataKey != null && metadataKey.getParent() != null) {
                    path.add(0, metadataKey.getParent());
                    metadataKey = values.get(metadataKey.getParent());
                }

                Element taxonPath = context.createAndAppendElement("taxonPath", classification);
                Element tpSource = context.createAndAppendElement("source", taxonPath);
                Element tpSourceString = context.createAndAppendElement("string", tpSource, "EAF Thesaurus");
                addLanguageAttribute(tpSourceString, "x-t-eaf");

                for (String id : path) {
                    Element taxon = context.createAndAppendElement("taxon", taxonPath);
                    context.createAndAppendElement("id", taxon, id);
                    Element entry = context.createAndAppendElement("entry", taxon);
                    if (values.get(id) != null) {
                        Element string = context.createAndAppendElement("string", entry, values.get(id).getCaption());
                        //<string language="de">Sachkunde</string>
                        addLanguageAttribute(context, string);
                    } else {
                        log.error("no value (caption) found for: {}", id);
                    }
                }
            }
        } catch (Throwable e) {
            log.error(e.getMessage(), e);
        }

    }


    private void createSourceValue(Context context, String name, Element parent, Object value) {
        createSourceValue(context, name, parent, value, "LOMv1.0");
    }

    private void createSourceValue(Context context, String name, Element parent, Object value, String source) {
        if (value == null) {
            return;
        }

        if (value instanceof List) {
            ((List<?>) value).forEach(x -> createSourceValue(context, name, parent, x, source));
        }

        Element element = context.createAndAppendElement(name, parent);
        addLanguageAttribute(context.createAndAppendElement("langstring", context.createAndAppendElement("source", element), source), "x-none");
        addLanguageAttribute(context.createAndAppendElement("langstring", context.createAndAppendElement("value", element), value.toString()), "x-none");

    }
}
