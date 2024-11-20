package org.edu_sharing.service.search.model;

import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.mapping.FieldType;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.dictionary.DataTypeDefinition;
import org.alfresco.service.cmr.dictionary.PropertyDefinition;
import org.alfresco.service.cmr.search.SearchParameters;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.Pair;
import org.apache.log4j.Logger;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.springframework.context.ApplicationContext;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;


public class SortDefinition implements Serializable {
	static Logger logger = Logger.getLogger(SortDefinition.class);

	private static final List<String> ALLOWED_SORT_MAIN_PROPERTIES = Collections.singletonList(
			"fullpath"
	);
	transient ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();
	transient ServiceRegistry serviceRegistry = (ServiceRegistry) applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);
	
	List<SortDefinitionEntry> sortDefinitionEntries = new ArrayList<>();

    public static class SortDefinitionEntry implements Serializable{
		String property;
		boolean ascending;
		
		public String getProperty() {
			return property;
		}
		
		public void setProperty(String property) {
			this.property = property;
		}
		
		public boolean isAscending() {
			return ascending;
		}
		
		public void setAscending(boolean ascending) {
			this.ascending = ascending;
		}
		public SortDefinitionEntry(){}
		public SortDefinitionEntry(String property,boolean ascending){
			this.property=property;
			this.ascending=ascending;
		}
	}
	public SortDefinition(){}
	/**
	 * Fills this SortDefinition with a string list of properties
	 * @param sortProperties the names to sort by
	 * @param sortAscending sort ascending or descending
	 */
	public SortDefinition(Iterable<String> sortProperties,List<Boolean> sortAscending){
		this(null,sortProperties,sortAscending);		
	}
	public SortDefinition(String namespace, Iterable<String> sortProperties, List<Boolean> sortAscending) {
		if(sortProperties == null)
			return;			
		if(sortAscending==null) {
			sortAscending= new ArrayList<>();
			sortAscending.add(true);
		}
		int i=0;
		for(String sortProp : sortProperties){
				SortDefinitionEntry entry = new SortDefinitionEntry();
				Boolean sortAsc=sortAscending.size()==1 ? sortAscending.get(0) : sortAscending.get(i);
				entry.setAscending(sortAsc);
				entry.setProperty(namespace!=null && sortProp.split(":").length == 1 ? "{"+namespace+"}"+sortProp : sortProp);
				addSortDefinitionEntry(entry);
				i++;
			}
	}
	public List<SortDefinitionEntry> getSortDefinitionEntries() {
		return sortDefinitionEntries;
	}
	public void addSortDefinitionEntry(SortDefinitionEntry sortDefinitionEntry,int position){
		sortDefinitionEntries.add(position,sortDefinitionEntry);
	}
	public void addSortDefinitionEntry(SortDefinitionEntry sortDefinitionEntry){
		sortDefinitionEntries.add(sortDefinitionEntry);
	}
	/** 
	 * 
	 * @return true if this SortDefinition has entries
	 */
	public boolean hasContent() {
		return !sortDefinitionEntries.isEmpty();
	}
	public List<Pair<QName, Boolean>> asSortProperties() {
		if(!hasContent())
			return null;
		List<Pair<QName, Boolean>> list=new ArrayList<Pair<QName, Boolean>>();
		for(SortDefinitionEntry entry : sortDefinitionEntries){
			list.add(new Pair<QName, Boolean>(QName.createQName(entry.property),entry.ascending));
		}
		return list;
	}
	public String getFirstSortBy() {
		if(!hasContent())
			return null;
		return sortDefinitionEntries.get(0).property;
	}
	public boolean getFirstSortAscending() {
		if(!hasContent())
			return true;
		return sortDefinitionEntries.get(0).ascending;
	}
	public void applyToSearchParameters(SearchParameters searchParameters) {
		// Group by Folders & Files
		sortDefinitionEntries.add(0, new SortDefinitionEntry("TYPE",false));
		for (SortDefinitionEntry sortDefintionEntry : getSortDefinitionEntries()) {
			searchParameters.addSort(sortDefintionEntry.getProperty(), sortDefintionEntry.isAscending());
		}
	}

	public void applyToSearchSourceBuilder(co.elastic.clients.elasticsearch.core.SearchRequest.Builder builder) {
		// Group by Folders & Files
		builder.sort(sort -> sort.field(field -> field.field("type").order(SortOrder.Desc)));
		for (SortDefinitionEntry sortDefintionEntry : getSortDefinitionEntries()) {
			SortOrder sortOrder = sortDefintionEntry.ascending ? SortOrder.Asc : SortOrder.Desc;
			if(sortDefintionEntry.getProperty().equalsIgnoreCase("score")) {
				builder.sort(sort->sort.score(score->score.order(sortOrder)));
			} else if(sortDefintionEntry.getProperty().equalsIgnoreCase("tree")) {
				builder.sort(sort->sort.script(script -> script.script(s->s.source("doc['fullpath'].value + '/' + doc['nodeRef.id'].value"))));
			}else if(ALLOWED_SORT_MAIN_PROPERTIES.contains(sortDefintionEntry.getProperty())) {
				builder.sort(sort->sort.field(field->field.field(sortDefintionEntry.getProperty()).order(sortOrder)));
			}else {
				String addSuffix = "";
				String property = CCConstants.getValidGlobalName(sortDefintionEntry.getProperty());
				if(sortDefintionEntry.getProperty().equalsIgnoreCase("sys:node-uuid")) {
					// do nothing, this field is already a keyword!
				} else if(Arrays.asList("cm:created", "cm:modified").contains(sortDefintionEntry.getProperty())) {
					// use numeric
					addSuffix = "number";
				} else if(List.of("ccm:replicationsourcetimestamp").contains(sortDefintionEntry.getProperty())) {
					// use date
					addSuffix = "date";
				} else if(List.of("cclom:title").contains(sortDefintionEntry.getProperty())) {
					// temporary fix in 9.0, can be removed in 9.1 (tracker: 216445ab)
					addSuffix = "keyword";
				} else if(property != null){
					PropertyDefinition propDef = serviceRegistry.getDictionaryService().getProperty(QName.createQName(property));
					if(propDef != null) {
						if (Stream.of(DataTypeDefinition.TEXT, DataTypeDefinition.MLTEXT, DataTypeDefinition.DATE, DataTypeDefinition.DATETIME, DataTypeDefinition.BOOLEAN).anyMatch(qName -> qName.equals(propDef.getDataType().getName()))) {
							addSuffix = "sort";
						} else if (Stream.of(DataTypeDefinition.INT, DataTypeDefinition.LONG, DataTypeDefinition.FLOAT, DataTypeDefinition.DOUBLE).anyMatch(qName -> qName.equals(propDef.getDataType().getName()))) {
							addSuffix = "number";
						} else {
							logger.warn("Could not detect field type for elastic: " + propDef.getDataType() + ", " + property);
						}
					}
				}
				String name = "properties." + sortDefintionEntry.getProperty() + ((!addSuffix.isEmpty()) ? ("." + addSuffix) :"" );
				// currently, we use a dynamic model which might cause that fields not yet exists. We want to ignore this errors to let the request
				builder.sort(sort->sort.field(field->field.field(name).order(sortOrder).unmappedType(FieldType.Keyword)));
				if(addSuffix.equals("sort") || addSuffix.equals("number")) {
					// we can't assume that the field exists, so for security, we always sort for a keyword field as a second option
					builder.sort(sort->sort.field(field->field
							.field("properties." + sortDefintionEntry.getProperty() + ".keyword")
							.order(sortOrder).unmappedType(FieldType.Keyword)));
				}
			}
		}
	}
}
