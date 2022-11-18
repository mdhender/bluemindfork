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
package net.bluemind.systemcheck.checks;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.system.api.IInstallation;
import net.bluemind.system.api.SubscriptionInformations;

public class SubscriptionExpirationCheck extends AbstractCheck {

	@Override
	public CheckResult verify(IServiceProvider provider, SetupCheckResults results, Map<String, String> collected)
			throws Exception {
		SubscriptionInformations subInfo = provider.instance(IInstallation.class).getSubscriptionInformations();

		if (subInfo == null || subInfo.kind == SubscriptionInformations.Kind.NONE) {
			return cr(CheckState.ERROR, "check.subscription.none", "NONE");
		}

		LocalDate ends = subInfo.ends.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
		LocalDate now = LocalDate.now();
		CheckResult cr = null;
		if (now.isAfter(ends)) {
			cr = cr(CheckState.ERROR, "check.subscription.expiration.expired",
					String.valueOf(ChronoUnit.DAYS.between(ends, now)));
		} else {
			cr = cr(CheckState.OK, "check.subscription.expiration.active",
					String.valueOf(ChronoUnit.DAYS.between(now, ends)));
		}

		return cr;
	}

}
