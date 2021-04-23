/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2021
  *
  * This file is part of BlueMind. BlueMind is a messaging and collaborative
  * solution.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of either the GNU Affero General Public License as
  * published by the Free Software Foundation (version 3 of the License).
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.videoconferencing.service.template;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringEscapeUtils;

import com.google.common.base.Strings;

import io.vertx.core.json.JsonObject;
import net.bluemind.core.container.api.IContainerManagement;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.icalendar.api.ICalendarElement;
import net.bluemind.resource.api.ResourceDescriptor;
import net.bluemind.user.api.IUserSettings;

public class VideoConferencingTemplateHelper {

	private static final String SEPARATOR = "<div>~.~.~.~.~.~.~.~.~.~.~.~.~.~.~.~</div>";

	/** The HTML tag containing the transformed template. */
	private static final String TEMPLATE_HTML_TAG_NAME = "videoconferencingtemplate";
	private static final String TEMPLATE_HTML_TAG_BEGIN = "<" + TEMPLATE_HTML_TAG_NAME + " id=\"{id}\"><br>"
			+ SEPARATOR;
	private static final String TEMPLATE_HTML_TAG_END = SEPARATOR + "<br></" + TEMPLATE_HTML_TAG_NAME + ">";

	/** Used for cleaning the text before adding a template. */
	private static final Pattern TRAILING_WHITE_SPACES = Pattern.compile("(\\s|<\\s*br\\s*/?\\s*>\\s*)+$");

	/**
	 * The regular expression string for matching a resource template. Contains a
	 * placeholder for the resource identifier: <i>{id}</i>.
	 */
	private static final String TEMPLATE_PATTERN = "<\\s*" + TEMPLATE_HTML_TAG_NAME
			+ "\\s+id\\s*=\\s*\"{id}\"\\s*>.*?<\\s*/\\s*" + TEMPLATE_HTML_TAG_NAME + "\\s*>";

	/**
	 * Note: '[\p{L}\p{N}_]' is the unicode alternative of '\w' (matches characters
	 * with accents)
	 */
	private static final Pattern TEMPLATE_VARIABLES_PATTERN = Pattern.compile("\\$\\{([\\p{L}\\p{N}_\\s]+)\\}");

	/** The default language for the choice of a localized template. */
	private static final String DEFAULT_LANGUAGE_TAG = "fr";

	public String processTemplate(final BmContext context, ItemValue<ResourceDescriptor> resource,
			final ICalendarElement vevent) {
		// check the given uid corresponds to an actual resource
		final ServerSideServiceProvider provider = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);

		IContainerManagement containerMgmtService = provider.instance(IContainerManagement.class,
				resource.uid + "-settings-container");
		Map<String, String> settings = containerMgmtService.getSettings();

		IUserSettings userSettingsService = context.getServiceProvider().instance(IUserSettings.class,
				context.getSecurityContext().getContainerUid());
		String lang = userSettingsService.get(context.getSecurityContext().getSubject()).get("lang");

		final String template = localizedTemplate(settings.get("templates"), lang);

		String result = template;

		final Matcher matcher = TEMPLATE_VARIABLES_PATTERN.matcher(template);
		final Map<String, String> props = mapOfProps(resource.value, vevent.organizer.commonName, vevent.conference);
		while (matcher.find()) {
			final String propertyName = matcher.group(1);
			final String propertyValue = props.get(propertyName);
			if (propertyValue != null) {
				result = result.replaceAll(String.format("\\$\\{%s\\}", propertyName), propertyValue);
			}
		}

		// remove lines having not-replaced variables
		final StringBuilder filteredResult = new StringBuilder();
		int index = 0;
		final String[] resultLines = result.split("\\n");
		for (final String resultLine : resultLines) {
			if (!TEMPLATE_VARIABLES_PATTERN.matcher(resultLine).find()) {
				filteredResult.append(resultLine);
				if (index != resultLines.length - 1) {
					filteredResult.append('\n');
				}
			}
			index++;
		}
		result = filteredResult.toString();

		// add special tag
		result = addTag(result, resource.uid);

		return result;
	}

	private String addTag(final String processedTemplate, final String resourceId) {
		return tagBegin(resourceId) + processedTemplate + tagEnd();
	}

	/** Retrieve the template corresponding to the given locale. */
	private static String localizedTemplate(final String templatesStr, String localeLanguageTag) {
		if (localeLanguageTag == null || localeLanguageTag.isEmpty()) {
			localeLanguageTag = DEFAULT_LANGUAGE_TAG;
		}

		Map<String, Object> templates = new JsonObject(templatesStr).getMap();

		String result = (String) templates.get(localeLanguageTag);
		if (result == null) {
			// fallback (to any template found)
			result = (String) templates.values().iterator().next();
		}

		// some variables like ${étage} may be broken due to HTML escaping:
		// ${&eacute;tage}
		return StringEscapeUtils.unescapeHtml(result);
	}

	/**
	 * Build the map of properties keyed by their localized labels (i.e.: one
	 * property value mays have 2 entries if 2 locales exist). Also add special
	 * properties, like the name of the event organizer.<br>
	 * <br>
	 * Property key sample:
	 * 
	 * <pre>
	 * fr::MaClefDePropEnFrancais\nen::MyEnglishPropKey
	 * </pre>
	 */
	private static Map<String, String> mapOfProps(final ResourceDescriptor resourceDescriptor,
			final String organizerName, String url) {

		final Map<String, String> result = new HashMap<>();

		// add common variables FIXME handle localization correctly
		result.put("Organisateur", organizerName);
		result.put("Organizer", organizerName);
		result.put("NomRessource", resourceDescriptor.label);
		result.put("ResourceName", resourceDescriptor.label);

		String anchor = "<a href=\"" + url + "\" target=\"_blank\">" + url + "</a>";

		result.put("URL", anchor);

		return result;
	}

	public boolean containsTemplate(final String text, final String resourceId) {
		return matcherForTemplateRegex(text, resourceId).find();
	}

	public String removeTemplate(final String text, final String resourceId) {
		return matcherForTemplateRegex(text, resourceId).replaceAll("");
	}

	public String addTemplate(String text, String processedTemplate) {
		String sanitizedText = sanitizeText(text);
		if (Strings.isNullOrEmpty(processedTemplate)) {
			return sanitizedText;
		}
		return String.format("%s%s", sanitizedText, processedTemplate);
	}

	/** Remove trailing white spaces, &lt;br&gt; included. */
	private String sanitizeText(final String description) {
		return TRAILING_WHITE_SPACES.matcher(description).replaceFirst("");
	}

	private Matcher matcherForTemplateRegex(final String text, final String resourceId) {
		return Pattern.compile(TEMPLATE_PATTERN.replace("{id}", resourceId), Pattern.DOTALL).matcher(text);
	}

	private String tagBegin(final String resourceId) {
		return TEMPLATE_HTML_TAG_BEGIN.replace("{id}", resourceId);
	}

	private String tagEnd() {
		return TEMPLATE_HTML_TAG_END;
	}
}
