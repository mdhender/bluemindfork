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

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import io.vertx.core.json.JsonArray;
import net.bluemind.cli.cmd.api.CliContext;
import net.bluemind.cli.cmd.api.CliException;
import net.bluemind.cli.cmd.api.ICmdLet;
import net.bluemind.cli.cmd.api.ICmdLetRegistration;
import net.bluemind.directory.api.IOrgUnits;
import net.bluemind.directory.api.OrgUnitPath;
import net.bluemind.directory.api.OrgUnitQuery;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "get", description = "Search BlueMind delegations")
public class OuGetCommand implements ICmdLet, Runnable {
	public static class Reg implements ICmdLetRegistration {
		@Override
		public Optional<String> group() {
			return Optional.of("ou");
		}

		@Override
		public Class<? extends ICmdLet> commandClass() {
			return OuGetCommand.class;
		}
	}

	private CliContext ctx;

	@Option(names = "--domain", required = true, description = "Target domain - must not be global.virt")
	private String domain;

	@ArgGroup(exclusive = true, multiplicity = "0..1")
	private SearchScope searchScope;

	private static class SearchScope {
		@Option(names = "--uid", required = false, description = "Get delegation from its UID")
		String uid;

		@Option(names = "--name", required = false, description = "Get delegation from its name")
		String name;
	}

	@Override
	public void run() {
		if (domain.equals("global.virt")) {
			throw new CliException("Domain must not be global.virt!");
		}

		if (searchScope == null) {
			toJson(searchByName(""));
			return;
		}

		Optional.ofNullable(searchScope.uid).map(this::searchByUid).map(Arrays::asList).ifPresent(this::toJson);

		Optional.ofNullable(searchScope.name).map(this::searchByName).ifPresent(this::toJson);
	}

	public OrgUnitPath searchByUid(String uid) {
		return ctx.adminApi().instance(IOrgUnits.class, domain).getPath(uid);
	}

	public List<OrgUnitPath> searchByName(String name) {
		OrgUnitQuery orgUnitQuery = new OrgUnitQuery();
		orgUnitQuery.query = name;

		return ctx.adminApi().instance(IOrgUnits.class, domain).search(orgUnitQuery);
	}

	public void toJson(List<OrgUnitPath> orgUnitPaths) {
		ctx.info(new JsonArray(orgUnitPaths).encode());
	}

	@Override
	public Runnable forContext(CliContext ctx) {
		this.ctx = ctx;
		return this;
	}
}
