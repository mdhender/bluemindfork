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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.calendar.api.ICalendarUids;
import net.bluemind.calendar.api.ICalendarViewUids;
import net.bluemind.calendar.api.IFreebusyUids;
import net.bluemind.calendar.service.UserCalendarServiceFactory;
import net.bluemind.core.api.report.DiagnosticReport;
import net.bluemind.core.container.api.IContainerManagement;
import net.bluemind.core.container.api.IContainers;
import net.bluemind.core.container.model.ContainerDescriptor;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.model.acl.AccessControlEntry;
import net.bluemind.core.container.model.acl.Verb;
import net.bluemind.core.container.repair.ContainerRepairOp;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.directory.api.BaseDirEntry.Kind;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.user.api.IUser;
import net.bluemind.user.api.User;

public class UserCalendarRepair implements ContainerRepairOp {

	private static final Logger logger = LoggerFactory.getLogger(UserCalendarRepair.class);

	@Override
	public void check(String domainUid, DirEntry entry, DiagnosticReport report, IServerTaskMonitor monitor) {
		ItemValue<User> user = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IUser.class, domainUid).getComplete(entry.entryUid);

		verifyDefaultContainer(domainUid, user, report, monitor, () -> {
			monitor.log("Default calendar of user " + user.uid + " is missing");
			logger.info("Default calendar of user {} is missing", user.uid);
		});

		verifyCalendarViewContainer(domainUid, user, report, monitor, () -> {
			monitor.log("Calendar view of user " + user.uid + " is missing");
			logger.info("Calendar view of user {} is missing", user.uid);
		});

		verifyFreebusyViewContainer(domainUid, user, report, monitor, () -> {
			monitor.log("Freebusy container of user " + user.uid + " is missing");
			logger.info("Freebusy container of user {} is missing", user.uid);
		});

	}

	@Override
	public void repair(String domainUid, DirEntry entry, DiagnosticReport report, IServerTaskMonitor monitor) {
		ItemValue<User> user = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IUser.class, domainUid).getComplete(entry.entryUid);

		verifyDefaultContainer(domainUid, user, report, monitor, () -> {
			monitor.log("Repairing default calendar of user " + user.uid);
			logger.info("Repairing default calendar of user {}", user.uid);
			UserCalendarServiceFactory calendarServiceFactory = new UserCalendarServiceFactory();
			calendarServiceFactory.getService(SecurityContext.SYSTEM).createDefault(domainUid, user);
		});

		verifyCalendarViewContainer(domainUid, user, report, monitor, () -> {
			monitor.log("Repairing calendar view of user " + user.uid);
			logger.info("Repairing calendar view of user {}", user.uid);
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

		verifyFreebusyViewContainer(domainUid, user, report, monitor, () -> {
			monitor.log("Repairing freebusy container of user " + user.uid);
			logger.info("Repairing freebusy container of user {}", user.uid);
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

	}

	private void verifyDefaultContainer(String domainUid, ItemValue<User> user, DiagnosticReport report,
			IServerTaskMonitor monitor, Runnable maintenance) {

		String containerUid = ICalendarUids.defaultUserCalendar(user.uid);
		verifyContainer(domainUid, report, monitor, maintenance, containerUid);

	}

	private void verifyCalendarViewContainer(String domainUid, ItemValue<User> user, DiagnosticReport report,
			IServerTaskMonitor monitor, Runnable maintenance) {

		String containerUid = getViewContainerUid(user.uid);
		verifyContainer(domainUid, report, monitor, maintenance, containerUid);
	}

	private void verifyFreebusyViewContainer(String domainUid, ItemValue<User> user, DiagnosticReport report,
			IServerTaskMonitor monitor, Runnable maintenance) {

		String containerUid = IFreebusyUids.getFreebusyContainerUid(user.uid);
		verifyContainer(domainUid, report, monitor, maintenance, containerUid);
	}

	private String getViewContainerUid(String uid) {
		return ICalendarViewUids.userCalendarView(uid);
	}

	@Override
	public Kind supportedKind() {
		return Kind.USER;
	}

}
