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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.google.common.base.Strings;
import com.google.common.hash.Hashing;

import net.bluemind.backend.mail.replica.api.IReplicatedMailboxesMgmt;
import net.bluemind.cli.cmd.api.CliException;
import net.bluemind.cli.cmd.api.ICmdLet;
import net.bluemind.cli.cmd.api.ICmdLetRegistration;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "history-guid", description = "Show mail history by body GUID")
public class MailHistoryByGuidCommand extends MailHistoryCommand implements ICmdLet, Runnable {

	@Option(names = "--body-guid", description = "Body-GUID (Email hashed using SHA-1)")
	public String guid;

	@Option(names = "--eml", description = "Path to an eml file")
	public String eml;

	@Override
	public void run() {
		if (Strings.isNullOrEmpty(guid) && Strings.isNullOrEmpty(eml)) {
			ctx.error("You must specify --body-guid or --eml");
			return;
		}
		if (Strings.isNullOrEmpty(guid)) {
			guid = createGuid();
		}

		searchMessageHistory();
	}

	private void searchMessageHistory() {
		IReplicatedMailboxesMgmt search = ctx.adminApi().instance(IReplicatedMailboxesMgmt.class);

		List<ItemHistory> items = search.getBodyGuidReferences(guid).stream().map(this::getHistory)
				.collect(Collectors.toList());
		printTable(items);

	}

	@SuppressWarnings("deprecation")
	private String createGuid() {
		try {
			byte[] emlContent = Files.readAllBytes(new File(eml).toPath());
			return Hashing.sha1().hashBytes(emlContent).toString();
		} catch (IOException e) {
			throw new CliException("Cannot hash eml file " + eml, e);
		}
	}

	public static class Reg implements ICmdLetRegistration {

		@Override
		public Optional<String> group() {
			return Optional.of("mail");
		}

		@Override
		public Class<? extends ICmdLet> commandClass() {
			return MailHistoryByGuidCommand.class;
		}
	}

}
