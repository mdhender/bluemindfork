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
package net.bluemind.mailbox.persistence;

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

import com.google.common.base.Splitter;

import net.bluemind.core.api.Email;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.persistence.AbstractItemValueStore;
import net.bluemind.core.container.persistence.BooleanCreator;
import net.bluemind.core.container.persistence.StringCreator;
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

	private static final String DELETE_MAILBOX_QUERY = "DELETE FROM t_mailbox WHERE item_id = ?";

	@Override
	public void delete(Item item) throws SQLException {
		deleteEmails(item);
		delete(DELETE_MAILBOX_QUERY, new Object[] { item.id });

	}

	private static final String MBOX_GET_QUERY = "SELECT " //
			+ MailboxColumns.cols.names() //
			+ ", t_directory_entry.datalocation "//
			+ ", la,ra,all_aliases,is_def " //
			+ " FROM t_mailbox " //
			+ " left join t_directory_entry on t_directory_entry.item_id = t_mailbox.item_id " + " left outer join (" //
			+ "   SELECT item_id," //
			+ "   array_agg(" + EmailColumns.left_address.name() + ") la, "//
			+ "   array_agg(" + EmailColumns.right_address.name() + ") ra, "//
			+ "   array_agg(" + EmailColumns.all_aliases.name() + ") all_aliases, "//
			+ "   array_agg(" + EmailColumns.is_default.name() + ") is_def"//
			+ "   FROM t_mailbox_email group by t_mailbox_email.item_id) as emails " //
			+ " on emails.item_id = t_mailbox.item_id " //
			+ " WHERE t_mailbox.item_id = ?";

	@Override
	public Mailbox get(Item item) throws SQLException {
		Mailbox m = unique(MBOX_GET_QUERY, MAILBOX_CREATOR,
				Arrays.asList(MailboxColumns.populator(), EmailColumns.aggPopulator(container.domainUid)),
				new Object[] { item.id });
		if (m == null)
			return null;

		return m;
	}

	private static final String DELETE_EMAILS_QUERY = "DELETE FROM t_mailbox_email WHERE item_id = ?";

	private void deleteEmails(Item item) throws SQLException {
		delete(DELETE_EMAILS_QUERY, new Object[] { item.id });
	}

	private void setEmails(Item item, Collection<Email> emails) throws SQLException {
		deleteEmails(item);

		if (emails == null || emails.isEmpty()) {
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

	private static final String EMAIL_SEARCH_QUERY = "SELECT item.uid " //
			+ " FROM t_mailbox_email e" //
			+ " JOIN t_container_item item ON (item.id = e.item_id)" //
			+ " WHERE container_id = ?" //
			+ " AND (" //
			+ "   e.left_address || '@' || e.right_address = ?" //
			+ "   OR" //
			+ "   (e.all_aliases = true AND e.left_address = ?::text)" //
			+ " ) LIMIT 1";

	private static final String EMAIL_ALIAS_SEARCH_QUERY = "SELECT TRUE " //
			+ " FROM t_mailbox_email e" //
			+ " JOIN t_container_item item ON (item.id = e.item_id)" //
			+ " WHERE container_id = ?" //
			+ " AND (e.right_address = ?) LIMIT 1";

	/**
	 * @param q
	 * @return
	 * @throws SQLException
	 */
	public String emailSearch(String email) throws SQLException {
		String leftPart = email.split("@")[0];
		return unique(EMAIL_SEARCH_QUERY, StringCreator.FIRST, Collections.emptyList(),
				new Object[] { container.id, email, leftPart });
	}

	public boolean isUsedAlias(String alias) throws SQLException {
		Boolean unique = unique(EMAIL_ALIAS_SEARCH_QUERY, BooleanCreator.FIRST, Collections.emptyList(),
				new Object[] { container.id, alias });
		return unique != null;
	}

	/**
	 * @param q
	 * @return
	 * @throws SQLException
	 */
	private static final String EMAIL_TYPE_SEARCH_QUERY = "SELECT item.uid" //
			+ " FROM t_mailbox m" //
			+ " INNER JOIN t_container_item item ON m.item_id = item.id" //
			+ " WHERE m.type = ?::enum_mailbox_type" //
			+ " AND item.container_id = ?";

	public List<String> typeSearch(Type type) throws SQLException {
		return select(EMAIL_TYPE_SEARCH_QUERY, StringCreator.FIRST, Collections.emptyList(),
				new Object[] { type.name(), container.id });
	}

	/**
	 * @param q
	 * @return
	 * @throws SQLException
	 */
	private static final String NAME_SEARCH_QUERY = "SELECT item.uid" //
			+ " FROM t_mailbox e" //
			+ " INNER JOIN t_container_item item ON e.item_id = item.id" //
			+ " WHERE e.name = ?" //
			+ " AND item.container_id = ?";

	public String nameSearch(String name) throws SQLException {
		return unique(NAME_SEARCH_QUERY, StringCreator.FIRST, Collections.emptyList(),
				new Object[] { name, container.id });
	}

	public boolean nameAlreadyUsed(Long itemId, Mailbox mailbox) throws SQLException {
		if (mailbox == null) {
			return false;
		}

		String query = "SELECT 1 FROM t_mailbox ";
		query += "INNER JOIN t_container_item ON t_container_item.id = t_mailbox.item_id ";
		query += "WHERE container_id = ? AND name = ?";

		Object[] parameters = new Object[] { container.id, mailbox.name.toLowerCase() };
		if (itemId != null) {
			query += " AND item_id != ?";
			parameters = new Object[] { container.id, mailbox.name, itemId };
		}
		query = "SELECT EXISTS (" + query + ")";

		return unique(query, BooleanCreator.FIRST, Collections.emptyList(), parameters);
	}

	private static final Splitter AT_SPLITTER = Splitter.on('@');

	public boolean emailAlreadyUsed(Long itemId, Collection<Email> emails) throws SQLException {

		if (emails == null || emails.isEmpty()) {
			return false;
		}

		ArrayList<String> address = new ArrayList<>(emails.size());
		Iterator<Email> it = emails.iterator();
		while (it.hasNext()) {
			address.add(it.next().address);
		}

		String query = "SELECT 1 FROM t_mailbox_email INNER JOIN t_container_item "
				+ " ON t_container_item.id = t_mailbox_email.item_id " //
				+ " , ( SELECT ?::text[] as left, ?::text[] as full, ?::text[] as leftall ) as param " + //
				" WHERE container_id = ? AND ( " //
				+ "    ( all_aliases = true and left_address= ANY (param.left)) OR "
				+ "    ( all_aliases = false and ( left_address || '@' || right_address = ANY (param.full) OR left_address = ANY(leftall) )) "
				+ ")";

		String[] left = emails.stream().map(e -> AT_SPLITTER.split(e.address).iterator().next()).toArray(String[]::new);

		String[] leftAll = emails.stream().filter(e -> e.allAliases)
				.map(e -> AT_SPLITTER.split(e.address).iterator().next()).toArray(String[]::new);

		String[] all = emails.stream().map(e -> e.address).toArray(String[]::new);

		Object[] parameters = null;

		if (itemId != null) {
			query += " AND item_id != ?";
			parameters = new Object[] { left, all, leftAll, container.id, itemId };
		} else {
			parameters = new Object[] { left, all, leftAll, container.id };
		}
		query = "SELECT EXISTS (" + query + ")";

		return unique(query, BooleanCreator.FIRST, Collections.emptyList(), parameters);

	}

	public boolean emailAlreadyUsed(Collection<Email> emails) throws SQLException {
		return emailAlreadyUsed(null, emails);
	}

	private static final String LIST_QUOTA_QUERY = "SELECT item.uid FROM t_mailbox "
			+ " INNER JOIN t_container_item item ON item_id = item.id WHERE container_id = ? AND quota > 0";

	public List<String> listQuota() throws SQLException {
		return select(LIST_QUOTA_QUERY, StringCreator.FIRST, Collections.emptyList(), new Object[] { container.id });
	}

	private static final String ROUTING_SEARCH_QUERY = "SELECT item.uid" //
			+ " FROM t_mailbox m" //
			+ " INNER JOIN t_container_item item ON m.item_id = item.id" //
			+ " WHERE m.routing = ?::enum_mailbox_routing" //
			+ " AND item.container_id = ?";

	public List<String> routingSearch(Routing routing) throws SQLException {
		return select(ROUTING_SEARCH_QUERY, StringCreator.FIRST, Collections.emptyList(),
				new Object[] { routing.name(), container.id });
	}

	private static final String DELETE_BY_ALIAS_QUERY = "DELETE FROM t_mailbox_email WHERE right_address = ?";

	public void deleteEmailByAlias(String alias) throws SQLException {
		delete(DELETE_BY_ALIAS_QUERY, new Object[] { alias });
	}

	private static final String ALLUIDS_QUERY = "SELECT i.uid FROM t_container_item i JOIN t_mailbox m ON m.item_id = i.id WHERE i.container_id = ?";

	public List<String> allUids() throws SQLException {
		return select(ALLUIDS_QUERY, StringCreator.FIRST, Collections.emptyList(), new Object[] { container.id });
	}

	private static final String IS_QUOTA_GREATER_QUERY = "SELECT EXISTS(" //
			+ " SELECT m.item_id FROM t_mailbox m " //
			+ " INNER JOIN t_container_item item ON m.item_id = item.id " //
			+ " WHERE m.quota > ? AND item.container_id = ?)";

	public Boolean isQuotaGreater(int quotaMax) throws SQLException {
		return unique(IS_QUOTA_GREATER_QUERY, BooleanCreator.FIRST, Collections.emptyList(),
				new Object[] { quotaMax, container.id });
	}

}
