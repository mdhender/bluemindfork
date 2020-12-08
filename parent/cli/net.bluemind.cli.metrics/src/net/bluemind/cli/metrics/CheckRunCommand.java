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
import java.util.Set;

import net.bluemind.cli.cmd.api.CliContext;
import net.bluemind.cli.cmd.api.ICmdLet;
import net.bluemind.cli.cmd.api.ICmdLetRegistration;
import net.bluemind.cli.utils.Tasks;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.core.task.api.TaskStatus;
import net.bluemind.metrics.alerts.api.CheckResult;
import net.bluemind.metrics.alerts.api.IProductChecks;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(name = "run", description = "display the TICK stack status")
public class CheckRunCommand implements ICmdLet, Runnable {

	public static class Reg implements ICmdLetRegistration {

		@Override
		public Optional<String> group() {
			return Optional.of("check");
		}

		@Override
		public Class<? extends ICmdLet> commandClass() {
			return CheckRunCommand.class;
		}

	}

	private CliContext ctx;

	@Parameters(paramLabel = "<check-name>", description = "Triggers the execution a check with the given name")
	public String check;

	public CheckRunCommand() {
	}

	@Override
	public Runnable forContext(CliContext ctx) {
		this.ctx = ctx;
		return this;
	}

	public void run() {
		IProductChecks checks = ctx.adminApi().instance(IProductChecks.class);
		TaskRef ref = checks.check(check);
		TaskStatus ckeckTaskStatus = Tasks.follow(ctx, ref, "Ckeck request failed.");
		if (!ckeckTaskStatus.state.succeed) {
			return;
		}

		ctx.info("'" + check + "' was requested.");
		int i = 0;
		Set<String> avail = checks.availableChecks();
		while (!avail.contains(check) && i++ < 10) {
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
			avail = checks.availableChecks();
		}
		CheckResult result = checks.lastResult(check);
		if (result != null) {
			Checks.printResult(ctx, check, result);
		}

	}

}
