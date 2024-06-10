package org.edu_sharing.restservices;

import java.util.*;
import java.util.stream.Collectors;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.edu_sharing.alfresco.lightbend.LightbendConfigLoader;
import org.edu_sharing.metadataset.v2.MetadataSet;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.client.tools.I18nAngular;
import org.edu_sharing.repository.server.MCAlfrescoAPIClient;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.restservices.statistic.v1.model.Filter;
import org.edu_sharing.restservices.statistic.v1.model.FilterEntry;
import org.edu_sharing.restservices.statistic.v1.model.StatisticEntity;
import org.edu_sharing.restservices.statistic.v1.model.StatisticEntry;
import org.edu_sharing.restservices.statistic.v1.model.Statistics;
import org.edu_sharing.service.mime.MimeTypesV2;
import org.edu_sharing.service.search.SearchServiceFactory;
import org.edu_sharing.service.search.model.SortDefinition;
import org.edu_sharing.service.statistic.StatisticService;
import org.edu_sharing.service.statistic.StatisticServiceFactory;
import org.edu_sharing.service.statistic.StatisticsGlobal;

public class StatisticDao {
    private static Map<String,String> SUB_GROUP_MAPPING=new HashMap<>();
    static{
        SUB_GROUP_MAPPING.put("subject",null);
        SUB_GROUP_MAPPING.put("keywords",CCConstants.LOM_PROP_GENERAL_KEYWORD);
        SUB_GROUP_MAPPING.put("language",CCConstants.LOM_PROP_GENERAL_LANGUAGE);
        SUB_GROUP_MAPPING.put("fileFormat",CCConstants.LOM_PROP_TECHNICAL_FORMAT);
        SUB_GROUP_MAPPING.put("encodingFormat",CCConstants.LOM_PROP_TECHNICAL_FORMAT);
        SUB_GROUP_MAPPING.put("learningResourceType",CCConstants.LOM_PROP_EDUCATIONAL_LEARNINGRESOURCETYPE);
        SUB_GROUP_MAPPING.put("educationalUse",CCConstants.LOM_PROP_EDUCATIONAL_CONTEXT);
        SUB_GROUP_MAPPING.put("intendedEndUserRole",CCConstants.CCM_PROP_IO_REPL_EDUCATIONAL_INTENDEDENDUSERROLE);
    }

	public static StatisticsGlobal getGlobal(String group, List<String> subGroup) throws DAOException {
		try {
			String property="repository.statistics.api.enabled";
            boolean activate= LightbendConfigLoader.get().getBoolean(property);
            if(!activate && !new MCAlfrescoAPIClient().isAdmin()){
                throw new SecurityException(property+" is not set to true in config. No access allowed");
            }
            if(subGroup==null) {
                subGroup = new ArrayList<>();
			}
            //subGroup.add(CCConstants.getValidLocalName(CCConstants.LOM_PROP_TECHNICAL_FORMAT));
			StatisticsGlobal statistics=new StatisticsGlobal();
			/*StatisticsGlobal.Repository repository=new StatisticsGlobal.Repository();
			repository.name=ApplicationInfoList.getHomeRepository().getAppCaption();
			repository.domain=ApplicationInfoList.getHomeRepository().getDomain();
			repository.queryTime=System.currentTimeMillis()/1000;
			statistics.setRepository(repository);
			*/
			StatisticsGlobal.StatisticsUser user=new StatisticsGlobal.StatisticsUser();
			user.count=countUser(null);
			statistics.setUser(user);
			List<StatisticsGlobal.StatisticsKeyGroup> groups=new ArrayList<>();
			StatisticsGlobal.StatisticsGroup overall=new StatisticsGlobal.StatisticsGroup();
            overall.count=(countElements(null));
            overall.subGroups = getFacets(null,subGroup);
            statistics.setOverall(overall);
            for(String g : getPrimaryGroup(group)) {
				String lucene=escapeProperty(getGroupProperty(group))+":\""+g+"\"";
				int count=countElements(lucene);
				if(count>0) {
					StatisticsGlobal.StatisticsKeyGroup entry=new StatisticsGlobal.StatisticsKeyGroup();
					entry.key = g;
					entry.displayName = I18nAngular.getTranslationAngular("common", "LICENSE." + g);
					entry.count = count;
					groups.add(entry);
					entry.subGroups = getFacets(lucene,subGroup);
				}
			}
			statistics.setGroups(groups);
            statistics.setUser(getUser());
			return statistics;
		}
		catch(Throwable t) {
			throw DAOException.mapping(t);
		}
	}

    private static StatisticsGlobal.StatisticsUser getUser() throws Exception {
        StatisticsGlobal.StatisticsUser user = new StatisticsGlobal.StatisticsUser();
        AuthenticationUtil.runAsSystem(()-> {
            user.count = SearchServiceFactory.getLocalService().searchUsers("*", true, 0, 0, new SortDefinition(), null).getTotalCount();
            return null;
        });
        return user;
    }

    private static String getGroupProperty(String group) {
        if(group==null || group.trim().isEmpty()){
            group="license";
        }
        if(group.equals("license")){
            return CCConstants.CCM_PROP_IO_COMMONLICENSE_KEY;
        }
        return CCConstants.getValidGlobalName(group);
    }

    private static Collection<String> getPrimaryGroup(String group) {
	    if(group==null || group.trim().isEmpty()){
            group="license";
        }
        if(group.equals("license")){
            return CCConstants.getAllLicenseKeys();
        }
        throw new IllegalArgumentException("Unsupported groupe type: "+group);
    }

    private static List<StatisticsGlobal.StatisticsGroup.StatisticsSubGroup> getFacets(String lucene, List<String> properties) throws Throwable {
	    if(properties.size()==0)
	        return null;
        List<String> mappedProps = new ArrayList<>(properties.stream().map(prop -> {
            String mapped=SUB_GROUP_MAPPING.get(prop);
            if(mapped==null)
                throw new IllegalArgumentException("Group Type not supported: "+prop);
            return CCConstants.getValidLocalName(mapped);
        }).collect(Collectors.toSet()));

		List<Map<String, Integer>> data = countFacets(lucene, mappedProps);
		int i=0;
		List<StatisticsGlobal.StatisticsGroup.StatisticsSubGroup> facets = new ArrayList<>();
		for(String prop : properties) {
		    String mapped=CCConstants.getValidLocalName(SUB_GROUP_MAPPING.get(prop));
            StatisticsGlobal.StatisticsGroup.StatisticsSubGroup facet = new StatisticsGlobal.StatisticsGroup.StatisticsSubGroup();
			facet.id=prop;
			Map<String, Integer> counts = data.get(mappedProps.indexOf(mapped));
			List<StatisticsGlobal.StatisticsGroup.StatisticsSubGroup.SubGroupItem> result = new ArrayList<>();
			if(prop.equals("fileFormat")) {
				Map<String, Integer> countsSum=new HashMap<>();
				for(String key : counts.keySet()) {
					String mappedMime=MimeTypesV2.getTypeFromMimetype(key);
					if(countsSum.containsKey(mappedMime)) {
						countsSum.put(mappedMime, countsSum.get(mappedMime)+counts.get(key));
					}
					else {
						countsSum.put(mappedMime, counts.get(key));
					}
				}
				counts=countsSum;
				counts.remove("file");
                for(String key : counts.keySet()) {
                    result.add(new StatisticsGlobal.StatisticsGroup.StatisticsSubGroup.SubGroupItem(key,I18nAngular.getTranslationAngular("common","MEDIATYPE."+key),counts.get(key)));
                }
			}
			else {
                for (String key : counts.keySet()) {
                    result.add(new StatisticsGlobal.StatisticsGroup.StatisticsSubGroup.SubGroupItem(key, counts.get(key)));
                }
            }

			facet.count=result;
			facets.add(facet);
		}
		return facets;
	}
	private static String escapeProperty(String property) {
		return "@"+CCConstants.getValidLocalName(property).replace(":","\\:");
	}
	private static String getQuery(String property, List<String> values) {
		String lucene="";
		for(String value : values) {
			if(!lucene.isEmpty())
				lucene+=" OR ";
			lucene+=property + ":\"" + value + "\"";
		}
		return lucene;
	}
	public static int countElements(String lucene) throws Throwable {
		StatisticService statisticService = StatisticServiceFactory
				.getStatisticService(ApplicationInfoList.getHomeRepository().getAppId());
		return (int)statisticService.countForQuery(CCConstants.metadatasetdefault_id, MetadataSet.DEFAULT_CLIENT_QUERY,CCConstants.getValidLocalName(CCConstants.CCM_TYPE_IO), lucene);
	}
	public static List<Map<String,Integer>> countFacets(String lucene, Collection<String> facets) throws Throwable {
		StatisticService statisticService = StatisticServiceFactory
				.getStatisticService(ApplicationInfoList.getHomeRepository().getAppId());
		return statisticService.countFacetsForQuery(CCConstants.metadatasetdefault_id, MetadataSet.DEFAULT_CLIENT_QUERY,CCConstants.getValidLocalName(CCConstants.CCM_TYPE_IO), lucene,facets);
	}
	public static int countUser(String lucene) throws Throwable {
		// does not work at the moment because of scoped search service and permissions
		StatisticService statisticService = StatisticServiceFactory
				.getStatisticService(ApplicationInfoList.getHomeRepository().getAppId());
		return (int)statisticService.countForQuery(CCConstants.metadatasetdefault_id, MetadataSet.DEFAULT_CLIENT_QUERY,"cm:person", lucene);
	}
	public Statistics get(String context, List<String> properties, Filter filter) throws DAOException {

		try {

			org.edu_sharing.service.statistic.Filter backendFilter = new org.edu_sharing.service.statistic.Filter();

			for (FilterEntry entry : filter.getEntries()) {
				org.edu_sharing.service.statistic.FilterEntry backendFilterEntry = new org.edu_sharing.service.statistic.FilterEntry();
				backendFilterEntry.setProperty(entry.getProperty());
				backendFilterEntry.setValues(entry.getValues());
				backendFilter.getEntries().add(backendFilterEntry);
			}

			StatisticService statisticService = StatisticServiceFactory
					.getStatisticService(ApplicationInfoList.getHomeRepository().getAppId());
			org.edu_sharing.service.statistic.Statistics statisticsBackend = statisticService.get(context, properties,
					backendFilter);
			Statistics statistics = new Statistics();
			for (org.edu_sharing.service.statistic.StatisticEntry entry : statisticsBackend.getEntries()) {
				StatisticEntry statEntry = new StatisticEntry();
				statEntry.setProperty(entry.getProperty());
				List<StatisticEntity> entities = new ArrayList<>();
				for (Map.Entry<String, Integer> statEntity : entry.getStatistic().entrySet()) {
					StatisticEntity entity = new StatisticEntity();
					entity.setValue(statEntity.getKey());
					entity.setCount(statEntity.getValue());
					entities.add(entity);
				}

				statEntry.setEntities(entities);
				statistics.getEntries().add(statEntry);
			}

			return statistics;
		} catch (Throwable t) {
			throw DAOException.mapping(t);
		}
	}

}
