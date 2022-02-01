/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2018
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
package net.bluemind.system.stateobserver.testhelper.internal;

import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.system.api.SystemState;
import net.bluemind.system.stateobserver.IStateListener;
import net.bluemind.system.stateobserver.testhelper.StateTestHelper;

public class TestHelperStateListener implements IStateListener {

	@Override
	public void stateChanged(SystemState newState) {
		if (newState == SystemState.CORE_STATE_RUNNING) {
			VertxPlatform.eventBus().publish(StateTestHelper.BUS_ADDR, "running !!");
		}
	}

}
