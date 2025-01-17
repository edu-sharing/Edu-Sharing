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
 * <p>Java-Klasse für titleType.
 * 
 * <p>Das folgende Schemafragment gibt den erwarteten Content an, der in dieser Klasse enthalten ist.
 * <pre>
 * &lt;simpleType name="titleType"&gt;
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string"&gt;
 *     &lt;enumeration value="AlternativeTitle"/&gt;
 *     &lt;enumeration value="Subtitle"/&gt;
 *     &lt;enumeration value="TranslatedTitle"/&gt;
 *     &lt;enumeration value="Other"/&gt;
 *   &lt;/restriction&gt;
 * &lt;/simpleType&gt;
 * </pre>
 * 
 */
@XmlType(name = "titleType")
@XmlEnum
public enum TitleType {

    @XmlEnumValue("AlternativeTitle")
    ALTERNATIVE_TITLE("AlternativeTitle"),
    @XmlEnumValue("Subtitle")
    SUBTITLE("Subtitle"),
    @XmlEnumValue("TranslatedTitle")
    TRANSLATED_TITLE("TranslatedTitle"),
    @XmlEnumValue("Other")
    OTHER("Other");
    private final String value;

    TitleType(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static TitleType fromValue(String v) {
        for (TitleType c: TitleType.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
