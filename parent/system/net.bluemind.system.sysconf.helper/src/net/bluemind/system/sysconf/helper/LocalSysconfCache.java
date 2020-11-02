/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2020
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
package net.bluemind.system.sysconf.helper;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.system.api.ISystemConfiguration;
import net.bluemind.system.api.SystemConf;
import net.bluemind.system.hook.ISystemConfigurationObserver;

public class LocalSysconfCache {

	private static final Logger logger = LoggerFactory.getLogger(LocalSysconfCache.class);

	private LocalSysconfCache() {

	}

	private static class Holder {
		Optional<SystemConf> current = Optional.empty();
	}

	private static final Holder HELD = new Holder();

	private static SystemConf firstLoad() {
		ISystemConfiguration confApi = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(ISystemConfiguration.class);

		SystemConf ret = confApi.getValues();
		HELD.current = Optional.of(ret);
		logger.info("Initial load of {}", ret);
		return ret;
	}

	public static SystemConf get() {
		return HELD.current.orElseGet(LocalSysconfCache::firstLoad);
	}

	public static class Updater implements ISystemConfigurationObserver {
		@Override
		public void onUpdated(BmContext context, SystemConf previous, SystemConf conf) throws ServerFault {
			HELD.current = Optional.of(conf);
			logger.info("Configuration updated to {}", conf);
		}
	}

}
