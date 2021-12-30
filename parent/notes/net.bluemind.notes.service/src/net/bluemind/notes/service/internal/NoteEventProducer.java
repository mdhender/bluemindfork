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
package net.bluemind.notes.service.internal;

import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.LocalJsonObject;
import net.bluemind.notes.api.VNote;
import net.bluemind.notes.hook.NoteHookAddress;
import net.bluemind.notes.hook.VNoteMessage;

public class NoteEventProducer {

	private final Container container;
	private final String loginAtDomain;
	private final EventBus eventBus;
	private final SecurityContext securityContext;

	public NoteEventProducer(Container container, SecurityContext sc, EventBus ev) {
		this.container = container;
		this.loginAtDomain = sc.getSubject();
		this.eventBus = ev;
		this.securityContext = sc;
	}

	public void vnoteCreated(String uid, VNote vnote) {
		VNoteMessage msg = getVNoteMessage(uid, vnote);
		eventBus.publish(NoteHookAddress.CREATED, new LocalJsonObject<>(msg));
	}

	public void vnoteUpdated(String uid, VNote oldVnote, VNote vnote) {
		VNoteMessage msg = getVNoteMessage(uid, vnote);
		msg.oldVnote = oldVnote;
		eventBus.publish(NoteHookAddress.UPDATED, new LocalJsonObject<>(msg));
	}

	public void vnoteDeleted(String uid, VNote vnote) {
		VNoteMessage msg = getVNoteMessage(uid, vnote);

		eventBus.publish(NoteHookAddress.DELETED, new LocalJsonObject<>(msg));
	}

	public void changed() {
		JsonObject body = new JsonObject();
		body.put("loginAtDomain", loginAtDomain);
		eventBus.publish(NoteHookAddress.getChangedEventAddress(container.uid), body);

		eventBus.publish(NoteHookAddress.CHANGED,
				new JsonObject().put("container", container.uid).put("type", container.type)
						.put("loginAtDomain", loginAtDomain).put("domainUid", securityContext.getContainerUid()));
	}

	private VNoteMessage getVNoteMessage(String uid, VNote vnote) {
		VNoteMessage ret = new VNoteMessage();
		ret.itemUid = uid;
		ret.vnote = vnote;
		ret.securityContext = securityContext;
		ret.container = this.container;
		return ret;
	}

}
