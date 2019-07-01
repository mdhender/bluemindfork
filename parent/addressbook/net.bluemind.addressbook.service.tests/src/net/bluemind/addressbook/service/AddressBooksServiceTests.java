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
package net.bluemind.addressbook.service;

import static org.junit.Assert.assertEquals;

import java.sql.SQLException;
import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;

import net.bluemind.addressbook.api.IAddressBook;
import net.bluemind.addressbook.api.IAddressBooks;
import net.bluemind.addressbook.api.VCard;
import net.bluemind.addressbook.api.VCardInfo;
import net.bluemind.addressbook.api.VCardQuery;
import net.bluemind.core.api.ListResult;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.api.ContainerSubscription;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.ItemContainerValue;
import net.bluemind.core.container.model.acl.AccessControlEntry;
import net.bluemind.core.container.model.acl.Verb;
import net.bluemind.core.container.persistance.AclStore;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.user.api.IUserSubscription;

public class AddressBooksServiceTests extends AbstractServiceTests {

	private Container container2;
	private Container container3;

	protected IAddressBook getService(SecurityContext context) throws ServerFault {
		return ServerSideServiceProvider.getProvider(context).instance(IAddressBook.class, container.uid);
	}

	protected IAddressBook getService(Container container, SecurityContext context) throws ServerFault {
		return ServerSideServiceProvider.getProvider(context).instance(IAddressBook.class, container.uid);
	}

	private IAddressBooks getAddressBooks() throws ServerFault {
		return ServerSideServiceProvider.getProvider(defaultSecurityContext).instance(IAddressBooks.class,
				container.uid);
	}

	@Before
	public void before() throws Exception {
		super.before();

		container2 = createTestContainer(owner);
		container3 = createTestContainer(owner);
		AclStore aclStore = new AclStore(JdbcTestHelper.getInstance().getDataSource());
		aclStore.store(container2,
				Arrays.asList(AccessControlEntry.create(defaultSecurityContext.getSubject(), Verb.All)));

		aclStore.store(container3,
				Arrays.asList(AccessControlEntry.create(defaultSecurityContext.getSubject(), Verb.All)));

	}

	@Test
	public void testSearch() throws ServerFault, SQLException {

		VCard card = defaultVCard();
		card.organizational = new VCard.Organizational();
		card.identification.formatedName.value = "albert";
		getService(container, defaultSecurityContext).create("testUid", card);

		card.identification.formatedName.value = "bernard";
		getService(container, defaultSecurityContext).create("testUid2", card);

		card.identification.formatedName.value = "cecile";
		getService(container, defaultSecurityContext).create("testUid3", card);

		card.identification.formatedName.value = "alan";
		getService(container2, defaultSecurityContext).create("2testUid", card);

		card.identification.formatedName.value = "berty";
		getService(container2, defaultSecurityContext).create("2testUid2", card);

		card.identification.formatedName.value = "celine";
		getService(container2, defaultSecurityContext).create("2testUid3", card);

		card.identification.formatedName.value = "coco";
		getService(container3, defaultSecurityContext).create("3testUid", card);

		IUserSubscription subService = ServerSideServiceProvider.getProvider(defaultSecurityContext)
				.instance(IUserSubscription.class, container.domainUid);
		try {
			// double susbscribe is no more possible ( database constraint )
			// BM-10772 : Show only one result even if subscribed multiple time
			// to the same ADB.
			System.out.println("attempt sub to " + container);
			subService.subscribe(defaultSecurityContext.getSubject(),
					Arrays.asList(ContainerSubscription.create(container.uid, true)));
			System.err.println("sub to " + container.uid + " @ " + container.domainUid);
		} catch (Exception e) {
			System.err.println("failed sub to " + container.uid + ": " + e.getMessage());
		}

		subService.subscribe(defaultSecurityContext.getSubject(),
				Arrays.asList(ContainerSubscription.create(container2.uid, true)));

		refreshIndexes();

		IAddressBooks addressBooks = getAddressBooks();

		ListResult<ItemContainerValue<VCardInfo>> res = addressBooks.search(VCardQuery.create("*:*"));
		for (ItemContainerValue<VCardInfo> v : res.values) {
			System.out.println("Found " + v.displayName + " from " + v.containerUid);
		}

		assertEquals(6, res.total);
		assertEquals(6, res.values.size());
		// check sorted by FN
		assertEquals("alan", res.values.get(0).displayName);
		assertEquals(container2.uid, res.values.get(0).containerUid);

		assertEquals("albert", res.values.get(1).displayName);
		assertEquals(container.uid, res.values.get(1).containerUid);

		assertEquals("bernard", res.values.get(2).displayName);
		assertEquals(container.uid, res.values.get(2).containerUid);

		assertEquals("berty", res.values.get(3).displayName);
		assertEquals(container2.uid, res.values.get(3).containerUid);

		assertEquals("cecile", res.values.get(4).displayName);
		assertEquals(container.uid, res.values.get(4).containerUid);

		assertEquals("celine", res.values.get(5).displayName);
		assertEquals(container2.uid, res.values.get(5).containerUid);

		subService.subscribe(defaultSecurityContext.getSubject(),
				Arrays.asList(ContainerSubscription.create(container3.uid, true)));

		res = addressBooks.search(VCardQuery.create("*:*"));

		assertEquals(7, res.total);
		assertEquals(7, res.values.size());
	}
}
