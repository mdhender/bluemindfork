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
package net.bluemind.cli.mapi;

import java.util.Optional;

import io.airlift.airline.Arguments;
import io.airlift.airline.Command;
import io.airlift.airline.Option;
import net.bluemind.cli.cmd.api.CliContext;
import net.bluemind.cli.cmd.api.ICmdLet;
import net.bluemind.cli.cmd.api.ICmdLetRegistration;
import net.bluemind.cli.utils.CliUtils;
import net.bluemind.core.api.Regex;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.exchange.mapi.api.IMapiMailbox;
import net.bluemind.mailbox.api.IMailboxes;
import net.bluemind.mailbox.api.Mailbox;

@Command(name = "logging", description = "Enable/Disable per-user MAPI logs")
public class LoggingCommand implements ICmdLet, Runnable {

	public static class Reg implements ICmdLetRegistration {

		@Override
		public Optional<String> group() {
			return Optional.of("mapi");
		}

		@Override
		public Class<? extends ICmdLet> commandClass() {
			return LoggingCommand.class;
		}
	}

	private CliContext ctx;

	@Arguments(required = true, description = "email address")
	public String target;

	@Option(name = "--enable", description = "Enable the per-user logs (disable if not specified)")
	public boolean enable = false;

	@Override
	public void run() {
		if (!Regex.EMAIL.validate(target)) {
			ctx.error(target + " is not an email.");
			return;
		}
		CliUtils cliUtils = new CliUtils(ctx);
		String domainUid = cliUtils.getDomainUidFromEmailOrDomain(target);

		IMailboxes boxApi = ctx.adminApi().instance(IMailboxes.class, domainUid);
		ItemValue<Mailbox> mailbox = boxApi.byEmail(target);
		if (mailbox == null) {
			ctx.error("Mailbox not found for email '" + target + "'");
			return;
		}
		ctx.info("Switch logs " + (enable ? "ON" : "OFF") + " for " + target + " (mailbox uid " + mailbox.uid + ")");
		IMapiMailbox mapiMboxApi = ctx.adminApi().instance(IMapiMailbox.class, domainUid, mailbox.uid);
		mapiMboxApi.enablePerUserLog(enable);
	}

	@Override
	public Runnable forContext(CliContext ctx) {
		this.ctx = ctx;
		return this;
	}
}
