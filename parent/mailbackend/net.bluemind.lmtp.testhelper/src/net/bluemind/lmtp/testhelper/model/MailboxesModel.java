/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2017
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
package net.bluemind.lmtp.testhelper.model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import net.bluemind.lmtp.testhelper.model.FakeMailbox.State;

public class MailboxesModel {

	private static final MailboxesModel mdl = new MailboxesModel();

	public static MailboxesModel get() {
		return mdl;
	}

	public final Map<String, FakeMailbox> knownRecipients = new HashMap<>();
	public final Set<String> validSenders = new HashSet<>();
	public final Set<String> domains = new HashSet<>();

	public void reset() {
		validSenders.clear();
		knownRecipients.clear();
	}

	public MailboxesModel addValidSender(String email) {
		newEmail(email);
		validSenders.add(email);
		addMailbox(new FakeMailbox(email, State.Ok));
		return this;
	}

	private void newEmail(String email) {
		domains.add(email.split("@")[1]);
	}

	public boolean isValidSender(String email) {
		return validSenders.contains(email);
	}

	public MailboxesModel addMailbox(FakeMailbox fm) {
		knownRecipients.put(fm.email, fm);
		return this;
	}

	public Optional<FakeMailbox> mailbox(String email) {
		return Optional.ofNullable(knownRecipients.get(email));
	}

}
