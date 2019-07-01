/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2018
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

import net.bluemind.mailflow.common.api.Message;
import net.bluemind.mailflow.rbe.IClientContext;
import net.bluemind.mailflow.rbe.MailRule;
import net.bluemind.mailflow.rbe.MailRuleEvaluation;

public class SendDateIsAfter extends DefaultRule implements MailRule {

	@Override
	public String identifier() {
		return "SendDateIsAfter";
	}

	@Override
	public String description() {
		return "Rule matches if mail has been sent after a given date";
	}

	@Override
	public MailRuleEvaluation evaluate(Message message, IClientContext context) {
		try {
			if (System.currentTimeMillis() >= Long.parseLong(configuration.get("timestamp"))) {
				return MailRuleEvaluation.accepted();
			}
		} catch (NumberFormatException e) {
		}
		return MailRuleEvaluation.rejected();
	}

}
