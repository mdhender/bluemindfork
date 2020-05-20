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

import java.io.File;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;
import net.bluemind.core.api.AsyncHandler;
import net.bluemind.resource.api.IResourcesAsync;

public class ResourceIconUploadHandler extends BaseUploadHandler {

	private static Logger logger = LoggerFactory.getLogger(ResourceIconUploadHandler.class);

	@Override
	public void handle(final HttpServerRequest request) {
		final String domainUid = request.params().get("domainUid");
		final String resourceTypeUid = request.params().get("resourceId");
		String iconId = request.params().get("iconId");
		UUID parsed = null;
		try {
			parsed = UUID.fromString(iconId);
		} catch (IllegalArgumentException e) {
			request.response().setStatusCode(500).end();
			return;
		}

		if (domainUid == null || resourceTypeUid == null || iconId == null) {
			request.response().setStatusCode(500);
			request.response().setStatusMessage("bad paramters");
			request.response().end();
			return;
		}

		File file = repository.getTempFile(parsed);
		if (file == null || !file.exists()) {
			request.response().setStatusCode(500);
			request.response().setStatusMessage("temp file doesnt exists anymore");
			request.response().end();
			return;
		}

		vertx.fileSystem().readFile(file.getPath(), new Handler<AsyncResult<Buffer>>() {

			@Override
			public void handle(AsyncResult<Buffer> event) {
				doSetIcon(request, domainUid, resourceTypeUid, event.result().getBytes());
			}
		});

	}

	protected void doSetIcon(final HttpServerRequest request, final String domainUid, final String resourceUid,
			byte[] data) {
		IResourcesAsync rt = getProvider(request).instance("bm/core", IResourcesAsync.class, domainUid);
		rt.setIcon(resourceUid, data, new AsyncHandler<Void>() {

			@Override
			public void success(Void value) {
				request.response().setStatusCode(200);
				request.response().end();
			}

			@Override
			public void failure(Throwable e) {
				logger.error("error during set image to resource type {}:{} : {}", domainUid, resourceUid,
						e.getMessage());
				request.response().setStatusCode(500);
				request.response().setStatusMessage(e.getMessage() != null ? e.getMessage() : "null");
				request.response().end();
			}
		});
	}

}
