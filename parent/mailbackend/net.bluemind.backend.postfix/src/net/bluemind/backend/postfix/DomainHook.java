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

package net.bluemind.backend.postfix;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;

import net.bluemind.backend.postfix.internal.maps.events.EventProducer;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.BmContext;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.api.DomainSettingsKeys;
import net.bluemind.domain.hook.DomainHookAdapter;
import net.bluemind.mailbox.service.IInCoreMailboxes;

public class DomainHook extends DomainHookAdapter {
	@Override
	public void onSettingsUpdated(BmContext context, ItemValue<Domain> domain, Map<String, String> previousSettings,
			Map<String, String> currentSettings) throws ServerFault {
		for (String setting : Arrays.asList(DomainSettingsKeys.mail_routing_relay.name(),
				DomainSettingsKeys.mail_forward_unknown_to_relay.name())) {
			String previousSetting = previousSettings == null ? null : previousSettings.get(setting);
			String currentSetting = currentSettings == null ? null : currentSettings.get(setting);

			if ((previousSetting == null && currentSetting != null)
					|| (previousSetting != null && currentSetting == null)
					|| (previousSetting != null && !previousSetting.equals(currentSetting))) {
				EventProducer.dirtyMaps();
				break;
			}
		}
	}

	@Override
	public void onAliasesUpdated(BmContext context, ItemValue<Domain> domain, Set<String> previousAliases)
			throws ServerFault {
		Set<String> prev = previousAliases == null ? Collections.emptySet() : previousAliases;
		Set<String> current = domain.value.aliases == null ? Collections.emptySet() : domain.value.aliases;

		SetView<String> deleted = Sets.difference(prev, current);
		SetView<String> added = Sets.difference(current, prev);

		if (!deleted.isEmpty()) {
			IInCoreMailboxes mailboxes = context.su().provider().instance(IInCoreMailboxes.class, domain.uid);
			deleted.forEach(alias -> {
				mailboxes.deleteEmailByAlias(alias);
			});
		}

		if (!added.isEmpty() || !deleted.isEmpty()) {
			EventProducer.dirtyMaps();
		}

	}
}
