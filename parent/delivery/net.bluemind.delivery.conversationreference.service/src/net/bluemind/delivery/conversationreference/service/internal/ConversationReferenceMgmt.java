/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2022
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
package net.bluemind.delivery.conversationreference.service.internal;

import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.rest.BmContext;
import net.bluemind.delivery.conversationreference.persistence.ConversationReferenceStore;
import net.bluemind.delivery.conversationreference.service.IInCoreConversationReferenceMgmt;

public class ConversationReferenceMgmt implements IInCoreConversationReferenceMgmt {

	private static Logger logger = LoggerFactory.getLogger(ConversationReferenceMgmt.class);

	private final BmContext context;

	public ConversationReferenceMgmt(BmContext ctx) {
		this.context = ctx;
	}

	@Override
	public long deleteEntriesOlderThanOneYear() throws ServerFault {
		long totalDeletedRecords = 0L;
		var datasources = context.getAllMailboxDataSource();
		for (var datasource : datasources) {
			var store = new ConversationReferenceStore(datasource);

			try {
				long deletedRecords = store.deleteEntriesOlderThanOneYear();
				totalDeletedRecords += deletedRecords;
				logger.info("cleaned {} conversation references on {}", deletedRecords, datasource.getConnection());
			} catch (SQLException e) {
				throw ServerFault.sqlFault(e);
			}
		}
		return totalDeletedRecords;
	}
}
