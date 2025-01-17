//
// Diese Datei wurde mit der Eclipse Implementation of JAXB, v3.0.0 generiert 
// Siehe https://eclipse-ee4j.github.io/jaxb-ri 
// Änderungen an dieser Datei gehen bei einer Neukompilierung des Quellschemas verloren. 
// Generiert: 2025.01.17 um 09:46:02 AM CET 
//


package org.edu_sharing.service.handleservicedoi.model.xml;

import jakarta.xml.bind.annotation.XmlEnum;
import jakarta.xml.bind.annotation.XmlEnumValue;
import jakarta.xml.bind.annotation.XmlType;


/**
 * <p>Java-Klasse für numberType.
 * 
 * <p>Das folgende Schemafragment gibt den erwarteten Content an, der in dieser Klasse enthalten ist.
 * <pre>
 * &lt;simpleType name="numberType"&gt;
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string"&gt;
 *     &lt;enumeration value="Article"/&gt;
 *     &lt;enumeration value="Chapter"/&gt;
 *     &lt;enumeration value="Report"/&gt;
 *     &lt;enumeration value="Other"/&gt;
 *   &lt;/restriction&gt;
 * &lt;/simpleType&gt;
 * </pre>
 * 
 */
@XmlType(name = "numberType")
@XmlEnum
public enum NumberType {

    @XmlEnumValue("Article")
    ARTICLE("Article"),
    @XmlEnumValue("Chapter")
    CHAPTER("Chapter"),
    @XmlEnumValue("Report")
    REPORT("Report"),
    @XmlEnumValue("Other")
    OTHER("Other");
    private final String value;

    NumberType(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static NumberType fromValue(String v) {
        for (NumberType c: NumberType.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
