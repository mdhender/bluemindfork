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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.List;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.calendar.api.VEvent;
import net.bluemind.core.api.date.BmDateTime;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.persistence.AbstractItemValueStore;
import net.bluemind.core.container.persistence.StringCreator;
import net.bluemind.core.jdbc.convert.DateTimeType;

public class VEventStore extends AbstractItemValueStore<VEvent> {

	private static final Logger logger = LoggerFactory.getLogger(VEventStore.class);

	private static final Creator<VEvent> EVENT_CREATOR = con -> new VEvent();

	public static class ItemUid {
		public String itemUid;
	}

	private static final Creator<VEventStore.ItemUid> REMINDER_CREATOR = con -> new VEventStore.ItemUid();

	private Container container;

	private VEventOccurrenceStore recurringStore;

	public VEventStore(DataSource pool, Container container) {
		super(pool);
		this.container = container;
		this.recurringStore = new VEventOccurrenceStore(pool, container);
	}

	@Override
	public void create(Item item, VEvent event) throws SQLException {
		String query = "INSERT INTO t_calendar_vevent (" + VEventColumns.ALL.names() + ", item_id) VALUES ("
				+ VEventColumns.ALL.values() + ", ?)";

		insert(query, event, VEventColumns.values(item.id));
	}

	@Override
	public void update(Item item, VEvent value) throws SQLException {
		delete(item);
		create(item, value);
	}

	@Override
	public VEvent get(Item item) throws SQLException {
		String query = "SELECT " + VEventColumns.ALL.names()
				+ " FROM t_calendar_vevent WHERE item_id = ? AND recurid_timestamp is null";
		return unique(query, EVENT_CREATOR, VEventColumns.populator(), new Object[] { item.id });
	}

	@Override
	public void delete(Item item) throws SQLException {
		delete("DELETE FROM t_calendar_vevent WHERE item_id = ?", new Object[] { item.id });
	}

	@Override
	public void deleteAll() throws SQLException {
		delete("DELETE FROM t_calendar_vevent WHERE item_id IN (SELECT id FROM t_container_item WHERE container_id = ?) AND recurid_timestamp IS NULL",
				new Object[] { container.id });
		recurringStore.deleteAll();
	}

	@Override
	public List<VEvent> getMultiple(List<Item> items) throws SQLException {
		String query = "SELECT item_id, " + VEventColumns.ALL.names()
				+ " FROM t_calendar_vevent WHERE item_id = ANY(?::int4[]) AND recurid_timestamp IS NULL ORDER BY item_id ASC";
		List<ItemV<VEvent>> values = select(query, (ResultSet con) -> {
			return new ItemV<>();
		}, (ResultSet rs, int index, ItemV<VEvent> card) -> {
			card.itemId = rs.getLong(index++);
			card.value = new VEvent();
			return VEventColumns.populator().populate(rs, index, card.value);

		}, new Object[] { items.stream().map(i -> i.id).toArray(Long[]::new) });

		return join(items, values);
	}

	/**
	 * @param dtalarm
	 * @return
	 * @throws SQLException
	 */
	public List<ItemUid> getReminder(BmDateTime dtalarm) throws SQLException {
		String query = "SELECT DISTINCT i.uid FROM t_container_item i, t_calendar_vevent v, "
				+ "unnest(v.rdate_timestamp || (array[null]::timestamp without time zone[])) rdate, "
				+ "unnest(v.valarm_trigger || (array[null]::integer[])) alarm "
				+ " WHERE i.id = v.item_id AND container_id = ? AND valarm_trigger IS NOT NULL AND ( "
				+ "    (dtstart_timestamp + (alarm || ' seconds')::interval) = (COALESCE(timezone(dtstart_timezone, ?), ?)) "
				+ "      OR (rrule_frequency IS NOT NULL AND (rrule_until_timestamp IS NULL OR rrule_until_timestamp > (COALESCE(timezone(dtstart_timezone, ?), ?)) ))"
				+ "      OR (rdate + (alarm || ' seconds')::interval) = (COALESCE(timezone(dtstart_timezone, ?), ?))"
				+ " )";

		Timestamp reminder = DateTimeType.asTimestamp(dtalarm);
		return select(query, REMINDER_CREATOR, VEventColumns.itemUidPopulator(),
				new Object[] { container.id, reminder, reminder, reminder, reminder, reminder, reminder });
	}

	public List<String> getEventUidsWithAlarm() throws SQLException {
		String query = "SELECT DISTINCT i.uid FROM t_container_item i, t_calendar_vevent v "
				+ " WHERE i.id = v.item_id AND container_id = ? AND valarm_trigger IS NOT NULL";
		return select(query, StringCreator.FIRST, Collections.emptyList(), new Object[] { container.id });
	}

}
