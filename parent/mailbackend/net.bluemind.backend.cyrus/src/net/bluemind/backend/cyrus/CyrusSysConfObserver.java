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
package net.bluemind.backend.cyrus;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.BmContext;
import net.bluemind.mailbox.service.MailboxesStorageFactory;
import net.bluemind.server.api.IServer;
import net.bluemind.server.api.Server;
import net.bluemind.server.api.TagDescriptor;
import net.bluemind.system.api.SysConfKeys;
import net.bluemind.system.api.SystemConf;
import net.bluemind.system.hook.ISystemConfigurationObserver;

public class CyrusSysConfObserver implements ISystemConfigurationObserver {

	@Override
	public void onUpdated(BmContext context, SystemConf previous, SystemConf conf) throws ServerFault {
		boolean initialized = false;

		String prev = previous.stringValue(SysConfKeys.imap_max_child.name());
		String now = conf.stringValue(SysConfKeys.imap_max_child.name());

		// NOTE: Topololgy can't be used here, because during setup, the topology
		// is not available yet
		IServer serverApi = context.su().provider().instance(IServer.class, "default");
		List<ItemValue<Server>> backends = serverApi.allComplete().stream()
				.filter(ivs -> ivs.value.tags.contains(TagDescriptor.mail_imap.getTag())).toList();

		if (!StringUtils.equals(prev, now)) {
			initialized = true;

			for (ItemValue<Server> server : backends) {
				if (server.value.tags.contains("mail/imap")) {
					MailboxesStorageFactory.getMailStorage().initialize(context.su(), server);
				}
			}
		}

		// if the server has been initialized, the hsm config has already been written
		if (!initialized) {
			String prevArchiveKind = previous.stringValue(SysConfKeys.archive_kind.name());
			String prevHsmDays = previous.stringValue(SysConfKeys.archive_days.name());
			String prevHsmThreshold = previous.stringValue(SysConfKeys.archive_size_threshold.name());
			String prevRetention = previous.stringValue(SysConfKeys.cyrus_expunged_retention_time.name());

			String currentArchiveKind = conf.stringValue(SysConfKeys.archive_kind.name());
			String currentHsmDays = conf.stringValue(SysConfKeys.archive_days.name());
			String currentHsmThreshold = conf.stringValue(SysConfKeys.archive_size_threshold.name());
			String currentRetention = conf.stringValue(SysConfKeys.cyrus_expunged_retention_time.name());

			if ((!equals(prevArchiveKind, currentArchiveKind)) || (!equals(prevHsmDays, currentHsmDays))
					|| (!equals(prevHsmThreshold, currentHsmThreshold)) || (!equals(prevRetention, currentRetention))) {
				for (ItemValue<Server> server : backends) {
					MailboxesStorageFactory.getMailStorage().rewriteCyrusConfiguration(server.uid, true);
				}
			}
		}
	}

	private boolean equals(String s1, String s2) {
		return s1 != null ? s1.equals(s2) : s2 == null;
	}

}
