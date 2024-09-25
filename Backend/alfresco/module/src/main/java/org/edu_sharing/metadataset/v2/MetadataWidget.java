package org.edu_sharing.metadataset.v2;

import com.google.gson.internal.LinkedHashTreeMap;
import com.google.gson.internal.LinkedTreeMap;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.edu_sharing.utils.TreeNode;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

@Data
public class MetadataWidget extends MetadataTranslatable {

    private String configuration;
	private ValuespaceMerge valuespaceMerge = ValuespaceMerge.separate;

    public enum Required {
        mandatory,
        mandatoryForPublish,
        recommended,
        optional,
        ignore
    }

    public enum IdRelation {
        graphql,
    }

    public enum InteractionType {
        Input,
        None
    }

    public enum WidgetExpandable {
        disabled,
        expanded,
        collapsed
    }

    public enum WidgetFilterMode {
        disabled,
        auto,
        always
    }
	public enum ValuespaceMerge {
		/**
		 * all valuespaces are presented as an invidual tree on the main level (default)
		 */
		separate,
		/**
		 * all valuespaces are merged into one list/tree
		 */
		merge,
	}
    public enum TextEscapingPolicy {
        // no escaping, strongly discouraged since it can allow XSS vulnerabilities if the data comes from untrusted sources
        none,
        // escape html but allow basic formatting and links (default)
        htmlBasic,
        // escape all data, only allow text
        all
    }

    public static class Subwidget implements Serializable {
        private String id;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

    }

    private static final String[] MULTIVALUE_WIDGETS = new String[]{
            "vcard",
            "checkboxHorizontal",
            "checkboxVertical",
            "multivalueTree",
            "multivalueBadges",
            "multivalueFixedBadges",
            "multivalueSuggestBadges",
            "multivalueGroup",
            "multioption",
            "multivalueCombined"
    };

    private Map<IdRelation, String> ids = new HashMap<>();
    private String id;
    private String type; //return MetadataReaderV2.getTranslation(this,caption,locale);
    private String caption;
    private String bottomCaption;
    private String icon;    //return MetadataReaderV2.getTranslation(this,placeholder,locale);
    private String placeholder;
    private String defaultvalue;
    private String template;
    private String suggestionSource;
    private String suggestionQuery;
    private String suggestDisplayProperty;
    private String unit;
    private String format;
    private String valuespaceSort = "default";
    private Integer min;
    private Integer max;
    private Integer defaultMin;
    private Integer defaultMax;
    private Integer step;
    private boolean extended;
    private boolean allowempty;
    private boolean valuespaceClient = true;
    private boolean hideIfEmpty;
    private boolean inherit = true;
    private Boolean countDefaultvalueAsFilter = false;
    private Required required = Required.optional;
    private Map<String, MetadataKey> values;
    private List<Subwidget> subwidgets;
    private int maxlength;
    private TextEscapingPolicy textEscapingPolicy = TextEscapingPolicy.htmlBasic;
    private InteractionType interactionType = InteractionType.Input;
    private WidgetFilterMode filterMode = WidgetFilterMode.disabled;
    private WidgetExpandable expandable = WidgetExpandable.disabled;
    /**
     * hint for the client if this widget creates a link to the search
     * so e.g. if you click a keyword, you can be directed to the search with this keyword as filter
     */
    private boolean searchable;
    private MetadataCondition condition;
    private String link;
    private String suggestionReceiver;


    public boolean isMultivalue() {
        return Arrays.asList(MULTIVALUE_WIDGETS).contains(type);
    }

    public Collection<MetadataKey> getValues() {
        if(values == null) {
            return null;
        }
        return values.values();
    }

    public void setValues(List<MetadataKey> values) {
        this.values = new LinkedHashMap<>(values.size());
        values.forEach(v -> this.values.put(v.getKey(), v));
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof MetadataWidget) {
            MetadataWidget other = (MetadataWidget) obj;
            if (other.id.equals(id)) {
                return templateEquals(other) && conditionEquals(other);
            }
            return false;
        }
        return super.equals(obj);
    }

    private boolean templateEquals(MetadataWidget other) {
        if (other.template == null)
            return template == null;
        return other.template.equals(template);
    }

    private boolean conditionEquals(MetadataWidget other) {
        if (other.condition == null)
            return condition == null;
        return other.condition.equals(condition);
    }

    public Map<String, MetadataKey> getValuesAsMap() {
        return values == null ? Collections.emptyMap() : values;
    }

    public Map<String, Collection<MetadataKey.MetadataKeyRelated>> getValuespaceMappingByRelation(MetadataKey.MetadataKeyRelated.Relation relation) {
        Map<String, Collection<MetadataKey.MetadataKeyRelated>> map = new HashMap<>();
        for (MetadataKey value : values.values()) {
            map.put(value.getKey(), value.getRelated().stream().filter(r -> r.getRelation().equals(relation)).collect(Collectors.toList()));
        }
        return map;
    }

    public Map<String, MetadataKey> getRootValuespaceMappings() {
        Map<String, MetadataKey> result = new LinkedHashMap<>();
        values.values().stream().filter(x -> x.getParent() == null).forEach(x->result.put(x.getKey(),x));
        return result;

    }

    public TreeNode<MetadataKey> getValuespaceTree() {
        return TreeNode.of(values.values(), MetadataKey::getKey, MetadataKey::getParent);
    }


	public void setValuespaceCombineStrategy(ValuespaceMerge valuespaceMerge) {
		this.valuespaceMerge = valuespaceMerge;
	}

	public ValuespaceMerge getValuespaceMerge() {
		return valuespaceMerge;
	}
}
