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
package net.bluemind.directory.service;

import net.bluemind.addressbook.api.VCard;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.mailbox.api.Mailbox;

public class DirEntryAndValue<T> {

	public final DirEntry entry;
	public final VCard vcard;
	public final Mailbox mailbox;
	public T value;

	public DirEntryAndValue(DirEntry entry, T value, VCard vcard, Mailbox mailbox) {
		this.entry = entry;
		this.value = value;
		this.vcard = vcard;
		this.mailbox = mailbox;
	}

}
