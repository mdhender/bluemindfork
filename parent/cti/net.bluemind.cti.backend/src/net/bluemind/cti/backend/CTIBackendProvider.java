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

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.eclipse.common.RunnableExtensionLoader;

public class CTIBackendProvider {

	private static List<ICTIBackend> backends;

	static {
		CTIBackendProvider.backends = new RunnableExtensionLoader<ICTIBackend>()
				.loadExtensionsWithPriority("net.bluemind.cti", "backend", "cti-backend", "backend");
	}

	public static ICTIBackend getBackend(String domain, String userUid) {

		for (ICTIBackend backend : CTIBackendProvider.backends) {
			if (backend.supports(domain, userUid)) {
				return backend;
			}
		}
		throw new ServerFault("No supported CTI implementation found");
	}

}
