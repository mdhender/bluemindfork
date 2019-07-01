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
package net.bluemind.eas.command.itemoperations;

import java.io.IOException;
import java.nio.ByteOrder;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpServerResponse;

import com.google.common.io.ByteSource;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.bluemind.eas.dto.base.AirSyncBaseResponse;
import net.bluemind.eas.dto.base.BodyOptions;
import net.bluemind.eas.dto.base.Callback;
import net.bluemind.eas.dto.base.DisposableByteSource;
import net.bluemind.eas.dto.base.LazyLoaded;
import net.bluemind.eas.http.EasHeaders;
import net.bluemind.eas.impl.Responder;
import net.bluemind.eas.impl.vertx.compat.VertxResponder;

public class MultipartBuilder {

	private final class Part {
		public final DisposableByteSource fbos;
		public final int size;

		public Part(DisposableByteSource fbos, int size) {
			this.fbos = fbos;
			this.size = size;
		}
	}

	private final class LoadParts implements Callback<AirSyncBaseResponse> {
		private final Iterator<LazyLoaded<BodyOptions, AirSyncBaseResponse>> it;
		private final Handler<Void> completion;
		private final List<Part> parts;

		private LoadParts(Iterator<LazyLoaded<BodyOptions, AirSyncBaseResponse>> it, List<Part> parts,
				Handler<Void> completion) {
			this.it = it;
			this.completion = completion;
			this.parts = parts;
		}

		@Override
		public void onResult(AirSyncBaseResponse data) {
			try {
				parts.add(new Part(data.body.data, (int) data.body.data.size()));
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
			}
			next();
		}

		public void next() {
			if (it.hasNext()) {
				LazyLoaded<BodyOptions, AirSyncBaseResponse> lazy = it.next();
				lazy.load(this);
			} else {
				completion.handle(null);
			}
		}
	}

	private final class WriteParts implements Handler<Void> {
		private final Iterator<Part> it;
		private final Handler<Void> completion;
		private final HttpServerResponse output;

		private WriteParts(Iterator<Part> it, HttpServerResponse output, Handler<Void> completion) {
			this.it = it;
			this.completion = completion;
			this.output = output;
		}

		public void write(Part p) {
			logger.info("Writing part, {} byte(s)", p.size);

			try {
				ByteSource bs = p.fbos.source();
				output.write(new Buffer(bs.read()));
				p.fbos.dispose();
			} catch (IOException e) {
				logger.error(e.getMessage(), e);
			}
			if (output.writeQueueFull()) {
				output.drainHandler(this);
			} else {
				next();
			}
		}

		public void next() {
			if (it.hasNext()) {
				Part p = it.next();
				write(p);
			} else {
				output.end();
				completion.handle(null);
			}
		}

		@Override
		public void handle(Void event) {
			output.drainHandler(null);
			next();
		}
	}

	private static final Logger logger = LoggerFactory.getLogger(MultipartBuilder.class);
	private List<LazyLoaded<BodyOptions, AirSyncBaseResponse>> lazyParts;
	private byte[] asWbxml;

	public MultipartBuilder() {
		this.lazyParts = new LinkedList<>();
	}

	MultipartBuilder wbxml(byte[] document) {
		this.asWbxml = document;
		return this;
	}

	MultipartBuilder asyncPart(LazyLoaded<BodyOptions, AirSyncBaseResponse> lazy) {
		logger.info("Adding asyncPart {}", lazy);
		lazyParts.add(lazy);
		return this;
	}

	public void build(Responder responder, final Handler<Void> completion) {
		final HttpServerResponse resp = ((VertxResponder) responder).response();
		resp.setChunked(true);
		resp.putHeader(EasHeaders.Server.MS_SERVER, "14.3");
		resp.putHeader("Server", "Microsoft-IIS/7.5");
		resp.putHeader("Cache-Control", "private");
		resp.putHeader("Content-Type", "application/vnd.ms-sync.multipart");

		final List<Part> parts = new LinkedList<>();
		byte[] wbxmlBinary = asWbxml;
		parts.add(new Part(DisposableByteSource.wrap(wbxmlBinary), wbxmlBinary.length));
		LoadParts theNextCallback = new LoadParts(lazyParts.iterator(), parts, new Handler<Void>() {

			@Override
			public void handle(Void event) {

				int size = parts.size();
				logger.info("Multipart output with {} part(s) {}", size, Integer.toHexString(size));
				ByteBuf byteBuf = Unpooled.buffer();
				byteBuf = byteBuf.order(ByteOrder.LITTLE_ENDIAN);
				byteBuf.writeInt(size);
				int offset = 4 + 8 * parts.size();
				for (Part p : parts) {
					logger.info("partMetaData offset: {}, length: {}", offset, p.size);
					byteBuf.writeInt(offset);
					byteBuf.writeInt(p.size);
					offset += p.size;
				}
				resp.write(new Buffer(byteBuf));
				WriteParts wp = new WriteParts(parts.iterator(), resp, completion);
				wp.next();
			}

		});
		theNextCallback.next();

	}

}
