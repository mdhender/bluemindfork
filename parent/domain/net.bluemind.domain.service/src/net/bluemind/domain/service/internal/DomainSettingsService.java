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
package net.bluemind.domain.service.internal;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.json.JsonObject;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.service.internal.ContainerStoreService;
import net.bluemind.core.container.service.internal.RBACManager;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.validator.Validator;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.api.DomainSettings;
import net.bluemind.domain.api.IDomainSettings;
import net.bluemind.domain.api.IDomains;
import net.bluemind.domain.hook.IDomainHook;
import net.bluemind.domain.persistence.DomainSettingsStore;
import net.bluemind.eclipse.common.RunnableExtensionLoader;
import net.bluemind.globalsettings.persistence.GlobalSettingsStore;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.role.api.BasicRoles;

public class DomainSettingsService implements IDomainSettings {
	private static final Logger logger = LoggerFactory.getLogger(DomainSettingsService.class);

	private final ContainerStoreService<Map<String, String>> domainSettingsStoreService;
	private final GlobalSettingsStore settingsStore;
	private final DomainSettingsStore domainSettingsStore;
	private final DomainSettingsValidator validator = new DomainSettingsValidator();
	private final Validator extValidator;
	private final String domainUid;
	private final BmContext context;
	private final RBACManager rbac;
	private final DomainSettingsCache cache;

	private static final List<IDomainHook> hooks = getHooks();

	public DomainSettingsService(BmContext context, Container domainSettingsContainer, String domainUid) {
		this.context = context;
		this.domainUid = domainUid;
		domainSettingsStore = new DomainSettingsStore(context.getDataSource(), domainSettingsContainer);
		domainSettingsStoreService = new ContainerStoreService<>(context.getDataSource(), SecurityContext.SYSTEM,
				domainSettingsContainer, "domainsettings", domainSettingsStore);

		settingsStore = new GlobalSettingsStore(context.getDataSource());
		rbac = new RBACManager(context).forDomain(domainUid);
		extValidator = new Validator(context);
		this.cache = DomainSettingsCache.get(context);
	}

	@Override
	public void set(Map<String, String> settings) throws ServerFault {
		rbac.check(BasicRoles.ROLE_ADMIN);
		cache.invalidate(domainUid);

		logger.debug("Set domain settings: {}", domainUid);
		DomainSettings newDomainSettings = new DomainSettings(domainUid, settings);

		ItemValue<Map<String, String>> oldValues = domainSettingsStoreService.get(domainUid, null);
		if (null == oldValues || null == oldValues.value || oldValues.value.isEmpty()) {
			validator.create(settings);
			extValidator.create(newDomainSettings);
		} else {
			validator.update(oldValues.value, settings, domainUid);

			DomainSettings oldDomainSettings = new DomainSettings(domainUid, oldValues.value);
			extValidator.update(oldDomainSettings, newDomainSettings);
		}

		domainSettingsStoreService.update(domainUid, null, settings);

		IDomains domainService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IDomains.class);
		ItemValue<Domain> domain = domainService.get(domainUid);

		for (IDomainHook hook : hooks) {
			hook.onSettingsUpdated(context, domain, oldValues.value, settings);
		}

		JsonObject event = new JsonObject();
		event.putString("containerUid", domainUid);
		VertxPlatform.eventBus().publish("domainsettings.updated", event);
	}

	@Override
	public Map<String, String> get() throws ServerFault {
		rbac.check(BasicRoles.ROLE_MANAGER);

		Map<String, String> cached = cache.getIfPresent(domainUid);
		if (cached != null) {
			return new HashMap<>(cached);
		}

		Map<String, String> domainSettings = new HashMap<String, String>();
		try {
			domainSettings.putAll(settingsStore.get());
		} catch (SQLException sqle) {
			throw new ServerFault(sqle);
		}

		ItemValue<Map<String, String>> ds = domainSettingsStoreService.get(domainUid, null);
		if (ds == null) {
			return domainSettings;
		} else if (ds.value != null && ds.value.size() > 0) {
			domainSettings.putAll(ds.value);
		}
		cache.put(domainUid, domainSettings);

		return domainSettings;
	}

	private static List<IDomainHook> getHooks() {
		RunnableExtensionLoader<IDomainHook> loader = new RunnableExtensionLoader<IDomainHook>();
		List<IDomainHook> hooks = loader.loadExtensions("net.bluemind.domain", "domainHook", "hook", "class");
		return hooks;
	}

}
