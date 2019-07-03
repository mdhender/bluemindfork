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
package net.bluemind.core.container.api;

public class OwnerSubscriptionsBusAddresses {

	public static final String BASE_ADDRESS = "bm." + IOwnerSubscriptionUids.TYPE + ".hook";

	/**
	 * Messages with domain, owner & version (long) are dispatched on this topic
	 */
	public static final String ALL_SUBSCRIPTION_CHANGES = BASE_ADDRESS + ".changed";

	public static final String ownerSubscriptionChanges(String ownerUid, String domainUid) {
		return BASE_ADDRESS + "." + IOwnerSubscriptionUids.getIdentifier(ownerUid, domainUid) + ".changed";
	}
}
