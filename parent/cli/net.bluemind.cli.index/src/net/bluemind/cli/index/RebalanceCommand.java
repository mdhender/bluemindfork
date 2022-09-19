/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2020
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
package net.bluemind.cli.index;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import net.bluemind.cli.cmd.api.CliContext;
import net.bluemind.cli.cmd.api.ICmdLet;
import net.bluemind.cli.cmd.api.ICmdLetRegistration;
import net.bluemind.cli.utils.CliUtils;
import net.bluemind.cli.utils.Tasks;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.mailbox.api.IMailboxMgmt;
import net.bluemind.mailbox.api.ShardStats;
import net.bluemind.mailbox.api.ShardStats.MailboxStats;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "rebalance", description = "Rebalance mailspool indices")
public class RebalanceCommand implements ICmdLet, Runnable {

	@Option(names = "--domain", description = "domain for mailboxes")
	public String domain = "global.virt";

	@Option(names = "--apply", description = "Run the operations on the indices (we default to dry mode)")
	public boolean rebalance = false;

	private CliContext ctx;

	@Override
	public void run() {

		CliUtils utils = new CliUtils(ctx);
		String domUid = utils.getDomainUidByDomain(domain);

		IMailboxMgmt mboxMgmt = ctx.longRequestTimeoutAdminApi().instance(IMailboxMgmt.class, "global.virt");
		List<ShardStats> existing = mboxMgmt.getShardsStats();
		int startCount = 1;
		int totalBoxes = 0;
		for (ShardStats ss : existing) {
			int idxId = Integer.parseInt(ss.indexName.substring("mailspool_".length()));
			startCount = Math.max(startCount, idxId);
			totalBoxes += ss.mailboxes.size();

		}
		int avgBoxCount = totalBoxes / existing.size();
		ctx.info("average box count: {}", avgBoxCount);

		mboxMgmt = ctx.longRequestTimeoutAdminApi().instance(IMailboxMgmt.class, domUid);

		existing.sort((s1, s2) -> Long.compare(s2.size, s1.size));
		int mid = existing.size() / 2;
		List<ShardStats> sources = existing.subList(0, mid);
		List<ShardStats> targets = existing.subList(mid, existing.size());
		Collections.reverse(targets);

		Iterator<ShardStats> src = sources.iterator();
		Iterator<ShardStats> tgt = targets.iterator();

		int count = Math.min(sources.size(), targets.size());
		int loop = 0;
		while (src.hasNext() && tgt.hasNext()) {
			ShardStats source = src.next();
			ShardStats target = tgt.next();
			ctx.info("[" + (++loop) + "/" + count + "] operation starting.");
			if (source.topMailbox.isEmpty()) {
				continue;
			}
			long srcGb = source.size / 1024 / 1024 / 1024;
			long targetGb = target.size / 1024 / 1024 / 1024;
			if (Math.abs(srcGb - targetGb) < 1) {
				ctx.warn(source.indexName + " and " + target.indexName + " have similar size (" + srcGb + " vs "
						+ targetGb + ")");
				continue;
			}
			MailboxStats topSrc = source.topMailbox.get(0);
			ctx.info("From {} ({}GB) to {} ({}GB)", source.indexName, srcGb, target.indexName, targetGb);
			ctx.info("Move {} ({}) from {} to {}", topSrc.mailboxUid, topSrc.docCount, source.indexName,
					target.indexName);

			if (rebalance) {
				try {
					TaskRef ref = mboxMgmt.moveIndex(topSrc.mailboxUid, target.indexName, true);
					Tasks.follow(ctx, ref, "",
							String.format("Failed to move index from %s to %s", topSrc.mailboxUid, tgt));
				} catch (Exception e) {
					if (e.getMessage() != null) {
						ctx.warn("WARN " + e.getMessage());
					} else {
						ctx.warn("WARN exception occured", e);
					}
				}
			}
		}

	}

	@Override
	public Runnable forContext(CliContext ctx) {
		this.ctx = ctx;
		return this;
	}

	public static class Reg implements ICmdLetRegistration {

		@Override
		public Optional<String> group() {
			return Optional.of("index");
		}

		@Override
		public Class<? extends ICmdLet> commandClass() {
			return RebalanceCommand.class;
		}
	}

}
