/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2020
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
package net.bluemind.cli.user.update;

import net.bluemind.cli.cmd.api.CliContext;
import net.bluemind.cli.user.UserUpdateCommand;
import net.bluemind.cli.user.UserUpdateCommand.PasswordMustChangeOption;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.user.api.IUser;
import net.bluemind.user.api.User;

public class PasswordMustChange extends UpdateCommand {
	public PasswordMustChange(CliContext ctx, UserUpdateCommand userUpdateCommand) {
		super(userUpdateCommand);
	}

	private PasswordMustChangeOption getPasswordMustchangeOption() {
		PasswordMustChangeOption passwordMustchange = getOptions().passwordMustChange;

		if (passwordMustchange == null) {
			return new PasswordMustChangeOption();
		}

		return getOptions().passwordMustChange;
	}

	@Override
	public boolean mustBeExecuted() {
		return getPasswordMustchangeOption().setPasswordMustChange
				|| getPasswordMustchangeOption().unsetPasswordMustChange;
	}

	@Override
	public void check() {
		// Nothing to do
	}

	@Override
	public void execute(String domainUid, ItemValue<User> user) {
		user.value.passwordMustChange = getPasswordMustchangeOption().setPasswordMustChange
				|| !getPasswordMustchangeOption().unsetPasswordMustChange;
		ctx.adminApi().instance(IUser.class, domainUid).update(user.uid, user.value);
	}
}
