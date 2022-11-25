/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2022
  *
  * This file is part of Blue Mind. Blue Mind is a messaging and collaborative
  * solution.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of either the GNU Affero General Public License as
  * published by the Free Software Foundation (version 3 of the License)
  * or the CeCILL as published by CeCILL.info (version 2 of the License).
  *
  * There are special exceptions to the terms and conditions of the
  * licenses as they are applied to this program. See LICENSE.txt in
  * the directory of this program distribution.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.node.server.handlers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermissions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;

public class MakeDirs implements Handler<HttpServerRequest> {
	private static final Logger logger = LoggerFactory.getLogger(MakeDirs.class);

	@Override
	public void handle(final HttpServerRequest req) {
		req.exceptionHandler(t -> do500(t, req));
		req.endHandler(v -> {
			if (!req.response().ended()) {
				req.response().end();
			}
		});
		req.bodyHandler((Buffer body) -> {
			JsonObject jso = new JsonObject(body.toString());
			if (logger.isDebugEnabled()) {
				logger.debug("mkdirs request {}", jso.encodePrettily());
			}
			Path dst = Paths.get(jso.getString("dst"));
			// Like "rwxrwx---"
			String permissions = jso.getString("permissions", "");
			String owner = jso.getString("owner", "");
			String group = jso.getString("group", "");

			logger.info("mkdirs {}", dst);

			try {
				if (!permissions.isEmpty()) {
					Files.createDirectories(dst,
							PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString(permissions)));
				} else {
					Files.createDirectories(dst);
				}
				if (!owner.isBlank()) {
					var user = dst.getFileSystem().getUserPrincipalLookupService().lookupPrincipalByName(owner);
					Files.setOwner(dst, user);
				}
				if (!group.isBlank()) {
					var groupp = dst.getFileSystem().getUserPrincipalLookupService().lookupPrincipalByGroupName(group);
					Files.getFileAttributeView(dst, PosixFileAttributeView.class, LinkOption.NOFOLLOW_LINKS)
							.setGroup(groupp);
				}
			} catch (IOException e) {
				do500(e, req);
			}

		});
	}

	private void do500(Throwable t, HttpServerRequest req) {
		logger.error(t.getMessage(), t);
		req.response().setStatusCode(500).end();
	}
}
