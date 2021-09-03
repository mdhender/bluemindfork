/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2019
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
package net.bluemind.cli.inject.imap;

import java.util.Optional;

import net.bluemind.cli.cmd.api.CliContext;
import net.bluemind.cli.cmd.api.ICmdLet;
import net.bluemind.cli.cmd.api.ICmdLetRegistration;
import net.bluemind.cli.inject.common.AbstractMailInjectCommand;
import net.bluemind.cli.inject.common.IMessageProducer;
import net.bluemind.cli.inject.common.MailExchangeInjector;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "imap", description = "Injects a batch of emails through IMAP")
public class ImapInjectCommand extends AbstractMailInjectCommand {

	@Option(names = "--folders", description = "Populate N top-lvl with N children mail folders each in the mailbox")
	public int folders = 0;

	public static class Reg implements ICmdLetRegistration {

		@Override
		public Optional<String> group() {
			return Optional.of("inject");
		}

		@Override
		public Class<? extends ICmdLet> commandClass() {
			return ImapInjectCommand.class;
		}
	}

	@Override
	protected MailExchangeInjector createInjector(CliContext ctx, String domUid, IMessageProducer prod) {
		return new ImapInjector(ctx.adminApi(), domUid, prod, folders);
	}

}
