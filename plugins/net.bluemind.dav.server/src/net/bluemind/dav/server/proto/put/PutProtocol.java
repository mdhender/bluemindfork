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
package net.bluemind.dav.server.proto.put;

import java.net.URLDecoder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.HttpServerResponse;

import net.bluemind.core.container.model.ContainerDescriptor;
import net.bluemind.dav.server.proto.DavHeaders;
import net.bluemind.dav.server.proto.IDavProtocol;
import net.bluemind.dav.server.store.DavResource;
import net.bluemind.dav.server.store.DavStore;
import net.bluemind.dav.server.store.LoggedCore;
import net.bluemind.vertx.common.Body;

public class PutProtocol implements IDavProtocol<PutQuery, PutResponse> {

	private static final Logger logger = LoggerFactory.getLogger(PutProtocol.class);

	@Override
	public void parse(final HttpServerRequest r, final DavResource davRes, final Handler<PutQuery> handler) {
		Body.handle(r, new Handler<Buffer>() {

			@Override
			public void handle(Buffer b) {
				for (String hn : r.headers().names()) {
					logger.info("{}: {}", hn, r.headers().get(hn));
				}

				String p = r.path();
				logger.info("[{}] parse {}\n{}", b.length(), p, b.toString());
				PutQuery pq = new PutQuery(davRes);
				DavHeaders.parse(pq, r.headers());
				pq.setCreate(r.headers().contains("If-None-Match"));
				int lastDot = p.lastIndexOf('.');
				int lastSlash = p.lastIndexOf('/', lastDot);
				if (lastDot > 0 && lastSlash > 0) {
					try {
						String col = p.substring(0, lastSlash + 1);
						String extId = URLDecoder.decode(p.substring(lastSlash + 1, lastDot), "UTF-8");
						pq.setCollection(col);
						pq.setExtId(extId);
					} catch (Exception e) {
						logger.error(e.getMessage(), e);
					}
				}

				String ct = r.headers().get("Content-Type");
				if (ct != null && ct.startsWith("text/calendar")) {
					pq.setCalendar(b.toString());
				} else {
					throw new RuntimeException("Unsupported content type: " + ct);
				}

				handler.handle(pq);
			}
		});
	}

	@Override
	public void execute(LoggedCore lc, PutQuery query, Handler<PutResponse> handler) {
		logger.info("execute");

		PutResponse pr = new PutResponse();
		pr.setStatus(501);

		String col = query.getCollection();
		DavStore ds = new DavStore(lc);
		DavResource dr = ds.from(col);
		ContainerDescriptor cal = lc.vStuffContainer(dr);
		try {
			CreateEntity.getByType(cal.type).create(lc, query, pr, cal);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			pr.setStatus(500);
		}

		handler.handle(pr);
	}

	@Override
	public void write(PutResponse response, HttpServerResponse sr) {
		if (response.getEtag() != null) {
			sr.headers().set("Etag", response.getEtag());
		}
		sr.setStatusCode(response.getStatus()).end();
	}

}
