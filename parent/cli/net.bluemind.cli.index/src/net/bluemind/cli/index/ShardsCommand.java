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

import java.util.List;
import java.util.Optional;

import io.vertx.core.json.JsonArray;
import net.bluemind.cli.cmd.api.CliContext;
import net.bluemind.cli.cmd.api.ICmdLet;
import net.bluemind.cli.cmd.api.ICmdLetRegistration;
import net.bluemind.core.utils.JsonUtils;
import net.bluemind.mailbox.api.IMailboxMgmt;
import net.bluemind.mailbox.api.ShardStats;
import picocli.CommandLine.Command;

/**
 * This is the defaut command on index related stuff to ensure we don't run a
 * destructive op by default
 *
 */
@Command(name = "shards", description = "Show index sharding statistics")
public class ShardsCommand implements ICmdLet, Runnable {

	public static class Reg implements ICmdLetRegistration {

		@Override
		public Optional<String> group() {
			return Optional.of("index");
		}

		@Override
		public Class<? extends ICmdLet> commandClass() {
			return ShardsCommand.class;
		}

	}

	private CliContext ctx;

	@Override
	public void run() {
		IMailboxMgmt mgmtApi = ctx.adminApi().instance(IMailboxMgmt.class, "global.virt");
		List<ShardStats> shardStats = mgmtApi.getShardsStats();
		JsonArray js = new JsonArray(JsonUtils.asString(shardStats));
		System.out.println(js.encodePrettily());
	}

	@Override
	public Runnable forContext(CliContext ctx) {
		this.ctx = ctx;
		return this;
	}

}
