package org.edu_sharing.restservices.mds.v1.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.edu_sharing.metadataset.v2.MetadataCondition;
import org.edu_sharing.metadataset.v2.MetadataKey;
import org.edu_sharing.metadataset.v2.MetadataWidget;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
@Schema(description = "")
public class MdsWidget {

	public static class MdsWidgetCondition {
		MetadataCondition.CONDITION_TYPE type;
		private String value;
		private boolean negate;
		private boolean dynamic;
		private String pattern;

		@JsonProperty(required = true)
		public String getValue() {
			return value;
		}
		public void setValue(String value) {
			this.value = value;
		}

		@JsonProperty(required = true)
		public MetadataCondition.CONDITION_TYPE getType() {
			return type;
		}
		public void setType(MetadataCondition.CONDITION_TYPE type) {
			this.type = type;
		}
		@JsonProperty(required = true)
		public boolean isNegate() {
			return negate;
		}
		public void setNegate(boolean negate) {
			this.negate = negate;
		}

		@JsonProperty(required = true)
		public void setDynamic(boolean dynamic) {
			this.dynamic = dynamic;
		}

		public boolean getDynamic() {
			return dynamic;
		}

		public void setPattern(String pattern) {
			this.pattern = pattern;
		}

		public String getPattern() {
			return pattern;
		}
	}
	public static class MdsSubwidget {
		private String id;

		public MdsSubwidget(org.edu_sharing.metadataset.v2.MetadataWidget.Subwidget key) {
			this.id = key.getId();
		}

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

	}
	private Map<MetadataWidget.IdRelation, String> ids = new HashMap<>();
	private String id,caption,bottomCaption,icon,type,link,template, configuration;
	private boolean hasValues;
	private List<MdsValue> values;
	private List<MdsSubwidget> mdsSubwidgets;
	private String placeholder;
	private String unit;
	private String format;
	private Integer min;
	private Integer max;
	private Integer defaultMin;
	private Integer defaultMax;
	private Integer step;
	private boolean isExtended;
	/**
	 * info if suggestions for new valuespace entries are allowed for this widget
	 */
	private boolean allowValuespaceSuggestions;
	private boolean isHideIfEmpty;
	private MetadataWidget.Required isRequired;
	private boolean allowempty;
	private String defaultvalue;

	@Schema(description = "When true, a set defaultvalue will still trigger the search to show an active filter. When false (default), the defaultvalue will be shown as if no filter is active")
	@Getter
	@Setter
	private boolean countDefaultvalueAsFilter;
	private boolean isSearchable;
	private MdsWidgetCondition condition;
	private int maxlength;
	private MetadataWidget.InteractionType interactionType;

	private MetadataWidget.WidgetFilterMode filterMode;
	private MetadataWidget.WidgetExpandable expandable;

	public MdsWidget(){}
	public MdsWidget(MetadataWidget widget) {
		this.id=widget.getId();
		this.ids=widget.getIds();
		this.caption=widget.getCaption();
		this.bottomCaption=widget.getBottomCaption();
		this.icon=widget.getIcon();
		this.type=widget.getType();
		this.link=widget.getLink();
		this.defaultvalue=widget.getDefaultvalue();
		this.countDefaultvalueAsFilter=widget.getCountDefaultvalueAsFilter();
		this.placeholder=widget.getPlaceholder();
		this.maxlength=widget.getMaxlength();
		this.interactionType=widget.getInteractionType();
		this.filterMode=widget.getFilterMode();
		this.expandable=widget.getExpandable();
		this.unit=widget.getUnit();
		this.configuration=widget.getConfiguration();
		this.min=widget.getMin();
		this.max=widget.getMax();
		this.defaultMin=widget.getDefaultMin();
		this.defaultMax=widget.getDefaultMax();
		this.step=widget.getStep();
		this.format=widget.getFormat();
		this.template=widget.getTemplate();
		this.allowValuespaceSuggestions =widget.getSuggestionReceiver() != null;
		this.isExtended=widget.isExtended();
		this.isHideIfEmpty=widget.isHideIfEmpty();
		this.isRequired=widget.getRequired();
		this.allowempty=widget.isAllowempty();
		this.isSearchable=widget.isSearchable();
		if(widget.getCondition()!=null) {
			this.condition=new MdsWidgetCondition();
			this.condition.setValue(widget.getCondition().getValue());
			this.condition.setType(widget.getCondition().getType());
			this.condition.setNegate(widget.getCondition().isNegate());
			this.condition.setDynamic(widget.getCondition().isDynamic());
			this.condition.setPattern(widget.getCondition().getPattern());
		}
		if(widget.getValues()!=null){
			this.hasValues = true;
			if(widget.isValuespaceClient()) {
				values = new ArrayList<>();
				for (MetadataKey key : widget.getValues()) {
					values.add(new MdsValue(key));
				}
			}
		}
		if(widget.getSubwidgets()!=null){
			mdsSubwidgets =new ArrayList<>();
			for(MetadataWidget.Subwidget key : widget.getSubwidgets()){
				mdsSubwidgets.add(new MdsSubwidget(key));
			}
		}

	}

	@JsonProperty
	public Map<MetadataWidget.IdRelation, String> getIds() {
		return ids;
	}

	public void setIds(Map<MetadataWidget.IdRelation, String> ids) {
		this.ids = ids;
	}

	@JsonProperty
	public boolean isAllowValuespaceSuggestions() {
		return allowValuespaceSuggestions;
	}

	public void setAllowValuespaceSuggestions(boolean allowValuespaceSuggestions) {
		this.allowValuespaceSuggestions = allowValuespaceSuggestions;
	}

	@JsonProperty
	public MdsWidgetCondition getCondition() {
		return condition;
	}
	public void setCondition(MdsWidgetCondition condition) {
		this.condition = condition;
	}
	@JsonProperty("isSearchable")
	public boolean isSearchable() {
		return isSearchable;
	}
	public void setSearchable(boolean searchable) {
		this.isSearchable = searchable;
	}
	@JsonProperty("bottomCaption")
	public String getBottomCaption() {
		return bottomCaption;
	}
	public void setBottomCaption(String bottomCaption) {
		this.bottomCaption = bottomCaption;
	}
	@JsonProperty("defaultvalue")
	public String getDefaultvalue() {
		return defaultvalue;
	}
	public void setDefaultvalue(String defaultvalue) {
		this.defaultvalue = defaultvalue;
	}
	@JsonProperty("template")
	public String getTemplate() {
		return template;
	}
	public void setTemplate(String template) {
		this.template = template;
	}
	@JsonProperty("icon")
	public String getIcon() {
		return icon;
	}
	public void setIcon(String icon) {
		this.icon = icon;
	}
	@JsonProperty("placeholder")
	public String getPlaceholder() {
		return placeholder;
	}
	public void setPlaceholder(String placeholder) {
		this.placeholder = placeholder;
	}
	@JsonProperty("min")
	public Integer getMin() {
		return min;
	}
	public void setMin(Integer min) {
		this.min = min;
	}
	@JsonProperty("max")
	public Integer getMax() {
		return max;
	}
	public void setMax(Integer max) {
		this.max = max;
	}
	@JsonProperty("defaultMin")
	public Integer getDefaultMin() {
		return defaultMin;
	}
	public void setDefaultMin(Integer defaultMin) {
		this.defaultMin = defaultMin;
	}
	@JsonProperty("defaultMax")
	public Integer getDefaultMax() {
		return defaultMax;
	}
	public void setDefaultMax(Integer defaultMax) {
		this.defaultMax = defaultMax;
	}
	@JsonProperty("step")
	public Integer getStep() {
		return step;
	}
	public void setStep(Integer step) {
		this.step = step;
	}
	@JsonProperty("hideIfEmpty")
	public boolean isHideIfEmpty() {
		return isHideIfEmpty;
	}
	public void setHideIfEmpty(boolean hideIfEmpty) {
		isHideIfEmpty = hideIfEmpty;
	}
	@JsonProperty("isExtended")
	public boolean isExtended() {
		return isExtended;
	}
	public void setExtended(boolean isExtended) {
		this.isExtended = isExtended;
	}
	@JsonProperty("allowempty")
	public boolean isAllowempty() {
		return allowempty;
	}
	public void setAllowempty(boolean allowempty) {
		this.allowempty = allowempty;
	}
	@JsonProperty("isRequired")
	public MetadataWidget.Required isRequired() {
		return isRequired;
	}
	public void setRequired(MetadataWidget.Required isRequired) {
		this.isRequired = isRequired;
	}
	@JsonProperty("values")
	public List<MdsValue> getValues() {
		return values;
	}
	public void setValues(List<MdsValue> values) {
		this.values = values;
	}

	@JsonProperty("hasValues")
	public boolean isHasValues() {
		return hasValues;
	}

	public void setHasValues(boolean hasValues) {
		this.hasValues = hasValues;
	}

	@JsonProperty("caption")
	public String getCaption() {
		return caption;
	}
	public void setCaption(String caption) {
		this.caption = caption;
	}
	@JsonProperty("type")
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	@JsonProperty
	public String getLink() {
		return link;
	}

	public void setLink(String link) {
		this.link = link;
	}
	@JsonProperty("id")
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	@JsonProperty
	public String getFormat() {
		return format;
	}
	public void setFormat(String format) {
		this.format = format;
	}
	@JsonProperty("unit")
	public String getUnit() {
		return unit;
	}
	public void setUnit(String unit) {
		this.unit = unit;
	}
	@JsonProperty
	public List<MdsSubwidget> getSubwidgets() {
		return mdsSubwidgets;
	}
	public void setSubwidgets(List<MdsSubwidget> mdsSubwidgets) {
		this.mdsSubwidgets = mdsSubwidgets;
	}
	@JsonProperty
	public int getMaxlength() {
		return maxlength;
	}

	public void setMaxlength(int maxlength) {
		this.maxlength = maxlength;
	}

	@JsonProperty
	public String getConfiguration() {
		return configuration;
	}

	public void setConfiguration(String configuration) {
		this.configuration = configuration;
	}

	@JsonProperty
	public MetadataWidget.InteractionType getInteractionType() {
		return interactionType;
	}

	public void setInteractionType(MetadataWidget.InteractionType interactionType) {
		this.interactionType = interactionType;
	}

	@JsonProperty
	public MetadataWidget.WidgetFilterMode getFilterMode() {
		return filterMode;
	}

	public void setFilterMode(MetadataWidget.WidgetFilterMode filterMode) {
		this.filterMode = filterMode;
	}

	@JsonProperty
	public MetadataWidget.WidgetExpandable getExpandable() {
		return expandable;
	}

	public void setExpandable(MetadataWidget.WidgetExpandable expandable) {
		this.expandable = expandable;
	}
}

