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
package net.bluemind.dataprotect.service;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import javax.sql.DataSource;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.caches.registry.CacheRegistry;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.core.rest.ServerSideServiceProvider;

public class BackupContext implements BmContext {

	private final javax.sql.DataSource pool;
	private final SecurityContext ctx;
	private final IServiceProvider provider;
	private final javax.sql.DataSource dataPool;

	public static class RestoreServiceProvider implements IServiceProvider {

		private IServiceProvider provider;

		public RestoreServiceProvider(IServiceProvider provider) {
			this.provider = provider;
		}

		@Override
		public <T> T instance(Class<T> interfaceClass, String... params) throws ServerFault {
			if (interfaceClass == CacheRegistry.class) {
				return null;
			} else {
				return provider.instance(interfaceClass, params);
			}
		}

	}

	public BackupContext(javax.sql.DataSource ds, DataSource dataPool, SecurityContext ctx) {
		this.pool = ds;
		this.dataPool = dataPool;
		this.ctx = ctx;
		this.provider = new RestoreServiceProvider(ServerSideServiceProvider.getProvider(this));
	}

	public String dataSourceLocation(DataSource ds) {
		if (ds == pool) {
			return "dir";
		} else if (ds == dataPool) {
			return "dataPool";
		} else {
			return null;
		}
	}

	@Override
	public BmContext su() {
		return new BackupContext(pool, dataPool, SecurityContext.SYSTEM);
	}

	@Override
	public IServiceProvider provider() {
		return provider;
	}

	@Override
	public IServiceProvider getServiceProvider() {
		return provider();
	}

	@Override
	public javax.sql.DataSource getDataSource() {
		return pool;
	}

	@Override
	public SecurityContext getSecurityContext() {
		return ctx;
	}

	@Override
	public BmContext su(String userUid, String domainUid) {
		return su(null, userUid, domainUid);
	}

	@Override
	public BmContext su(String sid, String userUid, String domainUid) {
		return null;
	}

	@Override
	public BmContext withRoles(Set<String> roles) {
		return this;
	}

	@Override
	public DataSource getMailboxDataSource(String datalocation) {
		return dataPool;
	}

	@Override
	public List<DataSource> getAllMailboxDataSource() {
		return Arrays.asList(dataPool);
	}

}
