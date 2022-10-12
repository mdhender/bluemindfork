/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2016
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
package net.bluemind.mailbox.api.utils;

import java.util.List;

import net.bluemind.mailbox.api.rules.MailFilterRule;
import net.bluemind.mailbox.api.rules.conditions.MailFilterRuleCondition;
import net.bluemind.mailbox.api.rules.conditions.MailFilterRuleFilter;
import net.bluemind.mailbox.api.rules.conditions.MailFilterRuleFilterContains;
import net.bluemind.mailbox.api.rules.conditions.MailFilterRuleFilterEquals;
import net.bluemind.mailbox.api.rules.conditions.MailFilterRuleFilterMatches;
import net.bluemind.mailbox.api.rules.conditions.MailFilterRuleOperatorName;

public class RuleParser {

	/**
	 * visit {@link #criteria}
	 * 
	 * @param handler visitor
	 */
	public static void visit(MailFilterRule rule, RuleHandler handler) {
		if (rule.conditions == null) {
			return;
		}

		rule.conditions.forEach(condition -> parseCondition(condition, handler));
	}

	private static void parseCondition(MailFilterRuleCondition condition, RuleHandler handler) {
		MailFilterRuleFilter filter = condition.filter();
		String field = convert(filter.fields.get(0));
		if (field == null) {
			return;
		}
		if (filter.operator == MailFilterRuleOperatorName.EXISTS && !condition.negate) {
			handler.exists(field);
		} else if (filter.operator == MailFilterRuleOperatorName.EXISTS && condition.negate) {
			handler.doesnotExist(field);
		} else if (filter.operator == MailFilterRuleOperatorName.EQUALS) {
			String value = firstValue(((MailFilterRuleFilterEquals) filter).values, "");
			if (!condition.negate) {
				handler.is(field, value);
			} else {
				handler.isNot(field, value);
			}
		} else if (filter.operator == MailFilterRuleOperatorName.CONTAINS) {
			String value = firstValue(((MailFilterRuleFilterContains) filter).values, "");
			if (!condition.negate) {
				handler.contains(field, value);
			} else {
				handler.doesnotContain(field, value);
			}
		} else if (filter.operator == MailFilterRuleOperatorName.MATCHES) {
			String value = firstValue(((MailFilterRuleFilterMatches) filter).values, "");
			if (!condition.negate) {
				handler.matches(field, value);
			} else {
				handler.doesnotMatch(field, value);
			}
		}
	}

	private static String firstValue(List<String> values, String orDefault) {
		return (values != null && !values.isEmpty()) ? values.get(0) : orDefault;
	}

	private static String convert(String field) {
		if (field.startsWith("from")) {
			return "FROM";
		} else if (field.startsWith("to")) {
			return "TO";
		} else if (field.startsWith("subject")) {
			return "SUBJECT";
		} else if (field.startsWith("part.content")) {
			return "BODY";
		} else if (field.startsWith("headers.")) {
			return field.split("\\.")[1];
		} else {
			return null;
		}
	}
}
