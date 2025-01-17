//
// Diese Datei wurde mit der Eclipse Implementation of JAXB, v3.0.0 generiert 
// Siehe https://eclipse-ee4j.github.io/jaxb-ri 
// Änderungen an dieser Datei gehen bei einer Neukompilierung des Quellschemas verloren. 
// Generiert: 2025.01.17 um 11:00:53 AM CET 
//


package org.edu_sharing.service.handleservicedoi.model.xml;

import jakarta.xml.bind.annotation.XmlEnum;
import jakarta.xml.bind.annotation.XmlEnumValue;
import jakarta.xml.bind.annotation.XmlType;


/**
 * <p>Java-Klasse für resourceType.
 * 
 * <p>Das folgende Schemafragment gibt den erwarteten Content an, der in dieser Klasse enthalten ist.
 * <pre>
 * &lt;simpleType name="resourceType"&gt;
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string"&gt;
 *     &lt;enumeration value="Audiovisual"/&gt;
 *     &lt;enumeration value="Award"/&gt;
 *     &lt;enumeration value="Book"/&gt;
 *     &lt;enumeration value="BookChapter"/&gt;
 *     &lt;enumeration value="Collection"/&gt;
 *     &lt;enumeration value="ComputationalNotebook"/&gt;
 *     &lt;enumeration value="ConferencePaper"/&gt;
 *     &lt;enumeration value="ConferenceProceeding"/&gt;
 *     &lt;enumeration value="DataPaper"/&gt;
 *     &lt;enumeration value="Dataset"/&gt;
 *     &lt;enumeration value="Dissertation"/&gt;
 *     &lt;enumeration value="Event"/&gt;
 *     &lt;enumeration value="Image"/&gt;
 *     &lt;enumeration value="Instrument"/&gt;
 *     &lt;enumeration value="InteractiveResource"/&gt;
 *     &lt;enumeration value="Journal"/&gt;
 *     &lt;enumeration value="JournalArticle"/&gt;
 *     &lt;enumeration value="Model"/&gt;
 *     &lt;enumeration value="OutputManagementPlan"/&gt;
 *     &lt;enumeration value="PeerReview"/&gt;
 *     &lt;enumeration value="PhysicalObject"/&gt;
 *     &lt;enumeration value="Preprint"/&gt;
 *     &lt;enumeration value="Project"/&gt;
 *     &lt;enumeration value="Report"/&gt;
 *     &lt;enumeration value="Service"/&gt;
 *     &lt;enumeration value="Software"/&gt;
 *     &lt;enumeration value="Sound"/&gt;
 *     &lt;enumeration value="Standard"/&gt;
 *     &lt;enumeration value="StudyRegistration"/&gt;
 *     &lt;enumeration value="Text"/&gt;
 *     &lt;enumeration value="Workflow"/&gt;
 *     &lt;enumeration value="Other"/&gt;
 *   &lt;/restriction&gt;
 * &lt;/simpleType&gt;
 * </pre>
 * 
 */
@XmlType(name = "resourceType")
@XmlEnum
public enum ResourceType {

    @XmlEnumValue("Audiovisual")
    AUDIOVISUAL("Audiovisual"),
    @XmlEnumValue("Award")
    AWARD("Award"),
    @XmlEnumValue("Book")
    BOOK("Book"),
    @XmlEnumValue("BookChapter")
    BOOK_CHAPTER("BookChapter"),
    @XmlEnumValue("Collection")
    COLLECTION("Collection"),
    @XmlEnumValue("ComputationalNotebook")
    COMPUTATIONAL_NOTEBOOK("ComputationalNotebook"),
    @XmlEnumValue("ConferencePaper")
    CONFERENCE_PAPER("ConferencePaper"),
    @XmlEnumValue("ConferenceProceeding")
    CONFERENCE_PROCEEDING("ConferenceProceeding"),
    @XmlEnumValue("DataPaper")
    DATA_PAPER("DataPaper"),
    @XmlEnumValue("Dataset")
    DATASET("Dataset"),
    @XmlEnumValue("Dissertation")
    DISSERTATION("Dissertation"),
    @XmlEnumValue("Event")
    EVENT("Event"),
    @XmlEnumValue("Image")
    IMAGE("Image"),
    @XmlEnumValue("Instrument")
    INSTRUMENT("Instrument"),
    @XmlEnumValue("InteractiveResource")
    INTERACTIVE_RESOURCE("InteractiveResource"),
    @XmlEnumValue("Journal")
    JOURNAL("Journal"),
    @XmlEnumValue("JournalArticle")
    JOURNAL_ARTICLE("JournalArticle"),
    @XmlEnumValue("Model")
    MODEL("Model"),
    @XmlEnumValue("OutputManagementPlan")
    OUTPUT_MANAGEMENT_PLAN("OutputManagementPlan"),
    @XmlEnumValue("PeerReview")
    PEER_REVIEW("PeerReview"),
    @XmlEnumValue("PhysicalObject")
    PHYSICAL_OBJECT("PhysicalObject"),
    @XmlEnumValue("Preprint")
    PREPRINT("Preprint"),
    @XmlEnumValue("Project")
    PROJECT("Project"),
    @XmlEnumValue("Report")
    REPORT("Report"),
    @XmlEnumValue("Service")
    SERVICE("Service"),
    @XmlEnumValue("Software")
    SOFTWARE("Software"),
    @XmlEnumValue("Sound")
    SOUND("Sound"),
    @XmlEnumValue("Standard")
    STANDARD("Standard"),
    @XmlEnumValue("StudyRegistration")
    STUDY_REGISTRATION("StudyRegistration"),
    @XmlEnumValue("Text")
    TEXT("Text"),
    @XmlEnumValue("Workflow")
    WORKFLOW("Workflow"),
    @XmlEnumValue("Other")
    OTHER("Other");
    private final String value;

    ResourceType(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static ResourceType fromValue(String v) {
        for (ResourceType c: ResourceType.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
