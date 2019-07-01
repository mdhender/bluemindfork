/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2018
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
package net.bluemind.backend.postfix.internal.maps;

import java.security.InvalidParameterException;
import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.jdbc.JdbcHelper;
import net.bluemind.core.rest.BmContext;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.api.DomainSettingsHelper;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.mailbox.api.Mailbox.Routing;
import net.bluemind.mailbox.api.Mailbox.Type;
import net.bluemind.server.api.Server;

public class MapRow {
	private static final Logger logger = LoggerFactory.getLogger(MapRow.class);
	private static final String EXTERNALUSER_DATALOCATION = "EXTERNALUSER-" + UUID.randomUUID().toString();

	public final int itemId;

	public final ItemValue<Domain> domain;
	public final Type type;
	public final Routing routing;
	public final String dataLocation;
	private final Integer[] membersItemsIds;
	public final Set<String> emails = new HashSet<>();
	private String recipients;
	private String mailboxName;
	private String defaultEmail;

	public MapRow(ItemValue<Domain> domain, int itemId, String name, Type type, Routing routing, String dataLocation,
			String mailboxName, Integer[] membersItemsIds) {
		this.domain = domain;
		this.itemId = itemId;
		this.type = type;
		this.routing = routing;
		this.dataLocation = dataLocation;
		this.membersItemsIds = membersItemsIds;
		this.mailboxName = mailboxName;
	}

	/**
	 * External user constructor
	 * 
	 * @param itemId
	 * @param externalEmail
	 */
	public MapRow(int itemId, String externalEmail) {
		this.domain = null;
		this.itemId = itemId;
		this.type = null;
		this.routing = Routing.none;
		this.dataLocation = null;
		this.membersItemsIds = null;
		this.mailboxName = externalEmail;
		this.recipients = null;
	}

	public static List<MapRow> build(BmContext context, List<ItemValue<Server>> servers,
			Map<String, DomainInfo> domainInfoByUid) throws SQLException {
		String fields = "tm.item_id AS item_id, tm.name AS name, tm.type AS type, tm.routing AS routing, tme.left_address AS left_address, tme.right_address AS right_address, tme.all_aliases AS all_aliases, tme.is_default AS is_default, tc.domain_uid AS domain_uid, tde.datalocation AS datalocation";
		String mapRowQuery = "SELECT " + fields + ", null AS members" //
				+ " FROM t_mailbox tm" //
				+ " INNER JOIN t_mailbox_email tme ON tm.item_id=tme.item_id" //
				+ " INNER JOIN t_container_item tci ON tm.item_id=tci.id" //
				+ " INNER JOIN t_container tc ON tc.id=tci.container_id" //
				+ " INNER JOIN t_directory_entry tde ON tm.item_id=tde.item_id" //
				+ " WHERE NOT tm.archived  AND tm.type != '" + Mailbox.Type.group.name() + "'::enum_mailbox_type" //
				+ " UNION" //
				+ " SELECT " + fields + ", array_agg(member) AS members" //
				+ " FROM t_mailbox tm" //
				+ " INNER JOIN t_mailbox_email tme ON tm.item_id=tme.item_id" //
				+ " INNER JOIN t_container_item tci ON tm.item_id=tci.id" //
				+ " INNER JOIN t_container tc ON tc.id=tci.container_id" //
				+ " INNER JOIN t_directory_entry tde ON tm.item_id=tde.item_id" //
				+ " INNER JOIN (SELECT group_id AS g, user_id AS member FROM t_group_usermember UNION SELECT group_id AS g, external_user_id AS member FROM t_group_externalusermember UNION SELECT group_parent_id AS g, group_child_id AS member FROM t_group_groupmember) AS mt ON g=tm.item_id" //
				+ " WHERE NOT tm.archived AND tm.type = '" + Mailbox.Type.group.name() + "'::enum_mailbox_type" //
				+ " GROUP BY tm.item_id, tm.name, tm.type, tm.routing, tme.left_address, tme.right_address, tme.all_aliases, tme.is_default, tc.domain_uid, tde.datalocation" //
				+ " UNION" //
				+ " SELECT " + fields + ", NULL AS members" //
				+ " FROM t_mailbox tm" //
				+ " INNER JOIN t_mailbox_email tme ON tm.item_id=tme.item_id" //
				+ " INNER JOIN t_container_item tci ON tm.item_id=tci.id" //
				+ " INNER JOIN t_container tc on tc.id=tci.container_id" //
				+ " INNER JOIN t_directory_entry tde ON tm.item_id=tde.item_id" //
				+ " LEFT JOIN (SELECT group_id AS g, user_id AS member FROM t_group_usermember UNION SELECT group_id AS g, external_user_id AS member FROM t_group_externalusermember UNION SELECT group_parent_id AS g, group_child_id AS member FROM t_group_groupmember) AS mt ON g=tm.item_id"
				+ " WHERE NOT tm.archived AND tm.type = '" + Mailbox.Type.group.name()
				+ "'::enum_mailbox_type AND routing = '" + Mailbox.Routing.internal.name()
				+ "'::enum_mailbox_routing AND member IS NULL"
				+ " GROUP BY tm.item_id, tm.name, tm.type, tm.routing, tme.left_address, tme.right_address, tme.all_aliases, tme.is_default, tc.domain_uid, tde.datalocation"
				+ " UNION"
				+ " SELECT tde.item_id AS item_id, tde.displayname AS name, 'user'::enum_mailbox_type AS type, 'none'::enum_mailbox_routing AS routing, tde.email AS left_address, NULL AS right_address, false AS all_aliases, false AS is_default, NULL AS domain_uid, '"
				+ EXTERNALUSER_DATALOCATION + "' AS datalocation, NULL AS members" //
				+ " FROM t_directory_entry tde WHERE tde.kind = 'EXTERNALUSER' AND NOT tde.flag_archived" //
				+ " GROUP BY tde.item_id, tde.displayname, type, routing, tde.email, right_address, all_aliases, is_default, domain_uid, datalocation";

		Map<Integer, MapRow> rowsByItemId = new HashMap<>();

		Connection conn = context.getDataSource().getConnection();
		ResultSet rs = null;
		PreparedStatement st = null;
		try {
			st = conn.prepareStatement(mapRowQuery);
			rs = st.executeQuery();

			while (rs.next()) {
				int itemId = rs.getInt("item_id");

				if (rowsByItemId.containsKey(itemId)) {
					rowsByItemId.get(itemId).addEmail(rs.getString("left_address"), rs.getString("right_address"),
							rs.getBoolean("all_aliases"), rs.getBoolean("is_default"));
				} else {
					MapRow row = build(servers, domainInfoByUid, rs);
					if (row != null) {
						rowsByItemId.put(row.itemId, row);
					}
				}
			}
		} finally {
			JdbcHelper.cleanup(conn, rs, st);
		}

		rowsByItemId.values().forEach(r -> r.expandRecipients(rowsByItemId));

		return rowsByItemId.values().stream().filter(r -> r.recipients != null).collect(Collectors.toList());
	}

	private static MapRow build(List<ItemValue<Server>> servers, Map<String, DomainInfo> domainInfoByUid, ResultSet rs)
			throws SQLException {
		String dataLocationUid = rs.getString("datalocation");
		if (EXTERNALUSER_DATALOCATION.equals(dataLocationUid)) {
			// External user
			return new MapRow(rs.getInt("item_id"), rs.getString("left_address"));
		}

		int itemId = rs.getInt("item_id");
		String name = rs.getString("name");
		Type type = Mailbox.Type.valueOf(rs.getString("type"));
		Routing routing = Mailbox.Routing.valueOf(rs.getString("routing"));

		String emailLeft = rs.getString("left_address");
		String emailRight = rs.getString("right_address");
		boolean allAliases = rs.getBoolean("all_aliases");
		boolean isDefault = rs.getBoolean("is_default");

		String domainUid = rs.getString("domain_uid");

		Integer[] membersItemsIds = new Integer[0];
		Array array = rs.getArray("members");
		if (array != null) {
			membersItemsIds = (Integer[]) array.getArray();
		}

		if (!domainInfoByUid.containsKey(domainUid)) {
			logger.warn(String.format("Unknown domain '%s' for item ID %d", domainUid, itemId));
			return null;
		}

		DomainInfo domainInfo = domainInfoByUid.get(domainUid);

		String dataLocationIp = null;
		if (routing == Routing.internal) {
			if (dataLocationUid == null) {
				logger.warn(String.format("datalocation is null for item ID %d, domain '%s'", itemId, domainUid));
				return null;
			}

			Optional<ItemValue<Server>> server = servers.stream().filter(s -> dataLocationUid.equals(s.uid))
					.findFirst();
			if (!server.isPresent()) {
				throw new InvalidParameterException("Server uid: " + dataLocationUid + " not found!");
			}

			dataLocationIp = server.get().value.address();
		} else if (routing == Routing.external) {
			dataLocationIp = DomainSettingsHelper.getSlaveRelayHost(domainInfo.domainSettings);

			if (dataLocationIp == null || dataLocationIp.isEmpty()) {
				throw new InvalidParameterException("No relay host defined for domain uid: " + domainInfo.domain.uid);
			}
		}

		String mailboxName = null;
		if (routing != Mailbox.Routing.none) {
			mailboxName = name + "@" + domainInfo.domain.value.name;
			if (type != Mailbox.Type.user) {
				mailboxName = "+" + mailboxName;
			}
		}

		MapRow mp = new MapRow(domainInfo.domain, itemId, name, type, routing, dataLocationIp, mailboxName,
				membersItemsIds);
		mp.addEmail(emailLeft, emailRight, allAliases, isDefault);

		return mp;
	}

	public void addEmail(String emailLeft, String emailRight, boolean allAliases, boolean isDefault) {
		if (isDefault) {
			defaultEmail = emailLeft + "@" + emailRight;
		}

		if (!allAliases) {
			emails.add(emailLeft + "@" + emailRight);
		} else {
			emails.add(emailLeft + "@" + domain.value.name);
			domain.value.aliases.forEach(da -> emails.add(emailLeft + "@" + da));
		}
	}

	public void expandRecipients(Map<Integer, MapRow> emailsByItemId) {
		if (type == null) {
			return;
		}

		if (type != Mailbox.Type.group) {
			if (routing == Routing.external && !Strings.isNullOrEmpty(defaultEmail)) {
				recipients = defaultEmail;
			} else {
				recipients = mailboxName;
			}
			return;
		}

		Set<String> r = new HashSet<>();

		if (routing != Mailbox.Routing.none) {
			r.add(mailboxName);
		}

		for (Integer memberItemId : membersItemsIds) {
			if (memberItemId == null) {
				continue;
			}

			MapRow row = emailsByItemId.get(memberItemId);
			if (row == null) {
				continue;
			}

			String rowAsRecipient = row.asMember();
			if (rowAsRecipient != null) {
				r.add(rowAsRecipient);
			}
		}

		recipients = String.join(",", r);
		if (recipients.isEmpty()) {
			recipients = null;
		}
	}

	private String asMember() {
		if (type == Mailbox.Type.user && routing == Mailbox.Routing.none && !Strings.isNullOrEmpty(defaultEmail)) {
			return defaultEmail;
		}

		if (type != Mailbox.Type.group) {
			return mailboxName;
		}

		if (emails.size() == 0) {
			return null;
		}

		return emails.iterator().next();
	}

	public String getRecipients() {
		return recipients;
	}

	public String getMailboxName() {
		if (routing == Routing.external && !Strings.isNullOrEmpty(defaultEmail)) {
			return defaultEmail;
		}

		return mailboxName;
	}
}
