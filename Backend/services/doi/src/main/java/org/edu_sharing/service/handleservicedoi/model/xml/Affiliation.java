//
// Diese Datei wurde mit der Eclipse Implementation of JAXB, v3.0.0 generiert 
// Siehe https://eclipse-ee4j.github.io/jaxb-ri 
// Änderungen an dieser Datei gehen bei einer Neukompilierung des Quellschemas verloren. 
// Generiert: 2025.01.08 um 08:27:34 PM CET 
//


package org.edu_sharing.service.handleservicedoi.model.xml;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlSchemaType;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.XmlValue;


/**
 * Uniquely identifies an affiliation, according to various identifier schemes.
 * 
 * <p>Java-Klasse für affiliation complex type.
 * 
 * <p>Das folgende Schemafragment gibt den erwarteten Content an, der in dieser Klasse enthalten ist.
 * 
 * <pre>
 * &lt;complexType name="affiliation"&gt;
 *   &lt;simpleContent&gt;
 *     &lt;extension base="&lt;http://datacite.org/schema/kernel-4&gt;nonemptycontentStringType"&gt;
 *       &lt;attribute name="affiliationIdentifier" type="{http://www.w3.org/2001/XMLSchema}string" /&gt;
 *       &lt;attribute name="affiliationIdentifierScheme" type="{http://www.w3.org/2001/XMLSchema}string" /&gt;
 *       &lt;attribute name="schemeURI" type="{http://www.w3.org/2001/XMLSchema}anyURI" /&gt;
 *     &lt;/extension&gt;
 *   &lt;/simpleContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "affiliation", propOrder = {
    "value"
})
public class Affiliation {

    @XmlValue
    protected String value;
    @XmlAttribute(name = "affiliationIdentifier")
    protected String affiliationIdentifier;
    @XmlAttribute(name = "affiliationIdentifierScheme")
    protected String affiliationIdentifierScheme;
    @XmlAttribute(name = "schemeURI")
    @XmlSchemaType(name = "anyURI")
    protected String schemeURI;

    /**
     * Ruft den Wert der value-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getValue() {
        return value;
    }

    /**
     * Legt den Wert der value-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setValue(String value) {
        this.value = value;
    }

    /**
     * Ruft den Wert der affiliationIdentifier-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getAffiliationIdentifier() {
        return affiliationIdentifier;
    }

    /**
     * Legt den Wert der affiliationIdentifier-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setAffiliationIdentifier(String value) {
        this.affiliationIdentifier = value;
    }

    /**
     * Ruft den Wert der affiliationIdentifierScheme-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getAffiliationIdentifierScheme() {
        return affiliationIdentifierScheme;
    }

    /**
     * Legt den Wert der affiliationIdentifierScheme-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setAffiliationIdentifierScheme(String value) {
        this.affiliationIdentifierScheme = value;
    }

    /**
     * Ruft den Wert der schemeURI-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getSchemeURI() {
        return schemeURI;
    }

    /**
     * Legt den Wert der schemeURI-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setSchemeURI(String value) {
        this.schemeURI = value;
    }

}
