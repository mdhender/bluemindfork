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
package net.bluemind.core.container.service.internal;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.container.api.IOfflineMgmt;
import net.bluemind.core.container.api.IdRange;
import net.bluemind.core.container.persistence.OfflineMgmtStore;
import net.bluemind.core.jdbc.JdbcAbstractStore;

public class OfflineMgmtService implements IOfflineMgmt {

	private static final Logger logger = LoggerFactory.getLogger(OfflineMgmtService.class);

	private final DataSource dataSource;

	public OfflineMgmtService(DataSource dataSource) {
		this.dataSource = dataSource;
		logger.info("Created with ds={}", dataSource);
	}

	@Override
	public IdRange allocateOfflineIds(int idCount) {
		OfflineMgmtStore store = new OfflineMgmtStore(dataSource);
		long startValue = JdbcAbstractStore.doOrFail(() -> store.reserveItemIds(idCount));
		logger.info("Allocated {} local replica ids, starting at {}", idCount, startValue);
		return IdRange.create(idCount, startValue);
	}

}
