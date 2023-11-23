/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2023
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

package net.bluemind.core.auditlogs.client.es.hooks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.auditlogs.client.es.DataStreamCache;
import net.bluemind.core.auditlogs.client.loader.AuditLogLoader;
import net.bluemind.core.auditlogs.client.loader.config.AuditLogStoreConfig;
import net.bluemind.core.auditlogs.exception.AuditLogCreationException;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.BmContext;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.hook.DomainHookAdapter;

public class DataStreamDomainsHook extends DomainHookAdapter {
	private static Logger logger = LoggerFactory.getLogger(DataStreamDomainsHook.class);

	@Override
	public void onCreated(BmContext context, ItemValue<Domain> domain) {
		DataStreamCache dataStreamCache = DataStreamCache.get(context);
		AuditLogLoader auditLogProvider = new AuditLogLoader();
		logger.info("Create auditlog store for domain: '{}'", domain.uid);
		try {

			String dataStreamName = AuditLogStoreConfig.resolveDataStreamName(domain.uid);
			auditLogProvider.getManager().setupAuditBackingStoreForDomain(domain.uid);
			dataStreamCache.put(dataStreamName, dataStreamName);
		} catch (AuditLogCreationException e) {
			logger.error("Failed to create auditlog store for domain '{}': {}", domain.uid, e.getMessage());
		}
	}

	@Override
	public void onDeleted(BmContext context, ItemValue<Domain> domain) {
		DataStreamCache dataStreamCache = DataStreamCache.get(context);
		String dataStreamName = AuditLogStoreConfig.resolveDataStreamName(domain.uid);
		AuditLogLoader auditLogProvider = new AuditLogLoader();
		logger.info("Remove auditlog store for domain: '{}'", domain.uid);
		auditLogProvider.getManager().removeAuditBackingStoreForDomain(domain.uid);
		dataStreamCache.invalidate(dataStreamName);
	}
}
