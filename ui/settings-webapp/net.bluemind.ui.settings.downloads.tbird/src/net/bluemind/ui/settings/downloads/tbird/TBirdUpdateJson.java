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
package net.bluemind.ui.settings.downloads.tbird;

import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import freemarker.template.Configuration;
import freemarker.template.Template;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;

public class TBirdUpdateJson implements Handler<HttpServerRequest> {
	private static final Logger logger = LoggerFactory.getLogger(TBirdUpdateJson.class);

	private Template template;

	public TBirdUpdateJson() throws IOException {
		Configuration freemarkerCfg = new Configuration();
		freemarkerCfg.setClassForTemplateLoading(this.getClass(), "/templates");
		freemarkerCfg.setTagSyntax(Configuration.AUTO_DETECT_TAG_SYNTAX);
		template = freemarkerCfg.getTemplate("updateJson.tpl");
	}

	@Override
	public void handle(HttpServerRequest request) {
		String userAgent = request.getHeader(HttpHeaders.USER_AGENT);
		String downloadUrl = "https://%s/settings/settings/download/";
		String version = Activator.bundle.getVersion().toString();

		// Add .0 to legacy version and .1 to webext version
		// to handle update from legacy extension to webext when updating to TB 78.
		// Sample scenario:
		// - TB 68 with legacy connector v 4.3.2000
		// - update BM to v 4.3.2030
		// - TB 68 update connector to v 4.3.2030.0
		// - TB 68 update to TB 78
		// - TB 78 update connector to v 4.3.2030.1
		if (userAgent.contains("Thunderbird/78")) {
			downloadUrl += "tbird-webext.xpi";
			version += ".1";
		} else {
			downloadUrl += "tbird.xpi";
			version += ".0";
		}

		StringWriter sw = new StringWriter();

		Map<String, Object> model = new HashMap<>();
		model.put("version", version);
		model.put("url", String.format(downloadUrl, TBirdDownloadHandler.getExternalUrl()));

		try {
			template.process(model, sw);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}

		HttpServerResponse resp = request.response();
		String body = sw.toString();
		byte[] data = body.getBytes();
		resp.setStatusCode(200);
		resp.end(Buffer.buffer(data));
	}
}
