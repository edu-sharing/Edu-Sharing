//
// Diese Datei wurde mit der Eclipse Implementation of JAXB, v3.0.0 generiert 
// Siehe https://eclipse-ee4j.github.io/jaxb-ri 
// Änderungen an dieser Datei gehen bei einer Neukompilierung des Quellschemas verloren. 
// Generiert: 2025.01.17 um 09:46:02 AM CET 
//


package org.edu_sharing.service.handleservicedoi.model.xml;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementRef;
import jakarta.xml.bind.annotation.XmlElements;
import jakarta.xml.bind.annotation.XmlMixed;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlSchemaType;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.XmlValue;
import jakarta.xml.bind.annotation.adapters.CollapsedStringAdapter;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;


/**
 * <p>Java-Klasse für anonymous complex type.
 * 
 * <p>Das folgende Schemafragment gibt den erwarteten Content an, der in dieser Klasse enthalten ist.
 * 
 * <pre>
 * &lt;complexType&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;all&gt;
 *         &lt;element name="identifier"&gt;
 *           &lt;complexType&gt;
 *             &lt;simpleContent&gt;
 *               &lt;extension base="&lt;http://datacite.org/schema/kernel-4&gt;nonemptycontentStringType"&gt;
 *                 &lt;attribute name="identifierType" use="required" type="{http://www.w3.org/2001/XMLSchema}anySimpleType" /&gt;
 *               &lt;/extension&gt;
 *             &lt;/simpleContent&gt;
 *           &lt;/complexType&gt;
 *         &lt;/element&gt;
 *         &lt;element name="creators"&gt;
 *           &lt;complexType&gt;
 *             &lt;complexContent&gt;
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *                 &lt;sequence&gt;
 *                   &lt;element name="creator" maxOccurs="unbounded"&gt;
 *                     &lt;complexType&gt;
 *                       &lt;complexContent&gt;
 *                         &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *                           &lt;sequence&gt;
 *                             &lt;element name="creatorName"&gt;
 *                               &lt;complexType&gt;
 *                                 &lt;simpleContent&gt;
 *                                   &lt;extension base="&lt;http://www.w3.org/2001/XMLSchema&gt;string"&gt;
 *                                     &lt;attribute name="nameType" type="{http://datacite.org/schema/kernel-4}nameType" /&gt;
 *                                     &lt;attribute ref="{http://www.w3.org/XML/1998/namespace}lang"/&gt;
 *                                   &lt;/extension&gt;
 *                                 &lt;/simpleContent&gt;
 *                               &lt;/complexType&gt;
 *                             &lt;/element&gt;
 *                             &lt;element name="givenName" type="{http://www.w3.org/2001/XMLSchema}anyType" minOccurs="0"/&gt;
 *                             &lt;element name="familyName" type="{http://www.w3.org/2001/XMLSchema}anyType" minOccurs="0"/&gt;
 *                             &lt;element name="nameIdentifier" type="{http://www.w3.org/2001/XMLSchema}anyType" maxOccurs="unbounded" minOccurs="0"/&gt;
 *                             &lt;element name="affiliation" type="{http://www.w3.org/2001/XMLSchema}anyType" maxOccurs="unbounded" minOccurs="0"/&gt;
 *                           &lt;/sequence&gt;
 *                         &lt;/restriction&gt;
 *                       &lt;/complexContent&gt;
 *                     &lt;/complexType&gt;
 *                   &lt;/element&gt;
 *                 &lt;/sequence&gt;
 *               &lt;/restriction&gt;
 *             &lt;/complexContent&gt;
 *           &lt;/complexType&gt;
 *         &lt;/element&gt;
 *         &lt;element name="titles"&gt;
 *           &lt;complexType&gt;
 *             &lt;complexContent&gt;
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *                 &lt;sequence&gt;
 *                   &lt;element name="title" maxOccurs="unbounded"&gt;
 *                     &lt;complexType&gt;
 *                       &lt;simpleContent&gt;
 *                         &lt;extension base="&lt;http://www.w3.org/2001/XMLSchema&gt;string"&gt;
 *                           &lt;attribute name="titleType" type="{http://datacite.org/schema/kernel-4}titleType" /&gt;
 *                           &lt;attribute ref="{http://www.w3.org/XML/1998/namespace}lang"/&gt;
 *                         &lt;/extension&gt;
 *                       &lt;/simpleContent&gt;
 *                     &lt;/complexType&gt;
 *                   &lt;/element&gt;
 *                 &lt;/sequence&gt;
 *               &lt;/restriction&gt;
 *             &lt;/complexContent&gt;
 *           &lt;/complexType&gt;
 *         &lt;/element&gt;
 *         &lt;element name="publisher"&gt;
 *           &lt;complexType&gt;
 *             &lt;simpleContent&gt;
 *               &lt;extension base="&lt;http://datacite.org/schema/kernel-4&gt;nonemptycontentStringType"&gt;
 *                 &lt;attribute name="publisherIdentifier" type="{http://www.w3.org/2001/XMLSchema}string" /&gt;
 *                 &lt;attribute name="publisherIdentifierScheme" type="{http://www.w3.org/2001/XMLSchema}string" /&gt;
 *                 &lt;attribute name="schemeURI" type="{http://www.w3.org/2001/XMLSchema}anyURI" /&gt;
 *                 &lt;attribute ref="{http://www.w3.org/XML/1998/namespace}lang"/&gt;
 *               &lt;/extension&gt;
 *             &lt;/simpleContent&gt;
 *           &lt;/complexType&gt;
 *         &lt;/element&gt;
 *         &lt;element name="publicationYear"&gt;
 *           &lt;simpleType&gt;
 *             &lt;restriction base="{http://datacite.org/schema/kernel-4}yearType"&gt;
 *             &lt;/restriction&gt;
 *           &lt;/simpleType&gt;
 *         &lt;/element&gt;
 *         &lt;element name="resourceType"&gt;
 *           &lt;complexType&gt;
 *             &lt;simpleContent&gt;
 *               &lt;extension base="&lt;http://www.w3.org/2001/XMLSchema&gt;string"&gt;
 *                 &lt;attribute name="resourceTypeGeneral" use="required" type="{http://datacite.org/schema/kernel-4}resourceType" /&gt;
 *               &lt;/extension&gt;
 *             &lt;/simpleContent&gt;
 *           &lt;/complexType&gt;
 *         &lt;/element&gt;
 *         &lt;element name="subjects" minOccurs="0"&gt;
 *           &lt;complexType&gt;
 *             &lt;complexContent&gt;
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *                 &lt;sequence&gt;
 *                   &lt;element name="subject" maxOccurs="unbounded" minOccurs="0"&gt;
 *                     &lt;complexType&gt;
 *                       &lt;simpleContent&gt;
 *                         &lt;extension base="&lt;http://www.w3.org/2001/XMLSchema&gt;string"&gt;
 *                           &lt;attribute name="subjectScheme" type="{http://www.w3.org/2001/XMLSchema}anySimpleType" /&gt;
 *                           &lt;attribute name="schemeURI" type="{http://www.w3.org/2001/XMLSchema}anyURI" /&gt;
 *                           &lt;attribute name="valueURI" type="{http://www.w3.org/2001/XMLSchema}anyURI" /&gt;
 *                           &lt;attribute name="classificationCode" type="{http://www.w3.org/2001/XMLSchema}anyURI" /&gt;
 *                           &lt;attribute ref="{http://www.w3.org/XML/1998/namespace}lang"/&gt;
 *                         &lt;/extension&gt;
 *                       &lt;/simpleContent&gt;
 *                     &lt;/complexType&gt;
 *                   &lt;/element&gt;
 *                 &lt;/sequence&gt;
 *               &lt;/restriction&gt;
 *             &lt;/complexContent&gt;
 *           &lt;/complexType&gt;
 *         &lt;/element&gt;
 *         &lt;element name="contributors" minOccurs="0"&gt;
 *           &lt;complexType&gt;
 *             &lt;complexContent&gt;
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *                 &lt;sequence&gt;
 *                   &lt;element name="contributor" maxOccurs="unbounded" minOccurs="0"&gt;
 *                     &lt;complexType&gt;
 *                       &lt;complexContent&gt;
 *                         &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *                           &lt;sequence&gt;
 *                             &lt;element name="contributorName"&gt;
 *                               &lt;complexType&gt;
 *                                 &lt;simpleContent&gt;
 *                                   &lt;extension base="&lt;http://datacite.org/schema/kernel-4&gt;nonemptycontentStringType"&gt;
 *                                     &lt;attribute name="nameType" type="{http://datacite.org/schema/kernel-4}nameType" /&gt;
 *                                     &lt;attribute ref="{http://www.w3.org/XML/1998/namespace}lang"/&gt;
 *                                   &lt;/extension&gt;
 *                                 &lt;/simpleContent&gt;
 *                               &lt;/complexType&gt;
 *                             &lt;/element&gt;
 *                             &lt;element name="givenName" type="{http://www.w3.org/2001/XMLSchema}anyType" minOccurs="0"/&gt;
 *                             &lt;element name="familyName" type="{http://www.w3.org/2001/XMLSchema}anyType" minOccurs="0"/&gt;
 *                             &lt;element name="nameIdentifier" type="{http://www.w3.org/2001/XMLSchema}anyType" maxOccurs="unbounded" minOccurs="0"/&gt;
 *                             &lt;element name="affiliation" type="{http://www.w3.org/2001/XMLSchema}anyType" maxOccurs="unbounded" minOccurs="0"/&gt;
 *                           &lt;/sequence&gt;
 *                           &lt;attribute name="contributorType" use="required" type="{http://datacite.org/schema/kernel-4}contributorType" /&gt;
 *                         &lt;/restriction&gt;
 *                       &lt;/complexContent&gt;
 *                     &lt;/complexType&gt;
 *                   &lt;/element&gt;
 *                 &lt;/sequence&gt;
 *               &lt;/restriction&gt;
 *             &lt;/complexContent&gt;
 *           &lt;/complexType&gt;
 *         &lt;/element&gt;
 *         &lt;element name="dates" minOccurs="0"&gt;
 *           &lt;complexType&gt;
 *             &lt;complexContent&gt;
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *                 &lt;sequence&gt;
 *                   &lt;element name="date" maxOccurs="unbounded" minOccurs="0"&gt;
 *                     &lt;complexType&gt;
 *                       &lt;simpleContent&gt;
 *                         &lt;extension base="&lt;http://www.w3.org/2001/XMLSchema&gt;string"&gt;
 *                           &lt;attribute name="dateType" use="required" type="{http://datacite.org/schema/kernel-4}dateType" /&gt;
 *                           &lt;attribute name="dateInformation" type="{http://www.w3.org/2001/XMLSchema}anySimpleType" /&gt;
 *                         &lt;/extension&gt;
 *                       &lt;/simpleContent&gt;
 *                     &lt;/complexType&gt;
 *                   &lt;/element&gt;
 *                 &lt;/sequence&gt;
 *               &lt;/restriction&gt;
 *             &lt;/complexContent&gt;
 *           &lt;/complexType&gt;
 *         &lt;/element&gt;
 *         &lt;element name="language" type="{http://www.w3.org/2001/XMLSchema}language" minOccurs="0"/&gt;
 *         &lt;element name="alternateIdentifiers" minOccurs="0"&gt;
 *           &lt;complexType&gt;
 *             &lt;complexContent&gt;
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *                 &lt;sequence&gt;
 *                   &lt;element name="alternateIdentifier" maxOccurs="unbounded" minOccurs="0"&gt;
 *                     &lt;complexType&gt;
 *                       &lt;simpleContent&gt;
 *                         &lt;extension base="&lt;http://www.w3.org/2001/XMLSchema&gt;string"&gt;
 *                           &lt;attribute name="alternateIdentifierType" use="required" type="{http://www.w3.org/2001/XMLSchema}anySimpleType" /&gt;
 *                         &lt;/extension&gt;
 *                       &lt;/simpleContent&gt;
 *                     &lt;/complexType&gt;
 *                   &lt;/element&gt;
 *                 &lt;/sequence&gt;
 *               &lt;/restriction&gt;
 *             &lt;/complexContent&gt;
 *           &lt;/complexType&gt;
 *         &lt;/element&gt;
 *         &lt;element name="relatedIdentifiers" minOccurs="0"&gt;
 *           &lt;complexType&gt;
 *             &lt;complexContent&gt;
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *                 &lt;sequence&gt;
 *                   &lt;element name="relatedIdentifier" maxOccurs="unbounded" minOccurs="0"&gt;
 *                     &lt;complexType&gt;
 *                       &lt;simpleContent&gt;
 *                         &lt;extension base="&lt;http://www.w3.org/2001/XMLSchema&gt;string"&gt;
 *                           &lt;attribute name="resourceTypeGeneral" type="{http://datacite.org/schema/kernel-4}resourceType" /&gt;
 *                           &lt;attribute name="relatedIdentifierType" use="required" type="{http://datacite.org/schema/kernel-4}relatedIdentifierType" /&gt;
 *                           &lt;attribute name="relationType" use="required" type="{http://datacite.org/schema/kernel-4}relationType" /&gt;
 *                           &lt;attribute name="relatedMetadataScheme" type="{http://www.w3.org/2001/XMLSchema}anySimpleType" /&gt;
 *                           &lt;attribute name="schemeURI" type="{http://www.w3.org/2001/XMLSchema}anyURI" /&gt;
 *                           &lt;attribute name="schemeType" type="{http://www.w3.org/2001/XMLSchema}anySimpleType" /&gt;
 *                         &lt;/extension&gt;
 *                       &lt;/simpleContent&gt;
 *                     &lt;/complexType&gt;
 *                   &lt;/element&gt;
 *                 &lt;/sequence&gt;
 *               &lt;/restriction&gt;
 *             &lt;/complexContent&gt;
 *           &lt;/complexType&gt;
 *         &lt;/element&gt;
 *         &lt;element name="sizes" minOccurs="0"&gt;
 *           &lt;complexType&gt;
 *             &lt;complexContent&gt;
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *                 &lt;sequence&gt;
 *                   &lt;element name="size" type="{http://www.w3.org/2001/XMLSchema}string" maxOccurs="unbounded" minOccurs="0"/&gt;
 *                 &lt;/sequence&gt;
 *               &lt;/restriction&gt;
 *             &lt;/complexContent&gt;
 *           &lt;/complexType&gt;
 *         &lt;/element&gt;
 *         &lt;element name="formats" minOccurs="0"&gt;
 *           &lt;complexType&gt;
 *             &lt;complexContent&gt;
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *                 &lt;sequence&gt;
 *                   &lt;element name="format" type="{http://www.w3.org/2001/XMLSchema}string" maxOccurs="unbounded" minOccurs="0"/&gt;
 *                 &lt;/sequence&gt;
 *               &lt;/restriction&gt;
 *             &lt;/complexContent&gt;
 *           &lt;/complexType&gt;
 *         &lt;/element&gt;
 *         &lt;element name="version" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="rightsList" minOccurs="0"&gt;
 *           &lt;complexType&gt;
 *             &lt;complexContent&gt;
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *                 &lt;sequence&gt;
 *                   &lt;element name="rights" maxOccurs="unbounded" minOccurs="0"&gt;
 *                     &lt;complexType&gt;
 *                       &lt;simpleContent&gt;
 *                         &lt;extension base="&lt;http://www.w3.org/2001/XMLSchema&gt;string"&gt;
 *                           &lt;attribute name="rightsURI" type="{http://www.w3.org/2001/XMLSchema}anyURI" /&gt;
 *                           &lt;attribute name="rightsIdentifier" type="{http://www.w3.org/2001/XMLSchema}anySimpleType" /&gt;
 *                           &lt;attribute name="rightsIdentifierScheme" type="{http://www.w3.org/2001/XMLSchema}anySimpleType" /&gt;
 *                           &lt;attribute name="schemeURI" type="{http://www.w3.org/2001/XMLSchema}anyURI" /&gt;
 *                           &lt;attribute ref="{http://www.w3.org/XML/1998/namespace}lang"/&gt;
 *                         &lt;/extension&gt;
 *                       &lt;/simpleContent&gt;
 *                     &lt;/complexType&gt;
 *                   &lt;/element&gt;
 *                 &lt;/sequence&gt;
 *               &lt;/restriction&gt;
 *             &lt;/complexContent&gt;
 *           &lt;/complexType&gt;
 *         &lt;/element&gt;
 *         &lt;element name="descriptions" minOccurs="0"&gt;
 *           &lt;complexType&gt;
 *             &lt;complexContent&gt;
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *                 &lt;sequence&gt;
 *                   &lt;element name="description" maxOccurs="unbounded" minOccurs="0"&gt;
 *                     &lt;complexType&gt;
 *                       &lt;complexContent&gt;
 *                         &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *                           &lt;choice&gt;
 *                             &lt;element name="br" maxOccurs="unbounded" minOccurs="0"&gt;
 *                               &lt;complexType&gt;
 *                                 &lt;complexContent&gt;
 *                                   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *                                   &lt;/restriction&gt;
 *                                 &lt;/complexContent&gt;
 *                               &lt;/complexType&gt;
 *                             &lt;/element&gt;
 *                           &lt;/choice&gt;
 *                           &lt;attribute name="descriptionType" use="required" type="{http://datacite.org/schema/kernel-4}descriptionType" /&gt;
 *                           &lt;attribute ref="{http://www.w3.org/XML/1998/namespace}lang"/&gt;
 *                         &lt;/restriction&gt;
 *                       &lt;/complexContent&gt;
 *                     &lt;/complexType&gt;
 *                   &lt;/element&gt;
 *                 &lt;/sequence&gt;
 *               &lt;/restriction&gt;
 *             &lt;/complexContent&gt;
 *           &lt;/complexType&gt;
 *         &lt;/element&gt;
 *         &lt;element name="geoLocations" minOccurs="0"&gt;
 *           &lt;complexType&gt;
 *             &lt;complexContent&gt;
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *                 &lt;sequence&gt;
 *                   &lt;element name="geoLocation" maxOccurs="unbounded" minOccurs="0"&gt;
 *                     &lt;complexType&gt;
 *                       &lt;complexContent&gt;
 *                         &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *                           &lt;choice maxOccurs="unbounded"&gt;
 *                             &lt;element name="geoLocationPlace" type="{http://www.w3.org/2001/XMLSchema}anyType" minOccurs="0"/&gt;
 *                             &lt;element name="geoLocationPoint" type="{http://datacite.org/schema/kernel-4}point" minOccurs="0"/&gt;
 *                             &lt;element name="geoLocationBox" type="{http://datacite.org/schema/kernel-4}box" minOccurs="0"/&gt;
 *                             &lt;element name="geoLocationPolygon" maxOccurs="unbounded" minOccurs="0"&gt;
 *                               &lt;complexType&gt;
 *                                 &lt;complexContent&gt;
 *                                   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *                                     &lt;sequence&gt;
 *                                       &lt;element name="polygonPoint" type="{http://datacite.org/schema/kernel-4}point" maxOccurs="unbounded" minOccurs="4"/&gt;
 *                                       &lt;element name="inPolygonPoint" type="{http://datacite.org/schema/kernel-4}point" minOccurs="0"/&gt;
 *                                     &lt;/sequence&gt;
 *                                   &lt;/restriction&gt;
 *                                 &lt;/complexContent&gt;
 *                               &lt;/complexType&gt;
 *                             &lt;/element&gt;
 *                           &lt;/choice&gt;
 *                         &lt;/restriction&gt;
 *                       &lt;/complexContent&gt;
 *                     &lt;/complexType&gt;
 *                   &lt;/element&gt;
 *                 &lt;/sequence&gt;
 *               &lt;/restriction&gt;
 *             &lt;/complexContent&gt;
 *           &lt;/complexType&gt;
 *         &lt;/element&gt;
 *         &lt;element name="fundingReferences" minOccurs="0"&gt;
 *           &lt;complexType&gt;
 *             &lt;complexContent&gt;
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *                 &lt;sequence&gt;
 *                   &lt;element name="fundingReference" maxOccurs="unbounded" minOccurs="0"&gt;
 *                     &lt;complexType&gt;
 *                       &lt;complexContent&gt;
 *                         &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *                           &lt;all&gt;
 *                             &lt;element name="funderName"&gt;
 *                               &lt;simpleType&gt;
 *                                 &lt;restriction base="{http://datacite.org/schema/kernel-4}nonemptycontentStringType"&gt;
 *                                 &lt;/restriction&gt;
 *                               &lt;/simpleType&gt;
 *                             &lt;/element&gt;
 *                             &lt;element name="funderIdentifier" minOccurs="0"&gt;
 *                               &lt;complexType&gt;
 *                                 &lt;simpleContent&gt;
 *                                   &lt;extension base="&lt;http://www.w3.org/2001/XMLSchema&gt;string"&gt;
 *                                     &lt;attribute name="funderIdentifierType" use="required" type="{http://datacite.org/schema/kernel-4}funderIdentifierType" /&gt;
 *                                     &lt;attribute name="schemeURI" type="{http://www.w3.org/2001/XMLSchema}anyURI" /&gt;
 *                                   &lt;/extension&gt;
 *                                 &lt;/simpleContent&gt;
 *                               &lt;/complexType&gt;
 *                             &lt;/element&gt;
 *                             &lt;element name="awardNumber" minOccurs="0"&gt;
 *                               &lt;complexType&gt;
 *                                 &lt;simpleContent&gt;
 *                                   &lt;extension base="&lt;http://www.w3.org/2001/XMLSchema&gt;string"&gt;
 *                                     &lt;attribute name="awardURI" type="{http://www.w3.org/2001/XMLSchema}anyURI" /&gt;
 *                                   &lt;/extension&gt;
 *                                 &lt;/simpleContent&gt;
 *                               &lt;/complexType&gt;
 *                             &lt;/element&gt;
 *                             &lt;element name="awardTitle" type="{http://www.w3.org/2001/XMLSchema}anyType" minOccurs="0"/&gt;
 *                           &lt;/all&gt;
 *                         &lt;/restriction&gt;
 *                       &lt;/complexContent&gt;
 *                     &lt;/complexType&gt;
 *                   &lt;/element&gt;
 *                 &lt;/sequence&gt;
 *               &lt;/restriction&gt;
 *             &lt;/complexContent&gt;
 *           &lt;/complexType&gt;
 *         &lt;/element&gt;
 *         &lt;element name="relatedItems" minOccurs="0"&gt;
 *           &lt;complexType&gt;
 *             &lt;complexContent&gt;
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *                 &lt;sequence&gt;
 *                   &lt;element name="relatedItem" maxOccurs="unbounded" minOccurs="0"&gt;
 *                     &lt;complexType&gt;
 *                       &lt;complexContent&gt;
 *                         &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *                           &lt;sequence&gt;
 *                             &lt;element name="relatedItemIdentifier" minOccurs="0"&gt;
 *                               &lt;complexType&gt;
 *                                 &lt;simpleContent&gt;
 *                                   &lt;extension base="&lt;http://www.w3.org/2001/XMLSchema&gt;string"&gt;
 *                                     &lt;attribute name="relatedItemIdentifierType" type="{http://datacite.org/schema/kernel-4}relatedIdentifierType" /&gt;
 *                                     &lt;attribute name="relatedMetadataScheme" type="{http://www.w3.org/2001/XMLSchema}anySimpleType" /&gt;
 *                                     &lt;attribute name="schemeURI" type="{http://www.w3.org/2001/XMLSchema}anyURI" /&gt;
 *                                     &lt;attribute name="schemeType" type="{http://www.w3.org/2001/XMLSchema}anySimpleType" /&gt;
 *                                   &lt;/extension&gt;
 *                                 &lt;/simpleContent&gt;
 *                               &lt;/complexType&gt;
 *                             &lt;/element&gt;
 *                             &lt;element name="creators" minOccurs="0"&gt;
 *                               &lt;complexType&gt;
 *                                 &lt;complexContent&gt;
 *                                   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *                                     &lt;sequence&gt;
 *                                       &lt;element name="creator" maxOccurs="unbounded" minOccurs="0"&gt;
 *                                         &lt;complexType&gt;
 *                                           &lt;complexContent&gt;
 *                                             &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *                                               &lt;sequence&gt;
 *                                                 &lt;element name="creatorName"&gt;
 *                                                   &lt;complexType&gt;
 *                                                     &lt;simpleContent&gt;
 *                                                       &lt;extension base="&lt;http://www.w3.org/2001/XMLSchema&gt;string"&gt;
 *                                                         &lt;attribute name="nameType" type="{http://datacite.org/schema/kernel-4}nameType" /&gt;
 *                                                         &lt;attribute ref="{http://www.w3.org/XML/1998/namespace}lang"/&gt;
 *                                                       &lt;/extension&gt;
 *                                                     &lt;/simpleContent&gt;
 *                                                   &lt;/complexType&gt;
 *                                                 &lt;/element&gt;
 *                                                 &lt;element name="givenName" type="{http://www.w3.org/2001/XMLSchema}anyType" minOccurs="0"/&gt;
 *                                                 &lt;element name="familyName" type="{http://www.w3.org/2001/XMLSchema}anyType" minOccurs="0"/&gt;
 *                                               &lt;/sequence&gt;
 *                                             &lt;/restriction&gt;
 *                                           &lt;/complexContent&gt;
 *                                         &lt;/complexType&gt;
 *                                       &lt;/element&gt;
 *                                     &lt;/sequence&gt;
 *                                   &lt;/restriction&gt;
 *                                 &lt;/complexContent&gt;
 *                               &lt;/complexType&gt;
 *                             &lt;/element&gt;
 *                             &lt;element name="titles" minOccurs="0"&gt;
 *                               &lt;complexType&gt;
 *                                 &lt;complexContent&gt;
 *                                   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *                                     &lt;sequence&gt;
 *                                       &lt;element name="title" maxOccurs="unbounded" minOccurs="0"&gt;
 *                                         &lt;complexType&gt;
 *                                           &lt;simpleContent&gt;
 *                                             &lt;extension base="&lt;http://www.w3.org/2001/XMLSchema&gt;string"&gt;
 *                                               &lt;attribute name="titleType" type="{http://datacite.org/schema/kernel-4}titleType" /&gt;
 *                                               &lt;attribute ref="{http://www.w3.org/XML/1998/namespace}lang"/&gt;
 *                                             &lt;/extension&gt;
 *                                           &lt;/simpleContent&gt;
 *                                         &lt;/complexType&gt;
 *                                       &lt;/element&gt;
 *                                     &lt;/sequence&gt;
 *                                   &lt;/restriction&gt;
 *                                 &lt;/complexContent&gt;
 *                               &lt;/complexType&gt;
 *                             &lt;/element&gt;
 *                             &lt;element name="publicationYear" minOccurs="0"&gt;
 *                               &lt;simpleType&gt;
 *                                 &lt;restriction base="{http://datacite.org/schema/kernel-4}yearType"&gt;
 *                                 &lt;/restriction&gt;
 *                               &lt;/simpleType&gt;
 *                             &lt;/element&gt;
 *                             &lt;element name="volume" type="{http://www.w3.org/2001/XMLSchema}anyType" minOccurs="0"/&gt;
 *                             &lt;element name="issue" type="{http://www.w3.org/2001/XMLSchema}anyType" minOccurs="0"/&gt;
 *                             &lt;element name="number" minOccurs="0"&gt;
 *                               &lt;complexType&gt;
 *                                 &lt;simpleContent&gt;
 *                                   &lt;extension base="&lt;http://www.w3.org/2001/XMLSchema&gt;string"&gt;
 *                                     &lt;attribute name="numberType" type="{http://datacite.org/schema/kernel-4}numberType" /&gt;
 *                                   &lt;/extension&gt;
 *                                 &lt;/simpleContent&gt;
 *                               &lt;/complexType&gt;
 *                             &lt;/element&gt;
 *                             &lt;element name="firstPage" type="{http://www.w3.org/2001/XMLSchema}anyType" minOccurs="0"/&gt;
 *                             &lt;element name="lastPage" type="{http://www.w3.org/2001/XMLSchema}anyType" minOccurs="0"/&gt;
 *                             &lt;element name="publisher" type="{http://www.w3.org/2001/XMLSchema}anyType" minOccurs="0"/&gt;
 *                             &lt;element name="edition" type="{http://www.w3.org/2001/XMLSchema}anyType" minOccurs="0"/&gt;
 *                             &lt;element name="contributors" minOccurs="0"&gt;
 *                               &lt;complexType&gt;
 *                                 &lt;complexContent&gt;
 *                                   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *                                     &lt;sequence&gt;
 *                                       &lt;element name="contributor" maxOccurs="unbounded" minOccurs="0"&gt;
 *                                         &lt;complexType&gt;
 *                                           &lt;complexContent&gt;
 *                                             &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *                                               &lt;sequence&gt;
 *                                                 &lt;element name="contributorName"&gt;
 *                                                   &lt;complexType&gt;
 *                                                     &lt;simpleContent&gt;
 *                                                       &lt;extension base="&lt;http://www.w3.org/2001/XMLSchema&gt;string"&gt;
 *                                                         &lt;attribute name="nameType" type="{http://datacite.org/schema/kernel-4}nameType" /&gt;
 *                                                         &lt;attribute ref="{http://www.w3.org/XML/1998/namespace}lang"/&gt;
 *                                                       &lt;/extension&gt;
 *                                                     &lt;/simpleContent&gt;
 *                                                   &lt;/complexType&gt;
 *                                                 &lt;/element&gt;
 *                                                 &lt;element name="givenName" type="{http://www.w3.org/2001/XMLSchema}anyType" minOccurs="0"/&gt;
 *                                                 &lt;element name="familyName" type="{http://www.w3.org/2001/XMLSchema}anyType" minOccurs="0"/&gt;
 *                                               &lt;/sequence&gt;
 *                                               &lt;attribute name="contributorType" use="required" type="{http://datacite.org/schema/kernel-4}contributorType" /&gt;
 *                                             &lt;/restriction&gt;
 *                                           &lt;/complexContent&gt;
 *                                         &lt;/complexType&gt;
 *                                       &lt;/element&gt;
 *                                     &lt;/sequence&gt;
 *                                   &lt;/restriction&gt;
 *                                 &lt;/complexContent&gt;
 *                               &lt;/complexType&gt;
 *                             &lt;/element&gt;
 *                           &lt;/sequence&gt;
 *                           &lt;attribute name="relatedItemType" use="required" type="{http://datacite.org/schema/kernel-4}resourceType" /&gt;
 *                           &lt;attribute name="relationType" use="required" type="{http://datacite.org/schema/kernel-4}relationType" /&gt;
 *                         &lt;/restriction&gt;
 *                       &lt;/complexContent&gt;
 *                     &lt;/complexType&gt;
 *                   &lt;/element&gt;
 *                 &lt;/sequence&gt;
 *               &lt;/restriction&gt;
 *             &lt;/complexContent&gt;
 *           &lt;/complexType&gt;
 *         &lt;/element&gt;
 *       &lt;/all&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {

})
@XmlRootElement(name = "resource")
public class Resource {

    @XmlElement(required = true)
    protected Resource.Identifier identifier;
    @XmlElement(required = true)
    protected Resource.Creators creators;
    @XmlElement(required = true)
    protected Resource.Titles titles;
    @XmlElement(required = true)
    protected Resource.Publisher publisher;
    @XmlElement(required = true)
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    protected String publicationYear;
    @XmlElement(required = true)
    protected Resource.ResourceType resourceType;
    protected Resource.Subjects subjects;
    protected Resource.Contributors contributors;
    protected Resource.Dates dates;
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    @XmlSchemaType(name = "language")
    protected String language;
    protected Resource.AlternateIdentifiers alternateIdentifiers;
    protected Resource.RelatedIdentifiers relatedIdentifiers;
    protected Resource.Sizes sizes;
    protected Resource.Formats formats;
    protected String version;
    protected Resource.RightsList rightsList;
    protected Resource.Descriptions descriptions;
    protected Resource.GeoLocations geoLocations;
    protected Resource.FundingReferences fundingReferences;
    protected Resource.RelatedItems relatedItems;

    /**
     * Ruft den Wert der identifier-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link Resource.Identifier }
     *     
     */
    public Resource.Identifier getIdentifier() {
        return identifier;
    }

    /**
     * Legt den Wert der identifier-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link Resource.Identifier }
     *     
     */
    public void setIdentifier(Resource.Identifier value) {
        this.identifier = value;
    }

    /**
     * Ruft den Wert der creators-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link Resource.Creators }
     *     
     */
    public Resource.Creators getCreators() {
        return creators;
    }

    /**
     * Legt den Wert der creators-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link Resource.Creators }
     *     
     */
    public void setCreators(Resource.Creators value) {
        this.creators = value;
    }

    /**
     * Ruft den Wert der titles-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link Resource.Titles }
     *     
     */
    public Resource.Titles getTitles() {
        return titles;
    }

    /**
     * Legt den Wert der titles-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link Resource.Titles }
     *     
     */
    public void setTitles(Resource.Titles value) {
        this.titles = value;
    }

    /**
     * Ruft den Wert der publisher-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link Resource.Publisher }
     *     
     */
    public Resource.Publisher getPublisher() {
        return publisher;
    }

    /**
     * Legt den Wert der publisher-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link Resource.Publisher }
     *     
     */
    public void setPublisher(Resource.Publisher value) {
        this.publisher = value;
    }

    /**
     * Ruft den Wert der publicationYear-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getPublicationYear() {
        return publicationYear;
    }

    /**
     * Legt den Wert der publicationYear-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setPublicationYear(String value) {
        this.publicationYear = value;
    }

    /**
     * Ruft den Wert der resourceType-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link Resource.ResourceType }
     *     
     */
    public Resource.ResourceType getResourceType() {
        return resourceType;
    }

    /**
     * Legt den Wert der resourceType-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link Resource.ResourceType }
     *     
     */
    public void setResourceType(Resource.ResourceType value) {
        this.resourceType = value;
    }

    /**
     * Ruft den Wert der subjects-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link Resource.Subjects }
     *     
     */
    public Resource.Subjects getSubjects() {
        return subjects;
    }

    /**
     * Legt den Wert der subjects-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link Resource.Subjects }
     *     
     */
    public void setSubjects(Resource.Subjects value) {
        this.subjects = value;
    }

    /**
     * Ruft den Wert der contributors-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link Resource.Contributors }
     *     
     */
    public Resource.Contributors getContributors() {
        return contributors;
    }

    /**
     * Legt den Wert der contributors-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link Resource.Contributors }
     *     
     */
    public void setContributors(Resource.Contributors value) {
        this.contributors = value;
    }

    /**
     * Ruft den Wert der dates-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link Resource.Dates }
     *     
     */
    public Resource.Dates getDates() {
        return dates;
    }

    /**
     * Legt den Wert der dates-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link Resource.Dates }
     *     
     */
    public void setDates(Resource.Dates value) {
        this.dates = value;
    }

    /**
     * Ruft den Wert der language-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getLanguage() {
        return language;
    }

    /**
     * Legt den Wert der language-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setLanguage(String value) {
        this.language = value;
    }

    /**
     * Ruft den Wert der alternateIdentifiers-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link Resource.AlternateIdentifiers }
     *     
     */
    public Resource.AlternateIdentifiers getAlternateIdentifiers() {
        return alternateIdentifiers;
    }

    /**
     * Legt den Wert der alternateIdentifiers-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link Resource.AlternateIdentifiers }
     *     
     */
    public void setAlternateIdentifiers(Resource.AlternateIdentifiers value) {
        this.alternateIdentifiers = value;
    }

    /**
     * Ruft den Wert der relatedIdentifiers-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link Resource.RelatedIdentifiers }
     *     
     */
    public Resource.RelatedIdentifiers getRelatedIdentifiers() {
        return relatedIdentifiers;
    }

    /**
     * Legt den Wert der relatedIdentifiers-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link Resource.RelatedIdentifiers }
     *     
     */
    public void setRelatedIdentifiers(Resource.RelatedIdentifiers value) {
        this.relatedIdentifiers = value;
    }

    /**
     * Ruft den Wert der sizes-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link Resource.Sizes }
     *     
     */
    public Resource.Sizes getSizes() {
        return sizes;
    }

    /**
     * Legt den Wert der sizes-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link Resource.Sizes }
     *     
     */
    public void setSizes(Resource.Sizes value) {
        this.sizes = value;
    }

    /**
     * Ruft den Wert der formats-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link Resource.Formats }
     *     
     */
    public Resource.Formats getFormats() {
        return formats;
    }

    /**
     * Legt den Wert der formats-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link Resource.Formats }
     *     
     */
    public void setFormats(Resource.Formats value) {
        this.formats = value;
    }

    /**
     * Ruft den Wert der version-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getVersion() {
        return version;
    }

    /**
     * Legt den Wert der version-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setVersion(String value) {
        this.version = value;
    }

    /**
     * Ruft den Wert der rightsList-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link Resource.RightsList }
     *     
     */
    public Resource.RightsList getRightsList() {
        return rightsList;
    }

    /**
     * Legt den Wert der rightsList-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link Resource.RightsList }
     *     
     */
    public void setRightsList(Resource.RightsList value) {
        this.rightsList = value;
    }

    /**
     * Ruft den Wert der descriptions-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link Resource.Descriptions }
     *     
     */
    public Resource.Descriptions getDescriptions() {
        return descriptions;
    }

    /**
     * Legt den Wert der descriptions-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link Resource.Descriptions }
     *     
     */
    public void setDescriptions(Resource.Descriptions value) {
        this.descriptions = value;
    }

    /**
     * Ruft den Wert der geoLocations-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link Resource.GeoLocations }
     *     
     */
    public Resource.GeoLocations getGeoLocations() {
        return geoLocations;
    }

    /**
     * Legt den Wert der geoLocations-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link Resource.GeoLocations }
     *     
     */
    public void setGeoLocations(Resource.GeoLocations value) {
        this.geoLocations = value;
    }

    /**
     * Ruft den Wert der fundingReferences-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link Resource.FundingReferences }
     *     
     */
    public Resource.FundingReferences getFundingReferences() {
        return fundingReferences;
    }

    /**
     * Legt den Wert der fundingReferences-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link Resource.FundingReferences }
     *     
     */
    public void setFundingReferences(Resource.FundingReferences value) {
        this.fundingReferences = value;
    }

    /**
     * Ruft den Wert der relatedItems-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link Resource.RelatedItems }
     *     
     */
    public Resource.RelatedItems getRelatedItems() {
        return relatedItems;
    }

    /**
     * Legt den Wert der relatedItems-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link Resource.RelatedItems }
     *     
     */
    public void setRelatedItems(Resource.RelatedItems value) {
        this.relatedItems = value;
    }


    /**
     * <p>Java-Klasse für anonymous complex type.
     * 
     * <p>Das folgende Schemafragment gibt den erwarteten Content an, der in dieser Klasse enthalten ist.
     * 
     * <pre>
     * &lt;complexType&gt;
     *   &lt;complexContent&gt;
     *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
     *       &lt;sequence&gt;
     *         &lt;element name="alternateIdentifier" maxOccurs="unbounded" minOccurs="0"&gt;
     *           &lt;complexType&gt;
     *             &lt;simpleContent&gt;
     *               &lt;extension base="&lt;http://www.w3.org/2001/XMLSchema&gt;string"&gt;
     *                 &lt;attribute name="alternateIdentifierType" use="required" type="{http://www.w3.org/2001/XMLSchema}anySimpleType" /&gt;
     *               &lt;/extension&gt;
     *             &lt;/simpleContent&gt;
     *           &lt;/complexType&gt;
     *         &lt;/element&gt;
     *       &lt;/sequence&gt;
     *     &lt;/restriction&gt;
     *   &lt;/complexContent&gt;
     * &lt;/complexType&gt;
     * </pre>
     * 
     * 
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "", propOrder = {
        "alternateIdentifier"
    })
    public static class AlternateIdentifiers {

        protected List<Resource.AlternateIdentifiers.AlternateIdentifier> alternateIdentifier;

        /**
         * Gets the value of the alternateIdentifier property.
         * 
         * <p>
         * This accessor method returns a reference to the live list,
         * not a snapshot. Therefore any modification you make to the
         * returned list will be present inside the Jakarta XML Binding object.
         * This is why there is not a <CODE>set</CODE> method for the alternateIdentifier property.
         * 
         * <p>
         * For example, to add a new item, do as follows:
         * <pre>
         *    getAlternateIdentifier().add(newItem);
         * </pre>
         * 
         * 
         * <p>
         * Objects of the following type(s) are allowed in the list
         * {@link Resource.AlternateIdentifiers.AlternateIdentifier }
         * 
         * 
         */
        public List<Resource.AlternateIdentifiers.AlternateIdentifier> getAlternateIdentifier() {
            if (alternateIdentifier == null) {
                alternateIdentifier = new ArrayList<Resource.AlternateIdentifiers.AlternateIdentifier>();
            }
            return this.alternateIdentifier;
        }


        /**
         * <p>Java-Klasse für anonymous complex type.
         * 
         * <p>Das folgende Schemafragment gibt den erwarteten Content an, der in dieser Klasse enthalten ist.
         * 
         * <pre>
         * &lt;complexType&gt;
         *   &lt;simpleContent&gt;
         *     &lt;extension base="&lt;http://www.w3.org/2001/XMLSchema&gt;string"&gt;
         *       &lt;attribute name="alternateIdentifierType" use="required" type="{http://www.w3.org/2001/XMLSchema}anySimpleType" /&gt;
         *     &lt;/extension&gt;
         *   &lt;/simpleContent&gt;
         * &lt;/complexType&gt;
         * </pre>
         * 
         * 
         */
        @XmlAccessorType(XmlAccessType.FIELD)
        @XmlType(name = "", propOrder = {
            "value"
        })
        public static class AlternateIdentifier {

            @XmlValue
            protected String value;
            @XmlAttribute(name = "alternateIdentifierType", required = true)
            @XmlSchemaType(name = "anySimpleType")
            protected String alternateIdentifierType;

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
             * Ruft den Wert der alternateIdentifierType-Eigenschaft ab.
             * 
             * @return
             *     possible object is
             *     {@link String }
             *     
             */
            public String getAlternateIdentifierType() {
                return alternateIdentifierType;
            }

            /**
             * Legt den Wert der alternateIdentifierType-Eigenschaft fest.
             * 
             * @param value
             *     allowed object is
             *     {@link String }
             *     
             */
            public void setAlternateIdentifierType(String value) {
                this.alternateIdentifierType = value;
            }

        }

    }


    /**
     * <p>Java-Klasse für anonymous complex type.
     * 
     * <p>Das folgende Schemafragment gibt den erwarteten Content an, der in dieser Klasse enthalten ist.
     * 
     * <pre>
     * &lt;complexType&gt;
     *   &lt;complexContent&gt;
     *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
     *       &lt;sequence&gt;
     *         &lt;element name="contributor" maxOccurs="unbounded" minOccurs="0"&gt;
     *           &lt;complexType&gt;
     *             &lt;complexContent&gt;
     *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
     *                 &lt;sequence&gt;
     *                   &lt;element name="contributorName"&gt;
     *                     &lt;complexType&gt;
     *                       &lt;simpleContent&gt;
     *                         &lt;extension base="&lt;http://datacite.org/schema/kernel-4&gt;nonemptycontentStringType"&gt;
     *                           &lt;attribute name="nameType" type="{http://datacite.org/schema/kernel-4}nameType" /&gt;
     *                           &lt;attribute ref="{http://www.w3.org/XML/1998/namespace}lang"/&gt;
     *                         &lt;/extension&gt;
     *                       &lt;/simpleContent&gt;
     *                     &lt;/complexType&gt;
     *                   &lt;/element&gt;
     *                   &lt;element name="givenName" type="{http://www.w3.org/2001/XMLSchema}anyType" minOccurs="0"/&gt;
     *                   &lt;element name="familyName" type="{http://www.w3.org/2001/XMLSchema}anyType" minOccurs="0"/&gt;
     *                   &lt;element name="nameIdentifier" type="{http://www.w3.org/2001/XMLSchema}anyType" maxOccurs="unbounded" minOccurs="0"/&gt;
     *                   &lt;element name="affiliation" type="{http://www.w3.org/2001/XMLSchema}anyType" maxOccurs="unbounded" minOccurs="0"/&gt;
     *                 &lt;/sequence&gt;
     *                 &lt;attribute name="contributorType" use="required" type="{http://datacite.org/schema/kernel-4}contributorType" /&gt;
     *               &lt;/restriction&gt;
     *             &lt;/complexContent&gt;
     *           &lt;/complexType&gt;
     *         &lt;/element&gt;
     *       &lt;/sequence&gt;
     *     &lt;/restriction&gt;
     *   &lt;/complexContent&gt;
     * &lt;/complexType&gt;
     * </pre>
     * 
     * 
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "", propOrder = {
        "contributor"
    })
    public static class Contributors {

        protected List<Resource.Contributors.Contributor> contributor;

        /**
         * Gets the value of the contributor property.
         * 
         * <p>
         * This accessor method returns a reference to the live list,
         * not a snapshot. Therefore any modification you make to the
         * returned list will be present inside the Jakarta XML Binding object.
         * This is why there is not a <CODE>set</CODE> method for the contributor property.
         * 
         * <p>
         * For example, to add a new item, do as follows:
         * <pre>
         *    getContributor().add(newItem);
         * </pre>
         * 
         * 
         * <p>
         * Objects of the following type(s) are allowed in the list
         * {@link Resource.Contributors.Contributor }
         * 
         * 
         */
        public List<Resource.Contributors.Contributor> getContributor() {
            if (contributor == null) {
                contributor = new ArrayList<Resource.Contributors.Contributor>();
            }
            return this.contributor;
        }


        /**
         * <p>Java-Klasse für anonymous complex type.
         * 
         * <p>Das folgende Schemafragment gibt den erwarteten Content an, der in dieser Klasse enthalten ist.
         * 
         * <pre>
         * &lt;complexType&gt;
         *   &lt;complexContent&gt;
         *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
         *       &lt;sequence&gt;
         *         &lt;element name="contributorName"&gt;
         *           &lt;complexType&gt;
         *             &lt;simpleContent&gt;
         *               &lt;extension base="&lt;http://datacite.org/schema/kernel-4&gt;nonemptycontentStringType"&gt;
         *                 &lt;attribute name="nameType" type="{http://datacite.org/schema/kernel-4}nameType" /&gt;
         *                 &lt;attribute ref="{http://www.w3.org/XML/1998/namespace}lang"/&gt;
         *               &lt;/extension&gt;
         *             &lt;/simpleContent&gt;
         *           &lt;/complexType&gt;
         *         &lt;/element&gt;
         *         &lt;element name="givenName" type="{http://www.w3.org/2001/XMLSchema}anyType" minOccurs="0"/&gt;
         *         &lt;element name="familyName" type="{http://www.w3.org/2001/XMLSchema}anyType" minOccurs="0"/&gt;
         *         &lt;element name="nameIdentifier" type="{http://www.w3.org/2001/XMLSchema}anyType" maxOccurs="unbounded" minOccurs="0"/&gt;
         *         &lt;element name="affiliation" type="{http://www.w3.org/2001/XMLSchema}anyType" maxOccurs="unbounded" minOccurs="0"/&gt;
         *       &lt;/sequence&gt;
         *       &lt;attribute name="contributorType" use="required" type="{http://datacite.org/schema/kernel-4}contributorType" /&gt;
         *     &lt;/restriction&gt;
         *   &lt;/complexContent&gt;
         * &lt;/complexType&gt;
         * </pre>
         * 
         * 
         */
        @XmlAccessorType(XmlAccessType.FIELD)
        @XmlType(name = "", propOrder = {
            "contributorName",
            "givenName",
            "familyName",
            "nameIdentifier",
            "affiliation"
        })
        public static class Contributor {

            @XmlElement(required = true)
            protected Resource.Contributors.Contributor.ContributorName contributorName;
            protected Object givenName;
            protected Object familyName;
            protected List<Object> nameIdentifier;
            protected List<Object> affiliation;
            @XmlAttribute(name = "contributorType", required = true)
            protected ContributorType contributorType;

            /**
             * Ruft den Wert der contributorName-Eigenschaft ab.
             * 
             * @return
             *     possible object is
             *     {@link Resource.Contributors.Contributor.ContributorName }
             *     
             */
            public Resource.Contributors.Contributor.ContributorName getContributorName() {
                return contributorName;
            }

            /**
             * Legt den Wert der contributorName-Eigenschaft fest.
             * 
             * @param value
             *     allowed object is
             *     {@link Resource.Contributors.Contributor.ContributorName }
             *     
             */
            public void setContributorName(Resource.Contributors.Contributor.ContributorName value) {
                this.contributorName = value;
            }

            /**
             * Ruft den Wert der givenName-Eigenschaft ab.
             * 
             * @return
             *     possible object is
             *     {@link Object }
             *     
             */
            public Object getGivenName() {
                return givenName;
            }

            /**
             * Legt den Wert der givenName-Eigenschaft fest.
             * 
             * @param value
             *     allowed object is
             *     {@link Object }
             *     
             */
            public void setGivenName(Object value) {
                this.givenName = value;
            }

            /**
             * Ruft den Wert der familyName-Eigenschaft ab.
             * 
             * @return
             *     possible object is
             *     {@link Object }
             *     
             */
            public Object getFamilyName() {
                return familyName;
            }

            /**
             * Legt den Wert der familyName-Eigenschaft fest.
             * 
             * @param value
             *     allowed object is
             *     {@link Object }
             *     
             */
            public void setFamilyName(Object value) {
                this.familyName = value;
            }

            /**
             * Gets the value of the nameIdentifier property.
             * 
             * <p>
             * This accessor method returns a reference to the live list,
             * not a snapshot. Therefore any modification you make to the
             * returned list will be present inside the Jakarta XML Binding object.
             * This is why there is not a <CODE>set</CODE> method for the nameIdentifier property.
             * 
             * <p>
             * For example, to add a new item, do as follows:
             * <pre>
             *    getNameIdentifier().add(newItem);
             * </pre>
             * 
             * 
             * <p>
             * Objects of the following type(s) are allowed in the list
             * {@link Object }
             * 
             * 
             */
            public List<Object> getNameIdentifier() {
                if (nameIdentifier == null) {
                    nameIdentifier = new ArrayList<Object>();
                }
                return this.nameIdentifier;
            }

            /**
             * Gets the value of the affiliation property.
             * 
             * <p>
             * This accessor method returns a reference to the live list,
             * not a snapshot. Therefore any modification you make to the
             * returned list will be present inside the Jakarta XML Binding object.
             * This is why there is not a <CODE>set</CODE> method for the affiliation property.
             * 
             * <p>
             * For example, to add a new item, do as follows:
             * <pre>
             *    getAffiliation().add(newItem);
             * </pre>
             * 
             * 
             * <p>
             * Objects of the following type(s) are allowed in the list
             * {@link Object }
             * 
             * 
             */
            public List<Object> getAffiliation() {
                if (affiliation == null) {
                    affiliation = new ArrayList<Object>();
                }
                return this.affiliation;
            }

            /**
             * Ruft den Wert der contributorType-Eigenschaft ab.
             * 
             * @return
             *     possible object is
             *     {@link ContributorType }
             *     
             */
            public ContributorType getContributorType() {
                return contributorType;
            }

            /**
             * Legt den Wert der contributorType-Eigenschaft fest.
             * 
             * @param value
             *     allowed object is
             *     {@link ContributorType }
             *     
             */
            public void setContributorType(ContributorType value) {
                this.contributorType = value;
            }


            /**
             * <p>Java-Klasse für anonymous complex type.
             * 
             * <p>Das folgende Schemafragment gibt den erwarteten Content an, der in dieser Klasse enthalten ist.
             * 
             * <pre>
             * &lt;complexType&gt;
             *   &lt;simpleContent&gt;
             *     &lt;extension base="&lt;http://datacite.org/schema/kernel-4&gt;nonemptycontentStringType"&gt;
             *       &lt;attribute name="nameType" type="{http://datacite.org/schema/kernel-4}nameType" /&gt;
             *       &lt;attribute ref="{http://www.w3.org/XML/1998/namespace}lang"/&gt;
             *     &lt;/extension&gt;
             *   &lt;/simpleContent&gt;
             * &lt;/complexType&gt;
             * </pre>
             * 
             * 
             */
            @XmlAccessorType(XmlAccessType.FIELD)
            @XmlType(name = "", propOrder = {
                "value"
            })
            public static class ContributorName {

                @XmlValue
                protected String value;
                @XmlAttribute(name = "nameType")
                protected NameType nameType;
                @XmlAttribute(name = "lang", namespace = "http://www.w3.org/XML/1998/namespace")
                protected String lang;

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
                 * Ruft den Wert der nameType-Eigenschaft ab.
                 * 
                 * @return
                 *     possible object is
                 *     {@link NameType }
                 *     
                 */
                public NameType getNameType() {
                    return nameType;
                }

                /**
                 * Legt den Wert der nameType-Eigenschaft fest.
                 * 
                 * @param value
                 *     allowed object is
                 *     {@link NameType }
                 *     
                 */
                public void setNameType(NameType value) {
                    this.nameType = value;
                }

                /**
                 * Ruft den Wert der lang-Eigenschaft ab.
                 * 
                 * @return
                 *     possible object is
                 *     {@link String }
                 *     
                 */
                public String getLang() {
                    return lang;
                }

                /**
                 * Legt den Wert der lang-Eigenschaft fest.
                 * 
                 * @param value
                 *     allowed object is
                 *     {@link String }
                 *     
                 */
                public void setLang(String value) {
                    this.lang = value;
                }

            }

        }

    }


    /**
     * <p>Java-Klasse für anonymous complex type.
     * 
     * <p>Das folgende Schemafragment gibt den erwarteten Content an, der in dieser Klasse enthalten ist.
     * 
     * <pre>
     * &lt;complexType&gt;
     *   &lt;complexContent&gt;
     *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
     *       &lt;sequence&gt;
     *         &lt;element name="creator" maxOccurs="unbounded"&gt;
     *           &lt;complexType&gt;
     *             &lt;complexContent&gt;
     *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
     *                 &lt;sequence&gt;
     *                   &lt;element name="creatorName"&gt;
     *                     &lt;complexType&gt;
     *                       &lt;simpleContent&gt;
     *                         &lt;extension base="&lt;http://www.w3.org/2001/XMLSchema&gt;string"&gt;
     *                           &lt;attribute name="nameType" type="{http://datacite.org/schema/kernel-4}nameType" /&gt;
     *                           &lt;attribute ref="{http://www.w3.org/XML/1998/namespace}lang"/&gt;
     *                         &lt;/extension&gt;
     *                       &lt;/simpleContent&gt;
     *                     &lt;/complexType&gt;
     *                   &lt;/element&gt;
     *                   &lt;element name="givenName" type="{http://www.w3.org/2001/XMLSchema}anyType" minOccurs="0"/&gt;
     *                   &lt;element name="familyName" type="{http://www.w3.org/2001/XMLSchema}anyType" minOccurs="0"/&gt;
     *                   &lt;element name="nameIdentifier" type="{http://www.w3.org/2001/XMLSchema}anyType" maxOccurs="unbounded" minOccurs="0"/&gt;
     *                   &lt;element name="affiliation" type="{http://www.w3.org/2001/XMLSchema}anyType" maxOccurs="unbounded" minOccurs="0"/&gt;
     *                 &lt;/sequence&gt;
     *               &lt;/restriction&gt;
     *             &lt;/complexContent&gt;
     *           &lt;/complexType&gt;
     *         &lt;/element&gt;
     *       &lt;/sequence&gt;
     *     &lt;/restriction&gt;
     *   &lt;/complexContent&gt;
     * &lt;/complexType&gt;
     * </pre>
     * 
     * 
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "", propOrder = {
        "creator"
    })
    public static class Creators {

        @XmlElement(required = true)
        protected List<Resource.Creators.Creator> creator;

        /**
         * Gets the value of the creator property.
         * 
         * <p>
         * This accessor method returns a reference to the live list,
         * not a snapshot. Therefore any modification you make to the
         * returned list will be present inside the Jakarta XML Binding object.
         * This is why there is not a <CODE>set</CODE> method for the creator property.
         * 
         * <p>
         * For example, to add a new item, do as follows:
         * <pre>
         *    getCreator().add(newItem);
         * </pre>
         * 
         * 
         * <p>
         * Objects of the following type(s) are allowed in the list
         * {@link Resource.Creators.Creator }
         * 
         * 
         */
        public List<Resource.Creators.Creator> getCreator() {
            if (creator == null) {
                creator = new ArrayList<Resource.Creators.Creator>();
            }
            return this.creator;
        }


        /**
         * <p>Java-Klasse für anonymous complex type.
         * 
         * <p>Das folgende Schemafragment gibt den erwarteten Content an, der in dieser Klasse enthalten ist.
         * 
         * <pre>
         * &lt;complexType&gt;
         *   &lt;complexContent&gt;
         *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
         *       &lt;sequence&gt;
         *         &lt;element name="creatorName"&gt;
         *           &lt;complexType&gt;
         *             &lt;simpleContent&gt;
         *               &lt;extension base="&lt;http://www.w3.org/2001/XMLSchema&gt;string"&gt;
         *                 &lt;attribute name="nameType" type="{http://datacite.org/schema/kernel-4}nameType" /&gt;
         *                 &lt;attribute ref="{http://www.w3.org/XML/1998/namespace}lang"/&gt;
         *               &lt;/extension&gt;
         *             &lt;/simpleContent&gt;
         *           &lt;/complexType&gt;
         *         &lt;/element&gt;
         *         &lt;element name="givenName" type="{http://www.w3.org/2001/XMLSchema}anyType" minOccurs="0"/&gt;
         *         &lt;element name="familyName" type="{http://www.w3.org/2001/XMLSchema}anyType" minOccurs="0"/&gt;
         *         &lt;element name="nameIdentifier" type="{http://www.w3.org/2001/XMLSchema}anyType" maxOccurs="unbounded" minOccurs="0"/&gt;
         *         &lt;element name="affiliation" type="{http://www.w3.org/2001/XMLSchema}anyType" maxOccurs="unbounded" minOccurs="0"/&gt;
         *       &lt;/sequence&gt;
         *     &lt;/restriction&gt;
         *   &lt;/complexContent&gt;
         * &lt;/complexType&gt;
         * </pre>
         * 
         * 
         */
        @XmlAccessorType(XmlAccessType.FIELD)
        @XmlType(name = "", propOrder = {
            "creatorName",
            "givenName",
            "familyName",
            "nameIdentifier",
            "affiliation"
        })
        public static class Creator {

            @XmlElement(required = true)
            protected Resource.Creators.Creator.CreatorName creatorName;
            protected Object givenName;
            protected Object familyName;
            protected List<Object> nameIdentifier;
            protected List<Object> affiliation;

            /**
             * Ruft den Wert der creatorName-Eigenschaft ab.
             * 
             * @return
             *     possible object is
             *     {@link Resource.Creators.Creator.CreatorName }
             *     
             */
            public Resource.Creators.Creator.CreatorName getCreatorName() {
                return creatorName;
            }

            /**
             * Legt den Wert der creatorName-Eigenschaft fest.
             * 
             * @param value
             *     allowed object is
             *     {@link Resource.Creators.Creator.CreatorName }
             *     
             */
            public void setCreatorName(Resource.Creators.Creator.CreatorName value) {
                this.creatorName = value;
            }

            /**
             * Ruft den Wert der givenName-Eigenschaft ab.
             * 
             * @return
             *     possible object is
             *     {@link Object }
             *     
             */
            public Object getGivenName() {
                return givenName;
            }

            /**
             * Legt den Wert der givenName-Eigenschaft fest.
             * 
             * @param value
             *     allowed object is
             *     {@link Object }
             *     
             */
            public void setGivenName(Object value) {
                this.givenName = value;
            }

            /**
             * Ruft den Wert der familyName-Eigenschaft ab.
             * 
             * @return
             *     possible object is
             *     {@link Object }
             *     
             */
            public Object getFamilyName() {
                return familyName;
            }

            /**
             * Legt den Wert der familyName-Eigenschaft fest.
             * 
             * @param value
             *     allowed object is
             *     {@link Object }
             *     
             */
            public void setFamilyName(Object value) {
                this.familyName = value;
            }

            /**
             * Gets the value of the nameIdentifier property.
             * 
             * <p>
             * This accessor method returns a reference to the live list,
             * not a snapshot. Therefore any modification you make to the
             * returned list will be present inside the Jakarta XML Binding object.
             * This is why there is not a <CODE>set</CODE> method for the nameIdentifier property.
             * 
             * <p>
             * For example, to add a new item, do as follows:
             * <pre>
             *    getNameIdentifier().add(newItem);
             * </pre>
             * 
             * 
             * <p>
             * Objects of the following type(s) are allowed in the list
             * {@link Object }
             * 
             * 
             */
            public List<Object> getNameIdentifier() {
                if (nameIdentifier == null) {
                    nameIdentifier = new ArrayList<Object>();
                }
                return this.nameIdentifier;
            }

            /**
             * Gets the value of the affiliation property.
             * 
             * <p>
             * This accessor method returns a reference to the live list,
             * not a snapshot. Therefore any modification you make to the
             * returned list will be present inside the Jakarta XML Binding object.
             * This is why there is not a <CODE>set</CODE> method for the affiliation property.
             * 
             * <p>
             * For example, to add a new item, do as follows:
             * <pre>
             *    getAffiliation().add(newItem);
             * </pre>
             * 
             * 
             * <p>
             * Objects of the following type(s) are allowed in the list
             * {@link Object }
             * 
             * 
             */
            public List<Object> getAffiliation() {
                if (affiliation == null) {
                    affiliation = new ArrayList<Object>();
                }
                return this.affiliation;
            }


            /**
             * <p>Java-Klasse für anonymous complex type.
             * 
             * <p>Das folgende Schemafragment gibt den erwarteten Content an, der in dieser Klasse enthalten ist.
             * 
             * <pre>
             * &lt;complexType&gt;
             *   &lt;simpleContent&gt;
             *     &lt;extension base="&lt;http://www.w3.org/2001/XMLSchema&gt;string"&gt;
             *       &lt;attribute name="nameType" type="{http://datacite.org/schema/kernel-4}nameType" /&gt;
             *       &lt;attribute ref="{http://www.w3.org/XML/1998/namespace}lang"/&gt;
             *     &lt;/extension&gt;
             *   &lt;/simpleContent&gt;
             * &lt;/complexType&gt;
             * </pre>
             * 
             * 
             */
            @XmlAccessorType(XmlAccessType.FIELD)
            @XmlType(name = "", propOrder = {
                "value"
            })
            public static class CreatorName {

                @XmlValue
                protected String value;
                @XmlAttribute(name = "nameType")
                protected NameType nameType;
                @XmlAttribute(name = "lang", namespace = "http://www.w3.org/XML/1998/namespace")
                protected String lang;

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
                 * Ruft den Wert der nameType-Eigenschaft ab.
                 * 
                 * @return
                 *     possible object is
                 *     {@link NameType }
                 *     
                 */
                public NameType getNameType() {
                    return nameType;
                }

                /**
                 * Legt den Wert der nameType-Eigenschaft fest.
                 * 
                 * @param value
                 *     allowed object is
                 *     {@link NameType }
                 *     
                 */
                public void setNameType(NameType value) {
                    this.nameType = value;
                }

                /**
                 * Ruft den Wert der lang-Eigenschaft ab.
                 * 
                 * @return
                 *     possible object is
                 *     {@link String }
                 *     
                 */
                public String getLang() {
                    return lang;
                }

                /**
                 * Legt den Wert der lang-Eigenschaft fest.
                 * 
                 * @param value
                 *     allowed object is
                 *     {@link String }
                 *     
                 */
                public void setLang(String value) {
                    this.lang = value;
                }

            }

        }

    }


    /**
     * <p>Java-Klasse für anonymous complex type.
     * 
     * <p>Das folgende Schemafragment gibt den erwarteten Content an, der in dieser Klasse enthalten ist.
     * 
     * <pre>
     * &lt;complexType&gt;
     *   &lt;complexContent&gt;
     *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
     *       &lt;sequence&gt;
     *         &lt;element name="date" maxOccurs="unbounded" minOccurs="0"&gt;
     *           &lt;complexType&gt;
     *             &lt;simpleContent&gt;
     *               &lt;extension base="&lt;http://www.w3.org/2001/XMLSchema&gt;string"&gt;
     *                 &lt;attribute name="dateType" use="required" type="{http://datacite.org/schema/kernel-4}dateType" /&gt;
     *                 &lt;attribute name="dateInformation" type="{http://www.w3.org/2001/XMLSchema}anySimpleType" /&gt;
     *               &lt;/extension&gt;
     *             &lt;/simpleContent&gt;
     *           &lt;/complexType&gt;
     *         &lt;/element&gt;
     *       &lt;/sequence&gt;
     *     &lt;/restriction&gt;
     *   &lt;/complexContent&gt;
     * &lt;/complexType&gt;
     * </pre>
     * 
     * 
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "", propOrder = {
        "date"
    })
    public static class Dates {

        protected List<Resource.Dates.Date> date;

        /**
         * Gets the value of the date property.
         * 
         * <p>
         * This accessor method returns a reference to the live list,
         * not a snapshot. Therefore any modification you make to the
         * returned list will be present inside the Jakarta XML Binding object.
         * This is why there is not a <CODE>set</CODE> method for the date property.
         * 
         * <p>
         * For example, to add a new item, do as follows:
         * <pre>
         *    getDate().add(newItem);
         * </pre>
         * 
         * 
         * <p>
         * Objects of the following type(s) are allowed in the list
         * {@link Resource.Dates.Date }
         * 
         * 
         */
        public List<Resource.Dates.Date> getDate() {
            if (date == null) {
                date = new ArrayList<Resource.Dates.Date>();
            }
            return this.date;
        }


        /**
         * <p>Java-Klasse für anonymous complex type.
         * 
         * <p>Das folgende Schemafragment gibt den erwarteten Content an, der in dieser Klasse enthalten ist.
         * 
         * <pre>
         * &lt;complexType&gt;
         *   &lt;simpleContent&gt;
         *     &lt;extension base="&lt;http://www.w3.org/2001/XMLSchema&gt;string"&gt;
         *       &lt;attribute name="dateType" use="required" type="{http://datacite.org/schema/kernel-4}dateType" /&gt;
         *       &lt;attribute name="dateInformation" type="{http://www.w3.org/2001/XMLSchema}anySimpleType" /&gt;
         *     &lt;/extension&gt;
         *   &lt;/simpleContent&gt;
         * &lt;/complexType&gt;
         * </pre>
         * 
         * 
         */
        @XmlAccessorType(XmlAccessType.FIELD)
        @XmlType(name = "", propOrder = {
            "value"
        })
        public static class Date {

            @XmlValue
            protected String value;
            @XmlAttribute(name = "dateType", required = true)
            protected DateType dateType;
            @XmlAttribute(name = "dateInformation")
            @XmlSchemaType(name = "anySimpleType")
            protected String dateInformation;

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
             * Ruft den Wert der dateType-Eigenschaft ab.
             * 
             * @return
             *     possible object is
             *     {@link DateType }
             *     
             */
            public DateType getDateType() {
                return dateType;
            }

            /**
             * Legt den Wert der dateType-Eigenschaft fest.
             * 
             * @param value
             *     allowed object is
             *     {@link DateType }
             *     
             */
            public void setDateType(DateType value) {
                this.dateType = value;
            }

            /**
             * Ruft den Wert der dateInformation-Eigenschaft ab.
             * 
             * @return
             *     possible object is
             *     {@link String }
             *     
             */
            public String getDateInformation() {
                return dateInformation;
            }

            /**
             * Legt den Wert der dateInformation-Eigenschaft fest.
             * 
             * @param value
             *     allowed object is
             *     {@link String }
             *     
             */
            public void setDateInformation(String value) {
                this.dateInformation = value;
            }

        }

    }


    /**
     * <p>Java-Klasse für anonymous complex type.
     * 
     * <p>Das folgende Schemafragment gibt den erwarteten Content an, der in dieser Klasse enthalten ist.
     * 
     * <pre>
     * &lt;complexType&gt;
     *   &lt;complexContent&gt;
     *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
     *       &lt;sequence&gt;
     *         &lt;element name="description" maxOccurs="unbounded" minOccurs="0"&gt;
     *           &lt;complexType&gt;
     *             &lt;complexContent&gt;
     *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
     *                 &lt;choice&gt;
     *                   &lt;element name="br" maxOccurs="unbounded" minOccurs="0"&gt;
     *                     &lt;complexType&gt;
     *                       &lt;complexContent&gt;
     *                         &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
     *                         &lt;/restriction&gt;
     *                       &lt;/complexContent&gt;
     *                     &lt;/complexType&gt;
     *                   &lt;/element&gt;
     *                 &lt;/choice&gt;
     *                 &lt;attribute name="descriptionType" use="required" type="{http://datacite.org/schema/kernel-4}descriptionType" /&gt;
     *                 &lt;attribute ref="{http://www.w3.org/XML/1998/namespace}lang"/&gt;
     *               &lt;/restriction&gt;
     *             &lt;/complexContent&gt;
     *           &lt;/complexType&gt;
     *         &lt;/element&gt;
     *       &lt;/sequence&gt;
     *     &lt;/restriction&gt;
     *   &lt;/complexContent&gt;
     * &lt;/complexType&gt;
     * </pre>
     * 
     * 
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "", propOrder = {
        "description"
    })
    public static class Descriptions {

        protected List<Resource.Descriptions.Description> description;

        /**
         * Gets the value of the description property.
         * 
         * <p>
         * This accessor method returns a reference to the live list,
         * not a snapshot. Therefore any modification you make to the
         * returned list will be present inside the Jakarta XML Binding object.
         * This is why there is not a <CODE>set</CODE> method for the description property.
         * 
         * <p>
         * For example, to add a new item, do as follows:
         * <pre>
         *    getDescription().add(newItem);
         * </pre>
         * 
         * 
         * <p>
         * Objects of the following type(s) are allowed in the list
         * {@link Resource.Descriptions.Description }
         * 
         * 
         */
        public List<Resource.Descriptions.Description> getDescription() {
            if (description == null) {
                description = new ArrayList<Resource.Descriptions.Description>();
            }
            return this.description;
        }


        /**
         * <p>Java-Klasse für anonymous complex type.
         * 
         * <p>Das folgende Schemafragment gibt den erwarteten Content an, der in dieser Klasse enthalten ist.
         * 
         * <pre>
         * &lt;complexType&gt;
         *   &lt;complexContent&gt;
         *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
         *       &lt;choice&gt;
         *         &lt;element name="br" maxOccurs="unbounded" minOccurs="0"&gt;
         *           &lt;complexType&gt;
         *             &lt;complexContent&gt;
         *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
         *               &lt;/restriction&gt;
         *             &lt;/complexContent&gt;
         *           &lt;/complexType&gt;
         *         &lt;/element&gt;
         *       &lt;/choice&gt;
         *       &lt;attribute name="descriptionType" use="required" type="{http://datacite.org/schema/kernel-4}descriptionType" /&gt;
         *       &lt;attribute ref="{http://www.w3.org/XML/1998/namespace}lang"/&gt;
         *     &lt;/restriction&gt;
         *   &lt;/complexContent&gt;
         * &lt;/complexType&gt;
         * </pre>
         * 
         * 
         */
        @XmlAccessorType(XmlAccessType.FIELD)
        @XmlType(name = "", propOrder = {
            "content"
        })
        public static class Description {

            @XmlElementRef(name = "br", namespace = "http://datacite.org/schema/kernel-4", type = JAXBElement.class, required = false)
            @XmlMixed
            protected List<Serializable> content;
            @XmlAttribute(name = "descriptionType", required = true)
            protected DescriptionType descriptionType;
            @XmlAttribute(name = "lang", namespace = "http://www.w3.org/XML/1998/namespace")
            protected String lang;

            /**
             * Gets the value of the content property.
             * 
             * <p>
             * This accessor method returns a reference to the live list,
             * not a snapshot. Therefore any modification you make to the
             * returned list will be present inside the Jakarta XML Binding object.
             * This is why there is not a <CODE>set</CODE> method for the content property.
             * 
             * <p>
             * For example, to add a new item, do as follows:
             * <pre>
             *    getContent().add(newItem);
             * </pre>
             * 
             * 
             * <p>
             * Objects of the following type(s) are allowed in the list
             * {@link JAXBElement }{@code <}{@link Resource.Descriptions.Description.Br }{@code >}
             * {@link String }
             * 
             * 
             */
            public List<Serializable> getContent() {
                if (content == null) {
                    content = new ArrayList<Serializable>();
                }
                return this.content;
            }

            /**
             * Ruft den Wert der descriptionType-Eigenschaft ab.
             * 
             * @return
             *     possible object is
             *     {@link DescriptionType }
             *     
             */
            public DescriptionType getDescriptionType() {
                return descriptionType;
            }

            /**
             * Legt den Wert der descriptionType-Eigenschaft fest.
             * 
             * @param value
             *     allowed object is
             *     {@link DescriptionType }
             *     
             */
            public void setDescriptionType(DescriptionType value) {
                this.descriptionType = value;
            }

            /**
             * Ruft den Wert der lang-Eigenschaft ab.
             * 
             * @return
             *     possible object is
             *     {@link String }
             *     
             */
            public String getLang() {
                return lang;
            }

            /**
             * Legt den Wert der lang-Eigenschaft fest.
             * 
             * @param value
             *     allowed object is
             *     {@link String }
             *     
             */
            public void setLang(String value) {
                this.lang = value;
            }


            /**
             * <p>Java-Klasse für anonymous complex type.
             * 
             * <p>Das folgende Schemafragment gibt den erwarteten Content an, der in dieser Klasse enthalten ist.
             * 
             * <pre>
             * &lt;complexType&gt;
             *   &lt;complexContent&gt;
             *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
             *     &lt;/restriction&gt;
             *   &lt;/complexContent&gt;
             * &lt;/complexType&gt;
             * </pre>
             * 
             * 
             */
            @XmlAccessorType(XmlAccessType.FIELD)
            @XmlType(name = "")
            public static class Br {


            }

        }

    }


    /**
     * <p>Java-Klasse für anonymous complex type.
     * 
     * <p>Das folgende Schemafragment gibt den erwarteten Content an, der in dieser Klasse enthalten ist.
     * 
     * <pre>
     * &lt;complexType&gt;
     *   &lt;complexContent&gt;
     *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
     *       &lt;sequence&gt;
     *         &lt;element name="format" type="{http://www.w3.org/2001/XMLSchema}string" maxOccurs="unbounded" minOccurs="0"/&gt;
     *       &lt;/sequence&gt;
     *     &lt;/restriction&gt;
     *   &lt;/complexContent&gt;
     * &lt;/complexType&gt;
     * </pre>
     * 
     * 
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "", propOrder = {
        "format"
    })
    public static class Formats {

        protected List<String> format;

        /**
         * Gets the value of the format property.
         * 
         * <p>
         * This accessor method returns a reference to the live list,
         * not a snapshot. Therefore any modification you make to the
         * returned list will be present inside the Jakarta XML Binding object.
         * This is why there is not a <CODE>set</CODE> method for the format property.
         * 
         * <p>
         * For example, to add a new item, do as follows:
         * <pre>
         *    getFormat().add(newItem);
         * </pre>
         * 
         * 
         * <p>
         * Objects of the following type(s) are allowed in the list
         * {@link String }
         * 
         * 
         */
        public List<String> getFormat() {
            if (format == null) {
                format = new ArrayList<String>();
            }
            return this.format;
        }

    }


    /**
     * <p>Java-Klasse für anonymous complex type.
     * 
     * <p>Das folgende Schemafragment gibt den erwarteten Content an, der in dieser Klasse enthalten ist.
     * 
     * <pre>
     * &lt;complexType&gt;
     *   &lt;complexContent&gt;
     *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
     *       &lt;sequence&gt;
     *         &lt;element name="fundingReference" maxOccurs="unbounded" minOccurs="0"&gt;
     *           &lt;complexType&gt;
     *             &lt;complexContent&gt;
     *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
     *                 &lt;all&gt;
     *                   &lt;element name="funderName"&gt;
     *                     &lt;simpleType&gt;
     *                       &lt;restriction base="{http://datacite.org/schema/kernel-4}nonemptycontentStringType"&gt;
     *                       &lt;/restriction&gt;
     *                     &lt;/simpleType&gt;
     *                   &lt;/element&gt;
     *                   &lt;element name="funderIdentifier" minOccurs="0"&gt;
     *                     &lt;complexType&gt;
     *                       &lt;simpleContent&gt;
     *                         &lt;extension base="&lt;http://www.w3.org/2001/XMLSchema&gt;string"&gt;
     *                           &lt;attribute name="funderIdentifierType" use="required" type="{http://datacite.org/schema/kernel-4}funderIdentifierType" /&gt;
     *                           &lt;attribute name="schemeURI" type="{http://www.w3.org/2001/XMLSchema}anyURI" /&gt;
     *                         &lt;/extension&gt;
     *                       &lt;/simpleContent&gt;
     *                     &lt;/complexType&gt;
     *                   &lt;/element&gt;
     *                   &lt;element name="awardNumber" minOccurs="0"&gt;
     *                     &lt;complexType&gt;
     *                       &lt;simpleContent&gt;
     *                         &lt;extension base="&lt;http://www.w3.org/2001/XMLSchema&gt;string"&gt;
     *                           &lt;attribute name="awardURI" type="{http://www.w3.org/2001/XMLSchema}anyURI" /&gt;
     *                         &lt;/extension&gt;
     *                       &lt;/simpleContent&gt;
     *                     &lt;/complexType&gt;
     *                   &lt;/element&gt;
     *                   &lt;element name="awardTitle" type="{http://www.w3.org/2001/XMLSchema}anyType" minOccurs="0"/&gt;
     *                 &lt;/all&gt;
     *               &lt;/restriction&gt;
     *             &lt;/complexContent&gt;
     *           &lt;/complexType&gt;
     *         &lt;/element&gt;
     *       &lt;/sequence&gt;
     *     &lt;/restriction&gt;
     *   &lt;/complexContent&gt;
     * &lt;/complexType&gt;
     * </pre>
     * 
     * 
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "", propOrder = {
        "fundingReference"
    })
    public static class FundingReferences {

        protected List<Resource.FundingReferences.FundingReference> fundingReference;

        /**
         * Gets the value of the fundingReference property.
         * 
         * <p>
         * This accessor method returns a reference to the live list,
         * not a snapshot. Therefore any modification you make to the
         * returned list will be present inside the Jakarta XML Binding object.
         * This is why there is not a <CODE>set</CODE> method for the fundingReference property.
         * 
         * <p>
         * For example, to add a new item, do as follows:
         * <pre>
         *    getFundingReference().add(newItem);
         * </pre>
         * 
         * 
         * <p>
         * Objects of the following type(s) are allowed in the list
         * {@link Resource.FundingReferences.FundingReference }
         * 
         * 
         */
        public List<Resource.FundingReferences.FundingReference> getFundingReference() {
            if (fundingReference == null) {
                fundingReference = new ArrayList<Resource.FundingReferences.FundingReference>();
            }
            return this.fundingReference;
        }


        /**
         * <p>Java-Klasse für anonymous complex type.
         * 
         * <p>Das folgende Schemafragment gibt den erwarteten Content an, der in dieser Klasse enthalten ist.
         * 
         * <pre>
         * &lt;complexType&gt;
         *   &lt;complexContent&gt;
         *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
         *       &lt;all&gt;
         *         &lt;element name="funderName"&gt;
         *           &lt;simpleType&gt;
         *             &lt;restriction base="{http://datacite.org/schema/kernel-4}nonemptycontentStringType"&gt;
         *             &lt;/restriction&gt;
         *           &lt;/simpleType&gt;
         *         &lt;/element&gt;
         *         &lt;element name="funderIdentifier" minOccurs="0"&gt;
         *           &lt;complexType&gt;
         *             &lt;simpleContent&gt;
         *               &lt;extension base="&lt;http://www.w3.org/2001/XMLSchema&gt;string"&gt;
         *                 &lt;attribute name="funderIdentifierType" use="required" type="{http://datacite.org/schema/kernel-4}funderIdentifierType" /&gt;
         *                 &lt;attribute name="schemeURI" type="{http://www.w3.org/2001/XMLSchema}anyURI" /&gt;
         *               &lt;/extension&gt;
         *             &lt;/simpleContent&gt;
         *           &lt;/complexType&gt;
         *         &lt;/element&gt;
         *         &lt;element name="awardNumber" minOccurs="0"&gt;
         *           &lt;complexType&gt;
         *             &lt;simpleContent&gt;
         *               &lt;extension base="&lt;http://www.w3.org/2001/XMLSchema&gt;string"&gt;
         *                 &lt;attribute name="awardURI" type="{http://www.w3.org/2001/XMLSchema}anyURI" /&gt;
         *               &lt;/extension&gt;
         *             &lt;/simpleContent&gt;
         *           &lt;/complexType&gt;
         *         &lt;/element&gt;
         *         &lt;element name="awardTitle" type="{http://www.w3.org/2001/XMLSchema}anyType" minOccurs="0"/&gt;
         *       &lt;/all&gt;
         *     &lt;/restriction&gt;
         *   &lt;/complexContent&gt;
         * &lt;/complexType&gt;
         * </pre>
         * 
         * 
         */
        @XmlAccessorType(XmlAccessType.FIELD)
        @XmlType(name = "", propOrder = {

        })
        public static class FundingReference {

            @XmlElement(required = true)
            protected String funderName;
            protected Resource.FundingReferences.FundingReference.FunderIdentifier funderIdentifier;
            protected Resource.FundingReferences.FundingReference.AwardNumber awardNumber;
            protected Object awardTitle;

            /**
             * Ruft den Wert der funderName-Eigenschaft ab.
             * 
             * @return
             *     possible object is
             *     {@link String }
             *     
             */
            public String getFunderName() {
                return funderName;
            }

            /**
             * Legt den Wert der funderName-Eigenschaft fest.
             * 
             * @param value
             *     allowed object is
             *     {@link String }
             *     
             */
            public void setFunderName(String value) {
                this.funderName = value;
            }

            /**
             * Ruft den Wert der funderIdentifier-Eigenschaft ab.
             * 
             * @return
             *     possible object is
             *     {@link Resource.FundingReferences.FundingReference.FunderIdentifier }
             *     
             */
            public Resource.FundingReferences.FundingReference.FunderIdentifier getFunderIdentifier() {
                return funderIdentifier;
            }

            /**
             * Legt den Wert der funderIdentifier-Eigenschaft fest.
             * 
             * @param value
             *     allowed object is
             *     {@link Resource.FundingReferences.FundingReference.FunderIdentifier }
             *     
             */
            public void setFunderIdentifier(Resource.FundingReferences.FundingReference.FunderIdentifier value) {
                this.funderIdentifier = value;
            }

            /**
             * Ruft den Wert der awardNumber-Eigenschaft ab.
             * 
             * @return
             *     possible object is
             *     {@link Resource.FundingReferences.FundingReference.AwardNumber }
             *     
             */
            public Resource.FundingReferences.FundingReference.AwardNumber getAwardNumber() {
                return awardNumber;
            }

            /**
             * Legt den Wert der awardNumber-Eigenschaft fest.
             * 
             * @param value
             *     allowed object is
             *     {@link Resource.FundingReferences.FundingReference.AwardNumber }
             *     
             */
            public void setAwardNumber(Resource.FundingReferences.FundingReference.AwardNumber value) {
                this.awardNumber = value;
            }

            /**
             * Ruft den Wert der awardTitle-Eigenschaft ab.
             * 
             * @return
             *     possible object is
             *     {@link Object }
             *     
             */
            public Object getAwardTitle() {
                return awardTitle;
            }

            /**
             * Legt den Wert der awardTitle-Eigenschaft fest.
             * 
             * @param value
             *     allowed object is
             *     {@link Object }
             *     
             */
            public void setAwardTitle(Object value) {
                this.awardTitle = value;
            }


            /**
             * <p>Java-Klasse für anonymous complex type.
             * 
             * <p>Das folgende Schemafragment gibt den erwarteten Content an, der in dieser Klasse enthalten ist.
             * 
             * <pre>
             * &lt;complexType&gt;
             *   &lt;simpleContent&gt;
             *     &lt;extension base="&lt;http://www.w3.org/2001/XMLSchema&gt;string"&gt;
             *       &lt;attribute name="awardURI" type="{http://www.w3.org/2001/XMLSchema}anyURI" /&gt;
             *     &lt;/extension&gt;
             *   &lt;/simpleContent&gt;
             * &lt;/complexType&gt;
             * </pre>
             * 
             * 
             */
            @XmlAccessorType(XmlAccessType.FIELD)
            @XmlType(name = "", propOrder = {
                "value"
            })
            public static class AwardNumber {

                @XmlValue
                protected String value;
                @XmlAttribute(name = "awardURI")
                @XmlSchemaType(name = "anyURI")
                protected String awardURI;

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
                 * Ruft den Wert der awardURI-Eigenschaft ab.
                 * 
                 * @return
                 *     possible object is
                 *     {@link String }
                 *     
                 */
                public String getAwardURI() {
                    return awardURI;
                }

                /**
                 * Legt den Wert der awardURI-Eigenschaft fest.
                 * 
                 * @param value
                 *     allowed object is
                 *     {@link String }
                 *     
                 */
                public void setAwardURI(String value) {
                    this.awardURI = value;
                }

            }


            /**
             * <p>Java-Klasse für anonymous complex type.
             * 
             * <p>Das folgende Schemafragment gibt den erwarteten Content an, der in dieser Klasse enthalten ist.
             * 
             * <pre>
             * &lt;complexType&gt;
             *   &lt;simpleContent&gt;
             *     &lt;extension base="&lt;http://www.w3.org/2001/XMLSchema&gt;string"&gt;
             *       &lt;attribute name="funderIdentifierType" use="required" type="{http://datacite.org/schema/kernel-4}funderIdentifierType" /&gt;
             *       &lt;attribute name="schemeURI" type="{http://www.w3.org/2001/XMLSchema}anyURI" /&gt;
             *     &lt;/extension&gt;
             *   &lt;/simpleContent&gt;
             * &lt;/complexType&gt;
             * </pre>
             * 
             * 
             */
            @XmlAccessorType(XmlAccessType.FIELD)
            @XmlType(name = "", propOrder = {
                "value"
            })
            public static class FunderIdentifier {

                @XmlValue
                protected String value;
                @XmlAttribute(name = "funderIdentifierType", required = true)
                protected FunderIdentifierType funderIdentifierType;
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
                 * Ruft den Wert der funderIdentifierType-Eigenschaft ab.
                 * 
                 * @return
                 *     possible object is
                 *     {@link FunderIdentifierType }
                 *     
                 */
                public FunderIdentifierType getFunderIdentifierType() {
                    return funderIdentifierType;
                }

                /**
                 * Legt den Wert der funderIdentifierType-Eigenschaft fest.
                 * 
                 * @param value
                 *     allowed object is
                 *     {@link FunderIdentifierType }
                 *     
                 */
                public void setFunderIdentifierType(FunderIdentifierType value) {
                    this.funderIdentifierType = value;
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

        }

    }


    /**
     * <p>Java-Klasse für anonymous complex type.
     * 
     * <p>Das folgende Schemafragment gibt den erwarteten Content an, der in dieser Klasse enthalten ist.
     * 
     * <pre>
     * &lt;complexType&gt;
     *   &lt;complexContent&gt;
     *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
     *       &lt;sequence&gt;
     *         &lt;element name="geoLocation" maxOccurs="unbounded" minOccurs="0"&gt;
     *           &lt;complexType&gt;
     *             &lt;complexContent&gt;
     *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
     *                 &lt;choice maxOccurs="unbounded"&gt;
     *                   &lt;element name="geoLocationPlace" type="{http://www.w3.org/2001/XMLSchema}anyType" minOccurs="0"/&gt;
     *                   &lt;element name="geoLocationPoint" type="{http://datacite.org/schema/kernel-4}point" minOccurs="0"/&gt;
     *                   &lt;element name="geoLocationBox" type="{http://datacite.org/schema/kernel-4}box" minOccurs="0"/&gt;
     *                   &lt;element name="geoLocationPolygon" maxOccurs="unbounded" minOccurs="0"&gt;
     *                     &lt;complexType&gt;
     *                       &lt;complexContent&gt;
     *                         &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
     *                           &lt;sequence&gt;
     *                             &lt;element name="polygonPoint" type="{http://datacite.org/schema/kernel-4}point" maxOccurs="unbounded" minOccurs="4"/&gt;
     *                             &lt;element name="inPolygonPoint" type="{http://datacite.org/schema/kernel-4}point" minOccurs="0"/&gt;
     *                           &lt;/sequence&gt;
     *                         &lt;/restriction&gt;
     *                       &lt;/complexContent&gt;
     *                     &lt;/complexType&gt;
     *                   &lt;/element&gt;
     *                 &lt;/choice&gt;
     *               &lt;/restriction&gt;
     *             &lt;/complexContent&gt;
     *           &lt;/complexType&gt;
     *         &lt;/element&gt;
     *       &lt;/sequence&gt;
     *     &lt;/restriction&gt;
     *   &lt;/complexContent&gt;
     * &lt;/complexType&gt;
     * </pre>
     * 
     * 
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "", propOrder = {
        "geoLocation"
    })
    public static class GeoLocations {

        protected List<Resource.GeoLocations.GeoLocation> geoLocation;

        /**
         * Gets the value of the geoLocation property.
         * 
         * <p>
         * This accessor method returns a reference to the live list,
         * not a snapshot. Therefore any modification you make to the
         * returned list will be present inside the Jakarta XML Binding object.
         * This is why there is not a <CODE>set</CODE> method for the geoLocation property.
         * 
         * <p>
         * For example, to add a new item, do as follows:
         * <pre>
         *    getGeoLocation().add(newItem);
         * </pre>
         * 
         * 
         * <p>
         * Objects of the following type(s) are allowed in the list
         * {@link Resource.GeoLocations.GeoLocation }
         * 
         * 
         */
        public List<Resource.GeoLocations.GeoLocation> getGeoLocation() {
            if (geoLocation == null) {
                geoLocation = new ArrayList<Resource.GeoLocations.GeoLocation>();
            }
            return this.geoLocation;
        }


        /**
         * <p>Java-Klasse für anonymous complex type.
         * 
         * <p>Das folgende Schemafragment gibt den erwarteten Content an, der in dieser Klasse enthalten ist.
         * 
         * <pre>
         * &lt;complexType&gt;
         *   &lt;complexContent&gt;
         *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
         *       &lt;choice maxOccurs="unbounded"&gt;
         *         &lt;element name="geoLocationPlace" type="{http://www.w3.org/2001/XMLSchema}anyType" minOccurs="0"/&gt;
         *         &lt;element name="geoLocationPoint" type="{http://datacite.org/schema/kernel-4}point" minOccurs="0"/&gt;
         *         &lt;element name="geoLocationBox" type="{http://datacite.org/schema/kernel-4}box" minOccurs="0"/&gt;
         *         &lt;element name="geoLocationPolygon" maxOccurs="unbounded" minOccurs="0"&gt;
         *           &lt;complexType&gt;
         *             &lt;complexContent&gt;
         *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
         *                 &lt;sequence&gt;
         *                   &lt;element name="polygonPoint" type="{http://datacite.org/schema/kernel-4}point" maxOccurs="unbounded" minOccurs="4"/&gt;
         *                   &lt;element name="inPolygonPoint" type="{http://datacite.org/schema/kernel-4}point" minOccurs="0"/&gt;
         *                 &lt;/sequence&gt;
         *               &lt;/restriction&gt;
         *             &lt;/complexContent&gt;
         *           &lt;/complexType&gt;
         *         &lt;/element&gt;
         *       &lt;/choice&gt;
         *     &lt;/restriction&gt;
         *   &lt;/complexContent&gt;
         * &lt;/complexType&gt;
         * </pre>
         * 
         * 
         */
        @XmlAccessorType(XmlAccessType.FIELD)
        @XmlType(name = "", propOrder = {
            "geoLocationPlaceOrGeoLocationPointOrGeoLocationBox"
        })
        public static class GeoLocation {

            @XmlElements({
                @XmlElement(name = "geoLocationPlace"),
                @XmlElement(name = "geoLocationPoint", type = Point.class),
                @XmlElement(name = "geoLocationBox", type = Box.class),
                @XmlElement(name = "geoLocationPolygon", type = Resource.GeoLocations.GeoLocation.GeoLocationPolygon.class)
            })
            protected List<Object> geoLocationPlaceOrGeoLocationPointOrGeoLocationBox;

            /**
             * Gets the value of the geoLocationPlaceOrGeoLocationPointOrGeoLocationBox property.
             * 
             * <p>
             * This accessor method returns a reference to the live list,
             * not a snapshot. Therefore any modification you make to the
             * returned list will be present inside the Jakarta XML Binding object.
             * This is why there is not a <CODE>set</CODE> method for the geoLocationPlaceOrGeoLocationPointOrGeoLocationBox property.
             * 
             * <p>
             * For example, to add a new item, do as follows:
             * <pre>
             *    getGeoLocationPlaceOrGeoLocationPointOrGeoLocationBox().add(newItem);
             * </pre>
             * 
             * 
             * <p>
             * Objects of the following type(s) are allowed in the list
             * {@link Object }
             * {@link Point }
             * {@link Box }
             * {@link Resource.GeoLocations.GeoLocation.GeoLocationPolygon }
             * 
             * 
             */
            public List<Object> getGeoLocationPlaceOrGeoLocationPointOrGeoLocationBox() {
                if (geoLocationPlaceOrGeoLocationPointOrGeoLocationBox == null) {
                    geoLocationPlaceOrGeoLocationPointOrGeoLocationBox = new ArrayList<Object>();
                }
                return this.geoLocationPlaceOrGeoLocationPointOrGeoLocationBox;
            }


            /**
             * <p>Java-Klasse für anonymous complex type.
             * 
             * <p>Das folgende Schemafragment gibt den erwarteten Content an, der in dieser Klasse enthalten ist.
             * 
             * <pre>
             * &lt;complexType&gt;
             *   &lt;complexContent&gt;
             *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
             *       &lt;sequence&gt;
             *         &lt;element name="polygonPoint" type="{http://datacite.org/schema/kernel-4}point" maxOccurs="unbounded" minOccurs="4"/&gt;
             *         &lt;element name="inPolygonPoint" type="{http://datacite.org/schema/kernel-4}point" minOccurs="0"/&gt;
             *       &lt;/sequence&gt;
             *     &lt;/restriction&gt;
             *   &lt;/complexContent&gt;
             * &lt;/complexType&gt;
             * </pre>
             * 
             * 
             */
            @XmlAccessorType(XmlAccessType.FIELD)
            @XmlType(name = "", propOrder = {
                "polygonPoint",
                "inPolygonPoint"
            })
            public static class GeoLocationPolygon {

                @XmlElement(required = true)
                protected List<Point> polygonPoint;
                protected Point inPolygonPoint;

                /**
                 * Gets the value of the polygonPoint property.
                 * 
                 * <p>
                 * This accessor method returns a reference to the live list,
                 * not a snapshot. Therefore any modification you make to the
                 * returned list will be present inside the Jakarta XML Binding object.
                 * This is why there is not a <CODE>set</CODE> method for the polygonPoint property.
                 * 
                 * <p>
                 * For example, to add a new item, do as follows:
                 * <pre>
                 *    getPolygonPoint().add(newItem);
                 * </pre>
                 * 
                 * 
                 * <p>
                 * Objects of the following type(s) are allowed in the list
                 * {@link Point }
                 * 
                 * 
                 */
                public List<Point> getPolygonPoint() {
                    if (polygonPoint == null) {
                        polygonPoint = new ArrayList<Point>();
                    }
                    return this.polygonPoint;
                }

                /**
                 * Ruft den Wert der inPolygonPoint-Eigenschaft ab.
                 * 
                 * @return
                 *     possible object is
                 *     {@link Point }
                 *     
                 */
                public Point getInPolygonPoint() {
                    return inPolygonPoint;
                }

                /**
                 * Legt den Wert der inPolygonPoint-Eigenschaft fest.
                 * 
                 * @param value
                 *     allowed object is
                 *     {@link Point }
                 *     
                 */
                public void setInPolygonPoint(Point value) {
                    this.inPolygonPoint = value;
                }

            }

        }

    }


    /**
     * <p>Java-Klasse für anonymous complex type.
     * 
     * <p>Das folgende Schemafragment gibt den erwarteten Content an, der in dieser Klasse enthalten ist.
     * 
     * <pre>
     * &lt;complexType&gt;
     *   &lt;simpleContent&gt;
     *     &lt;extension base="&lt;http://datacite.org/schema/kernel-4&gt;nonemptycontentStringType"&gt;
     *       &lt;attribute name="identifierType" use="required" type="{http://www.w3.org/2001/XMLSchema}anySimpleType" /&gt;
     *     &lt;/extension&gt;
     *   &lt;/simpleContent&gt;
     * &lt;/complexType&gt;
     * </pre>
     * 
     * 
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "", propOrder = {
        "value"
    })
    public static class Identifier {

        @XmlValue
        protected String value;
        @XmlAttribute(name = "identifierType", required = true)
        @XmlSchemaType(name = "anySimpleType")
        protected String identifierType;

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
         * Ruft den Wert der identifierType-Eigenschaft ab.
         * 
         * @return
         *     possible object is
         *     {@link String }
         *     
         */
        public String getIdentifierType() {
            return identifierType;
        }

        /**
         * Legt den Wert der identifierType-Eigenschaft fest.
         * 
         * @param value
         *     allowed object is
         *     {@link String }
         *     
         */
        public void setIdentifierType(String value) {
            this.identifierType = value;
        }

    }


    /**
     * <p>Java-Klasse für anonymous complex type.
     * 
     * <p>Das folgende Schemafragment gibt den erwarteten Content an, der in dieser Klasse enthalten ist.
     * 
     * <pre>
     * &lt;complexType&gt;
     *   &lt;simpleContent&gt;
     *     &lt;extension base="&lt;http://datacite.org/schema/kernel-4&gt;nonemptycontentStringType"&gt;
     *       &lt;attribute name="publisherIdentifier" type="{http://www.w3.org/2001/XMLSchema}string" /&gt;
     *       &lt;attribute name="publisherIdentifierScheme" type="{http://www.w3.org/2001/XMLSchema}string" /&gt;
     *       &lt;attribute name="schemeURI" type="{http://www.w3.org/2001/XMLSchema}anyURI" /&gt;
     *       &lt;attribute ref="{http://www.w3.org/XML/1998/namespace}lang"/&gt;
     *     &lt;/extension&gt;
     *   &lt;/simpleContent&gt;
     * &lt;/complexType&gt;
     * </pre>
     * 
     * 
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "", propOrder = {
        "value"
    })
    public static class Publisher {

        @XmlValue
        protected String value;
        @XmlAttribute(name = "publisherIdentifier")
        protected String publisherIdentifier;
        @XmlAttribute(name = "publisherIdentifierScheme")
        protected String publisherIdentifierScheme;
        @XmlAttribute(name = "schemeURI")
        @XmlSchemaType(name = "anyURI")
        protected String schemeURI;
        @XmlAttribute(name = "lang", namespace = "http://www.w3.org/XML/1998/namespace")
        protected String lang;

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
         * Ruft den Wert der publisherIdentifier-Eigenschaft ab.
         * 
         * @return
         *     possible object is
         *     {@link String }
         *     
         */
        public String getPublisherIdentifier() {
            return publisherIdentifier;
        }

        /**
         * Legt den Wert der publisherIdentifier-Eigenschaft fest.
         * 
         * @param value
         *     allowed object is
         *     {@link String }
         *     
         */
        public void setPublisherIdentifier(String value) {
            this.publisherIdentifier = value;
        }

        /**
         * Ruft den Wert der publisherIdentifierScheme-Eigenschaft ab.
         * 
         * @return
         *     possible object is
         *     {@link String }
         *     
         */
        public String getPublisherIdentifierScheme() {
            return publisherIdentifierScheme;
        }

        /**
         * Legt den Wert der publisherIdentifierScheme-Eigenschaft fest.
         * 
         * @param value
         *     allowed object is
         *     {@link String }
         *     
         */
        public void setPublisherIdentifierScheme(String value) {
            this.publisherIdentifierScheme = value;
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

        /**
         * Ruft den Wert der lang-Eigenschaft ab.
         * 
         * @return
         *     possible object is
         *     {@link String }
         *     
         */
        public String getLang() {
            return lang;
        }

        /**
         * Legt den Wert der lang-Eigenschaft fest.
         * 
         * @param value
         *     allowed object is
         *     {@link String }
         *     
         */
        public void setLang(String value) {
            this.lang = value;
        }

    }


    /**
     * <p>Java-Klasse für anonymous complex type.
     * 
     * <p>Das folgende Schemafragment gibt den erwarteten Content an, der in dieser Klasse enthalten ist.
     * 
     * <pre>
     * &lt;complexType&gt;
     *   &lt;complexContent&gt;
     *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
     *       &lt;sequence&gt;
     *         &lt;element name="relatedIdentifier" maxOccurs="unbounded" minOccurs="0"&gt;
     *           &lt;complexType&gt;
     *             &lt;simpleContent&gt;
     *               &lt;extension base="&lt;http://www.w3.org/2001/XMLSchema&gt;string"&gt;
     *                 &lt;attribute name="resourceTypeGeneral" type="{http://datacite.org/schema/kernel-4}resourceType" /&gt;
     *                 &lt;attribute name="relatedIdentifierType" use="required" type="{http://datacite.org/schema/kernel-4}relatedIdentifierType" /&gt;
     *                 &lt;attribute name="relationType" use="required" type="{http://datacite.org/schema/kernel-4}relationType" /&gt;
     *                 &lt;attribute name="relatedMetadataScheme" type="{http://www.w3.org/2001/XMLSchema}anySimpleType" /&gt;
     *                 &lt;attribute name="schemeURI" type="{http://www.w3.org/2001/XMLSchema}anyURI" /&gt;
     *                 &lt;attribute name="schemeType" type="{http://www.w3.org/2001/XMLSchema}anySimpleType" /&gt;
     *               &lt;/extension&gt;
     *             &lt;/simpleContent&gt;
     *           &lt;/complexType&gt;
     *         &lt;/element&gt;
     *       &lt;/sequence&gt;
     *     &lt;/restriction&gt;
     *   &lt;/complexContent&gt;
     * &lt;/complexType&gt;
     * </pre>
     * 
     * 
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "", propOrder = {
        "relatedIdentifier"
    })
    public static class RelatedIdentifiers {

        protected List<Resource.RelatedIdentifiers.RelatedIdentifier> relatedIdentifier;

        /**
         * Gets the value of the relatedIdentifier property.
         * 
         * <p>
         * This accessor method returns a reference to the live list,
         * not a snapshot. Therefore any modification you make to the
         * returned list will be present inside the Jakarta XML Binding object.
         * This is why there is not a <CODE>set</CODE> method for the relatedIdentifier property.
         * 
         * <p>
         * For example, to add a new item, do as follows:
         * <pre>
         *    getRelatedIdentifier().add(newItem);
         * </pre>
         * 
         * 
         * <p>
         * Objects of the following type(s) are allowed in the list
         * {@link Resource.RelatedIdentifiers.RelatedIdentifier }
         * 
         * 
         */
        public List<Resource.RelatedIdentifiers.RelatedIdentifier> getRelatedIdentifier() {
            if (relatedIdentifier == null) {
                relatedIdentifier = new ArrayList<Resource.RelatedIdentifiers.RelatedIdentifier>();
            }
            return this.relatedIdentifier;
        }


        /**
         * <p>Java-Klasse für anonymous complex type.
         * 
         * <p>Das folgende Schemafragment gibt den erwarteten Content an, der in dieser Klasse enthalten ist.
         * 
         * <pre>
         * &lt;complexType&gt;
         *   &lt;simpleContent&gt;
         *     &lt;extension base="&lt;http://www.w3.org/2001/XMLSchema&gt;string"&gt;
         *       &lt;attribute name="resourceTypeGeneral" type="{http://datacite.org/schema/kernel-4}resourceType" /&gt;
         *       &lt;attribute name="relatedIdentifierType" use="required" type="{http://datacite.org/schema/kernel-4}relatedIdentifierType" /&gt;
         *       &lt;attribute name="relationType" use="required" type="{http://datacite.org/schema/kernel-4}relationType" /&gt;
         *       &lt;attribute name="relatedMetadataScheme" type="{http://www.w3.org/2001/XMLSchema}anySimpleType" /&gt;
         *       &lt;attribute name="schemeURI" type="{http://www.w3.org/2001/XMLSchema}anyURI" /&gt;
         *       &lt;attribute name="schemeType" type="{http://www.w3.org/2001/XMLSchema}anySimpleType" /&gt;
         *     &lt;/extension&gt;
         *   &lt;/simpleContent&gt;
         * &lt;/complexType&gt;
         * </pre>
         * 
         * 
         */
        @XmlAccessorType(XmlAccessType.FIELD)
        @XmlType(name = "", propOrder = {
            "value"
        })
        public static class RelatedIdentifier {

            @XmlValue
            protected String value;
            @XmlAttribute(name = "resourceTypeGeneral")
            protected org.edu_sharing.service.handleservicedoi.model.xml.ResourceType resourceTypeGeneral;
            @XmlAttribute(name = "relatedIdentifierType", required = true)
            protected RelatedIdentifierType relatedIdentifierType;
            @XmlAttribute(name = "relationType", required = true)
            protected RelationType relationType;
            @XmlAttribute(name = "relatedMetadataScheme")
            @XmlSchemaType(name = "anySimpleType")
            protected String relatedMetadataScheme;
            @XmlAttribute(name = "schemeURI")
            @XmlSchemaType(name = "anyURI")
            protected String schemeURI;
            @XmlAttribute(name = "schemeType")
            @XmlSchemaType(name = "anySimpleType")
            protected String schemeType;

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
             * Ruft den Wert der resourceTypeGeneral-Eigenschaft ab.
             * 
             * @return
             *     possible object is
             *     {@link org.edu_sharing.service.handleservicedoi.model.xml.ResourceType }
             *     
             */
            public org.edu_sharing.service.handleservicedoi.model.xml.ResourceType getResourceTypeGeneral() {
                return resourceTypeGeneral;
            }

            /**
             * Legt den Wert der resourceTypeGeneral-Eigenschaft fest.
             * 
             * @param value
             *     allowed object is
             *     {@link org.edu_sharing.service.handleservicedoi.model.xml.ResourceType }
             *     
             */
            public void setResourceTypeGeneral(org.edu_sharing.service.handleservicedoi.model.xml.ResourceType value) {
                this.resourceTypeGeneral = value;
            }

            /**
             * Ruft den Wert der relatedIdentifierType-Eigenschaft ab.
             * 
             * @return
             *     possible object is
             *     {@link RelatedIdentifierType }
             *     
             */
            public RelatedIdentifierType getRelatedIdentifierType() {
                return relatedIdentifierType;
            }

            /**
             * Legt den Wert der relatedIdentifierType-Eigenschaft fest.
             * 
             * @param value
             *     allowed object is
             *     {@link RelatedIdentifierType }
             *     
             */
            public void setRelatedIdentifierType(RelatedIdentifierType value) {
                this.relatedIdentifierType = value;
            }

            /**
             * Ruft den Wert der relationType-Eigenschaft ab.
             * 
             * @return
             *     possible object is
             *     {@link RelationType }
             *     
             */
            public RelationType getRelationType() {
                return relationType;
            }

            /**
             * Legt den Wert der relationType-Eigenschaft fest.
             * 
             * @param value
             *     allowed object is
             *     {@link RelationType }
             *     
             */
            public void setRelationType(RelationType value) {
                this.relationType = value;
            }

            /**
             * Ruft den Wert der relatedMetadataScheme-Eigenschaft ab.
             * 
             * @return
             *     possible object is
             *     {@link String }
             *     
             */
            public String getRelatedMetadataScheme() {
                return relatedMetadataScheme;
            }

            /**
             * Legt den Wert der relatedMetadataScheme-Eigenschaft fest.
             * 
             * @param value
             *     allowed object is
             *     {@link String }
             *     
             */
            public void setRelatedMetadataScheme(String value) {
                this.relatedMetadataScheme = value;
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

            /**
             * Ruft den Wert der schemeType-Eigenschaft ab.
             * 
             * @return
             *     possible object is
             *     {@link String }
             *     
             */
            public String getSchemeType() {
                return schemeType;
            }

            /**
             * Legt den Wert der schemeType-Eigenschaft fest.
             * 
             * @param value
             *     allowed object is
             *     {@link String }
             *     
             */
            public void setSchemeType(String value) {
                this.schemeType = value;
            }

        }

    }


    /**
     * <p>Java-Klasse für anonymous complex type.
     * 
     * <p>Das folgende Schemafragment gibt den erwarteten Content an, der in dieser Klasse enthalten ist.
     * 
     * <pre>
     * &lt;complexType&gt;
     *   &lt;complexContent&gt;
     *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
     *       &lt;sequence&gt;
     *         &lt;element name="relatedItem" maxOccurs="unbounded" minOccurs="0"&gt;
     *           &lt;complexType&gt;
     *             &lt;complexContent&gt;
     *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
     *                 &lt;sequence&gt;
     *                   &lt;element name="relatedItemIdentifier" minOccurs="0"&gt;
     *                     &lt;complexType&gt;
     *                       &lt;simpleContent&gt;
     *                         &lt;extension base="&lt;http://www.w3.org/2001/XMLSchema&gt;string"&gt;
     *                           &lt;attribute name="relatedItemIdentifierType" type="{http://datacite.org/schema/kernel-4}relatedIdentifierType" /&gt;
     *                           &lt;attribute name="relatedMetadataScheme" type="{http://www.w3.org/2001/XMLSchema}anySimpleType" /&gt;
     *                           &lt;attribute name="schemeURI" type="{http://www.w3.org/2001/XMLSchema}anyURI" /&gt;
     *                           &lt;attribute name="schemeType" type="{http://www.w3.org/2001/XMLSchema}anySimpleType" /&gt;
     *                         &lt;/extension&gt;
     *                       &lt;/simpleContent&gt;
     *                     &lt;/complexType&gt;
     *                   &lt;/element&gt;
     *                   &lt;element name="creators" minOccurs="0"&gt;
     *                     &lt;complexType&gt;
     *                       &lt;complexContent&gt;
     *                         &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
     *                           &lt;sequence&gt;
     *                             &lt;element name="creator" maxOccurs="unbounded" minOccurs="0"&gt;
     *                               &lt;complexType&gt;
     *                                 &lt;complexContent&gt;
     *                                   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
     *                                     &lt;sequence&gt;
     *                                       &lt;element name="creatorName"&gt;
     *                                         &lt;complexType&gt;
     *                                           &lt;simpleContent&gt;
     *                                             &lt;extension base="&lt;http://www.w3.org/2001/XMLSchema&gt;string"&gt;
     *                                               &lt;attribute name="nameType" type="{http://datacite.org/schema/kernel-4}nameType" /&gt;
     *                                               &lt;attribute ref="{http://www.w3.org/XML/1998/namespace}lang"/&gt;
     *                                             &lt;/extension&gt;
     *                                           &lt;/simpleContent&gt;
     *                                         &lt;/complexType&gt;
     *                                       &lt;/element&gt;
     *                                       &lt;element name="givenName" type="{http://www.w3.org/2001/XMLSchema}anyType" minOccurs="0"/&gt;
     *                                       &lt;element name="familyName" type="{http://www.w3.org/2001/XMLSchema}anyType" minOccurs="0"/&gt;
     *                                     &lt;/sequence&gt;
     *                                   &lt;/restriction&gt;
     *                                 &lt;/complexContent&gt;
     *                               &lt;/complexType&gt;
     *                             &lt;/element&gt;
     *                           &lt;/sequence&gt;
     *                         &lt;/restriction&gt;
     *                       &lt;/complexContent&gt;
     *                     &lt;/complexType&gt;
     *                   &lt;/element&gt;
     *                   &lt;element name="titles" minOccurs="0"&gt;
     *                     &lt;complexType&gt;
     *                       &lt;complexContent&gt;
     *                         &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
     *                           &lt;sequence&gt;
     *                             &lt;element name="title" maxOccurs="unbounded" minOccurs="0"&gt;
     *                               &lt;complexType&gt;
     *                                 &lt;simpleContent&gt;
     *                                   &lt;extension base="&lt;http://www.w3.org/2001/XMLSchema&gt;string"&gt;
     *                                     &lt;attribute name="titleType" type="{http://datacite.org/schema/kernel-4}titleType" /&gt;
     *                                     &lt;attribute ref="{http://www.w3.org/XML/1998/namespace}lang"/&gt;
     *                                   &lt;/extension&gt;
     *                                 &lt;/simpleContent&gt;
     *                               &lt;/complexType&gt;
     *                             &lt;/element&gt;
     *                           &lt;/sequence&gt;
     *                         &lt;/restriction&gt;
     *                       &lt;/complexContent&gt;
     *                     &lt;/complexType&gt;
     *                   &lt;/element&gt;
     *                   &lt;element name="publicationYear" minOccurs="0"&gt;
     *                     &lt;simpleType&gt;
     *                       &lt;restriction base="{http://datacite.org/schema/kernel-4}yearType"&gt;
     *                       &lt;/restriction&gt;
     *                     &lt;/simpleType&gt;
     *                   &lt;/element&gt;
     *                   &lt;element name="volume" type="{http://www.w3.org/2001/XMLSchema}anyType" minOccurs="0"/&gt;
     *                   &lt;element name="issue" type="{http://www.w3.org/2001/XMLSchema}anyType" minOccurs="0"/&gt;
     *                   &lt;element name="number" minOccurs="0"&gt;
     *                     &lt;complexType&gt;
     *                       &lt;simpleContent&gt;
     *                         &lt;extension base="&lt;http://www.w3.org/2001/XMLSchema&gt;string"&gt;
     *                           &lt;attribute name="numberType" type="{http://datacite.org/schema/kernel-4}numberType" /&gt;
     *                         &lt;/extension&gt;
     *                       &lt;/simpleContent&gt;
     *                     &lt;/complexType&gt;
     *                   &lt;/element&gt;
     *                   &lt;element name="firstPage" type="{http://www.w3.org/2001/XMLSchema}anyType" minOccurs="0"/&gt;
     *                   &lt;element name="lastPage" type="{http://www.w3.org/2001/XMLSchema}anyType" minOccurs="0"/&gt;
     *                   &lt;element name="publisher" type="{http://www.w3.org/2001/XMLSchema}anyType" minOccurs="0"/&gt;
     *                   &lt;element name="edition" type="{http://www.w3.org/2001/XMLSchema}anyType" minOccurs="0"/&gt;
     *                   &lt;element name="contributors" minOccurs="0"&gt;
     *                     &lt;complexType&gt;
     *                       &lt;complexContent&gt;
     *                         &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
     *                           &lt;sequence&gt;
     *                             &lt;element name="contributor" maxOccurs="unbounded" minOccurs="0"&gt;
     *                               &lt;complexType&gt;
     *                                 &lt;complexContent&gt;
     *                                   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
     *                                     &lt;sequence&gt;
     *                                       &lt;element name="contributorName"&gt;
     *                                         &lt;complexType&gt;
     *                                           &lt;simpleContent&gt;
     *                                             &lt;extension base="&lt;http://www.w3.org/2001/XMLSchema&gt;string"&gt;
     *                                               &lt;attribute name="nameType" type="{http://datacite.org/schema/kernel-4}nameType" /&gt;
     *                                               &lt;attribute ref="{http://www.w3.org/XML/1998/namespace}lang"/&gt;
     *                                             &lt;/extension&gt;
     *                                           &lt;/simpleContent&gt;
     *                                         &lt;/complexType&gt;
     *                                       &lt;/element&gt;
     *                                       &lt;element name="givenName" type="{http://www.w3.org/2001/XMLSchema}anyType" minOccurs="0"/&gt;
     *                                       &lt;element name="familyName" type="{http://www.w3.org/2001/XMLSchema}anyType" minOccurs="0"/&gt;
     *                                     &lt;/sequence&gt;
     *                                     &lt;attribute name="contributorType" use="required" type="{http://datacite.org/schema/kernel-4}contributorType" /&gt;
     *                                   &lt;/restriction&gt;
     *                                 &lt;/complexContent&gt;
     *                               &lt;/complexType&gt;
     *                             &lt;/element&gt;
     *                           &lt;/sequence&gt;
     *                         &lt;/restriction&gt;
     *                       &lt;/complexContent&gt;
     *                     &lt;/complexType&gt;
     *                   &lt;/element&gt;
     *                 &lt;/sequence&gt;
     *                 &lt;attribute name="relatedItemType" use="required" type="{http://datacite.org/schema/kernel-4}resourceType" /&gt;
     *                 &lt;attribute name="relationType" use="required" type="{http://datacite.org/schema/kernel-4}relationType" /&gt;
     *               &lt;/restriction&gt;
     *             &lt;/complexContent&gt;
     *           &lt;/complexType&gt;
     *         &lt;/element&gt;
     *       &lt;/sequence&gt;
     *     &lt;/restriction&gt;
     *   &lt;/complexContent&gt;
     * &lt;/complexType&gt;
     * </pre>
     * 
     * 
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "", propOrder = {
        "relatedItem"
    })
    public static class RelatedItems {

        protected List<Resource.RelatedItems.RelatedItem> relatedItem;

        /**
         * Gets the value of the relatedItem property.
         * 
         * <p>
         * This accessor method returns a reference to the live list,
         * not a snapshot. Therefore any modification you make to the
         * returned list will be present inside the Jakarta XML Binding object.
         * This is why there is not a <CODE>set</CODE> method for the relatedItem property.
         * 
         * <p>
         * For example, to add a new item, do as follows:
         * <pre>
         *    getRelatedItem().add(newItem);
         * </pre>
         * 
         * 
         * <p>
         * Objects of the following type(s) are allowed in the list
         * {@link Resource.RelatedItems.RelatedItem }
         * 
         * 
         */
        public List<Resource.RelatedItems.RelatedItem> getRelatedItem() {
            if (relatedItem == null) {
                relatedItem = new ArrayList<Resource.RelatedItems.RelatedItem>();
            }
            return this.relatedItem;
        }


        /**
         * <p>Java-Klasse für anonymous complex type.
         * 
         * <p>Das folgende Schemafragment gibt den erwarteten Content an, der in dieser Klasse enthalten ist.
         * 
         * <pre>
         * &lt;complexType&gt;
         *   &lt;complexContent&gt;
         *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
         *       &lt;sequence&gt;
         *         &lt;element name="relatedItemIdentifier" minOccurs="0"&gt;
         *           &lt;complexType&gt;
         *             &lt;simpleContent&gt;
         *               &lt;extension base="&lt;http://www.w3.org/2001/XMLSchema&gt;string"&gt;
         *                 &lt;attribute name="relatedItemIdentifierType" type="{http://datacite.org/schema/kernel-4}relatedIdentifierType" /&gt;
         *                 &lt;attribute name="relatedMetadataScheme" type="{http://www.w3.org/2001/XMLSchema}anySimpleType" /&gt;
         *                 &lt;attribute name="schemeURI" type="{http://www.w3.org/2001/XMLSchema}anyURI" /&gt;
         *                 &lt;attribute name="schemeType" type="{http://www.w3.org/2001/XMLSchema}anySimpleType" /&gt;
         *               &lt;/extension&gt;
         *             &lt;/simpleContent&gt;
         *           &lt;/complexType&gt;
         *         &lt;/element&gt;
         *         &lt;element name="creators" minOccurs="0"&gt;
         *           &lt;complexType&gt;
         *             &lt;complexContent&gt;
         *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
         *                 &lt;sequence&gt;
         *                   &lt;element name="creator" maxOccurs="unbounded" minOccurs="0"&gt;
         *                     &lt;complexType&gt;
         *                       &lt;complexContent&gt;
         *                         &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
         *                           &lt;sequence&gt;
         *                             &lt;element name="creatorName"&gt;
         *                               &lt;complexType&gt;
         *                                 &lt;simpleContent&gt;
         *                                   &lt;extension base="&lt;http://www.w3.org/2001/XMLSchema&gt;string"&gt;
         *                                     &lt;attribute name="nameType" type="{http://datacite.org/schema/kernel-4}nameType" /&gt;
         *                                     &lt;attribute ref="{http://www.w3.org/XML/1998/namespace}lang"/&gt;
         *                                   &lt;/extension&gt;
         *                                 &lt;/simpleContent&gt;
         *                               &lt;/complexType&gt;
         *                             &lt;/element&gt;
         *                             &lt;element name="givenName" type="{http://www.w3.org/2001/XMLSchema}anyType" minOccurs="0"/&gt;
         *                             &lt;element name="familyName" type="{http://www.w3.org/2001/XMLSchema}anyType" minOccurs="0"/&gt;
         *                           &lt;/sequence&gt;
         *                         &lt;/restriction&gt;
         *                       &lt;/complexContent&gt;
         *                     &lt;/complexType&gt;
         *                   &lt;/element&gt;
         *                 &lt;/sequence&gt;
         *               &lt;/restriction&gt;
         *             &lt;/complexContent&gt;
         *           &lt;/complexType&gt;
         *         &lt;/element&gt;
         *         &lt;element name="titles" minOccurs="0"&gt;
         *           &lt;complexType&gt;
         *             &lt;complexContent&gt;
         *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
         *                 &lt;sequence&gt;
         *                   &lt;element name="title" maxOccurs="unbounded" minOccurs="0"&gt;
         *                     &lt;complexType&gt;
         *                       &lt;simpleContent&gt;
         *                         &lt;extension base="&lt;http://www.w3.org/2001/XMLSchema&gt;string"&gt;
         *                           &lt;attribute name="titleType" type="{http://datacite.org/schema/kernel-4}titleType" /&gt;
         *                           &lt;attribute ref="{http://www.w3.org/XML/1998/namespace}lang"/&gt;
         *                         &lt;/extension&gt;
         *                       &lt;/simpleContent&gt;
         *                     &lt;/complexType&gt;
         *                   &lt;/element&gt;
         *                 &lt;/sequence&gt;
         *               &lt;/restriction&gt;
         *             &lt;/complexContent&gt;
         *           &lt;/complexType&gt;
         *         &lt;/element&gt;
         *         &lt;element name="publicationYear" minOccurs="0"&gt;
         *           &lt;simpleType&gt;
         *             &lt;restriction base="{http://datacite.org/schema/kernel-4}yearType"&gt;
         *             &lt;/restriction&gt;
         *           &lt;/simpleType&gt;
         *         &lt;/element&gt;
         *         &lt;element name="volume" type="{http://www.w3.org/2001/XMLSchema}anyType" minOccurs="0"/&gt;
         *         &lt;element name="issue" type="{http://www.w3.org/2001/XMLSchema}anyType" minOccurs="0"/&gt;
         *         &lt;element name="number" minOccurs="0"&gt;
         *           &lt;complexType&gt;
         *             &lt;simpleContent&gt;
         *               &lt;extension base="&lt;http://www.w3.org/2001/XMLSchema&gt;string"&gt;
         *                 &lt;attribute name="numberType" type="{http://datacite.org/schema/kernel-4}numberType" /&gt;
         *               &lt;/extension&gt;
         *             &lt;/simpleContent&gt;
         *           &lt;/complexType&gt;
         *         &lt;/element&gt;
         *         &lt;element name="firstPage" type="{http://www.w3.org/2001/XMLSchema}anyType" minOccurs="0"/&gt;
         *         &lt;element name="lastPage" type="{http://www.w3.org/2001/XMLSchema}anyType" minOccurs="0"/&gt;
         *         &lt;element name="publisher" type="{http://www.w3.org/2001/XMLSchema}anyType" minOccurs="0"/&gt;
         *         &lt;element name="edition" type="{http://www.w3.org/2001/XMLSchema}anyType" minOccurs="0"/&gt;
         *         &lt;element name="contributors" minOccurs="0"&gt;
         *           &lt;complexType&gt;
         *             &lt;complexContent&gt;
         *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
         *                 &lt;sequence&gt;
         *                   &lt;element name="contributor" maxOccurs="unbounded" minOccurs="0"&gt;
         *                     &lt;complexType&gt;
         *                       &lt;complexContent&gt;
         *                         &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
         *                           &lt;sequence&gt;
         *                             &lt;element name="contributorName"&gt;
         *                               &lt;complexType&gt;
         *                                 &lt;simpleContent&gt;
         *                                   &lt;extension base="&lt;http://www.w3.org/2001/XMLSchema&gt;string"&gt;
         *                                     &lt;attribute name="nameType" type="{http://datacite.org/schema/kernel-4}nameType" /&gt;
         *                                     &lt;attribute ref="{http://www.w3.org/XML/1998/namespace}lang"/&gt;
         *                                   &lt;/extension&gt;
         *                                 &lt;/simpleContent&gt;
         *                               &lt;/complexType&gt;
         *                             &lt;/element&gt;
         *                             &lt;element name="givenName" type="{http://www.w3.org/2001/XMLSchema}anyType" minOccurs="0"/&gt;
         *                             &lt;element name="familyName" type="{http://www.w3.org/2001/XMLSchema}anyType" minOccurs="0"/&gt;
         *                           &lt;/sequence&gt;
         *                           &lt;attribute name="contributorType" use="required" type="{http://datacite.org/schema/kernel-4}contributorType" /&gt;
         *                         &lt;/restriction&gt;
         *                       &lt;/complexContent&gt;
         *                     &lt;/complexType&gt;
         *                   &lt;/element&gt;
         *                 &lt;/sequence&gt;
         *               &lt;/restriction&gt;
         *             &lt;/complexContent&gt;
         *           &lt;/complexType&gt;
         *         &lt;/element&gt;
         *       &lt;/sequence&gt;
         *       &lt;attribute name="relatedItemType" use="required" type="{http://datacite.org/schema/kernel-4}resourceType" /&gt;
         *       &lt;attribute name="relationType" use="required" type="{http://datacite.org/schema/kernel-4}relationType" /&gt;
         *     &lt;/restriction&gt;
         *   &lt;/complexContent&gt;
         * &lt;/complexType&gt;
         * </pre>
         * 
         * 
         */
        @XmlAccessorType(XmlAccessType.FIELD)
        @XmlType(name = "", propOrder = {
            "relatedItemIdentifier",
            "creators",
            "titles",
            "publicationYear",
            "volume",
            "issue",
            "number",
            "firstPage",
            "lastPage",
            "publisher",
            "edition",
            "contributors"
        })
        public static class RelatedItem {

            protected Resource.RelatedItems.RelatedItem.RelatedItemIdentifier relatedItemIdentifier;
            protected Resource.RelatedItems.RelatedItem.Creators creators;
            protected Resource.RelatedItems.RelatedItem.Titles titles;
            @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
            protected String publicationYear;
            protected Object volume;
            protected Object issue;
            protected Resource.RelatedItems.RelatedItem.Number number;
            protected Object firstPage;
            protected Object lastPage;
            protected Object publisher;
            protected Object edition;
            protected Resource.RelatedItems.RelatedItem.Contributors contributors;
            @XmlAttribute(name = "relatedItemType", required = true)
            protected org.edu_sharing.service.handleservicedoi.model.xml.ResourceType relatedItemType;
            @XmlAttribute(name = "relationType", required = true)
            protected RelationType relationType;

            /**
             * Ruft den Wert der relatedItemIdentifier-Eigenschaft ab.
             * 
             * @return
             *     possible object is
             *     {@link Resource.RelatedItems.RelatedItem.RelatedItemIdentifier }
             *     
             */
            public Resource.RelatedItems.RelatedItem.RelatedItemIdentifier getRelatedItemIdentifier() {
                return relatedItemIdentifier;
            }

            /**
             * Legt den Wert der relatedItemIdentifier-Eigenschaft fest.
             * 
             * @param value
             *     allowed object is
             *     {@link Resource.RelatedItems.RelatedItem.RelatedItemIdentifier }
             *     
             */
            public void setRelatedItemIdentifier(Resource.RelatedItems.RelatedItem.RelatedItemIdentifier value) {
                this.relatedItemIdentifier = value;
            }

            /**
             * Ruft den Wert der creators-Eigenschaft ab.
             * 
             * @return
             *     possible object is
             *     {@link Resource.RelatedItems.RelatedItem.Creators }
             *     
             */
            public Resource.RelatedItems.RelatedItem.Creators getCreators() {
                return creators;
            }

            /**
             * Legt den Wert der creators-Eigenschaft fest.
             * 
             * @param value
             *     allowed object is
             *     {@link Resource.RelatedItems.RelatedItem.Creators }
             *     
             */
            public void setCreators(Resource.RelatedItems.RelatedItem.Creators value) {
                this.creators = value;
            }

            /**
             * Ruft den Wert der titles-Eigenschaft ab.
             * 
             * @return
             *     possible object is
             *     {@link Resource.RelatedItems.RelatedItem.Titles }
             *     
             */
            public Resource.RelatedItems.RelatedItem.Titles getTitles() {
                return titles;
            }

            /**
             * Legt den Wert der titles-Eigenschaft fest.
             * 
             * @param value
             *     allowed object is
             *     {@link Resource.RelatedItems.RelatedItem.Titles }
             *     
             */
            public void setTitles(Resource.RelatedItems.RelatedItem.Titles value) {
                this.titles = value;
            }

            /**
             * Ruft den Wert der publicationYear-Eigenschaft ab.
             * 
             * @return
             *     possible object is
             *     {@link String }
             *     
             */
            public String getPublicationYear() {
                return publicationYear;
            }

            /**
             * Legt den Wert der publicationYear-Eigenschaft fest.
             * 
             * @param value
             *     allowed object is
             *     {@link String }
             *     
             */
            public void setPublicationYear(String value) {
                this.publicationYear = value;
            }

            /**
             * Ruft den Wert der volume-Eigenschaft ab.
             * 
             * @return
             *     possible object is
             *     {@link Object }
             *     
             */
            public Object getVolume() {
                return volume;
            }

            /**
             * Legt den Wert der volume-Eigenschaft fest.
             * 
             * @param value
             *     allowed object is
             *     {@link Object }
             *     
             */
            public void setVolume(Object value) {
                this.volume = value;
            }

            /**
             * Ruft den Wert der issue-Eigenschaft ab.
             * 
             * @return
             *     possible object is
             *     {@link Object }
             *     
             */
            public Object getIssue() {
                return issue;
            }

            /**
             * Legt den Wert der issue-Eigenschaft fest.
             * 
             * @param value
             *     allowed object is
             *     {@link Object }
             *     
             */
            public void setIssue(Object value) {
                this.issue = value;
            }

            /**
             * Ruft den Wert der number-Eigenschaft ab.
             * 
             * @return
             *     possible object is
             *     {@link Resource.RelatedItems.RelatedItem.Number }
             *     
             */
            public Resource.RelatedItems.RelatedItem.Number getNumber() {
                return number;
            }

            /**
             * Legt den Wert der number-Eigenschaft fest.
             * 
             * @param value
             *     allowed object is
             *     {@link Resource.RelatedItems.RelatedItem.Number }
             *     
             */
            public void setNumber(Resource.RelatedItems.RelatedItem.Number value) {
                this.number = value;
            }

            /**
             * Ruft den Wert der firstPage-Eigenschaft ab.
             * 
             * @return
             *     possible object is
             *     {@link Object }
             *     
             */
            public Object getFirstPage() {
                return firstPage;
            }

            /**
             * Legt den Wert der firstPage-Eigenschaft fest.
             * 
             * @param value
             *     allowed object is
             *     {@link Object }
             *     
             */
            public void setFirstPage(Object value) {
                this.firstPage = value;
            }

            /**
             * Ruft den Wert der lastPage-Eigenschaft ab.
             * 
             * @return
             *     possible object is
             *     {@link Object }
             *     
             */
            public Object getLastPage() {
                return lastPage;
            }

            /**
             * Legt den Wert der lastPage-Eigenschaft fest.
             * 
             * @param value
             *     allowed object is
             *     {@link Object }
             *     
             */
            public void setLastPage(Object value) {
                this.lastPage = value;
            }

            /**
             * Ruft den Wert der publisher-Eigenschaft ab.
             * 
             * @return
             *     possible object is
             *     {@link Object }
             *     
             */
            public Object getPublisher() {
                return publisher;
            }

            /**
             * Legt den Wert der publisher-Eigenschaft fest.
             * 
             * @param value
             *     allowed object is
             *     {@link Object }
             *     
             */
            public void setPublisher(Object value) {
                this.publisher = value;
            }

            /**
             * Ruft den Wert der edition-Eigenschaft ab.
             * 
             * @return
             *     possible object is
             *     {@link Object }
             *     
             */
            public Object getEdition() {
                return edition;
            }

            /**
             * Legt den Wert der edition-Eigenschaft fest.
             * 
             * @param value
             *     allowed object is
             *     {@link Object }
             *     
             */
            public void setEdition(Object value) {
                this.edition = value;
            }

            /**
             * Ruft den Wert der contributors-Eigenschaft ab.
             * 
             * @return
             *     possible object is
             *     {@link Resource.RelatedItems.RelatedItem.Contributors }
             *     
             */
            public Resource.RelatedItems.RelatedItem.Contributors getContributors() {
                return contributors;
            }

            /**
             * Legt den Wert der contributors-Eigenschaft fest.
             * 
             * @param value
             *     allowed object is
             *     {@link Resource.RelatedItems.RelatedItem.Contributors }
             *     
             */
            public void setContributors(Resource.RelatedItems.RelatedItem.Contributors value) {
                this.contributors = value;
            }

            /**
             * Ruft den Wert der relatedItemType-Eigenschaft ab.
             * 
             * @return
             *     possible object is
             *     {@link org.edu_sharing.service.handleservicedoi.model.xml.ResourceType }
             *     
             */
            public org.edu_sharing.service.handleservicedoi.model.xml.ResourceType getRelatedItemType() {
                return relatedItemType;
            }

            /**
             * Legt den Wert der relatedItemType-Eigenschaft fest.
             * 
             * @param value
             *     allowed object is
             *     {@link org.edu_sharing.service.handleservicedoi.model.xml.ResourceType }
             *     
             */
            public void setRelatedItemType(org.edu_sharing.service.handleservicedoi.model.xml.ResourceType value) {
                this.relatedItemType = value;
            }

            /**
             * Ruft den Wert der relationType-Eigenschaft ab.
             * 
             * @return
             *     possible object is
             *     {@link RelationType }
             *     
             */
            public RelationType getRelationType() {
                return relationType;
            }

            /**
             * Legt den Wert der relationType-Eigenschaft fest.
             * 
             * @param value
             *     allowed object is
             *     {@link RelationType }
             *     
             */
            public void setRelationType(RelationType value) {
                this.relationType = value;
            }


            /**
             * <p>Java-Klasse für anonymous complex type.
             * 
             * <p>Das folgende Schemafragment gibt den erwarteten Content an, der in dieser Klasse enthalten ist.
             * 
             * <pre>
             * &lt;complexType&gt;
             *   &lt;complexContent&gt;
             *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
             *       &lt;sequence&gt;
             *         &lt;element name="contributor" maxOccurs="unbounded" minOccurs="0"&gt;
             *           &lt;complexType&gt;
             *             &lt;complexContent&gt;
             *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
             *                 &lt;sequence&gt;
             *                   &lt;element name="contributorName"&gt;
             *                     &lt;complexType&gt;
             *                       &lt;simpleContent&gt;
             *                         &lt;extension base="&lt;http://www.w3.org/2001/XMLSchema&gt;string"&gt;
             *                           &lt;attribute name="nameType" type="{http://datacite.org/schema/kernel-4}nameType" /&gt;
             *                           &lt;attribute ref="{http://www.w3.org/XML/1998/namespace}lang"/&gt;
             *                         &lt;/extension&gt;
             *                       &lt;/simpleContent&gt;
             *                     &lt;/complexType&gt;
             *                   &lt;/element&gt;
             *                   &lt;element name="givenName" type="{http://www.w3.org/2001/XMLSchema}anyType" minOccurs="0"/&gt;
             *                   &lt;element name="familyName" type="{http://www.w3.org/2001/XMLSchema}anyType" minOccurs="0"/&gt;
             *                 &lt;/sequence&gt;
             *                 &lt;attribute name="contributorType" use="required" type="{http://datacite.org/schema/kernel-4}contributorType" /&gt;
             *               &lt;/restriction&gt;
             *             &lt;/complexContent&gt;
             *           &lt;/complexType&gt;
             *         &lt;/element&gt;
             *       &lt;/sequence&gt;
             *     &lt;/restriction&gt;
             *   &lt;/complexContent&gt;
             * &lt;/complexType&gt;
             * </pre>
             * 
             * 
             */
            @XmlAccessorType(XmlAccessType.FIELD)
            @XmlType(name = "", propOrder = {
                "contributor"
            })
            public static class Contributors {

                protected List<Resource.RelatedItems.RelatedItem.Contributors.Contributor> contributor;

                /**
                 * Gets the value of the contributor property.
                 * 
                 * <p>
                 * This accessor method returns a reference to the live list,
                 * not a snapshot. Therefore any modification you make to the
                 * returned list will be present inside the Jakarta XML Binding object.
                 * This is why there is not a <CODE>set</CODE> method for the contributor property.
                 * 
                 * <p>
                 * For example, to add a new item, do as follows:
                 * <pre>
                 *    getContributor().add(newItem);
                 * </pre>
                 * 
                 * 
                 * <p>
                 * Objects of the following type(s) are allowed in the list
                 * {@link Resource.RelatedItems.RelatedItem.Contributors.Contributor }
                 * 
                 * 
                 */
                public List<Resource.RelatedItems.RelatedItem.Contributors.Contributor> getContributor() {
                    if (contributor == null) {
                        contributor = new ArrayList<Resource.RelatedItems.RelatedItem.Contributors.Contributor>();
                    }
                    return this.contributor;
                }


                /**
                 * <p>Java-Klasse für anonymous complex type.
                 * 
                 * <p>Das folgende Schemafragment gibt den erwarteten Content an, der in dieser Klasse enthalten ist.
                 * 
                 * <pre>
                 * &lt;complexType&gt;
                 *   &lt;complexContent&gt;
                 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
                 *       &lt;sequence&gt;
                 *         &lt;element name="contributorName"&gt;
                 *           &lt;complexType&gt;
                 *             &lt;simpleContent&gt;
                 *               &lt;extension base="&lt;http://www.w3.org/2001/XMLSchema&gt;string"&gt;
                 *                 &lt;attribute name="nameType" type="{http://datacite.org/schema/kernel-4}nameType" /&gt;
                 *                 &lt;attribute ref="{http://www.w3.org/XML/1998/namespace}lang"/&gt;
                 *               &lt;/extension&gt;
                 *             &lt;/simpleContent&gt;
                 *           &lt;/complexType&gt;
                 *         &lt;/element&gt;
                 *         &lt;element name="givenName" type="{http://www.w3.org/2001/XMLSchema}anyType" minOccurs="0"/&gt;
                 *         &lt;element name="familyName" type="{http://www.w3.org/2001/XMLSchema}anyType" minOccurs="0"/&gt;
                 *       &lt;/sequence&gt;
                 *       &lt;attribute name="contributorType" use="required" type="{http://datacite.org/schema/kernel-4}contributorType" /&gt;
                 *     &lt;/restriction&gt;
                 *   &lt;/complexContent&gt;
                 * &lt;/complexType&gt;
                 * </pre>
                 * 
                 * 
                 */
                @XmlAccessorType(XmlAccessType.FIELD)
                @XmlType(name = "", propOrder = {
                    "contributorName",
                    "givenName",
                    "familyName"
                })
                public static class Contributor {

                    @XmlElement(required = true)
                    protected Resource.RelatedItems.RelatedItem.Contributors.Contributor.ContributorName contributorName;
                    protected Object givenName;
                    protected Object familyName;
                    @XmlAttribute(name = "contributorType", required = true)
                    protected ContributorType contributorType;

                    /**
                     * Ruft den Wert der contributorName-Eigenschaft ab.
                     * 
                     * @return
                     *     possible object is
                     *     {@link Resource.RelatedItems.RelatedItem.Contributors.Contributor.ContributorName }
                     *     
                     */
                    public Resource.RelatedItems.RelatedItem.Contributors.Contributor.ContributorName getContributorName() {
                        return contributorName;
                    }

                    /**
                     * Legt den Wert der contributorName-Eigenschaft fest.
                     * 
                     * @param value
                     *     allowed object is
                     *     {@link Resource.RelatedItems.RelatedItem.Contributors.Contributor.ContributorName }
                     *     
                     */
                    public void setContributorName(Resource.RelatedItems.RelatedItem.Contributors.Contributor.ContributorName value) {
                        this.contributorName = value;
                    }

                    /**
                     * Ruft den Wert der givenName-Eigenschaft ab.
                     * 
                     * @return
                     *     possible object is
                     *     {@link Object }
                     *     
                     */
                    public Object getGivenName() {
                        return givenName;
                    }

                    /**
                     * Legt den Wert der givenName-Eigenschaft fest.
                     * 
                     * @param value
                     *     allowed object is
                     *     {@link Object }
                     *     
                     */
                    public void setGivenName(Object value) {
                        this.givenName = value;
                    }

                    /**
                     * Ruft den Wert der familyName-Eigenschaft ab.
                     * 
                     * @return
                     *     possible object is
                     *     {@link Object }
                     *     
                     */
                    public Object getFamilyName() {
                        return familyName;
                    }

                    /**
                     * Legt den Wert der familyName-Eigenschaft fest.
                     * 
                     * @param value
                     *     allowed object is
                     *     {@link Object }
                     *     
                     */
                    public void setFamilyName(Object value) {
                        this.familyName = value;
                    }

                    /**
                     * Ruft den Wert der contributorType-Eigenschaft ab.
                     * 
                     * @return
                     *     possible object is
                     *     {@link ContributorType }
                     *     
                     */
                    public ContributorType getContributorType() {
                        return contributorType;
                    }

                    /**
                     * Legt den Wert der contributorType-Eigenschaft fest.
                     * 
                     * @param value
                     *     allowed object is
                     *     {@link ContributorType }
                     *     
                     */
                    public void setContributorType(ContributorType value) {
                        this.contributorType = value;
                    }


                    /**
                     * <p>Java-Klasse für anonymous complex type.
                     * 
                     * <p>Das folgende Schemafragment gibt den erwarteten Content an, der in dieser Klasse enthalten ist.
                     * 
                     * <pre>
                     * &lt;complexType&gt;
                     *   &lt;simpleContent&gt;
                     *     &lt;extension base="&lt;http://www.w3.org/2001/XMLSchema&gt;string"&gt;
                     *       &lt;attribute name="nameType" type="{http://datacite.org/schema/kernel-4}nameType" /&gt;
                     *       &lt;attribute ref="{http://www.w3.org/XML/1998/namespace}lang"/&gt;
                     *     &lt;/extension&gt;
                     *   &lt;/simpleContent&gt;
                     * &lt;/complexType&gt;
                     * </pre>
                     * 
                     * 
                     */
                    @XmlAccessorType(XmlAccessType.FIELD)
                    @XmlType(name = "", propOrder = {
                        "value"
                    })
                    public static class ContributorName {

                        @XmlValue
                        protected String value;
                        @XmlAttribute(name = "nameType")
                        protected NameType nameType;
                        @XmlAttribute(name = "lang", namespace = "http://www.w3.org/XML/1998/namespace")
                        protected String lang;

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
                         * Ruft den Wert der nameType-Eigenschaft ab.
                         * 
                         * @return
                         *     possible object is
                         *     {@link NameType }
                         *     
                         */
                        public NameType getNameType() {
                            return nameType;
                        }

                        /**
                         * Legt den Wert der nameType-Eigenschaft fest.
                         * 
                         * @param value
                         *     allowed object is
                         *     {@link NameType }
                         *     
                         */
                        public void setNameType(NameType value) {
                            this.nameType = value;
                        }

                        /**
                         * Ruft den Wert der lang-Eigenschaft ab.
                         * 
                         * @return
                         *     possible object is
                         *     {@link String }
                         *     
                         */
                        public String getLang() {
                            return lang;
                        }

                        /**
                         * Legt den Wert der lang-Eigenschaft fest.
                         * 
                         * @param value
                         *     allowed object is
                         *     {@link String }
                         *     
                         */
                        public void setLang(String value) {
                            this.lang = value;
                        }

                    }

                }

            }


            /**
             * <p>Java-Klasse für anonymous complex type.
             * 
             * <p>Das folgende Schemafragment gibt den erwarteten Content an, der in dieser Klasse enthalten ist.
             * 
             * <pre>
             * &lt;complexType&gt;
             *   &lt;complexContent&gt;
             *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
             *       &lt;sequence&gt;
             *         &lt;element name="creator" maxOccurs="unbounded" minOccurs="0"&gt;
             *           &lt;complexType&gt;
             *             &lt;complexContent&gt;
             *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
             *                 &lt;sequence&gt;
             *                   &lt;element name="creatorName"&gt;
             *                     &lt;complexType&gt;
             *                       &lt;simpleContent&gt;
             *                         &lt;extension base="&lt;http://www.w3.org/2001/XMLSchema&gt;string"&gt;
             *                           &lt;attribute name="nameType" type="{http://datacite.org/schema/kernel-4}nameType" /&gt;
             *                           &lt;attribute ref="{http://www.w3.org/XML/1998/namespace}lang"/&gt;
             *                         &lt;/extension&gt;
             *                       &lt;/simpleContent&gt;
             *                     &lt;/complexType&gt;
             *                   &lt;/element&gt;
             *                   &lt;element name="givenName" type="{http://www.w3.org/2001/XMLSchema}anyType" minOccurs="0"/&gt;
             *                   &lt;element name="familyName" type="{http://www.w3.org/2001/XMLSchema}anyType" minOccurs="0"/&gt;
             *                 &lt;/sequence&gt;
             *               &lt;/restriction&gt;
             *             &lt;/complexContent&gt;
             *           &lt;/complexType&gt;
             *         &lt;/element&gt;
             *       &lt;/sequence&gt;
             *     &lt;/restriction&gt;
             *   &lt;/complexContent&gt;
             * &lt;/complexType&gt;
             * </pre>
             * 
             * 
             */
            @XmlAccessorType(XmlAccessType.FIELD)
            @XmlType(name = "", propOrder = {
                "creator"
            })
            public static class Creators {

                protected List<Resource.RelatedItems.RelatedItem.Creators.Creator> creator;

                /**
                 * Gets the value of the creator property.
                 * 
                 * <p>
                 * This accessor method returns a reference to the live list,
                 * not a snapshot. Therefore any modification you make to the
                 * returned list will be present inside the Jakarta XML Binding object.
                 * This is why there is not a <CODE>set</CODE> method for the creator property.
                 * 
                 * <p>
                 * For example, to add a new item, do as follows:
                 * <pre>
                 *    getCreator().add(newItem);
                 * </pre>
                 * 
                 * 
                 * <p>
                 * Objects of the following type(s) are allowed in the list
                 * {@link Resource.RelatedItems.RelatedItem.Creators.Creator }
                 * 
                 * 
                 */
                public List<Resource.RelatedItems.RelatedItem.Creators.Creator> getCreator() {
                    if (creator == null) {
                        creator = new ArrayList<Resource.RelatedItems.RelatedItem.Creators.Creator>();
                    }
                    return this.creator;
                }


                /**
                 * <p>Java-Klasse für anonymous complex type.
                 * 
                 * <p>Das folgende Schemafragment gibt den erwarteten Content an, der in dieser Klasse enthalten ist.
                 * 
                 * <pre>
                 * &lt;complexType&gt;
                 *   &lt;complexContent&gt;
                 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
                 *       &lt;sequence&gt;
                 *         &lt;element name="creatorName"&gt;
                 *           &lt;complexType&gt;
                 *             &lt;simpleContent&gt;
                 *               &lt;extension base="&lt;http://www.w3.org/2001/XMLSchema&gt;string"&gt;
                 *                 &lt;attribute name="nameType" type="{http://datacite.org/schema/kernel-4}nameType" /&gt;
                 *                 &lt;attribute ref="{http://www.w3.org/XML/1998/namespace}lang"/&gt;
                 *               &lt;/extension&gt;
                 *             &lt;/simpleContent&gt;
                 *           &lt;/complexType&gt;
                 *         &lt;/element&gt;
                 *         &lt;element name="givenName" type="{http://www.w3.org/2001/XMLSchema}anyType" minOccurs="0"/&gt;
                 *         &lt;element name="familyName" type="{http://www.w3.org/2001/XMLSchema}anyType" minOccurs="0"/&gt;
                 *       &lt;/sequence&gt;
                 *     &lt;/restriction&gt;
                 *   &lt;/complexContent&gt;
                 * &lt;/complexType&gt;
                 * </pre>
                 * 
                 * 
                 */
                @XmlAccessorType(XmlAccessType.FIELD)
                @XmlType(name = "", propOrder = {
                    "creatorName",
                    "givenName",
                    "familyName"
                })
                public static class Creator {

                    @XmlElement(required = true)
                    protected Resource.RelatedItems.RelatedItem.Creators.Creator.CreatorName creatorName;
                    protected Object givenName;
                    protected Object familyName;

                    /**
                     * Ruft den Wert der creatorName-Eigenschaft ab.
                     * 
                     * @return
                     *     possible object is
                     *     {@link Resource.RelatedItems.RelatedItem.Creators.Creator.CreatorName }
                     *     
                     */
                    public Resource.RelatedItems.RelatedItem.Creators.Creator.CreatorName getCreatorName() {
                        return creatorName;
                    }

                    /**
                     * Legt den Wert der creatorName-Eigenschaft fest.
                     * 
                     * @param value
                     *     allowed object is
                     *     {@link Resource.RelatedItems.RelatedItem.Creators.Creator.CreatorName }
                     *     
                     */
                    public void setCreatorName(Resource.RelatedItems.RelatedItem.Creators.Creator.CreatorName value) {
                        this.creatorName = value;
                    }

                    /**
                     * Ruft den Wert der givenName-Eigenschaft ab.
                     * 
                     * @return
                     *     possible object is
                     *     {@link Object }
                     *     
                     */
                    public Object getGivenName() {
                        return givenName;
                    }

                    /**
                     * Legt den Wert der givenName-Eigenschaft fest.
                     * 
                     * @param value
                     *     allowed object is
                     *     {@link Object }
                     *     
                     */
                    public void setGivenName(Object value) {
                        this.givenName = value;
                    }

                    /**
                     * Ruft den Wert der familyName-Eigenschaft ab.
                     * 
                     * @return
                     *     possible object is
                     *     {@link Object }
                     *     
                     */
                    public Object getFamilyName() {
                        return familyName;
                    }

                    /**
                     * Legt den Wert der familyName-Eigenschaft fest.
                     * 
                     * @param value
                     *     allowed object is
                     *     {@link Object }
                     *     
                     */
                    public void setFamilyName(Object value) {
                        this.familyName = value;
                    }


                    /**
                     * <p>Java-Klasse für anonymous complex type.
                     * 
                     * <p>Das folgende Schemafragment gibt den erwarteten Content an, der in dieser Klasse enthalten ist.
                     * 
                     * <pre>
                     * &lt;complexType&gt;
                     *   &lt;simpleContent&gt;
                     *     &lt;extension base="&lt;http://www.w3.org/2001/XMLSchema&gt;string"&gt;
                     *       &lt;attribute name="nameType" type="{http://datacite.org/schema/kernel-4}nameType" /&gt;
                     *       &lt;attribute ref="{http://www.w3.org/XML/1998/namespace}lang"/&gt;
                     *     &lt;/extension&gt;
                     *   &lt;/simpleContent&gt;
                     * &lt;/complexType&gt;
                     * </pre>
                     * 
                     * 
                     */
                    @XmlAccessorType(XmlAccessType.FIELD)
                    @XmlType(name = "", propOrder = {
                        "value"
                    })
                    public static class CreatorName {

                        @XmlValue
                        protected String value;
                        @XmlAttribute(name = "nameType")
                        protected NameType nameType;
                        @XmlAttribute(name = "lang", namespace = "http://www.w3.org/XML/1998/namespace")
                        protected String lang;

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
                         * Ruft den Wert der nameType-Eigenschaft ab.
                         * 
                         * @return
                         *     possible object is
                         *     {@link NameType }
                         *     
                         */
                        public NameType getNameType() {
                            return nameType;
                        }

                        /**
                         * Legt den Wert der nameType-Eigenschaft fest.
                         * 
                         * @param value
                         *     allowed object is
                         *     {@link NameType }
                         *     
                         */
                        public void setNameType(NameType value) {
                            this.nameType = value;
                        }

                        /**
                         * Ruft den Wert der lang-Eigenschaft ab.
                         * 
                         * @return
                         *     possible object is
                         *     {@link String }
                         *     
                         */
                        public String getLang() {
                            return lang;
                        }

                        /**
                         * Legt den Wert der lang-Eigenschaft fest.
                         * 
                         * @param value
                         *     allowed object is
                         *     {@link String }
                         *     
                         */
                        public void setLang(String value) {
                            this.lang = value;
                        }

                    }

                }

            }


            /**
             * <p>Java-Klasse für anonymous complex type.
             * 
             * <p>Das folgende Schemafragment gibt den erwarteten Content an, der in dieser Klasse enthalten ist.
             * 
             * <pre>
             * &lt;complexType&gt;
             *   &lt;simpleContent&gt;
             *     &lt;extension base="&lt;http://www.w3.org/2001/XMLSchema&gt;string"&gt;
             *       &lt;attribute name="numberType" type="{http://datacite.org/schema/kernel-4}numberType" /&gt;
             *     &lt;/extension&gt;
             *   &lt;/simpleContent&gt;
             * &lt;/complexType&gt;
             * </pre>
             * 
             * 
             */
            @XmlAccessorType(XmlAccessType.FIELD)
            @XmlType(name = "", propOrder = {
                "value"
            })
            public static class Number {

                @XmlValue
                protected String value;
                @XmlAttribute(name = "numberType")
                protected NumberType numberType;

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
                 * Ruft den Wert der numberType-Eigenschaft ab.
                 * 
                 * @return
                 *     possible object is
                 *     {@link NumberType }
                 *     
                 */
                public NumberType getNumberType() {
                    return numberType;
                }

                /**
                 * Legt den Wert der numberType-Eigenschaft fest.
                 * 
                 * @param value
                 *     allowed object is
                 *     {@link NumberType }
                 *     
                 */
                public void setNumberType(NumberType value) {
                    this.numberType = value;
                }

            }


            /**
             * <p>Java-Klasse für anonymous complex type.
             * 
             * <p>Das folgende Schemafragment gibt den erwarteten Content an, der in dieser Klasse enthalten ist.
             * 
             * <pre>
             * &lt;complexType&gt;
             *   &lt;simpleContent&gt;
             *     &lt;extension base="&lt;http://www.w3.org/2001/XMLSchema&gt;string"&gt;
             *       &lt;attribute name="relatedItemIdentifierType" type="{http://datacite.org/schema/kernel-4}relatedIdentifierType" /&gt;
             *       &lt;attribute name="relatedMetadataScheme" type="{http://www.w3.org/2001/XMLSchema}anySimpleType" /&gt;
             *       &lt;attribute name="schemeURI" type="{http://www.w3.org/2001/XMLSchema}anyURI" /&gt;
             *       &lt;attribute name="schemeType" type="{http://www.w3.org/2001/XMLSchema}anySimpleType" /&gt;
             *     &lt;/extension&gt;
             *   &lt;/simpleContent&gt;
             * &lt;/complexType&gt;
             * </pre>
             * 
             * 
             */
            @XmlAccessorType(XmlAccessType.FIELD)
            @XmlType(name = "", propOrder = {
                "value"
            })
            public static class RelatedItemIdentifier {

                @XmlValue
                protected String value;
                @XmlAttribute(name = "relatedItemIdentifierType")
                protected RelatedIdentifierType relatedItemIdentifierType;
                @XmlAttribute(name = "relatedMetadataScheme")
                @XmlSchemaType(name = "anySimpleType")
                protected String relatedMetadataScheme;
                @XmlAttribute(name = "schemeURI")
                @XmlSchemaType(name = "anyURI")
                protected String schemeURI;
                @XmlAttribute(name = "schemeType")
                @XmlSchemaType(name = "anySimpleType")
                protected String schemeType;

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
                 * Ruft den Wert der relatedItemIdentifierType-Eigenschaft ab.
                 * 
                 * @return
                 *     possible object is
                 *     {@link RelatedIdentifierType }
                 *     
                 */
                public RelatedIdentifierType getRelatedItemIdentifierType() {
                    return relatedItemIdentifierType;
                }

                /**
                 * Legt den Wert der relatedItemIdentifierType-Eigenschaft fest.
                 * 
                 * @param value
                 *     allowed object is
                 *     {@link RelatedIdentifierType }
                 *     
                 */
                public void setRelatedItemIdentifierType(RelatedIdentifierType value) {
                    this.relatedItemIdentifierType = value;
                }

                /**
                 * Ruft den Wert der relatedMetadataScheme-Eigenschaft ab.
                 * 
                 * @return
                 *     possible object is
                 *     {@link String }
                 *     
                 */
                public String getRelatedMetadataScheme() {
                    return relatedMetadataScheme;
                }

                /**
                 * Legt den Wert der relatedMetadataScheme-Eigenschaft fest.
                 * 
                 * @param value
                 *     allowed object is
                 *     {@link String }
                 *     
                 */
                public void setRelatedMetadataScheme(String value) {
                    this.relatedMetadataScheme = value;
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

                /**
                 * Ruft den Wert der schemeType-Eigenschaft ab.
                 * 
                 * @return
                 *     possible object is
                 *     {@link String }
                 *     
                 */
                public String getSchemeType() {
                    return schemeType;
                }

                /**
                 * Legt den Wert der schemeType-Eigenschaft fest.
                 * 
                 * @param value
                 *     allowed object is
                 *     {@link String }
                 *     
                 */
                public void setSchemeType(String value) {
                    this.schemeType = value;
                }

            }


            /**
             * <p>Java-Klasse für anonymous complex type.
             * 
             * <p>Das folgende Schemafragment gibt den erwarteten Content an, der in dieser Klasse enthalten ist.
             * 
             * <pre>
             * &lt;complexType&gt;
             *   &lt;complexContent&gt;
             *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
             *       &lt;sequence&gt;
             *         &lt;element name="title" maxOccurs="unbounded" minOccurs="0"&gt;
             *           &lt;complexType&gt;
             *             &lt;simpleContent&gt;
             *               &lt;extension base="&lt;http://www.w3.org/2001/XMLSchema&gt;string"&gt;
             *                 &lt;attribute name="titleType" type="{http://datacite.org/schema/kernel-4}titleType" /&gt;
             *                 &lt;attribute ref="{http://www.w3.org/XML/1998/namespace}lang"/&gt;
             *               &lt;/extension&gt;
             *             &lt;/simpleContent&gt;
             *           &lt;/complexType&gt;
             *         &lt;/element&gt;
             *       &lt;/sequence&gt;
             *     &lt;/restriction&gt;
             *   &lt;/complexContent&gt;
             * &lt;/complexType&gt;
             * </pre>
             * 
             * 
             */
            @XmlAccessorType(XmlAccessType.FIELD)
            @XmlType(name = "", propOrder = {
                "title"
            })
            public static class Titles {

                protected List<Resource.RelatedItems.RelatedItem.Titles.Title> title;

                /**
                 * Gets the value of the title property.
                 * 
                 * <p>
                 * This accessor method returns a reference to the live list,
                 * not a snapshot. Therefore any modification you make to the
                 * returned list will be present inside the Jakarta XML Binding object.
                 * This is why there is not a <CODE>set</CODE> method for the title property.
                 * 
                 * <p>
                 * For example, to add a new item, do as follows:
                 * <pre>
                 *    getTitle().add(newItem);
                 * </pre>
                 * 
                 * 
                 * <p>
                 * Objects of the following type(s) are allowed in the list
                 * {@link Resource.RelatedItems.RelatedItem.Titles.Title }
                 * 
                 * 
                 */
                public List<Resource.RelatedItems.RelatedItem.Titles.Title> getTitle() {
                    if (title == null) {
                        title = new ArrayList<Resource.RelatedItems.RelatedItem.Titles.Title>();
                    }
                    return this.title;
                }


                /**
                 * <p>Java-Klasse für anonymous complex type.
                 * 
                 * <p>Das folgende Schemafragment gibt den erwarteten Content an, der in dieser Klasse enthalten ist.
                 * 
                 * <pre>
                 * &lt;complexType&gt;
                 *   &lt;simpleContent&gt;
                 *     &lt;extension base="&lt;http://www.w3.org/2001/XMLSchema&gt;string"&gt;
                 *       &lt;attribute name="titleType" type="{http://datacite.org/schema/kernel-4}titleType" /&gt;
                 *       &lt;attribute ref="{http://www.w3.org/XML/1998/namespace}lang"/&gt;
                 *     &lt;/extension&gt;
                 *   &lt;/simpleContent&gt;
                 * &lt;/complexType&gt;
                 * </pre>
                 * 
                 * 
                 */
                @XmlAccessorType(XmlAccessType.FIELD)
                @XmlType(name = "", propOrder = {
                    "value"
                })
                public static class Title {

                    @XmlValue
                    protected String value;
                    @XmlAttribute(name = "titleType")
                    protected TitleType titleType;
                    @XmlAttribute(name = "lang", namespace = "http://www.w3.org/XML/1998/namespace")
                    protected String lang;

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
                     * Ruft den Wert der titleType-Eigenschaft ab.
                     * 
                     * @return
                     *     possible object is
                     *     {@link TitleType }
                     *     
                     */
                    public TitleType getTitleType() {
                        return titleType;
                    }

                    /**
                     * Legt den Wert der titleType-Eigenschaft fest.
                     * 
                     * @param value
                     *     allowed object is
                     *     {@link TitleType }
                     *     
                     */
                    public void setTitleType(TitleType value) {
                        this.titleType = value;
                    }

                    /**
                     * Ruft den Wert der lang-Eigenschaft ab.
                     * 
                     * @return
                     *     possible object is
                     *     {@link String }
                     *     
                     */
                    public String getLang() {
                        return lang;
                    }

                    /**
                     * Legt den Wert der lang-Eigenschaft fest.
                     * 
                     * @param value
                     *     allowed object is
                     *     {@link String }
                     *     
                     */
                    public void setLang(String value) {
                        this.lang = value;
                    }

                }

            }

        }

    }


    /**
     * <p>Java-Klasse für anonymous complex type.
     * 
     * <p>Das folgende Schemafragment gibt den erwarteten Content an, der in dieser Klasse enthalten ist.
     * 
     * <pre>
     * &lt;complexType&gt;
     *   &lt;simpleContent&gt;
     *     &lt;extension base="&lt;http://www.w3.org/2001/XMLSchema&gt;string"&gt;
     *       &lt;attribute name="resourceTypeGeneral" use="required" type="{http://datacite.org/schema/kernel-4}resourceType" /&gt;
     *     &lt;/extension&gt;
     *   &lt;/simpleContent&gt;
     * &lt;/complexType&gt;
     * </pre>
     * 
     * 
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "", propOrder = {
        "value"
    })
    public static class ResourceType {

        @XmlValue
        protected String value;
        @XmlAttribute(name = "resourceTypeGeneral", required = true)
        protected org.edu_sharing.service.handleservicedoi.model.xml.ResourceType resourceTypeGeneral;

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
         * Ruft den Wert der resourceTypeGeneral-Eigenschaft ab.
         * 
         * @return
         *     possible object is
         *     {@link org.edu_sharing.service.handleservicedoi.model.xml.ResourceType }
         *     
         */
        public org.edu_sharing.service.handleservicedoi.model.xml.ResourceType getResourceTypeGeneral() {
            return resourceTypeGeneral;
        }

        /**
         * Legt den Wert der resourceTypeGeneral-Eigenschaft fest.
         * 
         * @param value
         *     allowed object is
         *     {@link org.edu_sharing.service.handleservicedoi.model.xml.ResourceType }
         *     
         */
        public void setResourceTypeGeneral(org.edu_sharing.service.handleservicedoi.model.xml.ResourceType value) {
            this.resourceTypeGeneral = value;
        }

    }


    /**
     * <p>Java-Klasse für anonymous complex type.
     * 
     * <p>Das folgende Schemafragment gibt den erwarteten Content an, der in dieser Klasse enthalten ist.
     * 
     * <pre>
     * &lt;complexType&gt;
     *   &lt;complexContent&gt;
     *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
     *       &lt;sequence&gt;
     *         &lt;element name="rights" maxOccurs="unbounded" minOccurs="0"&gt;
     *           &lt;complexType&gt;
     *             &lt;simpleContent&gt;
     *               &lt;extension base="&lt;http://www.w3.org/2001/XMLSchema&gt;string"&gt;
     *                 &lt;attribute name="rightsURI" type="{http://www.w3.org/2001/XMLSchema}anyURI" /&gt;
     *                 &lt;attribute name="rightsIdentifier" type="{http://www.w3.org/2001/XMLSchema}anySimpleType" /&gt;
     *                 &lt;attribute name="rightsIdentifierScheme" type="{http://www.w3.org/2001/XMLSchema}anySimpleType" /&gt;
     *                 &lt;attribute name="schemeURI" type="{http://www.w3.org/2001/XMLSchema}anyURI" /&gt;
     *                 &lt;attribute ref="{http://www.w3.org/XML/1998/namespace}lang"/&gt;
     *               &lt;/extension&gt;
     *             &lt;/simpleContent&gt;
     *           &lt;/complexType&gt;
     *         &lt;/element&gt;
     *       &lt;/sequence&gt;
     *     &lt;/restriction&gt;
     *   &lt;/complexContent&gt;
     * &lt;/complexType&gt;
     * </pre>
     * 
     * 
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "", propOrder = {
        "rights"
    })
    public static class RightsList {

        protected List<Resource.RightsList.Rights> rights;

        /**
         * Gets the value of the rights property.
         * 
         * <p>
         * This accessor method returns a reference to the live list,
         * not a snapshot. Therefore any modification you make to the
         * returned list will be present inside the Jakarta XML Binding object.
         * This is why there is not a <CODE>set</CODE> method for the rights property.
         * 
         * <p>
         * For example, to add a new item, do as follows:
         * <pre>
         *    getRights().add(newItem);
         * </pre>
         * 
         * 
         * <p>
         * Objects of the following type(s) are allowed in the list
         * {@link Resource.RightsList.Rights }
         * 
         * 
         */
        public List<Resource.RightsList.Rights> getRights() {
            if (rights == null) {
                rights = new ArrayList<Resource.RightsList.Rights>();
            }
            return this.rights;
        }


        /**
         * <p>Java-Klasse für anonymous complex type.
         * 
         * <p>Das folgende Schemafragment gibt den erwarteten Content an, der in dieser Klasse enthalten ist.
         * 
         * <pre>
         * &lt;complexType&gt;
         *   &lt;simpleContent&gt;
         *     &lt;extension base="&lt;http://www.w3.org/2001/XMLSchema&gt;string"&gt;
         *       &lt;attribute name="rightsURI" type="{http://www.w3.org/2001/XMLSchema}anyURI" /&gt;
         *       &lt;attribute name="rightsIdentifier" type="{http://www.w3.org/2001/XMLSchema}anySimpleType" /&gt;
         *       &lt;attribute name="rightsIdentifierScheme" type="{http://www.w3.org/2001/XMLSchema}anySimpleType" /&gt;
         *       &lt;attribute name="schemeURI" type="{http://www.w3.org/2001/XMLSchema}anyURI" /&gt;
         *       &lt;attribute ref="{http://www.w3.org/XML/1998/namespace}lang"/&gt;
         *     &lt;/extension&gt;
         *   &lt;/simpleContent&gt;
         * &lt;/complexType&gt;
         * </pre>
         * 
         * 
         */
        @XmlAccessorType(XmlAccessType.FIELD)
        @XmlType(name = "", propOrder = {
            "value"
        })
        public static class Rights {

            @XmlValue
            protected String value;
            @XmlAttribute(name = "rightsURI")
            @XmlSchemaType(name = "anyURI")
            protected String rightsURI;
            @XmlAttribute(name = "rightsIdentifier")
            @XmlSchemaType(name = "anySimpleType")
            protected String rightsIdentifier;
            @XmlAttribute(name = "rightsIdentifierScheme")
            @XmlSchemaType(name = "anySimpleType")
            protected String rightsIdentifierScheme;
            @XmlAttribute(name = "schemeURI")
            @XmlSchemaType(name = "anyURI")
            protected String schemeURI;
            @XmlAttribute(name = "lang", namespace = "http://www.w3.org/XML/1998/namespace")
            protected String lang;

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
             * Ruft den Wert der rightsURI-Eigenschaft ab.
             * 
             * @return
             *     possible object is
             *     {@link String }
             *     
             */
            public String getRightsURI() {
                return rightsURI;
            }

            /**
             * Legt den Wert der rightsURI-Eigenschaft fest.
             * 
             * @param value
             *     allowed object is
             *     {@link String }
             *     
             */
            public void setRightsURI(String value) {
                this.rightsURI = value;
            }

            /**
             * Ruft den Wert der rightsIdentifier-Eigenschaft ab.
             * 
             * @return
             *     possible object is
             *     {@link String }
             *     
             */
            public String getRightsIdentifier() {
                return rightsIdentifier;
            }

            /**
             * Legt den Wert der rightsIdentifier-Eigenschaft fest.
             * 
             * @param value
             *     allowed object is
             *     {@link String }
             *     
             */
            public void setRightsIdentifier(String value) {
                this.rightsIdentifier = value;
            }

            /**
             * Ruft den Wert der rightsIdentifierScheme-Eigenschaft ab.
             * 
             * @return
             *     possible object is
             *     {@link String }
             *     
             */
            public String getRightsIdentifierScheme() {
                return rightsIdentifierScheme;
            }

            /**
             * Legt den Wert der rightsIdentifierScheme-Eigenschaft fest.
             * 
             * @param value
             *     allowed object is
             *     {@link String }
             *     
             */
            public void setRightsIdentifierScheme(String value) {
                this.rightsIdentifierScheme = value;
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

            /**
             * Ruft den Wert der lang-Eigenschaft ab.
             * 
             * @return
             *     possible object is
             *     {@link String }
             *     
             */
            public String getLang() {
                return lang;
            }

            /**
             * Legt den Wert der lang-Eigenschaft fest.
             * 
             * @param value
             *     allowed object is
             *     {@link String }
             *     
             */
            public void setLang(String value) {
                this.lang = value;
            }

        }

    }


    /**
     * <p>Java-Klasse für anonymous complex type.
     * 
     * <p>Das folgende Schemafragment gibt den erwarteten Content an, der in dieser Klasse enthalten ist.
     * 
     * <pre>
     * &lt;complexType&gt;
     *   &lt;complexContent&gt;
     *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
     *       &lt;sequence&gt;
     *         &lt;element name="size" type="{http://www.w3.org/2001/XMLSchema}string" maxOccurs="unbounded" minOccurs="0"/&gt;
     *       &lt;/sequence&gt;
     *     &lt;/restriction&gt;
     *   &lt;/complexContent&gt;
     * &lt;/complexType&gt;
     * </pre>
     * 
     * 
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "", propOrder = {
        "size"
    })
    public static class Sizes {

        protected List<String> size;

        /**
         * Gets the value of the size property.
         * 
         * <p>
         * This accessor method returns a reference to the live list,
         * not a snapshot. Therefore any modification you make to the
         * returned list will be present inside the Jakarta XML Binding object.
         * This is why there is not a <CODE>set</CODE> method for the size property.
         * 
         * <p>
         * For example, to add a new item, do as follows:
         * <pre>
         *    getSize().add(newItem);
         * </pre>
         * 
         * 
         * <p>
         * Objects of the following type(s) are allowed in the list
         * {@link String }
         * 
         * 
         */
        public List<String> getSize() {
            if (size == null) {
                size = new ArrayList<String>();
            }
            return this.size;
        }

    }


    /**
     * <p>Java-Klasse für anonymous complex type.
     * 
     * <p>Das folgende Schemafragment gibt den erwarteten Content an, der in dieser Klasse enthalten ist.
     * 
     * <pre>
     * &lt;complexType&gt;
     *   &lt;complexContent&gt;
     *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
     *       &lt;sequence&gt;
     *         &lt;element name="subject" maxOccurs="unbounded" minOccurs="0"&gt;
     *           &lt;complexType&gt;
     *             &lt;simpleContent&gt;
     *               &lt;extension base="&lt;http://www.w3.org/2001/XMLSchema&gt;string"&gt;
     *                 &lt;attribute name="subjectScheme" type="{http://www.w3.org/2001/XMLSchema}anySimpleType" /&gt;
     *                 &lt;attribute name="schemeURI" type="{http://www.w3.org/2001/XMLSchema}anyURI" /&gt;
     *                 &lt;attribute name="valueURI" type="{http://www.w3.org/2001/XMLSchema}anyURI" /&gt;
     *                 &lt;attribute name="classificationCode" type="{http://www.w3.org/2001/XMLSchema}anyURI" /&gt;
     *                 &lt;attribute ref="{http://www.w3.org/XML/1998/namespace}lang"/&gt;
     *               &lt;/extension&gt;
     *             &lt;/simpleContent&gt;
     *           &lt;/complexType&gt;
     *         &lt;/element&gt;
     *       &lt;/sequence&gt;
     *     &lt;/restriction&gt;
     *   &lt;/complexContent&gt;
     * &lt;/complexType&gt;
     * </pre>
     * 
     * 
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "", propOrder = {
        "subject"
    })
    public static class Subjects {

        protected List<Resource.Subjects.Subject> subject;

        /**
         * Gets the value of the subject property.
         * 
         * <p>
         * This accessor method returns a reference to the live list,
         * not a snapshot. Therefore any modification you make to the
         * returned list will be present inside the Jakarta XML Binding object.
         * This is why there is not a <CODE>set</CODE> method for the subject property.
         * 
         * <p>
         * For example, to add a new item, do as follows:
         * <pre>
         *    getSubject().add(newItem);
         * </pre>
         * 
         * 
         * <p>
         * Objects of the following type(s) are allowed in the list
         * {@link Resource.Subjects.Subject }
         * 
         * 
         */
        public List<Resource.Subjects.Subject> getSubject() {
            if (subject == null) {
                subject = new ArrayList<Resource.Subjects.Subject>();
            }
            return this.subject;
        }


        /**
         * <p>Java-Klasse für anonymous complex type.
         * 
         * <p>Das folgende Schemafragment gibt den erwarteten Content an, der in dieser Klasse enthalten ist.
         * 
         * <pre>
         * &lt;complexType&gt;
         *   &lt;simpleContent&gt;
         *     &lt;extension base="&lt;http://www.w3.org/2001/XMLSchema&gt;string"&gt;
         *       &lt;attribute name="subjectScheme" type="{http://www.w3.org/2001/XMLSchema}anySimpleType" /&gt;
         *       &lt;attribute name="schemeURI" type="{http://www.w3.org/2001/XMLSchema}anyURI" /&gt;
         *       &lt;attribute name="valueURI" type="{http://www.w3.org/2001/XMLSchema}anyURI" /&gt;
         *       &lt;attribute name="classificationCode" type="{http://www.w3.org/2001/XMLSchema}anyURI" /&gt;
         *       &lt;attribute ref="{http://www.w3.org/XML/1998/namespace}lang"/&gt;
         *     &lt;/extension&gt;
         *   &lt;/simpleContent&gt;
         * &lt;/complexType&gt;
         * </pre>
         * 
         * 
         */
        @XmlAccessorType(XmlAccessType.FIELD)
        @XmlType(name = "", propOrder = {
            "value"
        })
        public static class Subject {

            @XmlValue
            protected String value;
            @XmlAttribute(name = "subjectScheme")
            @XmlSchemaType(name = "anySimpleType")
            protected String subjectScheme;
            @XmlAttribute(name = "schemeURI")
            @XmlSchemaType(name = "anyURI")
            protected String schemeURI;
            @XmlAttribute(name = "valueURI")
            @XmlSchemaType(name = "anyURI")
            protected String valueURI;
            @XmlAttribute(name = "classificationCode")
            @XmlSchemaType(name = "anyURI")
            protected String classificationCode;
            @XmlAttribute(name = "lang", namespace = "http://www.w3.org/XML/1998/namespace")
            protected String lang;

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
             * Ruft den Wert der subjectScheme-Eigenschaft ab.
             * 
             * @return
             *     possible object is
             *     {@link String }
             *     
             */
            public String getSubjectScheme() {
                return subjectScheme;
            }

            /**
             * Legt den Wert der subjectScheme-Eigenschaft fest.
             * 
             * @param value
             *     allowed object is
             *     {@link String }
             *     
             */
            public void setSubjectScheme(String value) {
                this.subjectScheme = value;
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

            /**
             * Ruft den Wert der valueURI-Eigenschaft ab.
             * 
             * @return
             *     possible object is
             *     {@link String }
             *     
             */
            public String getValueURI() {
                return valueURI;
            }

            /**
             * Legt den Wert der valueURI-Eigenschaft fest.
             * 
             * @param value
             *     allowed object is
             *     {@link String }
             *     
             */
            public void setValueURI(String value) {
                this.valueURI = value;
            }

            /**
             * Ruft den Wert der classificationCode-Eigenschaft ab.
             * 
             * @return
             *     possible object is
             *     {@link String }
             *     
             */
            public String getClassificationCode() {
                return classificationCode;
            }

            /**
             * Legt den Wert der classificationCode-Eigenschaft fest.
             * 
             * @param value
             *     allowed object is
             *     {@link String }
             *     
             */
            public void setClassificationCode(String value) {
                this.classificationCode = value;
            }

            /**
             * Ruft den Wert der lang-Eigenschaft ab.
             * 
             * @return
             *     possible object is
             *     {@link String }
             *     
             */
            public String getLang() {
                return lang;
            }

            /**
             * Legt den Wert der lang-Eigenschaft fest.
             * 
             * @param value
             *     allowed object is
             *     {@link String }
             *     
             */
            public void setLang(String value) {
                this.lang = value;
            }

        }

    }


    /**
     * <p>Java-Klasse für anonymous complex type.
     * 
     * <p>Das folgende Schemafragment gibt den erwarteten Content an, der in dieser Klasse enthalten ist.
     * 
     * <pre>
     * &lt;complexType&gt;
     *   &lt;complexContent&gt;
     *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
     *       &lt;sequence&gt;
     *         &lt;element name="title" maxOccurs="unbounded"&gt;
     *           &lt;complexType&gt;
     *             &lt;simpleContent&gt;
     *               &lt;extension base="&lt;http://www.w3.org/2001/XMLSchema&gt;string"&gt;
     *                 &lt;attribute name="titleType" type="{http://datacite.org/schema/kernel-4}titleType" /&gt;
     *                 &lt;attribute ref="{http://www.w3.org/XML/1998/namespace}lang"/&gt;
     *               &lt;/extension&gt;
     *             &lt;/simpleContent&gt;
     *           &lt;/complexType&gt;
     *         &lt;/element&gt;
     *       &lt;/sequence&gt;
     *     &lt;/restriction&gt;
     *   &lt;/complexContent&gt;
     * &lt;/complexType&gt;
     * </pre>
     * 
     * 
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "", propOrder = {
        "title"
    })
    public static class Titles {

        @XmlElement(required = true)
        protected List<Resource.Titles.Title> title;

        /**
         * Gets the value of the title property.
         * 
         * <p>
         * This accessor method returns a reference to the live list,
         * not a snapshot. Therefore any modification you make to the
         * returned list will be present inside the Jakarta XML Binding object.
         * This is why there is not a <CODE>set</CODE> method for the title property.
         * 
         * <p>
         * For example, to add a new item, do as follows:
         * <pre>
         *    getTitle().add(newItem);
         * </pre>
         * 
         * 
         * <p>
         * Objects of the following type(s) are allowed in the list
         * {@link Resource.Titles.Title }
         * 
         * 
         */
        public List<Resource.Titles.Title> getTitle() {
            if (title == null) {
                title = new ArrayList<Resource.Titles.Title>();
            }
            return this.title;
        }


        /**
         * <p>Java-Klasse für anonymous complex type.
         * 
         * <p>Das folgende Schemafragment gibt den erwarteten Content an, der in dieser Klasse enthalten ist.
         * 
         * <pre>
         * &lt;complexType&gt;
         *   &lt;simpleContent&gt;
         *     &lt;extension base="&lt;http://www.w3.org/2001/XMLSchema&gt;string"&gt;
         *       &lt;attribute name="titleType" type="{http://datacite.org/schema/kernel-4}titleType" /&gt;
         *       &lt;attribute ref="{http://www.w3.org/XML/1998/namespace}lang"/&gt;
         *     &lt;/extension&gt;
         *   &lt;/simpleContent&gt;
         * &lt;/complexType&gt;
         * </pre>
         * 
         * 
         */
        @XmlAccessorType(XmlAccessType.FIELD)
        @XmlType(name = "", propOrder = {
            "value"
        })
        public static class Title {

            @XmlValue
            protected String value;
            @XmlAttribute(name = "titleType")
            protected TitleType titleType;
            @XmlAttribute(name = "lang", namespace = "http://www.w3.org/XML/1998/namespace")
            protected String lang;

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
             * Ruft den Wert der titleType-Eigenschaft ab.
             * 
             * @return
             *     possible object is
             *     {@link TitleType }
             *     
             */
            public TitleType getTitleType() {
                return titleType;
            }

            /**
             * Legt den Wert der titleType-Eigenschaft fest.
             * 
             * @param value
             *     allowed object is
             *     {@link TitleType }
             *     
             */
            public void setTitleType(TitleType value) {
                this.titleType = value;
            }

            /**
             * Ruft den Wert der lang-Eigenschaft ab.
             * 
             * @return
             *     possible object is
             *     {@link String }
             *     
             */
            public String getLang() {
                return lang;
            }

            /**
             * Legt den Wert der lang-Eigenschaft fest.
             * 
             * @param value
             *     allowed object is
             *     {@link String }
             *     
             */
            public void setLang(String value) {
                this.lang = value;
            }

        }

    }

}
