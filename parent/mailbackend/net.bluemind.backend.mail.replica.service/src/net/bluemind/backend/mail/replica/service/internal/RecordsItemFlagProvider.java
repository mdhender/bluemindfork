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
package net.bluemind.backend.mail.replica.service.internal;

import java.util.Collection;
import java.util.EnumSet;

import net.bluemind.backend.mail.api.flags.MailboxItemFlag;
import net.bluemind.backend.mail.replica.api.MailboxRecord;
import net.bluemind.backend.mail.replica.api.MailboxRecord.InternalFlag;
import net.bluemind.core.container.model.ItemFlag;
import net.bluemind.core.container.service.internal.ContainerStoreService.IItemFlagsProvider;

public class RecordsItemFlagProvider implements IItemFlagsProvider<MailboxRecord> {

	@Override
	public Collection<ItemFlag> flags(MailboxRecord value) {
		Collection<ItemFlag> flags = EnumSet.noneOf(ItemFlag.class);
		if (value.flags.contains(MailboxItemFlag.System.Seen.value())) {
			flags.add(ItemFlag.Seen);
		}
		if (value.flags.contains(MailboxItemFlag.System.Deleted.value())
				|| value.internalFlags.contains(InternalFlag.expunged)) {
			flags.add(ItemFlag.Deleted);
		}
		if (value.flags.contains(MailboxItemFlag.System.Flagged.value())) {
			flags.add(ItemFlag.Important);
		}
		return flags;
	}

}
