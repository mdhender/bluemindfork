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
package net.bluemind.mailflow.service;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.BmContext;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.hook.DomainHookAdapter;
import net.bluemind.mailflow.api.IMailflowRules;
import net.bluemind.mailflow.service.internal.EmitMailflowEvent;

public class MailflowDomainHook extends DomainHookAdapter {

	private static final Logger logger = LoggerFactory.getLogger(MailflowDomainHook.class);

	@Override
	public void onBeforeDelete(BmContext context, ItemValue<Domain> domain) throws ServerFault {
		logger.info("Deleting all mailflow rules of domain {}", domain.uid);

		IMailflowRules service = context.provider().instance(IMailflowRules.class, domain.uid);
		service.listAssignments().forEach(assignment -> {
			service.delete(assignment.uid);
		});

		EmitMailflowEvent.invalidateDomainAliasCache(domain.uid);
	}

	@Override
	public void onUpdated(BmContext context, ItemValue<Domain> previousValue, ItemValue<Domain> domain)
			throws ServerFault {
		EmitMailflowEvent.invalidateDomainAliasCache(domain.uid);
	}

	@Override
	public void onAliasesUpdated(BmContext context, ItemValue<Domain> domain, Set<String> previousAliases)
			throws ServerFault {
		EmitMailflowEvent.invalidateDomainAliasCache(domain.uid);
	}

}
