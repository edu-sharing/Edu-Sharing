package org.edu_sharing.metadataset.v2.tools;

import com.google.common.collect.Lists;
import net.sourceforge.cardme.engine.VCardEngine;
import net.sourceforge.cardme.vcard.VCard;
import net.sourceforge.cardme.vcard.types.ExtendedType;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.edu_sharing.alfresco.repository.server.authentication.Context;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.metadataset.v2.*;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.client.tools.I18nAngular;
import org.edu_sharing.repository.client.tools.metadata.ValueTool;
import org.edu_sharing.repository.server.tools.DateTool;
import org.edu_sharing.repository.server.tools.VCardConverter;
import org.edu_sharing.repository.tools.URLHelper;
import org.edu_sharing.service.license.LicenseService;
import org.edu_sharing.service.nodeservice.NodeServiceFactory;
import org.edu_sharing.service.nodeservice.NodeServiceHelper;
import org.edu_sharing.service.permission.PermissionServiceFactory;
import org.edu_sharing.service.toolpermission.ToolPermissionServiceFactory;
import org.jsoup.Jsoup;
import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;
import org.owasp.html.Sanitizers;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;


/**
 * Class for rendering mds templates for the RenderService as html
 * Needs the active mds and the node properties
 * @author Torsten
 *
 */
public class MetadataTemplateRenderer {

	private final Map<String, String[]> propertiesNative;

	public enum RenderingMode{
		HTML,
		TEXT
	}
	private RenderingMode renderingMode = RenderingMode.HTML;
	private static final String GROUP_MULTIVALUE_DELIMITER = "[+]";
	private static final String TEXT_LICENSE_SEPERATOR = " / ";
	private static final String TEXT_MULTIVALUE_SEPERATOR = "; ";
	private String userName;
	private NodeRef nodeRef;
	private MetadataSet mds;
	private Map<String, String[]> properties;
	private static Logger logger=Logger.getLogger(MetadataTemplateRenderer.class);


	public MetadataTemplateRenderer(MetadataSet mds, NodeRef nodeRef, String userName, String type, List<String> aspects, Map<String, Object> properties) {
		this.mds = mds;
		this.nodeRef = nodeRef;
		this.userName = userName;

		propertiesNative = convertProps(
				NodeServiceHelper.addVirtualProperties(
						type,
						aspects,
						properties
				)
		);
		this.properties = cleanupTextMultivalueProperties(
				propertiesNative, renderingMode.equals(RenderingMode.HTML)
		);
	}

	public Map<String, String[]> getProcessedProperties(){
		return this.properties;
	}
	public static Map<String, String[]> convertProps(Map<String, Object> props) {
		Map<String, String[]> propsConverted = new HashMap<>();
		for(String key : props.keySet()){
			String keyLocal= CCConstants.getValidLocalName(key);
			if(keyLocal == null || props.get(key) == null) continue;

			String[] values=ValueTool.getMultivalue(props.get(key).toString());
			propsConverted.put(keyLocal, values);
		}
		return propsConverted;
	}

	public RenderingMode getRenderingMode() {
		return renderingMode;
	}

	public void setRenderingMode(RenderingMode renderingMode) {
		this.renderingMode = renderingMode;
		this.properties = cleanupTextMultivalueProperties(
				propertiesNative, renderingMode.equals(RenderingMode.HTML)
		);
	}

	public String render(String groupName) throws IllegalArgumentException {
		if(userName == null || userName.isEmpty()){
			throw new IllegalArgumentException("No username was given. Can't continue rendering metadata template");
		}
		return AuthenticationUtil.runAs(()-> {
			for (MetadataGroup group : mds.getGroups()) {
				if (group.getId().equals(groupName))
					return render(group);
			}
			throw new IllegalArgumentException("Group " + groupName + " was not found in the mds " + mds.getRepositoryId() + ":" + mds.getId());
		},userName);
	}

	private String render(MetadataGroup group) throws IllegalArgumentException {
		String html="";
		for(String view : group.getViews()){
			boolean found=false;
			for(MetadataTemplate template : mds.getTemplates()){
				if(template.getId().equals(view)){
					html += renderTemplate(template);
					found=true;
					break;
				}
			}
			if(!found) {
				html += "Error: View "+view+" was included in group "+group.getId()+" but not found in template list";
			}
		}
		return html;
	}
	public static <T> T firstNonNull(T ...items) {
		for(T i : items) {
			if (i != null) {
				return i;
			}
		}
		return null;
	}
	private String renderTemplate(MetadataTemplate template) throws IllegalArgumentException {
		String html="";
		if(renderingMode.equals(RenderingMode.HTML)) {
			html += "<div class='mdsGroup mdsGroup-" + template.getId() + "'>" + "<h2 class='mdsCaption " + template.getId() + "'>" + template.getCaption() + "</h2>" + "<div class='mdsContent'>";
		}
		String content=template.getHtml();
		for(MetadataWidget srcWidget : mds.getWidgets()){
			MetadataWidget widget=mds.findWidgetForTemplateAndCondition(srcWidget.getId(),template.getId(),properties);
			int start=content.indexOf("<"+srcWidget.getId());
			if(start==-1)
				continue;
			int end=content.indexOf(">",start);
			String first=content.substring(0, start);
			String second=content.substring(end+1);
			String attributes=content.substring(start+1+srcWidget.getId().length(),end);
			StringBuffer widgetHtml=new StringBuffer();
			// widget did not match a condition, remove it
			if(widget!=null) {
				widget = applyAttributes(widget, attributes);
				String[] values = properties.get(widget.getId());
				boolean wasEmpty = false;
				if (values == null) {
					values = new String[]{"-"};
					wasEmpty = true;
				}
				// handle false/disabled same as empty for checkbox widgets
				if("checkbox".equals(widget.getType()) && !wasEmpty) {
					if(values.length == 1 && StringUtils.equalsIgnoreCase(values[0], "false")) {
						wasEmpty = true;
					}
				}
				if("range".equals(widget.getType()) && wasEmpty) {
					String[] from = properties.get(srcWidget.getId() + "_from");
					String[] to = properties.get(srcWidget.getId() + "_to");
					if(from != null && to != null && from.length > 0 && to.length > 0) {
						values = new String[]{from[0] + "-" + to[0]};
						wasEmpty = false;
					}
				}
				if (renderingMode.equals(RenderingMode.HTML)) {
					widgetHtml.append("<div data-widget-id='").append(widget.getId()).append("' class='mdsWidget");
					if (widget.getType() != null) {
						widgetHtml.append(" mdsWidget_").append(widget.getType());
					}
					widgetHtml.append("'").append(attributes).append(">");
					if (widget.getCaption() != null) {
						widgetHtml.append("<h3 class='mdsWidgetCaption'>").append(widget.getCaption()).append("</h3>");
					}
					String innerContainerTag = widget.isMultivalue() ? "ul" : "div";
					widgetHtml
							.append("<")
							.append(innerContainerTag)
							.append(" class='mdsWidgetContent mds_")
							.append(widget.getId().replace(":", "_"));
					if (widget.isMultivalue()) {
						widgetHtml.append(" mdsWidgetMultivalue");
					}

					widgetHtml.append("'>");
				} else if (renderingMode.equals(RenderingMode.TEXT)) {
					widgetHtml.append(widget.getCaption()).append(": ");
				}
				boolean empty = true;
				if ("multivalueTree".equals(widget.getType())) {
					empty = renderTree(widgetHtml, widget);
				} else if ("multivalueCombined".equals(widget.getType())) {
					empty = renderWidgetSubwidgets(widget, widgetHtml);
					wasEmpty = empty;
				} else if ("material_feedback".equals(widget.getType())) {
					empty = renderMaterialFeedback(widget, widgetHtml);
					wasEmpty = empty;
				} else {
					int i = 0;
					for (String value : values) {
						String rawValue = value;
						Map<String, Object> vcardData = null;
						if ("vcard".equals(widget.getType())) {
							ArrayList<Map<String, Object>> map = VCardConverter.vcardToMap(
									// html in vcards gets escaped beforehand for security reason, unescape special chars to not break the format
									org.apache.commons.lang.StringEscapeUtils.unescapeHtml(value)
							);
							if (map.size() > 0) {
								vcardData = map.get(0);
							}
						}
						if (widget.getId().equals("license")) {
							wasEmpty = false;
							String licenseName = properties.containsKey(CCConstants.getValidLocalName(CCConstants.CCM_PROP_IO_COMMONLICENSE_KEY)) ?
									properties.get(CCConstants.getValidLocalName(CCConstants.CCM_PROP_IO_COMMONLICENSE_KEY))[0] : null;
							String licenseVersion = properties.containsKey(CCConstants.getValidLocalName(CCConstants.CCM_PROP_IO_COMMONLICENSE_CC_VERSION)) ?
									properties.get(CCConstants.getValidLocalName(CCConstants.CCM_PROP_IO_COMMONLICENSE_CC_VERSION))[0] : null;

							LicenseService license = new LicenseService();
							String link = license.getLicenseUrl(licenseName, Context.getCurrentInstance().getLocale(), licenseVersion);
							value = "";
							if (renderingMode.equals(RenderingMode.HTML)) {
								if (link != null)
									value = "<a href='" + link + "' target='_blank'>";
								value += "<img src='" +
										// @TODO 5.1 This can be set to dynamic!
										license.getIconUrl(licenseName, false) +
										"' alt=\"\">";
							}
							String name = getLicenseName(licenseName, properties);
							String group = getLicenseGroup(licenseName, properties);
							if (name != null) {
								if (renderingMode.equals(RenderingMode.HTML)) {
									value += "<div class='licenseName'>" + name + "</div>";
								} else if (renderingMode.equals(RenderingMode.TEXT)) {
									value += name;
								}
							}
							if (renderingMode.equals(RenderingMode.HTML) && link != null) {
								value += "</a>";
							}
							if (group != null && !group.equals(name)) {
								if (renderingMode.equals(RenderingMode.HTML)) {
									value += "<div class='licenseGroup'>" + group + "</div>";
								} else if (renderingMode.equals(RenderingMode.TEXT)) {
									if (!value.isEmpty()) {
										value += TEXT_LICENSE_SEPERATOR;
									}
									value += group;
								}
							}
							if (CCConstants.COMMON_LICENSE_CUSTOM.equals(licenseName)) {
								// skipping description
							} else {
								if (renderingMode.equals(RenderingMode.HTML)) {
									value += "<div class='licenseDescription'>" + getLicenseDescription(licenseName) + "</div>";
								} else if (renderingMode.equals(RenderingMode.TEXT)) {
									if (!value.isEmpty()) {
										value += TEXT_LICENSE_SEPERATOR;
									}
									value += getLicenseDescription(licenseName).replaceAll("((<br \\/>)|(\\n))", TEXT_LICENSE_SEPERATOR);
								}
							}
							if (renderingMode.equals(RenderingMode.HTML) && (
									properties.get(CCConstants.getValidLocalName(CCConstants.CCM_PROP_IO_LICENSE_TITLE_OF_WORK)) != null
											|| properties.get(CCConstants.getValidLocalName(CCConstants.CCM_PROP_IO_LICENSE_PROFILE_URL)) != null
							)) {
								value += "<div class='licenseTitleOfWork'>";
								value += "<div class='mdsWidgetCaptionChild'>" +
										I18nAngular.getTranslationAngular("common", "LICENSE.TITLE_OF_WORK")
										+ "</div>";
								boolean source = properties.get(CCConstants.getValidLocalName(CCConstants.CCM_PROP_IO_LICENSE_SOURCE_URL)) != null;
								if (source) {
									value += "<a href='" + properties.get(CCConstants.getValidLocalName(CCConstants.CCM_PROP_IO_LICENSE_SOURCE_URL))[0] + "'>";
								}
								String[] title = properties.get(CCConstants.getValidLocalName(CCConstants.CCM_PROP_IO_LICENSE_TITLE_OF_WORK));
								if(title != null) {
									value += cleanupText(MetadataWidget.TextEscapingPolicy.all, title[0]);
								} else {
									value += cleanupText(MetadataWidget.TextEscapingPolicy.all,
											I18nAngular.getTranslationAngular("common", "MDS.AUTHOR_UNSET")
									);
								}
								if (source) {
									value += "</a>";
								}
								if (properties.get(CCConstants.getValidLocalName(CCConstants.CCM_PROP_IO_LICENSE_PROFILE_URL)) != null) {
									String url = properties.get(CCConstants.getValidLocalName(CCConstants.CCM_PROP_IO_LICENSE_PROFILE_URL))[0];
									if (!url.contains("://")) {
										url = "http://" + url;
									}
									value += " (<a href='" +
											url + "'>" +
											I18nAngular.getTranslationAngular("common", "LICENSE.LINK_AUTHOR")
											+ "</a>)";
								}
								value += "</div>";
							}

						}
						if (value == null || value.trim().isEmpty())
							continue;
						value = renderWidgetValue(widget, value, i);
						boolean isLink = false;
						// do not use global link for vcard, they handle links seperately
						if (!widget.getType().equals("vcard") && widget.getLink() != null && !widget.getLink().isEmpty() && renderingMode.equals(RenderingMode.HTML)) {
							widgetHtml.append("<a href=\"").append(value).append("\" target=\"").append(widget.getLink()).append("\">");
							isLink = true;
						} else if (vcardData != null) {
							value = VCardConverter.getNameForVCard("", vcardData);
						}
						if (renderingMode.equals(RenderingMode.HTML)) {
							widgetHtml
									.append(widget.isMultivalue() ? "<li " : "<div ")
									.append("class='mdsValue' data-value-key='" + rawValue + "'>");
							if (widget.getIcon() != null) {
								widgetHtml.append(insertIcon(widget.getIcon()));
							}
						}
						if (!value.trim().isEmpty()) {
							if (!empty && renderingMode.equals(RenderingMode.TEXT)) {
								widgetHtml.append(TEXT_MULTIVALUE_SEPERATOR);
							}
							empty = false;
						}
						if (renderingMode.equals(RenderingMode.HTML)) {
							widgetHtml.append("<div>");
						}
						if (vcardData != null && renderingMode.equals(RenderingMode.HTML)) {
							Object linkUrl = vcardData.get(widget.getLink() == null ? CCConstants.VCARD_URL :
									widget.getLink().equals("email") ? CCConstants.VCARD_EMAIL
											: null);
							String url = linkUrl != null ? linkUrl.toString() : "";
							if (!url.isEmpty()) {
								if ("email".equals(widget.getLink())) {
									url = "mailto:" + url;
								} else if (!url.contains("://")) {
									url = "http://" + url;
								}
								widgetHtml.append("<a href=\"").append(url).append("\" target=\"_blank\">");
								isLink = true;
							}
						}
						widgetHtml.append(value);
						if (vcardData != null && renderingMode.equals(RenderingMode.HTML)) {
							if (isLink) {
								widgetHtml.append("</a>");
							}
							try {
								VCardEngine vCardEngine = new VCardEngine();
								VCard vcard = vCardEngine.parse(rawValue);
								String persistentIdUrl = null;
								for (ExtendedType type : vcard.getExtendedTypes()) {
									if ((type.getExtendedName().equals(CCConstants.VCARD_T_X_ORCID) ||
											type.getExtendedName().equals(CCConstants.VCARD_T_X_GND_URI) ||
											type.getExtendedName().equals(CCConstants.VCARD_T_X_ROR) ||
											type.getExtendedName().equals(CCConstants.VCARD_T_X_WIKIDATA)
									) && (type.getExtendedValue() != null && !type.getExtendedValue().isEmpty())) {
										persistentIdUrl = type.getExtendedValue();
										break;
									}
								}
								if (persistentIdUrl != null && !persistentIdUrl.isEmpty()) {
									widgetHtml.append("<a href=\"").append(persistentIdUrl)
											.append("\" target=\"blank\">")
											.append(MetadataHelper.getTranslation("vcard_link_persistent_id"))
											.append("</a>");
								}
							} catch (Throwable t) {
								// empty or invalid value
							}
						}

						if (renderingMode.equals(RenderingMode.HTML)) {
							widgetHtml.append("</div>");
						}
						if (renderingMode.equals(RenderingMode.HTML)) {
							widgetHtml.append(widget.isMultivalue() ? "</li>" : "</div>");
							if (isLink) {
								widgetHtml.append("</a>");
							}
						}
						i++;
					}
				}
				if (renderingMode.equals(RenderingMode.HTML)) {
					String innerContainerTag = widget.isMultivalue() ? "ul" : "div";
					widgetHtml.append("</").append(innerContainerTag).append("></div>");
				}
				if ((empty || wasEmpty) && widget.isHideIfEmpty()) {
					widgetHtml = new StringBuffer();
				}
			}
			content = first.trim();
			content += widgetHtml.toString().trim();
			if(!widgetHtml.toString().isEmpty() && renderingMode.equals(RenderingMode.TEXT)){
				// mark line breaks via multivalue seperator so they are not trimmed, and replace them later
				content += CCConstants.MULTIVALUE_SEPARATOR;
			}
			content += second.trim();
		}
		// when hideIfEmpty for template is true, and no content was rendered -> hide
		if(content.trim().isEmpty() && template.getHideIfEmpty()){
			return "";
		}
		if(renderingMode.equals(RenderingMode.TEXT)){
			content = content.replace(CCConstants.MULTIVALUE_SEPARATOR,"\n");
		}
		html+=content;
		if(renderingMode.equals(RenderingMode.HTML)) {
			html += "</div></div>";
		}
		return html;
	}

	private boolean renderMaterialFeedback(MetadataWidget widget, StringBuffer widgetHtml) {
		boolean empty=true;
		String parent = NodeServiceFactory.getLocalService().getPrimaryParent(nodeRef.getId());
		if(parent!=null){
			/* check that
				- the parent is of type collection
				- the user has the toolpermission TOOLPERMISSION_COLLECTION_FEEDBACK
				- the user is not guest
				- the user has the PERMISSION_FEEDBACK permission
				- the user is not administrator (PERMISSION_DELETE) of the collection
			 */
			logger.info(ToolPermissionServiceFactory.getInstance().hasToolPermission(CCConstants.CCM_VALUE_TOOLPERMISSION_MATERIAL_FEEDBACK)+" "+PermissionServiceFactory.getLocalService().hasPermission(StoreRef.PROTOCOL_WORKSPACE,StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier(),parent,CCConstants.PERMISSION_FEEDBACK)
					+" "+PermissionServiceFactory.getLocalService().hasPermission(StoreRef.PROTOCOL_WORKSPACE,StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier(),parent,CCConstants.PERMISSION_DELETE));
			if(
					ToolPermissionServiceFactory.getInstance().hasToolPermission(CCConstants.CCM_VALUE_TOOLPERMISSION_MATERIAL_FEEDBACK) &&
							PermissionServiceFactory.getLocalService().hasPermission(StoreRef.PROTOCOL_WORKSPACE,StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier(),nodeRef.getId(),CCConstants.PERMISSION_FEEDBACK) &&
							!PermissionServiceFactory.getLocalService().hasPermission(StoreRef.PROTOCOL_WORKSPACE,StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier(),nodeRef.getId(),CCConstants.PERMISSION_DELETE)
			){
				try {
					widgetHtml.
							append("<div class=\"mdsValue\">").
							append("<a href=\"").
							append(URLHelper.getNgRenderNodeUrl(nodeRef.getId(), null)).
							append("?action=OPTIONS.MATERIAL_FEEDBACK&feedbackClose=true\"").
							append(" data-es-auth-required=\"true\"").
							append(" data-es-action=\"OPTIONS.MATERIAL_FEEDBACK\"");
					if(widget.getLink()!=null){
						widgetHtml.append(" target=\"").append(widget.getLink()).append("\"");
					}
					widgetHtml.append(">");
					if(widget.getIcon()!=null){
						widgetHtml.append(insertIcon(widget.getIcon()));
					}
					widgetHtml.append(MetadataHelper.getTranslation("material_feedback_button")).append("</a></div>");
					empty=false;
				} catch (Exception e) {
					logger.warn(e.getMessage(),e);
				}
			}
		}
		return empty;
	}

	private boolean renderWidgetSubwidgets(MetadataWidget widget, StringBuffer widgetHtml) {
		boolean empty = true;
		// use the property with the longest value list for render
		long max= Collections.max(widget.getSubwidgets().stream().map((subwidget)->{
			try{
				return (long)properties.get(subwidget.getId()).length;
			}catch(NullPointerException e){}
			return 0L;
		}).collect(Collectors.toSet()));
		if(max>0) {
			empty = false;
			for (int i = 0; i < max; i++) {
				widgetHtml.append("<div class='mdsValue'>");
				for (MetadataWidget.Subwidget subwidget : widget.getSubwidgets()) {
					try {
						widgetHtml.append(renderWidgetValue(mds.findWidget(subwidget.getId()), properties.get(subwidget.getId())[i], i)).append(" ");
					} catch (IndexOutOfBoundsException | NullPointerException e) {
						logger.warn("Sub widget " + subwidget.getId() + " can not be rendered (main widget " + widget.getId() + "): The array values of the sub widgets do not match up", e);
					}
				}
				widgetHtml.append("</div>");
			}
		}
		return empty;
	}

	private boolean renderTree(StringBuffer widgetHtml, MetadataWidget widget) {
		Map<String, MetadataKey> valuesMap=widget.getValuesAsMap();
		String[] keys = properties.get(widget.getId());
		boolean empty=true;
		if(keys != null) {
			int i = 0;
			for (String value : keys) {
				MetadataKey key = valuesMap.get(value);
				if (key == null)
					continue;
				List<String> path = new ArrayList<>();
				int preventInfiniteLoop = 0;
				while (key != null) {
					path.add(key.getCaption());
					key = valuesMap.get(key.getParent());
					if (preventInfiniteLoop++ > 100) {
						logger.error("check valuespace for widget:" + widget.getId() + " key:" + key.getKey());
						break;
					}
				}
				path = Lists.reverse(path);
				int j = 0;
				if (renderingMode.equals(RenderingMode.HTML)) {
					widgetHtml.append("<li class='mdsValue' data-value-key='").append(value).append("'>");
				} else if (renderingMode.equals(RenderingMode.TEXT)) {
					if(i > 0) {
						widgetHtml.append(TEXT_MULTIVALUE_SEPERATOR);
					}
				}
				empty = path.size() == 0;
				for (String p : path) {
					if (j > 0) {
						if (renderingMode.equals(RenderingMode.HTML)) {
							widgetHtml.append("<span class='cdk-visually-hidden'>"+ I18nAngular.getTranslationAngular("common", "SUB_CATEGORY_ARIA")+"'</span>");
							widgetHtml.append("<i class='material-icons' aria-hidden='true'>keyboard_arrow_right</i>");
						} else if (renderingMode.equals(RenderingMode.TEXT)) {
							widgetHtml.append(" -> ");
						}
					}
					widgetHtml.append(p);
					j++;
				}
				if (renderingMode.equals(RenderingMode.HTML)) {
					widgetHtml.append("</li>");
				}
				i++;
			}
		}
		return empty;
	}
	private String renderWidgetValue(MetadataWidget widget,String value, int index){
		if(widget.getType()!=null){
			if(widget.getType().equals("date")){
				try{
					String[] data = properties.get(widget.getId() + CCConstants.LONG_DATE_SUFFIX);
					if(data == null) {
						data = properties.get(widget.getId());
					}
					Date dateValue = new DateTool().getDate(data[0]);
					if(widget.getFormat()!=null && !widget.getFormat().isEmpty()){
						value=new SimpleDateFormat(widget.getFormat()).format(dateValue);
					}
					else{
						value=new DateTool().formatDate(dateValue.getTime());
					}
				}catch(Throwable t){
					// wrong data or text
				}
			} else if (widget.getType().equals("duration")){
				NumberFormat nf = NumberFormat.getInstance();
				nf.setMaximumFractionDigits(0);
				nf.setMinimumIntegerDigits(1);
				long time = -1;
				try {
					time = Math.round(Long.parseLong(value) / 1000. / 60.);
				}catch(Throwable ignored) {
					try {
						time = Math.round(Duration.parse(value).getSeconds() / 60.);
					} catch(Throwable ignored2) {

					}
				}
				if(time != -1) {
					try {
						long mins = time % 60;
						long hours = time / 60;
						value = nf.format(hours) + "h ";
						nf.setMinimumIntegerDigits(2);
						value += nf.format(mins) + "m";
					}catch(Throwable ignored) {

					}
				}
			}
			if("number".equals(widget.getType()) && "bytes".equals(widget.getFormat())) {
				try{
					value=formatFileSize(Long.parseLong(value));
				}catch(Throwable t){
				}
			}
			if(widget.getType().equals("multivalueGroup")) {
				value=formatGroupValue(value,widget);
			}
			if(widget.getType().equals("checkbox")) {
				try{
					value = MetadataHelper.getTranslation(Boolean.parseBoolean(value) ? "boolean_yes" : "boolean_no");
				}catch(Throwable t){
					logger.info("Error parsing value "+value+" for checkbox widget "+widget.getId(),t);
				}
			}
		}
		Map<String, MetadataKey> valuesMap = widget.getValuesAsMap();
		MetadataKey entry = null;
		if(valuesMap.containsKey(value)) {
			entry = valuesMap.get(value);
			value = valuesMap.get(value).getCaption();
		}

		if(widget.getFormat()!=null && !widget.getFormat().isEmpty()){
			if(widget.getFormat().contains("${value}")) {
				value = widget.getFormat().replace("${value}", value);
			}
		}
		if(entry != null) {
			value = "<span class=\"value-caption\">" + value + "</span>";
			if(entry.getDescription() != null) {
				value += " <span class=\"value-description\">" + entry.getDescription() + "</span>";
			}
			if(entry.getIcon() != null) {
				value = "<img src=\"" + (entry.getIcon().startsWith("http://") || entry.getIcon().startsWith("https://") ? entry.getIcon() : "assets/images/" + entry.getIcon()) + "\" alt=\"\"><div class=\"value-group\">" + value + "</div>";
			}
			if(entry.getUrl() != null) {
				value = "<a href=\"" + entry.getUrl() + "\" target=\"_BLANK\">" + value + "</a>";
			}
		}

			return value;
	}

	/*
	public static Map<String, Object> cleanupTextProperties(MetadataSetV2 mds, Map<String, Object> properties) {
		Map<String,Object> cleaned=new HashMap<>();
		for(Map.Entry<String,Object> entry : properties.entrySet()){
			if(entry.getValue()==null)
				cleaned.put(entry.getKey(), null);
			else
				cleaned.put(entry.getKey(), cleanupText(, entry.getValue().toString()));
		}
		return cleaned;
	}
	*/
	public Map<String, String[]> cleanupTextMultivalueProperties(Map<String, String[]> properties, boolean htmlMode) {
		Map<String,String[]> cleaned=new HashMap<>();
		for(Map.Entry<String,String[]> entry : properties.entrySet()){
			if(entry.getValue()==null) {
				cleaned.put(entry.getKey(), null);
			} else {
				MetadataWidget widget;
				try {
					widget = mds.findWidget(entry.getKey());
				}catch(IllegalArgumentException e){
					widget = null;
				}
				MetadataWidget.TextEscapingPolicy textEscapingPolicy = widget == null ?
						MetadataWidget.TextEscapingPolicy.htmlBasic : widget.getTextEscapingPolicy();
				if(htmlMode) {
					cleaned.put(entry.getKey(),
							Arrays.stream(entry.getValue()).map((String v) -> cleanupText(textEscapingPolicy, v)).toArray(String[]::new)
					);
				} else {
					if(textEscapingPolicy.equals(MetadataWidget.TextEscapingPolicy.none)) {
						cleaned.put(entry.getKey(),entry.getValue());
					} else {
						cleaned.put(entry.getKey(),
								Arrays.stream(entry.getValue()).map((String v) -> Jsoup.parse(v).text()).toArray(String[]::new)
						);
					}
				}
			}
		}
		return cleaned;
	}

	private static String cleanupText(MetadataWidget.TextEscapingPolicy textEscapingPolicy, String untrustedHTML) {
		if(textEscapingPolicy.equals(MetadataWidget.TextEscapingPolicy.htmlBasic)) {
			PolicyFactory policy = Sanitizers.FORMATTING.and(Sanitizers.BLOCKS).and(
					new HtmlPolicyBuilder()
							.allowStandardUrlProtocols().allowElements("a")
							.allowAttributes("href", "target").onElements("a").requireRelNofollowOnLinks()
							.toFactory()
			);
			return policy.sanitize(untrustedHTML);
		} else if(textEscapingPolicy.equals(MetadataWidget.TextEscapingPolicy.all)){
			return StringEscapeUtils.escapeHtml4(untrustedHTML).replaceAll("[\\u0000-\\u001F\\u007F-\\u009F]","");
		} else if(textEscapingPolicy.equals(MetadataWidget.TextEscapingPolicy.none)){
			return untrustedHTML;
		} else {
			throw new RuntimeException("Invalid textEscapingPolicy " + textEscapingPolicy);
		}
	}

	private String getLicenseName(String licenseName, Map<String, String[]> properties) {
		if(licenseName==null || licenseName.isEmpty())
			return null;
		String[] description = properties.get(CCConstants.getValidLocalName(CCConstants.LOM_PROP_RIGHTS_RIGHTS_DESCRIPTION));
		if(CCConstants.COMMON_LICENSE_CUSTOM.equals(licenseName) && description != null) {
			return description[0];
		}
		List<String> supported=Arrays.asList(CCConstants.COMMON_LICENSE_CC_ZERO,CCConstants.COMMON_LICENSE_PDM);
		if(licenseName.startsWith(CCConstants.COMMON_LICENSE_CC_BY) || supported.contains(licenseName)) {
			String name=I18nAngular.getTranslationAngular("common","LICENSE.NAMES."+licenseName);
			if(licenseName.startsWith(CCConstants.COMMON_LICENSE_CC_BY)){
				String[] version=properties.get(CCConstants.getValidLocalName(CCConstants.CCM_PROP_IO_COMMONLICENSE_CC_VERSION));
				if(version!=null && version.length>0 && version[0]!=null && !version[0].isEmpty()){
					name+=" ("+version[0];
					try{
						if(Double.parseDouble(version[0])<4.0){
							String[] locale=properties.get(CCConstants.getValidLocalName(CCConstants.CCM_PROP_IO_COMMONLICENSE_CC_LOCALE));
							if(locale!=null && locale.length>0 && locale[0]!=null && !locale[0].isEmpty()) {
								if (locale[0].equals(locale[0].toUpperCase()))
									name += " - " + I18nAngular.getTranslationAngular("common","COUNTRY_CODE."+locale[0]);
								else
									name += " - " + I18nAngular.getTranslationAngular("common","LANGUAGE."+locale[0]);
							}
						}
					}catch(Throwable t){}
					name+=")";
				}
			}
			return name;
		}
		return null;
	}
	private String getLicenseGroup(String licenseName, Map<String, String[]> properties) {
		if(licenseName==null || licenseName.isEmpty())
			licenseName="NONE";
		if(licenseName.equals(CCConstants.COMMON_LICENSE_CUSTOM)) {
			return null;
		}
		if(licenseName.startsWith(CCConstants.COMMON_LICENSE_CC_BY)) {
			licenseName=CCConstants.COMMON_LICENSE_CC_BY;
		}
		if(licenseName.equals(CCConstants.COMMON_LICENSE_PDM)){
			licenseName=CCConstants.COMMON_LICENSE_CC_ZERO;
		}
		return I18nAngular.getTranslationAngular("common","LICENSE.GROUPS."+licenseName);
	}
	private String getLicenseDescription(String licenseName) {
		if(licenseName==null || licenseName.isEmpty())
			licenseName="NONE";

		if(licenseName.equals(CCConstants.COMMON_LICENSE_PDM)){
			licenseName=CCConstants.COMMON_LICENSE_CC_ZERO;
		}
		if(licenseName.startsWith(CCConstants.COMMON_LICENSE_CC_BY)){
			String result=I18nAngular.getTranslationAngular("common","LICENSE.DESCRIPTION.CC_BY");
			if(licenseName.contains("SA")){
				result+="\n"+I18nAngular.getTranslationAngular("common","LICENSE.DESCRIPTION.CC_SHARE_SA");
			}
			if(licenseName.contains("ND")){
				result+="\n"+I18nAngular.getTranslationAngular("common","LICENSE.DESCRIPTION.CC_SHARE_ND");
			}
			if(licenseName.contains("NC")){
				result+="\n"+I18nAngular.getTranslationAngular("common","LICENSE.DESCRIPTION.CC_COMMERCIAL_NC");
			}
			return result;
		}
		return I18nAngular.getTranslationAngular("common","LICENSE.DESCRIPTION."+licenseName);
	}

	private String formatGroupValue(String value,MetadataWidget widget) {
		if(value==null)
			return null;
		String[] splitted = StringUtils.splitByWholeSeparatorPreserveAllTokens(value,MetadataTemplateRenderer.GROUP_MULTIVALUE_DELIMITER);
		String result="";
		int i=0;
		for(String s : splitted) {
			Map<String, MetadataKey> valuesMap = mds.findWidget(widget.getSubwidgets().get(i).getId()).getValuesAsMap();
			if(!s.isEmpty()) {
				if(!result.isEmpty())
					result+=", ";
				if(valuesMap.containsKey(s))
					s=replaceHTML(valuesMap.get(s).getCaption());
				result+=s;
			}
			i++;
		}
		return result;
	}

	private String replaceHTML(String text) {
		return StringEscapeUtils.unescapeHtml4(text);
	}

	private MetadataWidget applyAttributes(MetadataWidget widget, String str) throws IllegalArgumentException {
		try {
			widget=widget.copyInstance();
		}catch(Throwable t) {
			throw new RuntimeException("Failed to serialize MetadataWidget class, check if all attributes are serializable",t);
		}
		while(true){
			str=str.substring(str.indexOf(" ")+1);
			int pos=str.indexOf("=");
			if(pos==-1) {
				break;
			}
			String name=str.substring(0,pos).trim();
			str=str.substring(pos+1);
			String search=" ";
			if(str.charAt(0)=='\''){
				search="'";
			}
			if(str.charAt(0)=='"'){
				search="\"";
			}
			if(!search.equals(" "))
				str=str.substring(1);
			int end=str.indexOf(search);
			String value=str.substring(0,end);
			str=str.substring(end+1);
			try{
				Field field = widget.getClass().getDeclaredField(name);
				field.setAccessible(true);
				if(field.getType().getSimpleName().equalsIgnoreCase("boolean")) {
					field.set(widget, Boolean.parseBoolean(value));
				} else if(field.getType().isEnum()) {
					try {
						field.set(widget, field.getType().getDeclaredMethod("valueOf", String.class).invoke(null, value));
					} catch(InvocationTargetException e) {
						if(e.getTargetException() instanceof IllegalArgumentException) {
							logger.info("enum constant for widget " + widget.getId() +" " + name + "=" + value + " could not be resolved and will be ignored");
						}
					}
				} else {
					field.set(widget, value);
				}
			}catch(Throwable t){
				logger.warn("Invalid attribute found for widget "+widget.getId()+", attribute "+name+" is unknown: " + t.getMessage());
				//throw new IllegalArgumentException("Invalid attribute found for widget "+widget.getId()+", attribute "+name+" is unknown",t);
			}
		}
		return widget;
	}
	private String formatFileSize(long size) {
		String[] sizes=new String[]{"Bytes","KB","MB","GB","TB"};
		double outSize=size;
		int i=0;
		while(outSize>1500){
			outSize/=1024;
			i++;
		}
		NumberFormat format = NumberFormat.getInstance();
		format.setMinimumFractionDigits(0);
		format.setMaximumFractionDigits(1);
		return format.format(outSize)+" "+sizes[i];
	}

	private String insertIcon(String name) {
		return "<i class='material-icons'>"+name+"</i>";
	}

}
