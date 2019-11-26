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
package net.bluemind.core.container.persistence;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcAbstractStore;

public class ContainerPersonalSettingsStore extends JdbcAbstractStore {

	private static final Creator<Map<String, String>> CREATOR = con -> new HashMap<>();

	@SuppressWarnings("unchecked")
	private static final EntityPopulator<Map<String, String>> POPULATOR = (rs, index, map) -> {
		map.putAll((Map<String, String>) rs.getObject(index++));
		return index;
	};

	private Container container;
	private SecurityContext context;

	public ContainerPersonalSettingsStore(DataSource dataSource, SecurityContext context, Container container) {
		super(dataSource);
		this.container = container;
		this.context = context;
	}

	public Map<String, String> get() throws SQLException {
		String query = "SELECT settings FROM t_container_personal_settings WHERE container_id = ? AND subject = ?";
		Map<String, String> ret = unique(query, CREATOR, POPULATOR,
				new Object[] { container.id, context.getSubject() });

		if (ret == null) {
			return new HashMap<String, String>();
		}

		return ret;
	}

	public void set(Map<String, String> settings) throws ServerFault {
		doOrFail(() -> {
			String query = "DELETE FROM t_container_personal_settings WHERE container_id = ? AND subject = ?";
			delete(query, new Object[] { container.id, context.getSubject() });

			query = "INSERT INTO t_container_personal_settings (settings, container_id, subject) VALUES (?, ?, ?)";
			insert(query, new Object[] { settings, container.id, context.getSubject() });
			return null;
		});
	}

	public void deleteAll() throws SQLException {
		delete("DELETE FROM t_container_personal_settings WHERE container_id = ?", new Object[] { container.id });
	}

}
