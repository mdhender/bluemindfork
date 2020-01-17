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
package net.bluemind.dav.server.proto.proppatch;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.namespace.QName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import net.bluemind.dav.server.proto.IDavProtocol;
import net.bluemind.dav.server.proto.QN;
import net.bluemind.dav.server.store.DavResource;
import net.bluemind.dav.server.store.DavStore;
import net.bluemind.dav.server.store.LoggedCore;
import net.bluemind.dav.server.store.PropSetResult;
import net.bluemind.dav.server.xml.DOMUtils;
import net.bluemind.dav.server.xml.MultiStatusBuilder;
import net.bluemind.vertx.common.Body;

public class PropPatchProtocol implements IDavProtocol<PropPatchQuery, PropPatchResponse> {

	private static final Logger logger = LoggerFactory.getLogger(PropPatchProtocol.class);

	@Override
	public void parse(final HttpServerRequest r, final DavResource davRes, final Handler<PropPatchQuery> handler) {
		Body.handle(r, new Handler<Buffer>() {

			@Override
			public void handle(Buffer body) {
				PropPatchQueryParser qp = new PropPatchQueryParser();
				PropPatchQuery pfq = qp.parse(davRes, r.headers(), body);
				handler.handle(pfq);
			}
		});
	}

	@Override
	public void execute(LoggedCore lc, PropPatchQuery query, Handler<PropPatchResponse> handler) {
		DavStore ds = new DavStore(lc);
		PropPatchResponse ppr = new PropPatchResponse(query.getResource());
		Map<QName, Element> tou = query.getToUpdate();
		Map<QName, PropSetResult> results = new HashMap<>();
		for (Entry<QName, Element> e : tou.entrySet()) {
			QName qn = e.getKey();
			PropSetResult status = ds.setValue(qn, e.getValue(), query.getResource());
			results.put(qn, status);
		}
		ppr.setResults(results);
		handler.handle(ppr);
	}

	@Override
	public void write(PropPatchResponse response, HttpServerResponse sr) {
		logger.warn("proppatch just says yes");
		MultiStatusBuilder msb = new MultiStatusBuilder();
		Element prop = msb.newResponse(response.getResource().getPath(), 200);
		for (QName qn : response.getResults().keySet()) {
			DOMUtils.createElement(prop, QN.elem(qn));
		}
		msb.sendAs(sr, true);
	}
}
