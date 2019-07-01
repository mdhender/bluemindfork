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
package net.bluemind.mailbox.service;

import java.util.List;

import net.bluemind.eclipse.common.RunnableExtensionLoader;
import net.bluemind.mailbox.service.internal.VoidMailboxesStorage;

public class MailboxesStorageFactory {

	public static IMailboxesStorage getMailStorage() {
		RunnableExtensionLoader<IMailboxesStorage> loader = new RunnableExtensionLoader<IMailboxesStorage>();
		List<IMailboxesStorage> storages = loader.loadExtensions("net.bluemind.mailbox", "storage", "mailbox-storage",
				"class");
		if (storages.size() >= 1) {
			return storages.get(0);
		} else {
			return VoidMailboxesStorage.INSTANCE;
		}
	}
}
