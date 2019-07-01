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
package net.bluemind.filehosting.service;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.filehosting.api.IFileHosting;
import net.bluemind.filehosting.service.internal.FileHostingService;

public class FileHostingServiceFactory implements ServerSideServiceProvider.IServerSideServiceFactory<IFileHosting> {

	@Override
	public Class<IFileHosting> factoryClass() {
		return IFileHosting.class;
	}

	@Override
	public IFileHosting instance(BmContext context, String... params) throws ServerFault {
		String domainUid = params[0];
		return new FileHostingService(context.getSecurityContext(), domainUid);
	}

}
