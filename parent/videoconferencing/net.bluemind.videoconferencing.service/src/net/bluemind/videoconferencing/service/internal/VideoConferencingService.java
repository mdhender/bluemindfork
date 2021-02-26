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
package net.bluemind.videoconferencing.service.internal;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

import net.bluemind.core.container.api.IContainerManagement;
import net.bluemind.core.rest.BmContext;
import net.bluemind.eclipse.common.RunnableExtensionLoader;
import net.bluemind.icalendar.api.ICalendarElement;
import net.bluemind.icalendar.api.ICalendarElement.Attendee;
import net.bluemind.icalendar.api.ICalendarElement.CUType;
import net.bluemind.resource.api.IResources;
import net.bluemind.resource.api.ResourceDescriptor;
import net.bluemind.resource.api.ResourceDescriptor.PropertyValue;
import net.bluemind.user.api.IUserSettings;
import net.bluemind.videoconferencing.api.IVideoConferenceUid;
import net.bluemind.videoconferencing.api.IVideoConferencing;
import net.bluemind.videoconferencing.api.IVideoConferencingProvider;
import net.bluemind.videoconferencing.service.template.VideoConferencingTemplateHelper;

public class VideoConferencingService implements IVideoConferencing {

	private static final Logger logger = LoggerFactory.getLogger(VideoConferencingService.class);

	private BmContext context;

	private static final List<IVideoConferencingProvider> providers = loadProviders();
	private static final VideoConferencingTemplateHelper templateHelper = new VideoConferencingTemplateHelper();

	public VideoConferencingService(BmContext context) {
		this.context = context;
	}

	@Override
	public ICalendarElement add(ICalendarElement vevent) {
		List<ResourceDescriptor> videoConferencingResoures = getVideoConferencingResource(vevent.attendees);
		if (videoConferencingResoures.isEmpty()) {
			return vevent;
		}

		ResourceDescriptor resourceDescriptor = videoConferencingResoures.get(0);

		Optional<PropertyValue> videoConferencingType = resourceDescriptor.properties.stream()
				.filter(p -> p.propertyId.equals(IVideoConferenceUid.TYPE)).findFirst();

		Optional<IVideoConferencingProvider> videoConferencingProviderOpt = providers.stream()
				.filter(p -> p.id().equals(videoConferencingType.get().value)).findFirst();

		if (!videoConferencingProviderOpt.isPresent()) {
			logger.warn("No implementation for videoconference provider {}", videoConferencingType.get().value);
			return vevent;
		}

		IVideoConferencingProvider videoConferencingProvider = videoConferencingProviderOpt.get();
		IContainerManagement containerMgmtService = context.getServiceProvider().instance(IContainerManagement.class,
				videoConferencingProvider.id() + "-settings-container");

		Map<String, String> settings = containerMgmtService.getSettings();
		String baseUrl = settings.get("url");
		vevent.conference = videoConferencingProvider.getUrl(baseUrl);

		IUserSettings userSettingsService = context.getServiceProvider().instance(IUserSettings.class,
				context.getSecurityContext().getContainerUid());
		String lang = userSettingsService.get(context.getSecurityContext().getSubject()).get("lang");

		String descriptionToAdd = templateHelper.processTemplate(context.getSecurityContext().getContainerUid(),
				videoConferencingProvider.id(), lang, vevent);

		if (vevent.description != null
				&& !templateHelper.containsTemplate(vevent.description, videoConferencingProvider.id())) {
			vevent.description = templateHelper.addTemplate(vevent.description, descriptionToAdd);
		}
		return vevent;
	}

	@Override
	public ICalendarElement remove(ICalendarElement vevent) {
		if (Strings.isNullOrEmpty(vevent.conference)) {
			return vevent;
		}

		vevent.conference = null;

		List<ResourceDescriptor> videoConferencingResoures = getVideoConferencingResource(vevent.attendees);
		ResourceDescriptor resourceDescriptor = videoConferencingResoures.get(0);
		Optional<PropertyValue> videoConferencingType = resourceDescriptor.properties.stream()
				.filter(p -> p.propertyId.equals(IVideoConferenceUid.TYPE)).findFirst();
		Optional<IVideoConferencingProvider> videoConferencingProvider = providers.stream()
				.filter(p -> p.id().equals(videoConferencingType.get().value)).findFirst();
		if (!videoConferencingProvider.isPresent()) {
			logger.warn("No implementation for videoconference provider {}", videoConferencingType.get().value);
			return vevent;
		}
		VideoConferencingTemplateHelper templateHelper = new VideoConferencingTemplateHelper();
		vevent.description = templateHelper.removeTemplate(vevent.description, videoConferencingProvider.get().id());

		vevent.attendees.removeIf(a -> a.cutype == CUType.Resource
				&& a.dir.equals("bm://" + context.getSecurityContext().getContainerUid() + "/resources/"
						+ videoConferencingProvider.get().id()));

		return vevent;
	}

	private List<ResourceDescriptor> getVideoConferencingResource(List<Attendee> attendees) {
		IResources resourceService = context.getServiceProvider().instance(IResources.class,
				context.getSecurityContext().getContainerUid());
		return attendees.stream().filter(a -> a.cutype.equals(CUType.Resource))
				.map(a -> getResource(a, resourceService))
				.filter(res -> res != null && res.typeIdentifier.equals(IVideoConferenceUid.UID))
				.collect(Collectors.toList());
	}

	private ResourceDescriptor getResource(Attendee a, IResources service) {
		String uid = a.dir.substring(a.dir.lastIndexOf("/") + 1);
		return service.get(uid);
	}

	private static List<IVideoConferencingProvider> loadProviders() {
		return new RunnableExtensionLoader<IVideoConferencingProvider>()
				.loadExtensions("net.bluemind.videoconferencing", "provider", "provider", "impl");
	}

}
