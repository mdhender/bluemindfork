/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2024
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
package net.bluemind.imap.endpoint;

import java.util.concurrent.atomic.AtomicReference;

import net.bluemind.system.api.SystemState;
import net.bluemind.system.stateobserver.IStateListener;

public class StateListener implements IStateListener {

	private static final AtomicReference<SystemState> cur = new AtomicReference<>(SystemState.CORE_STATE_UNKNOWN);

	@Override
	public void stateChanged(SystemState newState) {
		cur.set(newState);
	}

	public static SystemState state() {
		return cur.get();
	}
}
