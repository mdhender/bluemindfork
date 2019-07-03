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

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import net.bluemind.mailflow.common.api.Message;
import net.bluemind.mailflow.rbe.IClientContext;
import net.bluemind.mailflow.rbe.MailRule;
import net.bluemind.mailflow.rbe.MailRuleEvaluation;

public class RecipientIsExternalRule extends DefaultRule implements MailRule {

	@Override
	public String identifier() {
		return "RecipientIsExternalRule";
	}

	@Override
	public String description() {
		return "Checks if one of the recipients is external";
	}

	@Override
	public MailRuleEvaluation evaluate(Message message, IClientContext context) {
		Set<String> aliases = new HashSet<>(context.getSenderDomain().value.aliases);
		aliases.add(context.getSenderDomain().uid);

		Stream<String> recipientStream = message.recipients.stream();

		if (recipientStream.anyMatch(rec -> isExternal(rec, aliases))) {
			return MailRuleEvaluation.accepted();
		} else {
			return MailRuleEvaluation.rejected();
		}
	}

	private boolean isExternal(String rec, Set<String> aliases) {
		String domain = rec.substring(rec.indexOf("@") + 1);
		return aliases.stream().noneMatch(alias -> domain.equals(alias));
	}

}
