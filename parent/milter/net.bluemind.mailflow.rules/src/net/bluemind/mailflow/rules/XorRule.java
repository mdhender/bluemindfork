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
package net.bluemind.mailflow.rules;

import java.util.HashMap;
import java.util.Map;

import net.bluemind.mailflow.common.api.Message;
import net.bluemind.mailflow.rbe.IClientContext;
import net.bluemind.mailflow.rbe.MailRule;
import net.bluemind.mailflow.rbe.MailRuleEvaluation;

public class XorRule extends DefaultRule implements MailRule {

	@Override
	public String identifier() {
		return "XorRule";
	}

	@Override
	public String description() {
		return "Rule evaluating all children using a XOR criteria";
	}

	@Override
	public MailRuleEvaluation evaluate(Message message, IClientContext mailflowContext) {
		if (children.size() != 2) {
			return MailRuleEvaluation.rejected();
		}
		Map<String, String> data = new HashMap<>();
		MailRuleEvaluation cEval1 = children.get(0).evaluate(message, mailflowContext);
		MailRuleEvaluation cEval2 = children.get(1).evaluate(message, mailflowContext);
		if (cEval1.matches == cEval2.matches) {
			return MailRuleEvaluation.rejected();
		}
		if (cEval1.matches) {
			data.putAll(cEval1.data);
		}
		if (cEval2.matches) {
			data.putAll(cEval2.data);
		}
		return MailRuleEvaluation.accepted(data);
	}
}