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
import java.util.List;
import java.util.Map;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.sanitizer.Sanitizer;
import net.bluemind.core.validator.Validator;
import net.bluemind.domain.service.internal.DomainSettingsCache;
import net.bluemind.eclipse.common.RunnableExtensionLoader;
import net.bluemind.globalsettings.persistence.GlobalSettingsStore;
import net.bluemind.hornetq.client.MQ;
import net.bluemind.hornetq.client.Topic;
import net.bluemind.system.api.GlobalSettings;
import net.bluemind.system.api.IGlobalSettings;
import net.bluemind.system.service.IGlobalSettingsObserver;

public class GlobalSettingsService implements IGlobalSettings {
	private final BmContext context;
	private final GlobalSettingsStore store;
	private final DomainSettingsCache domCache;
	private final Validator validator;
	private final Sanitizer sanitizer;
	private List<IGlobalSettingsObserver> observers;

	public GlobalSettingsService(BmContext context) {
		this.context = context;
		store = new GlobalSettingsStore(context.getDataSource());
		domCache = DomainSettingsCache.get(context);

		sanitizer = new Sanitizer(context);
		validator = new Validator(context);

		RunnableExtensionLoader<IGlobalSettingsObserver> observerLoader = new RunnableExtensionLoader<IGlobalSettingsObserver>();
		observers = observerLoader.loadExtensions("net.bluemind.globalsettings", "hook", "observer", "class");
	}

	@Override
	public void set(Map<String, String> settings) throws ServerFault {
		GlobalSettings updates = GlobalSettings.build(settings);
		GlobalSettings current = GlobalSettings.build(get());

		sanitizer.update(current, updates);
		validator.update(current, updates);

		try {
			store.set(current.update(updates.settings).settings);
		} catch (SQLException e) {
			throw new ServerFault(e);
		}
		domCache.invalidateAll();

		MQ.getProducer(Topic.GLOBAL_SETTINGS_NOTIFICATIONS).send(MQ.newMessage());

		observers.forEach(observer -> observer.onUpdated(context, current, updates));
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
		GlobalSettings current = GlobalSettings.build(get());

		try {
			store.set(current.remove(key).settings);
		} catch (SQLException e) {
			throw new ServerFault(e);
		}

		MQ.getProducer(Topic.GLOBAL_SETTINGS_NOTIFICATIONS).send(MQ.newMessage());

		observers.forEach(observer -> observer.onDeleted(context, current, key));
	}
}
