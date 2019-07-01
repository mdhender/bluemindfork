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
package net.bluemind.system.service.internal;

import java.sql.SQLException;
import java.util.Map;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.rest.BmContext;
import net.bluemind.domain.service.internal.DomainSettingsCache;
import net.bluemind.globalsettings.persistance.GlobalSettingsStore;
import net.bluemind.hornetq.client.MQ;
import net.bluemind.hornetq.client.Topic;
import net.bluemind.system.api.IGlobalSettings;

public class GlobalSettingsService implements IGlobalSettings {

	private final GlobalSettingsStore store;
	private final DomainSettingsCache domCache;

	public GlobalSettingsService(BmContext context) {
		store = new GlobalSettingsStore(context.getDataSource());
		domCache = DomainSettingsCache.get(context);
	}

	@Override
	public void set(Map<String, String> settings) throws ServerFault {
		try {
			Map<String, String> merged = get();
			merged.putAll(settings);
			store.set(merged);
			domCache.invalidateAll();

			MQ.getProducer(Topic.GLOBAL_SETTINGS_NOTIFICATIONS).send(MQ.newMessage());

		} catch (SQLException e) {
			throw new ServerFault(e);
		}
	}

	@Override
	public Map<String, String> get() throws ServerFault {
		try {
			return store.get();
		} catch (SQLException e) {
			throw new ServerFault(e);
		}
	}

	@Override
	public void delete(String key) throws ServerFault {
		Map<String, String> values = get();
		values.remove(key);
		try {
			store.set(values);
			MQ.getProducer(Topic.GLOBAL_SETTINGS_NOTIFICATIONS).send(MQ.newMessage());
		} catch (SQLException e) {
			throw new ServerFault(e);
		}
	}

}
