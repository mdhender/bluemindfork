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
package net.bluemind.eas.backend.bm.mail;

import net.bluemind.backend.mail.api.MailboxItem;
import net.bluemind.backend.mail.api.flags.MailboxItemFlag;
import net.bluemind.backend.mail.api.flags.SystemFlag.AnsweredFlag;
import net.bluemind.backend.mail.api.flags.SystemFlag.FlaggedFlag;
import net.bluemind.backend.mail.api.flags.SystemFlag.SeenFlag;
import net.bluemind.eas.dto.email.EmailResponse;
import net.bluemind.eas.dto.email.EmailResponse.Flag;
import net.bluemind.eas.dto.email.EmailResponse.Flag.Status;
import net.bluemind.eas.dto.email.EmailResponse.LastVerbExecuted;

public class FlagsChange {

	public static EmailResponse asEmailResponse(MailboxItem mailboxItem) {
		EmailResponse em = new EmailResponse();

		// Set MessageClass to NULL otherwise WindowsPhone erases
		// email details (summary,...)
		em.messageClass = null;

		em.read = mailboxItem.flags.contains(new SeenFlag());
		em.flag = new Flag();

		if (mailboxItem.flags.contains(new FlaggedFlag())) {
			em.flag.flagType = "Flag for follow-up";
			em.flag.status = Status.Active;
		} else {
			em.flag.status = Status.Cleared;
		}

		if (mailboxItem.flags.contains(new AnsweredFlag())) {
			em.lastVerbExecuted = LastVerbExecuted.ReplyToSender;
		} else if (mailboxItem.flags.contains(new MailboxItemFlag("$Forwarded"))) {
			em.lastVerbExecuted = LastVerbExecuted.Forward;
		}

		return em;
	}

}
