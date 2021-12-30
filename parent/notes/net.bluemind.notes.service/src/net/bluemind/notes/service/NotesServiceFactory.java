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
package net.bluemind.notes.service;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.service.internal.RBACManager;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.notes.api.INotes;

public class NotesServiceFactory implements ServerSideServiceProvider.IServerSideServiceFactory<INotes> {

	@Override
	public Class<INotes> factoryClass() {
		return INotes.class;
	}

	@Override
	public INotes instance(BmContext context, String... params) throws ServerFault {
		RBACManager.forContext(context).checkNotAnoynmous();

		return new NotesService(context);
	}

}