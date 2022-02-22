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

import java.util.Optional;

import net.bluemind.cli.cmd.api.CliContext;
import net.bluemind.cli.cmd.api.ICmdLet;
import net.bluemind.cli.cmd.api.ICmdLetRegistration;
import net.bluemind.cli.journaling.utils.JournalingUtils;
import net.bluemind.mailflow.api.MailRuleActionAssignment;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "enable", description = "Enable a journaling mailflow action rule")
public class EnableJournalingActionCommand implements ICmdLet, Runnable {
	private CliContext ctx;

	@Option(required = true, names = { "--domain", "-d" }, description = "The domain name")
	public String domainName;

	@Option(required = true, names = { "--uid" }, description = "The action rule uid")
	public String actionUid;

	@Override
	public void run() {
		JournalingUtils journalingUtils = new JournalingUtils(ctx, domainName);

		MailRuleActionAssignment assignment = journalingUtils.getMailfloxApi().getAssignment(actionUid);
		journalingUtils.checkJournalingAssignment(actionUid, assignment);

		assignment.isActive = true;
		journalingUtils.getMailfloxApi().update(actionUid, assignment);
		ctx.info("Journaling action '{}' enabled", assignment.uid);
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
			return EnableJournalingActionCommand.class;
		}
	}

}
