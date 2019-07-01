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
package net.bluemind.directory.hollow.datamodel.producer;

import net.bluemind.core.container.model.ItemValue;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.hollow.datamodel.producer.Value.StringValue;
import net.bluemind.mailshare.api.Mailshare;

public class MailshareSerializer extends DirEntrySerializer {

	private ItemValue<Mailshare> mailshare;

	protected MailshareSerializer(ItemValue<Mailshare> mailshare, ItemValue<DirEntry> dirEntry, String domainUid) {
		super(dirEntry, domainUid);
		this.mailshare = mailshare;
	}

	public Value get(Property property) {
		switch (property) {
		case DisplayName:
			return new StringValue(mailshare.displayName);
		case SmtpAddress:
			return getDefaultSmtp();
		case Account:
		case AddressBookDisplayNamePrintableAscii:
			return new StringValue(mailshare.value.name);
		default:
			return super.get(property);
		}
	}

	private Value getDefaultSmtp() {
		if (dirEntry.value.email != null) {
			return new StringValue(dirEntry.value.email);
		} else {
			return new StringValue(mailshare.value.name + "@" + domainUid);
		}
	}

}
