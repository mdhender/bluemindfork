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
package net.bluemind.backend.cyrus.replication.server.utils;

import net.bluemind.backend.cyrus.partitions.CyrusBoxes;
import net.bluemind.backend.cyrus.partitions.CyrusBoxes.ReplicatedBox;
import net.bluemind.backend.cyrus.replication.server.Token;

public class ReplicatedBoxes {

	/**
	 * Computes partition from domain part & mailbox name from local part of a fully
	 * qualified userName.
	 * 
	 * Returns null otherwise.
	 * 
	 * @param userName fully qualified user name
	 * @return a {@link ReplicatedBox}
	 */
	public static ReplicatedBox forLoginAtDomain(String userName) {
		return CyrusBoxes.forLoginAtDomain(userName);
	}

	/**
	 * Input is "ex2016.vmw!user.tom.Deleted Messages" (or without the quotes)
	 * 
	 * @param fromBox
	 * @return
	 */
	public static ReplicatedBox forCyrusMailbox(String fromBox) {
		String fromMbox = Token.atomOrValue(fromBox);
		return CyrusBoxes.forCyrusMailbox(fromMbox);
	}

}
