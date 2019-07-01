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
package net.bluemind.system.subscriptionprovider;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.system.api.SubscriptionInformations;
import net.bluemind.system.helper.distrib.list.Distribution;

public interface ISubscriptionProvider {

	public SubscriptionInformations loadSubscriptionInformations() throws ServerFault;

	/**
	 * @param sub
	 * @throws ServerFault
	 *             code == ErrorCode.NOT_FOUND if update subscription is not
	 *             available
	 */
	public void updateSubscription(byte[] sub, Distribution serverOS) throws ServerFault;

	public void removeSubscription(Distribution distribution) throws ServerFault;

	public byte[] getRawSubscription() throws ServerFault;
}
