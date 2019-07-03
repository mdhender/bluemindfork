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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;

import net.bluemind.addressbook.api.IAddressBook;
import net.bluemind.addressbook.api.IAddressBooksMgmt;
import net.bluemind.addressbook.api.IAddressBooksMgmt.ChangesetItem;
import net.bluemind.addressbook.api.VCard;
import net.bluemind.addressbook.api.VCardQuery;
import net.bluemind.addressbook.persistance.VCardIndexStore;
import net.bluemind.core.api.Stream;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.model.acl.AccessControlEntry;
import net.bluemind.core.container.model.acl.Verb;
import net.bluemind.core.container.persistance.AclStore;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.elasticsearch.ElasticsearchTestHelper;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.rest.base.GenericStream;
import net.bluemind.core.rest.vertx.VertxStream;
import net.bluemind.core.task.api.ITask;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.core.task.api.TaskStatus;
import net.bluemind.core.utils.JsonUtils;

public class MgntTests extends AbstractServiceTests {

	private Container container2;
	private Container container3;

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

	IAddressBooksMgmt getBooksMgmt() throws ServerFault {
		return ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IAddressBooksMgmt.class);
	}

	IAddressBook getService(Container container, SecurityContext context) throws ServerFault {
		return ServerSideServiceProvider.getProvider(context).instance(IAddressBook.class, container.uid);
	}

	@Test
	public void testReindex() throws ServerFault, InterruptedException {

		VCard card = defaultVCard();
		card.identification.formatedName.value = "albert";
		getService(container, defaultSecurityContext).create("GGtestUid", card);

		card.identification.formatedName.value = "bernard";
		getService(container, defaultSecurityContext).create("testUid2", card);

		card.identification.formatedName.value = "cecile";
		getService(container, defaultSecurityContext).create("testUid3", card);

		card.identification.formatedName.value = "alan";
		getService(container2, defaultSecurityContext).create("2testUid", card);

		refreshIndexes();
		assertEquals(1, getService(container, defaultSecurityContext).search(VCardQuery.create("GGtestUid")).total);

		new VCardIndexStore(ElasticsearchTestHelper.getInstance().getClient(), container).deleteAll();

		refreshIndexes();
		assertEquals(0, getService(container, defaultSecurityContext).search(VCardQuery.create("GGtestUid")).total);

		TaskRef taskRef = getBooksMgmt().reindex(container.uid);

		ITask task = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(ITask.class,
				"" + taskRef.id);
		TaskStatus status = null;
		while (true) {
			status = task.status();
			if (status.state != TaskStatus.State.InProgress && status.state != TaskStatus.State.NotStarted) {
				break;
			}
			Thread.sleep(100);
		}

		assertNotNull(status);
		assertEquals(TaskStatus.State.Success, status.state);
		refreshIndexes();
		assertEquals(1, getService(container, defaultSecurityContext).search(VCardQuery.create("GGtestUid")).total);

	}

	@Override
	protected IAddressBook getService(SecurityContext context) throws ServerFault {
		// TODO Auto-generated method stub
		return null;
	}

	protected void refreshIndexes() {
		ElasticsearchTestHelper.getInstance().getClient().admin().indices().prepareRefresh("contact").execute()
				.actionGet();
	}

	@Test
	public void testBackup() throws Exception {

		VCard card = defaultVCard();
		card.identification.formatedName.value = "albert";
		getService(container, defaultSecurityContext).create("testUid", card);

		card.identification.formatedName.value = "bernard";
		getService(container, defaultSecurityContext).create("testUid2", card);

		card.identification.formatedName.value = "cecile";
		getService(container, defaultSecurityContext).create("testUid3", card);

		Stream backupStream = getBooksMgmt().backup(container.uid, 0L);

		VertxStream.read(backupStream).dataHandler(new Handler<Buffer>() {

			@Override
			public void handle(Buffer arg0) {
				System.out.println("data " + arg0);
			}
		});
	}

	@Test
	public void testRestore() throws Exception {

		VCard card = defaultVCard();
		card.identification.formatedName.value = "albert";
		getService(container, defaultSecurityContext).create("testUidToDelete", card);

		List<IAddressBooksMgmt.ChangesetItem> items = new ArrayList<>();

		card = defaultVCard();
		card.identification.formatedName.value = "albert";
		items.add(item("testUid", card));

		card = defaultVCard();
		card.identification.formatedName.value = "bernard";
		items.add(item("testUid2", card));

		card = defaultVCard();
		card.identification.formatedName.value = "cecile";
		items.add(item("testUid3", card));

		final Iterator<ChangesetItem> itemsIt = items.iterator();
		GenericStream<ChangesetItem> gs = new GenericStream<IAddressBooksMgmt.ChangesetItem>() {

			@Override
			protected StreamState<ChangesetItem> next() throws Exception {
				if (itemsIt.hasNext()) {
					return StreamState.data(itemsIt.next());
				} else {
					return StreamState.end();
				}
			}

			@Override
			protected Buffer serialize(ChangesetItem n) throws Exception {
				return new Buffer(JsonUtils.asString(n));
			}

		};

		getBooksMgmt().restore(container.uid, VertxStream.stream(gs), true);

		assertNotNull(getService(container, defaultSecurityContext).getComplete("testUid"));

		assertNotNull(getService(container, defaultSecurityContext).getComplete("testUid2"));
		assertNotNull(getService(container, defaultSecurityContext).getComplete("testUid3"));

		// reset== true, "testUidToDelete" should not be present
		assertNull(getService(container, defaultSecurityContext).getComplete("testUidToDelete"));

	}

	private ChangesetItem item(String uid, VCard card) {
		ItemValue<VCard> item = new ItemValue<>();
		item.value = card;
		item.uid = uid;
		item.createdBy = "test";
		item.updatedBy = "test2";
		ChangesetItem ci = new ChangesetItem();
		ci.item = item;
		return ci;
	}
}
