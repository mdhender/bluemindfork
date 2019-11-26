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
package net.bluemind.todolist.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.List;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.api.date.BmDateTime;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.model.SortDescriptor;
import net.bluemind.core.container.model.SortDescriptor.Direction;
import net.bluemind.core.container.model.SortDescriptor.Field;
import net.bluemind.core.container.persistence.AbstractItemValueStore;
import net.bluemind.core.container.persistence.LongCreator;
import net.bluemind.core.jdbc.convert.DateTimeType;
import net.bluemind.todolist.api.VTodo;

public class VTodoStore extends AbstractItemValueStore<VTodo> {

	private static final Logger logger = LoggerFactory.getLogger(VTodoStore.class);

	private static final Creator<VTodo> TODO_CREATOR = con -> new VTodo();

	public static class ItemUid {
		public String itemUid;
	}

	private Container container;

	public VTodoStore(DataSource pool, Container container) {
		super(pool);
		this.container = container;
	}

	@Override
	public void create(Item item, VTodo todo) throws SQLException {
		logger.debug("create ical vtodo for item {} ", item.id);

		StringBuilder query = new StringBuilder("insert into t_todolist_vtodo ( item_id, ");

		VTodoColumns.appendNames(null, query);
		query.append(") values ( " + item.id + " ,");
		VTodoColumns.appendValues(query);
		query.append(")");
		insert(query.toString(), todo, VTodoColumns.values());
	}

	@Override
	public void update(Item item, VTodo value) throws SQLException {
		logger.debug("create vtodo for item {} ", item.id);
		StringBuilder query = new StringBuilder("update t_todolist_vtodo set ( ");

		VTodoColumns.appendNames(null, query);
		query.append(") = ( ");
		VTodoColumns.appendValues(query);

		query.append(")");
		query.append("where item_id = " + item.id);

		update(query.toString(), value, VTodoColumns.values());

	}

	@Override
	public VTodo get(Item item) throws SQLException {
		StringBuilder query = new StringBuilder("select ");

		VTodoColumns.appendNames("vtodo", query);

		query.append(" from t_todolist_vtodo vtodo where item_id = " + item.id);
		VTodo ret = unique(query.toString(), TODO_CREATOR, VTodoColumns.populator());

		return ret;
	}

	@Override
	public void delete(Item item) throws SQLException {
		delete("delete from t_todolist_vtodo where item_id = ?", new Object[] { item.id });
	}

	@Override
	public void deleteAll() throws SQLException {
		delete("delete from t_todolist_vtodo where item_id in ( select id from t_container_item where container_id = ?)",
				new Object[] { container.id });
	}

	private static final Creator<VTodoStore.ItemUid> REMINDER_CREATOR = new Creator<VTodoStore.ItemUid>() {
		@Override
		public VTodoStore.ItemUid create(ResultSet con) throws SQLException {
			return new VTodoStore.ItemUid();
		}
	};

	public List<ItemUid> getReminder(BmDateTime dtalarm) throws SQLException {
		String query = "SELECT DISTINCT i.uid from t_container_item i, t_todolist_vtodo v, "
				+ "unnest(v.rdate_timestamp  || (array[null]::timestamp without time zone[])) rdate, "
				+ "unnest(v.valarm_trigger|| (array[null]::integer[])) alarm "
				+ " where i.id = v.item_id and container_id = ? " + " AND valarm_trigger IS NOT NULL " + " AND ( "
				+ "    (dtstart_timestamp + (alarm || ' seconds')::interval) = (COALESCE(timezone(dtstart_timezone, ?), ?)) "
				+ "	     OR (rrule_frequency IS NOT NULL AND (rrule_until_timestamp IS NULL OR rrule_until_timestamp > (COALESCE(timezone(dtstart_timezone, ?), ?)) ))"
				+ "      OR (rdate + (alarm || ' seconds')::interval) = (COALESCE(timezone(dtstart_timezone, ?), ?))"
				+ " )";
		Timestamp reminder = DateTimeType.asTimestamp(dtalarm);
		return select(query.toString(), REMINDER_CREATOR, VTodoColumns.itemUidPopulator(),
				new Object[] { container.id, reminder, reminder, reminder, reminder, reminder, reminder });

	}

	public List<Long> sortedIds(SortDescriptor sorted) throws SQLException {
		// {
		// "fields" : [ {
		// "column" : "PidLidTaskDueDate",
		// "dir" : "Asc"
		// }, {
		// "column" : "PidLidTaskOrdinal",
		// "dir" : "Asc"
		// } ]
		// }

		logger.debug("sorted by {}", sorted);
		String query = "SELECT item.id FROM t_todolist_vtodo rec "
				+ "INNER JOIN t_container_item item ON rec.item_id=item.id " //
				+ "WHERE item.container_id=? " //
				+ "AND (item.flags::bit(32) & 2::bit(32))=0::bit(32) " // not deleted
		;

		int added = 0;
		for (int i = 0; i < sorted.fields.size(); i++) {
			Field field = sorted.fields.get(i);
			switch (field.column) {
			case "PidLidTaskDueDate":
				if (added > 0) {
					query += ", ";
				} else if (added == 0) {
					query += "ORDER BY ";
				}
				query += "rec.due_timestamp " + dir(field);
				added++;
				break;
			default:
				break;
			}
		}
		logger.debug("query: '{}'", query);
		return select(query, LongCreator.FIRST, Collections.emptyList(), new Object[] { container.id });
	}

	private String dir(Field field) {
		return field.dir == Direction.Asc ? "ASC" : "DESC";
	}

}
