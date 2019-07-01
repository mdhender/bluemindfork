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

import net.bluemind.core.rest.BmContext;
import net.bluemind.core.sanitizer.ISanitizer;
import net.bluemind.core.sanitizer.ISanitizerFactory;
import net.bluemind.mailflow.api.MailRuleActionAssignmentDescriptor;

public class SignatureActionSanitizerFactory implements ISanitizerFactory<MailRuleActionAssignmentDescriptor> {

	@Override
	public Class<MailRuleActionAssignmentDescriptor> support() {
		return MailRuleActionAssignmentDescriptor.class;
	}

	@Override
	public ISanitizer<MailRuleActionAssignmentDescriptor> create(BmContext context) {
		return new SignatureActionSanitizer();
	}

}
