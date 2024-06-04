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
package net.bluemind.user.service.internal;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.service.internal.ContainerStoreService;
import net.bluemind.core.container.service.internal.RBACManager;
import net.bluemind.core.rest.BmContext;
import net.bluemind.domain.api.IDomainSettings;
import net.bluemind.eclipse.common.RunnableExtensionLoader;
import net.bluemind.role.api.BasicRoles;
import net.bluemind.user.api.IUserSettings;
import net.bluemind.user.api.UserSettings;
import net.bluemind.user.hook.settings.IUserSettingsHook;
import net.bluemind.user.persistence.UserSettingsStore;

public class UserSettingsService implements IUserSettings {
	private static final Logger logger = LoggerFactory.getLogger(UserSettingsService.class);
	private static final List<IUserSettingsHook> hooks = getHooks();

	private final ContainerStoreService<UserSettings> userSettingsStoreService;
	private final Container userSettings;
	private final IDomainSettings domainSettingsService;
	private final UserSettingsSanitizer sanitizer;
	private RBACManager rbacManager;

	public UserSettingsService(BmContext context, IDomainSettings domainSettingsService, Container userSettings,
			String domainUid) throws ServerFault {
		this.userSettings = userSettings;
		this.sanitizer = new UserSettingsSanitizer();
		this.domainSettingsService = domainSettingsService;
		var userSettingsStore = new UserSettingsStore(context.getDataSource(), userSettings);
		this.userSettingsStoreService = new ContainerStoreService<>(context.getDataSource(),
				context.getSecurityContext(), userSettings, userSettingsStore);
		this.rbacManager = new RBACManager(context).forDomain(domainUid);
	}

	private static List<IUserSettingsHook> getHooks() {
		RunnableExtensionLoader<IUserSettingsHook> loader = new RunnableExtensionLoader<>();
		return loader.loadExtensionsWithPriority("net.bluemind.user.hook", "usersettings", "hook", "impl");
	}

	@Override
	public void set(String uid, Map<String, String> settings) throws ServerFault {
		rbacManager.forEntry(uid).check(BasicRoles.ROLE_MANAGE_USER_SETTINGS);

		logger.debug("Update user settings: {}", uid);
		sanitizer.sanitize(settings, domainSettingsService);
		userSettingsStoreService.update(uid, null, UserSettings.of(settings));

		hooks.forEach(hook -> hook.onSettingsUpdate(userSettings.uid, uid));
	}

	@Override
	public Map<String, String> get(String uid) throws ServerFault {
		rbacManager.forEntry(uid).check(BasicRoles.ROLE_SELF, BasicRoles.ROLE_MANAGER);

		logger.debug("Get user settings: {}", uid);
		Map<String, String> loadedSettings = new HashMap<>();

		Map<String, String> ds = domainSettingsService.get();
		if (ds != null && ds.size() > 0) {
			loadedSettings.putAll(ds);
		}

		ItemValue<UserSettings> us = userSettingsStoreService.get(uid, null);
		if (us == null) {
			return loadedSettings;
		} else if (us.value != null && us.value.values != null && us.value.values.size() > 0) {
			loadedSettings.putAll(us.value.values);
		}

		return loadedSettings;
	}

	@Override
	public void setOne(String uid, String name, String value) throws ServerFault {
		rbacManager.forEntry(uid).check(BasicRoles.ROLE_MANAGE_USER_SETTINGS);

		logger.debug("Set setting {} with value {} for user {}", name, value, uid);
		Map<String, String> settings = this.get(uid);
		settings.put(name, value);

		this.set(uid, settings);
		hooks.forEach(hook -> hook.onSettingsUpdate(userSettings.uid, uid));
	}

	@Override
	public String getOne(String uid, String name) throws ServerFault {
		rbacManager.forEntry(uid).check(BasicRoles.ROLE_SELF, BasicRoles.ROLE_MANAGER);
		logger.debug("Get setting {} for user {}", name, uid);
		return this.get(uid).get(name);
	}
}
