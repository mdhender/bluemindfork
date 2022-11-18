/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2018
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

package net.bluemind.systemcheck.collect;

import java.util.List;
import java.util.Map;

import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.system.api.IInstallation;

public class SubscriptionContactCollector implements IDataCollector {
	public void collect(IServiceProvider provider, Map<String, String> collected) throws Exception {
		List<String> contacts = provider.instance(IInstallation.class).getSubscriptionContacts();
		if (!contacts.isEmpty()) {
			collected.put("check.subscriptionContact", String.join(", ", contacts));
		}
	}
}
