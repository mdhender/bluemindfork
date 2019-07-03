/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2017
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
package net.bluemind.eas.busmods;

import org.vertx.java.busmods.BusModBase;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;

import net.bluemind.eas.dto.EasBusEndpoints;
import net.bluemind.eas.impl.Backends;

public class CoreStateListenerVerticle extends BusModBase {

	@Override
	public void start() {
		super.start();
		eb.registerHandler(EasBusEndpoints.PURGE_SESSIONS, new Handler<Message<Object>>() {

			@Override
			public void handle(Message<Object> event) {
				Backends.dataAccess().purgeSessions();
			}
		});

	}

}
