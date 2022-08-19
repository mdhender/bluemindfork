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
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.cli.adm;

import java.util.List;
import java.util.Optional;

import com.google.common.base.Strings;

import net.bluemind.cli.cmd.api.CliContext;
import net.bluemind.cli.cmd.api.CliException;
import net.bluemind.cli.cmd.api.ICmdLet;
import net.bluemind.cli.cmd.api.ICmdLetRegistration;
import net.bluemind.cli.utils.Tasks;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.core.task.api.TaskStatus;
import net.bluemind.core.task.api.TaskStatus.State;
import net.bluemind.core.utils.JsonUtils;
import net.bluemind.system.api.ISchemaMgmt;
import net.bluemind.system.api.SchemaCheckInfo;
import picocli.CommandLine.Command;

@Command(name = "schema", description = "check database schema")
public class SchemaCheck implements ICmdLet, Runnable {

	public static class Reg implements ICmdLetRegistration {

		@Override
		public Optional<String> group() {
			return Optional.of("maintenance");
		}

		@Override
		public Class<? extends ICmdLet> commandClass() {
			return SchemaCheck.class;
		}
	}

	private CliContext ctx;

	@Override
	public void run() {
		try {
			TaskRef verify = ctx.adminApi().instance(ISchemaMgmt.class).verify();
			TaskStatus taskState = Tasks.follow(ctx, verify, "Verify schema", null);
			if (taskState.state == State.Success) {
				List<SchemaCheckInfo> info = JsonUtils.listReader(SchemaCheckInfo.class).read(taskState.result);
				info.forEach(this::displayStatement);
			} else {
				ctx.error("Cannot verify database schemas");
			}
		} catch (Exception e) {
			throw new CliException(e.getMessage());
		}
	}

	private void displayStatement(SchemaCheckInfo check) {
		if (Strings.isNullOrEmpty(check.statements)) {
			ctx.info("\nThe {} schema on server {} is compliant, there is no statements to execute. \n", check.db,
					check.server);
		} else {
			ctx.info("\nFollowing statements could be executed on server {} to be compliant with {} schema: \n",
					check.server, check.db);
			ctx.info(check.statements);
			ctx.info("\n");
		}
	}

	@Override
	public Runnable forContext(CliContext ctx) {
		this.ctx = ctx;
		return this;
	}

}
