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
package net.bluemind.webmodules.webapp.wrapper.handlers;

import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import io.vertx.core.http.HttpServerRequest;
import net.bluemind.webmodule.server.handlers.AbstractIndexHandler;

public class CalendarHandler extends AbstractIndexHandler {

	@Override
	protected String getTemplateName() {
		return "Calendar.ftl";
	}

	@Override
	protected void loadPageModel(HttpServerRequest request, Map<String, Object> model) {
		super.loadPageModel(request, model);
		String lang = getLang(request);
		ResourceBundle resourceBundle = ResourceBundle.getBundle("OSGI-INF/l10n/bundle", new Locale(lang));
		model.put("appName", resourceBundle.getString("calendar.name"));
	}

}
