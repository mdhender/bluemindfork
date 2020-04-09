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
import net.bluemind.cli.cmd.api.CliException;
import net.bluemind.cli.user.UserUpdateCommand;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.mailbox.api.IMailboxes;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.user.api.User;

public class Quota extends UpdateCommand {
	public Quota(CliContext ctx, UserUpdateCommand userUpdateCommand) {
		super(userUpdateCommand);
	}

	@Override
	public boolean mustBeExecuted() {
		return userUpdateCommand.quota != null;
	}

	@Override
	public void check() {
		if (userUpdateCommand.quota < 0) {
			throw new CliException("Quota must be greater or equal to 0");
		}
	}

	@Override
	public void execute(String domainUid, ItemValue<User> user) {
		IMailboxes mailboxesApi = ctx.adminApi().instance(IMailboxes.class, domainUid);
		ItemValue<Mailbox> mailboxItem = mailboxesApi.byEmail(user.value.defaultEmailAddress());
		if (mailboxItem == null) {
			throw new CliException("mailbox not found");
		}
		mailboxItem.value.quota = userUpdateCommand.quota;
		mailboxesApi.update(mailboxItem.uid, mailboxItem.value);
	}
}
