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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.addressbook.api.VCard;
import net.bluemind.addressbook.api.VCard.Kind;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.model.SortDescriptor;
import net.bluemind.core.container.persistence.AbstractItemValueStore;
import net.bluemind.core.container.persistence.LongCreator;
import net.bluemind.core.container.persistence.StringCreator;

public class VCardStore extends AbstractItemValueStore<VCard> {

	private static final Logger logger = LoggerFactory.getLogger(VCardStore.class);

	private static final Creator<VCard> CARD_CREATOR = con -> new VCard();

	private Container container;

	public VCardStore(DataSource pool, Container container) {
		super(pool);
		this.container = container;
	}

	@Override
	public void create(Item item, VCard card) throws SQLException {
		String query = "INSERT INTO t_addressbook_vcard (" + VCardColumns.COLUMNS_MAIN.names() + ", item_id) VALUES ("
				+ VCardColumns.COLUMNS_MAIN.values() + ", ?)";
		insert(query, card, VCardColumns.values(item));
	}

	@Override
	public void update(Item item, VCard value) throws SQLException {
		String query = "UPDATE t_addressbook_vcard SET (" + VCardColumns.COLUMNS_MAIN.names() + ") = ("
				+ VCardColumns.COLUMNS_MAIN.values() + ") " + " WHERE item_id = ?";
		update(query, value, VCardColumns.values(item));
	}

	private static final String GET_QUERY = "SELECT " + VCardColumns.COLUMNS_MAIN.names()
			+ " FROM t_addressbook_vcard WHERE item_id = ?";

	@Override
	public VCard get(Item item) throws SQLException {
		return unique(GET_QUERY, CARD_CREATOR, VCardColumns.populator(), new Object[] { item.id });
	}

	@Override
	public void delete(Item item) throws SQLException {
		delete("DELETE FROM t_addressbook_vcard WHERE item_id = ?", new Object[] { item.id });
	}

	@Override
	public void deleteAll() throws SQLException {
		delete("DELETE FROM t_addressbook_vcard WHERE item_id IN (SELECT id FROM t_container_item WHERE container_id = ?)",
				new Object[] { container.id });
	}

	@Override
	public List<VCard> getMultiple(List<Item> items) throws SQLException {
		String query = "SELECT item_id, " + VCardColumns.COLUMNS_MAIN.names()
				+ " FROM t_addressbook_vcard WHERE item_id = ANY(?::int4[])";
		List<ItemV<VCard>> values = select(query, (ResultSet con) -> new ItemV<>(),
				(ResultSet rs, int index, ItemV<VCard> card) -> {
					card.itemId = rs.getLong(index++);
					card.value = new VCard();
					return VCardColumns.populator().populate(rs, index, card.value);
				}, new Object[] { items.stream().map(i -> i.id).toArray(Long[]::new) });

		return join(items, values);
	}

	public List<String> findByEmail(String email) throws SQLException {
		String query = "select item.uid from t_addressbook_vcard card, t_container_item item where "
				+ " item.container_id = ? AND card.item_id = item.id" //
				+ " AND card.email @> ?::text[]";
		return select(query, StringCreator.FIRST, Collections.emptyList(),
				new Object[] { container.id, new String[] { email } });
	}

	public List<String> findGroupsContaining(String[] uid) throws SQLException {
		String query = "SELECT item.uid FROM t_addressbook_vcard card, t_container_item item WHERE "
				+ " item.container_id = ? AND card.item_id = item.id" //
				+ " AND card.member_item_uid && (?::text[])";
		return select(query, StringCreator.FIRST, Collections.emptyList(), new Object[] { container.id, uid });
	}

	public List<String> findByKind(Kind... kinds) throws SQLException {
		String query = "SELECT item.uid FROM t_addressbook_vcard card, t_container_item item WHERE "
				+ " item.container_id = ? AND card.item_id = item.id" //
				+ " AND card.kind = ANY(?::text[]) ";
		return select(query, new StringCreator(1), Arrays.asList(),
				new Object[] { container.id, Arrays.stream(kinds).map(Kind::name).toArray(String[]::new) });
	}

	public List<Long> sortedIds(SortDescriptor sorted) throws SQLException {
		logger.debug("sorted by {}", sorted);
		String query = "SELECT item.id FROM t_addressbook_vcard rec "
				+ "INNER JOIN t_container_item item ON rec.item_id = item.id " //
				+ "WHERE item.container_id = ? " //
				+ "AND (item.flags::bit(32) & 2::bit(32)) = 0::bit(32) " // not deleted
				+ "ORDER BY item.created DESC";
		// FIXME use sort params
		return select(query, LongCreator.FIRST, Collections.emptyList(), new Object[] { container.id });
	}

}
