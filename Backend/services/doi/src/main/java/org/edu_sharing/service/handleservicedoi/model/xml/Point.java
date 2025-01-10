//
// Diese Datei wurde mit der Eclipse Implementation of JAXB, v3.0.0 generiert 
// Siehe https://eclipse-ee4j.github.io/jaxb-ri 
// Änderungen an dieser Datei gehen bei einer Neukompilierung des Quellschemas verloren. 
// Generiert: 2025.01.08 um 08:27:34 PM CET 
//


package org.edu_sharing.service.handleservicedoi.model.xml;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;


/**
 * <p>Java-Klasse für point complex type.
 * 
 * <p>Das folgende Schemafragment gibt den erwarteten Content an, der in dieser Klasse enthalten ist.
 * 
 * <pre>
 * &lt;complexType name="point"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;all&gt;
 *         &lt;element name="pointLongitude" type="{http://datacite.org/schema/kernel-4}longitudeType"/&gt;
 *         &lt;element name="pointLatitude" type="{http://datacite.org/schema/kernel-4}latitudeType"/&gt;
 *       &lt;/all&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "point", propOrder = {

})
public class Point {

    protected float pointLongitude;
    protected float pointLatitude;

    /**
     * Ruft den Wert der pointLongitude-Eigenschaft ab.
     * 
     */
    public float getPointLongitude() {
        return pointLongitude;
    }

    /**
     * Legt den Wert der pointLongitude-Eigenschaft fest.
     * 
     */
    public void setPointLongitude(float value) {
        this.pointLongitude = value;
    }

    /**
     * Ruft den Wert der pointLatitude-Eigenschaft ab.
     * 
     */
    public float getPointLatitude() {
        return pointLatitude;
    }

    /**
     * Legt den Wert der pointLatitude-Eigenschaft fest.
     * 
     */
    public void setPointLatitude(float value) {
        this.pointLatitude = value;
    }

}
