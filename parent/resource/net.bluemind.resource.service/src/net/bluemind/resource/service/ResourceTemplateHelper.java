/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2019
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
package net.bluemind.resource.service;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringEscapeUtils;

import com.google.common.base.Strings;

import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.resource.api.IResources;
import net.bluemind.resource.api.ResourceDescriptor;
import net.bluemind.resource.api.type.IResourceTypes;
import net.bluemind.resource.api.type.ResourceTypeDescriptor;
import net.bluemind.resource.api.type.ResourceTypeDescriptor.Property;
import net.bluemind.resource.helper.IResourceTemplateHelper;

/**
 * Resource template manipulation methods.<br>
 * <i>Note: the word <u>template</u> is often used to denote the processed
 * template, i.e. the template with its variables replaced by the appropriate
 * values.</i>
 */
public class ResourceTemplateHelper implements IResourceTemplateHelper {
	/** The HTML tag containing the transformed template. */
	private static final String TEMPLATE_HTML_TAG_NAME = "resourcetemplate";
	private static final String TEMPLATE_HTML_TAG_BEGIN = "<" + TEMPLATE_HTML_TAG_NAME + " id=\"{id}\">";
	private static final String TEMPLATE_HTML_TAG_END = "</" + TEMPLATE_HTML_TAG_NAME + ">";

	/** Used for cleaning the text before adding a template. */
	private static final Pattern TRAILING_WHITE_SPACES = Pattern.compile("(\\s|<\\s*br\\s*/?\\s*>\\s*)+$");

	/** Separator between the previous text and the appended template. */
	private static final String TEMPLATE_SEPARATOR = "<br>\n";

	/**
	 * Added after the template end tag, it should prevent rich text editors to add
	 * text between the template tags when typing at the end of the editor's text
	 * area.
	 */
	private static final String TEMPLATE_SUFFIX = "<br><br>";
	private static final String TEMPLATE_SUFFIX_REGEX = "(\\s|<\\s*br\\s*/?>)*";

	/**
	 * The regular expression string for matching a resource template. Contains a
	 * placeholder for the resource identifier: <i>{id}</i>.
	 */
	private static final String TEMPLATE_PATTERN = "<\\s*" + TEMPLATE_HTML_TAG_NAME
			+ "\\s+id\\s*=\\s*\"{id}\"\\s*>.*?<\\s*/\\s*" + TEMPLATE_HTML_TAG_NAME + "\\s*>" + TEMPLATE_SUFFIX_REGEX;

	/**
	 * Properties are stored in a single string, separated by special tokens.
	 */
	private static final String PROPERTIES_SEPARATOR = "\n";
	private static final String PROPERTIES_LOCALIZATION_SEPARATOR = "::";

	/**
	 * Note: '[\p{L}\p{N}_]' is the unicode alternative of '\w' (matches characters
	 * with accents)
	 */
	private static final Pattern TEMPLATE_VARIABLES_PATTERN = Pattern.compile("\\$\\{([\\p{L}\\p{N}_\\s]+)\\}");

	/** The default language for the choice of a localized template. */
	private static final String DEFAULT_LANGUAGE_TAG = "fr";

	@Override
	public String processTemplate(final String domainUid, final String resourceUid, final String localeLanguageTag,
			final String organizerName) {
		// check the given uid corresponds to an actual resource
		final ServerSideServiceProvider provider = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);
		final IResources resourcesService = provider.instance(IResources.class, domainUid);
		final ResourceDescriptor resourceDescriptor = resourcesService.get(resourceUid);
		if (resourceDescriptor == null) {
			throw ServerFault.create(ErrorCode.NOT_FOUND,
					new Exception(String.format("No resource found for uid %s", resourceUid)));
		}

		// check the resource type has a template
		final IResourceTypes resourceTypesService = provider.instance(IResourceTypes.class, domainUid);
		final ResourceTypeDescriptor resourceTypeDescriptor = resourceTypesService
				.get(resourceDescriptor.typeIdentifier);
		if (resourceTypeDescriptor.templates.isEmpty()) {
			return null;
		}

		final String template = localizedTemplate(resourceTypeDescriptor, localeLanguageTag);
		if (template.isEmpty()) {
			return null;
		}
		String result = template;

		final Matcher matcher = TEMPLATE_VARIABLES_PATTERN.matcher(template);
		final Map<String, String> props = mapOfProps(resourceTypeDescriptor, resourceDescriptor, organizerName);
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
		result = addTag(result, resourceUid);

		return result;
	}

	private String addTag(final String processedTemplate, final String resourceId) {
		return tagBegin(resourceId) + "\n" + processedTemplate + "\n" + tagEnd();
	}

	/** Retrieve the template corresponding to the given locale. */
	private static String localizedTemplate(final ResourceTypeDescriptor resourceTypeDescriptor,
			String localeLanguageTag) {
		if (localeLanguageTag == null || localeLanguageTag.isEmpty()) {
			localeLanguageTag = DEFAULT_LANGUAGE_TAG;
		}
		String result = resourceTypeDescriptor.templates.get(localeLanguageTag);
		if (result == null) {
			// fallback (to any template found)
			result = resourceTypeDescriptor.templates.values().iterator().next();
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
	private static Map<String, String> mapOfProps(final ResourceTypeDescriptor resourceTypeDescriptor,
			final ResourceDescriptor resourceDescriptor, final String organizerName) {
		final Map<String, String> result = new HashMap<>();

		resourceDescriptor.properties.forEach(prop -> {
			final Property p = resourceTypeDescriptor.property(prop.propertyId);
			final String[] labels = p.label.split(PROPERTIES_SEPARATOR);
			for (final String label : labels) {
				result.put(label.substring(
						label.indexOf(PROPERTIES_LOCALIZATION_SEPARATOR) + PROPERTIES_LOCALIZATION_SEPARATOR.length()),
						prop.value);
			}
		});

		// add common variables FIXME handle localization correctly
		result.put("Organisateur", organizerName);
		result.put("Organizer", organizerName);
		result.put("NomRessource", resourceDescriptor.label);
		result.put("ResourceName", resourceDescriptor.label);

		return result;
	}

	@Override
	public boolean containsTemplate(final String text, final String resourceId) {
		return matcherForTemplateRegex(text, resourceId).find();
	}

	@Override
	public String removeTemplate(final String text, final String resourceId) {
		return matcherForTemplateRegex(text, resourceId).replaceAll("");
	}

	@Override
	public String addTemplate(String text, String processedTemplate) {
		String sanitizedText = sanitizeText(text);
		if (Strings.isNullOrEmpty(processedTemplate)) {
			return sanitizedText;
		}
		String separator = sanitizedText.isEmpty() ? "" : TEMPLATE_SEPARATOR;
		return String.format("%s%s%s%s", sanitizedText, separator, processedTemplate, TEMPLATE_SUFFIX);
	}

	/** Remove trailing white spaces, &lt;br&gt; included. */
	private String sanitizeText(final String description) {
		return TRAILING_WHITE_SPACES.matcher(description).replaceFirst("");
	}

	private Matcher matcherForTemplateRegex(final String text, final String resourceId) {
		return Pattern.compile(TEMPLATE_PATTERN.replace("{id}", resourceId), Pattern.DOTALL).matcher(text);
	}

	@Override
	public String tagBegin(final String resourceId) {
		return TEMPLATE_HTML_TAG_BEGIN.replace("{id}", resourceId);
	}

	@Override
	public String tagEnd() {
		return TEMPLATE_HTML_TAG_END;
	}
}
