/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2016
  *
  * This file is part of Blue Mind. Blue Mind is a messaging and collaborative
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
package net.bluemind.monitoring.handler.system;

import java.util.List;
import java.util.stream.Collectors;

import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.mailbox.api.IMailboxMgmt;
import net.bluemind.mailbox.api.ShardStats;
import net.bluemind.mailbox.api.ShardStats.State;
import net.bluemind.monitoring.api.FetchedData;
import net.bluemind.monitoring.api.ServerInformation;
import net.bluemind.monitoring.api.Status;
import net.bluemind.monitoring.service.util.Service;
import net.bluemind.server.api.Server;

public final class MailIndex extends Service {

	private static MailIndex instance;

	private MailIndex() {
		super("system", "mailindex");
		this.endpoints.add("load");
	}

	public static MailIndex getInstance() {
		if (MailIndex.instance == null) {
			MailIndex.instance = new MailIndex();
		}

		return MailIndex.instance;
	}

	@Override
	public ServerInformation getServerInfo(Server server, String endpoint) {
		switch (endpoint) {
		case "load":
			return this.load(server);
		}
		return null;

	}

	private ServerInformation load(Server server) {

		ServerInformation mailIndexStatus = new ServerInformation(server, "system", "mailindex", "load");

		mailIndexStatus.plugin = SystemHandler.BASE;
		mailIndexStatus.service = "mailindex";
		mailIndexStatus.status = Status.WARNING;

		try {
			IMailboxMgmt mailboxMgmt = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
					.instance(IMailboxMgmt.class, "global.virt");

			List<ShardStats> shardsStats = mailboxMgmt.getShardsStats();
			if (shardsStats.stream().anyMatch(sh -> sh.state == State.FULL)) {
				mailIndexStatus.status = Status.WARNING;
			} else {
				mailIndexStatus.status = Status.OK;
			}

			mailIndexStatus.dataList = shardsStats.stream().map(sh -> new FetchedData(sh.indexName, "" + sh.docCount))
					.collect(Collectors.toList());

			shardsStats.stream().filter(sh -> sh.state == State.FULL)//
					.forEach(sh -> mailIndexStatus.addMessage(String.format("index %s is full ( docCount %d , size %d )",
							sh.indexName, sh.docCount, sh.size)));

		} catch (Exception e) {
			logger.error("error retrieving cpu status ", e);
		}

		return mailIndexStatus;
	}

}
