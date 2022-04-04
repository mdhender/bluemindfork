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

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import io.netty.util.internal.StringUtil;
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
	private static final Cache<String, String> uidForEmail = Caffeine.newBuilder().recordStats()
			.expireAfterAccess(5, TimeUnit.MINUTES).maximumSize(2048).build();

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
		Function<Message<JsonObject>, Object> eventToKey = msg -> msg.body().getString("user", "anon");
		Handler<Message<JsonObject>> presHandler = this::handle;
		ThrottleMessages<JsonObject> tm = new ThrottleMessages<>(eventToKey, presHandler, vertx, 2000);
		vertx.eventBus().consumer(ADDR, tm);
	}

	private void handle(Message<? extends JsonObject> msg) {
		JsonObject js = msg.body();

		String user = js.getString("user");
		String show = js.getString("show");
		ServerSideServiceProvider core = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);

		boolean dnd = (show != null && !show.isEmpty() && !"online".equals(show));
		try {
			IDomains domApi = core.instance(IDomains.class, InstallationId.getIdentifier());
			ItemValue<Domain> domainItem = domApi.findByNameOrAliases(user.split("@")[1]);
			if (domainItem == null) {
				throw new ServerFault("Domain not found " + user.split("@")[1]);
			}
			String userUid = uidForEmail.get(user, userKey -> {
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

			if (!StringUtil.isNullOrEmpty(pres) && !"false".equals(pres)) {
				if ("dnd".equals(pres)) {
					setDoNotDisturb(core, show, dnd, domainItem, userUid);
				} else {
					setAvailability(core, dnd, domainItem, userUid, pres);
				}
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}

	private void setAvailability(ServerSideServiceProvider core, boolean dnd, ItemValue<Domain> domainItem,
			String userUid, String pres) {
		if (dnd) {
			ctiApi(core, userUid, domainItem.uid).ifPresent(service -> service.forward("IM", pres));
		} else {
			ctiApi(core, userUid, domainItem.uid).ifPresent(service -> service.forward("IM", ""));
		}
	}

	private void setDoNotDisturb(ServerSideServiceProvider core, String show, boolean dnd, ItemValue<Domain> domainItem,
			String userUid) {
		if (dnd) {
			ctiApi(core, userUid, domainItem.uid).ifPresent(
					service -> service.setStatus("IM", Status.create(Status.Type.DoNotDisturb, "Do not disturb")));
		} else {
			ctiApi(core, userUid, domainItem.uid)
					.ifPresent(service -> service.setStatus("IM", Status.create(Status.Type.Available, null)));
		}
	}

	private Optional<IComputerTelephonyIntegration> ctiApi(ServerSideServiceProvider core, String domain,
			String userUid) {
		try {
			return Optional.of(core.instance(IComputerTelephonyIntegration.class, domain, userUid));
		} catch (Exception e) {
			logger.warn("Cannot retrieve cti implementation of user {}@{}", userUid, domain, e);
			return Optional.empty();
		}

	}

}
