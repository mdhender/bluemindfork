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
package net.bluemind.addressbook.userbook;

import java.util.Arrays;

import net.bluemind.addressbook.api.AddressBookDescriptor;
import net.bluemind.addressbook.api.IAddressBookUids;
import net.bluemind.addressbook.api.IAddressBooksMgmt;
import net.bluemind.core.container.api.ContainerSubscription;
import net.bluemind.core.container.api.IContainerManagement;
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
import net.bluemind.user.api.IUserSubscription;
import net.bluemind.user.api.User;

public class UserAddressbookRepair implements ContainerRepairOp {

	@Override
	public void check(BmContext context, String domainUid, DirEntry entry, RepairTaskMonitor monitor) {
		ItemValue<User> user = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IUser.class, domainUid).getComplete(entry.entryUid);

		verifyDefaultContainer(domainUid, user, monitor, () -> {

		});

		ContainerRepairUtil.verifyContainerIsMarkedAsDefault(getDefaultContainerUid(user.uid), monitor, () -> {
		});

		verifyCollectedContactsContainer(domainUid, user, monitor, () -> {

		});

		ContainerRepairUtil.verifyContainerSubscription(entry.entryUid, domainUid, monitor, (container) -> {
		}, getDefaultContainerUid(entry.entryUid), getCollectedContactsContainerUid(entry.entryUid));

	}

	@Override
	public void repair(BmContext context, String domainUid, DirEntry entry, RepairTaskMonitor monitor) {
		ItemValue<User> user = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IUser.class, domainUid).getComplete(entry.entryUid);

		verifyDefaultContainer(domainUid, user, monitor, () -> {

			createAddressbook(domainUid, entry, user, "$$mycontacts$$", getDefaultContainerUid(entry.entryUid));

		});

		String defaultContainerUid = getDefaultContainerUid(user.uid);
		ContainerRepairUtil.verifyContainerIsMarkedAsDefault(defaultContainerUid, monitor, () -> {
			ContainerRepairUtil.setAsDefault(defaultContainerUid, context, monitor);
		});

		verifyCollectedContactsContainer(domainUid, user, monitor, () -> {

			createAddressbook(domainUid, entry, user, "$$collected_contacts$$",
					getCollectedContactsContainerUid(entry.entryUid));

		});

		ContainerRepairUtil.verifyContainerSubscription(entry.entryUid, domainUid, monitor, (container) -> {
			ContainerRepairUtil.subscribe(entry.entryUid, domainUid, container);
		}, defaultContainerUid, getCollectedContactsContainerUid(entry.entryUid));

	}

	private void createAddressbook(String domainUid, DirEntry entry, ItemValue<User> user, String name,
			String container) {

		AddressBookDescriptor contactsBookDescriptor = AddressBookDescriptor.create(name, user.uid, domainUid);

		IAddressBooksMgmt abMgmt = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IAddressBooksMgmt.class);
		abMgmt.create(container, contactsBookDescriptor, true);

		IContainerManagement manager = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IContainerManagement.class, container);
		manager.setAccessControlList(Arrays.asList(AccessControlEntry.create(user.uid, Verb.Write)));

		IUserSubscription userSubService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IUserSubscription.class, domainUid);
		userSubService.subscribe(user.uid, Arrays.asList(ContainerSubscription.create(container, true)));
	}

	private void verifyDefaultContainer(String domainUid, ItemValue<User> user, RepairTaskMonitor monitor,
			Runnable maintenance) {

		String containerUid = getDefaultContainerUid(user.uid);
		verifyContainer(domainUid, monitor, maintenance, containerUid);
	}

	private void verifyCollectedContactsContainer(String domainUid, ItemValue<User> user, RepairTaskMonitor monitor,
			Runnable maintenance) {

		String containerUid = getCollectedContactsContainerUid(user.uid);
		verifyContainer(domainUid, monitor, maintenance, containerUid);
	}

	private String getDefaultContainerUid(String userUid) {
		return IAddressBookUids.defaultUserAddressbook(userUid);
	}

	private String getCollectedContactsContainerUid(String userUid) {
		return IAddressBookUids.collectedContactsUserAddressbook(userUid);
	}

	@Override
	public Kind supportedKind() {
		return Kind.USER;
	}

}
