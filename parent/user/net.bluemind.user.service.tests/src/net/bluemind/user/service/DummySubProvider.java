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
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.user.service;

import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.system.api.SubscriptionInformations;
import net.bluemind.system.api.SubscriptionInformations.InstallationIndicator;
import net.bluemind.system.api.SubscriptionInformations.InstallationIndicator.Kind;
import net.bluemind.system.helper.distrib.list.Distribution;
import net.bluemind.system.subscriptionprovider.ISubscriptionProvider;

public class DummySubProvider implements ISubscriptionProvider {

	@Override
	public SubscriptionInformations loadSubscriptionInformations() throws ServerFault {
		SubscriptionInformations lic = new SubscriptionInformations();
		lic.customer = "test-customer";
		lic.kind = SubscriptionInformations.Kind.PROD;
		lic.dealer = "test-dealer";
		lic.distributor = "test-distributor";
		lic.valid = true;
		lic.indicator = new ArrayList<>(1);
		InstallationIndicator visioIndicator = new InstallationIndicator();
		visioIndicator.kind = Kind.FullVisioAccount;
		visioIndicator.expiration = Date.valueOf(LocalDate.now().plusMonths(4L));
		lic.indicator.add(visioIndicator);
		return lic;
	}

	@Override
	public void removeSubscription(Distribution os) throws ServerFault {
		// not implemented, only for tests
	}

	@Override
	public byte[] getRawSubscription() throws ServerFault {
		return new byte[0];
	}

	@Override
	public void updateSubscription(byte[] sub, Distribution serverOS) throws ServerFault {
		// not implemented, only for tests
	}

	@Override
	public void updateSubscriptionUrl(Distribution detect, String version) {
		// not implemented, only for tests
	}

}
