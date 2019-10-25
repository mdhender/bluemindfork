/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2019
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
package net.bluemind.core.sessions;

import java.util.List;

import net.bluemind.eclipse.common.RunnableExtensionLoader;

public class SessionDeletionListeners {

	private static final List<ISessionDeletionListener> listeners = load();

	private SessionDeletionListeners() {

	}

	private static List<ISessionDeletionListener> load() {
		RunnableExtensionLoader<ISessionDeletionListener> rel = new RunnableExtensionLoader<>();
		return rel.loadExtensionsWithPriority("net.bluemind.core.sessions", "deletion", "listener", "impl");
	}

	public static List<ISessionDeletionListener> get() {
		return listeners;
	}
}
