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
package net.bluemind.dav.server.proto.options;

import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import net.bluemind.dav.server.proto.DavHeaders;
import net.bluemind.dav.server.proto.IDavProtocol;
import net.bluemind.dav.server.store.DavResource;
import net.bluemind.dav.server.store.LoggedCore;

public class OptionsProtocol implements IDavProtocol<OptionsQuery, OptionsResponse> {

	@Override
	public void parse(final HttpServerRequest r, final DavResource davRes, final Handler<OptionsQuery> handler) {
		r.endHandler(new Handler<Void>() {

			@Override
			public void handle(Void event) {
				OptionsQuery oq = new OptionsQuery(davRes);
				handler.handle(oq);
			}
		});
	}

	@Override
	public void execute(LoggedCore lc, OptionsQuery query, Handler<OptionsResponse> handler) {
		OptionsResponse or = new OptionsResponse();
		or.setDavCapabilities(DavHeaders.DAV_CAPS);
		or.setAllowedMethods(
				"ACL, COPY, DELETE, GET, HEAD, LOCK, MKCOL, OPTIONS, PROPFIND, PROPPATCH, PUT, REPORT, UNLOCK");
		handler.handle(or);
	}

	@Override
	public void write(OptionsResponse response, HttpServerResponse sr) {
		sr.headers().add("DAV", response.getDavCapabilities());
		sr.headers().add("Allow", response.getAllowedMethods());
		sr.setStatusCode(200).end();
	}

}
