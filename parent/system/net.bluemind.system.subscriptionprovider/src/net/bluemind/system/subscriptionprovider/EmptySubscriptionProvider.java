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

import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.system.api.SubscriptionInformations;
import net.bluemind.system.helper.distrib.list.Distribution;

public class EmptySubscriptionProvider implements ISubscriptionProvider {

	@Override
	public SubscriptionInformations loadSubscriptionInformations() throws ServerFault {
		SubscriptionInformations lic = new SubscriptionInformations();
		lic.customer = "None";
		lic.kind = SubscriptionInformations.Kind.NONE;
		lic.dealer = "BlueMind";
		lic.distributor = "BlueMind";
		lic.valid = false;
		return lic;
	}

	@Override
	public void updateSubscription(byte[] subscription, Distribution serverOs) throws ServerFault {
		throw new ServerFault("Updating a subscription is not supported using this provider", ErrorCode.NOT_FOUND);
	}

	@Override
	public void removeSubscription(Distribution os) throws ServerFault {
	}

	@Override
	public byte[] getRawSubscription() throws ServerFault {
		return null;
	}

}
