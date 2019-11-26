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
package net.bluemind.core.container.service;

import java.sql.SQLException;
import java.util.Map;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.persistence.ContainerSettingsStore;
import net.bluemind.core.container.persistence.DataSourceRouter;
import net.bluemind.core.rest.BmContext;

public class ContainerSettings {

	private ContainerSettingsStore containerSettingsStore;

	public ContainerSettings(BmContext context, Container container) {
		this.containerSettingsStore = new ContainerSettingsStore(DataSourceRouter.get(context, container.uid),
				container);
	}

	public Map<String, String> get() throws ServerFault {
		try {
			return containerSettingsStore.getSettings();
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}
	}

	public void set(Map<String, String> settings) throws ServerFault {
		try {
			containerSettingsStore.setSettings(settings);
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}
	}

	public void mutate(Map<String, String> mutation) throws ServerFault {
		try {
			containerSettingsStore.mutateSettings(mutation);
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}

	}
}
