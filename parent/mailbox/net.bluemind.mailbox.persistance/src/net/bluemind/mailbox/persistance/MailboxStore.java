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
package net.bluemind.mailbox.persistance;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.api.Email;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.persistance.AbstractItemValueStore;
import net.bluemind.core.container.persistance.StringCreator;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.mailbox.api.Mailbox.Routing;
import net.bluemind.mailbox.api.Mailbox.Type;

public class MailboxStore extends AbstractItemValueStore<Mailbox> {

	private static final Logger logger = LoggerFactory.getLogger(MailboxStore.class);

	private final Container container;

	private static final Creator<Mailbox> MAILBOX_CREATOR = new Creator<Mailbox>() {
		@Override
		public Mailbox create(ResultSet con) throws SQLException {
			return new Mailbox();
		}
	};

	private static final Creator<Integer> INTEGER_CREATOR = new Creator<Integer>() {
		@Override
		public Integer create(ResultSet con) throws SQLException {
			return con.getInt(1);
		}
	};

	public MailboxStore(DataSource pool, Container container) {
		super(pool);
		this.container = container;
		logger.debug("created {}", this.container);
	}

	@Override
	public void create(Item item, Mailbox value) throws SQLException {
		String query = //
				"INSERT INTO t_mailbox " //
						+ " (" + MailboxColumns.cols.names() + ",item_id) " //
						+ " VALUES " //
						+ "( " + MailboxColumns.cols.values() + ", ? )";

		insert(query, value, MailboxColumns.statementValues(item.id));

		setEmails(item, value.emails);

	}

	@Override
	public void update(Item item, Mailbox value) throws SQLException {
		String query = //
				"UPDATE t_mailbox SET ( " //
						+ MailboxColumns.cols.names() //
						+ ") = (" //
						+ MailboxColumns.cols.values() //
						+ ")  WHERE item_id = ? ";

		update(query, value, MailboxColumns.statementValues(item.id));

		setEmails(item, value.emails);
	}

	@Override
	public void delete(Item item) throws SQLException {

		deleteEmails(item);
		delete("DELETE FROM t_mailbox WHERE item_id = ?", new Object[] { item.id });

	}

	@Override
	public Mailbox get(Item item) throws SQLException {
		String query = "SELECT " //
				+ MailboxColumns.cols.names() //
				+ ", t_directory_entry.datalocation "//
				+ ", la,ra,all_aliases,is_def " //
				+ " FROM t_mailbox " //
				+ " left join t_directory_entry on t_directory_entry.item_id = t_mailbox.item_id "
				+ " left outer join (" //
				+ "   SELECT item_id," //
				+ "   array_agg(" + EmailColumns.left_address.name() + ") la, "//
				+ "   array_agg(" + EmailColumns.right_address.name() + ") ra, "//
				+ "   array_agg(" + EmailColumns.all_aliases.name() + ") all_aliases, "//
				+ "   array_agg(" + EmailColumns.is_default.name() + ") is_def"//
				+ "   FROM t_mailbox_email group by t_mailbox_email.item_id) as emails " //
				+ " on emails.item_id = t_mailbox.item_id " //
				+ " WHERE t_mailbox.item_id = ?";

		Mailbox m = unique(query, MAILBOX_CREATOR,
				Arrays.asList(MailboxColumns.populator(), EmailColumns.aggPopulator(container.domainUid)),
				new Object[] { item.id });
		if (m == null)
			return null;

		return m;
	}

	private void deleteEmails(Item item) throws SQLException {
		delete("DELETE FROM t_mailbox_email WHERE item_id = ?", new Object[] { item.id });
	}

	private void setEmails(Item item, Collection<Email> emails) throws SQLException {
		deleteEmails(item);

		if (emails == null || emails.size() == 0) {
			return;
		}

		StringBuilder query = new StringBuilder("INSERT INTO t_mailbox_email (item_id,");
		EmailColumns.appendNames(null, query);
		query.append(") VALUES (?, ");
		EmailColumns.appendValues(query);
		query.append(")");
		batchInsert(query.toString(), emails, EmailColumns.statementValues(item.id));
	}

	@Override
	public void deleteAll() throws SQLException {
		delete("DELETE FROM t_mailbox WHERE item_id IN (SELECT id FROM t_container_item WHERE container_id = ?)",
				new Object[] { container.id });
		// FIXME delete emails ?!
	}

	/**
	 * @param q
	 * @return
	 * @throws SQLException
	 */
	public String emailSearch(String email) throws SQLException {
		String leftPart = email.split("@")[0];
		String query = "SELECT DISTINCT item.uid" //
				+ " FROM t_container_item item, t_mailbox_email e " + " WHERE e.item_id = item.id AND " //
				+ " ( " //
				+ "    ( e.left_address || '@' || e.right_address = ? OR ( e.all_aliases = true AND e.left_address= ?::text))" //
				+ "    OR lower(item.uid) = lower(?) " //
				+ "  )" //
				+ " AND item.container_id = ?";

		String mailboxUid = unique(query, StringCreator.FIRST, Collections.<EntityPopulator<String>>emptyList(),
				new Object[] { email, leftPart, leftPart, container.id });

		return mailboxUid;
	}

	/**
	 * @param q
	 * @return
	 * @throws SQLException
	 */
	public List<String> typeSearch(Type type) throws SQLException {

		String query = "SELECT item.uid" //
				+ " FROM t_mailbox m" //
				+ " INNER JOIN t_container_item item ON m.item_id = item.id" //
				+ " WHERE m.type = ?::enum_mailbox_type" //
				+ " AND item.container_id = ?";

		List<String> mailboxUids = select(query, StringCreator.FIRST, Collections.<EntityPopulator<String>>emptyList(),
				new Object[] { type.name(), container.id });

		return mailboxUids;
	}

	/**
	 * @param q
	 * @return
	 * @throws SQLException
	 */
	public String nameSearch(String name) throws SQLException {

		String query = "SELECT item.uid" //
				+ " FROM t_mailbox e" //
				+ " INNER JOIN t_container_item item ON e.item_id = item.id" //
				+ " WHERE e.name = ?" //
				+ " AND item.container_id = ?";

		String mailboxUid = unique(query, StringCreator.FIRST, Collections.<EntityPopulator<String>>emptyList(),
				new Object[] { name, container.id });

		return mailboxUid;
	}

	public boolean nameAlreadyUsed(Long itemId, Mailbox mailbox) throws SQLException {
		if (mailbox == null) {
			return false;
		}

		String query = "SELECT count(*) FROM t_mailbox ";
		query += "INNER JOIN t_container_item ON t_container_item.id = t_mailbox.item_id ";
		query += "WHERE container_id = ? AND name = ?";

		Object[] parameters = new Object[] { container.id, mailbox.name.toLowerCase() };
		if (itemId != null) {
			query += " AND item_id != ?";
			parameters = new Object[] { container.id, mailbox.name, itemId };
		}

		Integer total = unique(query, INTEGER_CREATOR, new ArrayList<EntityPopulator<Integer>>(0), parameters);

		if (total != 0) {
			return true;
		}

		return false;
	}

	public boolean emailAlreadyUsed(Long itemId, Collection<Email> emails) throws SQLException {

		if (emails == null || emails.isEmpty()) {
			return false;
		}

		ArrayList<String> address = new ArrayList<String>(emails.size());
		Iterator<Email> it = emails.iterator();
		while (it.hasNext()) {
			address.add(it.next().address);
		}

		String query = "" + "SELECT count(*) FROM t_mailbox_email INNER JOIN t_container_item "
				+ " ON t_container_item.id = t_mailbox_email.item_id " //
				+ " , ( SELECT ?::text[] as left, ?::text[] as full, ?::text[] as leftall ) as param " + //
				" WHERE container_id = ? AND ( " //
				+ "    ( all_aliases = true and left_address= ANY (param.left)) OR "
				+ "    ( all_aliases = false and ( left_address || '@' || right_address = ANY (param.full) OR left_address = ANY(leftall) )) "
				+ ")";

		String[] left = emails.stream().map(e -> {
			return e.address.split("@")[0];
		}).toArray(String[]::new);

		String[] leftAll = emails.stream().filter(e -> {
			return e.allAliases;
		}).map(e -> {
			return e.address.split("@")[0];
		}).toArray(String[]::new);

		String[] all = emails.stream().map(e -> {
			return e.address;
		}).toArray(String[]::new);

		Object[] parameters = null;

		if (itemId != null) {
			query += " AND item_id != ?";
			parameters = new Object[] { left, all, leftAll, container.id, itemId };
		} else {
			parameters = new Object[] { left, all, leftAll, container.id };
		}

		Integer total = unique(query.toString(), INTEGER_CREATOR, new ArrayList<EntityPopulator<Integer>>(0),
				parameters);

		if (total != 0) {
			return true;
		}

		return false;

	}

	public boolean emailAlreadyUsed(Collection<Email> emails) throws SQLException {
		return emailAlreadyUsed(null, emails);
	}

	public List<String> listQuota() throws SQLException {
		String query = "SELECT item.uid FROM t_mailbox "
				+ " INNER JOIN t_container_item item ON item_id = item.id WHERE container_id = ? AND quota > 0";

		List<String> uids = select(query, StringCreator.FIRST, Collections.<EntityPopulator<String>>emptyList(),
				new Object[] { container.id });

		return uids;
	}

	public List<String> routingSearch(Routing routing) throws SQLException {
		String query = "SELECT item.uid" //
				+ " FROM t_mailbox m" //
				+ " INNER JOIN t_container_item item ON m.item_id = item.id" //
				+ " WHERE m.routing = ?::enum_mailbox_routing" //
				+ " AND item.container_id = ?";

		List<String> mailboxUids = select(query, StringCreator.FIRST, Collections.<EntityPopulator<String>>emptyList(),
				new Object[] { routing.name(), container.id });

		return mailboxUids;
	}

	public void deleteEmailByAlias(String alias) throws SQLException {
		String query = "DELETE FROM t_mailbox_email WHERE right_address = ?";
		delete(query, new Object[] { alias });
	}

	public List<String> allUids() throws SQLException {
		final String query = "SELECT i.uid FROM t_container_item i JOIN t_mailbox m ON m.item_id = i.id WHERE i.container_id = ?";

		return select(query.toString(), StringCreator.FIRST, Collections.<EntityPopulator<String>>emptyList(),
				new Object[] { container.id });
	}

}
