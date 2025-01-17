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
 * <p>Java-Klasse für nameType.
 * 
 * <p>Das folgende Schemafragment gibt den erwarteten Content an, der in dieser Klasse enthalten ist.
 * <pre>
 * &lt;simpleType name="nameType"&gt;
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string"&gt;
 *     &lt;enumeration value="Organizational"/&gt;
 *     &lt;enumeration value="Personal"/&gt;
 *   &lt;/restriction&gt;
 * &lt;/simpleType&gt;
 * </pre>
 * 
 */
@XmlType(name = "nameType")
@XmlEnum
public enum NameType {

    @XmlEnumValue("Organizational")
    ORGANIZATIONAL("Organizational"),
    @XmlEnumValue("Personal")
    PERSONAL("Personal");
    private final String value;

    NameType(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static NameType fromValue(String v) {
        for (NameType c: NameType.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
