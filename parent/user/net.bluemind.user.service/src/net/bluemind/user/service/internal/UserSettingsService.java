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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.json.JsonObject;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.service.internal.ContainerStoreService;
import net.bluemind.core.container.service.internal.RBACManager;
import net.bluemind.core.rest.BmContext;
import net.bluemind.domain.api.IDomainSettings;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.role.api.BasicRoles;
import net.bluemind.user.api.IUserSettings;
import net.bluemind.user.persistence.UserSettingsStore;

public class UserSettingsService implements IUserSettings {
	private static final Logger logger = LoggerFactory.getLogger(UserSettingsService.class);

	private final ContainerStoreService<Map<String, String>> userSettingsStoreService;
	private final Container userSettings;
	private final UserSettingsStore userSettingsStore;
	private final IDomainSettings domainSettingsService;
	private final UserSettingsSanitizer sanitizer;

	private RBACManager rbacManager;

	private BmContext bmContext;

	private String domainUid;

	public UserSettingsService(BmContext context, IDomainSettings domainSettingsService, Container userSettings,
			String domainUid) throws ServerFault {
		this.bmContext = context;
		this.domainUid = domainUid;
		this.userSettings = userSettings;
		this.sanitizer = new UserSettingsSanitizer();
		this.domainSettingsService = domainSettingsService;
		userSettingsStore = new UserSettingsStore(context.getDataSource(), userSettings);
		userSettingsStoreService = new ContainerStoreService<>(context.getDataSource(), context.getSecurityContext(),
				userSettings, "usersettings", userSettingsStore);

		rbacManager = new RBACManager(context).forDomain(domainUid);

	}

	@Override
	public void set(String uid, Map<String, String> settings) throws ServerFault {
		rbacManager.forEntry(uid).check(BasicRoles.ROLE_MANAGE_USER_SETTINGS);

		logger.debug("Update user settings: {}", uid);
		sanitizer.sanitize(settings);
		userSettingsStoreService.update(uid, null, settings);

		JsonObject event = new JsonObject().put("containerUid", userSettings.uid).put("itemUid", uid);
		VertxPlatform.eventBus().publish("usersettings.updated", event);
	}

	@Override
	public Map<String, String> get(String uid) throws ServerFault {
		logger.debug("Get user settings: {}", uid);

		rbacManager.forEntry(uid).check(BasicRoles.ROLE_SELF, BasicRoles.ROLE_MANAGER);

		Map<String, String> userSettings = new HashMap<String, String>();

		Map<String, String> ds = domainSettingsService.get();
		if (ds != null && ds.size() > 0) {
			userSettings.putAll(ds);
		}

		ItemValue<Map<String, String>> us = userSettingsStoreService.get(uid, null);
		if (us == null) {
			return userSettings;
		} else if (us.value != null && us.value.size() > 0) {
			userSettings.putAll(us.value);
		}

		return userSettings;
	}

	class UserSettingsSanitizer {

		public void sanitize(Map<String, String> settings) {
			Map<String, String> domainSettings = domainSettingsService.get();
			List<String> duplicateKeys = new ArrayList<>();
			settings.keySet().forEach(key -> {
				if (domainSettings.containsKey(key) && settingsEquals(domainSettings.get(key), settings.get(key))) {
					duplicateKeys.add(key);
				}
			});

			duplicateKeys.forEach(key -> settings.remove(key));

		}

		private boolean settingsEquals(String value1, String value2) {
			return value1 == null ? value2 == null : value1.equals(value2);
		}

	}

}
