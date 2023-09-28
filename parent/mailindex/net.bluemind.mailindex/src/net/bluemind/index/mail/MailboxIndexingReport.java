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
package net.bluemind.index.mail;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MailboxIndexingReport {

	public static record Entry(String folder, int messageId) {
	}

	private List<Entry> entries = Collections.emptyList();

	public static MailboxIndexingReport create() {
		MailboxIndexingReport ret = new MailboxIndexingReport();
		ret.entries = new ArrayList<>();
		return ret;
	}

	public void add(String mailbox, int messageId) {
		entries.add(new Entry(mailbox, messageId));
	}

	public boolean hasErrors() {
		return !entries.isEmpty();
	}

	public List<Entry> listEntries() {
		return entries;
	}
}
