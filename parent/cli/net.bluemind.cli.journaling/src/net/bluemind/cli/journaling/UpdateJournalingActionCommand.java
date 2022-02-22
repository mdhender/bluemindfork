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

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

import com.google.common.base.Strings;

import net.bluemind.cli.cmd.api.CliContext;
import net.bluemind.cli.cmd.api.ICmdLet;
import net.bluemind.cli.cmd.api.ICmdLetRegistration;
import net.bluemind.cli.journaling.utils.JournalingUtils;
import net.bluemind.mailflow.api.MailRuleActionAssignment;
import net.bluemind.mailflow.api.MailflowRule;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "update", description = "Update a journaling mailflow action rule")
public class UpdateJournalingActionCommand implements ICmdLet, Runnable {
	private CliContext ctx;

	@Option(required = true, names = { "--domain", "-d" }, description = "The domain name")
	public String domainName;

	@Option(required = true, names = { "--uid" }, description = "The action rule uid to update")
	public String actionUid;

	@Option(required = false, names = { "--target", "-t" }, description = "Target email for journalisation")
	public String targetEmail;

	@Option(required = false, names = { "--filter",
			"-f" }, description = "Filtered emails separated by ';', default: all domain's emails")
	public String[] filteredEmails;

	@Option(required = false, names = { "--rule", "-r" }, description = "Rule identifier")
	public String ruleIdentifier;

	@Override
	public void run() {
		JournalingUtils journalingUtils = new JournalingUtils(ctx, domainName);

		MailRuleActionAssignment assignmentToUpdate = journalingUtils.getMailfloxApi().getAssignment(actionUid);
		journalingUtils.checkJournalingAssignment(actionUid, assignmentToUpdate);

		if (journalingUtils.checkRuleIdentifier(ruleIdentifier)) {
			MailflowRule rule = new MailflowRule();
			rule.ruleIdentifier = ruleIdentifier;
			assignmentToUpdate.rules = rule;
		}

		if (!Strings.isNullOrEmpty(targetEmail)) {
			journalingUtils.checkTargetEmail(targetEmail);
			assignmentToUpdate.actionConfiguration.put(JournalingUtils.TARGET_EMAIL_KEY, targetEmail);
		}
		if (filteredEmails.length > 0) {
			journalingUtils.checkEmailsFiltered(filteredEmails);
			assignmentToUpdate.actionConfiguration.put(JournalingUtils.EMAILS_FILTERED_KEY,
					Arrays.asList(filteredEmails).stream().collect(Collectors.joining(";")));
		}

		journalingUtils.getMailfloxApi().update(actionUid, assignmentToUpdate);
		ctx.info("Journaling action '{}' updated", actionUid);
		journalingUtils.displayAssignment(actionUid, assignmentToUpdate);
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
			return UpdateJournalingActionCommand.class;
		}
	}

}
