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
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.cli.mail;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import net.bluemind.authentication.api.IAuthentication;
import net.bluemind.authentication.api.LoginResponse;
import net.bluemind.authentication.api.LoginResponse.Status;
import net.bluemind.backend.mail.api.IMailboxFolders;
import net.bluemind.backend.mail.api.IMailboxFoldersByContainer;
import net.bluemind.backend.mail.api.MailboxFolder;
import net.bluemind.backend.mail.replica.api.IMailReplicaUids;
import net.bluemind.backend.mail.replica.api.IReplicatedMailboxesMgmt;
import net.bluemind.backend.mail.replica.api.MailboxRecordItemUri;
import net.bluemind.cli.cmd.api.CliException;
import net.bluemind.cli.cmd.api.ICmdLet;
import net.bluemind.cli.cmd.api.ICmdLetRegistration;
import net.bluemind.cli.utils.CliUtils;
import net.bluemind.cli.utils.CliUtils.ResolvedMailbox;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.mailbox.api.IMailboxes;
import net.bluemind.mailbox.api.Mailbox;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "history-imap", description = "Show mail history by Imap-UID")
public class MailHistoryByImapUIDCommand extends MailHistoryCommand implements ICmdLet, Runnable {

	@Option(required = true, names = "--email", description = "User email")
	public String email;

	@Option(required = true, names = "--imapUid", description = "Imap UID")
	public Long imapUid;

	@Option(required = false, defaultValue = "INBOX", names = "--folderPath", description = "Absolute imap folder path, defaults to INBOX")
	public String folderPath = "INBOX";

	@Override
	public void run() {
		searchMessageHistory();
	}

	private void searchMessageHistory() {
		IReplicatedMailboxesMgmt search = ctx.adminApi().instance(IReplicatedMailboxesMgmt.class);

		String domain = new CliUtils(ctx).getDomainUidByEmailOrDomain(email);

		CliUtils cu = new CliUtils(ctx);
		ResolvedMailbox resolved = cu.getMailboxByEmail(email);
		String resolvedUser = resolved.mailbox.value.name + "@" + resolved.domainUid;
		IAuthentication authApi = ctx.adminApi().instance(IAuthentication.class);
		LoginResponse sudo = authApi.su(resolvedUser);
		if (sudo.status != Status.Ok) {
			throw new CliException("Sudo as " + resolvedUser + " failed: " + sudo.status + " " + sudo.message);
		}

		IServiceProvider userProv = ctx.api(sudo.authKey);

		IMailboxes mboxService = ctx.adminApi().instance(IMailboxes.class, domain);
		ItemValue<Mailbox> mbox = mboxService.byEmail(email);

		IMailboxFolders mailboxFoldersService = userProv.instance(IMailboxFoldersByContainer.class,
				IMailReplicaUids.subtreeUid(domain, mbox));
		ItemValue<MailboxFolder> folder = mailboxFoldersService.byName(folderPath);

		List<Set<MailboxRecordItemUri>> imapUidReferences = search.getImapUidReferences(mbox.uid, folder.uid, imapUid);
		imapUidReferences.forEach(ret -> {
			List<ItemHistory> items = ret.stream().map(this::getHistory).collect(Collectors.toList());
			printTable(items);
		});
	}

	public static class Reg implements ICmdLetRegistration {

		@Override
		public Optional<String> group() {
			return Optional.of("mail");
		}

		@Override
		public Class<? extends ICmdLet> commandClass() {
			return MailHistoryByImapUIDCommand.class;
		}
	}

}
