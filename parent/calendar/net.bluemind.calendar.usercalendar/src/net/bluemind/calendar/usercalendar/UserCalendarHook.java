/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2016
 *
 * This file is part of BlueMind. BlueMind is a messaging and collaborative
 * solution.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of either the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License).
 *
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.calendar.usercalendar;

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.calendar.api.CalendarView;
import net.bluemind.calendar.api.CalendarView.CalendarViewType;
import net.bluemind.calendar.api.ICalendarUids;
import net.bluemind.calendar.api.ICalendarView;
import net.bluemind.calendar.api.ICalendarViewUids;
import net.bluemind.calendar.api.IFreebusyMgmt;
import net.bluemind.calendar.api.IFreebusyUids;
import net.bluemind.calendar.api.internal.IInCoreCalendarView;
import net.bluemind.calendar.service.UserCalendarServiceFactory;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.api.IContainerManagement;
import net.bluemind.core.container.api.IContainers;
import net.bluemind.core.container.model.ContainerDescriptor;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.model.acl.AccessControlEntry;
import net.bluemind.core.container.model.acl.Verb;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.user.api.User;
import net.bluemind.user.hook.DefaultUserHook;

public class UserCalendarHook extends DefaultUserHook {

	private static final String CALENDAR_VIEW = "calendarview";

	private static Logger logger = LoggerFactory.getLogger(UserCalendarHook.class);

	@Override
	public void onUserCreated(BmContext context, String domainUid, ItemValue<User> user) {
		if (!user.value.system) {
			createDefaultCalendar(domainUid, user);
			createViewsContainer(domainUid, user);
			createFreebusyContainer(domainUid, user);
		}
	}

	@Override
	public void onUserUpdated(BmContext context, String domainUid, ItemValue<User> previous, ItemValue<User> current)
			throws ServerFault {
		if (!current.value.system) {
			if (!getUserDisplayName(previous.value).equals(getUserDisplayName(current.value))) {
				UserCalendarServiceFactory calendarServiceFactory = new UserCalendarServiceFactory();
				try {
					calendarServiceFactory.getService(SecurityContext.SYSTEM).updateDefault(domainUid, current);
				} catch (ServerFault e) {
					logger.error(e.getMessage(), e);
				}
			}
		}
	}

	@Override
	public void beforeDelete(BmContext context, String domainUid, String uid, User previous) throws ServerFault {
		ItemValue<User> user = ItemValue.create(uid, previous);
		if (!user.value.system) {
			deleteAllCalendars(user);
			deleteViewsContainer(user);
			deleteFreebusyContainer(user);
		}
	}

	private String getUserDisplayName(User user) {
		if (user.contactInfos != null && user.contactInfos.identification.formatedName.value != null) {
			return user.contactInfos.identification.formatedName.value;
		} else {
			return user.login;
		}
	}

	private void createDefaultCalendar(String domainUid, ItemValue<User> user) {
		UserCalendarServiceFactory calendarServiceFactory = new UserCalendarServiceFactory();
		try {
			calendarServiceFactory.getService(SecurityContext.SYSTEM).createDefault(domainUid, user);
		} catch (ServerFault e) {
			logger.error(e.getMessage(), e);
		}
	}

	private void deleteAllCalendars(ItemValue<User> user) {
		UserCalendarServiceFactory calendarServiceFactory = new UserCalendarServiceFactory();
		try {
			calendarServiceFactory.getService(SecurityContext.SYSTEM).deleteAll(user);
		} catch (ServerFault e) {
			logger.error(e.getMessage(), e);
		}
	}

	private void createViewsContainer(String domainUid, ItemValue<User> user) {
		ServerSideServiceProvider serviceProvider = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);

		String containerUid = getViewContainerUid(user.uid);

		ContainerDescriptor containerDescriptor = ContainerDescriptor.create(containerUid,
				user.value.contactInfos.identification.formatedName.value, user.uid, CALENDAR_VIEW, domainUid, true);

		try {
			serviceProvider.instance(IContainers.class).create(containerUid, containerDescriptor);

			serviceProvider.instance(IContainerManagement.class, containerUid)
					.setAccessControlList(Arrays.asList(AccessControlEntry.create(user.uid, Verb.All)));

			createDefaultView(user, containerUid);

		} catch (ServerFault e) {
			logger.error(e.getMessage(), e);
		}
	}

	private void createDefaultView(ItemValue<User> user, String containerUid) throws ServerFault {
		ServerSideServiceProvider serviceProvider = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);

		ICalendarView viewService = serviceProvider.instance(ICalendarView.class, containerUid);
		CalendarView view = new CalendarView();
		view.label = "$$calendarhome$$";
		view.type = CalendarViewType.WEEK;
		view.calendars = Arrays.asList(ICalendarUids.defaultUserCalendar(user.uid));
		logger.info("Create default calendar view for user {} calendar view uid {}", user.uid, containerUid);

		viewService.create("default", view);
		viewService.setDefault("default");
	}

	private void deleteViewsContainer(ItemValue<User> user) {
		ServerSideServiceProvider serviceProvider = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);

		String containerUid = getViewContainerUid(user.uid);

		try {
			final IInCoreCalendarView viewService = serviceProvider.instance(IInCoreCalendarView.class, containerUid);
			viewService.list().values.forEach(view -> {
				try {
					viewService.delete(view.uid, true);
				} catch (Exception e) {
					logger.error("Error in viewService.delete: ", e.getMessage(), e);
				}
			});
			serviceProvider.instance(IContainers.class).delete(containerUid);
		} catch (ServerFault e) {
			logger.error(e.getMessage(), e);
		}

	}

	/**
	 * @param uid
	 * @return
	 */
	private static String getViewContainerUid(String uid) {
		return ICalendarViewUids.userCalendarView(uid);
	}

	private void createFreebusyContainer(String domainUid, ItemValue<User> user) {
		ServerSideServiceProvider serviceProvider = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);
		String containerUid = IFreebusyUids.getFreebusyContainerUid(user.uid);
		ContainerDescriptor containerDescriptor = ContainerDescriptor.create(containerUid, "freebusy container",
				user.uid, IFreebusyUids.TYPE, domainUid, true);
		try {
			serviceProvider.instance(IContainers.class).create(containerUid, containerDescriptor);
			serviceProvider.instance(IContainerManagement.class, containerUid).setAccessControlList(Arrays.asList(
					AccessControlEntry.create(user.uid, Verb.All), AccessControlEntry.create(domainUid, Verb.Read)));
		} catch (ServerFault e) {
			logger.error(e.getMessage(), e);
		}

	}

	private void deleteFreebusyContainer(ItemValue<User> user) {
		ServerSideServiceProvider serviceProvider = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);

		try {
			IFreebusyMgmt fb = serviceProvider.instance(IFreebusyMgmt.class,
					IFreebusyUids.getFreebusyContainerUid(user.uid));
			fb.get().forEach(fb::remove);
			serviceProvider.instance(IContainers.class).delete(IFreebusyUids.getFreebusyContainerUid(user.uid));
		} catch (ServerFault e) {
			logger.error(e.getMessage(), e);
		}
	}

}
