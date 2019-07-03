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
package net.bluemind.ui.mailbox.sharing;

import net.bluemind.ui.common.client.forms.acl.AclEntity;
import net.bluemind.ui.gwtsharing.client.IAclEntityValidator;
import net.bluemind.ui.gwtsharing.client.ValidationResult;
import net.bluemind.ui.mailbox.l10n.MailboxConstants;

public class CrossShardSharingValidator implements IAclEntityValidator {
		
		private IMailboxSharingEditor editor;

		public  CrossShardSharingValidator(IMailboxSharingEditor editor) {
			this.editor = editor;
		}
		
		@Override
		public ValidationResult validate(AclEntity subject) {
			
			String containerDatalocation = editor.getMailboxDataLocation();
			if (containerDatalocation.equals(subject.getEntry().dataLocation)) {
				return ValidationResult.valid();
			} else {
				return ValidationResult.invalid(MailboxConstants.INST.crossShardSharingForbidden());
			}
		}
}
