/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2019
  *
  * This file is part of BlueMind. BlueMind is a messaging and collaborative
  * solution.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of either the GNU Affero General Public License as
  * published by the Free Software Foundation (version 3 of the License).
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.cti.service.internal;

import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Handler;
import io.vertx.core.Verticle;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import net.bluemind.config.InstallationId;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.caches.registry.CacheRegistry;
import net.bluemind.core.caches.registry.ICacheRegistration;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.cti.api.IComputerTelephonyIntegration;
import net.bluemind.cti.api.Status;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.api.IDomains;
import net.bluemind.lib.vertx.IUniqueVerticleFactory;
import net.bluemind.lib.vertx.IVerticleFactory;
import net.bluemind.lib.vertx.utils.ThrottleMessages;
import net.bluemind.user.api.IUserSettings;

public class CTIPresenceHandler extends AbstractVerticle {
	private static final Logger logger = LoggerFactory.getLogger(CTIPresenceHandler.class);
	private static final Cache<String, String> uidForEmail = CacheBuilder.newBuilder()
			.recordStats()
			.expireAfterAccess(5, TimeUnit.MINUTES)
			.maximumSize(2048)
			.build();

	public static class CacheRegistration implements ICacheRegistration {
		@Override
		public void registerCaches(CacheRegistry cr) {
			cr.register(CTIPresenceHandler.class, uidForEmail);
		}
	}

	public static final String ADDR = "throttled.presence";

	public static class PresFactory implements IVerticleFactory, IUniqueVerticleFactory {

		@Override
		public boolean isWorker() {
			return true;
		}

		@Override
		public Verticle newInstance() {
			return new CTIPresenceHandler();
		}

	}

	@Override
	public void start() {
		Function<Message<JsonObject>, Object> eventToKey = (msg) -> msg.body().getString("user", "anon");
		Handler<Message<JsonObject>> presHandler = this::handle;
		ThrottleMessages<JsonObject> tm = new ThrottleMessages<JsonObject>(eventToKey, presHandler, vertx, 2000);
		vertx.eventBus().consumer(ADDR, tm);
	}

	private void handle(Message<? extends JsonObject> msg) {
		JsonObject js = msg.body();

		ServerSideServiceProvider core = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);
		String user = js.getString("user");
		String show = js.getString("show");

		boolean dnd = (show != null && !show.isEmpty() && !"online".equals(show));
		try {
			IDomains domApi = core.instance(IDomains.class, InstallationId.getIdentifier());
			ItemValue<Domain> domainItem = domApi.findByNameOrAliases(user.split("@")[1]);
			if (domainItem == null) {
				throw new ServerFault("Domain not found " + user.split("@")[1]);
			}
			String userUid = uidForEmail.get(user, () -> {
				ItemValue<net.bluemind.user.api.User> userItem = core
						.instance(net.bluemind.user.api.IUser.class, domainItem.uid).byEmail(user);
				if (userItem == null) {
					throw ServerFault.notFound("user " + user + " not found.");
				}
				return userItem.uid;
			});
			if (userUid == null) {
				return;
			}

			Map<String, String> settings = core.instance(IUserSettings.class, domainItem.uid).get(userUid);

			String pres = settings.get("im_set_phone_presence");
			IComputerTelephonyIntegration ctiApi = core.instance(IComputerTelephonyIntegration.class, domainItem.uid,
					userUid);
			if ("false".equals(pres)) {
				logger.debug("Don't touch Phone presence for " + user);
			} else if ("dnd".equals(pres)) {
				logger.info("set user: " + userUid + ", show: " + show + ", dnd: " + dnd);
				if (dnd) {
					ctiApi.setStatus("IM", Status.create(Status.Type.DoNotDisturb, "Do not disturb"));
				} else {
					ctiApi.setStatus("IM", Status.create(Status.Type.Available, null));
				}
			} else if (!pres.isEmpty()) {
				// FWD
				logger.info("set user: " + userUid + ", forwad: " + pres);
				if (dnd) {
					ctiApi.forward("IM", pres);
				} else {
					ctiApi.forward("IM", "");
				}
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}

}
