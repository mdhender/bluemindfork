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
package net.bluemind.addressbook.persistance;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.UUID;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.QueryBuilders;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import net.bluemind.addressbook.api.VCard;
import net.bluemind.addressbook.api.VCard.Communications.Email;
import net.bluemind.addressbook.api.VCardQuery;
import net.bluemind.core.api.ListResult;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.persistance.ContainerStore;
import net.bluemind.core.container.persistance.ItemStore;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.elasticsearch.ElasticsearchTestHelper;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.tag.api.TagRef;

public class VCardIndexStoreTests {

	private Client client;
	private Container container;
	private ItemStore itemStore;
	private Container container2;

	@Before
	public void setup() throws Exception {
		ElasticsearchTestHelper.getInstance().beforeTest();
		JdbcTestHelper.getInstance().beforeTest();

		SecurityContext securityContext = SecurityContext.ANONYMOUS;

		ContainerStore containerHome = new ContainerStore(JdbcTestHelper.getInstance().getDataSource(),
				securityContext);
		String containerId = "test" + System.nanoTime();
		container = Container.create(containerId, "test", "test", "me", true);
		container = containerHome.create(container);

		String containerId2 = "test2" + System.nanoTime();
		container2 = Container.create(containerId2, "test2", "test2", "me", true);
		container2 = containerHome.create(container2);

		assertNotNull(container);

		itemStore = new ItemStore(JdbcTestHelper.getInstance().getDataSource(), container, securityContext);

		client = ElasticsearchTestHelper.getInstance().getClient();
		try {
			client.admin().indices().prepareCreate("contact").execute().actionGet();
		} catch (Exception e) {
		}

	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	@Test
	public void testCreate() throws InterruptedException, SQLException {
		VCardIndexStore indexStore = new VCardIndexStore(client, container);

		VCard card = new VCard();

		card.identification = new VCard.Identification();

		card.identification.formatedName = VCard.Identification.FormatedName.create("default",
				Arrays.<VCard.Parameter>asList());
		String uid = "test" + System.nanoTime();
		Item item = Item.create(uid, UUID.randomUUID().toString());
		itemStore.create(item);

		indexStore.create(item.uid, card);

		client.admin().indices().prepareRefresh("contact").execute().actionGet();
		SearchResponse resp = client.prepareSearch("contact").setTypes(VCardIndexStore.VCARD_TYPE)
				.setQuery(QueryBuilders.termQuery("uid", item.uid)).execute().actionGet();

		assertEquals(1, resp.getHits().getTotalHits());
	}

	@Test
	public void testDelete() throws InterruptedException, SQLException {
		VCardIndexStore indexStore = new VCardIndexStore(client, container);

		VCard card = new VCard();

		card.identification = new VCard.Identification();

		card.identification.formatedName = VCard.Identification.FormatedName.create("default",
				Arrays.<VCard.Parameter>asList());
		String uid = "test" + System.nanoTime();
		Item item = Item.create(uid, UUID.randomUUID().toString());
		itemStore.create(item);

		indexStore.create(item.uid, card);
		indexStore.refresh();
		indexStore.delete(item.uid);

		indexStore.refresh();
		SearchResponse resp = client.prepareSearch("contact").setTypes(VCardIndexStore.VCARD_TYPE)
				.setQuery(QueryBuilders.termQuery("uid", item.uid)).execute().actionGet();

		assertEquals(0, resp.getHits().getTotalHits());
	}

	@Test
	public void testDeleteAll() throws SQLException {
		VCardIndexStore indexStore = new VCardIndexStore(client, container);

		VCard card = new VCard();

		card.identification = new VCard.Identification();

		card.identification.formatedName = VCard.Identification.FormatedName.create("default",
				Arrays.<VCard.Parameter>asList());
		String uid = "test" + System.nanoTime();
		Item item = Item.create(uid, UUID.randomUUID().toString());
		itemStore.create(item);

		indexStore.create(item.uid, card);
		indexStore.refresh();
		indexStore.deleteAll();
		indexStore.refresh();

		SearchResponse resp = client.prepareSearch("contact").setTypes(VCardIndexStore.VCARD_TYPE)
				.setQuery(QueryBuilders.termQuery("uid", item.uid)).execute().actionGet();

		assertEquals(0, resp.getHits().getTotalHits());
	}

	@Test
	public void testSearch() {
		VCardIndexStore indexStore = new VCardIndexStore(client, container);
		VCardIndexStore indexStore2 = new VCardIndexStore(client, container2);

		VCard card = new VCard();

		card.identification = new VCard.Identification();

		card.identification.formatedName = VCard.Identification.FormatedName.create("test1",
				Arrays.<VCard.Parameter>asList());
		String uid = "test" + System.nanoTime();
		Item item = Item.create(uid, UUID.randomUUID().toString());
		indexStore.create(item.uid, card);

		card = new VCard();

		card.identification = new VCard.Identification();

		card.identification.formatedName = VCard.Identification.FormatedName.create("testABC",
				Arrays.<VCard.Parameter>asList());
		card.identification.name = VCard.Identification.Name.create("toto", "firstname", null, null, null,
				Arrays.<VCard.Parameter>asList());

		uid = "test" + System.nanoTime();
		item = Item.create(uid, UUID.randomUUID().toString());

		indexStore.create(item.uid, card);

		indexStore2.create("test2" + System.nanoTime(), card);

		refreshIndexes();
		// check that filter on container is ok
		ListResult<String> res = indexStore
				.search(VCardQuery.create("value.identification.formatedName.value:testABC"));

		assertEquals(1, res.total);

		// test search on another field
		res = indexStore.search(VCardQuery.create("value.identification.name.givenNames:firstname"));
		assertEquals(1, res.total);

		// test free search
		res = indexStore.search(VCardQuery.create("testABC"));
		assertEquals(1, res.total);

		// test empty result
		res = indexStore.search(VCardQuery.create("value.identification.formatedName.value:fakeName"));

		assertEquals(0, res.total);

	}

	@Test
	public void testSearchByCategory() {
		VCardIndexStore indexStore = new VCardIndexStore(client, container);
		VCard card1 = new VCard();
		TagRef tag1 = new TagRef();
		tag1.label = "tag1";
		String uid1 = "test" + System.nanoTime();
		card1.explanatory.categories = Arrays.asList(tag1);
		indexStore.create(uid1, card1);

		VCard card2 = new VCard();
		TagRef tag2 = new TagRef();
		tag2.label = "tag2";
		String uid2 = "test" + System.nanoTime();
		card1.explanatory.categories = Arrays.asList(tag2);
		indexStore.create(uid2, card2);

		refreshIndexes();

		ListResult<String> res = indexStore.search(VCardQuery.create("value.explanatory.categories.label:tag1"));
		assertEquals(1, res.total);

		res = indexStore.search(VCardQuery.create("value.explanatory.categories.label:tag1"));
		assertEquals(1, res.total);

		res = indexStore.search(VCardQuery.create("value.explanatory.categories.label:tagX"));
		assertEquals(0, res.total);
	}

	@Test
	public void testSearchByEmail() {
		VCardIndexStore indexStore = new VCardIndexStore(client, container);
		VCard card = new VCard();

		String email = "email" + System.currentTimeMillis() + "@domain.lan";
		card.communications.emails = Arrays.asList(Email.create(email, Arrays.<VCard.Parameter>asList()));

		String uid = "test" + System.nanoTime();
		Item item = Item.create(uid, UUID.randomUUID().toString());

		indexStore.create(item.uid, card);
		refreshIndexes();

		ListResult<String> res = indexStore.search(VCardQuery.create("value.communications.emails.value:" + email));
		assertEquals(1, res.total);

		res = indexStore.search(VCardQuery.create("value.communications.emails.value:\"unknownemail@domain.lan\""));
		assertEquals(0, res.total);
	}

	@Test
	public void testSearchByLongEmail() {
		VCardIndexStore indexStore = new VCardIndexStore(client, container);
		VCard card = new VCard();

		String email = "pref-publique-cartesgrises@haute-garonne.gouv.fr";
		card.communications.emails = Arrays.asList(Email.create(email, Arrays.<VCard.Parameter>asList()));

		String uid = "test" + System.nanoTime();
		Item item = Item.create(uid, UUID.randomUUID().toString());

		indexStore.create(item.uid, card);
		refreshIndexes();

		ListResult<String> res = indexStore.search(VCardQuery.create("value.communications.emails.value:" + email));
		assertEquals(1, res.total);
	}

	@Test
	public void testSearchMatchAll() {
		VCardIndexStore indexStore = new VCardIndexStore(client, container);
		indexStore.deleteAll();
		VCard card = new VCard();
		card.identification.formatedName.value = "john";

		String uid = "test" + System.nanoTime();
		Item item = Item.create(uid, UUID.randomUUID().toString());

		indexStore.create(item.uid, card);
		refreshIndexes();

		ListResult<String> res = indexStore.search(VCardQuery.create(null));
		assertEquals(1, res.total);
	}

	@Test
	public void testSearchSort() {
		VCardIndexStore indexStore = new VCardIndexStore(client, container);
		indexStore.deleteAll();

		VCard card = new VCard();
		card.identification.formatedName.value = "john";
		String uid1 = "test" + System.nanoTime();
		indexStore.create(uid1, card);

		card = new VCard();
		card.identification.formatedName.value = "albator";
		String uid2 = "test" + System.nanoTime();
		indexStore.create(uid2, card);

		card = new VCard();
		card.identification.formatedName.value = "zorro";
		String uid3 = "test" + System.nanoTime();
		indexStore.create(uid3, card);

		refreshIndexes();

		ListResult<String> res = indexStore.search(VCardQuery.create(null));
		assertEquals(3, res.total);
		assertEquals(uid2, res.values.get(0));
		assertEquals(uid1, res.values.get(1));
		assertEquals(uid3, res.values.get(2));
	}

	@Test
	public void testSearchFormatedName() {
		VCardIndexStore indexStore = new VCardIndexStore(client, container);
		indexStore.deleteAll();

		VCard card = new VCard();
		card.identification = new VCard.Identification();
		card.identification.formatedName = VCard.Identification.FormatedName.create("Thomas",
				Arrays.<VCard.Parameter>asList());
		String uid = "test" + System.nanoTime();
		Item item = Item.create(uid, UUID.randomUUID().toString());
		indexStore.create(item.uid, card);

		item = Item.create(uid, UUID.randomUUID().toString());
		indexStore.create(item.uid, card);

		refreshIndexes();
		ListResult<String> res = indexStore.search(VCardQuery.create("value.identification.formatedName.value:tho"));
		assertEquals(1, res.total);
	}

	private void refreshIndexes() {
		ElasticsearchTestHelper.getInstance().getClient().admin().indices().prepareRefresh("contact").execute()
				.actionGet();
	}
}
