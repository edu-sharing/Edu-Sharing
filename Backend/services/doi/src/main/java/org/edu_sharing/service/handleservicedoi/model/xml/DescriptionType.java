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
 * <p>Java-Klasse für descriptionType.
 * 
 * <p>Das folgende Schemafragment gibt den erwarteten Content an, der in dieser Klasse enthalten ist.
 * <pre>
 * &lt;simpleType name="descriptionType"&gt;
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string"&gt;
 *     &lt;enumeration value="Abstract"/&gt;
 *     &lt;enumeration value="Methods"/&gt;
 *     &lt;enumeration value="SeriesInformation"/&gt;
 *     &lt;enumeration value="TableOfContents"/&gt;
 *     &lt;enumeration value="TechnicalInfo"/&gt;
 *     &lt;enumeration value="Other"/&gt;
 *   &lt;/restriction&gt;
 * &lt;/simpleType&gt;
 * </pre>
 * 
 */
@XmlType(name = "descriptionType")
@XmlEnum
public enum DescriptionType {

    @XmlEnumValue("Abstract")
    ABSTRACT("Abstract"),
    @XmlEnumValue("Methods")
    METHODS("Methods"),
    @XmlEnumValue("SeriesInformation")
    SERIES_INFORMATION("SeriesInformation"),
    @XmlEnumValue("TableOfContents")
    TABLE_OF_CONTENTS("TableOfContents"),
    @XmlEnumValue("TechnicalInfo")
    TECHNICAL_INFO("TechnicalInfo"),
    @XmlEnumValue("Other")
    OTHER("Other");
    private final String value;

    DescriptionType(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static DescriptionType fromValue(String v) {
        for (DescriptionType c: DescriptionType.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
