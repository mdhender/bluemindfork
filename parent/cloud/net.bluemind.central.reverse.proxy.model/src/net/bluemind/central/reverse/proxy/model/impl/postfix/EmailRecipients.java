/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2023
  *
  * This file is part of Blue Mind. Blue Mind is a messaging and collaborative
  * solution.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of either the GNU Affero General Public License as
  * published by the Free Software Foundation (version 3 of the License)
  * or the CeCILL as published by CeCILL.info (version 2 of the License).
  *
  * There are special exceptions to the terms and conditions of the
  * licenses as they are applied to this program. See LICENSE.txt in
  * the directory of this program distribution.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.central.reverse.proxy.model.impl.postfix;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class EmailRecipients {
	public record Recipient(String type, String uid) {
	}

	private Map<String, Set<Recipient>> recipients = new HashMap<>();

	public void addEmailRecipient(String emailUid, String recipientType, String recipientUid) {
		Recipient recipient = new Recipient(recipientType, recipientUid);

		recipients.computeIfAbsent(emailUid, uid -> new HashSet<>()).add(recipient);
	}

	public void removeEmailRecipient(String emailUid, String recipientType, String recipientUid) {
		Set<Recipient> r = recipients.get(emailUid);
		if (r != null) {
			Recipient toRemove = new Recipient(recipientType, recipientUid);
			r.removeIf(recipient -> recipient.equals(toRemove));

			if (r.isEmpty()) {
				recipients.remove(emailUid);
			}
		}
	}

	public Set<Recipient> getRecipients(String emailUid) {
		return recipients.get(emailUid);
	}

	public boolean hasRecipients(String emailUid) {
		return recipients.containsKey(emailUid);
	}

	public void remove(String emailUid) {
		recipients.remove(emailUid);
		recipients.values().forEach(ur -> ur.removeIf(ru -> ru.uid.equals(emailUid)));
	}
}
