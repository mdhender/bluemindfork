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
package net.bluemind.authentication.handler;

import java.security.InvalidParameterException;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Splitter;

import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import net.bluemind.authentication.api.IAuthentication;
import net.bluemind.authentication.api.ValidationKind;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.rest.http.vertx.NeedVertxExecutor;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.api.IDomains;
import net.bluemind.lib.vertx.BlockingCode;
import net.bluemind.network.topology.IServiceTopology;
import net.bluemind.network.topology.Topology;
import net.bluemind.network.topology.TopologyException;
import net.bluemind.pool.impl.BmConfIni;
import net.bluemind.user.api.IUser;
import net.bluemind.user.api.User;

public final class Nginx implements Handler<HttpServerRequest>, NeedVertxExecutor {

	private static final Logger logger = LoggerFactory.getLogger(Nginx.class);
	private Vertx vertx;
	private BlockingCode blocking;
	private static String defaultDomain;

	private static class QueryParameters {
		public final String clientIp;
		public final String backendPort;
		public final String protocol;
		public final String password;
		public final String user;
		public final String latd;
		public final long time;

		private QueryParameters(String clientIp, String protocol, String user, String latd, String password,
				String backendPort, long time) {
			this.clientIp = clientIp;
			this.protocol = protocol;
			this.user = user;
			this.latd = latd;
			this.password = password;
			this.backendPort = backendPort;
			this.time = time;
		}

		public static QueryParameters fromRequest(HttpServerRequest req, long time) {
			String clientIp = req.headers().get("Client-IP");
			String backendPort = req.headers().get("X-Auth-Port");
			String protocol = req.headers().get("Auth-Protocol");

			String user = req.headers().get("Auth-User");
			if (user == null || "".equals(user)) {
				throw new InvalidParameterException("null or empty login");
			}

			user = decode(user).toLowerCase();
			String latd = (!"admin0".equals(user) && defaultDomain != null && !user.contains("@"))
					? user + "@" + defaultDomain
					: user;

			String password = decode(req.headers().get("Auth-Pass"));

			return new QueryParameters(clientIp, protocol, user, latd, password, backendPort, time);
		}

	}

	private static class AuthResponse {
		ValidationKind validation;
		String backendSrv;
		String backendLatd;

		public static AuthResponse of(ValidationKind kind, String backendLatd, String backendSrv) {
			AuthResponse ar = new AuthResponse();
			ar.validation = kind;
			ar.backendLatd = backendLatd;
			ar.backendSrv = backendSrv;
			return ar;
		}
	}

	public Nginx() {
		loadDefaultDomain();
	}

	private void loadDefaultDomain() {
		BmConfIni conf = new BmConfIni();
		defaultDomain = conf.get("default-domain");
	}

	@Override
	public void handle(final HttpServerRequest req) {
		long time = System.currentTimeMillis();
		req.endHandler(v -> {
			HttpServerResponse resp = req.response();
			if (vertx == null) {
				resp.setStatusCode(500).setStatusMessage("missing vertx").end();
				return;
			}
			QueryParameters qp = QueryParameters.fromRequest(req, time);

			blocking.run(() -> computeResponse(qp)).whenComplete((ar, ex) -> {
				if (ex != null) {
					logger.error(ex.getMessage(), ex);
					fail(qp, resp);
				} else if (ar.validation == ValidationKind.NONE) {
					fail(qp, resp);
				} else {
					succeed(resp, qp, ar.backendSrv, ar.backendLatd);
				}
				resp.end();

			});

		});
	}

	private AuthResponse computeResponse(QueryParameters qp) {
		IAuthentication authApi = ServerSideServiceProvider.getProvider(SecurityContext.ANONYMOUS)
				.instance(IAuthentication.class);
		ValidationKind kind = authApi.validate(qp.latd, qp.password, "nginx-imap-password-check");
		if (kind != ValidationKind.NONE) {
			if (!qp.latd.contains("@")) {
				throw new InvalidParameterException("Invalid login@domain " + qp.latd);
			}

			Splitter splitter = Splitter.on('@').omitEmptyStrings().trimResults();
			Iterator<String> parts = splitter.split(qp.latd).iterator();
			parts.next();
			String domainPart = parts.next();
			ServerSideServiceProvider provider = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);

			ItemValue<Domain> domain = provider.instance(IDomains.class).findByNameOrAliases(domainPart);

			if (domain == null) {
				throw new InvalidParameterException("Fail to find domain " + domainPart);
			}

			IUser userApi = provider.instance(IUser.class, domain.uid);
			ItemValue<User> user = userApi.byEmail(qp.latd);
			if (user == null) {
				// User latd exists but email is given to something else (mailshare, group...)
				return AuthResponse.of(ValidationKind.NONE, null, null);
			}

			String backendLatd = user.value.login + "@" + domain.value.name;

			String backendSrv = null;
			IServiceTopology topology = Topology.get();
			if (topology.singleNode()) {
				backendSrv = topology.core().value.address();
			} else {
				backendSrv = topology.nodes().stream().filter(iv -> iv.uid.equals(user.value.dataLocation))
						.map(iv -> iv.value.address()).findFirst()
						.orElseThrow(() -> new TopologyException("uid " + user.value.dataLocation + " missing"));
			}

			long time = System.currentTimeMillis() - qp.time;
			logger.info("[{}][{}][{}] will use cyrus backend {} using login [{}], done in {}ms.", qp.clientIp,
					qp.protocol, qp.latd, backendSrv, backendLatd, time);
			return AuthResponse.of(kind, backendLatd, backendSrv);
		}

		return AuthResponse.of(kind, null, null);
	}

	/**
	 * @param latd
	 * @param resp
	 */
	private void fail(QueryParameters qp, HttpServerResponse resp) {
		logger.error("[{}] Denied auth from {}", qp == null ? null : qp.latd, qp == null ? null : qp.clientIp);
		resp.headers().add("Auth-Status", "Invalid login or password");
	}

	private void succeed(HttpServerResponse resp, QueryParameters qp, String backendSrv, String backendLatd) {
		MultiMap respHeaders = resp.headers();

		respHeaders.add("Auth-Status", "OK");
		respHeaders.add("Auth-Server", backendSrv);
		respHeaders.add("Auth-Port", qp.backendPort);

		// Support for login without @domain.tld and alias
		if (!qp.user.equals(backendLatd) || !qp.latd.equals(backendLatd)) {
			respHeaders.add("Auth-User", backendLatd);
		}
	}

	/**
	 * @param b64
	 * @return
	 */
	public static String decode(String b64) {
		return new String(java.util.Base64.getDecoder().decode((b64)));
	}

	@Override
	public void setVertxExecutor(Vertx vertx, ExecutorService bmExecutor) {
		this.vertx = vertx;
		this.blocking = BlockingCode.forVertx(this.vertx).withExecutor(bmExecutor);
		logger.info("Init with {}", vertx);

		vertx.eventBus().consumer("bm.defaultdomain.changed", new Handler<Message<Object>>() {

			@Override
			public void handle(Message<Object> event) {
				loadDefaultDomain();
			}
		});

	}
}
