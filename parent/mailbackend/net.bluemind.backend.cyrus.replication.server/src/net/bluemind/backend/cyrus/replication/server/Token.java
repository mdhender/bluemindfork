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

import java.io.File;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.Files;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.FileSystem;
import net.bluemind.backend.cyrus.replication.server.state.ReplicationException;
import net.bluemind.backend.cyrus.replication.server.utils.LiteralSize;
import net.bluemind.backend.cyrus.replication.server.utils.Patterns;

public abstract class Token {
	public static final String ROOT = rootPath();
	private static final String FN_BASE = "t";

	private static final String rootPath() {
		File shm = new File("/dev/shm");
		if (shm.exists() && shm.isDirectory()) {
			return "/dev/shm/bm-cyrus-replication/";
		} else {
			return "/var/spool/bm-cyrus-replication/";
		}
	}

	private static final AtomicLong counter = new AtomicLong();
	private static final Logger logger = LoggerFactory.getLogger(Token.class);

	static {
		new File(ROOT).mkdirs();
	}

	public static class LiteralFollowUp {
		private final int size;

		public LiteralFollowUp(int s) {
			this.size = s;
		}

		public int size() {
			return size;
		}
	}

	protected LiteralFollowUp followup;
	protected int length;
	private final CompletableFuture<Void> asyncPart;

	private static class TextToken extends Token {

		private StringBuilder value;

		public TextToken(String value, LiteralFollowUp followup, CompletableFuture<Void> async) {
			super(followup, value.length(), async);
			this.value = new StringBuilder(value);
		}

		public boolean isBinary() {
			return false;
		}

		@Override
		public String value() {
			return value.toString();
		}

		public void merge(Token t) {
			if (t.isBinary()) {
				throw new UnsupportedOperationException();
			} else {
				value.append(t.value());
				followup = t.followup;
				length = value.length();
			}
		}

		public String toString() {
			return String.format("[TEXT '%s']", value);
		}

	}

	private Token(LiteralFollowUp followup, int length, CompletableFuture<Void> async) {
		this.followup = followup;
		this.length = length;
		this.asyncPart = async;
	}

	private static File tokenFile(String tokRef) {
		return new File(ROOT, tokRef);
	}

	public static String atomOrValue(String value) {
		String v = value;
		Matcher m = Patterns.ATOM_TOKEN.matcher(v);
		if (m.find()) {
			File tok = Token.tokenFile(m.group(1));
			try {
				byte[] data = Files.toByteArray(tok);
				v = new String(data);
				tok.delete();
			} catch (IOException e) {
				throw ReplicationException.serverError(e);
			}
		}
		return v;
	}

	public static Token of(Buffer next, boolean binaryToken, FileSystem fs) {
		if (binaryToken) {
			long tokenId = counter.incrementAndGet();
			String fileName = FN_BASE + tokenId + ".bin";
			String path = ROOT + fileName;
			CompletableFuture<Void> writeCompletion = new CompletableFuture<>();
			int len = next.length();
			fs.writeFile(path, next, new Handler<AsyncResult<Void>>() {

				@Override
				public void handle(AsyncResult<Void> result) {
					if (result.succeeded()) {
						logger.debug("Token written ({} bytes)", len);
						writeCompletion.complete(null);
					} else {
						writeCompletion.completeExceptionally(result.cause());
					}
				}
			});
			return new TextToken("{" + fileName + "}", null, writeCompletion);
		} else {
			String asString = next.toString();
			int fastSize = LiteralSize.of(next.getByteBuf());
			if (fastSize > 0) {
				LiteralFollowUp follow = new LiteralFollowUp(fastSize);
				return new TextToken(asString, follow, CompletableFuture.completedFuture(null));
			} else {
				return new TextToken(asString, null, CompletableFuture.completedFuture(null));
			}
		}
	}

	public abstract String value();

	public abstract boolean isBinary();

	public abstract void merge(Token t);

	public LiteralFollowUp followup() {
		return followup;
	}

	public CompletableFuture<Void> asyncComponent() {
		return asyncPart;
	}

	public int length() {
		return length;
	}

}
