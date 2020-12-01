/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2019
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
package net.bluemind.cli.inject.node;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import net.bluemind.cli.cmd.api.CliContext;
import net.bluemind.cli.cmd.api.ICmdLet;
import net.bluemind.cli.cmd.api.ICmdLetRegistration;
import net.bluemind.cli.inject.common.MailExchangeInjector;
import net.bluemind.cli.utils.CliUtils;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.lib.vertx.VertxPlatform;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "node", description = "Injects a bunch of nodes operations")
public class NodeInjectCommand implements ICmdLet, Runnable {

	public static class Reg implements ICmdLetRegistration {

		@Override
		public Optional<String> group() {
			return Optional.of("inject");
		}

		@Override
		public Class<? extends ICmdLet> commandClass() {
			return NodeInjectCommand.class;
		}
	}

	@Parameters(paramLabel = "<domain_name>", description = "the domain (uid or alias)")
	public String domain;

	@Option(names = "--msg", description = "The number of messages to add (defaults to 100)")
	public int cycles = 100;

	@Option(names = "--workers", description = "number of workers for simultaneous operations")
	public int workers = 4;

	private CliContext ctx;

	@Override
	public void run() {
		CliUtils cli = new CliUtils(ctx);
		String domUid = cli.getDomainUidFromDomain(domain);
		if (domUid == null) {
			throw new ServerFault("domain " + domain + " not found");
		}
		try {
			VertxPlatform.spawnBlocking(20, TimeUnit.SECONDS);
			MailExchangeInjector inject = new NodeInjector(ctx, domUid);
			long time = System.currentTimeMillis();
			ctx.info("Starting injection of " + cycles + " message(s)");
			inject.runCycle(cycles, workers);
			ctx.info("Injection of " + cycles + " message(s) finished in " + (System.currentTimeMillis() - time)
					+ "ms.");
		} catch (Exception e) {
			e.printStackTrace();
			ctx.error(e.getMessage());
		}
	}

	@Override
	public Runnable forContext(CliContext ctx) {
		this.ctx = ctx;
		return this;
	}

}
