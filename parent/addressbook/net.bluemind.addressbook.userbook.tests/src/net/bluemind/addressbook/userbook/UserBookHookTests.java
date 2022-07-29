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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Lists;

import net.bluemind.addressbook.api.AddressBookDescriptor;
import net.bluemind.addressbook.api.IAddressBook;
import net.bluemind.addressbook.api.IAddressBookUids;
import net.bluemind.addressbook.api.IAddressBooksMgmt;
import net.bluemind.addressbook.api.VCard;
import net.bluemind.addressbook.api.VCard.Identification.FormatedName;
import net.bluemind.addressbook.api.VCard.Kind;
import net.bluemind.addressbook.userbook.UserBookHook.UserBookType;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.api.ContainerSubscriptionDescriptor;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.persistence.ContainerStore;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.elasticsearch.ElasticsearchTestHelper;
import net.bluemind.core.jdbc.JdbcActivator;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.tests.BmTestContext;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.server.api.Server;
import net.bluemind.tests.defaultdata.PopulateHelper;
import net.bluemind.user.api.IUser;
import net.bluemind.user.api.IUserSubscription;
import net.bluemind.user.api.User;

public class UserBookHookTests {

	private ContainerStore containerStore;
	private ContainerStore systemContainerStore;
	private UserBookHook hook;
	private BmContext bmContext;
	private String domainUid = "bm.lan";
	private String userUid;

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();

		JdbcActivator.getInstance().setDataSource(JdbcTestHelper.getInstance().getDataSource());
		ElasticsearchTestHelper.getInstance().beforeTest();

		VertxPlatform.spawnBlocking(30, TimeUnit.SECONDS);

		Server esServer = new Server();
		esServer.ip = ElasticsearchTestHelper.getInstance().getHost();
		esServer.tags = Lists.newArrayList("bm/es");
		PopulateHelper.initGlobalVirt(esServer);
		PopulateHelper.createTestDomain(domainUid, esServer);
		userUid = PopulateHelper.addUser("test", "bm.lan");
		hook = new UserBookHook();
		bmContext = new BmTestContext(SecurityContext.SYSTEM);
		ItemValue<User> user = testUser();
		containerStore = new ContainerStore(bmContext,
				JdbcActivator.getInstance().getMailboxDataSource(user.value.dataLocation), SecurityContext.SYSTEM);
		systemContainerStore = new ContainerStore(null, JdbcActivator.getInstance().getDataSource(),
				SecurityContext.SYSTEM);
	}

	@Test
	public void testOnCreated() throws Exception {

		ItemValue<User> userItem = testUser();
		// hook called on user creation. Do not call it again..
		// hook.onUserCreated(bmContext, domainUid, userItem);

		String containerId = UserBookHook.getUserBookId(userItem.uid, UserBookType.Contacts);
		Container contacts = containerStore.get(containerId);
		assertNotNull(contacts);

		containerId = UserBookHook.getUserBookId(userItem.uid, UserBookType.CollectedContacts);
		Container collected = containerStore.get(containerId);
		assertNotNull(collected);

		Container directory = systemContainerStore.get("addressbook_" + domainUid);
		assertNotNull(directory);

		IUserSubscription userSubService = bmContext.getServiceProvider().instance(IUserSubscription.class, domainUid);
		List<ContainerSubscriptionDescriptor> subs = userSubService.listSubscriptions(userUid, IAddressBookUids.TYPE);

		verifyContainerSubAndSync(subs, contacts, true);
		verifyContainerSubAndSync(subs, collected, true);
		verifyContainerSubAndSync(subs, directory, false);
	}

	private void verifyContainerSubAndSync(List<ContainerSubscriptionDescriptor> subs, Container c,
			boolean exepectedSync) {
		boolean isPresent = false;
		boolean sync = false;
		for (ContainerSubscriptionDescriptor container : subs) {
			if (container.containerUid.equals(c.uid)) {
				isPresent = true;
				sync = container.offlineSync;
				break;
			}
		}
		Assert.assertTrue(isPresent);
		Assert.assertEquals(exepectedSync, sync);
	}

	@Test
	public void testOnDelete() throws Exception {
		ItemValue<User> userItem = testUser();

		IAddressBooksMgmt abs = bmContext.getServiceProvider().instance(IAddressBooksMgmt.class);
		AddressBookDescriptor descriptor = new AddressBookDescriptor();
		descriptor.domainUid = domainUid;
		descriptor.name = "ab1";
		descriptor.owner = userItem.uid;
		descriptor.system = false;
		abs.create("ab1", descriptor, false);
		Container container = containerStore.get("ab1");
		assertNotNull(container);

		IAddressBook ab1 = bmContext.getServiceProvider().instance(IAddressBook.class, "ab1");
		VCard vcard1 = new VCard();
		vcard1.kind = Kind.individual;
		vcard1.identification.formatedName = FormatedName.create("Test");
		ab1.create("vcard1", vcard1);

		AddressBookDescriptor descriptor2 = new AddressBookDescriptor();
		descriptor2.domainUid = domainUid;
		descriptor2.name = "ab1";
		descriptor2.owner = userItem.uid;
		descriptor2.system = false;
		abs.create("ab2", descriptor, false);

		IAddressBook ab2 = bmContext.getServiceProvider().instance(IAddressBook.class, "ab2");
		VCard vcard2 = new VCard();
		vcard2.kind = Kind.individual;
		vcard2.identification.formatedName = FormatedName.create("Test");
		ab2.create("vcard2", vcard1);

		container = containerStore.get("ab2");
		assertNotNull(container);

		hook.beforeDelete(bmContext, domainUid, userItem.uid, userItem.value);

		String contactContainerId = UserBookHook.getUserBookId(userItem.uid, UserBookType.Contacts);
		container = containerStore.get(contactContainerId);
		assertNull(container);

		String collectedContainerId = UserBookHook.getUserBookId(userItem.uid, UserBookType.CollectedContacts);
		container = containerStore.get(collectedContainerId);
		assertNull(container);
		container = containerStore.get("ab1");
		assertNull(container);
		container = containerStore.get("ab2");
		assertNull(container);

		container = containerStore.get("collectedContainerId");
		assertNull(container);

	}

	private ItemValue<User> testUser() throws ServerFault {
		IUser userService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IUser.class,
				domainUid);
		return userService.getComplete(userUid);
	}
}
