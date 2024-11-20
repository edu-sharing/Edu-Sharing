package org.edu_sharing.service.nodeservice;

import org.edu_sharing.metadataset.v2.MetadataKey;
import org.edu_sharing.metadataset.v2.MetadataWidget;
import org.edu_sharing.repository.server.jobs.quartz.MigrateMetadataValuespaceJob;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Interceptor that will map from a source to a target property using the relations of a widget
 */
public class PropertiesSetInterceptorValuespaceMapperAbstract implements PropertiesSetInterceptor{
    private final boolean reverseMapping;
    protected String sourceProperty;
    protected String targetProperty;
    private boolean clearSourceProperty;
    private MigrateMetadataValuespaceJob.Mode mode;
    private final List<MetadataKey.MetadataKeyRelated.Relation> relations;


    protected MetadataWidget sourceWidget;
    protected MetadataWidget targetWidget;
    protected Map<MetadataKey.MetadataKeyRelated.Relation,Map<String, Collection<MetadataKey.MetadataKeyRelated>>> relationCache;
    /**
     * Init this Interceptor with the given property information
     * @param sourceProperty the source property to read the data from
     * @param targetProperty the target property (where to map the data to)
     * @param clearSourceProperty if true, the content of the sourceProperty will be removed
     * @param reverseMapping set to false if the sourceProperty contains the valuespace keys, set to true if the targetProperty contains them
     * @param relations (the relations to search for mappings, the first relation that returns result for a given value in the list will be used)
     */
    public PropertiesSetInterceptorValuespaceMapperAbstract(String sourceProperty,
                                                            String targetProperty,
                                                            boolean clearSourceProperty,
                                                            boolean reverseMapping,
                                                            MigrateMetadataValuespaceJob.Mode mode,
                                                            List<MetadataKey.MetadataKeyRelated.Relation> relations) {
        this.sourceProperty = sourceProperty;
        this.targetProperty = targetProperty;
        this.clearSourceProperty = clearSourceProperty;
        this.reverseMapping = reverseMapping;
        this.mode = mode;
        this.relations = relations;
    }

    /**
     * Set the widget
     * the widget will be used to fetch the relation mapping data
     * The target widget will be checked to constrain the valid valuespace
     */
    public void setWidget(MetadataWidget sourceWidget, MetadataWidget targetWidget) {
        this.sourceWidget = sourceWidget;
        this.targetWidget = targetWidget;
        relationCache = new HashMap<>();
        this.relations.forEach((r) -> relationCache.put(r, this.sourceWidget.getValuespaceMappingByRelation(r)));
    }


    @Override
    public Map<String, Object> beforeSetProperties(PropertiesGetInterceptor.PropertiesContext context) {
        mapValues(context);
        return context.getProperties();
    }

    private void mapValues(PropertiesGetInterceptor.PropertiesContext context) {
        Map<String, Object> map = context.getProperties();
        Object value = map.get(sourceProperty);
        if(clearSourceProperty) {
            map.put(sourceProperty, null);
        }
        List<Map<String, Collection<MetadataKey.MetadataKeyRelated>>> mappedList = relations.stream().map((m) -> relationCache.get(m)).collect(Collectors.toList());
        HashSet<String> result = MigrateMetadataValuespaceJob.mapValueToTarget(context.getNodeRef(),
                mappedList,
                targetWidget.getValues(),
                mode,
                value,
                map.get(targetProperty),
                reverseMapping,
                null
        );
        if(result.size() == 0) {
            result = null;
        }
        map.put(targetProperty, result);
    }
}
