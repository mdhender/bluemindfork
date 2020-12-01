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
package net.bluemind.cli.metrics;

import java.util.Optional;

import net.bluemind.cli.cmd.api.CliContext;
import net.bluemind.cli.cmd.api.ICmdLet;
import net.bluemind.cli.cmd.api.ICmdLetRegistration;
import net.bluemind.config.InstallationId;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.server.api.IServer;
import net.bluemind.server.api.Server;
import net.bluemind.server.api.TagDescriptor;
import picocli.CommandLine.Command;

@Command(name = "status", description = "display the TICK stack status")
public class TickStatusCommand implements ICmdLet, Runnable {

	public static class Reg implements ICmdLetRegistration {

		@Override
		public Optional<String> group() {
			return Optional.of("tick");
		}

		@Override
		public Class<? extends ICmdLet> commandClass() {
			return TickStatusCommand.class;
		}

	}

	private CliContext ctx;

	public TickStatusCommand() {
	}

	@Override
	public Runnable forContext(CliContext ctx) {
		this.ctx = ctx;
		return this;
	}

	public void run() {

		IServer serversApi = ctx.adminApi().instance(IServer.class, InstallationId.getIdentifier());
		Optional<ItemValue<Server>> tickServer = serversApi.allComplete().stream()
				.filter(srvItem -> srvItem.value.tags.contains(TagDescriptor.bm_metrics_influx.name())).findAny();
		if (tickServer.isPresent()) {
			System.out.println(ctx.ansi().a("Tick deployement ").fgBrightGreen().a("OK").reset());
		} else {
			System.out.println(ctx.ansi().a("Tick is not deployed ").fgBrightRed().a("FAILED").reset());
		}

	}

}
