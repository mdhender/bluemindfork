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
package net.bluemind.vertx.common.bus;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.busmods.BusModBase;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;

import net.bluemind.authentication.api.IAuthentication;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.http.ClientSideServiceProvider;
import net.bluemind.network.topology.Topology;
import net.bluemind.vertx.common.LocalJsonObject;
import net.bluemind.vertx.common.LoginRequest;
import net.bluemind.vertx.common.impl.LoginResponse;

public final class CoreAuth extends BusModBase {

	private Handler<Message<LocalJsonObject<LoginRequest>>> loginHandler;
	private Handler<Message<String>> logoutHandler;
	private Handler<Message<LocalJsonObject<LoginRequest>>> validateHandler;

	private static final Logger logger = LoggerFactory.getLogger(CoreAuth.class);

	@Override
	public void start() {
		super.start();

		loginHandler = new Handler<Message<LocalJsonObject<LoginRequest>>>() {
			@Override
			public void handle(Message<LocalJsonObject<LoginRequest>> msg) {
				login(msg);
			}
		};
		eb.registerHandler("core.login", loginHandler);

		validateHandler = new Handler<Message<LocalJsonObject<LoginRequest>>>() {
			@Override
			public void handle(Message<LocalJsonObject<LoginRequest>> msg) {
				validate(msg);
			}
		};
		eb.registerHandler("core.validate", validateHandler);

		logoutHandler = new Handler<Message<String>>() {
			@Override
			public void handle(Message<String> msg) {
				logout(msg);
			}
		};
		eb.registerHandler("core.logout", logoutHandler);
		logger.info("CoreAuth started.");

	}

	private void login(final Message<LocalJsonObject<LoginRequest>> loginMsg) {
		final LoginRequest jso = loginMsg.body().getValue();
		final String latd = jso.getLogin();
		final String role = jso.getRole();

		String url = "http://" + Topology.get().core().value.address() + ":8090";
		String pass = jso.getPass();
		long time = System.currentTimeMillis();
		try {
			IAuthentication authApi = authApi(url);
			net.bluemind.authentication.api.LoginResponse authResponse = authApi.login(latd, pass, jso.getOrigin());
			time = System.currentTimeMillis() - time;
			if (authResponse.status == net.bluemind.authentication.api.LoginResponse.Status.Ok) {
				Set<String> ownedRoles = authResponse.authUser.roles;
				if (role != null && !(ownedRoles.contains(SecurityContext.ROLE_SYSTEM) || ownedRoles.contains(role))) {
					logger.error("[{}] Fail to login {}, does not have role {}", latd, authResponse.status, role);
					LoginResponse lr = new LoginResponse(false, null, null, latd);
					loginMsg.reply(new LocalJsonObject<>(lr));
					return;
				}

				String sid = authResponse.authKey;
				if (logger.isDebugEnabled()) {
					logger.debug("[{}], '{}', login took {}ms.", latd, sid, time);
				}
				LoginResponse lr = new LoginResponse(true, url, sid, authResponse.latd);
				loginMsg.reply(new LocalJsonObject<>(lr));
			} else {
				logger.error("[{}] Fail to login {}", latd, authResponse.status);
				LoginResponse lr = new LoginResponse(false, null, null, latd);
				loginMsg.reply(new LocalJsonObject<>(lr));
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			LoginResponse lr = new LoginResponse(false, null, null, latd);
			loginMsg.reply(new LocalJsonObject<>(lr));
		}

	}

	private IAuthentication authApi(String coreUrl) throws ServerFault {
		return ClientSideServiceProvider.getProvider(coreUrl, null).instance(IAuthentication.class);
	}

	private void validate(final Message<LocalJsonObject<LoginRequest>> loginMsg) {
		final LoginRequest jso = loginMsg.body().getValue();
		final String latd = jso.getLogin();

		String url = "http://" + Topology.get().core().value.address() + ":8090";
		try {
			authApi(url).ping();
			LoginResponse lr = new LoginResponse(true, null, null, latd);
			loginMsg.reply(new LocalJsonObject<>(lr));
		} catch (Exception t) {
			LoginResponse lr = new LoginResponse(false, null, null, null);
			loginMsg.reply(new LocalJsonObject<>(lr));
		}

	}

	private void logout(final Message<String> outMsg) {

		String url = "http://" + Topology.get().core().value.address() + ":8090";
		try {
			authApi(url).logout();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		outMsg.reply();
	}
}
