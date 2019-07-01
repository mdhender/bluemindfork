/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2016
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
package net.bluemind.backend.cyrus.replication.server;

import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.file.FileSystem;
import org.vertx.java.core.net.NetSocket;
import org.vertx.java.core.parsetools.RecordParser;

public class ClientFramesHandler implements Handler<Buffer> {

	private static final Logger logger = LoggerFactory.getLogger(ClientFramesHandler.class);
	private TokensHandler tokensHandler;
	private ReplicationSession framesHandler;
	private final NetSocket client;
	private final RecordParser lineParser;
	private final Vertx vertx;

	public ClientFramesHandler(Vertx vx, NetSocket client, ReplicationSession replicationSession) {
		this.vertx = vx;
		this.client = client;
		this.framesHandler = replicationSession;
		FileSystem fs = vertx.fileSystem();
		this.tokensHandler = new TokensHandler();
		this.lineParser = RecordParser.newDelimited("\r\n", buffer -> {
			boolean binary = !tokensHandler.delimited();
			if (logger.isDebugEnabled()) {
				if (binary) {
					logger.debug("C: <{}bytes>", buffer.length());
				} else {
					logger.debug("C: {}", buffer);
				}
			}
			Token token = Token.of(buffer, binary, fs);
			tokensHandler.feed(token);
		});
		tokensHandler.parser(lineParser);
	}

	@Override
	public void handle(Buffer event) {
		client.pause();
		lineParser.handle(event);

		ReplicationFrame frame = null;
		CompletableFuture<Void> root = CompletableFuture.completedFuture(null);
		while ((frame = tokensHandler.next()) != null) {
			final ReplicationFrame currentFrame = frame;
			root = root.thenCompose(nothing -> {
				return currentFrame.asyncComponent();
			}).thenCompose(frameWithWritesCompleted -> {
				return framesHandler.processFrame(frameWithWritesCompleted);
			});
		}
		root.whenComplete((v, ex) -> {
			logger.debug("Resume read.");
			client.resume();
			if (ex != null) {
				logger.error("Frame processing ended with error", ex);
				// close the socket ?
			}
		});

	}

}
