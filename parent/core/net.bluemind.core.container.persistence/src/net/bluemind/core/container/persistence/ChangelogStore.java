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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.container.model.ChangeLogEntry;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.ContainerChangeset;
import net.bluemind.core.container.model.ItemChangeLogEntry;
import net.bluemind.core.container.model.ItemChangelog;
import net.bluemind.core.container.model.ItemFlag;
import net.bluemind.core.container.model.ItemFlagFilter;
import net.bluemind.core.container.model.ItemIdentifier;
import net.bluemind.core.container.model.ItemVersion;
import net.bluemind.core.jdbc.JdbcAbstractStore;

public class ChangelogStore extends JdbcAbstractStore {

	@SuppressWarnings("unused")
	private static final Logger logger = LoggerFactory.getLogger(ChangelogStore.class);

	private final Container container;

	private class ChangelogStatementValues implements StatementValues<LogEntry> {

		private byte type;

		public ChangelogStatementValues(byte type) {
			this.type = type;
		}

		@Override
		public int setValues(Connection con, PreparedStatement statement, int index, int currentRow, LogEntry value)
				throws SQLException {
			statement.setLong(index++, value.version);
			statement.setLong(index++, container.id);
			statement.setString(index++, value.itemUid);
			statement.setByte(index++, type);
			statement.setLong(index++, value.internalId);
			statement.setLong(index++, value.weightSeed);
			return index;
		}

	}

	private class ChangelogEntryPopulator implements EntityPopulator<ChangeLogEntry> {

		@Override
		public int populate(ResultSet rs, int index, ChangeLogEntry value) throws SQLException {
			value.version = rs.getLong(index++);
			value.itemUid = rs.getString(index++);
			value.type = ChangeLogEntry.Type.values()[rs.getByte(index++)];
			value.date = Date.from(rs.getTimestamp(index++).toInstant());
			value.internalId = rs.getLong(index++);
			value.weightSeed = rs.getLong(index++);
			value.author = "unknown";
			value.origin = "unknown";
			return index;
		}

	}

	private class LightChangelogEntryPopulator implements EntityPopulator<ChangeLogEntry> {

		@Override
		public int populate(ResultSet rs, int index, ChangeLogEntry value) throws SQLException {
			value.version = rs.getLong(index++);
			value.type = ChangeLogEntry.Type.values()[rs.getByte(index++)];
			value.itemUid = rs.getString(index++);
			value.internalId = rs.getLong(index++);
			value.weightSeed = rs.getLong(index++);
			return index;
		}

	}

	private class FlaggedChangelogEntryPopulator implements EntityPopulator<FlaggedChangeLogEntry> {

		@Override
		public int populate(ResultSet rs, int index, FlaggedChangeLogEntry value) throws SQLException {
			value.version = rs.getLong(index++);
			value.type = ChangeLogEntry.Type.values()[rs.getByte(index++)];
			value.itemUid = rs.getString(index++);
			value.internalId = rs.getLong(index++);
			value.flags = ItemFlag.flags(rs.getInt(index++));
			value.weightSeed = rs.getLong(index++);
			return index;
		}

	}

	public ChangelogStore(DataSource pool, Container container) {
		super(pool);
		this.container = container;
	}

	public static class LogEntry {
		public long version;
		public String itemUid;
		public Date date;
		public long internalId;
		public long weightSeed;

		public static LogEntry create(long version, String itemUid, String itemExtId, String author, String origin,
				long itemId, long weightSeed) {
			LogEntry ret = new LogEntry();
			ret.version = version;
			ret.itemUid = itemUid;
			ret.internalId = itemId;
			ret.weightSeed = weightSeed;
			return ret;
		}

		public static LogEntry create(String itemUid, String itemExtId, String author, String origin, long itemId,
				long weightSeed) {
			return create(-1, itemUid, itemExtId, author, origin, itemId, weightSeed);
		}
	}

	// proc_insert_t_changeset(p_version,p_container_id,p_item_uid,p_type,p_date,p_item_id,p_weight_seed)
	private static final String INSERT_QUERY = "CALL proc_insert_t_changeset(?,?,?,?,?,?);";

	private static final String CHANGESET_QUERY = """
			    SELECT version, type, item_uid, item_id, weight_seed
			    FROM t_changeset
			    WHERE container_id = ? AND version > ? order by item_id, version
			""";

	private static final String FLAGGED_CHANGESET_QUERY = """
			    SELECT cl.version, cl.type, cl.item_uid, cl.item_id, ci.flags, cl.weight_seed
			    FROM t_changeset cl
			    LEFT JOIN t_container_item ci ON (ci.container_id = cl.container_id AND ci.id = cl.item_id AND ci.version = cl.version)
			    WHERE cl.container_id = ? AND cl.version > ? order by cl.item_id, cl.version;
			""";

	private static final String DELETE_CHANGESET_QUERY = "DELETE FROM t_changeset WHERE container_id = ?";

	private static final String DELETE_CHANGESET_ITEM_QUERY = """
			    DELETE FROM t_changeset
			    WHERE container_id = ? AND item_uid = ? AND version = ?;
			""";

	private static final Creator<ChangeLogEntry> CREATOR = con -> new ChangeLogEntry();

	public void itemCreated(LogEntry entry) throws SQLException {
		execute(INSERT_QUERY, new Object[] { entry.version, container.id, entry.itemUid, (byte) 0, entry.internalId,
				entry.weightSeed });
	}

	public void itemUpdated(LogEntry entry) throws SQLException {
		execute(INSERT_QUERY, new Object[] { entry.version, container.id, entry.itemUid, (byte) 1, entry.internalId,
				entry.weightSeed });
	}

	public void itemDeleted(LogEntry entry) throws SQLException {
		execute(INSERT_QUERY, new Object[] { entry.version, container.id, entry.itemUid, (byte) 2, entry.internalId,
				entry.weightSeed });
	}

	private static final String ITEM_CHANGESET_NG_QUERY = //
			"SELECT version, item_uid, type, date, item_id, weight_seed " //
					+ " FROM t_changeset " //
					+ " where container_id = ? AND item_uid = ? AND version > ? AND  version <= ? "//
					+ " order by version";

	public ItemChangelog itemChangelog(String itemUid, long from, long to) throws SQLException {

		List<ChangeLogEntry> entries = select(ITEM_CHANGESET_NG_QUERY, CREATOR, new ChangelogEntryPopulator(),
				new Object[] { container.id, itemUid, from, to });
		ItemChangelog changelog = new ItemChangelog();
		changelog.entries = entries.stream().map(ItemChangeLogEntry::new).collect(Collectors.toList());
		return changelog;
	}

	// FIXME: bm-4 Remove useless to parameter
	public ContainerChangeset<String> changeset(long from, long to) throws SQLException {
		return changeset(s -> s, from, to);
	}

	public ContainerChangeset<String> changeset(IWeightProvider wp, long from, long to) throws SQLException {
		List<ChangeLogEntry> entries = select(CHANGESET_QUERY, CREATOR, new LightChangelogEntryPopulator(),
				new Object[] { container.id, from });
		return ChangelogUtils.toChangeset(wp, from, entries, entry -> entry.itemUid, ItemFlagFilter.all());
	}

	// FIXME: bm-4 Remove useless to parameter
	public ContainerChangeset<Long> changesetById(long from, long to) throws SQLException {
		return changesetById(s -> s, from, to);
	}

	public ContainerChangeset<Long> changesetById(IWeightProvider wp, long from, long to) throws SQLException {
		List<ChangeLogEntry> entries = select(CHANGESET_QUERY, CREATOR, new LightChangelogEntryPopulator(),
				new Object[] { container.id, from });
		return ChangelogUtils.toChangeset(wp, from, entries, entry -> entry.internalId, ItemFlagFilter.all());
	}

	public ContainerChangeset<ItemVersion> changesetById(long from, long to, ItemFlagFilter filter)
			throws SQLException {
		List<FlaggedChangeLogEntry> entries = select(FLAGGED_CHANGESET_QUERY, con -> new FlaggedChangeLogEntry(),
				new FlaggedChangelogEntryPopulator(), new Object[] { container.id, from });
		return ChangelogUtils.toChangeset(s -> s, from, entries, ItemVersion::new, filter);
	}

	public ContainerChangeset<ItemVersion> changesetById(IWeightProvider wp, long from, long to, ItemFlagFilter filter)
			throws SQLException {
		List<FlaggedChangeLogEntry> entries = select(FLAGGED_CHANGESET_QUERY, con -> new FlaggedChangeLogEntry(),
				new FlaggedChangelogEntryPopulator(), new Object[] { container.id, from });
		return ChangelogUtils.toChangeset(wp, from, entries, ItemVersion::new, filter);
	}

	public ContainerChangeset<ItemIdentifier> fullChangesetById(IWeightProvider wp, long from, long to)
			throws SQLException {
		List<FlaggedChangeLogEntry> entries = select(FLAGGED_CHANGESET_QUERY, con -> new FlaggedChangeLogEntry(),
				new FlaggedChangelogEntryPopulator(), new Object[] { container.id, from });
		return ChangelogUtils.toChangeset(wp, from, entries, ItemIdentifier::new, ItemFlagFilter.all());
	}

	public void deleteLog() throws SQLException {
		delete(DELETE_CHANGESET_QUERY, new Object[] { container.id });
	}

	public void insertLog(List<ChangeLogEntry> entries) throws SQLException {
		for (ChangeLogEntry entry : entries) {
			LogEntry le = LogEntry.create(entry.version, entry.itemUid, entry.itemExtId, entry.author, entry.origin,
					entry.internalId, entry.weightSeed);

			switch (entry.type) {
			case Created:
				itemCreated(le);
				break;
			case Deleted:
				itemDeleted(le);
				break;
			case Updated:
				itemUpdated(le);
				break;
			default:
				break;

			}
		}
	}

	private static final String INSERT_ITEMS_DELETED_QUERY = "INSERT INTO t_changeset (version, container_id, item_uid, type, date, item_id) "
			+ " ( SELECT seq.seq +  row_number() over () , i.container_id, i.uid, 2, now(), i.id FROM t_container_item i, t_container_sequence seq WHERE i.container_id = ? AND seq.container_id = i.container_id)";

	public void allItemsDeleted(String subject, String origin) throws SQLException {
		int insertCount = insert(INSERT_ITEMS_DELETED_QUERY, new Object[] { container.id });
		update("update t_container_sequence set seq = seq+? where container_id = ?",
				new Object[] { insertCount, container.id });
	}

}
