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

public class AndRule extends DefaultRule implements MailRule {

	@Override
	public String identifier() {
		return "AndRule";
	}

	@Override
	public String description() {
		return "Rule evaluating all children using an AND criteria";
	}

	@Override
	public MailRuleEvaluation evaluate(Message message, IClientContext mailflowContext) {
		Map<String, String> data = new HashMap<>();
		for (MailRule mailRule : children) {
			MailRuleEvaluation cEval = mailRule.evaluate(message, mailflowContext);
			if (!cEval.matches) {
				return MailRuleEvaluation.rejected();
			}
			data.putAll(cEval.data);
		}
		return MailRuleEvaluation.accepted(data);
	}

}