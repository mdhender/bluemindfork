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

	Map<String, String> getUserStats();

	Map<String, String> getGroupStats();

	/**
	 * @param deletedGroupExtId
	 * @throws ServerFault
	 */
	void deleteGroup(String deletedGroupUid);

	/**
	 * @param uid
	 * @param value
	 * @throws ServerFault
	 */
	void createGroup(ItemValue<Group> group);

	/**
	 * @param uid
	 * @param value
	 * @throws ServerFault
	 */
	void updateGroup(ItemValue<Group> group);

	/**
	 * @param user
	 * @throws ServerFault
	 */
	void suspendUser(ItemValue<User> user);

	/**
	 * @param user
	 * @throws ServerFault
	 */
	void createUser(ItemValue<User> user);

	/**
	 * @param user
	 * @throws ServerFault
	 */
	void updateUser(ItemValue<User> user);

	/**
	 * @param extIdPrefix
	 * @return
	 * @throws ServerFault
	 */
	List<String> getImportedGroupsExtId();

	/**
	 * @param extIdPrefix
	 * @return
	 * @throws ServerFault
	 */
	List<String> getImportedUsersExtId();

	/**
	 * @param uuid
	 * @return
	 * @throws ServerFault
	 */
	MailFilter getMailboxFilter(String uuid);

	/**
	 * @param uid
	 * @param mailFilter
	 * @throws ServerFault
	 */
	void setMailboxFilter(String mailboxUid, MailFilter filter);

	/**
	 * @param extId
	 * @return
	 * @throws ServerFault
	 */
	ItemValue<Group> getGroupByExtId(String extId);

	/**
	 * @param name
	 * @return
	 * @throws ServerFault
	 */
	ItemValue<Group> getGroupByName(String name);

	/**
	 * @param uid
	 * @return
	 * @throws ServerFault
	 */
	List<Member> getGroupMembers(String uid);

	/**
	 * @param uid
	 * @param groupsToRemove
	 * @throws ServerFault
	 */
	void removeMembers(String uid, List<Member> membersToRemove);

	/**
	 * @param uid
	 * @param groupsToAdd
	 * @throws ServerFault
	 */
	void addMembers(String uid, List<Member> membersToAdd);

	/**
	 * @param uuid
	 * @return
	 * @throws ServerFault
	 */
	ItemValue<User> getUserByExtId(String extId);

	/**
	 * @param uid
	 * @return
	 * @return
	 * @throws ServerFault
	 */
	List<ItemValue<Group>> memberOf(String uid);

	/**
	 * @param uid
	 * @return
	 * @throws ServerFault
	 */
	String userExternalId(String uid);

	/**
	 * @param uid
	 * @return
	 * @throws ServerFault
	 */
	String groupExternalId(String uid);

	/**
	 * @param uid
	 * @param photo
	 * @throws ServerFault
	 */
	void userSetPhoto(String uid, byte[] photo);

	/**
	 * @param uid
	 * @throws ServerFault
	 */
	void userDeletePhoto(String uid);

	/**
	 * @param uid
	 * @param mailboxQuota
	 */
	void setMailboxQuota(String uid, int mailboxQuota);

	/**
	 * @param internal
	 * @param member
	 * @return
	 */
	void setUserMailRouting(Routing routing, String userUid);
}