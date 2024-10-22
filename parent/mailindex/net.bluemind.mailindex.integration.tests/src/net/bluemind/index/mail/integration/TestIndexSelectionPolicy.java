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
package net.bluemind.index.mail.integration;

import java.util.List;

import net.bluemind.mailindex.hook.IIndexSelectionPolicy;

public class TestIndexSelectionPolicy implements IIndexSelectionPolicy {

	public static boolean distribute;
	private static int current = 0;

	@Override
	public String getMailspoolIndexName(List<String> shards, String mailboxUid) {
		if (!distribute) {
			return shards.get(0);
		}
		if (mailboxUid.equals("user00")) {
			TestIndexSelectionPolicy.current = 0;
		}
		String index = shards.get(TestIndexSelectionPolicy.current % 25);
		TestIndexSelectionPolicy.current++;
		System.err.println("assigning " + index + " to " + mailboxUid);
		return index;
	}

}
