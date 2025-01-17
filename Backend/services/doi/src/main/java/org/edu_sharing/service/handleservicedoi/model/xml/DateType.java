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
 * <p>Java-Klasse für dateType.
 * 
 * <p>Das folgende Schemafragment gibt den erwarteten Content an, der in dieser Klasse enthalten ist.
 * <pre>
 * &lt;simpleType name="dateType"&gt;
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string"&gt;
 *     &lt;enumeration value="Accepted"/&gt;
 *     &lt;enumeration value="Available"/&gt;
 *     &lt;enumeration value="Collected"/&gt;
 *     &lt;enumeration value="Copyrighted"/&gt;
 *     &lt;enumeration value="Coverage"/&gt;
 *     &lt;enumeration value="Created"/&gt;
 *     &lt;enumeration value="Issued"/&gt;
 *     &lt;enumeration value="Other"/&gt;
 *     &lt;enumeration value="Submitted"/&gt;
 *     &lt;enumeration value="Updated"/&gt;
 *     &lt;enumeration value="Valid"/&gt;
 *     &lt;enumeration value="Withdrawn"/&gt;
 *   &lt;/restriction&gt;
 * &lt;/simpleType&gt;
 * </pre>
 * 
 */
@XmlType(name = "dateType")
@XmlEnum
public enum DateType {

    @XmlEnumValue("Accepted")
    ACCEPTED("Accepted"),
    @XmlEnumValue("Available")
    AVAILABLE("Available"),
    @XmlEnumValue("Collected")
    COLLECTED("Collected"),
    @XmlEnumValue("Copyrighted")
    COPYRIGHTED("Copyrighted"),
    @XmlEnumValue("Coverage")
    COVERAGE("Coverage"),
    @XmlEnumValue("Created")
    CREATED("Created"),
    @XmlEnumValue("Issued")
    ISSUED("Issued"),
    @XmlEnumValue("Other")
    OTHER("Other"),
    @XmlEnumValue("Submitted")
    SUBMITTED("Submitted"),
    @XmlEnumValue("Updated")
    UPDATED("Updated"),
    @XmlEnumValue("Valid")
    VALID("Valid"),
    @XmlEnumValue("Withdrawn")
    WITHDRAWN("Withdrawn");
    private final String value;

    DateType(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static DateType fromValue(String v) {
        for (DateType c: DateType.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
