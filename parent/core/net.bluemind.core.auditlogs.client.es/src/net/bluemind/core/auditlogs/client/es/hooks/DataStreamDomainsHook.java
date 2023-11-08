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

import net.bluemind.core.auditlogs.client.loader.AuditLogLoader;
import net.bluemind.core.auditlogs.exception.DataStreamCreationException;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.BmContext;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.hook.DomainHookAdapter;

public class DataStreamDomainsHook extends DomainHookAdapter {
	private static Logger logger = LoggerFactory.getLogger(DataStreamDomainsHook.class);
	private static final String DATASTREAM_PREFIX = "audit_log";

	@Override
	public void onCreated(BmContext context, ItemValue<Domain> domain) {
		AuditLogLoader auditLogProvider = new AuditLogLoader();
		logger.info("Create datastream '{}' for domain: '{}'", DATASTREAM_PREFIX, domain.uid);
		try {
			auditLogProvider.getManager().createDataStreamForDomainIfNotExists(DATASTREAM_PREFIX, domain.uid);
		} catch (DataStreamCreationException e) {
			logger.error("Failed to create datastream '{}': {}", DATASTREAM_PREFIX + "_" + domain.uid, e.getMessage());
		}
	}

	@Override
	public void onDeleted(BmContext context, ItemValue<Domain> domain) {
		AuditLogLoader auditLogProvider = new AuditLogLoader();
		logger.info("Remove datastream '{}' for domain: '{}'", DATASTREAM_PREFIX, domain.uid);
		auditLogProvider.getManager().removeDatastreamForPrefixAndDomain(DATASTREAM_PREFIX, domain.uid);
	}
}
