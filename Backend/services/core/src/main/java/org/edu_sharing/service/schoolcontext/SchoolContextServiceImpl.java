package org.edu_sharing.service.schoolcontext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.edu_sharing.repository.client.rpc.SchoolContextValues;
import org.edu_sharing.repository.client.rpc.SearchResult;
import org.edu_sharing.repository.client.rpc.SearchToken;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.MCAlfrescoBaseClient;
import org.edu_sharing.repository.update.SchoolContext;

public class SchoolContextServiceImpl implements SchoolContextService {

	MCAlfrescoBaseClient repoClient;
	
	Logger logger = Logger.getLogger(SchoolContextServiceImpl.class);
	public SchoolContextServiceImpl(MCAlfrescoBaseClient repoClient) {
		this.repoClient = repoClient;
	}
	
	@Override
	public SchoolContextValues getSchoolConextValues() {
		SchoolContextValues scv = new SchoolContextValues();
		try{
			MCAlfrescoBaseClient mcAlfrescoBaseClient = repoClient;
			
			fillSchoolContextValues(SchoolContext.TYPE_AGEGROUP,scv.getAgeGroup(),mcAlfrescoBaseClient);
			fillSchoolContextValues(SchoolContext.TYPE_DISCIPLINE,scv.getSchoolSubject(),mcAlfrescoBaseClient);
			fillSchoolContextValues(SchoolContext.TYPE_FEDERALSTATE,scv.getFederalState(),mcAlfrescoBaseClient);
			fillSchoolContextValues(SchoolContext.TYPE_TYPEOFSCHOOL,scv.getTypeOfSchool(),mcAlfrescoBaseClient);
			
			scv.setResultBasedOnCombination(false);
			
		}catch(Throwable e){
			logger.error(e.getMessage(), e);
		}
		return scv;
	}
	
	public SchoolContextValues getSchoolConextValues(String nodeRefFederalState, String nodeRefTypeOfSchool, String nodeRefSchoolSubject, String nodeRefAgeGroup) {
		
		try{
			MCAlfrescoBaseClient mcAlfrescoBaseClient = repoClient;
			
			SearchToken st = new SearchToken();
			ArrayList<String> facettes = new ArrayList<>();
			facettes.add(SchoolContext.PROP_REF_AGEGROUP);
			facettes.add(SchoolContext.PROP_REF_DISCIPLINE);
			facettes.add(SchoolContext.PROP_REF_FEDERALSTATE);
			facettes.add(SchoolContext.PROP_REF_TYPEOFSCHOOL);
			
			st.setCountProps(facettes);
			
			SchoolContextValues scv = new SchoolContextValues();
			
			String baseQuery = "TYPE:\""+SchoolContext.TYPE_SCHOOLCONTEXT+"\"";
			
			String query = baseQuery;
			
			if(nodeRefFederalState != null && !nodeRefFederalState.trim().equals("")){
				query += " AND @essc\\:ref_federalstate:\""+nodeRefFederalState+"\""; 
			}
			
			if(nodeRefTypeOfSchool != null && !nodeRefTypeOfSchool.trim().equals("")){
				query += " AND @essc\\:ref_typeofschool:\""+nodeRefTypeOfSchool+"\""; 
			}
			
			if(nodeRefSchoolSubject != null && !nodeRefSchoolSubject.trim().equals("")){
				query += " AND @essc\\:ref_discipline:\""+nodeRefSchoolSubject+"\""; 
			}
			
			if(nodeRefAgeGroup != null && !nodeRefAgeGroup.trim().equals("")){
				query += " AND @essc\\:ref_agegroup:\""+nodeRefAgeGroup+"\""; 
			}
			
			logger.info(query);
			
			SearchResult result = mcAlfrescoBaseClient.searchSolr(query, 0, 0, facettes, 1, 500000);
			
			for(Map.Entry<String, Map<String,Integer>> entry: result.getCountedProps().entrySet()){
				
					for(Map.Entry<String,Integer> subEntry : entry.getValue().entrySet()){
						String nodeRefString = subEntry.getKey();
												
						String nodeId = nodeRefString.split("workspace://SpacesStore/")[1]; // 	workspace://SpacesStore/05f8c442-9c78-4368-83c2-48ae1eb7e6f2
												
						String name = mcAlfrescoBaseClient.getProperty("workspace", "SpacesStore", nodeId, CCConstants.CM_PROP_C_TITLE);
						
						//edu-sharing properties multilang = true {de_DE=Realschule}
						if(name != null && name.matches("\\{[a-z][a-z]_[A-Z][A-Z]=.*\\}")){
							String[] splitted = name.split("=");
							name = splitted[1].replace("}", "");
						}
						
						if(name != null && name.matches("\\{default=.*\\}")){
							String[] splitted = name.split("=");
							name = splitted[1].replace("}", "");
						}
						
						if(SchoolContext.PROP_REF_FEDERALSTATE.equals(entry.getKey())){
							scv.getFederalState().put(nodeRefString, name);
						}
						if(SchoolContext.PROP_REF_TYPEOFSCHOOL.equals(entry.getKey())){
							scv.getTypeOfSchool().put(nodeRefString, name);
						}
						if(SchoolContext.PROP_REF_DISCIPLINE.equals(entry.getKey())){
							scv.getSchoolSubject().put(nodeRefString, name);
						}
						if(SchoolContext.PROP_REF_AGEGROUP.equals(entry.getKey())){
							scv.getAgeGroup().put(nodeRefString, name);
						}
					}
				
			}
			
			scv.setResultBasedOnCombination(true);
			
			return scv;
			
		}catch(Throwable e){
			logger.error(e.getMessage(),e);
		}
		
		return null;
	}
	
	
	private void fillSchoolContextValues(String type, Map<String, String> targetMap, MCAlfrescoBaseClient mcAlfrescoBaseClient) throws Throwable{
		
		String query = "TYPE:\""+type+"\"";
		
		ArrayList<String> facette = new ArrayList<>();
		facette.add(CCConstants.SYS_PROP_NODE_UID);
		
		SearchResult srAG = mcAlfrescoBaseClient.searchSolr(query, 0, 0, facette, 1, 500000);
		
		Entry<String, Map<String, Integer>> entry = null;
		Iterator<Entry<String, Map<String, Integer>>> iter = srAG.getCountedProps().entrySet().iterator();
		if(iter.hasNext()){
			entry = iter.next();
		}
		
		if(entry != null){
			for(Map.Entry<String, Integer> facetteResult :entry.getValue().entrySet()){
				String nodeRefString = "workspace://SpacesStore/"+ facetteResult.getKey();
				String name = mcAlfrescoBaseClient.getProperty("workspace", "SpacesStore", facetteResult.getKey(), CCConstants.CM_PROP_C_TITLE);
				
				
				targetMap.put(nodeRefString, name);
			}
		}
		
	}
	
}
