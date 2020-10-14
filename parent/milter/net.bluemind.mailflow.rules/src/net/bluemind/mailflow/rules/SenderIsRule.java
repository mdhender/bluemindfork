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

import java.util.Map;

import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.IDirectory;
import net.bluemind.mailflow.common.api.Message;
import net.bluemind.mailflow.rbe.IClientContext;
import net.bluemind.mailflow.rbe.MailRule;
import net.bluemind.mailflow.rbe.MailRuleEvaluation;

public class SenderIsRule extends DefaultRule implements MailRule {

	@Override
	public String identifier() {
		return "SenderIsRule";
	}

	@Override
	public String description() {
		return "Rule matches if mail sender is the given entry";
	}

	@Override
	public MailRuleEvaluation evaluate(Message message, IClientContext mailflowContext) {
		String from = message.sendingAs.from;

		if (SenderMatcherFactory.get(configuration, mailflowContext).matches(configuration, from)) {
			return MailRuleEvaluation.accepted();
		} else {
			return MailRuleEvaluation.rejected();
		}
	}

	static class SenderMatcherFactory {

		static SenderMatcher get(Map<String, String> configuration, IClientContext mailflowContext) {
			if (configuration.containsKey("dirEntryUid")) {
				return (config, from) -> {
					IDirectory dir = mailflowContext.provider().instance(IDirectory.class,
							mailflowContext.getSenderDomain().uid);
					DirEntry entry = dir.getByEmail(from);

					return entry != null && entry.entryUid.equals(configuration.get("dirEntryUid"));

				};
			} else {
				return (config, from) -> from.equals(configuration.get("email"));
			}
		}
	}

	@FunctionalInterface
	interface SenderMatcher {
		boolean matches(Map<String, String> configuration, String from);
	}

}
