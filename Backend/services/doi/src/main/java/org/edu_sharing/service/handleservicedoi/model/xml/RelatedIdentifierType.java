//
// Diese Datei wurde mit der Eclipse Implementation of JAXB, v3.0.0 generiert 
// Siehe https://eclipse-ee4j.github.io/jaxb-ri 
// Änderungen an dieser Datei gehen bei einer Neukompilierung des Quellschemas verloren. 
// Generiert: 2025.01.08 um 08:27:34 PM CET 
//


package org.edu_sharing.service.handleservicedoi.model.xml;

import jakarta.xml.bind.annotation.XmlEnum;
import jakarta.xml.bind.annotation.XmlEnumValue;
import jakarta.xml.bind.annotation.XmlType;


/**
 * <p>Java-Klasse für relatedIdentifierType.
 * 
 * <p>Das folgende Schemafragment gibt den erwarteten Content an, der in dieser Klasse enthalten ist.
 * <pre>
 * &lt;simpleType name="relatedIdentifierType"&gt;
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string"&gt;
 *     &lt;enumeration value="ARK"/&gt;
 *     &lt;enumeration value="arXiv"/&gt;
 *     &lt;enumeration value="bibcode"/&gt;
 *     &lt;enumeration value="CSTR"/&gt;
 *     &lt;enumeration value="DOI"/&gt;
 *     &lt;enumeration value="EAN13"/&gt;
 *     &lt;enumeration value="EISSN"/&gt;
 *     &lt;enumeration value="Handle"/&gt;
 *     &lt;enumeration value="IGSN"/&gt;
 *     &lt;enumeration value="ISBN"/&gt;
 *     &lt;enumeration value="ISSN"/&gt;
 *     &lt;enumeration value="ISTC"/&gt;
 *     &lt;enumeration value="LISSN"/&gt;
 *     &lt;enumeration value="LSID"/&gt;
 *     &lt;enumeration value="PMID"/&gt;
 *     &lt;enumeration value="PURL"/&gt;
 *     &lt;enumeration value="RRID"/&gt;
 *     &lt;enumeration value="UPC"/&gt;
 *     &lt;enumeration value="URL"/&gt;
 *     &lt;enumeration value="URN"/&gt;
 *     &lt;enumeration value="w3id"/&gt;
 *   &lt;/restriction&gt;
 * &lt;/simpleType&gt;
 * </pre>
 * 
 */
@XmlType(name = "relatedIdentifierType")
@XmlEnum
public enum RelatedIdentifierType {

    ARK("ARK"),
    @XmlEnumValue("arXiv")
    AR_XIV("arXiv"),
    @XmlEnumValue("bibcode")
    BIBCODE("bibcode"),
    CSTR("CSTR"),
    DOI("DOI"),
    @XmlEnumValue("EAN13")
    EAN_13("EAN13"),
    EISSN("EISSN"),
    @XmlEnumValue("Handle")
    HANDLE("Handle"),
    IGSN("IGSN"),
    ISBN("ISBN"),
    ISSN("ISSN"),
    ISTC("ISTC"),
    LISSN("LISSN"),
    LSID("LSID"),
    PMID("PMID"),
    PURL("PURL"),
    RRID("RRID"),
    UPC("UPC"),
    URL("URL"),
    URN("URN"),
    @XmlEnumValue("w3id")
    W_3_ID("w3id");
    private final String value;

    RelatedIdentifierType(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static RelatedIdentifierType fromValue(String v) {
        for (RelatedIdentifierType c: RelatedIdentifierType.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
