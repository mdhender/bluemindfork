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
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.cli.journaling.utils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.google.common.base.Strings;

import net.bluemind.cli.cmd.api.CliContext;
import net.bluemind.cli.cmd.api.CliException;
import net.bluemind.cli.utils.CliUtils;
import net.bluemind.core.api.Regex;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.domain.api.Domain;
import net.bluemind.mailflow.api.IMailflowRules;
import net.bluemind.mailflow.api.MailRuleActionAssignment;
import net.bluemind.mailflow.api.MailRuleActionAssignmentDescriptor;

public class JournalingUtils {
	public static final String ACTION_IDENTIFIER = "JournalingAction";
	public static final String TARGET_EMAIL_KEY = "targetEmail";
	public static final String EMAILS_FILTERED_KEY = "emailsFiltered";
	public static final String DEFAULT_RULE_IDENTIFIER = "MatchAlwaysRule";

	private final CliUtils cliUtils;
	private final CliContext ctx;
	private final IMailflowRules mailfloxApi;

	public JournalingUtils(CliContext ctx, String domainName) {
		this.ctx = ctx;
		this.cliUtils = new CliUtils(ctx);
		this.mailfloxApi = loadDomainCtx(domainName);
	}

	private IMailflowRules loadDomainCtx(String domainName) {
		if (Strings.isNullOrEmpty(domainName)) {
			throw new CliException("Domain name is mandatory");
		}

		ItemValue<Domain> domain = cliUtils.getDomain(domainName)
				.orElseThrow(() -> new CliException(String.format("Domain '%s' not found", domainName)));

		return ctx.adminApi().instance(IMailflowRules.class, domain.uid);
	}

	public IMailflowRules getMailfloxApi() {
		return mailfloxApi;
	}

	public void checkTargetEmail(String targetEmail) {
		if (Strings.isNullOrEmpty(targetEmail) || !Regex.EMAIL.validate(targetEmail)) {
			throw new CliException("Invalid target email format : " + targetEmail);
		}
	}

	public void checkEmailsFiltered(String[] filteredEmails) {
		if (filteredEmails.length > 0) {
			String invalidEmails = Arrays.asList(filteredEmails).stream().filter(e -> !Regex.EMAIL.validate(e))
					.collect(Collectors.joining(", "));
			if (!Strings.isNullOrEmpty(invalidEmails)) {
				throw new CliException("Invalid filtered emails format : " + invalidEmails);
			}
		}
	}

	public boolean checkRuleIdentifier(String ruleIdentifier) {
		if (Strings.isNullOrEmpty(ruleIdentifier)) {
			return false;
		}

		String authorizedRules = mailfloxApi.listRules().stream().map(r -> r.ruleIdentifier)
				.collect(Collectors.joining(", "));
		if (!authorizedRules.contains(ruleIdentifier)) {
			throw new CliException("Invalid rule identifier : " + ruleIdentifier + "(authorized rules are : ["
					+ authorizedRules + "])");
		}
		return true;
	}

	public void checkJournalingAssignment(String actionUid, MailRuleActionAssignment assignment) {
		if (assignment == null) {
			throw new CliException("Journaling action '" + actionUid + "' not found");
		}
		if (!JournalingUtils.ACTION_IDENTIFIER.equals(assignment.actionIdentifier)) {
			throw new CliException("Action '" + actionUid + "' is not a JournalingAction");
		}
	}

	public void displayAssignment(String uid, MailRuleActionAssignmentDescriptor assignmentDesc) {
		MailRuleActionAssignment assignment = (MailRuleActionAssignment) assignmentDesc;
		assignment.uid = uid;
		displayAssignments(Arrays.asList(assignment));
	}

	public void displayAssignments(List<MailRuleActionAssignment> assignments) {
		int size = assignments.size();
		String[] headers = { "Rule Identifier", "Assignment UID", "Target email", "Filtered emails", "Active" };
		String[][] asTable = new String[size][headers.length];

		int i = 0;
		for (MailRuleActionAssignment assignment : assignments) {
			asTable[i][0] = assignment.rules.ruleIdentifier;
			asTable[i][1] = assignment.uid;
			asTable[i][2] = assignment.actionConfiguration.get(JournalingUtils.TARGET_EMAIL_KEY);
			String emailsFiltered = assignment.actionConfiguration.get(JournalingUtils.EMAILS_FILTERED_KEY);
			if (!Strings.isNullOrEmpty(emailsFiltered)) {
				asTable[i][3] = emailsFiltered;
			}
			asTable[i][4] = Boolean.toString(assignment.isActive);
			i++;
		}
		ctx.info(cliUtils.getAsciiTable(headers, asTable));
	}

}
