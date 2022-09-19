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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.calendar.api.VEvent;
import net.bluemind.calendar.api.VEventCounter;
import net.bluemind.calendar.api.VEventOccurrence;
import net.bluemind.calendar.api.VEventSeries;
import net.bluemind.core.api.date.BmDateTime;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.model.SortDescriptor;
import net.bluemind.core.container.model.SortDescriptor.Direction;
import net.bluemind.core.container.model.SortDescriptor.Field;
import net.bluemind.core.container.persistence.AbstractItemValueStore;
import net.bluemind.core.container.persistence.LongCreator;
import net.bluemind.core.container.persistence.StringCreator;

public class VEventSeriesStore extends AbstractItemValueStore<VEventSeries> {

	private static final Logger logger = LoggerFactory.getLogger(VEventSeriesStore.class);

	private Container container;

	private VEventOccurrenceStore recurringStore;
	private VEventStore eventStore;
	private VEventCounterStore counterStore;

	public VEventSeriesStore(DataSource pool, Container container) {
		super(pool);
		this.container = container;
		this.recurringStore = new VEventOccurrenceStore(pool, container);
		this.counterStore = new VEventCounterStore(pool, container);
		this.eventStore = new VEventStore(pool, container);
	}

	@Override
	public void create(Item item, VEventSeries series) throws SQLException {
		insert("INSERT INTO t_calendar_series ( " + SeriesColumns.cols.names() + ", item_id) VALUES ("
				+ SeriesColumns.cols.values() + ", ?)", series, SeriesColumns.values(item.id));
		if (null != series.main) {
			eventStore.create(item, series.main);
		}
		recurringStore.create(item, series.occurrences);
		counterStore.create(item, series.counters);
	}

	@Override
	public void update(Item item, VEventSeries series) throws SQLException {
		logger.debug("updating vevent series for item {} ", item.id);
		delete(item);
		create(item, series);
	}

	@Override
	public VEventSeries get(Item item) throws SQLException {
		List<VEventSeries> res = loadSeries(Arrays.asList(item));
		if (res.isEmpty()) {
			return null;
		} else {
			return res.get(0);
		}
	}

	public class VEventDB {
		public long itemId;
		public String icsUid;
		public Map<String, String> properties;
		public boolean main;
		public boolean acceptCounters;
		public VEvent occurrence;
	}

	private List<VEventSeries> loadSeries(List<Item> items) throws SQLException {

		String query = "SELECT series.item_id, series.ics_uid, properties, accept_counters, recurid_timestamp IS NULL, "
				+ VEventOccurrenceColumns.ALL.names() + " FROM t_calendar_series series, t_calendar_vevent v" //
				+ " WHERE series.item_id = ANY(?::int8[]) AND series.item_id = v.item_id ORDER BY series.item_id";

		Long[] itemsId = items.stream().map(i -> i.id).toArray(i -> new Long[i]);
		List<VEventDB> values = select(query, (ResultSet con) -> {
			return new VEventDB();
		}, (ResultSet rs, int index, VEventDB event) -> {
			event.itemId = rs.getLong(index++);
			event.icsUid = rs.getString(index++);
			@SuppressWarnings("unchecked")
			Map<String, String> props = (Map<String, String>) rs.getObject(index++);
			if (props != null) {
				event.properties = props;
			} else {
				event.properties = Collections.emptyMap();
			}
			event.acceptCounters = rs.getBoolean(index++);
			event.main = rs.getBoolean(index++);

			if (event.main) {
				event.occurrence = new VEvent();
				return VEventColumns.populator().populate(rs, index, event.occurrence);
			} else {
				event.occurrence = new VEventOccurrence();
				return VEventOccurrenceColumns.populator().populate(rs, index, (VEventOccurrence) event.occurrence);
			}

		}, new Object[] { itemsId });

		return asSeries(values, items, loadCounters(itemsId));
	}

	public class VEventCounterDB {
		public long itemId;
		public VEventCounter counter;
	}

	private Map<Long, List<VEventCounter>> loadCounters(Long[] items) throws SQLException {
		String query = "SELECT series.item_id, " + VEventCounterColumns.SELECT_ALL.names()
				+ " FROM t_calendar_series series, t_calendar_vevent_counter v" //
				+ " WHERE series.item_id = ANY(?::int8[]) AND series.item_id = (v.vevent).item_id ORDER BY series.item_id";

		List<VEventCounterDB> values = select(query, (ResultSet con) -> {
			return new VEventCounterDB();
		}, (ResultSet rs, int index, VEventCounterDB event) -> {
			event.itemId = rs.getLong(index++);
			VEventCounter vEventCounter = new VEventCounter();
			vEventCounter.originator = new VEventCounter.CounterOriginator();
			vEventCounter.counter = new VEventOccurrence();
			event.counter = vEventCounter;
			return VEventCounterColumns.populator().populate(rs, index, event.counter);
		}, new Object[] { items });

		return values.stream().collect(Collectors.toMap(counter -> counter.itemId,
				counter -> Arrays.asList(counter.counter), (counter1, counter2) -> {
					List<VEventCounter> mergedCounters = new ArrayList<>();
					mergedCounters.addAll(counter1);
					mergedCounters.addAll(counter2);
					return mergedCounters;
				}));
	}

	private List<VEventSeries> asSeries(List<VEventDB> values, List<Item> items,
			Map<Long, List<VEventCounter>> counters) {

		List<VEventSeries> ret = new ArrayList<>(items.size());

		Map<Long, VEventSeries> m = new HashMap<>(items.size());
		long current = -1;
		VEventSeries series = new VEventSeries();

		for (VEventDB dbV : values) {
			if (current != dbV.itemId) {
				current = dbV.itemId;
				series = new VEventSeries();
				series.icsUid = dbV.icsUid;
				series.properties = dbV.properties;
				series.acceptCounters = dbV.acceptCounters;
				series.occurrences = new ArrayList<>(1);
				m.put(current, series);
			}
			if (dbV.main) {
				series.main = dbV.occurrence;
			} else {
				series.occurrences.add((VEventOccurrence) dbV.occurrence);
			}
		}

		for (Item i : items) {
			VEventSeries s = m.get(i.id);
			if (s != null) { // don't fail when requesting an non-existing item
				s.counters = counters.computeIfAbsent(i.id, id -> new ArrayList<>());
			}
			ret.add(s);
		}
		return ret;
	}

	@Override
	public void delete(Item item) throws SQLException {
		delete("DELETE FROM t_calendar_series WHERE item_id = ?", new Object[] { item.id });
		delete("DELETE FROM t_calendar_vevent WHERE item_id = ?", new Object[] { item.id });
		delete("DELETE FROM t_calendar_vevent_counter WHERE (vevent).item_id = ?", new Object[] { item.id });
	}

	@Override
	public void deleteAll() throws SQLException {
		delete("DELETE FROM t_calendar_series WHERE item_id in (SELECT id FROM t_container_item WHERE container_id = ?)",
				new Object[] { container.id });
		delete("DELETE FROM t_calendar_vevent WHERE item_id in (SELECT id FROM t_container_item WHERE container_id = ?) AND recurid_timestamp is null",
				new Object[] { container.id });
		recurringStore.deleteAll();
		counterStore.deleteAll();
	}

	@Override
	public List<VEventSeries> getMultiple(List<Item> items) throws SQLException {
		return loadSeries(items);
	}

	/**
	 * @param dtalarm
	 * @return
	 * @throws SQLException
	 */
	public List<VEventStore.ItemUid> getReminder(BmDateTime dtalarm) throws SQLException {
		return eventStore.getReminder(dtalarm);
	}

	public List<String> findByIcsUid(String uid) throws SQLException {
		return select("SELECT item.uid FROM t_container_item item, t_calendar_series series" //
				+ " WHERE item.id = series.item_id AND item.container_id = ? AND lower(series.ics_uid) = ?", //
				rs -> rs.getString(1), Collections.emptyList(), new Object[] { container.id, uid.toLowerCase() });
	}

	public List<Long> sortedIds(SortDescriptor sorted) throws SQLException {
		logger.debug("sorted by {}", sorted);
		String query = "SELECT item.id FROM t_calendar_series rec "
				+ "INNER JOIN t_container_item item ON rec.item_id = item.id " //
				+ "INNER JOIN t_calendar_vevent ev ON (rec.item_id = ev.item_id AND ev.recurid_timestamp IS NULL) "//
				+ "WHERE item.container_id=? " //
				+ "AND (item.flags::bit(32) & 2::bit(32)) = 0::bit(32) " // not deleted
		;
		int added = 0;
		for (int i = 0; i < sorted.fields.size(); i++) {
			Field field = sorted.fields.get(i);
			switch (field.column) {
			case "PidLidAppointmentEndWhole":
				if (added > 0) {
					query += ", ";
				} else if (added == 0) {
					query += "ORDER BY ";
				}
				query += "ev.dtend_timestamp " + dir(field);
				added++;
				break;
			case "PidLidRecurring":
				if (added > 0) {
					query += ", ";
				} else if (added == 0) {
					query += "ORDER BY ";
				}
				query += "ev.rrule_frequency IS NOT NULL " + dir(field);
				added++;
				break;
			default:
				break;
			}
		}
		return select(query, LongCreator.FIRST, Collections.emptyList(), new Object[] { container.id });
	}

	private String dir(Field field) {
		return field.dir == Direction.Asc ? "ASC" : "DESC";
	}

	public List<String> searchPendingPropositions(String owner) throws SQLException {
		String query = "SELECT DISTINCT ci.uid FROM t_calendar_series series " //
				+ "JOIN t_calendar_vevent_counter v ON (v.vevent).item_id = series.item_id " //
				+ "JOIN t_calendar_vevent ve ON ve.item_id = series.item_id " //
				+ "JOIN t_container_item ci ON ci.id = series.item_id " //
				+ "JOIN t_container c ON ci.container_id = c.id " //
				+ "WHERE c.id = ? AND ve.organizer_dir = ?";
		return select(query, StringCreator.FIRST, Collections.emptyList(), new Object[] { container.id, owner });
	}
}
