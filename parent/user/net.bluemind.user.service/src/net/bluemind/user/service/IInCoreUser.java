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
package net.bluemind.user.service;

import java.util.List;
import java.util.Set;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.group.member.IInCoreGroupMember;
import net.bluemind.user.api.IUser;

public interface IInCoreUser extends IUser, IInCoreGroupMember {
	public boolean passwordUpdateNeeded(String login);

	public boolean checkPassword(String login, String password) throws ServerFault;

	public void deleteUserIdentitiesForMailbox(String uid) throws ServerFault;

	public void deleteUserIdentitiesForMailbox(String userUid, String mailboxUid) throws ServerFault;

	public Set<String> directResolvedRoles(String userUid, List<String> groups) throws ServerFault;
}
