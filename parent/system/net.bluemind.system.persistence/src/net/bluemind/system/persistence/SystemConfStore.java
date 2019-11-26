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
package net.bluemind.system.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import net.bluemind.core.jdbc.JdbcAbstractStore;

public class SystemConfStore extends JdbcAbstractStore {

	private static final Creator<Map<String, String>> CREATOR = new Creator<Map<String, String>>() {
		@Override
		public Map<String, String> create(ResultSet con) throws SQLException {
			return new HashMap<String, String>();
		}
	};

	public SystemConfStore(DataSource pool) {
		super(pool);
	}

	public void update(Map<String, String> values) throws SQLException {
		insert("UPDATE t_systemconf set configuration = ?", values, SystemConfColumns.statementValues());
	}

	public Map<String, String> get() throws SQLException {
		Map<String, String> settings = unique("SELECT configuration FROM t_systemconf", CREATOR,
				SystemConfColumns.populator());

		return settings;
	}

	private static class BmInfoEntry {

		public String name;
		public String value;

	}

	public Map<String, String> get30() throws SQLException {

		List<BmInfoEntry> values = select("SELECT name,value from bminfo", new Creator<BmInfoEntry>() {

			@Override
			public BmInfoEntry create(ResultSet con) throws SQLException {
				return new BmInfoEntry();
			}
		}, new EntityPopulator<BmInfoEntry>() {

			@Override
			public int populate(ResultSet rs, int index, BmInfoEntry value) throws SQLException {
				value.name = rs.getString(1);
				value.value = rs.getString(2);
				return 0;
			}
		}

		);

		Map<String, String> settings = new HashMap<>();
		for (BmInfoEntry value : values) {
			settings.put(value.name, value.value);
		}

		return settings;
	}

}
