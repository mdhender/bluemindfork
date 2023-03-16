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

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.junit.Test;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.query_dsl.TermQuery;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.json.JsonData;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import net.bluemind.addressbook.api.AddressBookBusAddresses;
import net.bluemind.addressbook.api.IAddressBook;
import net.bluemind.addressbook.api.VCard;
import net.bluemind.addressbook.api.VCard.Communications;
import net.bluemind.addressbook.api.VCard.Communications.Email;
import net.bluemind.addressbook.api.VCard.Communications.Impp;
import net.bluemind.addressbook.api.VCard.Identification.FormatedName;
import net.bluemind.addressbook.api.VCard.Kind;
import net.bluemind.addressbook.api.VCard.Organizational.Member;
import net.bluemind.addressbook.api.VCard.Parameter;
import net.bluemind.addressbook.api.VCardChanges;
import net.bluemind.addressbook.hook.internal.VCardMessage;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ChangeLogEntry.Type;
import net.bluemind.core.container.model.ContainerUpdatesResult;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.LocalJsonObject;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.tests.vertx.VertxEventChecker;
import net.bluemind.lib.elasticsearch.ESearchActivator;
import net.bluemind.tag.persistence.ItemTagRef;

public class AddressBookLogServiceTests extends AbstractServiceTests {

	protected IAddressBook getService(SecurityContext context) throws ServerFault {
		return ServerSideServiceProvider.getProvider(context).instance(IAddressBook.class, container.uid);
	}

	private List<Parameter> getEmptyAttributeTestList() {
		return Arrays.asList(new VCard.Parameter[] { Parameter.create("", "bad"), Parameter.create("bad", ""),
				Parameter.create("good-key", "good-value") });
	}

	@Override
	protected VCard defaultVCard() {
		VCard card = new VCard();

		card.identification = new VCard.Identification();

		card.communications.emails = Arrays.asList(Communications.Email.create("toto@bluemind.net", new ArrayList<>()),
				Communications.Email.create("titi@bluemind.net", new ArrayList<>()));

		Impp impp1 = Impp.create("jabber1", getEmptyAttributeTestList());
		Impp impp2 = Impp.create("jabber2", getEmptyAttributeTestList());
		card.communications.impps = Arrays.asList(impp1, impp2);

		card.identification.formatedName = VCard.Identification.FormatedName.create("default",
				Arrays.<VCard.Parameter>asList());

		card.explanatory.categories = Arrays.asList(tagRef1, tagRef2);

		card.related.assistant = "Sylvain Garcia";
		card.related.manager = "David Phan";

		VCard.Organizational organizational = VCard.Organizational.create("Loser", "Boss", //
				VCard.Organizational.Org.create("Blue-mind", "tlse", "Dev"), //
				Arrays.<VCard.Organizational.Member>asList());

		VCard.DeliveryAddressing.Address addr = VCard.DeliveryAddressing.Address.create("test", "postOfficeBox",
				"extentedAddress", "streetAddress", "locality", "region", "zip", "countryName",
				Arrays.<VCard.Parameter>asList(VCard.Parameter.create("TYPE", "home"),
						VCard.Parameter.create("TYPE", "work")));

		card.deliveryAddressing = Arrays.asList(VCard.DeliveryAddressing.create(addr));

		card.organizational = organizational;

		card.identification.anniversary = new Date();

		return card;
	}

	@Test
	public void testCreate() throws ServerFault, SQLException, ElasticsearchException, IOException {
		VCard card = defaultVCard();
		String uid = "test_" + System.nanoTime();

		VertxEventChecker<LocalJsonObject<VCardMessage>> createdMessageChecker = new VertxEventChecker<>(
				AddressBookBusAddresses.CREATED);

		VertxEventChecker<JsonObject> changedMessageChecker = new VertxEventChecker<>(
				AddressBookBusAddresses.getChangedEventAddress(container.uid));

		getService(defaultSecurityContext).create(uid, card);

		Item item = itemStore.get(uid);
		assertNotNull(item);
		VCard vcard = vCardStore.get(item);
		assertNotNull(vcard);

		assertEquals(false, vcard.identification.photo);

		assertEquals("David Phan", vcard.related.manager);
		assertEquals("Sylvain Garcia", vcard.related.assistant);

		assertEquals("Loser", vcard.organizational.title);
		assertEquals("Boss", vcard.organizational.role);
		assertEquals("Dev", vcard.organizational.org.department);

		List<ItemTagRef> tags = tagRefStore.get(item);
		assertNotNull(tags);
		assertEquals(2, tags.size());

		Message<LocalJsonObject<VCardMessage>> message = createdMessageChecker.shouldSuccess();
		assertNotNull(message);
		assertEquals(uid, message.body().getValue().itemUid);
		assertEquals(container.uid, message.body().getValue().container.uid);

		Message<JsonObject> containerMessage = changedMessageChecker.shouldSuccess();
		assertNotNull(containerMessage);

		ElasticsearchClient esClient = ESearchActivator.getClient();
		ESearchActivator.refreshIndex("audit_log");

		SearchResponse<JsonData> response = esClient.search(s -> s //
				.index("audit_log") //
				.query(q -> q.bool(b -> b
						.must(TermQuery.of(t -> t.field("container.uid").value(container.uid))._toQuery())
						.must(TermQuery.of(t -> t.field("logtype").value(VCard.class.getSimpleName()))._toQuery())
						.must(TermQuery.of(t -> t.field("action").value(Type.Created.toString()))._toQuery()))),
				JsonData.class);
		assertEquals(1L, response.hits().total().value());
	}

	@Test
	public void testUpdate() throws ServerFault, ElasticsearchException, IOException {
		String uid = "test_" + System.nanoTime();
		VCard card = defaultVCard();
		getService(defaultSecurityContext).create(uid, card);
		card.identification.name = VCard.Identification.Name.create("update", null, null, null, null,
				Collections.<VCard.Parameter>emptyList());

		card.explanatory.categories = Arrays.asList(tagRef1);

		getService(defaultSecurityContext).update(uid, card);
		ElasticsearchClient esClient = ESearchActivator.getClient();

		ESearchActivator.refreshIndex("audit_log");
		SearchResponse<JsonData> response = esClient.search(s -> s //
				.index("audit_log") //
				.query(q -> q.bool(b -> b
						.must(TermQuery.of(t -> t.field("container.uid").value(container.uid))._toQuery())
						.must(TermQuery.of(t -> t.field("logtype").value(VCard.class.getSimpleName()))._toQuery())
						.must(TermQuery.of(t -> t.field("action").value(Type.Created.toString()))._toQuery()))),
				JsonData.class);
		assertEquals(1L, response.hits().total().value());

		response = esClient.search(s -> s //
				.index("audit_log") //
				.query(q -> q.bool(b -> b
						.must(TermQuery.of(t -> t.field("container.uid").value(container.uid))._toQuery())
						.must(TermQuery.of(t -> t.field("logtype").value(VCard.class.getSimpleName()))._toQuery())
						.must(TermQuery.of(t -> t.field("action").value(Type.Updated.toString()))._toQuery()))),
				JsonData.class);
		assertEquals(1L, response.hits().total().value());
	}

	@Test
	public void testDelete() throws SQLException, ServerFault, ElasticsearchException, IOException {
		String uid = "test_" + System.nanoTime();
		VCard card = defaultVCard();
		getService(defaultSecurityContext).create(uid, card);

		VertxEventChecker<LocalJsonObject<VCardMessage>> createdMessageChecker = new VertxEventChecker<>(
				AddressBookBusAddresses.UPDATED);

		VertxEventChecker<JsonObject> changedMessageChecker = new VertxEventChecker<>(
				AddressBookBusAddresses.getChangedEventAddress(container.uid));

		createdMessageChecker.shouldFail();
		changedMessageChecker.shouldFail();

		createdMessageChecker = new VertxEventChecker<>(AddressBookBusAddresses.DELETED);

		changedMessageChecker = new VertxEventChecker<>(AddressBookBusAddresses.getChangedEventAddress(container.uid));

		getService(defaultSecurityContext).delete(uid);

		Message<LocalJsonObject<VCardMessage>> message = createdMessageChecker.shouldSuccess();
		assertNotNull(message);
		assertEquals(uid, message.body().getValue().itemUid);
		assertEquals(container.uid, message.body().getValue().container.uid);

		Message<JsonObject> cmessage = changedMessageChecker.shouldSuccess();
		assertNotNull(cmessage);

		assertNull(itemStore.get(uid));
		ElasticsearchClient esClient = ESearchActivator.getClient();

		ESearchActivator.refreshIndex("audit_log");

		SearchResponse<JsonData> response = esClient.search(s -> s //
				.index("audit_log") //
				.query(q -> q.bool(b -> b
						.must(TermQuery.of(t -> t.field("container.uid").value(container.uid))._toQuery())
						.must(TermQuery.of(t -> t.field("logtype").value(VCard.class.getSimpleName()))._toQuery())
						.must(TermQuery.of(t -> t.field("action").value(Type.Created.toString()))._toQuery()))),
				JsonData.class);
		assertEquals(1L, response.hits().total().value());

		response = esClient.search(s -> s //
				.index("audit_log") //
				.query(q -> q.bool(b -> b
						.must(TermQuery.of(t -> t.field("container.uid").value(container.uid))._toQuery())
						.must(TermQuery.of(t -> t.field("logtype").value(VCard.class.getSimpleName()))._toQuery())
						.must(TermQuery.of(t -> t.field("action").value(Type.Updated.toString()))._toQuery()))),
				JsonData.class);
		assertEquals(0L, response.hits().total().value());

		response = esClient.search(s -> s //
				.index("audit_log") //
				.query(q -> q.bool(b -> b
						.must(TermQuery.of(t -> t.field("container.uid").value(container.uid))._toQuery())
						.must(TermQuery.of(t -> t.field("logtype").value(VCard.class.getSimpleName()))._toQuery())
						.must(TermQuery.of(t -> t.field("action").value(Type.Deleted.toString()))._toQuery()))),
				JsonData.class);
		assertEquals(1L, response.hits().total().value());
	}

	@Test
	public void testMUpdates() throws ServerFault, SQLException, ElasticsearchException, IOException {
		VCard card = defaultVCard();
		String uid1 = create(card);
		String uid2 = create(defaultVCard());

		String uid3 = "testcreate_" + System.nanoTime();

		card.kind = Kind.group;
		card.identification.formatedName = FormatedName.create("test25");

		String uid4 = "testcreate_" + System.nanoTime();
		VCard group = defaultVCard();
		group.kind = Kind.group;
		String uid5 = "testcreate_" + System.nanoTime();
		group.organizational.member = Arrays.asList(Member.create(container.uid, uid5, "fakeName", "fake@email.la"));
		VCard member = defaultVCard();
		member.communications.emails = Arrays.asList(Email.create("fake@email.la"));
		VCardChanges changes = VCardChanges.create(
				// add
				Arrays.asList(VCardChanges.ItemAdd.create(uid3, defaultVCard()),
						// Create group before member
						VCardChanges.ItemAdd.create(uid4, group), VCardChanges.ItemAdd.create(uid5, member)

				),
				// modify
				Arrays.asList(VCardChanges.ItemModify.create(uid1, card)),
				// delete
				Arrays.asList(VCardChanges.ItemDelete.create(uid2)));

		ContainerUpdatesResult ret = getService(defaultSecurityContext).updates(changes);
		assertEquals(3, ret.added.size());
		assertEquals(1, ret.updated.size());
		assertEquals(1, ret.removed.size());

		List<Item> items = itemStore.getMultiple(Arrays.asList(uid1, uid2));
		assertEquals(1, items.size());
		assertEquals(uid1, items.get(0).uid);

		// vcard uid1
		VCard res = vCardStore.get(items.get(0));
		assertNotNull(res);
		assertEquals("test25", res.identification.formatedName.value);

		Item item3 = itemStore.get(uid3);
		assertNotNull(item3);
		VCard vcard = vCardStore.get(item3);
		assertNotNull(vcard);

		Item item4 = itemStore.get(uid4);
		assertNotNull(item4);
		VCard vgroup = vCardStore.get(item4);
		assertEquals(1, vgroup.organizational.member.size());
		assertEquals(container.uid, vgroup.organizational.member.get(0).containerUid);
		ElasticsearchClient esClient = ESearchActivator.getClient();
		ESearchActivator.refreshIndex("audit_log");

		SearchResponse<JsonData> response = esClient.search(s -> s //
				.index("audit_log") //
				.query(q -> q.bool(b -> b
						.must(TermQuery.of(t -> t.field("container.uid").value(container.uid))._toQuery())
						.must(TermQuery.of(t -> t.field("logtype").value(VCard.class.getSimpleName()))._toQuery())
						.must(TermQuery.of(t -> t.field("action").value(Type.Created.toString()))._toQuery()))),
				JsonData.class);
		assertEquals(3L, response.hits().total().value());

		response = esClient.search(s -> s //
				.index("audit_log") //
				.query(q -> q.bool(b -> b
						.must(TermQuery.of(t -> t.field("container.uid").value(container.uid))._toQuery())
						.must(TermQuery.of(t -> t.field("logtype").value(VCard.class.getSimpleName()))._toQuery())
						.must(TermQuery.of(t -> t.field("action").value(Type.Updated.toString()))._toQuery()))),
				JsonData.class);
		assertEquals(1L, response.hits().total().value());

	}

}
