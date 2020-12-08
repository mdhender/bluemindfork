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

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.calendar.api.VEvent;
import net.bluemind.calendar.api.VEventSeries;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.utils.JsonUtils;
import net.bluemind.icalendar.api.ICalendarElement.Attendee;
import net.bluemind.icalendar.api.ICalendarElement.CUType;
import net.bluemind.icalendar.api.ICalendarElement.Organizer;
import net.bluemind.resource.api.type.ResourceTypeDescriptor;
import net.bluemind.resource.helper.IResourceTemplateHelper;
import net.bluemind.resource.helper.ResourceTemplateHelpers;
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
 * 
 * @see ResourceTemplateHelper
 */
public final class ResourceTemplateHandler {
	private static final Logger LOGGER = LoggerFactory.getLogger(ResourceTemplateHandler.class);
	private static final IResourceTemplateHelper RESOURCE_TEMPLATE_HELPER = ResourceTemplateHelpers.getInstance();

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
			this.removeFromDescription(vEvent, resourceAttendee);
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
			LOGGER.warn("Attendee identifier not found {}", JsonUtils.asString(resourceAttendee));
			return;
		}
		final String localeLanguageTag = this.organizerLanguage(vEvent, domainUid);
		final String organizerName = vEvent.organizer.commonName;
		final String descriptionToAdd = RESOURCE_TEMPLATE_HELPER.processTemplate(domainUid, resourceId.get(),
				localeLanguageTag, organizerName);
		// append the result of the template to the event's description
		// avoid to add it multiple times (in case of an update)
		if (vEvent.description != null
				&& !RESOURCE_TEMPLATE_HELPER.containsTemplate(vEvent.description, resourceId.get())) {
			vEvent.description = RESOURCE_TEMPLATE_HELPER.addTemplate(vEvent.description, descriptionToAdd);
		}
	}

	/** Remove the transformed template from the description. */
	private void removeFromDescription(final VEvent vEvent, final Attendee resourceAttendee) {
		final Optional<String> resourceId = this.toResourceId(resourceAttendee);
		if (!resourceId.isPresent()) {
			LOGGER.warn("Attendee identifier not found {}", JsonUtils.asString(resourceAttendee));
			return;
		}
		vEvent.description = RESOURCE_TEMPLATE_HELPER.removeTemplate(vEvent.description, resourceId.get());
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

		return Optional.ofNullable(resourceId);
	}

	private String pathToId(final String path) {
		return path != null ? path.substring(path.lastIndexOf('/') + 1) : null;
	}

	private IServiceProvider provider() {
		return ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);
	}

}
