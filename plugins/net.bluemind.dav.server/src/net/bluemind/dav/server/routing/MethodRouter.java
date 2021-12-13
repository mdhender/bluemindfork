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
package net.bluemind.dav.server.routing;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import net.bluemind.core.rest.http.ClientSideServiceProvider;
import net.bluemind.dav.server.proto.DavMethod;
import net.bluemind.dav.server.proto.IDavProtocol;
import net.bluemind.dav.server.proto.IProtocolFactory;
import net.bluemind.dav.server.proto.MissingProtocol;
import net.bluemind.dav.server.proto.ProtocolFactory;
import net.bluemind.dav.server.proto.UnknownQuery;
import net.bluemind.dav.server.proto.UnknownResponse;
import net.bluemind.dav.server.store.DavResource;
import net.bluemind.dav.server.store.DavStore;
import net.bluemind.dav.server.store.LoggedCore;
import net.bluemind.dav.server.store.ResType;
import net.bluemind.network.topology.Topology;
import net.bluemind.vertx.common.http.BasicAuthHandler.AuthenticatedRequest;

public final class MethodRouter implements Handler<AuthenticatedRequest> {

	private final DavMethod<UnknownQuery, UnknownResponse> notimplemented = new DavMethod<>(new MissingProtocol());
	private static final Logger logger = LoggerFactory.getLogger(MethodRouter.class);

	private final Map<ResType, DavMethod<?, ?>> putBinding;
	private final Map<ResType, DavMethod<?, ?>> getBinding;
	private final Map<ResType, DavMethod<?, ?>> headBinding;
	private final Map<ResType, DavMethod<?, ?>> postBinding;
	private final Map<ResType, DavMethod<?, ?>> deleteBinding;
	private final Map<ResType, DavMethod<?, ?>> propfindBinding;
	private final Map<ResType, DavMethod<?, ?>> proppatchBinding;
	private final Map<ResType, DavMethod<?, ?>> reportBinding;
	private final Map<ResType, DavMethod<?, ?>> optionsBinding;
	private final Map<ResType, DavMethod<?, ?>> mkcalendarBinding;
	private final Map<ResType, DavMethod<?, ?>> mkcolBinding;
	private final Map<ResType, DavMethod<?, ?>> moveBinding;

	public MethodRouter() {
		postBinding = new HashMap<>();
		propfindBinding = new HashMap<>();
		deleteBinding = new HashMap<>();
		getBinding = new HashMap<>();
		headBinding = new HashMap<>();
		proppatchBinding = new HashMap<>();
		putBinding = new HashMap<>();
		reportBinding = new HashMap<>();
		optionsBinding = new HashMap<>();
		mkcalendarBinding = new HashMap<>();
		mkcolBinding = new HashMap<>();
		moveBinding = new HashMap<>();

	}

	private <Q, R> DavMethod<Q, R> method(IDavProtocol<Q, R> proto) {
		IProtocolFactory<Q, R> pf = new ProtocolFactory<Q, R>(proto);
		return new DavMethod<>(pf.getProtocol(), pf.getExecutorAddress());
	}

	public <Q, R> MethodRouter postHandler(ResType rt, IDavProtocol<Q, R> proto) {
		postBinding.put(rt, method(proto));
		return this;
	}

	public <Q, R> MethodRouter propfindHandler(ResType rt, IDavProtocol<Q, R> proto) {
		propfindBinding.put(rt, method(proto));
		return this;
	}

	public <Q, R> MethodRouter deleteHandler(ResType rt, IDavProtocol<Q, R> proto) {
		deleteBinding.put(rt, method(proto));
		return this;
	}

	public <Q, R> MethodRouter getHandler(ResType rt, IDavProtocol<Q, R> proto) {
		getBinding.put(rt, method(proto));
		return this;
	}

	public <Q, R> MethodRouter headHandler(ResType rt, IDavProtocol<Q, R> proto) {
		headBinding.put(rt, method(proto));
		return this;
	}

	public <Q, R> MethodRouter proppatchHandler(ResType rt, IDavProtocol<Q, R> proto) {
		proppatchBinding.put(rt, method(proto));
		return this;
	}

	public <Q, R> MethodRouter putHandler(ResType rt, IDavProtocol<Q, R> proto) {
		putBinding.put(rt, method(proto));
		return this;
	}

	public <Q, R> MethodRouter reportHandler(ResType rt, IDavProtocol<Q, R> proto) {
		reportBinding.put(rt, method(proto));
		return this;
	}

	public <Q, R> MethodRouter mkcalendarHandler(ResType rt, IDavProtocol<Q, R> proto) {
		mkcalendarBinding.put(rt, method(proto));
		return this;
	}

	public <Q, R> MethodRouter mkcolHandler(ResType rt, IDavProtocol<Q, R> proto) {
		mkcolBinding.put(rt, method(proto));
		return this;
	}

	public <Q, R> MethodRouter moveHandler(ResType rt, IDavProtocol<Q, R> proto) {
		moveBinding.put(rt, method(proto));
		return this;
	}

	public <Q, R> MethodRouter optionsHandler(ResType rt, IDavProtocol<Q, R> proto) {
		optionsBinding.put(rt, method(proto));
		return this;
	}

	public void handle(AuthenticatedRequest r) {
		String coreBase = "http://" + Topology.get().core().value.address() + ":8090";
		ClientSideServiceProvider sp = ClientSideServiceProvider.getProvider(coreBase, r.sid);
		handle(new LoggedCore(sp), r.req);
	}

	public void handle(LoggedCore lc, HttpServerRequest r) {
		String method = r.method().name();
		DavStore ds = new DavStore(lc);
		DavResource res = ds.from(r.path());
		if (!"CREATE".equals(method) && !"MKCALENDAR".equals(method) && !ds.existingResource(res)) {
			String msg = String.format("Resource %s does not match any known dav resource.", res.getPath());
			logger.warn(msg);
			r.response().setStatusCode(404).setStatusMessage(msg).end();
			return;
		}

		ResType rt = res.getResType();
		DavMethod<?, ?> dmh = null;
		switch (method) {
		case "PROPFIND":
			dmh = propfindBinding.get(rt);
			break;
		case "PROPPATCH":
			dmh = proppatchBinding.get(rt);
			break;
		case "POST":
			dmh = postBinding.get(rt);
			break;
		case "OPTIONS":
			dmh = optionsBinding.get(rt);
			break;
		case "REPORT":
			dmh = reportBinding.get(rt);
			break;
		case "GET":
			dmh = getBinding.get(rt);
			break;
		case "HEAD":
			dmh = headBinding.get(rt);
			break;
		case "MKCOL":
			dmh = mkcolBinding.get(rt);
			break;
		case "PUT":
			dmh = putBinding.get(rt);
			break;
		case "DELETE":
			dmh = deleteBinding.get(rt);
			break;
		case "MKCALENDAR":
			dmh = mkcalendarBinding.get(rt);
			break;
		case "MOVE":
			dmh = moveBinding.get(rt);
			break;
		default:
			logger.error("Route missing for HTTP method: {}", method);
		}
		if (dmh != null) {
			dmh.davMethod(lc, res, r);
		} else {
			notimplemented.davMethod(lc, res, r);
		}
	}

}
