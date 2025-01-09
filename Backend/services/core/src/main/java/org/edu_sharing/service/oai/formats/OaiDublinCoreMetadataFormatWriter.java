package org.edu_sharing.service.oai.formats;

import io.gdcc.xoai.dataprovider.model.MetadataFormat;
import org.apache.commons.lang3.StringUtils;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.tools.VCardConverter;
import org.edu_sharing.repository.tools.URLHelper;
import org.edu_sharing.service.license.LicenseService;
import org.edu_sharing.service.util.PropertyMapper;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.extensions.surf.util.ISO8601DateFormat;
import org.springframework.stereotype.Component;
import org.w3c.dom.Element;

import javax.xml.parsers.ParserConfigurationException;
import java.time.Duration;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class OaiDublinCoreMetadataFormatWriter extends AbstractMetadataFormatWriter {

    @Value("${repository.privacy.filterVCardEmail:true}")
    protected boolean filterVCardEmail;

    private final MetadataFormat metadataFormat = MetadataFormat.metadataFormat("dc")
            .withSchemaLocation("http://www.openarchives.org/OAI/2.0/oai_dc/ http://www.openarchives.org/OAI/2.0/oai_dc.xsd")
            .withNamespace("http://www.openarchives.org/OAI/2.0/")
            .withTransformer(MetadataFormat.identity());

    @Override
    public void build(Context context) throws ParserConfigurationException {
        PropertyMapper propertyMapper = context.getPropertyMapper();
        Element root = context.createAndAppendElement("oai_dc:dc");
        addNamespace(root);

        context.createAndAppendElement("dc:identifier", root, URLHelper.getNgRenderNodeUrl(context.getNodeId(), null));
        String handleId = propertyMapper.getString(CCConstants.CCM_PROP_PUBLISHED_HANDLE_ID);
        if (StringUtils.isNotBlank(handleId)) {
            context.createAndAppendElement("dc:identifier", root, "info:eu-repo/semantics/altIdentifier/hdl/" + handleId);
        }
        String doiId = propertyMapper.getString(CCConstants.CCM_PROP_PUBLISHED_DOI_ID);
        if (StringUtils.isNotBlank(doiId)) {
            context.createAndAppendElement("dc:identifier", root, "info:eu-repo/semantics/altIdentifier/doi/" + doiId);
        }
        context.createAndAppendElement("dc:identifier", root, "UID:urn:uuid:" + propertyMapper.getString(CCConstants.SYS_PROP_NODE_UID));

        context.createAndAppendElement("dc:title", root, propertyMapper.getString(CCConstants.LOM_PROP_GENERAL_TITLE, propertyMapper.getString(CCConstants.CM_NAME)));
        context.createAndAppendElement("dc:description", root, propertyMapper.getString(CCConstants.LOM_PROP_GENERAL_DESCRIPTION));
        context.createAndAppendElement("dc:language", root, propertyMapper.getString(CCConstants.LOM_PROP_GENERAL_LANGUAGE));
        context.createAndAppendElement("dc:subject", root, propertyMapper.getStringList(CCConstants.LOM_PROP_GENERAL_KEYWORD));
        context.createAndAppendElement("dc:subject", root, propertyMapper.getStringList(CCConstants.CCM_PROP_IO_REPL_CLASSIFICATION_KEYWORD).stream().map(x -> String.format("https://d-nb.info/gnd/%s-%s", x.substring(1, x.length() - 1), x.substring(x.length() - 1))).collect(Collectors.toList()));
        context.createAndAppendElement("dc:subject", root, propertyMapper.getStringList(CCConstants.CCM_PROP_IO_REPL_TAXON_ID));

        context.createAndAppendElement("dc:creator", root, propertyMapper.getString(CCConstants.CCM_PROP_IO_UNIVERSITY));
        context.createAndAppendElement("dc:creator", root, prepareContributor(context, propertyMapper.getStringList(CCConstants.CCM_PROP_IO_REPL_METADATACONTRIBUTER_CREATOR), null));
        context.createAndAppendElement("dc:publisher", root, prepareContributor(context, propertyMapper.getStringList(CCConstants.CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_PUBLISHER), null));

        context.createAndAppendElement("dc:date", root, handleDate(context, CCConstants.CM_PROP_C_CREATED));
        context.createAndAppendElement("dc:date", root, handleDate(context, CCConstants.CCM_PROP_IO_PUBLISHED_DATE));

        context.createAndAppendElement("dc:contributor", root, prepareContributor(context, propertyMapper.getStringList(CCConstants.CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_AUTHOR), "Author"));
        context.createAndAppendElement("dc:contributor", root, prepareContributor(context, propertyMapper.getStringList(CCConstants.CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_CONTENT_PROVIDER), "Content Provider"));
        context.createAndAppendElement("dc:contributor", root, prepareContributor(context, propertyMapper.getStringList(CCConstants.CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_EDITOR), "Editor"));
        context.createAndAppendElement("dc:contributor", root, prepareContributor(context, propertyMapper.getStringList(CCConstants.CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_EDUCATIONAL_VALIDATOR),  "Educational Validator"));
        context.createAndAppendElement("dc:contributor", root, prepareContributor(context, propertyMapper.getStringList(CCConstants.CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_GRAPHICAL_DESIGNER), "Graphical Designer"));
        context.createAndAppendElement("dc:contributor", root, prepareContributor(context, propertyMapper.getStringList(CCConstants.CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_INITIATOR), "Initiator"));
        context.createAndAppendElement("dc:contributor", root, prepareContributor(context, propertyMapper.getStringList(CCConstants.CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_INSTRUCTIONAL_DESIGNER),  "Instructional Designer"));
        context.createAndAppendElement("dc:contributor", root, prepareContributor(context, propertyMapper.getStringList(CCConstants.CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_SCRIPT_WRITER), "Script Writer"));
        context.createAndAppendElement("dc:contributor", root, prepareContributor(context, propertyMapper.getStringList(CCConstants.CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_SUBJECT_MATTER_EXPERT),  "Subject Matter Expert"));
        context.createAndAppendElement("dc:contributor", root, prepareContributor(context, propertyMapper.getStringList(CCConstants.CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_TECHNICAL_IMPLEMENTER),  "Technical Implementer"));
        context.createAndAppendElement("dc:contributor", root, prepareContributor(context, propertyMapper.getStringList(CCConstants.CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_TECHNICAL_VALIDATOR),  "Technical Validator"));
        context.createAndAppendElement("dc:contributor", root, prepareContributor(context, propertyMapper.getStringList(CCConstants.CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_TERMINATOR), "Terminator"));
        context.createAndAppendElement("dc:contributor", root, prepareContributor(context, propertyMapper.getStringList(CCConstants.CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_VALIDATOR), "Validator"));

        context.createAndAppendElement("dc:format", root, propertyMapper.getString(CCConstants.LOM_PROP_TECHNICAL_FORMAT));
        context.createAndAppendElement("dc:source", root, propertyMapper.getString(CCConstants.CCM_PROP_IO_REPLICATIONSOURCE));
        context.createAndAppendElement("dc:type", root, "info:eu-repo/semantics/other");
        context.createAndAppendElement("dc:type", root, propertyMapper.getString(CCConstants.CCM_PROP_IO_REPL_EDUCATIONAL_LEARNINGRESSOURCETYPE));

        Integer tarFrom = propertyMapper.getInteger(CCConstants.CCM_PROP_IO_REPL_EDUCATIONAL_TYPICALAGERANGEFROM);
        Integer tarTo = propertyMapper.getInteger(CCConstants.CCM_PROP_IO_REPL_EDUCATIONAL_TYPICALAGERANGETO);
        if (tarFrom != null && tarTo != null) {
            context.createAndAppendElement("dc:audience", root, String.format("Learner (%s-%s)", tarFrom, tarTo));
        }
        context.createAndAppendElement("dc:audience", root, propertyMapper.getStringList(CCConstants.CCM_PROP_IO_REPL_EDUCATIONAL_INTENDEDENDUSERROLE));


        String commonLicenceKey = propertyMapper.getString(CCConstants.CCM_PROP_IO_COMMONLICENSE_KEY);
        if (StringUtils.isBlank(commonLicenceKey)) {
            context.createAndAppendElement("dc:rights", root, propertyMapper.getBoolean(CCConstants.LOM_PROP_RIGHTS_COPY_RIGHT) ? "info:eu-repo/semantics/restrictedAccess" : "info:eu-repo/semantics/openAccess");
            context.createAndAppendElement("dc:rights", root, propertyMapper.getBoolean(CCConstants.LOM_PROP_RIGHTS_RIGHTS_DESCRIPTION));
        } else {
            String commonLicenseLocale = propertyMapper.getString(CCConstants.CCM_PROP_IO_COMMONLICENSE_CC_LOCALE, Locale.getDefault().getLanguage());
            String commonLicenseVersion = propertyMapper.getString(CCConstants.CCM_PROP_IO_COMMONLICENSE_CC_VERSION);
            String url = new LicenseService().getLicenseUrl(commonLicenceKey, commonLicenseLocale, commonLicenseVersion);

            boolean isOpenAccess = commonLicenseLocale.equals("CC_0") || commonLicenseLocale.equals("PDM");
            context.createAndAppendElement("dc:rights", root, isOpenAccess ? "info:eu-repo/semantics/openAccess" : "info:eu-repo/semantics/restrictedAccess");
            context.createAndAppendElement("dc:rights", root, url);
        }
    }

    protected void addNamespace(Element element) {
        element.setAttribute("xmlns:oai_dc", "http://www.openarchives.org/OAI/2.0/oai_dc/");
        element.setAttribute("xmlns:dc", "http://purl.org/dc/elements/1.1/");
        element.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
        element.setAttribute("xsi:schemaLocation", "http://www.w3.org/2001/XMLSchema-instance");
        element.setAttribute("xsi:schemaLocation", "http://www.openarchives.org/OAI/2.0/oai_dc/ http://www.openarchives.org/OAI/2.0/oai_dc.xsd");
    }

    protected List<String> prepareContributor(Context context, List<String> contrib, String role) {
        Stream<String> stream = contrib.stream()
                //sometimes there are empty values in list
                .filter(StringUtils::isNotBlank)
                .map(VCardConverter::getInvertedNameForVCardString);

        if(StringUtils.isNotBlank(role)){
            stream = stream.map(x->String.format("%s (%s)", x, role));
        }

        return stream.collect(Collectors.toList());
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

    private String handleDate(Context context, String propertyId) {
        PropertyMapper propertyMapper = context.getPropertyMapper();
        String result = null;
        try {
            String date = propertyMapper.getString(propertyId, "");
            if (date.startsWith("P")) {
                Duration duration = Duration.parse(date);
                long seconds = duration.getSeconds();
                result = String.format("%02d:%02d:%02d", (seconds / 3600) % 24, (seconds / 60) % 60, seconds % 60);
            }
        } catch (Exception e) {
            Long date = propertyMapper.getLong(propertyId);
            if (date != null) {
                result = ISO8601DateFormat.format(new Date(date));
            }
        }
        return result;
    }

    @NotNull
    @Override
    public MetadataFormat getFormat() {
        return metadataFormat;
    }
}
