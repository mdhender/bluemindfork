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

import java.util.Optional;

import io.airlift.airline.Arguments;
import io.airlift.airline.Command;
import net.bluemind.cli.cmd.api.CliContext;
import net.bluemind.cli.cmd.api.ICmdLet;
import net.bluemind.cli.cmd.api.ICmdLetRegistration;
import net.bluemind.system.api.IInstallation;

/**
 * Delete then create an index and its mapping
 *
 */
@Command(name = "reset", description = "Reset one index")
public class ResetCommand implements ICmdLet, Runnable {

	public static class Reg implements ICmdLetRegistration {

		@Override
		public Optional<String> group() {
			return Optional.of("index");
		}

		@Override
		public Class<? extends ICmdLet> commandClass() {
			return ResetCommand.class;
		}

	}

	@Arguments(required = true, description = "target index (mailspool, event, contact, etc)")
	public String index;

	private CliContext ctx;

	@Override
	public void run() {
		System.out.println("Resetting index " + index + "...");
		long time = System.currentTimeMillis();
		IInstallation instApi = ctx.adminApi().instance(IInstallation.class);
		instApi.resetIndex(index);
		time = System.currentTimeMillis() - time;
		System.out.println("" + index + "reset in " + time + "ms.");
	}

	@Override
	public Runnable forContext(CliContext ctx) {
		this.ctx = ctx;
		return this;
	}

}
