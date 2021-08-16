/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2021
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
package net.bluemind.webmodules.videoconferencing.saas;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import net.bluemind.webmodule.server.NeedVertx;
import net.bluemind.webmodule.server.WebModule;
import net.bluemind.webmodule.server.handlers.AbstractIndexHandler;
import net.bluemind.webmodule.server.handlers.IWebModuleConsumer;
import net.bluemind.webmodule.server.handlers.StaticFileHandler;

public class VisioHandler extends AbstractIndexHandler implements NeedVertx, IWebModuleConsumer {

	private static final Logger logger = LoggerFactory.getLogger(VisioHandler.class);
	private static final Pattern roomPattern = Pattern.compile(
			"(?:[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[1-5][0-9a-fA-F]{3}-[89ABab][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$)|(?:[0-9a-zA-Z-_]{7,14}$)");

	private static final String BLUEMIND_VIDEO_DOMAIN = tryReadDomain(Paths.get("/etc/bm", "bluemind.video"),
			"video.bluemind.net");

	private static String tryReadDomain(Path p, String defaultValue) {
		if (p.toFile().exists()) {
			try {
				return new String(Files.readAllBytes(p)).trim();
			} catch (IOException ie) {
			}
		}
		return defaultValue;
	}

	private Vertx vertx;
	private WebModule webModule;
	private StaticFileHandler staticHandler;

	public VisioHandler() {
		logger.info("VISIO HANDLER STARTED");
	}

	@Override
	protected String getTemplateName() {
		return "visio.ftl";
	}

	@Override
	public void handle(HttpServerRequest request) {
		Matcher roomMatch = roomPattern.matcher(request.path());
		if (!roomMatch.find()) {
			logger.debug("{} does not match room pattern, serving static content", request.path());
			staticHandler.handle(request);
			return;
		}
		super.handle(request);
	}

	@Override
	protected void loadPageModel(HttpServerRequest request, Map<String, Object> model) {
		String sessionId = request.headers().get("BMSessionId");
		if (sessionId == null || sessionId.isEmpty()) {
			model.put("BMLang", getAnonymousLang(request));
		}
		model.put("BMJitsiSaasDomain", BLUEMIND_VIDEO_DOMAIN);
	}

	private String getAnonymousLang(HttpServerRequest request) {
		String acceptLang = request.headers().get("Accept-Language");
		if (acceptLang == null) {
			return "en";
		}
		return Locale.LanguageRange.parse(acceptLang).stream() //
				.map(range -> Locale.forLanguageTag(range.getRange())).findFirst() //
				.orElse(new Locale.Builder().setLanguage("en").build()).getLanguage().toLowerCase();
	}

	@Override
	public void setVertx(Vertx vertx) {
		this.vertx = vertx;
		setupStaticHandler();
	}

	@Override
	public void setModule(WebModule module) {
		super.setModule(module);
		this.webModule = module;
		setupStaticHandler();
	}

	private void setupStaticHandler() {
		/*
		 * The order in which setModule and setVertx is not know in advence, so we just
		 * try to initialize in both.
		 */
		if (webModule != null && vertx != null) {
			staticHandler = new StaticFileHandler(vertx, webModule.root, webModule.index, webModule.resources, true,
					true);
		}
	}

}
