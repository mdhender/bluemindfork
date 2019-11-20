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
package net.bluemind.authentication.service;

import java.util.List;
import java.util.UUID;

import javax.sql.DataSource;

import net.bluemind.authentication.api.APIKey;
import net.bluemind.authentication.api.IAPIKeys;
import net.bluemind.authentication.persistence.APIKeyStore;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.context.SecurityContext;

public class APIKeysService implements IAPIKeys {

	private APIKeyStore store;
	private SecurityContext context;

	public APIKeysService(DataSource pool, SecurityContext context) {
		store = new APIKeyStore(pool, context);
		this.context = context;
	}

	@Override
	public APIKey create(String displayName) throws ServerFault {
		if (context.isAnonymous()) {
			throw new ServerFault("Invalid securityContext", ErrorCode.PERMISSION_DENIED);
		}

		if (displayName == null || displayName.trim().isEmpty()) {
			throw new ServerFault("API key display name cannot be empty");
		}

		APIKey apikey = new APIKey();
		apikey.sid = UUID.randomUUID().toString();
		apikey.displayName = displayName;
		store.create(apikey);

		return store.get(apikey.sid);
	}

	@Override
	public void delete(String sid) throws ServerFault {
		if (context.isAnonymous()) {
			throw new ServerFault("Invalid securityContext", ErrorCode.PERMISSION_DENIED);
		}

		store.delete(sid);
	}

	@Override
	public List<APIKey> list() throws ServerFault {
		if (context.isAnonymous()) {
			throw new ServerFault("Invalid securityContext", ErrorCode.PERMISSION_DENIED);
		}

		return store.list();
	}

	@Override
	public APIKey get(String sid) throws ServerFault {
		return store.get(sid);
	}

}
