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
package net.bluemind.directory.hollow.datamodel.producer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Verticle;

import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.domain.api.IDomains;
import net.bluemind.lib.vertx.IVerticleFactory;
import net.bluemind.lib.vertx.utils.ThrottleMessages;

public class DirectorySerializationVerticle extends Verticle {

	private static final Logger logger = LoggerFactory.getLogger(DirectorySerializationVerticle.class);

	@Override
	public void start() {
		activateSerializers();
		registerDomainChangeHandler();
		registerDirectoryChangeHandler();
	}

	private void activateSerializers() {
		try {
			IDomains domApi = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IDomains.class);
			domApi.all().forEach(dom -> Serializers.put(dom.uid, createSerializer(dom.uid)));
		} catch (Exception e) {
			logger.warn("Cannot activate domain serializers", e);
		}
	}

	private DirectorySerializer createSerializer(String domainUid) {
		DirectorySerializer s = new DirectorySerializer(domainUid);
		s.start();
		return s;
	}

	private void registerDomainChangeHandler() {
		vertx.eventBus().registerHandler(DirectorySerializationDomainHook.DOMAIN_CHANGE_EVENT, msg -> {
			JsonObject data = (JsonObject) msg.body();
			String domain = data.getString("domain");
			String action = data.getString("action");
			switch (action) {
			case "create":
				Serializers.put(domain, createSerializer(domain)).produce();
				break;
			case "delete":
				Serializers.forDomain(domain).remove();
				Serializers.remove(domain);
				break;
			}
		});
	}

	private void registerDirectoryChangeHandler() {
		Handler<Message<? extends JsonObject>> dirChangeHandler = (Message<? extends JsonObject> msg) -> {
			String dom = msg.body().getString("domain");
			DirectorySerializer ser = Serializers.forDomain(dom);
			if (ser != null) {
				ser.produce();
			} else {
				logger.warn("Missing serializer for domain {}", dom);
			}
		};
		ThrottleMessages<JsonObject> tm = new ThrottleMessages<JsonObject>((msg) -> msg.body().getString("domain"),
				dirChangeHandler, vertx, 1000);
		vertx.eventBus().registerHandler("dir.changed", tm);
	}

	public static class DirectorySerializationVerticleFactory implements IVerticleFactory {

		@Override
		public boolean isWorker() {
			return true;
		}

		@Override
		public Verticle newInstance() {
			return new DirectorySerializationVerticle();
		}

	}

}
