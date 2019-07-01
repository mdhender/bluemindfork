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
package net.bluemind.mailflow.service.internal;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.sanitizer.ISanitizer;
import net.bluemind.mailflow.api.MailRuleActionAssignmentDescriptor;

public class MailflowAssignmentSanitizer implements ISanitizer<MailRuleActionAssignmentDescriptor> {

	@Override
	public void create(MailRuleActionAssignmentDescriptor assignment) throws ServerFault {
		assignment.position = Math.min(99, assignment.position);
	}

	@Override
	public void update(MailRuleActionAssignmentDescriptor current, MailRuleActionAssignmentDescriptor newValue)
			throws ServerFault {
		create(newValue);
	}

}
