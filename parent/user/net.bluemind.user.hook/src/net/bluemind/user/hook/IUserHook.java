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
package net.bluemind.user.hook;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.BmContext;
import net.bluemind.user.api.User;

/**
 * Hook interface for {@link User} changes
 *
 */
public interface IUserHook {

	public boolean handleGlobalVirt();

	/**
	 * This is called before a user create
	 * 
	 * @param context
	 * @param domainUid
	 * @param uid
	 * @param user
	 * @throws ServerFault
	 */
	void beforeCreate(BmContext context, String domainUid, String uid, User user) throws ServerFault;


	/**
	 * This is called before a user update
	 * 
	 * @param context
	 * @param domainUid
	 * @param uid
	 * @param update
	 * @param previous
	 * @throws ServerFault
	 */
	void beforeUpdate(BmContext context, String domainUid, String uid, User update, User previous) throws ServerFault;

	/**
	 * This is called before a user delete
	 * 
	 * @param context
	 * @param domainUid
	 * @param uid
	 * @param previous
	 * @throws ServerFault
	 */
	void beforeDelete(BmContext context, String domainUid, String uid, User previous) throws ServerFault;

	/**
	 * This is called after a user is created in the database
	 * 
	 * @param created
	 * @throws ServerFault
	 */
	void onUserCreated(BmContext context, String domainUid, ItemValue<User> created) throws ServerFault;

	/**
	 * This is called after a user is updated in the database
	 * 
	 * @param previous
	 * @param current
	 * @throws ServerFault
	 */
	void onUserUpdated(BmContext context, String domainUid, ItemValue<User> previous, ItemValue<User> current)
			throws ServerFault;

	/**
	 * This is called after a user is deleted in the database
	 * 
	 * @param previous
	 * @throws ServerFault
	 */
	void onUserDeleted(BmContext context, String domainUid, ItemValue<User> deleted) throws ServerFault;

}
