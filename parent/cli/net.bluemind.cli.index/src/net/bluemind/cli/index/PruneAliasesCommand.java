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

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import net.bluemind.cli.cmd.api.CliContext;
import net.bluemind.cli.cmd.api.ICmdLet;
import net.bluemind.cli.cmd.api.ICmdLetRegistration;
import net.bluemind.cli.utils.CliUtils;
import net.bluemind.directory.api.IDirectory;
import net.bluemind.lib.elasticsearch.ESearchActivator;
import net.bluemind.mailbox.api.IMailboxMgmt;
import net.bluemind.mailbox.api.SimpleShardStats;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "prune-aliases", description = "Remove extra aliases from indices")
public class PruneAliasesCommand implements ICmdLet, Runnable {

	@Option(names = "--apply", description = "non-dry mode")
	public boolean apply = false;

	@Parameters(paramLabel = "<domain>", description = "Domain of the aliases")
	public String domain;

	private CliContext ctx;

	@Override
	public void run() {

		CliUtils utils = new CliUtils(ctx);
		String domUid = utils.getDomainUidByDomain(domain);

		IMailboxMgmt mboxMgmt = ctx.longRequestTimeoutAdminApi().instance(IMailboxMgmt.class, "global.virt");
		List<SimpleShardStats> existing = mboxMgmt.getLiteStats();
		IDirectory dirApi = ctx.adminApi().instance(IDirectory.class, domUid);
		for (SimpleShardStats ss : existing) {
			Set<String> aliasToPrune = new HashSet<>();
			for (String uid : ss.mailboxes) {
				if (dirApi.findByEntryUid(uid) == null) {
					aliasToPrune.add("mailspool_alias_" + uid);
				}
			}
			if (!aliasToPrune.isEmpty()) {
				if (apply) {
					ESearchActivator.getClient().admin().indices().prepareAliases()
							.removeAlias(ss.indexName, aliasToPrune.toArray(String[]::new)).execute().actionGet();
					ctx.info("Removed {} alias(es) from {} ({})", aliasToPrune.size(), ss.indexName, aliasToPrune);
				} else {
					ctx.info("Should remove {} alias(es) from {} ({})", aliasToPrune.size(), ss.indexName,
							aliasToPrune);
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
			return PruneAliasesCommand.class;
		}
	}

}
