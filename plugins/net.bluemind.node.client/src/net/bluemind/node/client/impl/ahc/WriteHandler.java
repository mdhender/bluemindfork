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
package net.bluemind.node.client.impl.ahc;

import java.io.IOException;
import java.io.InputStream;

import org.asynchttpclient.BoundRequestBuilder;
import org.asynchttpclient.request.body.Body;
import org.asynchttpclient.request.body.generator.BodyGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpHeaders;
import net.bluemind.common.io.FileBackedOutputStream;
import net.bluemind.core.api.fault.ServerFault;

public class WriteHandler extends DefaultAsyncHandler<Void> {

	private static final Logger logger = LoggerFactory.getLogger(WriteHandler.class);

	private final InputStream source;
	private final String path;

	public WriteHandler(String path, InputStream source) {
		super("W '" + path + "'", false);
		this.path = path;
		this.source = source;
	}

	@Override
	protected Void getResult(int status, HttpHeaders headers, FileBackedOutputStream body) {
		if (status != 200) {
			logger.warn("PUT {} error: {}", path, status);
			throw new ServerFault();
		}
		return null;
	}

	private static class NodeBodyGenerator implements BodyGenerator {

		private final InputStream in;
		private final byte[] defaultChunk;

		public NodeBodyGenerator(InputStream in) {
			this.in = in;
			this.defaultChunk = new byte[8192];
		}

		@Override
		public Body createBody() {
			return new Body() {

				@Override
				public void close() throws IOException {
					in.close();
				}

				@Override
				public long getContentLength() {
					return -1;
				}

				@Override
				public BodyState transferTo(ByteBuf target) throws IOException {
					byte[] tgt = target.writableBytes() >= 8192 ? defaultChunk : new byte[target.writableBytes()];
					int read = in.read(tgt);
					switch (read) {
					case -1:
						return BodyState.STOP;
					case 0:
						return BodyState.CONTINUE;
					default:
						target.writeBytes(tgt, 0, read);
						return BodyState.CONTINUE;
					}
				}

			};
		}

	}

	@Override
	public BoundRequestBuilder prepare(BoundRequestBuilder rb) {
		rb.setBody(new NodeBodyGenerator(source));
		return rb;
	}

}
