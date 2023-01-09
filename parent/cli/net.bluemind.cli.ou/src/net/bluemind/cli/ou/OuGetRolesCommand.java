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
import java.util.stream.Collectors;

import io.vertx.core.json.Json;
import net.bluemind.cli.cmd.api.CliContext;
import net.bluemind.cli.cmd.api.CliException;
import net.bluemind.cli.cmd.api.ICmdLet;
import net.bluemind.cli.cmd.api.ICmdLetRegistration;
import net.bluemind.directory.api.IOrgUnits;
import net.bluemind.directory.api.OrgUnitPath;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "get-roles", description = "Get roles by user/group on a delegation")
public class OuGetRolesCommand implements ICmdLet, Runnable {
	public static class Reg implements ICmdLetRegistration {
		@Override
		public Optional<String> group() {
			return Optional.of("ou");
		}

		@Override
		public Class<? extends ICmdLet> commandClass() {
			return OuGetRolesCommand.class;
		}
	}

	private static class EntityRoles {
		@SuppressWarnings("unused")
		public final String entityUid;
		@SuppressWarnings("unused")
		public final Set<String> roles;

		private EntityRoles(String entityUid, Set<String> roles) {
			this.entityUid = entityUid;
			this.roles = roles;
		}

		public static EntityRoles build(String entityUid, Set<String> roles) {
			return new EntityRoles(entityUid, roles == null ? Collections.emptySet() : roles);
		}
	}

	private static class OuRolesByEntities {
		public final String ouUid;
		public final Set<EntityRoles> rolesByEntities;
		@SuppressWarnings("unused")
		public final OuRolesByEntities parent;

		private OuRolesByEntities(String ouUid, Set<EntityRoles> rolesByEntities) {
			this.ouUid = ouUid;
			this.rolesByEntities = rolesByEntities;
			this.parent = null;
		}

		private OuRolesByEntities(String ouUid, Set<EntityRoles> rolesByEntities, OuRolesByEntities parent) {
			this.ouUid = ouUid;
			this.rolesByEntities = rolesByEntities;
			this.parent = parent;
		}

		public static OuRolesByEntities build(String ouUid, Set<EntityRoles> rolesByEntities) {
			return new OuRolesByEntities(ouUid, rolesByEntities == null ? Collections.emptySet() : rolesByEntities);
		}

		public OuRolesByEntities addParent(OuRolesByEntities parent) {
			return new OuRolesByEntities(ouUid, rolesByEntities, parent);
		}
	}

	private CliContext ctx;

	@Option(names = "--domain", required = true, description = "Target domain - must not be global.virt")
	private String domain;

	@Option(names = "--ou", required = true, description = "Target delegation UID")
	private String ou;

	@Override
	public void run() {
		if (domain.equals("global.virt")) {
			throw new CliException("Domain must not be global.virt!");
		}

		ctx.info(Json.encode(Optional.ofNullable(ctx.adminApi().instance(IOrgUnits.class, domain).getPath(ou))
				.map(this::rolesFromOrgUnitPath).orElse(null)));
	}

	private OuRolesByEntities rolesFromOrgUnitPath(OrgUnitPath orgUnitPath) {
		if (orgUnitPath == null) {
			return null;
		}

		return OuRolesByEntities.build(orgUnitPath.uid, getRolesForOu(orgUnitPath.uid))
				.addParent(rolesFromOrgUnitPath(orgUnitPath.parent));
	}

	private Set<EntityRoles> getRolesForOu(String ouUid) {
		return ctx.adminApi().instance(IOrgUnits.class, domain).getAdministrators(ouUid).stream()
				.map(entityUid -> EntityRoles.build(entityUid, ctx.adminApi().instance(IOrgUnits.class, domain)
						.getAdministratorRoles(ouUid, entityUid, Collections.emptyList())))
				.collect(Collectors.toSet());
	}

	@Override
	public Runnable forContext(CliContext ctx) {
		this.ctx = ctx;
		return this;
	}
}
