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

import net.bluemind.cli.cmd.api.CliContext;
import net.bluemind.cli.cmd.api.ICmdLet;
import net.bluemind.cli.cmd.api.ICmdLetRegistration;
import net.bluemind.cli.utils.Tasks;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.system.api.IInstallation;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

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

	@Parameters(paramLabel = "<index_name>", description = "target index (mailspool, event, contact, etc)")
	public String index;

	private CliContext ctx;

	@Override
	public void run() {
		ctx.info("Resetting index {}...", index);
		long time = System.currentTimeMillis();
		IInstallation instApi = ctx.adminApi().instance(IInstallation.class);
		TaskRef ref = instApi.resetIndex(index);
		Tasks.follow(ctx, ref, "", "Cannot reset index " + index);
		time = System.currentTimeMillis() - time;
		ctx.info("{} reseted in {}ms", index, time);
	}

	@Override
	public Runnable forContext(CliContext ctx) {
		this.ctx = ctx;
		return this;
	}

}
