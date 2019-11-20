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

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import io.airlift.airline.Command;
import net.bluemind.cli.cmd.api.CliContext;
import net.bluemind.cli.cmd.api.ICmdLet;
import net.bluemind.cli.cmd.api.ICmdLetRegistration;
import net.bluemind.metrics.alerts.api.CheckResult;
import net.bluemind.metrics.alerts.api.IProductChecks;

@Command(name = "status", description = "display product check results")
public class CheckStatusCommand implements ICmdLet, Runnable {

	public static class Reg implements ICmdLetRegistration {

		@Override
		public Optional<String> group() {
			return Optional.of("check");
		}

		@Override
		public Class<? extends ICmdLet> commandClass() {
			return CheckStatusCommand.class;
		}

	}

	private CliContext ctx;

	public CheckStatusCommand() {
	}

	@Override
	public Runnable forContext(CliContext ctx) {
		this.ctx = ctx;
		return this;
	}

	public void run() {
		IProductChecks checks = ctx.adminApi().instance(IProductChecks.class);
		Set<String> available = checks.availableChecks();
		// ensure we always report in the same order
		List<String> sorted = available.stream().sorted().collect(Collectors.toList());
		ctx.info(sorted.size() + " check result(s) available.");
		for (String check : sorted) {
			CheckResult result = checks.lastResult(check);
			Checks.printResult(ctx, check, result);
		}

	}

}
