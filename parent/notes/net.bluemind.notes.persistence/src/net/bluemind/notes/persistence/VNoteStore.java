/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2022
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
package net.bluemind.notes.persistence;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.model.SortDescriptor;
import net.bluemind.core.container.persistence.AbstractItemValueStore;
import net.bluemind.core.container.persistence.LongCreator;
import net.bluemind.notes.api.VNote;

public class VNoteStore extends AbstractItemValueStore<VNote> {

	private static final Logger logger = LoggerFactory.getLogger(VNoteStore.class);

	private static final Creator<VNote> NOTE_CREATOR = con -> new VNote();

	private Container container;

	public VNoteStore(DataSource pool, Container container) {
		super(pool);
		this.container = container;
	}

	@Override
	public void create(Item item, VNote note) throws SQLException {
		String query = "INSERT INTO t_notes_vnote (" + VNoteColumns.cols.names() + ", item_id) VALUES ("
				+ VNoteColumns.cols.values() + ", ?)";
		insert(query, note, VNoteColumns.values(item));
	}

	@Override
	public void update(Item item, VNote value) throws SQLException {
		String query = "UPDATE t_notes_vnote SET (" + VNoteColumns.cols.names() + ") = (" + VNoteColumns.cols.values()
				+ ") WHERE item_id = ?";
		update(query, value, VNoteColumns.values(item));
	}

	@Override
	public VNote get(Item item) throws SQLException {
		String query = "SELECT " + VNoteColumns.cols.names() + " FROM t_notes_vnote WHERE item_id = ?";
		return unique(query, NOTE_CREATOR, VNoteColumns.populator(), new Object[] { item.id });
	}

	@Override
	public void delete(Item item) throws SQLException {
		delete("DELETE FROM t_notes_vnote WHERE item_id = ?", new Object[] { item.id });
	}

	@Override
	public void deleteAll() throws SQLException {
		delete("DELETE FROM t_notes_vnote WHERE item_id IN (SELECT id FROM t_container_item WHERE container_id = ?)",
				new Object[] { container.id });
	}

	public List<Long> sortedIds(SortDescriptor sorted) throws SQLException {
		String query = "SELECT item.id FROM t_notes_vnote "
				+ "INNER JOIN t_container_item AS item ON t_notes_vnote.item_id = item.id " //
				+ "WHERE item.container_id = ? " //
				+ "AND (item.flags::bit(32) & 2::bit(32)) = 0::bit(32) " // not deleted
				+ "ORDER BY item.created DESC";
		// FIXME use sort params
		return select(query, LongCreator.FIRST, Collections.emptyList(), new Object[] { container.id });
	}

}
