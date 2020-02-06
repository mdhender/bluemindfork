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
package net.bluemind.eas.command.getattachment;

import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;

import io.vertx.core.WorkerExecutor;
import net.bluemind.eas.backend.BackendSession;
import net.bluemind.eas.backend.IBackend;
import net.bluemind.eas.backend.MSAttachementData;
import net.bluemind.eas.http.AuthorizedDeviceQuery;
import net.bluemind.eas.http.IEasRequestEndpoint;
import net.bluemind.eas.impl.Backends;
import net.bluemind.eas.impl.Responder;
import net.bluemind.eas.impl.vertx.compat.SessionWrapper;
import net.bluemind.eas.impl.vertx.compat.VertxResponder;
import net.bluemind.lib.vertx.VertxPlatform;

public final class GetAttachmentEndpoint implements IEasRequestEndpoint {

	private static final Logger logger = LoggerFactory.getLogger(GetAttachmentEndpoint.class);

	private static final WorkerExecutor getAttachExecutor = VertxPlatform.getVertx()
			.createSharedWorkerExecutor("get-attach", 4);

	@Override
	public void handle(AuthorizedDeviceQuery dq) {
		final IBackend backend = Backends.dataAccess();
		final BackendSession bs = SessionWrapper.wrap(dq);
		final Responder responder = new VertxResponder(dq.request(), dq.request().response());

		final String an = dq.optionalParams().attachmentName();
		logger.info("GetAttachment, submit");

		getAttachExecutor.<MSAttachementData>executeBlocking((prom) -> {
			MSAttachementData attach;
			try {
				attach = backend.getContentsExporter(bs).getEmailAttachement(bs, an);
				prom.complete(attach);
			} catch (Exception e) {
				prom.fail(e);
			}
		}, false, (result) -> {
			if (result.succeeded()) {
				try {
					MSAttachementData attach = result.result();
					responder.sendResponseFile(attach.getContentType(), attach.getFile().source().openStream());
					attach.getFile().dispose();
				} catch (Exception e) {
					logger.error(e.getMessage(), e);
					responder.sendStatus(500);
				}
			} else {
				logger.error(result.cause().getMessage(), result.cause());
				responder.sendStatus(500);
			}
		});

	}

	@Override
	public Collection<String> supportedCommands() {
		return ImmutableList.of("GetAttachment");
	}

	@Override
	public boolean acceptsVersion(double protocolVersion) {
		return protocolVersion < 14;
	}

}
