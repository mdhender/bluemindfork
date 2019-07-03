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
package net.bluemind.exchange.mapi.service.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.rest.BmContext;
import net.bluemind.exchange.mapi.api.IMapiFoldersMgmt;
import net.bluemind.exchange.mapi.api.IMapiMailbox;
import net.bluemind.user.api.User;
import net.bluemind.user.hook.DefaultUserHook;

public class MapiUserHook extends DefaultUserHook {

	private static final Logger logger = LoggerFactory.getLogger(MapiUserHook.class);

	@Override
	public void beforeDelete(BmContext context, String domainUid, String uid, User user) throws ServerFault {
		logger.info("Deleting MAPI folders of {}", uid);

		try {
			IMapiFoldersMgmt mapiFoldersApi = context.su().provider().instance(IMapiFoldersMgmt.class, domainUid, uid);
			mapiFoldersApi.deleteAll();
		} catch (ServerFault sf) {
			if (sf.getCode() == ErrorCode.NOT_FOUND) {
				logger.warn("mapi replica is missing (step1)");
			} else {
				throw sf;
			}
		}
		try {
			IMapiMailbox mapiApi = context.su().provider().instance(IMapiMailbox.class, domainUid, uid);
			mapiApi.delete();
		} catch (ServerFault sf) {
			if (sf.getCode() == ErrorCode.NOT_FOUND) {
				logger.warn("mapi replica is missing (step2)");
			} else {
				throw sf;
			}
		}
	}

}
