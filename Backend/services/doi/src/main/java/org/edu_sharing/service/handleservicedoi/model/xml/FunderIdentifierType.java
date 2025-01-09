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
 * <p>Java-Klasse für funderIdentifierType.
 * 
 * <p>Das folgende Schemafragment gibt den erwarteten Content an, der in dieser Klasse enthalten ist.
 * <pre>
 * &lt;simpleType name="funderIdentifierType"&gt;
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string"&gt;
 *     &lt;enumeration value="ISNI"/&gt;
 *     &lt;enumeration value="GRID"/&gt;
 *     &lt;enumeration value="ROR"/&gt;
 *     &lt;enumeration value="Crossref Funder ID"/&gt;
 *     &lt;enumeration value="Other"/&gt;
 *   &lt;/restriction&gt;
 * &lt;/simpleType&gt;
 * </pre>
 * 
 */
@XmlType(name = "funderIdentifierType")
@XmlEnum
public enum FunderIdentifierType {

    ISNI("ISNI"),
    GRID("GRID"),
    ROR("ROR"),
    @XmlEnumValue("Crossref Funder ID")
    CROSSREF_FUNDER_ID("Crossref Funder ID"),
    @XmlEnumValue("Other")
    OTHER("Other");
    private final String value;

    FunderIdentifierType(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static FunderIdentifierType fromValue(String v) {
        for (FunderIdentifierType c: FunderIdentifierType.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
