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
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

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

	private static final ListeningExecutorService getAttachExecutor = MoreExecutors
			.listeningDecorator(Executors.newCachedThreadPool());

	@Override
	public void handle(AuthorizedDeviceQuery dq) {
		final IBackend backend = Backends.dataAccess();
		final BackendSession bs = SessionWrapper.wrap(dq);
		final Responder responder = new VertxResponder(dq.request(), dq.request().response());

		final String an = dq.optionalParams().attachmentName();
		final String localAddr = "future." + an + "." + System.nanoTime();
		logger.info("GetAttachment, submit");
		final ListenableFuture<MSAttachementData> future = getAttachExecutor.submit(new Callable<MSAttachementData>() {

			@Override
			public MSAttachementData call() throws Exception {
				logger.info("getEmailAttachment from executor service");
				MSAttachementData attach = backend.getContentsExporter(bs).getEmailAttachement(bs, an);
				return attach;
			}
		});
		final EventBus eb = VertxPlatform.eventBus();
		Handler<Message<Boolean>> futureResolved = new Handler<Message<Boolean>>() {

			@Override
			public void handle(Message<Boolean> event) {
				logger.info("Future resolved.");
				try {
					MSAttachementData attach = future.get();
					responder.sendResponseFile(attach.getContentType(), attach.getFile().source().openStream());
					attach.getFile().dispose();
				} catch (Exception e) {
					logger.error(e.getMessage(), e);
					responder.sendStatus(500);
				}
				eb.unregisterHandler(localAddr, this);
			}
		};
		eb.registerLocalHandler(localAddr, futureResolved);

		Futures.addCallback(future, new FutureCallback<MSAttachementData>() {
			public void reply() {
				eb.send(localAddr, true);
			}

			@Override
			public void onSuccess(MSAttachementData attach) {
				reply();
			}

			@Override
			public void onFailure(Throwable t) {
				reply();
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
