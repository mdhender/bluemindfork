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
import net.bluemind.core.container.persistance.DataSourceRouter;
import net.bluemind.core.container.persistance.OfflineMgmtStore;
import net.bluemind.core.jdbc.JdbcAbstractStore;
import net.bluemind.core.rest.BmContext;

public class OfflineMgmtService implements IOfflineMgmt {

	private static final Logger logger = LoggerFactory.getLogger(OfflineMgmtService.class);
	private final BmContext context;

	public OfflineMgmtService(BmContext context, String ownerUid, String domainUid) {
		this.context = context;
		logger.info("Created for {} {}", ownerUid, domainUid);
	}

	@Override
	public IdRange allocateOfflineIds(int idCount) {
		String uidOnDataDS = "calendar:Default:" + context.getSecurityContext().getSubject();
		DataSource ds = DataSourceRouter.get(context, uidOnDataDS);
		DataSource defaultDs = context.getDataSource();
		logger.info("Allocating IDs using ds {}, default is {} (same {})", ds, defaultDs, ds == defaultDs);
		OfflineMgmtStore store = new OfflineMgmtStore(ds);
		long startValue = JdbcAbstractStore.doOrFail(() -> store.reserveItemIds(idCount));
		IdRange ir = new IdRange();
		ir.count = idCount;
		ir.globalCounter = startValue;
		logger.info("Allocated {} local replica ids, starting at {}", idCount, startValue);
		return ir;
	}

}
