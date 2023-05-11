/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2023
  *
  * This file is part of BlueMind. BlueMind is a messaging and collaborative
  * solution.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of either the GNU Affero General Public License as
  * published by the Free Software Foundation (version 3 of the License).
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.dataprotect.service.action;

import java.util.ArrayList;
import java.util.List;

import org.apache.james.mime4j.dom.address.Mailbox;

import net.bluemind.core.rest.BmContext;
import net.bluemind.core.sendmail.SendmailHelper;
import net.bluemind.dataprotect.api.Restorable;
import net.bluemind.dataprotect.api.RestorableKind;
import net.bluemind.system.api.IInstallation;
import net.bluemind.user.api.IUser;

public class RecipientHelper {

	public static List<String> getRecipientList(BmContext context, Restorable item) {
		List<String> emails = new ArrayList<>();

		IUser userService = context.provider().instance(IUser.class, item.domainUid);
		if (item.kind == RestorableKind.USER) {
			String address = userService.getComplete(item.liveEntryUid()).value.defaultEmail().address;
			emails.add(address);
		} else if (!context.getSecurityContext().isDomainGlobal()) {
			String address = userService.getComplete(context.getSecurityContext().getSubject()).value
					.defaultEmail().address;
			emails.add(address);
		} else {
			emails.addAll(context.provider().instance(IInstallation.class).getSubscriptionContacts());
		}

		return emails;
	}

	public static Mailbox createNotReplyMailbox(String domainUid) {
		return SendmailHelper.formatAddress(String.format("no-reply@%s", domainUid),
				String.format("no-reply@%s", domainUid));
	}

}
