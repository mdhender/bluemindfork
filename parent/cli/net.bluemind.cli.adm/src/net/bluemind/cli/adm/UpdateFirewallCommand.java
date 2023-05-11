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
package net.bluemind.cli.adm;

import java.util.Optional;

import net.bluemind.cli.cmd.api.CliContext;
import net.bluemind.cli.cmd.api.CliException;
import net.bluemind.cli.cmd.api.ICmdLet;
import net.bluemind.cli.cmd.api.ICmdLetRegistration;
import net.bluemind.cli.utils.Tasks;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.core.task.api.TaskStatus;
import net.bluemind.core.task.api.TaskStatus.State;
import net.bluemind.system.api.IInternalFirewallMgmt;
import picocli.CommandLine.Command;

@Command(name = "update-fw", description = "Update firewall rules")
public class UpdateFirewallCommand implements ICmdLet, Runnable {

	private CliContext ctx;

	public static class Reg implements ICmdLetRegistration {

		@Override
		public Optional<String> group() {
			return Optional.of("maintenance");
		}

		@Override
		public Class<? extends ICmdLet> commandClass() {
			return UpdateFirewallCommand.class;
		}
	}

	@Override
	public void run() {
		try {
			TaskRef updateTask = ctx.adminApi().instance(IInternalFirewallMgmt.class).updateFirewallRules();
			TaskStatus taskState = Tasks.follow(ctx, updateTask, "Update ip tables", null);
			if (taskState.state == State.Success) {
				ctx.info("Firewall rules updated");
			} else {
				ctx.error("Cannot update firewall rules {}: {}", taskState.state.name(), taskState.result);
			}
		} catch (Exception e) {
			throw new CliException(e.getMessage());
		}
	}

	@Override
	public Runnable forContext(CliContext ctx) {
		this.ctx = ctx;
		return this;
	}

}
