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
package net.bluemind.cli.mapi;

import java.util.Optional;

import net.bluemind.cli.cmd.api.CliContext;
import net.bluemind.cli.cmd.api.ICmdLet;
import net.bluemind.cli.cmd.api.ICmdLetRegistration;
import net.bluemind.cli.user.LoggingCommand;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "logging", description = "Enable/Disable per-user MAPI logs")
public class MapiLoggingCommand implements ICmdLet, Runnable {

	public static class Reg implements ICmdLetRegistration {

		@Override
		public Optional<String> group() {
			return Optional.of("mapi");
		}

		@Override
		public Class<? extends ICmdLet> commandClass() {
			return MapiLoggingCommand.class;
		}
	}

	private CliContext ctx;

	@Parameters(paramLabel = "<email>", description = "email address")
	public String target;

	@Option(names = "--enable", description = "Enable the per-user logs (disable if not specified)")
	public boolean enable = false;

	@Override
	public void run() {
		LoggingCommand userLoggingCommand = new LoggingCommand();
		userLoggingCommand.enable = enable;
		userLoggingCommand.endpoint = LoggingCommand.Endpoint.MAPI;
		userLoggingCommand.forTarget(target);
		userLoggingCommand.forContext(ctx);
		userLoggingCommand.run();
	}

	@Override
	public Runnable forContext(CliContext ctx) {
		this.ctx = ctx;
		return this;
	}
}
