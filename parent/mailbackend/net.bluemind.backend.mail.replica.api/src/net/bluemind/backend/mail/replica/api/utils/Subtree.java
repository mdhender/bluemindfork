/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2019
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
package net.bluemind.backend.mail.replica.api.utils;

import com.google.common.base.MoreObjects;

import net.bluemind.backend.mail.replica.api.MailboxReplicaRootDescriptor.Namespace;

public class Subtree {

	public String ownerUid;
	public String mailboxName;
	public Namespace namespace;
	public String domainUid;

	public Subtree() {
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(Subtree.class)//
				.add("domainUid", domainUid)//
				.add("namespace", namespace.name())//
				.add("mailboxName", mailboxName)//
				.add("ownerUid", ownerUid)//
				.toString();
	}

	public String subtreeUid() {
		return "subtree_" + domainUid.replace('.', '_') + "!" + namespace.prefix() + ownerUid;
	}

}
