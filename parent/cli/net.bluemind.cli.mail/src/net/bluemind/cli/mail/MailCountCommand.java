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
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import net.bluemind.cli.cmd.api.CliContext;
import net.bluemind.cli.cmd.api.ICmdLet;
import net.bluemind.cli.cmd.api.ICmdLetRegistration;
import net.bluemind.core.email.MailCounter;
import picocli.CommandLine.Command;

@Command(name = "count", description = "List the number of unique messages in the cyrus spool on the current server")
public class MailCountCommand implements ICmdLet, Runnable {
	private CliContext ctx;

	@Override
	public void run() {
		File spool = new File("/var/spool/cyrus/data");
		File hsm = new File("/var/spool/bm-hsm/cyrus-archives");
		Set<Long> files = new HashSet<>();
		long spoolCount;
		try {
			MailCounter.count(spool, files);
			spoolCount = files.size();
			MailCounter.count(hsm, files);
			long hsmCount = files.size() - spoolCount;
			long total = files.size();
			ctx.info("Found " + total + " mails. Spool: " + spoolCount + ", HSM: " + hsmCount);
		} catch (IOException e) {
			ctx.error("Cannot count messages: " + e.getMessage());
		}

	}

	@Override
	public Runnable forContext(CliContext ctx) {
		this.ctx = ctx;
		return this;
	}

	public static class Reg implements ICmdLetRegistration {

		@Override
		public Optional<String> group() {
			return Optional.of("mail");
		}

		@Override
		public Class<? extends ICmdLet> commandClass() {
			return MailCountCommand.class;
		}
	}

}
