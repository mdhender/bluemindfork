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
package net.bluemind.cli.index;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import net.bluemind.cli.cmd.api.CliContext;
import net.bluemind.cli.cmd.api.ICmdLet;
import net.bluemind.cli.cmd.api.ICmdLetRegistration;
import net.bluemind.cli.utils.Tasks;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.api.IDomains;
import net.bluemind.mailbox.api.IMailboxMgmt;
import net.bluemind.mailbox.api.IMailboxes;
import net.bluemind.mailbox.api.ShardStats;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "move", description = "Move a mailbox to a target index")
public class MoveCommand implements ICmdLet, Runnable {

	public static class Reg implements ICmdLetRegistration {

		@Override
		public Optional<String> group() {
			return Optional.of("index");
		}

		@Override
		public Class<? extends ICmdLet> commandClass() {
			return MoveCommand.class;
		}
	}

	private CliContext ctx;

	@Parameters(paramLabel = "<mailbox_uid>", description = "the mailbox uid")
	public String mailbox;

	@Option(names = "--dest", description = "index destination. Will select the smallest index if not specified")
	public String dest;

	@Override
	public void run() {

		IDomains domApi = ctx.adminApi().instance(IDomains.class);

		ItemValue<Domain> domain = null;

		for (ItemValue<Domain> dom : domApi.all()) {
			IMailboxes mboxApi = ctx.adminApi().instance(IMailboxes.class, dom.uid);
			if (mboxApi.getComplete(mailbox) != null) {
				domain = dom;
				break;
			}
		}

		if (domain == null) {
			throw new ServerFault("Mailbox not found");
		}

		if (dest == null || dest.isEmpty()) {
			ctx.info("looking up stats to figure out most suitable target for {}", mailbox);
			IMailboxMgmt mboxMgmtApi = ctx.longRequestTimeoutAdminApi().instance(IMailboxMgmt.class, "global.virt");
			List<ShardStats> shards = mboxMgmtApi.getShardsStats();
			ShardStats shard = null;
			Iterator<ShardStats> it = shards.iterator();
			while (it.hasNext()) {
				ShardStats s = it.next();
				if (shard == null || s.size < shard.size) {
					shard = s;
				}
			}

			if (shard == null) {
				// do nothing
				ctx.error("Cannot find destination shard. Do nothing.");
				return;
			}

			dest = shard.indexName;

			ctx.info("Moved index from {} to {}", mailbox, dest);
		}
		IMailboxMgmt mboxMgmtApi = ctx.adminApi().instance(IMailboxMgmt.class, domain.uid);
		TaskRef tr = mboxMgmtApi.moveIndex(mailbox, dest, true);
		Tasks.follow(ctx, tr, "", String.format("Failed to move index from %s to %s", mailbox, dest));
	}

	@Override
	public Runnable forContext(CliContext ctx) {
		this.ctx = ctx;
		return this;
	}

}
