/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2023
  *
  * This file is part of Blue Mind. Blue Mind is a messaging and collaborative
  * solution.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of either the GNU Affero General Public License as
  * published by the Free Software Foundation (version 3 of the License)
  * or the CeCILL as published by CeCILL.info (version 2 of the License).
  *
  * There are special exceptions to the terms and conditions of the
  * licenses as they are applied to this program. See LICENSE.txt in
  * the directory of this program distribution.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.cli.ou;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import net.bluemind.cli.cmd.api.CliContext;
import net.bluemind.cli.cmd.api.CliException;
import net.bluemind.cli.cmd.api.ICmdLet;
import net.bluemind.cli.cmd.api.ICmdLetRegistration;
import net.bluemind.directory.api.IOrgUnits;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "set-roles", description = "Add roles for an user/group on a delegation")
public class OuSetRolesCommand implements ICmdLet, Runnable {
	public static class Reg implements ICmdLetRegistration {
		@Override
		public Optional<String> group() {
			return Optional.of("ou");
		}

		@Override
		public Class<? extends ICmdLet> commandClass() {
			return OuSetRolesCommand.class;
		}
	}

	private CliContext ctx;

	@Option(names = "--domain", required = true, description = "Target domain - must not be global.virt")
	private String domain;

	@Option(names = "--ou", required = true, description = "Target delegation UID")
	private String ou;

	@Option(names = "--target", required = true, description = "Target user/group UID")
	private String target;

	@ArgGroup(exclusive = true, multiplicity = "1")
	private RolesOptions rolesOptions;

	private static class RolesOptions {
		@Option(names = "--delete", required = true, description = "Remove current roles")
		private boolean remove;

		@Option(names = "--roles", required = true, split = ",", description = "Comma separated list of BlueMind roles IDs\nReplace current roles with this new list of roles")
		private String[] roles;
	}

	@Override
	public void run() {
		if (domain.equals("global.virt")) {
			throw new CliException("Domain must not be global.virt!");
		}

		ctx.adminApi().instance(IOrgUnits.class, domain).setAdministratorRoles(ou, target, Collections.emptySet());

		if (rolesOptions.remove) {
			return;
		}

		ctx.adminApi().instance(IOrgUnits.class, domain).setAdministratorRoles(ou, target, Set.of(rolesOptions.roles));
	}

	@Override
	public Runnable forContext(CliContext ctx) {
		this.ctx = ctx;
		return this;
	}
}
