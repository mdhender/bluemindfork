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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

import net.bluemind.calendar.api.CalendarSettingsData;
import net.bluemind.calendar.api.CalendarSettingsData.Day;
import net.bluemind.calendar.api.ICalendarSettings;
import net.bluemind.calendar.api.ICalendarUids;
import net.bluemind.calendar.api.VEvent;
import net.bluemind.core.api.Email;
import net.bluemind.core.container.api.IContainerManagement;
import net.bluemind.core.container.api.IContainers;
import net.bluemind.core.container.model.ContainerDescriptor;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.model.acl.AccessControlEntry;
import net.bluemind.core.container.model.acl.Verb;
import net.bluemind.core.container.service.internal.RBACManager;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.domain.api.IDomainSettings;
import net.bluemind.eclipse.common.RunnableExtensionLoader;
import net.bluemind.icalendar.api.ICalendarElement;
import net.bluemind.icalendar.api.ICalendarElement.Attendee;
import net.bluemind.icalendar.api.ICalendarElement.CUType;
import net.bluemind.resource.api.IResources;
import net.bluemind.resource.api.ResourceDescriptor;
import net.bluemind.resource.api.ResourceDescriptor.PropertyValue;
import net.bluemind.resource.api.ResourceReservationMode;
import net.bluemind.videoconferencing.api.IVideoConferenceUids;
import net.bluemind.videoconferencing.api.IVideoConferencing;
import net.bluemind.videoconferencing.api.IVideoConferencingProvider;
import net.bluemind.videoconferencing.api.VideoConference;
import net.bluemind.videoconferencing.api.VideoConferencingResourceDescriptor;
import net.bluemind.videoconferencing.service.template.VideoConferencingTemplateHelper;

public class VideoConferencingService implements IVideoConferencing {

	private static final Logger logger = LoggerFactory.getLogger(VideoConferencingService.class);

	private BmContext context;
	private String domainUid;
	private RBACManager rbac;

	private static final List<IVideoConferencingProvider> providers = loadProviders();
	private static final VideoConferencingTemplateHelper templateHelper = new VideoConferencingTemplateHelper();

	public VideoConferencingService(BmContext context, String domainUid) {
		this.context = context;
		this.domainUid = domainUid;
		rbac = new RBACManager(context);
	}

	@Override
	public VEvent add(VEvent vevent) {
		List<ItemValue<ResourceDescriptor>> videoConferencingResoures = getVideoConferencingResource(vevent.attendees);
		if (videoConferencingResoures.isEmpty()) {
			return vevent;
		}

		ItemValue<ResourceDescriptor> resource = videoConferencingResoures.get(0);
		Optional<PropertyValue> videoConferencingType = resource.value.properties.stream()
				.filter(p -> p.propertyId.equals(IVideoConferenceUids.PROVIDER_TYPE)).findFirst();

		Optional<IVideoConferencingProvider> videoConferencingProviderOpt = providers.stream()
				.filter(p -> p.id().equals(videoConferencingType.get().value)).findFirst();

		if (!videoConferencingProviderOpt.isPresent()) {
			logger.warn("No implementation for videoconference provider {}", videoConferencingType.get().value);
			return vevent;
		}

		IVideoConferencingProvider videoConferencingProvider = videoConferencingProviderOpt.get();

		Set<String> requiredRoles = videoConferencingProvider.getRequiredRoles();
		if (!requiredRoles.isEmpty()) {
			rbac.check(requiredRoles);
		}

		IContainerManagement containerMgmtService = context.getServiceProvider().instance(IContainerManagement.class,
				resource.uid + "-settings-container");

		VideoConference conferenceInfo = videoConferencingProvider.getConferenceInfo(context,
				containerMgmtService.getSettings(), resource, vevent);

		if (vevent.conference == null || vevent.conference.trim().isEmpty()) {
			vevent.conference = conferenceInfo.conference;
		}

		if (vevent.conferenceId == null || vevent.conferenceId.trim().isEmpty()) {
			vevent.conferenceId = conferenceInfo.conferenceId;
		}

		if (vevent.description == null) {
			vevent.description = "";
		}

		if (!templateHelper.containsTemplate(vevent.description, resource.uid)) {
			vevent.description = templateHelper.addTemplate(vevent.description, conferenceInfo.description);
		}

		return vevent;
	}

	@Override
	public VEvent remove(VEvent vevent) {
		if (Strings.isNullOrEmpty(vevent.conference)) {
			logger.info("Video conference not removed from to the event {} on domain {} because does not exist",
					vevent.summary, domainUid);
			return vevent;
		}

		String confId = vevent.conferenceId;

		vevent.conference = null;
		vevent.conferenceId = null;
		vevent.conferenceConfiguration = new HashMap<>();

		List<ItemValue<ResourceDescriptor>> videoConferencingResources = getVideoConferencingResource(vevent.attendees);
		if (videoConferencingResources.isEmpty()) {
			return vevent;
		}

		ItemValue<ResourceDescriptor> resource = videoConferencingResources.get(0);
		Optional<PropertyValue> videoConferencingType = resource.value.properties.stream()
				.filter(p -> p.propertyId.equals(IVideoConferenceUids.PROVIDER_TYPE)).findFirst();
		Optional<IVideoConferencingProvider> videoConferencingProvider = providers.stream()
				.filter(p -> p.id().equals(videoConferencingType.get().value)).findFirst();
		if (!videoConferencingProvider.isPresent()) {
			logger.warn("No implementation for videoconference provider {}", videoConferencingType.get().value);
			return vevent;
		}
		vevent.description = templateHelper.removeTemplate(vevent.description, resource.uid);

		vevent.attendees.removeIf(a -> a.cutype == CUType.Resource && a.dir
				.equals("bm://" + context.getSecurityContext().getContainerUid() + "/resources/" + resource.uid));

		IContainerManagement containerMgmtService = context.getServiceProvider().instance(IContainerManagement.class,
				resource.uid + "-settings-container");

		videoConferencingProvider.get().deleteConference(context, containerMgmtService.getSettings(), confId);

		return vevent;
	}

	private List<ItemValue<ResourceDescriptor>> getVideoConferencingResource(List<Attendee> attendees) {
		IResources resourceService = context.getServiceProvider().instance(IResources.class,
				context.getSecurityContext().getContainerUid());
		return attendees.stream().filter(a -> a.cutype == CUType.Resource).map(a -> getResource(a, resourceService))
				.filter(res -> res.isPresent()
						&& res.get().value.typeIdentifier.equals(IVideoConferenceUids.RESOURCETYPE_UID))
				.map(Optional::get).collect(Collectors.toList());
	}

	private Optional<ItemValue<ResourceDescriptor>> getResource(Attendee a, IResources service) {
		String uid = a.dir.substring(a.dir.lastIndexOf("/") + 1);
		ResourceDescriptor res = service.get(uid);
		if (res != null) {
			return Optional.ofNullable(ItemValue.create(uid, res));
		}
		return Optional.empty();
	}

	private static List<IVideoConferencingProvider> loadProviders() {
		return new RunnableExtensionLoader<IVideoConferencingProvider>()
				.loadExtensions("net.bluemind.videoconferencing", "provider", "provider", "impl");
	}

	@Override
	public VEvent update(VEvent old, VEvent current) {
		if (Strings.isNullOrEmpty(old.conference)) {
			return add(current);
		}

		List<ItemValue<ResourceDescriptor>> oldConferenceResources = getVideoConferencingResource(old.attendees);
		if (oldConferenceResources.isEmpty()) {
			return current;
		}

		resetConferenceTemplate(current, oldConferenceResources.get(0));

		List<ItemValue<ResourceDescriptor>> videoConferencingResoures = getVideoConferencingResource(current.attendees);
		if (videoConferencingResoures.isEmpty()) {
			current.conference = null;
			current.conferenceId = null;
			current.conferenceConfiguration = new HashMap<>();
			return current;
		}

		return add(current);
	}

	private void resetConferenceTemplate(ICalendarElement current,
			ItemValue<ResourceDescriptor> oldResourceDescriptor) {
		current.description = templateHelper.removeTemplate(current.description, oldResourceDescriptor.uid);
	}

	@Override
	public void createResource(String uid, VideoConferencingResourceDescriptor descriptor) {
		Optional<IVideoConferencingProvider> provider = providers.stream()
				.filter(p -> p.id().equals(descriptor.provider)).findFirst();

		if (!provider.isPresent()) {
			logger.warn("No provider {}, skip resource creation", descriptor.provider);
			return;
		}

		IServiceProvider sp = context.getServiceProvider();

		IResources resourcesService = sp.instance(IResources.class, domainUid);

		ResourceDescriptor resource = new ResourceDescriptor();
		resource.label = descriptor.label;
		resource.typeIdentifier = IVideoConferenceUids.RESOURCETYPE_UID;
		resource.properties = new ArrayList<>();
		resource.properties.add(PropertyValue.create(IVideoConferenceUids.PROVIDER_TYPE, descriptor.provider));
		String email = UUID.randomUUID().toString().toLowerCase() + "@" + domainUid;
		resource.emails = Arrays.asList(Email.create(email, true, true));
		resource.reservationMode = ResourceReservationMode.AUTO_ACCEPT;

		logger.info("Create videoconferencing resource for domain {}, label {}, provider {}", domainUid,
				descriptor.label, descriptor.provider);

		resourcesService.create(uid, resource);

		// icon
		if (provider.get().getIcon().isPresent()) {
			resourcesService.setIcon(uid, provider.get().getIcon().get());
		}

		// calendar settings
		IDomainSettings domSettingsService = sp.instance(IDomainSettings.class, domainUid);
		Map<String, String> domSettings = domSettingsService.get();
		CalendarSettingsData calSettings = createCalendarSettings(domSettings);
		ICalendarSettings calSettingsService = sp.instance(ICalendarSettings.class,
				ICalendarUids.resourceCalendar(uid));
		calSettingsService.set(calSettings);

		// default calendar acl
		if (!descriptor.acls.isEmpty()) {
			IContainerManagement containerManagementService = sp.instance(IContainerManagement.class,
					ICalendarUids.resourceCalendar(uid));
			containerManagementService.setAccessControlList(descriptor.acls);
		}

		// container settings
		String resourceSettingsContainerUid = uid + "-settings-container";
		ContainerDescriptor cd = new ContainerDescriptor();
		cd.uid = resourceSettingsContainerUid;
		cd.domainUid = domainUid;
		cd.name = resourceSettingsContainerUid;
		cd.owner = uid;
		cd.type = "container_settings";
		IContainers containersService = sp.instance(IContainers.class);
		logger.info("Create videoconferencing resource settings container {}", cd.uid);

		containersService.create(cd.uid, cd);

		// acls
		IContainerManagement containerManagementService = sp.instance(IContainerManagement.class,
				resourceSettingsContainerUid);
		containerManagementService.setAccessControlList(Arrays.asList(AccessControlEntry.create(domainUid, Verb.Read)));

	}

	private CalendarSettingsData createCalendarSettings(Map<String, String> domainSettings) {
		CalendarSettingsData calSettings = new CalendarSettingsData();
		if (domainSettings.containsKey("working_days")) {
			calSettings.workingDays = getWorkingDays(domainSettings.get("working_days"));
		} else {
			calSettings.workingDays = Arrays.asList(Day.MO, Day.TU, Day.WE, Day.TH, Day.FR);
		}
		if (domainSettings.containsKey("timezone")) {
			calSettings.timezoneId = domainSettings.get("timezone");
		} else {
			calSettings.timezoneId = "UTC";
		}
		if (domainSettings.containsKey("work_hours_start")) {
			calSettings.dayStart = toMillisOfDay(domainSettings.get("work_hours_start"));
		} else {
			calSettings.dayStart = 9 * 60 * 60 * 1000;
		}
		if (domainSettings.containsKey("work_hours_end")) {
			calSettings.dayEnd = toMillisOfDay(domainSettings.get("work_hours_end"));
		} else {
			calSettings.dayEnd = 18 * 60 * 60 * 1000;
		}
		if (domainSettings.containsKey("min_duration")) {
			calSettings.minDuration = Math.max(60, Integer.parseInt(domainSettings.get("min_duration")));
		} else {
			calSettings.minDuration = 60;
		}
		if (!validMinDuration(calSettings.minDuration)) {
			calSettings.minDuration = 60;
		}
		return calSettings;
	}

	private boolean validMinDuration(Integer minDuration) {
		return minDuration == 60 || minDuration == 120 || minDuration == 720 || minDuration == 1440;
	}

	private Integer toMillisOfDay(String value) {
		double time = Double.parseDouble(value);
		int timeHour = (int) Double.parseDouble(value);
		int timeMinute = (int) ((time - timeHour) * 60);
		int minutes = timeHour * 60 + timeMinute;
		return minutes * 60 * 1000;
	}

	private List<Day> getWorkingDays(String string) {
		List<Day> days = new ArrayList<>();
		for (String dayString : string.split(",")) {
			switch (dayString.trim().toLowerCase()) {
			case "mon":
				days.add(Day.MO);
				break;
			case "tue":
				days.add(Day.TU);
				break;
			case "wed":
				days.add(Day.WE);
				break;
			case "thu":
				days.add(Day.TH);
				break;
			case "fri":
				days.add(Day.FR);
				break;
			case "sat":
				days.add(Day.SA);
				break;
			case "sun":
				days.add(Day.SU);
				break;
			}
		}
		return days;
	}
}
