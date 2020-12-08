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
package net.bluemind.node.server.handlers;

import java.io.File;
import java.io.IOException;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.atomic.LongAdder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.buffer.ByteBuf;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;

public class WriteFile implements Handler<HttpServerRequest> {

	@SuppressWarnings("serial")
	private static class WriteException extends RuntimeException {
		public WriteException(IOException ioe) {
			super(ioe);
		}

	}

	private static final Logger logger = LoggerFactory.getLogger(WriteFile.class);

	@Override
	public void handle(final HttpServerRequest req) {
		final String path = UrlPath.dec(req.params().get("param0"));
		logger.debug("PUT {}...", path);
		writeFile(req, path);
	}

	private void writeFile(final HttpServerRequest req, final String path) {
		File asFile = new File(path);
		asFile.getParentFile().mkdirs();
		try {
			SeekableByteChannel chan = Files.newByteChannel(asFile.toPath(), StandardOpenOption.CREATE,
					StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
			req.exceptionHandler(t -> {
				safeClose(chan);
				do500(t, req);
			});
			LongAdder len = new LongAdder();
			req.handler(buf -> {
				try {
					ByteBuf netty = buf.getByteBuf();
					len.add(netty.readableBytes());
					chan.write(netty.nioBuffer());
				} catch (IOException e) {
					throw new WriteException(e);
				}
			});
			req.endHandler(v -> {
				safeClose(chan);
				logger.info("PUT {} completed, wrote {} byte(s)", path, len.sum());
				req.response().end();
			});
		} catch (IOException e) {
			do500(e, req);
		}
	}

	private void safeClose(SeekableByteChannel chan) {
		try {
			chan.close();
		} catch (IOException e) {
			throw new WriteException(e);
		}
	}

	private void do500(Throwable t, HttpServerRequest req) {
		logger.error(t.getMessage(), t);
		req.response().setStatusCode(500).end();
	}
}
