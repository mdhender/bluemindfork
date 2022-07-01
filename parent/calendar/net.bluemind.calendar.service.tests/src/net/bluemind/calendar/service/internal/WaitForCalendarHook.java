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
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.calendar.service.internal;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import net.bluemind.calendar.hook.ICalendarHook;
import net.bluemind.calendar.hook.VEventMessage;

public class WaitForCalendarHook implements ICalendarHook {

	public static Map<String, CompletableFuture<Void>> observers = new HashMap<>();

	@Override
	public void onEventCreated(VEventMessage message) {
		notify(message.itemUid);
	}

	@Override
	public void onEventUpdated(VEventMessage message) {
		notify(message.itemUid);
	}

	@Override
	public void onEventDeleted(VEventMessage message) {
		notify(message.itemUid);
	}

	public static CompletableFuture<Void> register(String uid) {
		if (observers.containsKey(uid)) {
			return observers.get(uid);
		}
		CompletableFuture<Void> future = new CompletableFuture<>();
		observers.put(uid, future);
		return future;
	}

	private void notify(String itemUid) {
		CompletableFuture<Void> future = observers.remove(itemUid);
		if (future != null) {
			future.complete(null);
		}
	}

}
