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
package net.bluemind.device.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.sql.DataSource;

import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.persistence.AbstractItemValueStore;
import net.bluemind.core.container.persistence.StringCreator;
import net.bluemind.device.api.Device;

public class DeviceStore extends AbstractItemValueStore<Device> {
	private Container container;

	private static final Creator<Device> DEVICE_CREATOR = new Creator<Device>() {
		@Override
		public Device create(ResultSet con) throws SQLException {
			return new Device();
		}
	};

	public DeviceStore(DataSource dataSource, Container container) {
		super(dataSource);
		this.container = container;
	}

	@Override
	public void create(Item item, Device value) throws SQLException {
		StringBuilder query = new StringBuilder("INSERT INTO t_eas_device (");
		DeviceColumns.cols.appendNames(null, query);
		query.append(", item_id) VALUES (");
		DeviceColumns.cols.appendValues(query);
		query.append(", ?)");

		insert(query.toString(), value, DeviceColumns.values(item));

	}

	private static String updateRequest() {
		StringBuilder query = new StringBuilder("UPDATE t_eas_device SET (");
		DeviceColumns.cols.appendNames(null, query);
		query.append(") = (");
		DeviceColumns.cols.appendValues(query);
		query.append(")");
		query.append("WHERE item_id = ?");
		return query.toString();
	}

	private static final String UPDATE_REQ = updateRequest();

	@Override
	public void update(Item item, Device value) throws SQLException {
		update(UPDATE_REQ, value, DeviceColumns.values(item));
	}

	@Override
	public void delete(Item item) throws SQLException {
		delete("DELETE FROM t_eas_device WHERE item_id = ?", new Object[] { item.id });
	}

	@Override
	public Device get(Item item) throws SQLException {
		StringBuilder query = new StringBuilder("SELECT ");
		DeviceColumns.cols.appendNames("device", query);
		query.append(" FROM t_eas_device device WHERE item_id = ?");
		return unique(query.toString(), DEVICE_CREATOR,
				Arrays.<EntityPopulator<Device>>asList(DeviceColumns.populator()), new Object[] { item.id });
	}

	@Override
	public void deleteAll() throws SQLException {
		delete("DELETE FROM t_eas_device WHERE item_id IN (SELECT id FROM t_container_item WHERE container_id = ?)",
				new Object[] { container.id });
	}

	public List<Device> getWipedDevice() throws SQLException {
		StringBuilder query = new StringBuilder();
		query.append("SELECT ");
		DeviceColumns.cols.appendNames(null, query);
		query.append(" FROM t_eas_device WHERE wipe");
		return select(query.toString(), DEVICE_CREATOR, DeviceColumns.populator());
	}

	public String byIdentifier(String identifier) throws SQLException {
		String q = "SELECT uid FROM t_container_item INNER JOIN t_eas_device ON item_id = t_container_item.id WHERE identifier = ? AND container_id = ?";
		return unique(q, StringCreator.FIRST, Collections.emptyList(), new Object[] { identifier, container.id });

	}
}
