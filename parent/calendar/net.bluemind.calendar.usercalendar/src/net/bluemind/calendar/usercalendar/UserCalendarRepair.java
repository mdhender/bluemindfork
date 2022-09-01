/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2018
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
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import net.bluemind.calendar.api.ICalendarUids;
import net.bluemind.calendar.api.ICalendarViewUids;
import net.bluemind.calendar.api.IFreebusyMgmt;
import net.bluemind.calendar.api.IFreebusyUids;
import net.bluemind.calendar.service.UserCalendarServiceFactory;
import net.bluemind.core.container.api.IContainerManagement;
import net.bluemind.core.container.api.IContainers;
import net.bluemind.core.container.model.ContainerDescriptor;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.model.acl.AccessControlEntry;
import net.bluemind.core.container.model.acl.Verb;
import net.bluemind.core.container.repair.ContainerRepairOp;
import net.bluemind.core.container.repair.ContainerRepairUtil;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.directory.api.BaseDirEntry.Kind;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.service.RepairTaskMonitor;
import net.bluemind.user.api.IUser;
import net.bluemind.user.api.User;

public class UserCalendarRepair implements ContainerRepairOp {

	@Override
	public void check(BmContext context, String domainUid, DirEntry entry, RepairTaskMonitor monitor) {
		ItemValue<User> user = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IUser.class, domainUid).getComplete(entry.entryUid);

		verifyDefaultContainer(domainUid, user, monitor, () -> {
		});

		String containerUid = ICalendarUids.defaultUserCalendar(user.uid);
		ContainerRepairUtil.verifyContainerIsMarkedAsDefault(containerUid, monitor, () -> {
		});

		verifyCalendarViewContainer(domainUid, user, monitor, () -> {
		});

		verifyFreebusyContainer(domainUid, user, monitor, () -> {
		});

		verifyFreebusyCalendars(domainUid, user, monitor, (cals) -> {
		});

		ContainerRepairUtil.verifyContainerSubscription(entry.entryUid, domainUid, monitor, (container) -> {
		}, containerUid);

	}

	@Override
	public void repair(BmContext context, String domainUid, DirEntry entry, RepairTaskMonitor monitor) {
		ItemValue<User> user = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IUser.class, domainUid).getComplete(entry.entryUid);

		verifyDefaultContainer(domainUid, user, monitor, () -> {
			UserCalendarServiceFactory calendarServiceFactory = new UserCalendarServiceFactory();
			calendarServiceFactory.getService(SecurityContext.SYSTEM).createDefault(domainUid, user);
		});

		String defaultCalendarUid = ICalendarUids.defaultUserCalendar(user.uid);
		ContainerRepairUtil.verifyContainerIsMarkedAsDefault(defaultCalendarUid, monitor, () -> {
			ContainerRepairUtil.setAsDefault(defaultCalendarUid, context, monitor);
		});

		verifyCalendarViewContainer(domainUid, user, monitor, () -> {
			String containerUid = getViewContainerUid(user.uid);
			ContainerDescriptor descriptor = ContainerDescriptor.create(containerUid,
					user.value.contactInfos.identification.formatedName.value, user.uid, "calendarview", domainUid,
					true);

			IContainers service = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
					.instance(IContainers.class);
			service.create(containerUid, descriptor);

			ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
					.instance(IContainerManagement.class, containerUid)
					.setAccessControlList(Arrays.asList(AccessControlEntry.create(user.uid, Verb.All)));
		});

		verifyFreebusyContainer(domainUid, user, monitor, () -> {
			IContainers service = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
					.instance(IContainers.class);
			String containerUid = IFreebusyUids.getFreebusyContainerUid(user.uid);
			ContainerDescriptor containerDescriptor = ContainerDescriptor.create(containerUid, "freebusy container",
					user.uid, IFreebusyUids.TYPE, domainUid, true);

			service.create(containerUid, containerDescriptor);
			ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
					.instance(IContainerManagement.class, containerUid)
					.setAccessControlList(Arrays.asList(AccessControlEntry.create(user.uid, Verb.All),
							AccessControlEntry.create(domainUid, Verb.Read)));
		});

		verifyFreebusyCalendars(domainUid, user, monitor, (cals) -> {
			String containerUid = IFreebusyUids.getFreebusyContainerUid(user.uid);
			ServerSideServiceProvider provider = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);
			IFreebusyMgmt mgmt = provider.instance(IFreebusyMgmt.class, containerUid);
			mgmt.set(cals);
		});

		ContainerRepairUtil.verifyContainerSubscription(entry.entryUid, domainUid, monitor, (container) -> {
			ContainerRepairUtil.subscribe(entry.entryUid, domainUid, container);
		}, defaultCalendarUid);

	}

	private void verifyDefaultContainer(String domainUid, ItemValue<User> user, RepairTaskMonitor monitor,
			Runnable maintenance) {

		String containerUid = ICalendarUids.defaultUserCalendar(user.uid);
		verifyContainer(domainUid, monitor, maintenance, containerUid);

	}

	private void verifyCalendarViewContainer(String domainUid, ItemValue<User> user, RepairTaskMonitor monitor,
			Runnable maintenance) {

		String containerUid = getViewContainerUid(user.uid);
		verifyContainer(domainUid, monitor, maintenance, containerUid);
	}

	private void verifyFreebusyContainer(String domainUid, ItemValue<User> user, RepairTaskMonitor monitor,
			Runnable maintenance) {

		String containerUid = IFreebusyUids.getFreebusyContainerUid(user.uid);
		verifyContainer(domainUid, monitor, maintenance, containerUid);
	}

	private void verifyFreebusyCalendars(String domainUid, ItemValue<User> user, RepairTaskMonitor monitor,
			Consumer<List<String>> maintenance) {

		String containerUid = IFreebusyUids.getFreebusyContainerUid(user.uid);
		ServerSideServiceProvider provider = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);
		IContainers containerService = provider.instance(IContainers.class);
		ContainerDescriptor container = containerService.getIfPresent(containerUid);
		if (container != null) {
			IFreebusyMgmt mgmt = provider.instance(IFreebusyMgmt.class, containerUid);
			List<String> cals = mgmt.get();
			List<String> valid = cals.stream().filter(cal -> verifyCalendar(containerService, user.uid, cal))
					.collect(Collectors.toList());
			if (cals.size() != valid.size()) {
				monitor.notify("Freebusy contains foreign calendars");
				maintenance.accept(valid);
			}
		}
	}

	private boolean verifyCalendar(IContainers containerService, String owner, String calendar) {
		ContainerDescriptor calContainer = containerService.getIfPresent(calendar);
		return calContainer != null && calContainer.owner.equals(owner);
	}

	private String getViewContainerUid(String uid) {
		return ICalendarViewUids.userCalendarView(uid);
	}

	@Override
	public Kind supportedKind() {
		return Kind.USER;
	}

}
