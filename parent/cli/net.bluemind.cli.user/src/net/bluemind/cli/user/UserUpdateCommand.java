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

import io.airlift.airline.Command;
import io.airlift.airline.Option;
import net.bluemind.cli.cmd.api.CliException;
import net.bluemind.cli.cmd.api.ICmdLet;
import net.bluemind.cli.cmd.api.ICmdLetRegistration;
import net.bluemind.cli.directory.common.SingleOrDomainOperation;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.directory.api.BaseDirEntry.Kind;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.mailbox.api.IMailboxes;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.user.api.ChangePassword;
import net.bluemind.user.api.IUser;

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
	
	@Option(name = "--extId", description = "update user external id (used by AD/LDAP synchronisaion), must start with [ldap|ad]://")
	public String extId = null;
	
	@Option(name = "--quota", description = "update user mailbox quota")
	public Integer quota = null;

	public UserUpdateCommand() {
	}
	
	@Override
	public void synchronousDirOperation(String domainUid, ItemValue<DirEntry> de) {	
		if (de.uid.equals("admin0@global.virt")) {
			// only allow password update fir admin0
			if (extId != null || quota != null) {
				throw new CliException("extId and quota modification aren't allowed for admin0");
			}
		}
		
		updatePassword(domainUid, de);
		
		updateExtId(domainUid, de);
		
		updateQuota(domainUid, de);
		
	}

	private void updatePassword(String domainUid, ItemValue<DirEntry> de) {
		IUser userApi = ctx.adminApi().instance(IUser.class, domainUid);

		if (password != null) {
			if (password.trim().equals("")) {
				throw new CliException("Refusing empty password");
			}
			userApi.setPassword(de.uid, ChangePassword.create(password));
		}
	}

	private void updateExtId(String domainUid, ItemValue<DirEntry> de) {
		IUser userApi = ctx.adminApi().instance(IUser.class, domainUid);

		if (extId != null) {
			if (extId.equals("")) {
				throw new CliException("Refusing empty extId");
			}
			if (extId.startsWith("ldap://") || extId.startsWith("ad://")) {
				userApi.setExtId(de.uid, extId);
			} else {
				throw new CliException("Invalid format for extId, must start with [ad|ldap]://");
			}
		}
	}

	private void updateQuota(String domainUid, ItemValue<DirEntry> de) {
		if (quota != null) {
			IMailboxes mailboxesApi = ctx.adminApi().instance(IMailboxes.class, domainUid);
			ItemValue<Mailbox> mailboxItem = mailboxesApi.byEmail(de.value.email);
			if (mailboxItem == null) {
				throw new CliException("mailbox not found");
			}
			mailboxItem.value.quota = quota;
			mailboxesApi.update(mailboxItem.uid, mailboxItem.value);
		}
	}

	@Override
	public Kind[] getDirEntryKind() {
		return new Kind[] { Kind.USER };
	}
}
