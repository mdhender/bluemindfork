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
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.network.topology.producer.tests;

import java.util.concurrent.CompletableFuture;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.BmContext;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.server.api.Server;
import net.bluemind.server.hook.DefaultServerHook;

public class LatchHook extends DefaultServerHook {

	public static CompletableFuture<Void> currentPromise;

	@Override
	public void onServerCreated(BmContext context, ItemValue<Server> item) throws ServerFault {
		unlock();
	}

	@Override
	public void onServerUpdated(BmContext context, ItemValue<Server> previousValue, Server value) throws ServerFault {
		unlock();
	}

	@Override
	public void onServerDeleted(BmContext context, ItemValue<Server> itemValue) throws ServerFault {
		unlock();
	}

	@Override
	public void onServerTagged(BmContext context, ItemValue<Server> itemValue, String tag) throws ServerFault {
		unlock();
	}

	private void unlock() {
		final CompletableFuture<Void> ref = currentPromise;
		if (ref != null) {
			// wait for more than the producer delay
			VertxPlatform.getVertx().setTimer(1000, tid -> {
				ref.complete(null);
			});
		}
	}

}
