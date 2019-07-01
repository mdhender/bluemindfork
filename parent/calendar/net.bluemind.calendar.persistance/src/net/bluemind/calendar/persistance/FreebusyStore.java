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
package net.bluemind.calendar.persistance;

import java.sql.Array;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.sql.DataSource;

import net.bluemind.core.container.model.Container;
import net.bluemind.core.jdbc.JdbcAbstractStore;

public class FreebusyStore extends JdbcAbstractStore {

	private Container container;

	public FreebusyStore(DataSource dataSource, Container container) {
		super(dataSource);
		this.container = container;
	}

	public List<String> get() throws SQLException {
		String query = "SELECT calendars from t_freebusy where container_id = ?";
		List<String> unique = unique(query, mapCreator, mapPopulator, new Object[] { container.id });
		return null == unique ? new ArrayList<>() : unique;
	}

	public void set(List<String> calendars) throws SQLException {
		delete();
		String query = "INSERT INTO t_freebusy (container_id, calendars) VALUES(?, ?)";
		insert(query, new Object[] { container.id, calendars.toArray(new String[0]) });
	}

	public void add(String calendar) throws SQLException {
		List<String> calendars = get();
		if (calendars.isEmpty()) {
			calendars = new ArrayList<String>(1);
			calendars.add(calendar);
			String query = "INSERT INTO t_freebusy (container_id, calendars) VALUES(?, ?)";
			insert(query, new Object[] { container.id, calendars.toArray(new String[0]) });
		} else {
			calendars.add(calendar);
			String query = "UPDATE t_freebusy set calendars = ? where container_id = ?";
			update(query, calendars, statementValues, new Object[] { container.id });
		}
	}

	public void remove(String calendar) throws SQLException {
		List<String> calendars = get();
		if (!calendars.isEmpty()) {
			calendars.remove(calendar);
			if (calendars.isEmpty()) {
				delete();
			} else {
				String query = "UPDATE t_freebusy set calendars = ? where container_id = ?";
				update(query, calendars, statementValues, new Object[] { container.id });
			}
		}
	}

	public void delete() throws SQLException {
		delete("DELETE FROM t_freebusy WHERE container_id = ?", new Object[] { container.id });
	}

	private static final Creator<List<String>> mapCreator = con -> new ArrayList<>();

	private static final EntityPopulator<List<String>> mapPopulator = (rs, index, list) -> {
		list.addAll(arrayOfString(rs.getArray(index++)));
		return index;
	};

	protected static List<String> arrayOfString(Array array) throws SQLException {
		List<String> ret = null;
		if (array != null) {
			ret = Arrays.asList((String[]) array.getArray());
		} else {
			ret = new ArrayList<String>();
		}
		return ret;
	}

	private static final StatementValues<List<String>> statementValues = (con, statement, index, currentRow,
			calendars) -> {
		statement.setArray(index++, con.createArrayOf("text", calendars.toArray(new String[0])));
		return index;
	};

}
