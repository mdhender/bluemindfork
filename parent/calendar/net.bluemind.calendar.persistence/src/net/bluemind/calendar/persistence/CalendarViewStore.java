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
package net.bluemind.calendar.persistence;

import java.sql.SQLException;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.calendar.api.CalendarView;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.persistence.AbstractItemValueStore;
import net.bluemind.core.jdbc.Columns;

public class CalendarViewStore extends AbstractItemValueStore<CalendarView> {

	private static final Logger logger = LoggerFactory.getLogger(CalendarViewStore.class);
	private Container container;

	private static final Creator<CalendarView> VIEW_CREATOR = con -> new CalendarView();

	public CalendarViewStore(DataSource dataSource, Container container) {
		super(dataSource);
		this.container = container;
	}

	@Override
	public void create(Item item, CalendarView value) throws SQLException {
		StringBuilder query = new StringBuilder("INSERT INTO t_calendarview (");
		CalendarViewColumns.cols.appendNames(null, query);
		query.append(", item_id) VALUES (");
		CalendarViewColumns.cols.appendValues(query);
		query.append(", ?)");
		insert(query.toString(), value, CalendarViewColumns.values(), new Object[] { item.id });
	}

	@Override
	public void update(Item item, CalendarView value) throws SQLException {
		StringBuilder query = new StringBuilder("UPDATE t_calendarview SET (");
		CalendarViewColumns.cols.appendNames(null, query);
		query.append(") = (");
		CalendarViewColumns.cols.appendValues(query);
		query.append(")");
		query.append("WHERE item_id = ?");
		update(query.toString(), value, CalendarViewColumns.values(), new Object[] { item.id });
	}

	@Override
	public void delete(Item item) throws SQLException {
		delete("DELETE FROM t_calendarview WHERE item_id = ?", new Object[] { item.id });
	}

	@Override
	public CalendarView get(Item item) throws SQLException {
		StringBuilder query = new StringBuilder("SELECT ");
		Columns cols = Columns.create().cols(CalendarViewColumns.cols).col("is_default");
		cols.appendNames("calendarview", query);
		query.append(" FROM t_calendarview calendarview WHERE item_id = ?");

		return unique(query.toString(), VIEW_CREATOR, CalendarViewColumns.populator(), new Object[] { item.id });
	}

	@Override
	public void deleteAll() throws SQLException {
		delete("DELETE FROM t_calendarview WHERE item_id in (SELECT id FROM t_container_item WHERE container_id = ?)",
				new Object[] { container.id });

	}

	public void setDefault(Item item) throws SQLException {
		String query = "UPDATE t_calendarview set is_default = (item_id = ?) WHERE item_id in (SELECT id FROM t_container_item WHERE container_id = ?)";
		update(query, null, new Object[] { item.id, container.id });
	}
}
