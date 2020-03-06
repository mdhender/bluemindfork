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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import io.airlift.airline.Command;
import io.airlift.airline.Option;
import net.bluemind.cli.cmd.api.CliException;
import net.bluemind.cli.cmd.api.ICmdLet;
import net.bluemind.cli.cmd.api.ICmdLetRegistration;
import net.bluemind.cli.directory.common.SingleOrDomainOperation;
import net.bluemind.cli.user.update.ExternalId;
import net.bluemind.cli.user.update.Password;
import net.bluemind.cli.user.update.PasswordMustChange;
import net.bluemind.cli.user.update.Quota;
import net.bluemind.cli.user.update.UpdateCommand;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.directory.api.BaseDirEntry.Kind;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.user.api.IUser;
import net.bluemind.user.api.User;

@Command(name = "update", description = "update users")
public class UserUpdateCommand extends SingleOrDomainOperation {

	public static class Reg implements ICmdLetRegistration {

		@Override
		public Optional<String> group() {
			return Optional.of("user");
		}

		@Override
		public Class<? extends ICmdLet> commandClass() {
			return UserUpdateCommand.class;
		}
	}

	@Option(name = "--password", description = "update user password")
	public String password = null;

	@Option(name = "--set-password-must-change", description = "set user password must change")
	public boolean setPasswordMustChange = false;

	@Option(name = "--unset-password-must-change", description = "unset user password must change")
	public boolean unsetPasswordMustChange = false;

	@Option(name = "--external-id", description = "update user external id (used by AD/LDAP synchronisaion), empty to unset")
	public String extId = null;

	@Option(name = "--quota", description = "update user mailbox quota")
	public Integer quota = null;

	private List<UpdateCommand> commands = new ArrayList<>();

	public UserUpdateCommand() {
		commands.add(new ExternalId(ctx, this));
		commands.add(new Password(ctx, this));
		commands.add(new PasswordMustChange(ctx, this));
		commands.add(new Quota(ctx, this));
	}

	@Override
	public void synchronousDirOperation(String domainUid, ItemValue<DirEntry> de) {
		List<UpdateCommand> commandsToRun = commands.stream().filter(UpdateCommand::mustBeExecuted)
				.map(command -> command.setContext(ctx)).collect(Collectors.toList());

		if (commandsToRun.size() == 0) {
			return;
		}

		if (de.uid.equals("admin0_global.virt")
				&& (commandsToRun.size() != 1 || !(commandsToRun.get(0) instanceof Password))) {
			throw new CliException("Only password update is allowed for user admin0@global.virt");
		}

		commandsToRun.forEach(command -> command.check());

		ItemValue<User> user = ctx.adminApi().instance(IUser.class, domainUid).getComplete(de.uid);

		commandsToRun.forEach(command -> command.execute(domainUid, user));
	}

	@Override
	public Kind[] getDirEntryKind() {
		return new Kind[] { Kind.USER };
	}
}
