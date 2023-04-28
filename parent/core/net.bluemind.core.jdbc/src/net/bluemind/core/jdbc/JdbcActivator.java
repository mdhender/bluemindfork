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
package net.bluemind.core.jdbc;

import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.rest.PromiseServiceProvider;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.pool.BMPoolActivator;

public class JdbcActivator implements BundleActivator {

	private static final Logger logger = LoggerFactory.getLogger(JdbcActivator.class);
	private static JdbcActivator instance;
	private DataSource dataSource;
	private Map<String, DataSource> mailboxDataSource = new HashMap<>();
	private String schemaName;

	private synchronized void setInstance(JdbcActivator jdbcactivator) {
		JdbcActivator.instance = jdbcactivator;
	}

	public static synchronized JdbcActivator getInstance() {
		return instance;
	}

	@Override
	public void start(BundleContext context) throws Exception {
		setInstance(this);
		try {
			setDataSource(BMPoolActivator.getDefault().defaultPool().getDataSource());
		} catch (Exception e) {
			logger.error("error during pool starting", e);
		}
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		dataSource = null;
		setInstance(null);
	}

	public DataSource getDataSource() {
		return dataSource;
	}

	public Map<String, DataSource> getMailboxDataSource() {
		return mailboxDataSource;
	}

	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
		ServerSideServiceProvider.defaultDataSource = dataSource;
		PromiseServiceProvider.defaultDataSource = dataSource;
	}

	public void setMailboxDataSource(Map<String, DataSource> mailboxDataSource) {
		this.mailboxDataSource = mailboxDataSource;
		ServerSideServiceProvider.mailboxDataSource = mailboxDataSource;
		PromiseServiceProvider.mailboxDataSource = mailboxDataSource;
	}

	public DataSource getMailboxDataSource(String datalocation) {
		return mailboxDataSource.get(datalocation);
	}

	public void addMailboxDataSource(String dataLocation, DataSource ds) {
		mailboxDataSource.put(dataLocation, ds);
		ServerSideServiceProvider.mailboxDataSource = mailboxDataSource;
		PromiseServiceProvider.mailboxDataSource = mailboxDataSource;
	}

	public void restartDataSource() throws Exception {
		BMPoolActivator.getDefault().restartDefaultPool();
		setDataSource(BMPoolActivator.getDefault().defaultPool().getDataSource());
	}

	public String getSchemaName() {
		return schemaName;
	}

	public void setSchemaName(String schemaName) {
		this.schemaName = schemaName;
	}
}
