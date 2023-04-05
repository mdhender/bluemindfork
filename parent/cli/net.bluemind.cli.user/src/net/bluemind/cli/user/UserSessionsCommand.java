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
package net.bluemind.cli.user;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import com.github.freva.asciitable.AsciiTable;
import com.github.freva.asciitable.Column;

import net.bluemind.authentication.mgmt.api.ISessionsMgmt;
import net.bluemind.authentication.mgmt.api.SessionEntry;
import net.bluemind.cli.cmd.api.CliContext;
import net.bluemind.cli.cmd.api.ICmdLet;
import net.bluemind.cli.cmd.api.ICmdLetRegistration;
import net.bluemind.cli.utils.CliUtils;
import net.bluemind.core.utils.JsonUtils;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "sessions", description = "Get current sessions informations")
public class UserSessionsCommand implements ICmdLet, Runnable {
	public static class Reg implements ICmdLetRegistration {
		@Override
		public Optional<String> group() {
			return Optional.of("user");
		}

		@Override
		public Class<? extends ICmdLet> commandClass() {
			return UserSessionsCommand.class;
		}
	}

	private CliContext ctx;
	protected CliUtils cliUtils;

	@Option(names = "--json", required = false, defaultValue = "false", description = "Display sessions using Json format\nTabel format otherwise")
	public Boolean json;

	@Option(names = "--domain", required = false, description = "Get sessions from this domain UID or alias")
	public String domain;

	@Override
	public Runnable forContext(CliContext ctx) {
		this.ctx = ctx;
		this.cliUtils = new CliUtils(ctx);
		return this;
	}

	public void run() {
		List<SessionEntry> sessions = ctx.adminApi().instance(ISessionsMgmt.class)
				.list(Optional.ofNullable(domain).map(cliUtils::getDomainUidByDomain).orElse(null));

		ctx.info(json ? JsonUtils.asString(sessions)
				: AsciiTable.getTable(sessions,
						Arrays.asList(
								new Column().header("Created (" + cliUtils.localTz + ")")
										.with(session -> cliUtils.epochToLocalDate(session.created)),
								new Column().header("Email").with(session -> session.email),
								new Column().header("Domain UID").with(session -> session.domainUid),
								new Column().header("User UID").with(session -> session.userUid),
								new Column().header("Origin").with(session -> session.origin),
								new Column().header("Remote addresses")
										.with(session -> String.join(", ", session.remoteAddresses)))));
	}
}
