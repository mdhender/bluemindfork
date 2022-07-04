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
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.imap.endpoint.driver;

import java.util.List;

import net.bluemind.eclipse.common.RunnableExtensionLoader;

public class Drivers {

	private static final MailboxDriver selected = loadDriver();

	private Drivers() {
	}

	public static MailboxDriver activeDriver() {
		return selected;
	}

	private static MailboxDriver loadDriver() {
		RunnableExtensionLoader<MailboxDriver> rel = new RunnableExtensionLoader<>();
		List<MailboxDriver> loaded = rel.loadExtensionsWithPriority("net.bluemind.imap.endpoint", "drivers", "driver",
				"impl");
		return loaded.isEmpty() ? null : loaded.get(0);
	}

}
