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

package net.bluemind.server.api;

import net.bluemind.core.api.fault.ServerFault;

public final class NodeUtils {

	public static void waitFor(IServer service, String serverUid, String ref) throws ServerFault {
		CommandStatus status;
		do {
			status = service.getStatus(serverUid, ref);
			if (!status.complete) {
				try {
					Thread.sleep(200);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			}
		} while (!status.complete);
	}

	public static void exec(IServer service, String serverUid, String command) throws ServerFault {
		String ref = service.submit(serverUid, command);
		waitFor(service, serverUid, ref);
	}
}
