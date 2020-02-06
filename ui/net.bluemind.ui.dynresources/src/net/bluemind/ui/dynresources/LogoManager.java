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
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.ui.dynresources;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.ByteStreams;

import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import net.bluemind.config.Token;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.rest.http.ClientSideServiceProvider;
import net.bluemind.hornetq.client.Topic;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.locator.client.LocatorClient;
import net.bluemind.system.api.CustomLogo;
import net.bluemind.system.api.IInstallation;
import net.bluemind.webmodule.server.LogoVersion;

public class LogoManager {

	private static final Logger logger = LoggerFactory.getLogger(LogoManager.class);
	private static Map<String, CustomLogo> logos = new HashMap<String, CustomLogo>();

	public static void init() {

		loadDefaultLogo();

		loadInstallationLogo();

		VertxPlatform.eventBus().consumer(Topic.UI_RESOURCES_NOTIFICATIONS, new Handler<Message<JsonObject>>() {

			@Override
			public void handle(Message<JsonObject> event) {
				JsonObject msg = event.body();
				String entity = msg.getString("entity");
				String op = msg.getString("operation");

				if ("setLogo".equals(op)) {
					LogoManager.setLogo(entity, msg.getString("version"));
				} else if ("deleteLogo".equals(op)) {
					LogoManager.deleteLogo(entity);
				}
			}
		});

	}

	public static CustomLogo getLogo() {
		CustomLogo logo;
		if (logos.containsKey("installation")) {
			logo = logos.get("installation");
		} else {
			logo = logos.get("default");
		}

		return logo;
	}

	public static boolean hasCustomLogo() {
		return logos.containsKey("installation");
	}

	public static void setLogo(String entity, String version) {
		logger.info("set logo for {}, version {}", entity, version);
		if ("installation".equals(entity)) {
			loadInstallationLogo();
		}
	}

	public static void deleteLogo(String entity) {
		logger.info("delete logo for {}", entity);
		LogoVersion.deleteVersion(entity);
		logos.remove(entity);
	}

	public static void loadAll() {
		loadDefaultLogo();
		loadInstallationLogo();
	}

	private static void loadDefaultLogo() {
		try (InputStream in = LogoHandler.class.getClassLoader()
				.getResourceAsStream("web-resources/images/logo-bluemind.png")) {
			CustomLogo cl = new CustomLogo();
			cl.content = ByteStreams.toByteArray(in);
			cl.version = "0";
			logos.put("default", cl);
			LogoVersion.setVersion("default", "0");
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}

	private static void loadInstallationLogo() {
		LocatorClient lc = new LocatorClient();
		String core = lc.locateHost("bm/core", "admin0@global.virt");
		if (core == null) {
			core = "127.0.0.1";
		}
		String coreUrl = "http://" + core + ":8090";
		try {
			CustomLogo cl = ClientSideServiceProvider.getProvider(coreUrl, Token.admin0()).instance(IInstallation.class)
					.getLogo();
			if (cl != null) {
				logos.put("installation", cl);
				LogoVersion.setVersion("installation", cl.version);
			}
		} catch (ServerFault sf) {
			logger.warn("Fail to load installation logo", sf);
		}

	}

}
