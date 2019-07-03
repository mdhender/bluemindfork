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
package net.bluemind.index.mail.impl;

import java.util.List;

import org.elasticsearch.client.Client;

import net.bluemind.mailindex.hook.IIndexSelectionPolicy;

public class RoundRobinIndexSelectionPolicy implements IIndexSelectionPolicy {

	private static int current = 0;

	@Override
	public String getMailspoolIndexName(Client client, List<String> shards, String mailboxUid) {
		if (mailboxUid.equals("user00")) {
			RoundRobinIndexSelectionPolicy.current = 0;
		}
		String index = shards.get(RoundRobinIndexSelectionPolicy.current % 25);
		RoundRobinIndexSelectionPolicy.current++;
		System.err.println("assigning " + index + " to " + mailboxUid);
		return index;
	}

}
