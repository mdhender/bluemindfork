/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2022
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
package net.bluemind.delivery.lmtp.common;

import com.google.common.base.MoreObjects;

import net.bluemind.core.container.model.ItemValue;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.domain.api.Domain;
import net.bluemind.mailbox.api.Mailbox;

public class ResolvedBox {

	public final DirEntry entry;
	public final ItemValue<Mailbox> mbox;
	public final ItemValue<Domain> dom;
	private final LmtpAddress addr;

	public ResolvedBox(DirEntry entry, ItemValue<net.bluemind.mailbox.api.Mailbox> mbox, ItemValue<Domain> dom) {
		this.entry = entry;
		this.mbox = mbox;
		this.dom = dom;
		this.addr = new LmtpAddress(entry.email);
	}

	public LmtpAddress address() {
		return addr;
	}

	public String getDomainPart() {
		return dom.uid;
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(ResolvedBox.class).add("e", entry.email).add("u", entry.entryUid)
				.add("dn", entry.displayName).toString();
	}

}