/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2022
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
package net.bluemind.cli.index.ring;

import java.util.Optional;

import net.bluemind.cli.cmd.api.CliContext;
import net.bluemind.cli.cmd.api.ICmdLet;
import net.bluemind.cli.cmd.api.ICmdLetRegistration;
import net.bluemind.cli.utils.CliUtils;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "add-mailspool", description = "Add a new mailspool index")
public class AddMailspoolIndexCommand implements ICmdLet, Runnable {

	@Option(names = "--numericIndex", description = "Numeric position in the ring")
	public int numericIndex;

	private CliContext ctx;

	@Override
	public void run() {
		CliUtils utils = new CliUtils(ctx);

	}

	@Override
	public Runnable forContext(CliContext ctx) {
		this.ctx = ctx;
		return this;
	}

	public static class Reg implements ICmdLetRegistration {

		@Override
		public Optional<String> group() {
			return Optional.of("index-ring");
		}

		@Override
		public Class<? extends ICmdLet> commandClass() {
			return AddMailspoolIndexCommand.class;
		}
	}

}
