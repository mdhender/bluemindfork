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
	private static JdbcActivator INSTANCE;
	private DataSource dataSource;
	private Map<String, DataSource> mailboxDataSource = new HashMap<String, DataSource>();
	private String schemaName;

	@Override
	public void start(BundleContext context) throws Exception {
		JdbcActivator.INSTANCE = this;
		try {
			setDataSource(BMPoolActivator.getDefault().defaultPool().getDataSource());
		} catch (Exception e) {
			logger.error("error during pool starting", e);
		}
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		dataSource = null;
		JdbcActivator.INSTANCE = null;
	}

	public static JdbcActivator getInstance() {
		return INSTANCE;
	}

	public DataSource getDataSource() {
		return dataSource;
	}

	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
		ServerSideServiceProvider.defaultDataSource = dataSource;
		PromiseServiceProvider.defaultDataSource = dataSource;
	}

	public void setMailboxDataSource(Map<String, DataSource> mailboxDataSource) {
		this.mailboxDataSource = mailboxDataSource;
		ServerSideServiceProvider.mailboxDataSource = mailboxDataSource;
	}

	public DataSource getMailboxDataSource(String datalocation) {
		return mailboxDataSource.get(datalocation);
	}

	public void addMailboxDataSource(String dataLocation, DataSource ds) {
		mailboxDataSource.put(dataLocation, ds);
		ServerSideServiceProvider.mailboxDataSource = mailboxDataSource;
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
