/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2022
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
package net.bluemind.dav.server.proto.post;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import net.bluemind.dav.server.proto.IDavProtocol;
import net.bluemind.dav.server.proto.post.VEventStuffPostProtocol.VEventStuffContext;
import net.bluemind.dav.server.proto.sharing.SharingProtocol;
import net.bluemind.dav.server.proto.sharing.SharingQuery;
import net.bluemind.dav.server.proto.sharing.SharingQueryParser;
import net.bluemind.dav.server.store.DavResource;
import net.bluemind.dav.server.store.LoggedCore;
import net.bluemind.dav.server.xml.MultiStatusBuilder;
import net.bluemind.vertx.common.Body;

public class VEventStuffPostProtocol implements IDavProtocol<VEventStuffContext, Object> {

	private static final Logger logger = LoggerFactory.getLogger(VEventStuffPostProtocol.class);

	@Override
	public void parse(HttpServerRequest r, DavResource davRes, Handler<VEventStuffContext> handler) {
		Body.handle(r, new Handler<Buffer>() {

			@Override
			public void handle(Buffer event) {
				logReq(logger, r, event);
				String bodyString = new String(event.getBytes());
				ProtocolImplementation implementation = null;
				if (bodyString.toLowerCase().contains("multiput")) {
					implementation = ProtocolImplementation.MULTIPUT;
				} else {
					implementation = ProtocolImplementation.SHARING;
				}
				logger.info("VEventStuffPostProtocol implementation is {}", implementation);
				handler.handle(new VEventStuffContext(event, davRes, implementation, r.headers()));
			}
		});
	}

	@Override
	public void execute(LoggedCore lc, VEventStuffContext ctx, Handler<Object> handler) {
		switch (ctx.impl) {
		case SHARING:
			SharingQuery sQuery = new SharingQueryParser().parse(ctx.ressource, ctx.headers, ctx.data);
			SharingProtocol.execute(lc, sQuery, ctx);
			handler.handle(new Object());
		case MULTIPUT:
			CalMultiputQuery mQuery = new CalMultiputQueryParser().parse(ctx.ressource, ctx.headers, ctx.data);
			CalMultiputResponse resp = CalMultiputProtocol.execute(lc, mQuery, ctx);
			handler.handle(resp);
			break;
		}
	}

	@Override
	public void write(Object resp, HttpServerResponse sr) {
		if (resp instanceof CalMultiputResponse) {
			CalMultiputResponse response = (CalMultiputResponse) resp;
			MultiStatusBuilder msb = CalMultiputProtocol.getResponse(response);
			msb.sendAs(sr, true);
		} else
			sr.setStatusCode(200).end();
	}

	private enum ProtocolImplementation {
		SHARING, MULTIPUT
	}

	public static class VEventStuffContext {
		public final Buffer data;
		public final DavResource ressource;
		public final ProtocolImplementation impl;
		public final MultiMap headers;

		public VEventStuffContext(Buffer data, DavResource ressource, ProtocolImplementation impl, MultiMap headers) {
			this.data = data;
			this.ressource = ressource;
			this.impl = impl;
			this.headers = headers;
		}
	}

}
