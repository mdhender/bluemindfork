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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.airlift.airline.Command;
import io.airlift.airline.Option;
import net.bluemind.cli.cmd.api.CliContext;
import net.bluemind.cli.cmd.api.ICmdLet;
import net.bluemind.cli.cmd.api.ICmdLetRegistration;
import net.bluemind.cli.utils.Tasks;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.metrics.alerts.api.ITickConfiguration;

@Command(name = "reconfigure", description = "update the TICK configuration on all servers")
public class TickReconfigureCommand implements ICmdLet, Runnable {

	public static class Reg implements ICmdLetRegistration {

		@Override
		public Optional<String> group() {
			return Optional.of("tick");
		}

		@Override
		public Class<? extends ICmdLet> commandClass() {
			return TickReconfigureCommand.class;
		}

	}

	private static final Logger logger = LoggerFactory.getLogger(TickReconfigureCommand.class);

	private CliContext ctx;

	@Option(name = "--dry", description = "Dry-run (do nothing)")
	public boolean dry = false;

	public TickReconfigureCommand() {
	}

	@Override
	public Runnable forContext(CliContext ctx) {
		this.ctx = ctx;
		return this;
	}

	public void run() {
		if (dry) {
			logger.warn("Dry mode does nothing");
		} else {
			ITickConfiguration tickApi = ctx.adminApi().instance(ITickConfiguration.class);
			TaskRef ref = tickApi.reconfigure();
			Tasks.follow(ctx, ref);
		}
	}

}
