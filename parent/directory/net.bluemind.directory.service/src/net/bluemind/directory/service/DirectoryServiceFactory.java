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
package net.bluemind.directory.service;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.directory.api.IDirectory;
import net.bluemind.directory.service.internal.DirectoryService;
import net.bluemind.domain.api.Domain;

public class DirectoryServiceFactory extends AbstractDirServiceFactory<IDirectory>
		implements ServerSideServiceProvider.IServerSideServiceFactory<IDirectory> {

	@Override
	public Class<IDirectory> factoryClass() {
		return IDirectory.class;
	}

	@Override
	protected IDirectory instanceImpl(BmContext context, ItemValue<Domain> domainValue, Container container)
			throws ServerFault {
		return new DirectoryService(context, container, domainValue);
	}
}
