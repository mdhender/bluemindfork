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
package net.bluemind.ui.adminconsole.directory.server;

import io.vertx.core.http.HttpServerRequest;
import net.bluemind.core.api.AsyncHandler;
import net.bluemind.resource.api.IResourcesAsync;

public class ResourceIconUploadHandler extends BaseUploadHandler<IResourcesAsync> {

	@Override
	protected String entityIdParameter() {
		return "resourceId";
	}

	@Override
	protected IResourcesAsync entityService(final HttpServerRequest request, final String domainUid) {
		return getProvider(request).instance("bm/core", IResourcesAsync.class, domainUid);
	}

	@Override
	protected void setUploadData(IResourcesAsync entityService, String entityId, byte[] data,
			AsyncHandler<Void> handler) {
		entityService.setIcon(entityId, data, handler);
	}

}
