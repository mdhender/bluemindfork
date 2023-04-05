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

import java.util.Optional;

import net.bluemind.authentication.mgmt.api.ISessionsMgmt;
import net.bluemind.cli.cmd.api.CliContext;
import net.bluemind.cli.cmd.api.CliException;
import net.bluemind.cli.cmd.api.ICmdLet;
import net.bluemind.cli.cmd.api.ICmdLetRegistration;
import net.bluemind.cli.utils.CliUtils;
import net.bluemind.core.api.Regex;
import net.bluemind.directory.api.BaseDirEntry.Kind;
import net.bluemind.directory.api.IDirectory;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "logout", description = "close all user sessions")
public class UserLogoutCommand implements ICmdLet, Runnable {

	public static class Reg implements ICmdLetRegistration {

		@Override
		public Optional<String> group() {
			return Optional.of("user");
		}

		@Override
		public Class<? extends ICmdLet> commandClass() {
			return UserLogoutCommand.class;
		}

	}

	private CliContext ctx;
	protected CliUtils cliUtils;

	@ArgGroup(exclusive = true, multiplicity = "1")
	private Scope scope;

	private static class Scope {
		@Option(names = "--uid", required = false, description = "User UID to logout")
		private String userUid;

		@Option(names = "--email", required = false, description = "User email to logout")
		private String userEmail;
	}

	@Override
	public Runnable forContext(CliContext ctx) {
		this.ctx = ctx;
		this.cliUtils = new CliUtils(ctx);
		return this;
	}

	public void run() {
		Optional.ofNullable(scope.userUid)
				.ifPresent(uid -> ctx.adminApi().instance(ISessionsMgmt.class).logoutUser(uid));

		Optional.ofNullable(scope.userEmail).map(this::emailToUid)
				.ifPresent(uid -> ctx.adminApi().instance(ISessionsMgmt.class).logoutUser(uid));
	}

	private String emailToUid(String email) {
		if (!Regex.EMAIL.validate(email)) {
			throw new CliException(String.format("Invalid email : %s", email));
		}

		Optional<String> userUid = Optional.ofNullable(
				ctx.adminApi().instance(IDirectory.class, cliUtils.getDomainUidByEmail(email)).getByEmail(email))
				.filter(de -> de.kind == Kind.USER).map(de -> de.entryUid);

		if (userUid.isEmpty()) {
			throw new CliException(String.format("User %s not found", email));
		}

		return userUid.orElse(null);
	}
}
