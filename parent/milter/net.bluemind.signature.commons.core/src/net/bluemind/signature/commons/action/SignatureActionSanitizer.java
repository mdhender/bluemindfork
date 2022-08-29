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
package net.bluemind.signature.commons.action;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.sanitizer.ISanitizer;
import net.bluemind.mailflow.api.MailRuleActionAssignmentDescriptor;

public class SignatureActionSanitizer implements ISanitizer<MailRuleActionAssignmentDescriptor> {

	@Override
	public void create(MailRuleActionAssignmentDescriptor assignment) throws ServerFault {
		if (assignment.actionIdentifier.equals("AddSignatureAction")) {
			assignment.actionConfiguration.put("html", sanitize(assignment.actionConfiguration.get("html")));
		}
	}

	@Override
	public void update(MailRuleActionAssignmentDescriptor current, MailRuleActionAssignmentDescriptor newValue)
			throws ServerFault {
		create(newValue);
	}

	private String sanitize(String html) {
		return Jsoup.clean(html, Safelist.relaxed().addTags("style").addAttributes(":all", "style")
				.addProtocols("img", "src", "data").addProtocols("a", "href", "#").addProtocols("a", "href", "callto"));
	}

}