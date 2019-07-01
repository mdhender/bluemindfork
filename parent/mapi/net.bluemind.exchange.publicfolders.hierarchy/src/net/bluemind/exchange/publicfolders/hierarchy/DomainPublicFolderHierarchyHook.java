/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2018
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
package net.bluemind.exchange.publicfolders.hierarchy;

import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.api.internal.IInternalContainersFlatHierarchyMgmt;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.hook.IDomainHook;
import net.bluemind.exchange.publicfolders.common.PublicFolders;

public class DomainPublicFolderHierarchyHook implements IDomainHook {

	private static final Logger logger = LoggerFactory.getLogger(DomainPublicFolderHierarchyHook.class);

	@Override
	public void onCreated(BmContext context, ItemValue<Domain> domain) throws ServerFault {
		logger.info("INIT public folders hierarchy for {}", domain);
		IInternalContainersFlatHierarchyMgmt chMgmt = ServerSideServiceProvider.getProvider(context).instance(
				IInternalContainersFlatHierarchyMgmt.class, domain.uid, PublicFolders.mailboxGuid(domain.uid));
		chMgmt.init();
	}

	@Override
	public void onDeleted(BmContext context, ItemValue<Domain> domain) throws ServerFault {
		logger.info("DELETE public folders hierarchy for {}", domain);
		IInternalContainersFlatHierarchyMgmt chMgmt = ServerSideServiceProvider.getProvider(context).instance(
				IInternalContainersFlatHierarchyMgmt.class, domain.uid, PublicFolders.mailboxGuid(domain.uid));
		chMgmt.delete();
	}

	@Override
	public void onUpdated(BmContext context, ItemValue<Domain> previousValue, ItemValue<Domain> domain)
			throws ServerFault {
		// TODO Auto-generated method stub

	}

	@Override
	public void onSettingsUpdated(BmContext context, ItemValue<Domain> domain, Map<String, String> previousSettings,
			Map<String, String> currentSettings) throws ServerFault {
		// TODO Auto-generated method stub

	}

	@Override
	public void onBeforeDelete(BmContext context, ItemValue<Domain> domain) throws ServerFault {
		// TODO Auto-generated method stub

	}

	@Override
	public void onAliasesUpdated(BmContext context, ItemValue<Domain> domain, Set<String> previousAliases)
			throws ServerFault {
		// TODO Auto-generated method stub

	}

}
