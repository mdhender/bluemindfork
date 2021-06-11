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
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.user.api.IUser;
import net.bluemind.user.api.User;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

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

	@Parameters(paramLabel = "<email>", description = "email address")
	public String email;

	public UserLogoutCommand() {
	}

	@Override
	public Runnable forContext(CliContext ctx) {
		this.ctx = ctx;
		this.cliUtils = new CliUtils(ctx);
		return this;
	}

	public void run() {
		if (!Regex.EMAIL.validate(email)) {
			throw new CliException(String.format("Invalid email : %", email));
		}

		String domainUid = cliUtils.getDomainUidByEmail(email);
		IUser userApi = ctx.adminApi().instance(IUser.class, domainUid);
		ItemValue<User> user = userApi.byEmail(email);
		if (user == null) {
			throw new CliException(String.format("User %s not found", email));
		}

		ISessionsMgmt sessionApi = ctx.adminApi().instance(ISessionsMgmt.class);
		sessionApi.logoutUser(user.value.login + '@' + domainUid);
	}

}
