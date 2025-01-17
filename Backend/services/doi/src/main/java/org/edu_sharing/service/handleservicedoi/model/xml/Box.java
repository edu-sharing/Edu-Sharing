//
// Diese Datei wurde mit der Eclipse Implementation of JAXB, v3.0.0 generiert 
// Siehe https://eclipse-ee4j.github.io/jaxb-ri 
// Änderungen an dieser Datei gehen bei einer Neukompilierung des Quellschemas verloren. 
// Generiert: 2025.01.17 um 09:46:02 AM CET 
//


package org.edu_sharing.service.handleservicedoi.model.xml;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;


/**
 * <p>Java-Klasse für box complex type.
 * 
 * <p>Das folgende Schemafragment gibt den erwarteten Content an, der in dieser Klasse enthalten ist.
 * 
 * <pre>
 * &lt;complexType name="box"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;all&gt;
 *         &lt;element name="westBoundLongitude" type="{http://datacite.org/schema/kernel-4}longitudeType"/&gt;
 *         &lt;element name="eastBoundLongitude" type="{http://datacite.org/schema/kernel-4}longitudeType"/&gt;
 *         &lt;element name="southBoundLatitude" type="{http://datacite.org/schema/kernel-4}latitudeType"/&gt;
 *         &lt;element name="northBoundLatitude" type="{http://datacite.org/schema/kernel-4}latitudeType"/&gt;
 *       &lt;/all&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "box", propOrder = {

})
public class Box {

    protected float westBoundLongitude;
    protected float eastBoundLongitude;
    protected float southBoundLatitude;
    protected float northBoundLatitude;

    /**
     * Ruft den Wert der westBoundLongitude-Eigenschaft ab.
     * 
     */
    public float getWestBoundLongitude() {
        return westBoundLongitude;
    }

    /**
     * Legt den Wert der westBoundLongitude-Eigenschaft fest.
     * 
     */
    public void setWestBoundLongitude(float value) {
        this.westBoundLongitude = value;
    }

    /**
     * Ruft den Wert der eastBoundLongitude-Eigenschaft ab.
     * 
     */
    public float getEastBoundLongitude() {
        return eastBoundLongitude;
    }

    /**
     * Legt den Wert der eastBoundLongitude-Eigenschaft fest.
     * 
     */
    public void setEastBoundLongitude(float value) {
        this.eastBoundLongitude = value;
    }

    /**
     * Ruft den Wert der southBoundLatitude-Eigenschaft ab.
     * 
     */
    public float getSouthBoundLatitude() {
        return southBoundLatitude;
    }

    /**
     * Legt den Wert der southBoundLatitude-Eigenschaft fest.
     * 
     */
    public void setSouthBoundLatitude(float value) {
        this.southBoundLatitude = value;
    }

    /**
     * Ruft den Wert der northBoundLatitude-Eigenschaft ab.
     * 
     */
    public float getNorthBoundLatitude() {
        return northBoundLatitude;
    }

    /**
     * Legt den Wert der northBoundLatitude-Eigenschaft fest.
     * 
     */
    public void setNorthBoundLatitude(float value) {
        this.northBoundLatitude = value;
    }

}
