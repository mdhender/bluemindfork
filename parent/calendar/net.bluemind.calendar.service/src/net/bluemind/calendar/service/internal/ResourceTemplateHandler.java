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
package net.bluemind.calendar.service.internal;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;

import net.bluemind.calendar.api.VEvent;
import net.bluemind.calendar.api.VEventSeries;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.utils.JsonUtils;
import net.bluemind.icalendar.api.ICalendarElement.Attendee;
import net.bluemind.icalendar.api.ICalendarElement.CUType;
import net.bluemind.icalendar.api.ICalendarElement.Organizer;
import net.bluemind.resource.api.IResources;
import net.bluemind.resource.api.ResourceDescriptor;
import net.bluemind.resource.api.type.IResourceTypes;
import net.bluemind.resource.api.type.ResourceTypeDescriptor;
import net.bluemind.resource.api.type.ResourceTypeDescriptor.Property;
import net.bluemind.user.api.IUserSettings;

/**
 * Handle the case we want to modify the description of a calendar event because
 * a 'resource' attendee has a template.<br>
 * Let's say we have the resource 'Vault 101' of type 'Nuclear bunker' invited
 * to our event with this template:
 * 
 * <pre>
 * ${Organizer} urges you to go to the nuclear bunker ${NuclearBunkerId} localized
 * at ${NuclearBunkerAddress}. Please stay calm.
 * </pre>
 * 
 * Then we replace the variables and append the result to the event
 * description.<br>
 * <br>
 * Notes:
 * <ul>
 * <li>the template is bound to the resource's type
 * {@link ResourceTypeDescriptor}, not the resource instance</li>
 * <li>the variables are defined in
 * {@link ResourceTypeDescriptor#properties}</li>
 * </ul>
 */
public final class ResourceTemplateHandler {
	private static final String TEMPLATE_HTML_TAG = "<resourcetemplate />";
	private static final Pattern TEMPLATE_HTML_TAG_PATTERN = Pattern.compile("<\\s*resourcetemplate\\s*/?>");
	private static final String PROPERTIES_SEPARATOR = "\n";
	private static final String PROPERTIES_LOCALIZATION_SEPARATOR = "::";
	private static final Logger LOGGER = LoggerFactory.getLogger(ResourceTemplateHandler.class);
	private static final Pattern TEMPLATE_VARIABLES_PATTERN = Pattern.compile("\\$\\{([\\w\\s]+)\\}");
	private static final String DEFAULT_LANGUAGE_TAG = "fr";

	/**
	 * Process resources templates, if any, of a newly created event. Then append
	 * the result to the event description.
	 */
	public void handleCreatedEvent(final VEventSeries vEventSeries, final String domainUid) {
		final long start = System.currentTimeMillis();
		vEventSeries.flatten().forEach(vEvent -> {
			vEvent.attendees.stream().filter(this::isResource).forEach(resourceAttendee -> {
				this.addToDescription(vEvent, resourceAttendee, domainUid);
			});
		});
		LOGGER.debug("Handled resource template in {}ms (handleCreatedEvent)", System.currentTimeMillis() - start);
	}

	/**
	 * Process added resources templates of an existing event. Then append the
	 * result to the event description.
	 */
	public void handleAddedResources(final VEvent vEvent, final List<Attendee> addedAttendees, final String domainUid) {
		final long start = System.currentTimeMillis();
		addedAttendees.stream().filter(this::isResource).forEach(resourceAttendee -> {
			this.addToDescription(vEvent, resourceAttendee, domainUid);
		});
		LOGGER.debug("Handled resource template in {}ms (handleAddedResources)", System.currentTimeMillis() - start);
	}

	/**
	 * Process removed resources templates of an existing event. Then remove the
	 * result from the event description.
	 */
	public void handleDeletedResources(final VEvent vEvent, final List<Attendee> deletedAttendees) {
		final long start = System.currentTimeMillis();
		deletedAttendees.stream().filter(this::isResource).forEach(resourceAttendee -> {
			this.removeFromDescription(vEvent);
		});
		LOGGER.debug("Handled resource template in {}ms (handleDeletedResources)", System.currentTimeMillis() - start);
	}

	private boolean isResource(final Attendee attendee) {
		return attendee.cutype == CUType.Resource;
	}

	/**
	 * Detect attendee of kind 'resource', then pick a localized template, then
	 * transform it using {@link ResourceTypeDescriptor#properties}, then add it to
	 * the event description.
	 */
	private void addToDescription(final VEvent vEvent, final Attendee resourceAttendee, final String domainUid) {
		final Optional<String> resourceId = this.toResourceId(resourceAttendee);
		if (!resourceId.isPresent()) {
			throw new ServerFault(
					String.format("Attendee identifier not found: %s", JsonUtils.asString(resourceAttendee)));
		}
		final IServiceProvider provider = this.provider();
		final ResourceDescriptor resourceDescriptor = provider.instance(IResources.class, domainUid)
				.get(resourceId.get());
		final IResourceTypes resourceTypeService = provider.instance(IResourceTypes.class, domainUid);
		final ResourceTypeDescriptor resourceTypeDescriptor = resourceTypeService
				.get(resourceDescriptor.typeIdentifier);
		if (resourceTypeDescriptor.templates != null && !resourceTypeDescriptor.templates.isEmpty()) {
			// this is the organizer's language that is used for the template
			final String localeLanguageTag = this.organizerLanguage(vEvent, domainUid);
			final String descriptionToAdd = this.processTemplate(resourceTypeDescriptor, resourceDescriptor,
					localeLanguageTag, vEvent);
			// append the result of the template to the event's description. Avoid to add it
			// multiple times (in case of an update).
			if (!vEvent.description.contains(descriptionToAdd)) {
				vEvent.description += TEMPLATE_HTML_TAG;
				vEvent.description += descriptionToAdd;
			}
		}
	}

	/** Remove all transformed templates from the description. */
	private void removeFromDescription(final VEvent vEvent) {
		final Matcher matcher = TEMPLATE_HTML_TAG_PATTERN.matcher(vEvent.description);
		if (matcher.find()) {
			vEvent.description = vEvent.description.substring(0, matcher.start());
		}
	}

	private String organizerLanguage(final VEvent vEvent, final String domainUid) {
		final Organizer organizer = vEvent.organizer;
		final String userId = organizer.dir.substring(organizer.dir.lastIndexOf('/') + 1);
		final IUserSettings userSettingsService = this.provider().instance(IUserSettings.class, domainUid);
		return userSettingsService.get(userId).get("lang");
	}

	private Optional<String> toResourceId(final Attendee resourceAttendee) {
		String resourceId = this.pathToId(resourceAttendee.dir);
		if (resourceId == null) {
			resourceId = this.pathToId(resourceAttendee.uri);
		}
		return Optional.fromNullable(resourceId);
	}

	private String pathToId(final String path) {
		return path != null ? path.substring(path.lastIndexOf('/') + 1) : null;
	}

	private IServiceProvider provider() {
		return ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);
	}

	/**
	 * Replace the template variables using
	 * {@link ResourceTypeDescriptor#properties}.
	 */
	private String processTemplate(final ResourceTypeDescriptor resourceTypeDescriptor,
			final ResourceDescriptor resourceDescriptor, final String localeLanguageTag, final VEvent vEvent) {
		final String template = this.localizedTemplate(resourceTypeDescriptor, localeLanguageTag);
		String result = template;
		final Matcher matcher = TEMPLATE_VARIABLES_PATTERN.matcher(template);
		final Map<String, String> props = this.mapOfProps(resourceTypeDescriptor, resourceDescriptor, vEvent);
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

		return result;
	}

	/** Retrieve the template corresponding to the given locale. */
	private String localizedTemplate(final ResourceTypeDescriptor resourceTypeDescriptor, String localeLanguageTag) {
		if (localeLanguageTag == null || localeLanguageTag.isEmpty()) {
			localeLanguageTag = DEFAULT_LANGUAGE_TAG;
		}
		String result = resourceTypeDescriptor.templates.get(localeLanguageTag);
		if (result == null) {
			// fallback (to any template found)
			result = resourceTypeDescriptor.templates.values().iterator().next();
		}
		return result;
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
	private Map<String, String> mapOfProps(final ResourceTypeDescriptor resourceTypeDescriptor,
			final ResourceDescriptor resourceDescriptor, final VEvent vEvent) {
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
		result.put("Organisateur", vEvent.organizer.commonName);
		result.put("Organizer", vEvent.organizer.commonName);
		result.put("NomRessource", resourceDescriptor.label);
		result.put("ResourceName", resourceDescriptor.label);

		return result;
	}

	/**
	 * @return the separator between the already existing description and the
	 *         transformed template
	 */
	public static String separator() {
		return TEMPLATE_HTML_TAG;
	}

}
