package org.edu_sharing.restservices.mds.v1.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.edu_sharing.metadataset.v2.MetadataCondition;
import org.edu_sharing.metadataset.v2.MetadataKey;
import org.edu_sharing.metadataset.v2.MetadataWidget;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class MdsWidget {

    @Data
    public static class MdsWidgetCondition {
        @JsonProperty(required = true)
        private MetadataCondition.CONDITION_TYPE type;
        @JsonProperty(required = true)
        private String value;
        @JsonProperty(required = true)
        private boolean negate;
        @JsonProperty(required = true)
        private boolean dynamic;
        private String pattern;
    }

    @Data
    public static class MdsSubwidget {
        private String id;

        public MdsSubwidget(org.edu_sharing.metadataset.v2.MetadataWidget.Subwidget key) {
            this.id = key.getId();
        }
    }

    private Map<MetadataWidget.IdRelation, String> ids = new HashMap<>();
    private String id;
    private String caption;
    private String bottomCaption;
    private String icon;
    private String type;
    private String link;
    private String template;
    private String configuration;
    private boolean hasValues;
    private List<MdsValue> values;
    private List<MdsSubwidget> subwidgets;
    private String placeholder;
    private String unit;
    private String format;
    private Integer min;
    private Integer max;
    private Integer defaultMin;
    private Integer defaultMax;
    private Integer step;
    @JsonProperty("isExtended")
    private boolean isExtended;
    /**
     * info if suggestions for new valuespace entries are allowed for this widget
     */
    private boolean allowValuespaceSuggestions;
    private boolean hideIfEmpty;
    @JsonProperty("isRequired")
    private MetadataWidget.Required isRequired;
    private boolean allowempty;
    private String defaultvalue;

    @Schema(description = "When true, a set defaultvalue will still trigger the search to show an active filter. When false (default), the defaultvalue will be shown as if no filter is active")
    private boolean countDefaultvalueAsFilter;
    @JsonProperty("isSearchable")
    private boolean isSearchable;
    private MdsWidgetCondition condition;
    private int maxlength;
    private MetadataWidget.InteractionType interactionType;

    private MetadataWidget.WidgetFilterMode filterMode;
    private MetadataWidget.WidgetExpandable expandable;

    public MdsWidget() {
    }

    public MdsWidget(MetadataWidget widget) {
        this.id = widget.getId();
        this.ids = widget.getIds();
        this.caption = widget.getCaption();
        this.bottomCaption = widget.getBottomCaption();
        this.icon = widget.getIcon();
        this.type = widget.getType();
        this.link = widget.getLink();
        this.defaultvalue = widget.getDefaultvalue();
        this.countDefaultvalueAsFilter = widget.getCountDefaultvalueAsFilter();
        this.placeholder = widget.getPlaceholder();
        this.maxlength = widget.getMaxlength();
        this.interactionType = widget.getInteractionType();
        this.filterMode = widget.getFilterMode();
        this.expandable = widget.getExpandable();
        this.unit = widget.getUnit();
        this.configuration = widget.getConfiguration();
        this.min = widget.getMin();
        this.max = widget.getMax();
        this.defaultMin = widget.getDefaultMin();
        this.defaultMax = widget.getDefaultMax();
        this.step = widget.getStep();
        this.format = widget.getFormat();
        this.template = widget.getTemplate();
        this.allowValuespaceSuggestions = widget.getSuggestionReceiver() != null;
        this.isExtended = widget.isExtended();
        this.hideIfEmpty = widget.isHideIfEmpty();
        this.isRequired = widget.getRequired();
        this.allowempty = widget.isAllowempty();
        this.isSearchable = widget.isSearchable();
        if (widget.getCondition() != null) {
            this.condition = new MdsWidgetCondition();
            this.condition.setValue(widget.getCondition().getValue());
            this.condition.setType(widget.getCondition().getType());
            this.condition.setNegate(widget.getCondition().isNegate());
            this.condition.setDynamic(widget.getCondition().isDynamic());
            this.condition.setPattern(widget.getCondition().getPattern());
        }
        if (widget.getValues() != null) {
            this.hasValues = true;
            if (widget.isValuespaceClient()) {
                values = new ArrayList<>();
                for (MetadataKey key : widget.getValues()) {
                    values.add(new MdsValue(key));
                }
            }
        }
        if (widget.getSubwidgets() != null) {
            subwidgets = new ArrayList<>();
            for (MetadataWidget.Subwidget key : widget.getSubwidgets()) {
                subwidgets.add(new MdsSubwidget(key));
            }
        }

    }
}

