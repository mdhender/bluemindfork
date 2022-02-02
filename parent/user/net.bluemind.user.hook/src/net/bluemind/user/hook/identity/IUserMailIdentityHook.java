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

import net.bluemind.core.rest.BmContext;
import net.bluemind.user.api.UserMailIdentity;

/**
 * Hook interface for {@link UserMailIdentities} changes
 *
 */
public interface IUserMailIdentityHook {

	void beforeCreate(BmContext context, String domainUid, String uid, UserMailIdentity identity);

	void onIdentityCreated(BmContext context, String domainUid, String userUid, String id, UserMailIdentity current);

	void beforeUpdate(BmContext context, String domainUid, String uid, UserMailIdentity update,
			UserMailIdentity previous);

	void onIdentityUpdated(BmContext context, String domainUid, String userUid, String id, UserMailIdentity current,
			UserMailIdentity previous);

	void beforeDelete(BmContext context, String domainUid, String uid, UserMailIdentity previous);

	void onIdentityDeleted(BmContext context, String domainUid, String userUid, String id, UserMailIdentity previous);

	void onIdentityDefault(BmContext context, String domainUid, String userUid, String id);

}
