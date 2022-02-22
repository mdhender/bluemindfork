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
import java.util.HashMap;
import java.util.Optional;
import java.util.stream.Collectors;

import net.bluemind.cli.cmd.api.CliContext;
import net.bluemind.cli.cmd.api.ICmdLet;
import net.bluemind.cli.cmd.api.ICmdLetRegistration;
import net.bluemind.cli.journaling.utils.JournalingUtils;
import net.bluemind.core.utils.GUID;
import net.bluemind.mailflow.api.MailRuleActionAssignmentDescriptor;
import net.bluemind.mailflow.api.MailflowRouting;
import net.bluemind.mailflow.api.MailflowRule;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "create", description = "Add a journaling mailflow action rule")
public class AddJournalingActionCommand implements ICmdLet, Runnable {
	private CliContext ctx;

	@Option(required = true, names = { "--domain", "-d" }, description = "The domain name")
	public String domainName;

	@Option(required = true, names = { "--target", "-t" }, description = "Target email for journalisation")
	public String targetEmail;

	@Option(required = false, names = { "--filter",
			"-f" }, description = "Filtered emails separated by ';', default: all domain's emails")
	public String[] filteredEmails;

	@Option(required = false, names = { "--rule", "-r" }, description = "Rule identifier, default: MatchAlwaysRule")
	public String ruleIdentifier;

	@Override
	public void run() {
		JournalingUtils journalingUtils = new JournalingUtils(ctx, domainName);
		journalingUtils.checkTargetEmail(targetEmail);
		journalingUtils.checkEmailsFiltered(filteredEmails);

		if (!journalingUtils.checkRuleIdentifier(ruleIdentifier)) {
			ruleIdentifier = JournalingUtils.DEFAULT_RULE_IDENTIFIER;
		}

		MailRuleActionAssignmentDescriptor assignment = createAssignmentDescriptor();
		String uid = GUID.get();
		journalingUtils.getMailfloxApi().create(uid, assignment);

		ctx.info("Journaling action '{}' created", uid);
		journalingUtils.displayAssignment(uid, assignment);
	}

	private MailRuleActionAssignmentDescriptor createAssignmentDescriptor() {
		MailRuleActionAssignmentDescriptor assignment = new MailRuleActionAssignmentDescriptor();
		assignment.actionConfiguration = new HashMap<>();
		assignment.actionConfiguration.put(JournalingUtils.TARGET_EMAIL_KEY, targetEmail);
		if (filteredEmails.length > 0) {
			assignment.actionConfiguration.put(JournalingUtils.EMAILS_FILTERED_KEY,
					Arrays.asList(filteredEmails).stream().collect(Collectors.joining(";")));
		}
		assignment.actionIdentifier = JournalingUtils.ACTION_IDENTIFIER;
		assignment.routing = MailflowRouting.ALL;
		assignment.isActive = true;
		assignment.position = 1;

		MailflowRule rule = new MailflowRule();
		rule.ruleIdentifier = ruleIdentifier;
		assignment.rules = rule;

		return assignment;
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
			return AddJournalingActionCommand.class;
		}
	}

}
