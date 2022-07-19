/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2017
  *
  * This file is part of BlueMind. BlueMind is a messaging and collaborative
  * solution.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of either the GNU Affero General Public License as
  * published by the Free Software Foundation (version 3 of the License).
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.backend.mail.replica.persistence;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Splitter;

import net.bluemind.backend.mail.api.MessageBody;
import net.bluemind.backend.mail.api.flags.MailboxItemFlag;
import net.bluemind.backend.mail.replica.api.ImapBinding;
import net.bluemind.backend.mail.replica.api.MailboxRecord;
import net.bluemind.backend.mail.replica.api.MailboxRecord.InternalFlag;
import net.bluemind.backend.mail.replica.api.MailboxRecordItemUri;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.model.ItemFlag;
import net.bluemind.core.container.model.ItemFlagFilter;
import net.bluemind.core.container.model.SortDescriptor;
import net.bluemind.core.container.persistence.AbstractItemValueStore;
import net.bluemind.core.container.persistence.FlagsSqlFilter;
import net.bluemind.core.container.persistence.LongCreator;
import net.bluemind.core.container.persistence.StringCreator;

public class MailboxRecordStore extends AbstractItemValueStore<MailboxRecord> {

	private static final Logger logger = LoggerFactory.getLogger(MailboxRecordStore.class);
	private static final Creator<MailboxRecord> MB_CREATOR = con -> new MailboxRecord();
	private final Container container;

	public MailboxRecordStore(DataSource pool, Container container) {
		super(pool);
		this.container = container;
	}

	public MailboxRecordStore(DataSource pool) {
		this(pool, null);
	}

	private static final String REC_CREATE_QUERY = "INSERT INTO t_mailbox_record (message_body_guid, "
			+ MailboxRecordColumns.COLUMNS.names() + ", container_id, item_id) VALUES (decode(?, 'hex'), "
			+ MailboxRecordColumns.COLUMNS.values() + ", ?, ?)";

	@Override
	public void create(Item item, MailboxRecord value) throws SQLException {
		insert(REC_CREATE_QUERY, value, MailboxRecordColumns.values(container.id, item));
	}

	private static final String REC_UPDATE_QUERY = "UPDATE t_mailbox_record SET (message_body_guid, "
			+ MailboxRecordColumns.COLUMNS.names() + ") = (decode(?, 'hex'), " + MailboxRecordColumns.COLUMNS.values()
			+ " )" + " WHERE container_id = ? AND item_id = ?";

	@Override
	public void update(Item item, MailboxRecord value) throws SQLException {
		update(REC_UPDATE_QUERY, value, MailboxRecordColumns.values(container.id, item));
	}

	@Override
	public void delete(Item item) throws SQLException {
		delete("DELETE FROM t_mailbox_record WHERE item_id = ?", new Object[] { item.id });
	}

	private static final String GET_ITEM_QUERY = "SELECT encode(message_body_guid, 'hex'), "
			+ MailboxRecordColumns.COLUMNS.names() + " FROM t_mailbox_record WHERE item_id = ?";
	private static final List<EntityPopulator<MailboxRecord>> POPULATORS = Arrays
			.asList(MailboxRecordColumns.populator());

	@Override
	public MailboxRecord get(Item item) throws SQLException {
		return unique(GET_ITEM_QUERY, MB_CREATOR, POPULATORS, new Object[] { item.id });
	}

	@Override
	public List<MailboxRecord> getMultiple(List<Item> items) throws SQLException {
		String query = "select item_id, encode(message_body_guid, 'hex'), " + MailboxRecordColumns.COLUMNS.names()
				+ " FROM t_mailbox_record WHERE item_id = ANY(?::int4[])";
		List<ItemV<MailboxRecord>> values = select(query, con -> new ItemV<MailboxRecord>(), (rs, index, itemv) -> {
			itemv.itemId = rs.getLong(index++);
			itemv.value = new MailboxRecord();
			return MailboxRecordColumns.populator().populate(rs, index, itemv.value);
		}, new Object[] { items.stream().map(i -> i.id).toArray(Long[]::new) });

		return join(items, values);
	}

	public List<MailboxRecordItemV> getExpiredItems(int days) throws SQLException {
		String query = "select c.uid, mbr.item_id, encode(message_body_guid, 'hex'), "
				+ MailboxRecordColumns.COLUMNS.names("mbr") + " FROM t_mailbox_record mbr " //
				+ "JOIN t_container c on c.id = mbr.container_id "//
				+ "WHERE mbr.system_flags::bit(32) & (" + InternalFlag.expunged.value + ")::bit(32)= " //
				+ "(" + InternalFlag.expunged.value + ")::bit(32) " //
				+ "AND mbr.last_updated < (now() - interval '" + days + " days') LIMIT 10000";

		return select(query, con -> new MailboxRecordItemV(), (rs, index, itemv) -> {
			itemv.containerUid = rs.getString(index++);
			itemv.item = new ItemV<>();
			itemv.item.itemId = rs.getLong(index++);
			itemv.item.value = new MailboxRecord();
			return MailboxRecordColumns.populator().populate(rs, index, itemv.item.value);
		}, new Object[0]);
	}

	@Override
	public void deleteAll() throws SQLException {
		delete("DELETE FROM t_mailbox_record WHERE container_id = ?", new Object[] { container.id });
	}

	/**
	 * Retrieve a collection of {@link RecordID} from an array of imap uids.
	 * 
	 * @param uidArrays
	 * @return
	 * @throws SQLException
	 */
	public List<RecordID> identifiers(long... uidArrays) throws SQLException {
		if (uidArrays.length == 0) {
			return Collections.emptyList();
		}
		String inString = Arrays.stream(uidArrays).mapToObj(Long::toString).collect(Collectors.joining(","));
		String query = "" + //
				"SELECT mr.imap_uid, mr.item_id FROM t_mailbox_record mr "
				+ "WHERE mr.container_id=? AND mr.imap_uid IN (" + inString + ")";
		return select(query, RecordID.CREATOR, RecordID.POPULATOR, new Object[] { container.id });
	}

	public List<ImapBinding> bindings(List<Long> itemIds) throws SQLException {
		if (itemIds.isEmpty()) {
			return Collections.emptyList();
		}
		String inString = itemIds.stream().map(Object::toString).collect(Collectors.joining(","));
		String query = "" + //
				"SELECT item_id, imap_uid, encode(message_body_guid, 'hex') FROM t_mailbox_record "
				+ "WHERE container_id = ? AND item_id IN (" + inString + ")";
		List<ImapBinding> notSorted = select(query, rs -> new ImapBinding(), (rs, index, value) -> {
			value.itemId = rs.getInt(index++);
			value.imapUid = rs.getInt(index++);
			value.bodyGuid = rs.getString(index++);
			return index;
		}, new Object[] { container.id });
		List<ImapBinding> ret = new ArrayList<>(notSorted.size());
		Map<Long, ImapBinding> sortHelper = notSorted.stream().collect(Collectors.toMap(ib -> ib.itemId, ib -> ib));
		itemIds.forEach(k -> Optional.ofNullable(sortHelper.get(k)).ifPresent(ret::add));
		return ret;
	}

	public List<Long> sortedIds(SortDescriptor sorted) throws SQLException {
		logger.debug("sorted by {}", sorted);
		String query = "SELECT item.id FROM t_mailbox_record rec "
				+ "INNER JOIN t_container_item item ON rec.item_id = item.id " //
				+ "WHERE item.container_id = ? " //
				+ "AND (item.flags::bit(32) & 2::bit(32)) = 0::bit(32)"; // not
																			// deleted
		StringBuilder sort = new StringBuilder();
		if (sorted == null || sorted.fields.isEmpty()) {
			sort.append("rec.internal_date desc");
		} else {
			sorted.fields.forEach(field -> {
				String dir = field.dir == SortDescriptor.Direction.Asc ? "ASC" : "DESC";
				sort.append(field.column + " " + dir + ",");
			});
			sort.deleteCharAt(sort.length() - 1);
		}
		query += " ORDER BY " + sort.toString();

		return select(query, LongCreator.FIRST, Collections.emptyList(), new Object[] { container.id });
	}

	/**
	 * @return the list of {@link ImapBinding} for which the corresponding
	 *         {@link MessageBody} has a {@link MessageBody#bodyVersion} lower than
	 *         <code>version</code>
	 */
	public List<ImapBinding> havingBodyVersionLowerThan(final int version) throws SQLException {
		final StringBuilder sql = new StringBuilder();
		sql.append("SELECT ci.id, mbr.imap_uid, encode(mbr.message_body_guid, 'hex') FROM t_mailbox_record mbr ");
		sql.append("INNER JOIN t_container_item ci ON ci.id = mbr.item_id ");
		sql.append("LEFT JOIN t_message_body mb ON mbr.message_body_guid = mb.guid ");
		sql.append("WHERE ci.container_id = ? ");
		sql.append("AND (mb.body_version < ? OR mb.guid IS NULL) ");
		sql.append("AND (ci.flags::bit(32) & (" + ItemFlag.Deleted.value + ")::bit(32)) = 0::bit(32) ");
		sql.append("AND (mbr.system_flags::bit(32) & (" + InternalFlag.expunged.value + ")::bit(32)) = 0::bit(32)");
		return select(sql.toString(), rs -> new ImapBinding(), (rs, index, value) -> {
			value.itemId = rs.getInt(index++);
			value.imapUid = rs.getInt(index++);
			value.bodyGuid = rs.getString(index++);
			return index;
		}, new Object[] { container.id, version });
	}

	public List<ImapBinding> recentItems(Date d) throws SQLException {
		String query = "SELECT rec.item_id, rec.imap_uid, encode(rec.message_body_guid, 'hex') FROM t_mailbox_record rec "
				+ "INNER JOIN t_message_body mb ON rec.message_body_guid = mb.guid "//
				+ "WHERE rec.container_id = ? AND mb.date_header >= ?";

		return select(query, rs -> new ImapBinding(), (rs, index, value) -> {
			value.itemId = rs.getInt(index++);
			value.imapUid = rs.getInt(index++);
			value.bodyGuid = rs.getString(index++);
			return index;
		}, new Object[] { container.id, new Timestamp(d.getTime()) });
	}

	public List<ImapBinding> unreadItems() throws SQLException {
		String query = "SELECT item.id, rec.imap_uid FROM t_mailbox_record rec "
				+ "INNER JOIN t_container_item item ON rec.item_id=item.id " //
				+ "WHERE item.container_id = ? " //
				+ "AND (item.flags::bit(32) & (" + ItemFlag.Deleted.value + ")::bit(32)) = 0::bit(32) " // not deleted
				+ "AND (item.flags::bit(32) & (" + ItemFlag.Seen.value + ")::bit(32)) = 0::bit(32) " // not seen
				+ "ORDER BY internal_date DESC";

		return select(query, rs -> new ImapBinding(), (rs, index, value) -> {
			value.itemId = rs.getInt(index++);
			value.imapUid = rs.getInt(index++);
			value.bodyGuid = null;
			return index;
		}, new Object[] { container.id });
	}

	public static class MailboxRecordItemV {
		private ItemV<MailboxRecord> item;
		private String containerUid;

		private MailboxRecordItemV() {
		}

		public ItemV<MailboxRecord> item() {
			return item;
		}

		public String containerUid() {
			return containerUid;
		}
	}

	public List<MailboxRecordItemUri> getBodyGuidReferences(String guid) throws SQLException {
		String query = "SELECT c.uid, ci.uid, encode(mbr.message_body_guid, 'hex'), mbr.imap_uid, c.owner " //
				+ "FROM t_mailbox_record mbr " //
				+ "JOIN t_container_item ci ON ci.id = mbr.item_id " //
				+ "JOIN t_container c ON c.id = ci.container_id " //
				+ "WHERE mbr.message_body_guid = decode(?, 'hex') ORDER BY ci.created";

		return select(query, con -> new MailboxRecordItemUri(), (rs, index, itemUri) -> {
			itemUri.containerUid = rs.getString(index++);
			itemUri.itemUid = rs.getString(index++);
			itemUri.bodyGuid = rs.getString(index++);
			itemUri.imapUid = rs.getLong(index++);
			itemUri.owner = rs.getString(index++);
			return index;
		}, new Object[] { guid });
	}

	public String getImapUidReferences(long uid, String owner) throws SQLException {
		String query = "SELECT encode(mbr.message_body_guid, 'hex') " //
				+ "FROM t_mailbox_record mbr " //
				+ "JOIN t_container_item ci ON ci.id = mbr.item_id " //
				+ "JOIN t_container c ON c.id = ci.container_id " //
				+ "WHERE c.id=? AND mbr.imap_uid = ? AND c.owner = ? ORDER BY ci.created";

		return unique(query, StringCreator.FIRST, Collections.emptyList(), new Object[] { container.id, uid, owner });
	}

	public List<Long> getConversationIds(ItemFlagFilter filter) throws SQLException {
		StringBuilder query = new StringBuilder("FROM v_conversation_by_folder " //
				+ "WHERE folder_id=?");

		int mustNot = filter.mustNot.stream().filter(flag -> flag != ItemFlag.Deleted).map(this::adaptFlag).reduce(0,
				(f, flag) -> f | flag);
		if (mustNot != 0) {
			query.append(" AND (mask::bit(32) & " + mustNot + "::bit(32)) = " + mustNot + "::bit(32)");
		}
		int must = filter.must.stream().map(this::adaptFlag).reduce(0, (f, flag) -> f | flag);
		if (must != 0) {
			query.append(" AND (flags::bit(32) & " + must + "::bit(32)) = " + must + "::bit(32)");
		}
		query.append(" order by date desc");
		String sqlValues = "SELECT conversation_id " + query.toString();

		return select(sqlValues, LongCreator.FIRST, Collections.emptyList(), new Object[] { container.id });
	}

	private int adaptFlag(ItemFlag flag) {
		switch (flag) {
		case Seen:
			return MailboxItemFlag.System.Seen.value().value;
		case Deleted:
			return MailboxItemFlag.System.Deleted.value().value;
		case Important:
			return MailboxItemFlag.System.Flagged.value().value;
		default:
			throw new IllegalArgumentException();
		}
	}

	public List<Long> getItemsByConversations(List<String> conversationUids) throws SQLException {
		Long[] conversationIds = conversationUids.stream().map(uid -> Long.parseUnsignedLong(uid, 16))
				.toArray(Long[]::new);
		String select = "SELECT ci.id FROM t_mailbox_record rec JOIN t_container_item ci on rec.item_id = ci.id WHERE rec.conversation_id = ANY(?) AND rec.container_id = ? AND (ci.flags::bit(32) & 2::bit(32)) = 0::bit(32)";

		return select(select, LongCreator.FIRST, Collections.emptyList(),
				new Object[] { conversationIds, container.id });
	}

	public List<Long> imapIdset(String set, ItemFlagFilter itemFilter) throws SQLException {
		String q = "select rec.item_id from t_mailbox_record rec "
				+ "INNER JOIN t_container_item ci on rec.item_id = ci.id WHERE rec.container_id=? AND " + asSql(set)
				+ FlagsSqlFilter.filterSql("ci", itemFilter);
		return select(q, LongCreator.FIRST, Collections.emptyList(), new Object[] { container.id });
	}

	public static String asSql(String idset) {
		List<String> parts = new ArrayList<>();
		Splitter.on(',').splitToStream(idset).forEach(r -> {
			int idx = r.indexOf(':');
			if (idx > 0) {
				String start = r.substring(0, idx);
				String upper = r.substring(idx + 1, r.length());
				if (upper.equals("*")) {
					parts.add("rec.imap_uid >= " + start);
				} else {
					parts.add("(rec.imap_uid >= " + start + " and rec.imap_uid <= " + upper + ")");
				}
			} else {
				parts.add("rec.imap_uid = " + r);
			}
		});
		return parts.stream().collect(Collectors.joining(" OR ", "(", ")"));
	}
}
