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
import net.bluemind.cli.utils.Tasks;
import net.bluemind.config.InstallationId;
import net.bluemind.core.backup.continuous.mgmt.api.IContinuousBackupMgmt;
import net.bluemind.core.task.api.TaskRef;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(name = "join", description = "Register bluemind installation as a forest member")
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

	@Parameters(description = "forest id (sub-directory in zookeeper), default is ${DEFAULT-VALUE}", defaultValue = "default-forest")
	public String forest = "default-forest";

	@Override
	public void run() {
		ctx.info("Joining installation '" + InstallationId.getIdentifier() + "' to '" + forest + "'...");

		IContinuousBackupMgmt mgmtApi = ctx.adminApi().instance(IContinuousBackupMgmt.class);
		TaskRef ref = mgmtApi.join(forest);
		Tasks.followStream(ctx, "join-" + forest, ref).join();
	}

	@Override
	public Runnable forContext(CliContext ctx) {
		this.ctx = ctx;
		return this;
	}

}
