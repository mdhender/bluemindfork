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
package net.bluemind.addressbook.persistence;

import static net.bluemind.addressbook.persistence.VCardIndexStore.VCARD_READ_ALIAS;
import static net.bluemind.addressbook.persistence.VCardIndexStore.VCARD_WRITE_ALIAS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.sql.Date;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.query_dsl.TermQuery;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.json.JsonData;
import net.bluemind.addressbook.api.VCard;
import net.bluemind.addressbook.api.VCard.Communications.Email;
import net.bluemind.addressbook.api.VCardQuery;
import net.bluemind.core.api.ListResult;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.persistence.ContainerStore;
import net.bluemind.core.container.persistence.ItemStore;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.elasticsearch.ElasticsearchTestHelper;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.tag.api.TagRef;

public class VCardIndexStoreTests {

	private ElasticsearchClient client;
	private Container container;
	private ItemStore itemStore;
	private Container container2;
	private VCardIndexStore indexStore;

	@Before
	public void setup() throws Exception {
		ElasticsearchTestHelper.getInstance().beforeTest();
		JdbcTestHelper.getInstance().beforeTest();

		SecurityContext securityContext = SecurityContext.ANONYMOUS;

		ContainerStore containerHome = new ContainerStore(null, JdbcTestHelper.getInstance().getDataSource(),
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
		indexStore = new VCardIndexStore(client, container, null);

	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	@Test
	public void testCreate() throws InterruptedException, SQLException, ElasticsearchException, IOException {
		VCard card = new VCard();

		card.identification = new VCard.Identification();

		card.identification.formatedName = VCard.Identification.FormatedName.create("default",
				Arrays.<VCard.Parameter>asList());
		String uid = "test" + System.nanoTime();
		Item item = Item.create(uid, UUID.randomUUID().toString());
		itemStore.create(item);

		indexStore.create(item, card);

		refreshIndexes();

		SearchResponse<JsonData> response = client.search(s -> s //
				.index(VCARD_READ_ALIAS) //
				.query(TermQuery.of(t -> t.field("uid").value(item.uid))._toQuery()), JsonData.class);

		assertEquals(1, response.hits().total().value());
	}

	@Test
	public void testDelete() throws InterruptedException, SQLException, ElasticsearchException, IOException {
		VCard card = new VCard();

		card.identification = new VCard.Identification();

		card.identification.formatedName = VCard.Identification.FormatedName.create("default",
				Arrays.<VCard.Parameter>asList());
		String uid = "test" + System.nanoTime();
		Item item = Item.create(uid, UUID.randomUUID().toString());
		itemStore.create(item);

		indexStore.create(item, card);
		indexStore.refresh();
		indexStore.delete(item.uid);

		indexStore.refresh();

		SearchResponse<JsonData> response = client.search(s -> s //
				.index(VCARD_READ_ALIAS) //
				.query(TermQuery.of(t -> t.field("uid").value(item.uid))._toQuery()), JsonData.class);

		assertEquals(0, response.hits().total().value());
	}

	@Test
	public void testDeleteAll() throws SQLException, ElasticsearchException, IOException {
		VCard card = new VCard();

		card.identification = new VCard.Identification();

		card.identification.formatedName = VCard.Identification.FormatedName.create("default",
				Arrays.<VCard.Parameter>asList());
		String uid = "test" + System.nanoTime();
		Item item = Item.create(uid, UUID.randomUUID().toString());
		itemStore.create(item);

		indexStore.create(item, card);
		indexStore.refresh();
		indexStore.deleteAll();
		indexStore.refresh();

		SearchResponse<JsonData> response = client.search(s -> s //
				.index(VCARD_READ_ALIAS) //
				.query(TermQuery.of(t -> t.field("uid").value(item.uid))._toQuery()), JsonData.class);

		assertEquals(0, response.hits().total().value());
	}

	@Test
	public void testSearch() throws ElasticsearchException, IOException {
		VCardIndexStore indexStore2 = new VCardIndexStore(client, container2, null);

		VCard card = new VCard();
		card.identification = new VCard.Identification();
		card.identification.formatedName = VCard.Identification.FormatedName.create("test1",
				Arrays.<VCard.Parameter>asList());
		card.identification.anniversary = Date.valueOf(LocalDate.of(2023, 12, 31)); // ts:1703977200
		String uid = "test" + System.nanoTime();
		Item item = Item.create(uid, System.nanoTime());
		indexStore.create(item, card);

		card = new VCard();
		card.identification = new VCard.Identification();
		card.identification.formatedName = VCard.Identification.FormatedName.create("testABC",
				Arrays.<VCard.Parameter>asList());
		card.identification.name = VCard.Identification.Name.create("toto", "firstname", null, null, null,
				Arrays.<VCard.Parameter>asList());
		card.identification.anniversary = Date.valueOf(LocalDate.of(2022, 12, 31)); // ts:1672441200
		uid = "test" + System.nanoTime();
		item = Item.create(uid, System.nanoTime());
		indexStore.create(item, card);

		indexStore2.create(Item.create("test2" + System.nanoTime(), System.nanoTime()), card);

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

		res = indexStore.search(VCardQuery.create("value.identification.anniversary:[1703977200000 TO *]"));
		assertEquals(1, res.total);

		res = indexStore.search(VCardQuery.create("value.identification.anniversary:[1672441200000 TO *]"));
		assertEquals(2, res.total);
	}

	@Test
	public void testSearch_25000() throws Exception {
		int max = 25000;
        List<ItemValue<VCard>> allCards = new ArrayList<>();
		for (int i = 0; i < max; i++) {
			VCard card = new VCard();
			card.identification = new VCard.Identification();
			card.identification.formatedName = VCard.Identification.FormatedName.create(String.format("card%05d", i),
					Arrays.<VCard.Parameter>asList());
			String uid = String.format("test%05d", i);
			allCards.add(ItemValue.create(Item.create(uid, System.nanoTime()), card));
		}
		indexStore.updates(allCards);

		refreshIndexes();

		// test with specific unique formatted name
		ListResult<String> res = indexStore
				.search(VCardQuery.create("value.identification.formatedName.value:card11111"));
		assertEquals(1, res.total);

		// test empty result
		res = indexStore.search(VCardQuery.create("value.identification.formatedName.value:fakeName"));
		assertEquals(0, res.total);

		// test more than 10000
		VCardQuery q = VCardQuery.create("value.identification.formatedName.value:card*");
		q.size = 1000;
		q.from = 15000;
		res = indexStore.search(q);
		assertEquals(max, res.total);
		assertEquals("test15000", res.values.get(0));
		assertEquals("test15999", res.values.get(res.values.size() - 1));

		// test more than 10000 with specific order
		q.orderBy = VCardQuery.OrderBy.Pertinance;
		res = indexStore.search(q);
		assertEquals(max, res.total);
		assertEquals("test15000", res.values.get(0));
		assertEquals("test15999", res.values.get(res.values.size() - 1));

		// test specific from and size = 0
		q.size = 0;
		q.from = 15000;
		res = indexStore.search(q);
		assertEquals(max, res.total);
		assertEquals(0, res.values.size());

		// test specific from and size = -1
		q.size = -1;
		q.from = 20000;
		res = indexStore.search(q);
		assertEquals(max, res.total);
		assertEquals("test20000", res.values.get(0));
		assertEquals("test24999", res.values.get(res.values.size() - 1));

		// test specific from and size = -1 less than 10000
		q.size = -1;
		q.from = 100;
		res = indexStore.search(q);
		assertEquals(max, res.total);

	}

	@Test
	public void testSearchByCategory() throws ElasticsearchException, IOException {
		VCard card1 = new VCard();
		TagRef tag1 = new TagRef();
		tag1.label = "tag1";
		String uid1 = "test" + System.nanoTime();
		card1.explanatory.categories = Arrays.asList(tag1);
		indexStore.create(Item.create(uid1, System.nanoTime()), card1);

		VCard card2 = new VCard();
		TagRef tag2 = new TagRef();
		tag2.label = "tag2";
		String uid2 = "test" + System.nanoTime();
		card1.explanatory.categories = Arrays.asList(tag2);
		indexStore.create(Item.create(uid2, System.nanoTime()), card2);

		refreshIndexes();

		ListResult<String> res = indexStore.search(VCardQuery.create("value.explanatory.categories.label:tag1"));
		assertEquals(1, res.total);

		res = indexStore.search(VCardQuery.create("value.explanatory.categories.label:tag1"));
		assertEquals(1, res.total);

		res = indexStore.search(VCardQuery.create("value.explanatory.categories.label:tagX"));
		assertEquals(0, res.total);
	}

	@Test
	public void testSearchByEmail() throws ElasticsearchException, IOException {
		VCard card = new VCard();

		String email = "email" + System.currentTimeMillis() + "@domain.lan";
		card.communications.emails = Arrays.asList(Email.create(email, Arrays.<VCard.Parameter>asList()));

		String uid = "test" + System.nanoTime();
		Item item = Item.create(uid, UUID.randomUUID().toString());

		indexStore.create(item, card);
		refreshIndexes();

		ListResult<String> res = indexStore.search(VCardQuery.create("value.communications.emails.value:" + email));
		assertEquals(1, res.total);

		res = indexStore.search(VCardQuery.create("value.communications.emails.value:\"unknownemail@domain.lan\""));
		assertEquals(0, res.total);
	}

	@Test
	public void testSearchByLongEmail() throws ElasticsearchException, IOException {
		VCard card = new VCard();

		String email = "pref-publique-cartesgrises@haute-garonne.gouv.fr";
		card.communications.emails = Arrays.asList(Email.create(email, Arrays.<VCard.Parameter>asList()));

		String uid = "test" + System.nanoTime();
		Item item = Item.create(uid, UUID.randomUUID().toString());

		indexStore.create(item, card);
		refreshIndexes();

		ListResult<String> res = indexStore.search(VCardQuery.create("value.communications.emails.value:" + email));
		assertEquals(1, res.total);
	}

	@Test
	public void testSearchMatchAll() throws ElasticsearchException, IOException {
		VCard card = new VCard();
		card.identification.formatedName.value = "john";

		String uid = "test" + System.nanoTime();
		Item item = Item.create(uid, UUID.randomUUID().toString());

		indexStore.create(item, card);
		refreshIndexes();

		ListResult<String> res = indexStore.search(VCardQuery.create(null));
		assertEquals(1, res.total);
	}

	@Test
	public void testSearchSort() throws ElasticsearchException, IOException {

		VCard card = new VCard();
		card.identification.formatedName.value = "john";
		String uid1 = "test" + System.nanoTime();
		indexStore.create(Item.create(uid1, System.nanoTime()), card);

		card = new VCard();
		card.identification.formatedName.value = "albator";
		String uid2 = "test" + System.nanoTime();
		indexStore.create(Item.create(uid2, System.nanoTime()), card);

		card = new VCard();
		card.identification.formatedName.value = "zorro";
		String uid3 = "test" + System.nanoTime();
		indexStore.create(Item.create(uid3, System.nanoTime()), card);

		refreshIndexes();

		ListResult<String> res = indexStore.search(VCardQuery.create(null));
		assertEquals(3, res.total);
		assertEquals(uid2, res.values.get(0));
		assertEquals(uid1, res.values.get(1));
		assertEquals(uid3, res.values.get(2));
	}

	@Test
	public void testSearchFormatedName() throws ElasticsearchException, IOException {
		VCard card = new VCard();
		card.identification = new VCard.Identification();
		card.identification.formatedName = VCard.Identification.FormatedName.create("Thomas",
				Arrays.<VCard.Parameter>asList());
		String uid = "test" + System.nanoTime();
		Item item = Item.create(uid, UUID.randomUUID().toString());
		indexStore.create(item, card);

		item = Item.create(uid, UUID.randomUUID().toString());
		indexStore.create(item, card);

		refreshIndexes();
		ListResult<String> res = indexStore.search(VCardQuery.create("value.identification.formatedName.value:tho"));
		assertEquals(1, res.total);
	}

	@Test
	public void testUpdate() throws SQLException, ElasticsearchException, IOException {
		VCard card = new VCard();
		card.identification.formatedName.value = "batman";
		Item item1 = itemStore.create(Item.create("uid" + System.nanoTime(), UUID.randomUUID().toString()));
		indexStore.create(item1, card);

		refreshIndexes();

		ListResult<String> res = indexStore.search(VCardQuery.create("value.identification.formatedName.value:batman"));
		assertEquals(1, res.total);

		card.identification.formatedName.value = "robin";

		indexStore.update(item1, card);

		refreshIndexes();

		res = indexStore.search(VCardQuery.create("value.identification.formatedName.value:batman"));
		assertEquals(0, res.total);
		res = indexStore.search(VCardQuery.create("value.identification.formatedName.value:robin"));
		assertEquals(1, res.total);

	}

	@Test
	public void testUpdates() throws SQLException, ElasticsearchException, IOException {
		VCard card = new VCard();
		card.identification.formatedName.value = "batman";
		Item item1 = itemStore.create(Item.create("uid" + System.nanoTime(), UUID.randomUUID().toString()));
		indexStore.create(item1, card);

		VCard card2 = new VCard();
		card2.identification.formatedName.value = "robin";
		Item item2 = itemStore.create(Item.create("uid" + System.nanoTime(), UUID.randomUUID().toString()));
		indexStore.create(item2, card2);

		refreshIndexes();

		ListResult<String> res = indexStore.search(VCardQuery.create("value.identification.formatedName.value:batman"));
		assertEquals(1, res.total);

		res = indexStore.search(VCardQuery.create("value.identification.formatedName.value:robin"));
		assertEquals(1, res.total);

		card.identification.formatedName.value = "wallace";
		card2.identification.formatedName.value = "gromit";

		indexStore.updates(Arrays.asList(ItemValue.create(item1, card), ItemValue.create(item2, card2)));

		refreshIndexes();

		res = indexStore.search(VCardQuery.create("value.identification.formatedName.value:batman"));
		assertEquals(0, res.total);
		res = indexStore.search(VCardQuery.create("value.identification.formatedName.value:wallace"));
		assertEquals(1, res.total);

		res = indexStore.search(VCardQuery.create("value.identification.formatedName.value:robin"));
		assertEquals(0, res.total);
		res = indexStore.search(VCardQuery.create("value.identification.formatedName.value:gromit"));
		assertEquals(1, res.total);

	}

	private void refreshIndexes() throws ElasticsearchException, IOException {
		client.indices().refresh(r -> r.index(VCARD_WRITE_ALIAS));
	}
}
