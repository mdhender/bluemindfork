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
package net.bluemind.ui.gwtsharing.client;

import java.util.List;

import com.google.gwt.core.client.JavaScriptObject;

import net.bluemind.core.api.AsyncHandler;
import net.bluemind.core.container.api.ContainerQuery;
import net.bluemind.core.container.api.IContainersAsync;
import net.bluemind.core.container.api.gwt.endpoint.ContainersGwtEndpoint;
import net.bluemind.core.container.model.ContainerDescriptor;
import net.bluemind.ui.common.client.forms.Ajax;

public abstract class BaseOwnerSharingsModelHandler extends BaseSharingsModelHandler {

	private final String type;

	public BaseOwnerSharingsModelHandler(String type, String modelId) {
		super(modelId);
		this.type = type;
	}

	@Override
	protected void loadContainers(JavaScriptObject model, AsyncHandler<List<ContainerDescriptor>> handler) {
		IContainersAsync containers = new ContainersGwtEndpoint(Ajax.TOKEN.getSessionId());
		containers.all(ContainerQuery.ownerAndType(getOwner(model), type), handler);

	}

	protected abstract String getOwner(JavaScriptObject model);

}
