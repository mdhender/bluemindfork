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
package net.bluemind.calendar.service.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.calendar.api.ICalendar;
import net.bluemind.calendar.api.ICalendarUids;
import net.bluemind.calendar.api.ICalendarUids.UserCalendarType;
import net.bluemind.calendar.api.ICalendarViewUids;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.api.ContainerQuery;
import net.bluemind.core.container.api.ContainerSubscription;
import net.bluemind.core.container.api.IContainerManagement;
import net.bluemind.core.container.api.IContainers;
import net.bluemind.core.container.model.ContainerDescriptor;
import net.bluemind.core.container.model.ContainerModifiableDescriptor;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.model.acl.AccessControlEntry;
import net.bluemind.core.container.model.acl.Verb;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.core.task.service.TaskUtils;
import net.bluemind.directory.api.BaseDirEntry.AccountType;
import net.bluemind.user.api.IUserSubscription;
import net.bluemind.user.api.User;

/**
 * Create default user calendar
 *
 */
public class UserCalendarService {

	private static Logger logger = LoggerFactory.getLogger(UserCalendarService.class);

	public static final String TYPE = "user";

	private ServerSideServiceProvider serviceProvider;

	public UserCalendarService(SecurityContext securityContext) {
		serviceProvider = ServerSideServiceProvider.getProvider(securityContext);
	}

	/**
	 * @param user
	 * @throws ServerFault
	 */
	public void createDefault(String domainUid, ItemValue<User> user) throws ServerFault {

		String containerUid = getDefaultCalendarUid(user.uid);
		logger.info("Create default calendar for user {}, calendarUid {}", user.uid, containerUid);
		create(domainUid, user, containerUid, getUserDisplayName(user), true);
	}

	/**
	 * @param user
	 * @throws ServerFault
	 */
	public void updateDefault(String domainUid, ItemValue<User> user) throws ServerFault {

		String containerUid = getDefaultCalendarUid(user.uid);
		logger.info("Updating default calendar for user {}, calendarUid {}", user.uid, containerUid);
		update(domainUid, user, containerUid, getUserDisplayName(user), true);
	}

	/**
	 * @param u
	 */
	public void deleteDefault(ItemValue<User> user) throws ServerFault {
		logger.info("Delete default calendar for user {}", user.uid);
		delete(getDefaultCalendarUid(user.uid));
	}

	/**
	 * @param user
	 * @param name
	 * @return
	 * @throws ServerFault
	 */
	public String create(String domainUid, ItemValue<User> user, String name) throws ServerFault {

		String containerUid = getUserCreatedCalendarUid(user);
		logger.info("Create calendar '{}' , uid {} for user {}", name, containerUid, user.uid);

		create(domainUid, user, containerUid, name, false);

		return containerUid;
	}

	/**
	 * @param user
	 * @param name
	 * @return
	 * @throws ServerFault
	 */
	public String update(String domainUid, ItemValue<User> user, String name) throws ServerFault {

		String containerUid = getUserCreatedCalendarUid(user);
		logger.info("Updating calendar '{}' , uid {} for user {}", name, containerUid, user.uid);

		update(domainUid, user, containerUid, name, false);

		return containerUid;
	}

	public void deleteAll(ItemValue<User> user) {
		deleteDefault(user);
		IContainers cs = serviceProvider.instance(IContainers.class);
		ContainerQuery query = new ContainerQuery();
		query.type = "calendar";
		query.owner = user.uid;
		cs.all(query).forEach(cal -> delete(cal.uid));
	}

	/**
	 * @param uid
	 * @throws ServerFault
	 */
	public void delete(String uid) throws ServerFault {
		logger.info("Deleting calendar {}", uid);
		ICalendar cal = serviceProvider.instance(ICalendar.class, uid);
		TaskRef tr = cal.reset();
		TaskUtils.wait(serviceProvider, tr);
		serviceProvider.instance(IContainers.class).delete(uid);
	}

	/**
	 * @param user
	 * @return
	 * @throws ServerFault
	 */
	public ContainerDescriptor getDefaultCalendarContainer(ItemValue<User> user) throws ServerFault {
		return serviceProvider.instance(IContainerManagement.class, getDefaultCalendarUid(user.uid)).getDescriptor();
	}

	/**
	 * @param uri
	 * @return
	 * @throws ServerFault
	 */
	public ContainerDescriptor getCalendarContainerFromUri(String uri) throws ServerFault {
		return serviceProvider.instance(IContainerManagement.class, uri).getDescriptor();
	}

	/**
	 * @param domainUid
	 * @param user
	 * @param uid
	 * @param isDefault
	 * @throws ServerFault
	 */
	private void create(String domainUid, ItemValue<User> user, String uid, String name, boolean isDefault)
			throws ServerFault {

		String userSubject = user.uid;
		ContainerDescriptor calendarDescriptor = ContainerDescriptor.create(uid, name, userSubject, ICalendarUids.TYPE,
				domainUid, isDefault);

		IContainers containerService = serviceProvider.instance(IContainers.class);
		containerService.create(uid, calendarDescriptor);

		logger.info("Subscribe userSubject: {} to container {}", userSubject, uid);
		IUserSubscription userSubService = serviceProvider.instance(IUserSubscription.class, domainUid);
		userSubService.subscribe(user.uid, Arrays.asList(ContainerSubscription.create(uid, true)));

		logger.info("Set ACLs for userSubject: {} to container {}", userSubject, uid);
		IContainerManagement manager = serviceProvider.instance(IContainerManagement.class, uid);

		List<AccessControlEntry> acls = new ArrayList<AccessControlEntry>();
		acls.add(AccessControlEntry.create(userSubject, Verb.All));
		if (user.value.accountType == AccountType.FULL) {
			acls.add(AccessControlEntry.create(domainUid, Verb.Invitation));
		}

		manager.setAccessControlList(acls);
	}

	/**
	 * @param domainUid
	 * @param user
	 * @param uid
	 * @param isDefault
	 * @throws ServerFault
	 */
	private void update(String domainUid, ItemValue<User> user, String uid, String name, boolean isDefault)
			throws ServerFault {

		ContainerModifiableDescriptor calendarDescriptor = new ContainerModifiableDescriptor();
		calendarDescriptor.name = name;
		calendarDescriptor.defaultContainer = isDefault;

		IContainers containerService = serviceProvider.instance(IContainers.class);
		containerService.update(uid, calendarDescriptor);

	}

	public static String getDefaultCalendarUid(String uid) {
		return ICalendarUids.defaultUserCalendar(uid);
	}

	private static String getUserCreatedCalendarUid(ItemValue<User> user) {
		return ICalendarUids.TYPE + ":" + UserCalendarType.UserCreated + ":" + user.uid + ":" + UUID.randomUUID();
	}

	public static String getDefaultCalendarViewContainerUid(String uid) {
		return ICalendarViewUids.userCalendarView(uid);
	}

	private String getUserDisplayName(ItemValue<User> userItem) {
		User user = userItem.value;
		if (user.contactInfos != null && user.contactInfos.identification.formatedName.value != null) {
			return user.contactInfos.identification.formatedName.value;
		} else {
			return user.login;
		}
	}

}
