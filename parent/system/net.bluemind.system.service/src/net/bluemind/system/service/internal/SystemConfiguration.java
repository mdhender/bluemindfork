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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.api.ParametersValidator;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.service.internal.RBACManager;
import net.bluemind.core.jdbc.JdbcAbstractStore;
import net.bluemind.core.rest.BmContext;
import net.bluemind.role.api.BasicRoles;
import net.bluemind.system.api.ISystemConfiguration;
import net.bluemind.system.api.SystemConf;
import net.bluemind.system.persistence.SystemConfStore;
import net.bluemind.system.service.hooks.SystemConfigurationHooks;

public class SystemConfiguration implements ISystemConfiguration {

	private static final Logger logger = LoggerFactory.getLogger(SystemConfiguration.class);
	private SystemConfStore systemConfStore;
	private BmContext context;
	private RBACManager rbac;

	public SystemConfiguration(BmContext context) {
		this.context = context;
		this.systemConfStore = new SystemConfStore(context.getDataSource());
		rbac = new RBACManager(context);

	}

	@Override
	public SystemConf getValues() throws ServerFault {
		rbac.check(BasicRoles.ROLE_MANAGE_SYSTEM_CONF);

		Properties props = new Properties();
		if (new File("/etc/bm/bm.ini").exists()) {
			try (FileInputStream in = new FileInputStream("/etc/bm/bm.ini")) {
				props.load(in);

			} catch (IOException e) {
				logger.error("error during loading bm.ini", e);
			}
		} else {
			logger.warn("/etc/bm/bm.ini not found");
		}

		Map<String, String> values = props.entrySet().stream()
				.collect(Collectors.toMap(entry -> (String) entry.getKey(), entry -> (String) entry.getValue()));

		try {
			values.putAll(systemConfStore.get());
		} catch (Exception e) {
			logger.warn("error retrieving system configuration from database ({}), will try 3.0 bm-info",
					e.getMessage());

			try {
				values.putAll(systemConfStore.get30());
			} catch (Exception e1) {

				if (new File("/etc/bm/bm-core.tok").exists()) {
					// Something goes wrong
					// BlueMind is already installed but we cannot
					// retrieve configuration from database
					throw new ServerFault("Fail to fetch system configuration", e1);
				}

				logger.error("error retrieving system configuration (3.0) from database", e1);
			}
		}

		return SystemConf.create(values);
	}

	@Override
	public void updateMutableValues(Map<String, String> values) throws ServerFault {
		rbac.check(BasicRoles.ROLE_MANAGE_SYSTEM_CONF);

		ParametersValidator.notNull(values);

		SystemConf previous = getValues();
		SystemConfigurationHooks.getInstance().sanitize(previous, values);
		SystemConfigurationHooks.getInstance().validate(previous, values);
		Map<String, String> merged = SystemConf.merge(previous, values);

		JdbcAbstractStore.doOrFail(() -> {
			systemConfStore.update(merged);
			return null;
		});
		SystemConfigurationHooks.getInstance().fireUpdated(context, previous, getValues());
	}

}
