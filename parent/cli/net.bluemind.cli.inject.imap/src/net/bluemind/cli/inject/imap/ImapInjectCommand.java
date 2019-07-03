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
package net.bluemind.cli.inject.imap;

import java.util.Optional;

import io.airlift.airline.Arguments;
import io.airlift.airline.Command;
import io.airlift.airline.Option;
import net.bluemind.cli.cmd.api.CliContext;
import net.bluemind.cli.cmd.api.ICmdLet;
import net.bluemind.cli.cmd.api.ICmdLetRegistration;
import net.bluemind.cli.utils.CliUtils;
import net.bluemind.core.api.fault.ServerFault;

@Command(name = "imap", description = "Injects a batch of emails through IMAP")
public class ImapInjectCommand implements ICmdLet, Runnable {

	public static class Reg implements ICmdLetRegistration {

		@Override
		public Optional<String> group() {
			return Optional.of("inject");
		}

		@Override
		public Class<? extends ICmdLet> commandClass() {
			return ImapInjectCommand.class;
		}
	}

	@Arguments(required = true, description = "the domain (uid or alias)")
	public String domain;

	@Option(name = "msg", description = "The number of messages to add")
	public int cycles = 100;

	private CliContext ctx;

	@Override
	public void run() {
		CliUtils cli = new CliUtils(ctx);
		String domUid = cli.getDomainUidFromDomain(domain);
		if (domUid == null) {
			throw new ServerFault("domain " + domain + " not found");
		}
		ImapInjector inject = new ImapInjector(ctx.adminApi(), domUid);
		inject.runCycle(cycles);
	}

	@Override
	public Runnable forContext(CliContext ctx) {
		this.ctx = ctx;
		return this;
	}

}
