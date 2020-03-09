/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2017
  *
  * This file is part of BlueMind. BlueMind is a messaging and collaborative
  * solution.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of either the GNU Affero General Public License as
  * published by the Free Software Foundation (version 3 of the License).
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
import java.util.Optional;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.caches.registry.CacheRegistry;
import net.bluemind.core.caches.registry.ICacheRegistration;
import net.bluemind.core.rest.BmContext;

public class DataSourceRouter {

	private static final Logger logger = LoggerFactory.getLogger(DataSourceRouter.class);
	private static final Cache<String, Optional<String>> cache = CacheBuilder.newBuilder().build();

	public static class CacheReg implements ICacheRegistration {

		@Override
		public void registerCaches(CacheRegistry cr) {
			cr.register("datasource-router-locations", cache);
		}

	}

	public static DataSource get(BmContext context, String containerUid) {

		Optional<String> loc = cache.getIfPresent(containerUid);
		if (loc == null) {
			ContainerStore directoryContainerStore = new ContainerStore(context, context.getDataSource(),
					context.getSecurityContext());
			try {
				loc = directoryContainerStore.getContainerLocation(containerUid);
				if (loc != null) {
					cache.put(containerUid, loc);
				}
			} catch (SQLException e) {
				throw ServerFault.sqlFault(e);
			}
		}
		if (logger.isDebugEnabled()) {
			logger.debug("{} for {}", loc, containerUid);
		}
		return loc == null ? context.getDataSource()
				: loc.map(l -> context.getMailboxDataSource(l)).orElse(context.getDataSource());
	}

	public static String location(BmContext context, String containerUid) {

		Optional<String> loc = cache.getIfPresent(containerUid);
		if (loc == null) {
			ContainerStore directoryContainerStore = new ContainerStore(context, context.getDataSource(),
					context.getSecurityContext());
			try {
				loc = directoryContainerStore.getContainerLocation(containerUid);
				if (loc != null) {
					cache.put(containerUid, loc);
				}
			} catch (SQLException e) {
				throw ServerFault.sqlFault(e);
			}
		}
		return loc == null ? null : loc.orElse(null);
	}

	public static void invalidateContainer(String containerUid) {
		cache.invalidate(containerUid);
	}

}
