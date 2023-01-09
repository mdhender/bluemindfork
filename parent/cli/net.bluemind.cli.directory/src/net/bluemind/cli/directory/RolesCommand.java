/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2023
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
package net.bluemind.cli.directory;

import java.util.Optional;
import java.util.stream.Collectors;

import io.vertx.core.json.JsonArray;
import net.bluemind.cli.cmd.api.CliContext;
import net.bluemind.cli.cmd.api.ICmdLet;
import net.bluemind.cli.cmd.api.ICmdLetRegistration;
import net.bluemind.role.api.IRoles;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "roles", description = "Get available BlueMind roles and categories")
public class RolesCommand implements ICmdLet, Runnable {
	public static class Reg implements ICmdLetRegistration {
		@Override
		public Optional<String> group() {
			return Optional.of("directory");
		}

		@Override
		public Class<? extends ICmdLet> commandClass() {
			return RolesCommand.class;
		}
	}

	private CliContext ctx;

	@ArgGroup(exclusive = true, multiplicity = "1")
	private Scope scope;

	private static class Scope {
		@Option(names = "--roles", required = true, description = "Get all available BlueMind roles")
		boolean roles;

		@Option(names = "--categories", required = true, description = "Get all available role categories.\nRole categories are groupings of roles")
		boolean categories;
	}

	@Override
	public void run() {
		if (scope.roles) {
			ctx.info(new JsonArray(
					ctx.adminApi().instance(IRoles.class).getRoles().stream().collect(Collectors.toList())).encode());
			return;
		}

		if (scope.categories) {
			ctx.info(new JsonArray(
					ctx.adminApi().instance(IRoles.class).getRolesCategories().stream().collect(Collectors.toList()))
					.encode());
			return;
		}
	}

	@Override
	public Runnable forContext(CliContext ctx) {
		this.ctx = ctx;
		return this;
	}
}
