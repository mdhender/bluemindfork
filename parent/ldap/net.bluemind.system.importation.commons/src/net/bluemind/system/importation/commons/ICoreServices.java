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
package net.bluemind.system.importation.commons;

import java.util.List;
import java.util.Map;
import java.util.Set;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.group.api.Group;
import net.bluemind.group.api.Member;
import net.bluemind.mailbox.api.MailFilter;
import net.bluemind.mailbox.api.Mailbox.Routing;
import net.bluemind.user.api.User;

/**
 * @author Anthony Prades <anthony.prades@blue-mind.net>
 *
 */
public interface ICoreServices {
	public class ExtUidState {
		public final Set<String> suspended;
		public final Set<String> active;

		public ExtUidState(Map<Boolean, Set<String>> extUidByStatus) {
			suspended = extUidByStatus.get(true);
			active = extUidByStatus.get(false);
		}
	}

	public Map<String, String> getUserStats();

	public Map<String, String> getGroupStats();

	/**
	 * @param deletedGroupExtId
	 * @throws ServerFault
	 */
	public void deleteGroup(String deletedGroupUid);

	/**
	 * @param uid
	 * @param value
	 * @throws ServerFault
	 */
	public void createGroup(ItemValue<Group> group);

	/**
	 * @param uid
	 * @param value
	 * @throws ServerFault
	 */
	public void updateGroup(ItemValue<Group> group);

	/**
	 * @param user
	 * @throws ServerFault
	 */
	void suspendUser(ItemValue<User> user);

	/**
	 * @param user
	 * @throws ServerFault
	 */
	void unsuspendUser(ItemValue<User> user);

	/**
	 * @param user
	 */
	public void createUser(ItemValue<User> user);

	/**
	 * @param user
	 * @throws ServerFault
	 */
	public void updateUser(ItemValue<User> user);

	/**
	 * @param extIdPrefix
	 * @return
	 * @throws ServerFault
	 */
	public Set<String> getImportedGroupsExtId();

	/**
	 * @return
	 */
	public ExtUidState getUsersExtIdByState();

	/**
	 * @param uuid
	 * @return
	 * @throws ServerFault
	 */
	public MailFilter getMailboxFilter(String uuid);

	/**
	 * @param uid
	 * @param mailFilter
	 * @throws ServerFault
	 */
	public void setMailboxFilter(String mailboxUid, MailFilter filter);

	/**
	 * @param extId
	 * @return
	 * @throws ServerFault
	 */
	public ItemValue<Group> getGroupByExtId(String extId);

	/**
	 * @param name
	 * @return
	 * @throws ServerFault
	 */
	public ItemValue<Group> getGroupByName(String name);

	/**
	 * @param uid
	 * @return
	 * @throws ServerFault
	 */
	public List<Member> getGroupMembers(String uid);

	/**
	 * @param uid
	 * @param groupsToRemove
	 * @throws ServerFault
	 */
	public void removeMembers(String uid, List<Member> membersToRemove);

	/**
	 * @param uid
	 * @param groupsToAdd
	 * @throws ServerFault
	 */
	public void addMembers(String uid, List<Member> membersToAdd);

	/**
	 * @param uuid
	 * @return
	 * @throws ServerFault
	 */
	public ItemValue<User> getUserByExtId(String extId);

	/**
	 * @param uid
	 * @return
	 * @return
	 * @throws ServerFault
	 */
	public List<ItemValue<Group>> memberOf(String uid);

	/**
	 * @param uid
	 * @param photo
	 * @throws ServerFault
	 */
	public void userSetPhoto(String uid, byte[] photo);

	/**
	 * @param uid
	 * @throws ServerFault
	 */
	public void userDeletePhoto(String uid);

	/**
	 * @param internal
	 * @param member
	 * @return
	 */
	public void setUserMailRouting(Routing routing, String userUid);
}