/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2021
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
package net.bluemind.core.container.service.internal;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.backup.continuous.api.Providers;
import net.bluemind.system.api.SystemState;
import net.bluemind.system.state.StateContext;

public class ReadOnlyMode {
	private ReadOnlyMode() {
	}

	public static void checkWritable() {
		if (StateContext.getState() == SystemState.CORE_STATE_DEMOTED || (!Providers.get().leadership().isLeader()
				&& StateContext.getState() != SystemState.CORE_STATE_CLONING)) {
			throw new ServerFault("instance is not writable as state is " + SystemState.CORE_STATE_DEMOTED);
		}
	}
}
