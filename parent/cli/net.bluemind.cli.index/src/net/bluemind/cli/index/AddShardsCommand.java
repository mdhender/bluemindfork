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

import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import net.bluemind.cli.cmd.api.CliContext;
import net.bluemind.cli.cmd.api.ICmdLet;
import net.bluemind.cli.cmd.api.ICmdLetRegistration;
import net.bluemind.cli.utils.CliUtils;
import net.bluemind.cli.utils.Tasks;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.mailbox.api.IMailboxMgmt;
import net.bluemind.mailbox.api.ShardStats;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "add-mailspools", description = "Rebalance mailbox indices on fresh shards")
public class AddShardsCommand implements ICmdLet, Runnable {

	@Option(names = "--rebalance", description = "rebalance existing indices")
	public boolean rebalance = false;

	@Parameters(paramLabel = "<count>", description = "how many mailspool_xx should we add (default: 5)")
	public int expands = 5;

	@Option(names = "--domain", description = "domain for mailboxes")
	public String domain = "global.virt";

	private CliContext ctx;

	@Override
	public void run() {

		CliUtils utils = new CliUtils(ctx);
		String domUid = utils.getDomainUidByDomain(domain);

		if (expands > 20) {
			ctx.error(
					"Adding more than 20 indices at a time is not possible. Having too many indices can be an issue too.");
			System.exit(1);
		}

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
		int tgtBoxCount = totalBoxes / (existing.size() + expands);
		ctx.info("average box count: {}, target after rebalance: {}", avgBoxCount, tgtBoxCount);

		int loops = Math.max(1, avgBoxCount - tgtBoxCount);
		ctx.info("We should loop {} time(s) for rebalancing.", loops);

		String[] targets = IntStream.rangeClosed(startCount + 1, startCount + 1 + expands)
				.mapToObj(i -> "mailspool_" + i).toArray(String[]::new);

		List<ArrayDeque<String>> moveQueues = existing.stream().map(ss -> {
			// build a list a mailboxes with the biggest on top
			ArrayDeque<String> toMove = new ArrayDeque<>(loops);
			Iterator<String> remaining = ss.mailboxes.iterator();
			while (remaining.hasNext() && toMove.size() < loops) {
				toMove.add(remaining.next());
			}
			return toMove;
		}).collect(Collectors.toList());

		mboxMgmt = ctx.longRequestTimeoutAdminApi().instance(IMailboxMgmt.class, domUid);

		int cnt = 0;
		for (int i = 0; i < loops; i++) {
			for (ArrayDeque<String> src : moveQueues) {
				if (src.isEmpty()) {
					continue;
				}
				String tgt = targets[++cnt % targets.length];
				String mbox = src.poll();
				ctx.info("[" + (i + 1) + "/" + loops + "] Move mailbox " + mbox + " to " + tgt + " (" + src.size()
						+ " remaining)");

				if (rebalance) {
					try {
						TaskRef ref = mboxMgmt.moveIndex(mbox, tgt);
						Tasks.follow(ctx, ref, String.format("Failed to move index from %s to %s", mbox, tgt));
					} catch (Exception e) {
						ctx.warn("WARN rebalancing failed for " + mbox + ": " + e.getMessage());
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
			return AddShardsCommand.class;
		}
	}

}
