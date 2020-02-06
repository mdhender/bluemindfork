/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2017
 *
 * This file is part of Blue Mind. Blue Mind is a messaging and collaborative
 * solution.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of either the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License)
 * or the CeCILL as published by CeCILL.info (version 2 of the License).
 *
 * There are special exceptions to the terms and conditions of the
 * licenses as they are applied to this program. See LICENSE.txt in
 * the directory of this program distribution.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.core.container.service.internal;

import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;
import net.bluemind.core.container.api.OwnerSubscriptionsBusAddresses;

public class OwnerSubscriptionsEventProducer {

	private String domainUid;
	private String ownerUid;
	private EventBus eventBus;

	public OwnerSubscriptionsEventProducer(String domainUid, String ownerUid, EventBus bus) {
		this.domainUid = domainUid;
		this.ownerUid = ownerUid;
		this.eventBus = bus;
	}

	public void changed(long version) {
		JsonObject change = new JsonObject().put("version", version);
		change.put("domain", domainUid).put("owner", ownerUid);
		eventBus.publish(OwnerSubscriptionsBusAddresses.ALL_SUBSCRIPTION_CHANGES, change);
		eventBus.publish(OwnerSubscriptionsBusAddresses.ownerSubscriptionChanges(ownerUid, domainUid), change);

	}

}
