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
import java.util.stream.Collectors;

import net.bluemind.backend.mail.replica.api.IReplicatedMailboxesMgmt;
import net.bluemind.cli.cmd.api.ICmdLet;
import net.bluemind.cli.cmd.api.ICmdLetRegistration;
import net.bluemind.cli.utils.CliUtils;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "history-imap", description = "Show mail history by Imap-UID")
public class MailHistoryByImapUIDCommand extends MailHistoryCommand implements ICmdLet, Runnable {

	@Option(required = true, names = "--email", description = "User email")
	public String email;

	@Option(required = true, names = "--imapUid", description = "Imap UID")
	public Long imapUid;

	@Override
	public void run() {
		searchMessageHistory();
	}

	private void searchMessageHistory() {
		IReplicatedMailboxesMgmt search = ctx.adminApi().instance(IReplicatedMailboxesMgmt.class);

		search.getImapUidReferences(new CliUtils(ctx).getUserUidByEmail(email), imapUid).forEach(ret -> {
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
