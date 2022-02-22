/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2022
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
package net.bluemind.cli.journaling;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import net.bluemind.cli.cmd.api.CliContext;
import net.bluemind.cli.cmd.api.ICmdLet;
import net.bluemind.cli.cmd.api.ICmdLetRegistration;
import net.bluemind.cli.journaling.utils.JournalingUtils;
import net.bluemind.mailflow.api.MailRuleActionAssignment;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "list", description = "List all journaling mailflow action assignments for a domain")
public class ListJournalingActionCommand implements ICmdLet, Runnable {
	private CliContext ctx;

	@Option(required = true, names = { "--domain", "-d" }, description = "The domain name")
	public String domainName;

	@Override
	public void run() {
		JournalingUtils journalingUtils = new JournalingUtils(ctx, domainName);

		List<MailRuleActionAssignment> assignments = journalingUtils.getMailfloxApi().listAssignments().stream()
				.filter(a -> JournalingUtils.ACTION_IDENTIFIER.equals(a.actionIdentifier)).collect(Collectors.toList());

		if (assignments.isEmpty()) {
			ctx.info("No Journaling action found");
		} else {
			ctx.info("{} Journaling action(s) found", assignments.size());
		}
		journalingUtils.displayAssignments(assignments);
	}

	@Override
	public Runnable forContext(CliContext ctx) {
		this.ctx = ctx;
		return this;
	}

	public static class Reg implements ICmdLetRegistration {

		@Override
		public Optional<String> group() {
			return Optional.of("journaling");
		}

		@Override
		public Class<? extends ICmdLet> commandClass() {
			return ListJournalingActionCommand.class;
		}
	}

}
