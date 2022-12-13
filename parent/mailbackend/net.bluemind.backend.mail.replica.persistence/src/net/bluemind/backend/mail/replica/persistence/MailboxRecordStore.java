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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import com.google.common.base.Splitter;

import net.bluemind.backend.mail.api.MessageBody;
import net.bluemind.backend.mail.api.flags.MailboxItemFlag;
import net.bluemind.backend.mail.replica.api.ImapBinding;
import net.bluemind.backend.mail.replica.api.MailboxRecord;
import net.bluemind.backend.mail.replica.api.MailboxRecord.InternalFlag;
import net.bluemind.backend.mail.replica.api.MailboxRecordItemUri;
import net.bluemind.backend.mail.replica.api.WithId;
import net.bluemind.core.container.api.Count;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.model.ItemFlag;
import net.bluemind.core.container.model.ItemFlagFilter;
import net.bluemind.core.container.persistence.AbstractItemValueStore;
import net.bluemind.core.container.persistence.LongCreator;
import net.bluemind.core.container.persistence.StringCreator;

/*
 * PLEASE READ ME
 * 
 * A word of warning: this table is partitionned by subtree_id and container_id,
 * so EVERY SINGLE REQUEST **MUST** use subtree_id and container_id
 */

public class MailboxRecordStore extends AbstractItemValueStore<MailboxRecord> {
	private static final Creator<MailboxRecord> MB_CREATOR = con -> new MailboxRecord();
	private final Container folderContainer;
	private final Container subtreeContainer;

	public MailboxRecordStore(DataSource pool, Container folderContainer, Container subtreeContainer) {
		super(pool);
		this.folderContainer = folderContainer;
		this.subtreeContainer = subtreeContainer;
	}

	/*
	 * Deprecated: specifying folderContainer and mailboxContainerId is mandatory
	 * because t_mailbox_record is partitioned by mailbox
	 */
	@Deprecated(forRemoval = true)
	public MailboxRecordStore(DataSource pool) {
		this(pool, null, null);
	}

	private static final String REC_CREATE_QUERY = "INSERT INTO t_mailbox_record (message_body_guid, "
			+ MailboxRecordColumns.COLUMNS.names() + ", subtree_id, container_id, item_id) VALUES (decode(?, 'hex'), "
			+ MailboxRecordColumns.COLUMNS.values() + ", ?, ?, ?)";

	@Override
	public void create(Item item, MailboxRecord value) throws SQLException {
		insert(REC_CREATE_QUERY, value, MailboxRecordColumns.values(subtreeContainer.id, folderContainer.id, item));
	}

	private static final String REC_UPDATE_QUERY = "UPDATE t_mailbox_record SET (message_body_guid, "
			+ MailboxRecordColumns.COLUMNS.names() + ") = (decode(?, 'hex'), " + MailboxRecordColumns.COLUMNS.values()
			+ " )" + " WHERE subtree_id = ? AND container_id = ? AND item_id = ?";

	@Override
	public void update(Item item, MailboxRecord value) throws SQLException {
		update(REC_UPDATE_QUERY, value, MailboxRecordColumns.values(subtreeContainer.id, folderContainer.id, item));
	}

	@Override
	public void delete(Item item) throws SQLException {
		delete("DELETE FROM t_mailbox_record WHERE subtree_id = ? AND container_id = ? AND item_id = ?",
				new Object[] { subtreeContainer.id, folderContainer.id, item.id });
	}

	private static final String GET_ITEM_QUERY = "SELECT encode(message_body_guid, 'hex'), "
			+ MailboxRecordColumns.COLUMNS.names()
			+ " FROM t_mailbox_record WHERE subtree_id = ? AND container_id = ? AND item_id = ?";
	private static final List<EntityPopulator<MailboxRecord>> POPULATORS = Arrays
			.asList(MailboxRecordColumns.populator());

	@Override
	public MailboxRecord get(Item item) throws SQLException {
		return unique(GET_ITEM_QUERY, MB_CREATOR, POPULATORS,
				new Object[] { subtreeContainer.id, folderContainer.id, item.id });
	}

	@Override
	public List<MailboxRecord> getMultiple(List<Item> items) throws SQLException {
		String query = "select item_id, encode(message_body_guid, 'hex'), " + MailboxRecordColumns.COLUMNS.names()
				+ " FROM t_mailbox_record WHERE subtree_id = ? AND container_id = ? AND item_id = ANY(?::int8[])";
		List<ItemV<MailboxRecord>> values = select(query, items.size(), con -> new ItemV<MailboxRecord>(),
				(ResultSet rs, int index, ItemV<MailboxRecord> itemv) -> {
					itemv.itemId = rs.getLong(index++);
					itemv.value = new MailboxRecord();
					return MailboxRecordColumns.populator().populate(rs, index, itemv.value);
				}, new Object[] { subtreeContainer.id, folderContainer.id,
						items.stream().map(i -> i.id).toArray(Long[]::new) });

		return join(items, values);
	}

	@Override
	public void deleteAll() throws SQLException {
		delete("DELETE FROM t_mailbox_record WHERE subtree_id = ? AND container_id = ?",
				new Object[] { subtreeContainer.id, folderContainer.id });
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
		String query = "" + //
				"SELECT mr.imap_uid, mr.item_id FROM t_mailbox_record mr "
				+ "WHERE mr.subtree_id = ? AND mr.container_id = ? AND mr.imap_uid = ANY(?)";
		return select(query, RecordID.CREATOR, RecordID.POPULATOR,
				new Object[] { subtreeContainer.id, folderContainer.id, uidArrays });
	}

	public List<ImapBinding> bindings(List<Long> itemIds) throws SQLException {
		if (itemIds.isEmpty()) {
			return Collections.emptyList();
		}
		String query = """
				SELECT item_id, imap_uid, encode(message_body_guid, 'hex') FROM t_mailbox_record
				WHERE subtree_id = ? AND container_id = ? AND item_id = ANY (?)
				""";
		List<ImapBinding> notSorted = select(query, rs -> new ImapBinding(), (rs, index, value) -> {
			value.itemId = rs.getInt(index++);
			value.imapUid = rs.getInt(index++);
			value.bodyGuid = rs.getString(index++);
			return index;
		}, new Object[] { subtreeContainer.id, folderContainer.id, itemIds.toArray(Long[]::new) });
		List<ImapBinding> ret = new ArrayList<>(notSorted.size());
		Map<Long, ImapBinding> sortHelper = notSorted.stream().collect(Collectors.toMap(ib -> ib.itemId, ib -> ib));
		itemIds.forEach(k -> Optional.ofNullable(sortHelper.get(k)).ifPresent(ret::add));
		return ret;
	}

	public List<Long> sortedIds(String query) throws SQLException {
		return select(query, LongCreator.FIRST, Collections.emptyList(),
				new Object[] { subtreeContainer.id, folderContainer.id });
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
		sql.append("WHERE mbr.subtree_id = ? AND ci.container_id = ? ");
		sql.append("AND (mb.body_version < ? OR mb.guid IS NULL) ");
		sql.append("AND (ci.flags::bit(32) & (" + ItemFlag.Deleted.value + ")::bit(32)) = 0::bit(32) ");
		sql.append("AND (mbr.system_flags::bit(32) & (" + InternalFlag.expunged.value + ")::bit(32)) = 0::bit(32)");
		return select(sql.toString(), rs -> new ImapBinding(), (rs, index, value) -> {
			value.itemId = rs.getInt(index++);
			value.imapUid = rs.getInt(index++);
			value.bodyGuid = rs.getString(index++);
			return index;
		}, new Object[] { subtreeContainer.id, folderContainer.id, version });
	}

	public List<ImapBinding> recentItems(Date d) throws SQLException {
		String query = "SELECT rec.item_id, rec.imap_uid, encode(rec.message_body_guid, 'hex') FROM t_mailbox_record rec "
				+ "INNER JOIN t_message_body mb ON rec.message_body_guid = mb.guid "//
				+ "WHERE rec.subtree_id = ? AND rec.container_id = ? AND mb.date_header >= ?";

		return select(query, rs -> new ImapBinding(), (rs, index, value) -> {
			value.itemId = rs.getInt(index++);
			value.imapUid = rs.getInt(index++);
			value.bodyGuid = rs.getString(index++);
			return index;
		}, new Object[] { subtreeContainer.id, folderContainer.id, Timestamp.from(d.toInstant()) });
	}

	public List<ImapBinding> unreadItems() throws SQLException {
		// TODO optimize: we don't need item.flags, do we ?
		String query = "SELECT item.id, rec.imap_uid FROM t_mailbox_record rec "
				+ "INNER JOIN t_container_item item ON rec.item_id=item.id " //
				+ "WHERE rec.subtree_id = ? AND item.container_id = ? " //
				+ "AND (item.flags::bit(32) & (" + ItemFlag.Deleted.value + ")::bit(32)) = 0::bit(32) " // not deleted
				+ "AND (item.flags::bit(32) & (" + ItemFlag.Seen.value + ")::bit(32)) = 0::bit(32) " // not seen
				+ "ORDER BY internal_date DESC";

		return select(query, rs -> new ImapBinding(), (rs, index, value) -> {
			value.itemId = rs.getInt(index++);
			value.imapUid = rs.getInt(index++);
			value.bodyGuid = null;
			return index;
		}, new Object[] { subtreeContainer.id, folderContainer.id });
	}

	public Count count(ItemFlagFilter itemFilter) throws SQLException {
		String query = "SELECT count(*) FROM t_mailbox_record rec "
				+ "WHERE rec.subtree_id = ? AND rec.container_id = ? " //
				+ filterSql("rec", itemFilter);
		long count = unique(query, rs -> rs.getLong(1), (rs, index, v) -> index,
				new Object[] { subtreeContainer.id, folderContainer.id });
		return Count.of(count);
	}

	public long weight() throws SQLException {
		// TODO optimize: we don't need item.flags, do we ?

		String query = "SELECT SUM(b.size) FROM t_mailbox_record rec "
				+ "INNER JOIN t_container_item item ON rec.item_id=item.id " //
				+ "INNER JOIN t_message_body b ON rec.message_body_guid=b.guid " //
				+ "WHERE rec.subtree_id = ? AND item.container_id = ? " //
				+ "AND (item.flags::bit(32) & (" + ItemFlag.Deleted.value + ")::bit(32)) = 0::bit(32) ";

		AtomicLong weight = unique(query, con -> new AtomicLong(), (rs, index, value) -> {
			value.set(rs.getLong(1));
			return index;
		}, new Object[] { subtreeContainer.id, folderContainer.id });
		return weight.get();

	}

	public static class MailboxRecordItemV {
		private String containerUid;
		private long itemId;
		private long imapUid;

		private MailboxRecordItemV() {
		}

		public String containerUid() {
			return containerUid;
		}

		public long imapUid() {
			return imapUid;
		}

		public long itemId() {
			return itemId;
		}

	}

	// TODO: this query is a problem: access cross partitions
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
				+ "WHERE mbr.subtree_id = ? AND c.id = ? AND mbr.imap_uid = ? AND c.owner = ? ORDER BY ci.created";

		return unique(query, StringCreator.FIRST, Collections.emptyList(),
				new Object[] { subtreeContainer.id, folderContainer.id, uid, owner });
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

	public List<Long> getItemsByConversations(Long[] conversationIds) throws SQLException {
		String select = """
				SELECT item_id
				FROM t_mailbox_record rec
				WHERE rec.subtree_id = ?
				AND rec.container_id = ?
				AND rec.conversation_id = ANY(?)
				AND system_flags::bit(32) & 4::bit(32) = 0::bit(32)
				""";
		return select(select, LongCreator.FIRST, Collections.emptyList(),
				new Object[] { subtreeContainer.id, folderContainer.id, conversationIds });
	}

	private String filterSql(String recAlias, ItemFlagFilter filter) {
		String fsql = "";
		if (!filter.must.isEmpty()) {
			int v = filter.must.stream().map(this::adaptFlag).reduce(0, (f, flag) -> f | flag);
			fsql += " AND (" + recAlias + ".system_flags::bit(32) & " + v + "::bit(32))=" + v + "::bit(32)";
		}
		if (!filter.mustNot.isEmpty()) {
			int v = filter.mustNot.stream().map(this::adaptFlag).reduce(0, (f, flag) -> f | flag);
			fsql += " AND (" + recAlias + ".system_flags::bit(32) & " + v + "::bit(32))=0::bit(32)";
		}
		return fsql;
	}

	public List<Long> imapIdset(String set, ItemFlagFilter itemFilter) throws SQLException {
		String q = "select rec.item_id from t_mailbox_record rec WHERE rec.subtree_id = ? AND rec.container_id = ? AND "
				+ asSql(set) + filterSql("rec", itemFilter) + " ORDER BY rec.imap_uid";
		return select(q, LongCreator.FIRST, Collections.emptyList(),
				new Object[] { subtreeContainer.id, folderContainer.id });
	}

	public static String asSql(String idset) {
		if (idset.equals("1:*")) {
			return "TRUE";
		}
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

	private static final String SLICE_QUERY = "SELECT item_id, encode(message_body_guid, 'hex'), "
			+ MailboxRecordColumns.COLUMNS.names()
			+ " FROM t_mailbox_record WHERE subtree_id = ? AND container_id = ? AND item_id = ANY(?::int8[])";

	public List<WithId<MailboxRecord>> slice(List<Long> itemIds) throws SQLException {
		EntityPopulator<MailboxRecord> popul = MailboxRecordColumns.populator();
		List<WithId<MailboxRecord>> results = select(SLICE_QUERY, itemIds.size(),
				x -> new WithId<MailboxRecord>(0, new MailboxRecord()),
				(ResultSet rs, int idx, WithId<MailboxRecord> toUpd) -> {
					toUpd.itemId = rs.getInt(idx++);
					return popul.populate(rs, idx, toUpd.value);
				}, new Object[] { subtreeContainer.id, folderContainer.id, itemIds.toArray(Long[]::new) });
		return sort(results, itemIds, r -> r.itemId);
	}

	private static final <T, W> List<W> sort(List<W> items, List<T> selectors, Function<W, T> getSelector) {
		Map<T, W> index = items.stream().collect(
				Collectors.toMap(getSelector, a -> a, (v1, v2) -> v1, () -> new HashMap<>(2 * selectors.size())));
		return selectors.stream().map(index::get).filter(Objects::nonNull).toList();
	}
}
