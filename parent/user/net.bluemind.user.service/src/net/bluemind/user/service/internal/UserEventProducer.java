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
package net.bluemind.user.service.internal;

import java.util.HashMap;
import java.util.Map;

import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.service.DirEventProducer;
import net.bluemind.user.api.User;
import net.bluemind.user.api.UsersHookAddress;

public class UserEventProducer extends DirEventProducer {

	public UserEventProducer(String domainUid, EventBus ev) {
		super(domainUid, DirEntry.Kind.USER.name(), ev);

	}

	public void changed(String uid, User user) {
		Map<String, String> data = new HashMap<>();
		data.put("login", user.login);
		super.changed(uid, data);
		eventBus.publish(UsersHookAddress.BASE_ADDRESS, new JsonObject().put("domain", domainUid));
	}

	public void deleted(String uid, User user) {
		Map<String, String> data = new HashMap<>();
		data.put("login", user.login);
		super.deleted(uid, data);
		eventBus.publish(UsersHookAddress.BASE_ADDRESS, new JsonObject().put("domain", domainUid));
	}

	public void passwordUpdated(String uid) {
		eventBus.publish(UsersHookAddress.PASSWORD_UPDATED, new JsonObject().put("domain", domainUid).put("uid", uid));
	}
}
