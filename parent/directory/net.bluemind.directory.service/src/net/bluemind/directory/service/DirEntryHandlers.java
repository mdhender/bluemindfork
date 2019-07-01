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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.directory.api.BaseDirEntry.Kind;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.eclipse.common.RunnableExtensionLoader;

public class DirEntryHandlers {

	private static final DirEntryHandlers INSTANCE = new DirEntryHandlers();

	private Map<DirEntry.Kind, DirEntryHandler> handlers;

	private DirEntryHandlers() {

		handlers = new HashMap<DirEntry.Kind, DirEntryHandler>();
		RunnableExtensionLoader<DirEntryHandler> loader = new RunnableExtensionLoader<DirEntryHandler>();
		List<DirEntryHandler> handlers = loader.loadExtensions("net.bluemind.directory", "handler", "handler", "class");

		for (DirEntryHandler h : handlers) {
			this.handlers.put(h.kind(), h);
		}
	}

	private static DirEntryHandlers getInstance() {
		return INSTANCE;
	}

	/**
	 * get {@link DirEntryHandler} for this kind
	 * 
	 * @param kind
	 * @return handler
	 * @throws ServerFault
	 *                         if no handler for this kind
	 */
	public static DirEntryHandler byKind(Kind kind) throws ServerFault {

		DirEntryHandler handler = getInstance().handlers.get(kind);
		if (handler == null) {
			throw new ServerFault("not handler for kind " + kind);
		}

		return handler;
	}

}
