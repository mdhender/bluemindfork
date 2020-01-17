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
package net.bluemind.directory.service;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;

public class DirEventProducer {

	protected EventBus eventBus;
	protected String domainUid;
	public static final String address = "dir.changed";
	public static final String addressDeleted = "dir.entry.deleted";

	public DirEventProducer(String domainUid, EventBus ev) {
		this.domainUid = domainUid;
		this.eventBus = ev;

	}

	public void changed(String uid, Map<String, String> additionalValues) {
		JsonObject data = new JsonObject().put("domain", domainUid).put("uid", uid);
		additionalValues.forEach(data::put);
		eventBus.publish(address, data);
	}

	public void changed(String uid, long version, Map<String, String> additionalValues) {
		Map<String, String> data = new HashMap<>();
		data.put("version", Long.toString(version));
		data.putAll(additionalValues);
		changed(uid, additionalValues);
	}

	public void changed(String uid, long version) {
		changed(uid, version, Collections.emptyMap());
	}

	public void deleted(String uid, long version) {
		changed(uid, version, Collections.emptyMap());
		eventBus.publish(addressDeleted, "");
	}

	public void deleted(String uid, Map<String, String> data) {
		changed(uid, data);
		eventBus.publish(addressDeleted, "");
	}
}
