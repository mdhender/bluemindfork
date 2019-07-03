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
package net.bluemind.cti.backend;

import java.util.List;

import net.bluemind.cti.backend.internal.NoCtiBackend;
import net.bluemind.eclipse.common.RunnableExtensionLoader;

public class CTIBackendProvider {

	private static ICTIBackend backend;

	static {
		List<ICTIBackend> backends = new RunnableExtensionLoader<ICTIBackend>().loadExtensions("net.bluemind.cti",
				"backend", "cti-backend", "backend");
		if (!backends.isEmpty()) {
			backend = backends.get(0);
		} else {
			backend = new NoCtiBackend();
		}
	}

	public static ICTIBackend getBackend() {
		return backend;
	}
}
