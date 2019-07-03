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
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.HttpServerResponse;

import freemarker.template.Configuration;
import freemarker.template.Template;
import net.bluemind.utils.IniFile;

public class TBirdUpdateRdf implements Handler<HttpServerRequest> {
	static final Logger logger = LoggerFactory.getLogger(TBirdUpdateRdf.class);

	private Template template;

	public TBirdUpdateRdf() throws IOException {

		Configuration freemarkerCfg = new Configuration();
		freemarkerCfg.setClassForTemplateLoading(this.getClass(), "/templates");
		freemarkerCfg.setTagSyntax(Configuration.AUTO_DETECT_TAG_SYNTAX);
		template = freemarkerCfg.getTemplate("updateRdf.tpl");

	}

	@Override
	public void handle(HttpServerRequest request) {
		IniFile ini = new IniFile("/etc/bm/bm.ini") {

			@Override
			public String getCategory() {
				return "bm";
			}

		};

		StringWriter sw = new StringWriter();

		Map<String, Object> model = new HashMap<>();
		model.put("version", Activator.bundle.getVersion().toString());
		model.put("url", "https://" + ini.getData().get("external-url") + "/settings/settings/download/tbird.xpi");

		try {
			template.process(model, sw);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}

		HttpServerResponse resp = request.response();
		String body = sw.toString();
		byte[] data = body.getBytes();
		resp.setStatusCode(200);
		resp.end(new Buffer(data));

	}

}
