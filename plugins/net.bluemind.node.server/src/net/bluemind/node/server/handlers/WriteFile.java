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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.AclFileAttributeView;
import java.nio.file.attribute.FileOwnerAttributeView;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.UserDefinedFileAttributeView;
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
		final String path = req.params().get("param0");
		logger.debug("PUT {}...", path);
		writeFile(req, path);
	}

	private void writeFile(final HttpServerRequest req, final String path) {
		// LC: write to temporary file, then move atomically, so the file is either
		// fully written, or not present
		Path originalPath = Paths.get(path);
		try {
			Files.createDirectories(originalPath.getParent());
			Path tempPath = Files.createTempFile(originalPath.getParent(), ".nc_", "");
			SeekableByteChannel chan = Files.newByteChannel(tempPath, StandardOpenOption.CREATE, // NOSONAR: async
																									// handler
					StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
			copyAllAttributes(originalPath, tempPath);
			req.exceptionHandler(t -> {
				try {
					Files.deleteIfExists(tempPath);
				} catch (IOException ie) {
					// Ignore
				}
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
				try {
					Files.move(tempPath, originalPath, StandardCopyOption.ATOMIC_MOVE);
					logger.info("PUT {} completed, wrote {} byte(s)", originalPath, len.sum());
					req.response().end();
				} catch (IOException e) {
					logger.error("PUT {} rename to {} failed", tempPath, originalPath);
					do500(new WriteException(e), req);
				}
			});
		} catch (IOException e) {
			do500(e, req);
		}
	}

	private void copyAllAttributes(Path src, Path dst) throws IOException {
		if (!Files.exists(src)) {
			return;
		}
		AclFileAttributeView acl = Files.getFileAttributeView(src, AclFileAttributeView.class);
		if (acl != null) {
			Files.getFileAttributeView(dst, AclFileAttributeView.class).setAcl(acl.getAcl());
		}
		FileOwnerAttributeView ownerAttrs = Files.getFileAttributeView(src, FileOwnerAttributeView.class);
		if (ownerAttrs != null) {
			FileOwnerAttributeView targetOwner = Files.getFileAttributeView(dst, FileOwnerAttributeView.class);
			targetOwner.setOwner(ownerAttrs.getOwner());
		}
		PosixFileAttributeView posixAttrs = Files.getFileAttributeView(src, PosixFileAttributeView.class);
		if (posixAttrs != null) {
			PosixFileAttributes sourcePosix = posixAttrs.readAttributes();
			PosixFileAttributeView targetPosix = Files.getFileAttributeView(dst, PosixFileAttributeView.class);
			targetPosix.setPermissions(sourcePosix.permissions());
			targetPosix.setGroup(sourcePosix.group());
		}
		UserDefinedFileAttributeView userAttrs = Files.getFileAttributeView(src, UserDefinedFileAttributeView.class);
		if (userAttrs != null) {
			UserDefinedFileAttributeView targetUser = Files.getFileAttributeView(dst,
					UserDefinedFileAttributeView.class);
			for (String key : userAttrs.list()) {
				ByteBuffer buffer = ByteBuffer.allocate(userAttrs.size(key));
				userAttrs.read(key, buffer);
				buffer.flip();
				targetUser.write(key, buffer);
			}
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
