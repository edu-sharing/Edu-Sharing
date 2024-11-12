package org.edu_sharing.service.oai.formats;

import io.gdcc.xoai.dataprovider.model.MetadataFormat;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.apache.commons.lang3.StringUtils;
import org.edu_sharing.metadataset.v2.MetadataKey;
import org.edu_sharing.metadataset.v2.MetadataWidget;
import org.edu_sharing.metadataset.v2.tools.MetadataHelper;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.tools.URLTool;
import org.edu_sharing.repository.server.tools.VCardConverter;
import org.edu_sharing.repository.tools.URLHelper;
import org.edu_sharing.service.license.LicenseService;
import org.edu_sharing.service.util.PropertyMapper;
import org.edu_sharing.spring.scope.refresh.annotations.RefreshScope;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.w3c.dom.Element;

import javax.xml.parsers.ParserConfigurationException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Lazy
@Slf4j
@Component
@RefreshScope
@RequiredArgsConstructor
public class OaiLomMetadataFormatWriter extends AbstractMetadataFormatWriter {
    private final static String LANG_ATTRIBUTE = "language";

    @Value("${exporter.oai.lom.identifier.catalog:#{T(org.edu_sharing.repository.server.tools.ApplicationInfoList).getHomeRepository().getAppId()}}")
    protected String identifierCatalog;


    @Value("${repository.privacy.filterVCardEmail:true}")
    protected boolean filterVCardEmail;

    private final MetadataFormat metadataFormat = MetadataFormat.metadataFormat("lom")
            .withSchemaLocation("http://www.w3.org/2001/XMLSchema-instance http://ltsc.ieee.org/xsd/LOM http://ltsc.ieee.org/xsd/lomv1.0/lom.xsd")
            .withNamespace("http://ltsc.ieee.org/xsd/LOM")
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
        lom.setAttribute("xmlns", "http://ltsc.ieee.org/xsd/LOM");
        lom.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
        lom.setAttribute("xsi:schemaLocation", "http://www.w3.org/2001/XMLSchema-instance");
        lom.setAttribute("xsi:schemaLocation", "http://ltsc.ieee.org/xsd/LOM  http://ltsc.ieee.org/xsd/lomv1.0/lom.xsd");
    }

    protected void addContent(Context context, Element parent) {
        createGeneral(context, parent);
        createLifecycle(context, parent);
        createMetadata(context, parent);
        createTechnical(context, parent);
        createEducational(context, parent);
        createRights(context, parent);
        createThumbnail(context, parent);
        createClassification(context, parent);
    }

    protected void createGeneral(Context context, Element lom) {
        Element general = context.createAndAppendElement("general", lom);
        PropertyMapper propertyMapper = context.getPropertyMapper();

        createIdentifier(context, general, identifierCatalog, propertyMapper.getString(CCConstants.SYS_PROP_NODE_UID));
        createIdentifier(context, general, "HDL", propertyMapper.getString(CCConstants.CCM_PROP_PUBLISHED_HANDLE_ID));

        createTitle(context, general);
        context.createAndAppendElement(LANG_ATTRIBUTE, general, propertyMapper.getString(CCConstants.LOM_PROP_GENERAL_LANGUAGE));

        createDescription(context, general);
        createKeyword(context, general);
        context.createAndAppendElement("structure", general, propertyMapper.getString(CCConstants.LOM_PROP_GENERAL_STRUCTURE));
        context.createAndAppendElement("value", context.createAndAppendElement("aggregationLevel", general), propertyMapper.getString(CCConstants.LOM_PROP_GENERAL_AGGREGATIONLEVEL));

    }

    private void createKeyword(Context context, Element general) {
        PropertyMapper propertyMapper = context.getPropertyMapper();
        Element keyword = context.createAndAppendElement("keyword", general);
        context.createAndAppendElement("string", keyword, propertyMapper.getStringList(CCConstants.LOM_PROP_GENERAL_KEYWORD))
                .forEach(x -> addLanguageAttribute(context, x));
    }

    private void createDescription(Context context, Element general) {
        PropertyMapper propertyMapper = context.getPropertyMapper();
        Element description = context.createAndAppendElement("string", context.createAndAppendElement("description", general), propertyMapper.getString(CCConstants.LOM_PROP_GENERAL_DESCRIPTION));
        addLanguageAttribute(context, description);
    }

    protected void createTitle(Context context, Element general) {
        PropertyMapper propertyMapper = context.getPropertyMapper();
        String titleText = propertyMapper.getString(CCConstants.LOM_PROP_GENERAL_TITLE);
        if (StringUtils.isBlank(titleText)) {
            titleText = propertyMapper.getString(CCConstants.CM_NAME);
        }

        Element title = context.createAndAppendElement("string", context.createAndAppendElement("title", general), titleText);
        addLanguageAttribute(context, title);
    }


    protected void createIdentifier(Context context, @NonNull Element general, @NonNull String catalogText, String entryText) {
        if (StringUtils.isBlank(entryText)) {
            return;
        }

        Element identifier = context.createAndAppendElement("identifier", general);
        context.createAndAppendElement("catalog", identifier, catalogText);
        context.createAndAppendElement("entry", identifier, entryText);
    }

    protected void createLifecycle(Context context, Element lom) {
        PropertyMapper propertyMapper = context.getPropertyMapper();
        Element lifeCycle = context.createAndAppendElement("lifeCycle", lom);
        context.createAndAppendElement("version", lifeCycle, propertyMapper.getString(CCConstants.LOM_PROP_LIFECYCLE_VERSION));
        context.createAndAppendElement("value", context.createAndAppendElement("status", lifeCycle), propertyMapper.getString(CCConstants.LOM_PROP_LIFECYCLE_STATUS));

        //@todo autor englisch?, role source
        addContributor(context, lifeCycle, CCConstants.CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_AUTHOR, "author");
        addContributor(context, lifeCycle, CCConstants.CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_CONTENT_PROVIDER, "content provider");
        addContributor(context, lifeCycle, CCConstants.CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_EDITOR, "editor");
        addContributor(context, lifeCycle, CCConstants.CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_EDUCATIONAL_VALIDATOR, "educational validator");
        addContributor(context, lifeCycle, CCConstants.CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_GRAPHICAL_DESIGNER, "graphical designer");
        addContributor(context, lifeCycle, CCConstants.CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_INITIATOR, "initiator");
        addContributor(context, lifeCycle, CCConstants.CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_INSTRUCTIONAL_DESIGNER, "instructional designer");
        addContributor(context, lifeCycle, CCConstants.CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_PUBLISHER, "publisher");
        addContributor(context, lifeCycle, CCConstants.CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_SCRIPT_WRITER, "script writer");
        addContributor(context, lifeCycle, CCConstants.CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_SUBJECT_MATTER_EXPERT, "subject matter expert");
        addContributor(context, lifeCycle, CCConstants.CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_TECHNICAL_IMPLEMENTER, "technical implementer");
        addContributor(context, lifeCycle, CCConstants.CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_TECHNICAL_VALIDATOR, "technical validator");
        addContributor(context, lifeCycle, CCConstants.CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_TERMINATOR, "terminator");
        addContributor(context, lifeCycle, CCConstants.CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_UNKNOWN, "unknown");
        addContributor(context, lifeCycle, CCConstants.CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_VALIDATOR, "validator");
    }

    protected void addContributor(Context context, Element eleParent, String contributorProperty, String role) {
        PropertyMapper propertyMapper = context.getPropertyMapper();
        List<String> contributer = propertyMapper.getStringList(contributorProperty);
        contributer = prepareContributor(context, contributer);

        if (!contributer.isEmpty()) {
            Element eleContribute = context.createAndAppendElement("contribute", eleParent);
            context.createAndAppendElement("value", context.createAndAppendElement("role", eleContribute), role);
            context.createAndAppendElementAsCData("entity", eleContribute, contributer);
        }

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
        Element metaMetadata = context.createAndAppendElement("metaMetadata", lom);
        addContributor(context, metaMetadata, CCConstants.CCM_PROP_IO_REPL_METADATACONTRIBUTER_CREATOR, "creator");
        addContributor(context, metaMetadata, CCConstants.CCM_PROP_IO_REPL_METADATACONTRIBUTER_PROVIDER, "provider");
        addContributor(context, metaMetadata, CCConstants.CCM_PROP_IO_REPL_METADATACONTRIBUTER_VALIDATOR, "validator");
    }

    protected void createTechnical(Context context, Element lom) {
        //technical -> first is the "real" binary data
        PropertyMapper propertyMapper = context.getPropertyMapper();
        String format = propertyMapper.getString(CCConstants.LOM_PROP_TECHNICAL_FORMAT);
        if (StringUtils.isNotBlank(format)) {
            Element technical = context.createAndAppendElement("technical", lom);
            context.createAndAppendElement("format", technical, format);
            context.createAndAppendElement("location", technical, URLTool.getDownloadServletUrl(context.getNodeId(), null, false));
        }

        // second is text/html for rendering
        Element technical = context.createAndAppendElement("technical", lom);
        context.createAndAppendElement("format", technical, "text/html");
        context.createAndAppendElement("location", technical, URLHelper.getNgRenderNodeUrl(context.getNodeId(), null));
    }

    protected void createEducational(Context context, Element lom) {
        PropertyMapper propertyMapper = context.getPropertyMapper();
        //educational
        Element educational = context.createAndAppendElement("educational", lom);
        context.createAndAppendElement("value", context.createAndAppendElement("learningResourceType", educational), propertyMapper.getStringList(CCConstants.CCM_PROP_IO_REPL_EDUCATIONAL_LEARNINGRESSOURCETYPE));

        List<String> educationalContext = propertyMapper.getStringList(CCConstants.CCM_PROP_IO_REPL_EDUCATIONAL_CONTEXT);
        for (String eduContext : educationalContext) {
            context.createAndAppendElement("value", context.createAndAppendElement("context", educational), eduContext);
        }

        context.createAndAppendElement("value", context.createAndAppendElement("intendedEndUserRole", educational), propertyMapper.getStringList(CCConstants.CCM_PROP_IO_REPL_EDUCATIONAL_INTENDEDENDUSERROLE));

        Integer tarFrom = propertyMapper.getInteger(CCConstants.CCM_PROP_IO_REPL_EDUCATIONAL_TYPICALAGERANGEFROM);
        Integer tarTo = propertyMapper.getInteger(CCConstants.CCM_PROP_IO_REPL_EDUCATIONAL_TYPICALAGERANGETO);

        if (tarFrom == null) {
            return;
        }
        if (tarTo == null) {
            return;
        }

        String tar = tarFrom + "-" + tarTo;
        addLanguageAttribute(context, context.createAndAppendElement("string", context.createAndAppendElement("typicalAgeRange", educational), tar));
    }

    protected void createRights(Context context, Element lom) {
        PropertyMapper propertyMapper = context.getPropertyMapper();
        Element rights = context.createAndAppendElement("rights", lom);
        Element copyrightAndOtherRestrictions = context.createAndAppendElement("copyrightAndOtherRestrictions", rights);
        String commonLicenceKey = propertyMapper.getString(CCConstants.CCM_PROP_IO_COMMONLICENSE_KEY);
        if (StringUtils.isNotBlank(commonLicenceKey)) {
            String commonLicenseLocale = propertyMapper.getString(CCConstants.CCM_PROP_IO_COMMONLICENSE_CC_LOCALE, Locale.getDefault().getLanguage());
            String commonLicenseVersion = propertyMapper.getString(CCConstants.CCM_PROP_IO_COMMONLICENSE_CC_VERSION);
            String url = new LicenseService().getLicenseUrl(commonLicenceKey, commonLicenseLocale, commonLicenseVersion);
            context.createAndAppendElement("value", copyrightAndOtherRestrictions, "yes");
            Element description = context.createAndAppendElement("description", rights);
            Element rightsDescStrEle = context.createAndAppendElement("string", description, url);
            addLanguageAttribute(context, rightsDescStrEle);
            context.createAndAppendElement("cost", rights, "no");
        } else {
            context.createAndAppendElement("value", copyrightAndOtherRestrictions, propertyMapper.getBoolean(CCConstants.LOM_PROP_RIGHTS_COPY_RIGHT));
            context.createAndAppendElement("description", rights, propertyMapper.getString(CCConstants.LOM_PROP_RIGHTS_RIGHTS_DESCRIPTION));
        }
    }

    protected void createThumbnail(Context context, Element lom) {
        // TODO can we use CCM_PROP_IO_THUMBNAILURL?
        //String thumbnailUrl = (String)nodeService.getProperty(nodeRef, QName.createQName(CCConstants.CCM_PROP_IO_THUMBNAILURL));
        String thumbnailUrl = URLTool.getPreviewServletUrl(new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, context.getNodeId()));
        if (thumbnailUrl != null) {
            Element relation = context.createAndAppendElement("relation", lom);

            Element kind = context.createAndAppendElement("kind", relation);
            context.createAndAppendElement("source", kind, "LOM-DEv1.0");
            context.createAndAppendElement("value", kind, "hasthumbnail");

            Element resource = context.createAndAppendElement("resource", relation);
            Element description = context.createAndAppendElement("description", resource);
            Element string = context.createAndAppendElement("string", description, thumbnailUrl);
            addLanguageAttribute(string, "en");
        }
    }

    protected void createClassification(Context context, Element lom) {
        PropertyMapper propertyMapper = context.getPropertyMapper();
        List<String> taxonIds = propertyMapper.getStringList(CCConstants.CCM_PROP_IO_REPL_TAXON_ID);
        List<String> classificationKeyword = propertyMapper.getStringList(CCConstants.CCM_PROP_IO_REPL_CLASSIFICATION_KEYWORD);
        if (taxonIds.isEmpty() && classificationKeyword.isEmpty()) {
            return;
        }

        Element classification = context.createAndAppendElement("classification", lom);
        Element purpose = context.createAndAppendElement("purpose", classification);
        context.createAndAppendElement("source", purpose, "LOMv1.0");
        context.createAndAppendElement("value", purpose, "discipline");

        classificationKeyword.forEach(kw -> addLanguageAttribute(
                context.createAndAppendElement("string",
                        context.createAndAppendElement("keyword", classification),
                        kw),
                "de"));

        if (taxonIds.isEmpty()) {
            return;
        }

        Element taxonPath = context.createAndAppendElement("taxonPath", classification);
        Element tpSource = context.createAndAppendElement("source", taxonPath);
        Element tpSourceString = context.createAndAppendElement("string", tpSource, "EAF Thesaurus");
        addLanguageAttribute(tpSourceString, "x-t-eaf");

        try {
            MetadataWidget widget = MetadataHelper.getLocalDefaultMetadataset().findWidget("ccm:taxonid");
            Map<String, MetadataKey> values = widget.getValuesAsMap();
            if (values == null) {
                return;
            }

            for (String taxonId : taxonIds) {
                Element taxon = context.createAndAppendElement("taxon", taxonPath);
                context.createAndAppendElement("id", taxon, taxonId);
                //ask eaf kataolog todo allow other kataloges

                MetadataKey metadataKey = values.get(taxonId);
                if (metadataKey != null) {
                    addLanguageAttribute(
                            context.createAndAppendElement(
                                    "string",
                                    context.createAndAppendElement("entry", taxon),
                                    metadataKey.getCaption()),
                            "de");
                }
            }
        } catch (Throwable e) {
            log.error(e.getMessage(), e);
        }
    }
}
