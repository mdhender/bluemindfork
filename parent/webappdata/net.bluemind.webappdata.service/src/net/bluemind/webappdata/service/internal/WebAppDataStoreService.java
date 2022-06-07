/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2022
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
package net.bluemind.webappdata.service.internal;

import javax.sql.DataSource;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.service.internal.ContainerStoreService;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.webappdata.api.WebAppData;
import net.bluemind.webappdata.persistence.WebAppDataStore;

public class WebAppDataStoreService extends ContainerStoreService<WebAppData> {

	private WebAppDataStore store;

	public WebAppDataStoreService(DataSource pool, SecurityContext securityContext, Container container) {
		super(pool, securityContext, container, new WebAppDataStore(pool, container));
		this.store = new WebAppDataStore(pool, container);
	}

	WebAppData getByKey(String key) {
		try {
			return store.getByKey(key);
		} catch (Exception e) {
			throw ServerFault.sqlFault(e);
		}
	}
}
