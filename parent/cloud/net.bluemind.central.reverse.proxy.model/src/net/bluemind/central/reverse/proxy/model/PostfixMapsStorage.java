/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2023
  *
  * This file is part of Blue Mind. Blue Mind is a messaging and collaborative
  * solution.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of either the GNU Affero General Public License as
  * published by the Free Software Foundation (version 3 of the License)
  * or the CeCILL as published by CeCILL.info (version 2 of the License).
  *
  * There are special exceptions to the terms and conditions of the
  * licenses as they are applied to this program. See LICENSE.txt in
  * the directory of this program distribution.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.central.reverse.proxy.model;

import java.util.Collection;
import java.util.Set;

import net.bluemind.central.reverse.proxy.model.common.DirInfo.DirEmail;
import net.bluemind.central.reverse.proxy.model.impl.HashMapPostfixMapsStorage;

public interface PostfixMapsStorage {
	static HashMapPostfixMapsStorage create() {
		return new HashMapPostfixMapsStorage();
	}

	void updateDataLocation(String uid, String ip);

	void removeDataLocation(String uid);

	void updateDomain(String domainUid, Set<String> aliases);

	Collection<String> domainAliases(String domainUid);

	boolean domainManaged(String domainAlias);

	void updateDomainSettings(String domainUid, String mailRoutingRelay, boolean mailForwardUnknown);

	void removeDomain(String domainUid);

	void updateMailbox(String domainUid, String uid, String name, String routing, String dataLocationUid);

	void removeMailbox(String uid);

	boolean mailboxManaged(String name);

	void addRecipient(String groupUid, String recipientType, String recipientUid);

	void removeRecipient(String groupUid, String recipientType, String recipientUid);

	Collection<String> aliasToMailboxes(String email);

	void updateEmails(String uid, Collection<DirEmail> emails);

	void removeUid(String uid);

	String mailboxRelay(String mailbox);
}
