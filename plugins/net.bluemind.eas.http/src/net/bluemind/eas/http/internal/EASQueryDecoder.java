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
package net.bluemind.eas.http.internal;

import org.vertx.java.core.Handler;

import net.bluemind.eas.http.AuthenticatedEASQuery;
import net.bluemind.eas.http.query.EASQueryBuilder;
import net.bluemind.vertx.common.http.BasicAuthHandler.AuthenticatedRequest;
import net.bluemind.vertx.common.request.Requests;

public final class EASQueryDecoder implements Handler<AuthenticatedRequest> {

	private final Handler<AuthenticatedEASQuery> next;

	public EASQueryDecoder(Handler<AuthenticatedEASQuery> next) {
		this.next = next;
	}

	@Override
	public void handle(AuthenticatedRequest event) {
		Requests.tag(event.req, "user", event.login);
		AuthenticatedEASQuery query = decode(event);
		Requests.tag(event.req, "cmd", query.command());
		Requests.tag(event.req, "device", query.deviceIdentifier());
		Requests.tag(event.req, "type", query.deviceType());
		Requests.tag(event.req, "pv", Double.toString(query.protocolVersion()));
		next.handle(query);
	}

	private AuthenticatedEASQuery decode(AuthenticatedRequest event) {
		return EASQueryBuilder.from(event);
	}

}
