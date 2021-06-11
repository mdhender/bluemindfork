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
package net.bluemind.cli.inject.common;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.ImmutableMap;

import net.bluemind.cli.cmd.api.CliContext;
import net.bluemind.cli.cmd.api.ICmdLet;
import net.bluemind.cli.utils.CliUtils;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.lib.vertx.VertxPlatform;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

public abstract class AbstractMailInjectCommand implements ICmdLet, Runnable {

	private static final Map<String, IMessageProducer> prods = ImmutableMap.of(//
			"got1024", new GOTMessageProducer(1024), //
			"got128", new GOTMessageProducer(128), //
			"small", new SmallRandomMessageProducer());

	@Parameters(paramLabel = "<domain_name>", description = "the domain (uid or alias)")
	public String domain;

	@Option(names = "--msg", description = "The number of messages to add (defaults to 100)")
	public int cycles = 100;

	@Option(names = "--prod", description = "Random message producer (got1024 (default)), got128 or small)")
	public String producer = "got1024";

	@Option(names = "--workers", description = "number of workers for simultaneous operations")
	public int workers = 4;

	private CliContext ctx;

	@Override
	public void run() {
		CliUtils cli = new CliUtils(ctx);
		String domUid = cli.getDomainUidByDomain(domain);
		if (domUid == null) {
			throw new ServerFault("domain " + domain + " not found");
		}
		VertxPlatform.spawnBlocking(30, TimeUnit.SECONDS);
		try {
			IMessageProducer prod = Optional.ofNullable(prods.get(producer)).orElseGet(GOTMessageProducer::new);
			ctx.info("Producer " + prod + " selected.");
			MailExchangeInjector inject = createInjector(ctx, domUid, prod);
			long time = System.currentTimeMillis();
			ctx.info("Starting injection of " + cycles + " message(s) using " + prod);
			inject.runCycle(cycles, workers);
			ctx.info("Injection of " + cycles + " message(s) finished in " + (System.currentTimeMillis() - time)
					+ "ms.");
		} catch (Exception e) {
			e.printStackTrace();
			ctx.error(e.getMessage());
		}
	}

	protected abstract MailExchangeInjector createInjector(CliContext ctx, String domUid, IMessageProducer prod);

	@Override
	public Runnable forContext(CliContext ctx) {
		this.ctx = ctx;
		return this;
	}

}
