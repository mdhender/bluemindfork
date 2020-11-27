/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2020
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
package net.bluemind.user.hook.identity;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.rest.BmContext;
import net.bluemind.user.api.User;
import net.bluemind.user.api.UserMailIdentity;

/**
 * Hook interface for {@link User} changes
 *
 */
public interface IUserMailIdentityHook {

	/**
	 * This is called before a user create
	 * 
	 * @param context
	 * @param domainUid
	 * @param uid
	 * @param identity
	 * @throws ServerFault
	 */
	void beforeCreate(BmContext context, String domainUid, String uid, UserMailIdentity identity);

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
	void beforeUpdate(BmContext context, String domainUid, String uid, UserMailIdentity update,
			UserMailIdentity previous);

	/**
	 * This is called before a user delete
	 * 
	 * @param context
	 * @param domainUid
	 * @param uid
	 * @param previous
	 * @throws ServerFault
	 */
	void beforeDelete(BmContext context, String domainUid, String uid, UserMailIdentity previous);

}
