/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2022
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
package net.bluemind.notes.hook.internal;

import java.util.List;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import net.bluemind.core.rest.LocalJsonObject;
import net.bluemind.eclipse.common.RunnableExtensionLoader;
import net.bluemind.notes.hook.INoteHook;
import net.bluemind.notes.hook.NoteHookAddress;
import net.bluemind.notes.hook.VNoteMessage;

public class NoteHookVerticle extends AbstractVerticle {

	@Override
	public void start() {
		RunnableExtensionLoader<INoteHook> loader = new RunnableExtensionLoader<>();
		List<INoteHook> hooks = loader.loadExtensions("net.bluemind.notes", "hook", "hook", "impl");
		EventBus eventBus = vertx.eventBus();

		for (final INoteHook hook : hooks) {
			eventBus.consumer(NoteHookAddress.CREATED, (Message<LocalJsonObject<VNoteMessage>> message) -> vertx
					.executeBlocking(prom -> hook.onNoteCreated(message.body().getValue()), false));
			eventBus.consumer(NoteHookAddress.UPDATED, (Message<LocalJsonObject<VNoteMessage>> message) -> vertx
					.executeBlocking(prom -> hook.onNoteUpdated(message.body().getValue()), false));
			eventBus.consumer(NoteHookAddress.DELETED, (Message<LocalJsonObject<VNoteMessage>> message) -> vertx
					.executeBlocking(prom -> hook.onNoteDeleted(message.body().getValue()), false));
		}

	}
}
