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
package net.bluemind.forest.instance.cli;

import java.util.Optional;

import net.bluemind.cli.cmd.api.CliContext;
import net.bluemind.cli.cmd.api.ICmdLet;
import net.bluemind.cli.cmd.api.ICmdLetRegistration;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(name = "join", description = "Join bluemind instance to forest servers")
public class JoinCommand implements ICmdLet, Runnable {

	public static class Reg implements ICmdLetRegistration {

		@Override
		public Optional<String> group() {
			return Optional.of("forest");
		}

		@Override
		public Class<? extends ICmdLet> commandClass() {
			return JoinCommand.class;
		}

	}

	private CliContext ctx;

	@Parameters(description = "forest id (sub-directory in zookeeper)")
	public String forest;

	@Override
	public void run() {
		ctx.info("Should join " + forest);

	}

	@Override
	public Runnable forContext(CliContext ctx) {
		this.ctx = ctx;
		return this;
	}

}
