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
package net.bluemind.cli.group;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import net.bluemind.cli.cmd.api.CliContext;
import net.bluemind.cli.cmd.api.CliException;
import net.bluemind.cli.cmd.api.ICmdLet;
import net.bluemind.cli.cmd.api.ICmdLetRegistration;
import net.bluemind.group.api.IGroup;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "set-roles", description = "Add roles of a group")
public class GroupSetRolesCommand implements ICmdLet, Runnable {
	public static class Reg implements ICmdLetRegistration {
		@Override
		public Optional<String> group() {
			return Optional.of("group");
		}

		@Override
		public Class<? extends ICmdLet> commandClass() {
			return GroupSetRolesCommand.class;
		}
	}

	private CliContext ctx;

	@Option(names = "--domain", required = true, description = "Target domain - must not be global.virt")
	private String domain;

	@ArgGroup(exclusive = true, multiplicity = "1")
	private GroupOptions groupOptions;

	private static class GroupOptions {
		@Option(names = "--name", required = true, description = "Target group name")
		private String name;

		@Option(names = "--uid", required = true, description = "Target group UID")
		private String uid;
	}

	@ArgGroup(exclusive = true, multiplicity = "1")
	private RolesOptions rolesOptions;

	private static class RolesOptions {
		@Option(names = "--remove", required = true, description = "Remove current roles")
		private boolean remove;

		@Option(names = "--roles", required = true, split = ",", description = "Comma separated list of BlueMind roles IDs\nReplace current roles with this new list of roles")
		private String[] roles;
	}

	@Override
	public void run() {
		if (domain.equals("global.virt")) {
			throw new CliException("Domain must not be global.virt!");
		}

		IGroup groupApi = ctx.adminApi().instance(IGroup.class, domain);
		String groupUid = Optional.ofNullable(groupOptions.name).map(groupApi::byName).map(g -> g.uid)
				.orElse(groupOptions.uid);

		if (groupUid == null) {
			throw new CliException(
					"Group " + groupOptions.name != null ? groupOptions.name : groupOptions.uid + " not found!");
		}

		groupApi.setRoles(groupUid, Collections.emptySet());

		if (rolesOptions.remove) {
			return;
		}

		groupApi.setRoles(groupUid, Set.of(rolesOptions.roles));
	}

	@Override
	public Runnable forContext(CliContext ctx) {
		this.ctx = ctx;
		return this;
	}
}
