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
package net.bluemind.core.container.service;

import net.bluemind.core.container.api.IInternalContainerManagement;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.service.internal.ContainerManagement;
import net.bluemind.core.rest.BmContext;

public class InternalContainerManagementFactory extends CommonContainerManagementFactory<IInternalContainerManagement> {

	@Override
	protected IInternalContainerManagement create(BmContext context, Container container) {
		return new ContainerManagement(context, container);
	}

	@Override
	public Class<IInternalContainerManagement> factoryClass() {
		return IInternalContainerManagement.class;
	}
}
