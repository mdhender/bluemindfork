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
package net.bluemind.dav.server.proto.propfind;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.xml.namespace.QName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import net.bluemind.dav.server.proto.DavHeaders;
import net.bluemind.dav.server.proto.IDavProtocol;
import net.bluemind.dav.server.proto.IPropertyValue;
import net.bluemind.dav.server.proto.QN;
import net.bluemind.dav.server.store.DavResource;
import net.bluemind.dav.server.store.DavStore;
import net.bluemind.dav.server.store.LoggedCore;
import net.bluemind.dav.server.store.SyncTokens;
import net.bluemind.dav.server.xml.DOMUtils;
import net.bluemind.dav.server.xml.MultiStatusBuilder;
import net.bluemind.vertx.common.Body;

public class PropFindProtocol implements IDavProtocol<PropFindQuery, PropFindResponse> {

	private static final Logger logger = LoggerFactory.getLogger(PropFindProtocol.class);

	@Override
	public void parse(final HttpServerRequest r, final DavResource davRes, final Handler<PropFindQuery> handler) {
		Body.handle(r, new Handler<Buffer>() {
			@Override
			public void handle(Buffer body) {
				PropFindQueryParser qp = new PropFindQueryParser();
				PropFindQuery pfq = qp.parse(davRes, r.headers(), body);
				handler.handle(pfq);
			}
		});
	}

	@Override
	public void execute(LoggedCore lc, PropFindQuery query, Handler<PropFindResponse> handler) {
		DavStore ds = new DavStore(lc);
		Set<QName> queried = query.getQueried();
		DavResource dr = ds.from(query.getPath());
		List<DavResource> paths = ds.addChildren(dr, query.getDepth());
		PropFindResponse last = null;
		PropFindResponse first = null;
		for (DavResource res : paths) {
			if (res == null) {
				logger.warn("[{}] Null DAV resource in child list", lc.getUser().value.login);
				continue;
			}
			PropFindResponse pfr = getOneLevel(ds, res, queried);
			if (last != null) {
				last.setNext(pfr);
			} else {
				first = pfr;
			}
			last = pfr;
		}
		handler.handle(first);
	}

	private PropFindResponse getOneLevel(DavStore ps, DavResource dr, Set<QName> queried) {
		PropFindResponse pfr = new PropFindResponse();
		pfr.setHref(dr.getPath());
		pfr.setStatus(200);
		List<IPropertyValue> values = new ArrayList<>(queried.size());
		for (QName prop : queried) {
			IPropertyValue value = ps.getValue(prop, dr);
			if (value != null) {
				values.add(value);
			}
		}
		pfr.setEtag(dr.getEtag());
		pfr.setPropValues(values);
		return pfr;
	}

	@Override
	public void write(PropFindResponse response, HttpServerResponse sr) {
		try {
			MultiStatusBuilder msb = new MultiStatusBuilder();
			PropFindResponse cur = response;
			do {
				appendOne(cur, msb);
				cur = cur.getNext();
			} while (cur != null);

			sr.headers().set("DAV", DavHeaders.DAV_CAPS);
			if (response.getEtag() != null) {
				String etag = response.getEtag();
				logger.info("Added ETag header with lastMod at {}", SyncTokens.getEtagDate(etag));
				sr.headers().set("ETag", etag);
			}
			msb.sendAs(sr);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			sr.setStatusCode(500).setStatusMessage(e.getMessage() != null ? e.getMessage() : "null").end();
		}
	}

	private void appendOne(PropFindResponse response, MultiStatusBuilder msb) {
		Element aprop = msb.newResponse(response.getHref(), response.getStatus());
		for (IPropertyValue pv : response.getPropValues()) {
			Element pve = DOMUtils.createElement(aprop, QN.elem(pv.getName()));
			pv.appendValue(pve);
		}
	}
}
