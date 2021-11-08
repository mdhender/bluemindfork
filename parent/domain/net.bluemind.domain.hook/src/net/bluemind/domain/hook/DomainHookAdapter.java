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
package net.bluemind.domain.hook;

import java.util.Map;
import java.util.Set;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.BmContext;
import net.bluemind.domain.api.Domain;

public class DomainHookAdapter implements IDomainHook {

	@Override
	public void onCreated(BmContext context, ItemValue<Domain> domain) throws ServerFault {
		// empty impl
	}

	@Override
	public void onUpdated(BmContext context, ItemValue<Domain> previousValue, ItemValue<Domain> domain)
			throws ServerFault {
		// empty impl
	}

	@Override
	public void onSettingsUpdated(BmContext context, ItemValue<Domain> domain, Map<String, String> previousSettings,
			Map<String, String> currentSettings) throws ServerFault {
		// empty impl
	}

	@Override
	public void onDeleted(BmContext context, ItemValue<Domain> domain) throws ServerFault {
		// empty impl
	}

	@Override
	public void onAliasesUpdated(BmContext context, ItemValue<Domain> domain, Set<String> previousAliases)
			throws ServerFault {
		// empty impl
	}

	@Override
	public void onBeforeDelete(BmContext context, ItemValue<Domain> domain) throws ServerFault {
		// empty impl
	}

	@Override
	public void onDomainItemsDeleted(BmContext context, ItemValue<Domain> domain) throws ServerFault {
		// empty impl
	}

}
