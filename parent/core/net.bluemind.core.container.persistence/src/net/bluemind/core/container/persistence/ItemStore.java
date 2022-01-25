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
package net.bluemind.core.container.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.model.ItemFlag;
import net.bluemind.core.container.model.ItemFlagFilter;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.Columns;
import net.bluemind.core.jdbc.JdbcAbstractStore;

public class ItemStore extends JdbcAbstractStore {

	public static final Columns COLUMNS = Columns.create() //
			.col("id")//
			.col("uid") //
			.col("version") //
			.col("external_id") //
			.col("displayname") //
			.col("createdby") //
			.col("updatedby") //
			.col("created") //
			.col("updated")//
			.col("flags");

	static final Logger logger = LoggerFactory.getLogger(ItemStore.class);
	private SecurityContext securityContext;
	protected final Container container;

	public static final EntityPopulator<Item> ITEM_POPULATOR = new EntityPopulator<Item>() {

		@Override
		public int populate(ResultSet rs, int index, Item value) throws SQLException {
			value.id = rs.getLong(index++);
			value.uid = rs.getString(index++);
			value.version = rs.getLong(index++);
			value.externalId = rs.getString(index++);
			value.displayName = rs.getString(index++);
			value.createdBy = rs.getString(index++);
			value.updatedBy = rs.getString(index++);
			value.created = rs.getTimestamp(index++);
			value.updated = rs.getTimestamp(index++);
			value.flags = ItemFlag.flags(rs.getInt(index++));

			return index;
		}

	};
	public static final List<EntityPopulator<Item>> ITEM_POPULATORS = Arrays.asList(ITEM_POPULATOR);

	private static final String BY_ID_QUERY = "SELECT " + COLUMNS.names() + " FROM t_container_item "
			+ " WHERE id = ? and container_id = ? ";
	private static final String BY_UID_QUERY = "SELECT " + COLUMNS.names() + " FROM t_container_item "
			+ " WHERE uid = ? and container_id = ? ";

	public ItemStore(DataSource pool, Container container, SecurityContext contextHolder) {
		super(pool);
		this.container = container;
		this.securityContext = contextHolder;
	}

	private static final String NEXT_VERSION_QUERY = "with nv as (update t_container_sequence set seq = seq+1 where container_id = ? RETURNING seq, container_id as contid) ";
	private static final String FORCED_VERSION_QUERY = "with nv as (update t_container_sequence set seq = %d where container_id = ? RETURNING seq, container_id as contid) ";

	public Item create(Item item) throws SQLException {
		String versionQuery = item.version == 0L ? NEXT_VERSION_QUERY
				: String.format(FORCED_VERSION_QUERY, item.version);
		String itemIdSeq = null;
		if (item.id > 0) {
			long currentSeqValue = unique("SELECT last_value FROM t_container_item_id_seq", new LongCreator(1),
					Collections.emptyList());
			if (currentSeqValue < item.id && item.version == 0L) {
				throw new SQLException(
						"ItemId " + item.id + " needs to be smaller than current sequence value of " + currentSeqValue);
			}
			itemIdSeq = Long.toString(item.id);
		} else {
			itemIdSeq = "nextval('t_container_item_id_seq')";
		}

		String insertQuery = versionQuery + "INSERT INTO t_container_item " //
				+ " (id, container_id, uid, version, external_id, displayname, createdby, updatedby, created, updated, flags) "
				+ "  " //
				+ "( SELECT " + itemIdSeq + ", " + container.id + ", ?, nv.seq, ?, ?, ?, ?, ?, ?, ? FROM nv )  " //
				+ " RETURNING " + COLUMNS.names();
		return insertAndReturn(insertQuery, item,
				Collections.singletonList((con, statement, index, rowIndex, value) -> {
					String principal = getPrincipal();
					statement.setLong(index++, container.id);
					statement.setString(index++, item.uid);
					statement.setString(index++, item.externalId);
					statement.setString(index++, item.displayName);
					statement.setString(index++, principal);
					statement.setString(index++, principal);
					statement.setTimestamp(index++, item.created != null ? toTimestamp(item.created) : now());
					statement.setTimestamp(index++, item.updated != null ? toTimestamp(item.updated) : now());
					statement.setInt(index++, ItemFlag.value(item.flags));
					return index;
				}), ItemCreator.INSTANCE, ITEM_POPULATOR);
	}

	private Timestamp toTimestamp(Date date) {
		return date == null ? now() : new Timestamp(date.getTime());
	}

	private Timestamp now() {
		return Timestamp.from(Instant.now());
	}

	public long count(ItemFlagFilter filter) throws SQLException {
		String q = "SELECT COUNT(*) FROM t_container_item ci WHERE container_id = " + container.id;
		q += FlagsSqlFilter.filterSql("ci", filter);
		return unique(q, rs -> rs.getLong(1), (rs, index, v) -> index);
	}

	public Item createWithUidNull(Item item) throws SQLException {
		String q = "with nv as ( with ll as ( select nextval('t_container_item_id_seq'::regclass) as zid,container_id, seq as nseq FROM t_container_sequence where container_id = ? "
				+ " FOR UPDATE ) "
				+ " UPDATE t_container_sequence SET seq = ll.nseq+1 FROM ll WHERE t_container_sequence.container_id = ll.container_id RETURNING seq, ll.zid) ";
		String iQ = "" //
				+ q //
				+ "INSERT INTO t_container_item " //
				+ " ( id, container_id, uid, version, external_id, displayname, createdby, updatedby, created, updated, flags) "
				+ " ( SELECT " //
				+ " nv.zid, " + container.id + ", nv.zid, nv.seq, ?, ?, ?, ?, now(), now(), ? FROM nv) " //
				+ " RETURNING " + COLUMNS.names();

		return insertAndReturn(iQ, item, Collections.singletonList((con, statement, index, rowIndex, value) -> {
			String principal = getPrincipal();
			statement.setLong(index++, container.id);
			statement.setString(index++, value.externalId);
			statement.setString(index++, value.displayName);
			statement.setString(index++, principal);
			statement.setString(index++, principal);
			statement.setLong(index++, ItemFlag.value(value.flags));
			return index;
		}), (rs) -> new Item(), ITEM_POPULATOR);
	}

	private static final String UPDATE_QUERY = ""//
			+ NEXT_VERSION_QUERY //
			+ "UPDATE t_container_item SET " //
			+ " (version, updatedby, updated, displayname) " + " = " //
			+ "(nv.seq, ?, now(), ?) FROM nv WHERE container_id = ? AND uid = ? " //
			+ " RETURNING " + COLUMNS.names();

	public Item update(String uid, final String displayName) throws SQLException {
		return insertAndReturn(UPDATE_QUERY, uid, Collections.singletonList((con, statement, index, rowIndex, uid1) -> {
			String principal = getPrincipal();
			statement.setLong(index++, container.id);
			statement.setString(index++, principal);
			statement.setString(index++, displayName);
			statement.setLong(index++, container.id);
			statement.setString(index++, uid1);
			return index;
		}), (rs) -> new Item(), ITEM_POPULATOR);
	}

	public Item setExtId(String uid, final String extId) throws SQLException {
		String updateQuery = ""//
				+ NEXT_VERSION_QUERY //
				+ "UPDATE t_container_item SET " //
				+ " (version, updatedby, updated, external_id) " + " = " //
				+ "(nv.seq, ?, now(), ?) FROM nv WHERE container_id = ? AND uid = ? " //
				+ " RETURNING " + COLUMNS.names();

		return insertAndReturn(updateQuery, uid, Collections.singletonList((con, statement, index, rowIndex, uid1) -> {
			String principal = getPrincipal();
			statement.setLong(index++, container.id);
			statement.setString(index++, principal);
			statement.setString(index++, extId);
			statement.setLong(index++, container.id);
			statement.setString(index++, uid1);
			return index;
		}), (rs) -> new Item(), ITEM_POPULATOR);
	}

	public Item update(String uid, final String displayName, Collection<ItemFlag> flags) throws SQLException {
		Item item = new Item();
		item.uid = uid;
		return update(item, displayName, flags);
	}

	public Item update(Item item, final String displayName, Collection<ItemFlag> flags) throws SQLException {
		String versionQuery = item.version == 0L ? NEXT_VERSION_QUERY
				: String.format(FORCED_VERSION_QUERY, item.version);
		String updateQuery = ""//
				+ versionQuery //
				+ "UPDATE t_container_item SET " //
				+ " (version, updatedby, updated, displayname, flags) " + " = " //
				+ "(nv.seq, ?, ?, ?, ?) FROM nv WHERE container_id = ? AND uid = ? RETURNING " + COLUMNS.names();

		return insertAndReturn(updateQuery, item.uid,
				Collections.singletonList((con, statement, index, rowIndex, uid1) -> {
					String principal = getPrincipal();
					statement.setLong(index++, container.id);
					statement.setString(index++, principal);
					statement.setTimestamp(index++, item.updated != null ? toTimestamp(item.updated) : now());
					statement.setString(index++, displayName);
					statement.setLong(index++, ItemFlag.value(flags));

					statement.setLong(index++, container.id);
					statement.setString(index++, uid1);
					return index;
				}), rs -> new Item(), ITEM_POPULATOR);
	}

	public Item update(long id, final String displayName, Collection<ItemFlag> flags) throws SQLException {
		String updateQuery = ""//
				+ NEXT_VERSION_QUERY //
				+ "UPDATE t_container_item SET " //
				+ " (version, updatedby, updated, displayname, flags) " + " = " //
				+ " (nv.seq, ?, now(), ?, ?) FROM nv WHERE container_id = ? AND id = ? RETURNING " + COLUMNS.names();

		return insertAndReturn(updateQuery, id, Collections.singletonList((con, statement, index, rowIndex, id1) -> {
			String principal = getPrincipal();
			statement.setLong(index++, container.id);
			statement.setString(index++, principal);
			statement.setString(index++, displayName);
			statement.setLong(index++, ItemFlag.value(flags));

			statement.setLong(index++, container.id);
			statement.setLong(index++, id1);
			return index;
		}), rs -> new Item(), ITEM_POPULATOR);
	}

	public Item touch(String uid) throws SQLException {
		String updateQuery = ""//
				+ NEXT_VERSION_QUERY //
				+ "UPDATE t_container_item SET " //
				+ " (version, updatedby, updated) " + " = " //
				+ "(nv.seq, ?, now()) FROM nv WHERE container_id = ? AND uid = ? " //
				+ " RETURNING " + COLUMNS.names();
		return insertAndReturn(updateQuery, uid, Collections.singletonList((con, statement, index, rowIndex, uid1) -> {
			String principal = getPrincipal();
			statement.setLong(index++, container.id);
			statement.setString(index++, principal);
			statement.setLong(index++, container.id);
			statement.setString(index++, uid1);
			return index;
		}), ItemCreator.INSTANCE, ITEM_POPULATOR);
	}

	public Item get(String uid) throws SQLException {
		return unique(BY_UID_QUERY, ItemCreator.INSTANCE, ITEM_POPULATORS, new Object[] { uid, container.id });
	}

	public Item getByExtId(String extId) throws SQLException {
		String selectQuery = "SELECT " + COLUMNS.names() + " FROM t_container_item "
				+ " WHERE external_id = ? AND container_id = ?";
		return unique(selectQuery, ItemCreator.INSTANCE, ITEM_POPULATORS, new Object[] { extId, container.id });

	}

	public Item getById(long id) throws SQLException {
		return unique(BY_ID_QUERY, ItemCreator.INSTANCE, ITEM_POPULATORS, new Object[] { id, container.id });
	}

	public List<Item> getMultiple(List<String> uids) throws SQLException {
		String selectQuery = "SELECT " + COLUMNS.names()
				+ " FROM t_container_item WHERE container_id = ? AND uid = ANY (?)";

		String[] array = uids.toArray(new String[0]);
		return sort(select(selectQuery, ItemCreator.INSTANCE, ITEM_POPULATORS, new Object[] { container.id, array }),
				uids, item -> item.uid);

	}

	public List<Item> getMultipleById(List<Long> uids) throws SQLException {
		if (uids.isEmpty()) {
			return Collections.emptyList();
		}
		StringBuilder selectQuery = new StringBuilder(
				"SELECT " + COLUMNS.names() + " FROM t_container_item WHERE container_id = ? AND id IN (0");
		for (long l : uids) {
			selectQuery.append(",").append(l);
		}
		selectQuery.append(")");
		return sort(
				select(selectQuery.toString(), ItemCreator.INSTANCE, ITEM_POPULATORS, new Object[] { container.id }),
				uids, item -> item.id);
	}

	public List<Item> all() throws SQLException {
		String selectQuery = "SELECT " + COLUMNS.names() + " FROM t_container_item WHERE container_id = ?";
		return select(selectQuery, ItemCreator.INSTANCE, ITEM_POPULATORS, new Object[] { container.id });
	}

	public List<Item> filtered(ItemFlagFilter filter) throws SQLException {
		return filtered(filter, null, null);
	}

	public List<Item> filtered(ItemFlagFilter filter, Integer offset, Integer limit) throws SQLException {
		String selectQuery = "SELECT " + COLUMNS.names() + " FROM t_container_item ci WHERE container_id = ? ";
		selectQuery += FlagsSqlFilter.filterSql("ci", filter);
		if (offset != null && limit != null) {
			selectQuery += " ORDER BY ci.id DESC LIMIT " + limit + " OFFSET " + offset;
		}
		return select(selectQuery, ItemCreator.INSTANCE, ITEM_POPULATORS, new Object[] { container.id });
	}

	public int getItemCount() throws SQLException {
		return unique("select count(*) FROM t_container_item WHERE container_id = ?", c -> {
			return c.getInt(1);
		}, Collections.emptyList(), new Object[] { container.id });
	}

	public void delete(Item item) throws SQLException {
		delete("DELETE FROM t_container_item WHERE container_id = ? AND id = ?",
				new Object[] { container.id, item.id });

	}

	public void deleteAll() throws SQLException {
		delete("DELETE FROM t_container_item WHERE container_id = ?", new Object[] { container.id });

	}

	private String getPrincipal() {
		return securityContext.getOwnerPrincipal();
	}

	public List<String> allItemUids() throws SQLException {
		String query = "SELECT uid FROM t_container_item WHERE container_id = ?";
		return select(query, StringCreator.FIRST, Collections.emptyList(), new Object[] { container.id });
	}

	public List<Long> allItemIds() throws SQLException {
		return select("SELECT id FROM t_container_item where container_id = ?", LongCreator.FIRST,
				Collections.emptyList(), new Object[] { container.id });
	}

	private static final String GET_FOR_UPDATE = "SELECT " + COLUMNS.names() + " FROM t_container_item "
			+ " WHERE uid = ? AND container_id = ? FOR NO KEY UPDATE";

	public Item getForUpdate(String uid) throws SQLException {
		return unique(GET_FOR_UPDATE, ItemCreator.INSTANCE, ITEM_POPULATORS, new Object[] { uid, container.id });
	}

	public Item getForUpdate(long id) throws SQLException {
		String selectQuery = "SELECT " + COLUMNS.names() + " FROM t_container_item "
				+ " WHERE id = ? and container_id = ? FOR NO KEY UPDATE";
		return unique(selectQuery, ItemCreator.INSTANCE, ITEM_POPULATORS, new Object[] { id, container.id });

	}

	private <T> List<Item> sort(List<Item> items, List<T> selectors, Function<Item, T> getSelector) {
		Map<T, Item> index = items.stream().collect(Collectors.toMap(getSelector, a -> a));
		return selectors.stream().map(index::get).filter(Objects::nonNull).collect(Collectors.toList());
	}

	public long getVersion() throws SQLException {
		return unique("SELECT seq FROM t_container_sequence WHERE container_id = ?", c -> c.getLong(1),
				Collections.emptyList(), new Object[] { container.id });
	}

}
