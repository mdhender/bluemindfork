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
package net.bluemind.directory.persistance.internal;

import java.sql.Types;

import net.bluemind.core.container.model.Item;
import net.bluemind.core.jdbc.Columns;
import net.bluemind.directory.api.BaseDirEntry;
import net.bluemind.directory.api.BaseDirEntry.Kind;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.persistance.DirEntryStore;

public class DirEntryColumns {

	public static final Columns COLUMNS_MAIN = Columns.create() //
			.col("kind")//
			.col("account_type") //
			.col("entry_uid") //
			.col("displayname") //
			.col("email") //
			.col("flag_hidden") //
			.col("flag_system") //
			.col("flag_archived") //
			.col("datalocation") //
			.col("orgunit_item_id");

	public static final Columns UPD_COLUMNS_MAIN = Columns.create() //
			.col("kind")//
			.col("entry_uid") //
			.col("displayname") //
			.col("email") //
			.col("flag_hidden") //
			.col("flag_system") //
			.col("flag_archived") //
			.col("datalocation") //
			.col("orgunit_item_id");

	public static DirEntryStore.StatementValues<DirEntry> values(final Item item) {
		return (conn, statement, index, currentRow, value) -> {
			statement.setString(index++, value.kind.name());

			if (value.accountType != null) {
				statement.setString(index++, value.accountType.name());
			} else {
				statement.setNull(index++, Types.VARCHAR);
			}

			statement.setString(index++, value.entryUid);
			statement.setString(index++, value.displayName);
			statement.setString(index++, value.email);
			statement.setBoolean(index++, value.hidden);
			statement.setBoolean(index++, value.system);
			statement.setBoolean(index++, value.archived);
			statement.setString(index++, value.dataLocation);
			statement.setString(index++, value.orgUnitUid);
			statement.setLong(index++, item.id);
			return index;

		};

	}

	public static DirEntryStore.StatementValues<DirEntry> updValues(final Item item) {
		return (conn, statement, index, currentRow, value) -> {
			statement.setString(index++, value.kind.name());
			statement.setString(index++, value.entryUid);
			statement.setString(index++, value.displayName);
			statement.setString(index++, value.email);
			statement.setBoolean(index++, value.hidden);
			statement.setBoolean(index++, value.system);
			statement.setBoolean(index++, value.archived);
			statement.setString(index++, value.dataLocation);
			statement.setString(index++, value.orgUnitUid);
			statement.setLong(index++, item.id);
			return index;

		};

	}

	public static DirEntryStore.EntityPopulator<DirEntry> populator(String domainUid) {
		return (rs, index, value) -> {
			String kindAsString = rs.getString(index++);
			String at = rs.getString(index++);
			if (at != null && !at.isEmpty()) {
				value.accountType = DirEntry.AccountType.valueOf(at);
			}

			value.entryUid = rs.getString(index++);
			value.displayName = rs.getString(index++);
			value.email = rs.getString(index++);
			value.hidden = rs.getBoolean(index++);
			value.system = rs.getBoolean(index++);
			value.archived = rs.getBoolean(index++);
			value.dataLocation = rs.getString(index++);

			value.kind = DirEntry.Kind.valueOf(kindAsString);
			switch (value.kind) {
			case USER:
				value.path = domainUid + "/users/" + value.entryUid;
				break;
			case GROUP:
				value.path = domainUid + "/groups/" + value.entryUid;
				break;
			case RESOURCE:
				value.path = domainUid + "/resources/" + value.entryUid;
				break;
			case MAILSHARE:
				value.path = domainUid + "/mailshares/" + value.entryUid;
				break;
			case ADDRESSBOOK:
				value.path = domainUid + "/addressbooks/" + value.entryUid;
				break;
			case CALENDAR:
				value.path = domainUid + "/calendars/" + value.entryUid;
				break;
			case ORG_UNIT:
				value.path = domainUid + "/ous/" + value.entryUid;
				break;
			case DOMAIN:
				value.path = domainUid;
			default:
				break;
			}

			// skip orgunit_item_id
			index++;
			value.orgUnitUid = rs.getString(index++);
			return index;
		};

	}

	public static DirEntryStore.EntityPopulator<BaseDirEntry> baseDirEntryPopulator() {
		return (rs, index, value) -> {
			value.entryUid = rs.getString(index++);
			value.displayName = rs.getString(index++);

			String accountType = rs.getString(index++);
			if (accountType != null && !accountType.isEmpty()) {
				value.accountType = DirEntry.AccountType.valueOf(accountType);
			}
			value.kind = Kind.valueOf(rs.getString(index++));

			return index;
		};
	}

}
