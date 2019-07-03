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
package net.bluemind.addressbook.userbook;

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.addressbook.api.AddressBookDescriptor;
import net.bluemind.addressbook.api.IAddressBook;
import net.bluemind.addressbook.api.IAddressBooksMgmt;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.api.ContainerQuery;
import net.bluemind.core.container.api.ContainerSubscription;
import net.bluemind.core.container.api.IContainerManagement;
import net.bluemind.core.container.api.IContainers;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.model.acl.AccessControlEntry;
import net.bluemind.core.container.model.acl.Verb;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.user.api.IUserSubscription;
import net.bluemind.user.api.User;
import net.bluemind.user.hook.DefaultUserHook;

public class UserBookHook extends DefaultUserHook {

	private static final Logger logger = LoggerFactory.getLogger(UserBookHook.class);

	public enum UserBookType {
		Contacts, CollectedContacts
	}

	public UserBookHook() {
	}

	private final IServiceProvider sp() {
		return ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);
	}

	@Override
	public void onUserCreated(BmContext context, String domainUid, ItemValue<User> created) {
		if (!created.value.system) {
			createUserAddressBooks(domainUid, created);
		}
	}

	private void createUserAddressBooks(String domainUid, ItemValue<User> user) {
		logger.info("**** Create books for user {}", user.uid);
		// contacts
		String contactsBookId = getUserBookId(user.uid, UserBookType.Contacts);

		// collected_contacts
		String collectContactsBookId = getUserBookId(user.uid, UserBookType.CollectedContacts);

		AddressBookDescriptor contactsBookDescriptor = AddressBookDescriptor.create("$$mycontacts$$", user.uid,
				domainUid);

		AddressBookDescriptor ccollectContactsBookBookDescriptor = AddressBookDescriptor
				.create("$$collected_contacts$$", user.uid, domainUid);

		try {
			IAddressBooksMgmt abMgmt = sp().instance(IAddressBooksMgmt.class);
			abMgmt.create(contactsBookId, contactsBookDescriptor, true);
			abMgmt.create(collectContactsBookId, ccollectContactsBookBookDescriptor, false);

			IContainerManagement manager = sp().instance(IContainerManagement.class, contactsBookId);
			manager.setAccessControlList(Arrays.asList(AccessControlEntry.create(user.uid, Verb.Write)));

			manager = sp().instance(IContainerManagement.class, collectContactsBookId);
			manager.setAccessControlList(Arrays.asList(AccessControlEntry.create(user.uid, Verb.Write)));

			IUserSubscription userSubService = sp().instance(IUserSubscription.class, domainUid);
			userSubService.subscribe(user.uid,
					Arrays.asList(ContainerSubscription.create(contactsBookId, true),
							ContainerSubscription.create(collectContactsBookId, true),
							ContainerSubscription.create("addressbook_" + domainUid, false)));

		} catch (ServerFault e) {
			logger.error("error during addressbook creation ", e);
		}

	}

	@Override
	public void onUserUpdated(BmContext context, String domainUid, ItemValue<User> previous, ItemValue<User> current) {
		// Nothing to do on update

	}

	@Override
	public void beforeDelete(BmContext context, String domainUid, String uid, User previous) throws ServerFault {
		ItemValue<User> user = ItemValue.create(uid, previous);
		if (!previous.system) {
			String contactsBookId = getUserBookId(uid, UserBookType.Contacts);
			String collectContactsBookId = getUserBookId(uid, UserBookType.CollectedContacts);

			try {
				logger.info("Delete addressbook {} for user {}", contactsBookId, previous.login);
				deleteAddressbook(user, contactsBookId, context.su());

				logger.info("Delete addressbook {} for user {}", collectContactsBookId, previous.login);
				deleteAddressbook(user, collectContactsBookId, context.su());

				ContainerQuery query = new ContainerQuery();
				query.type = "addressbook";
				query.owner = uid;
				sp().instance(IContainers.class).all(query).forEach(todo -> deleteAddressbook(user, todo.uid, context));

			} catch (ServerFault e) {
				logger.error("error during addressbook deletion ", e);
			}
		}
	}

	private void deleteAddressbook(ItemValue<User> user, String container, BmContext context) {
		logger.info("Delete addressbook {} of user {}", container, user.value.login);
		IAddressBook ab = context.provider().instance(IAddressBook.class, container);
		ab.reset();
		sp().instance(IContainers.class).delete(container);
	}

	public static String getUserBookId(String uid, UserBookType type) {
		return "book:" + type + "_" + uid;
	}
}
