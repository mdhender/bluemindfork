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

package net.bluemind.defaultapp.server;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.http.HttpServerRequest;
import net.bluemind.role.api.BasicRoles;
import net.bluemind.webmodule.server.handlers.AbstractFtlHandler;

public class DefaultAppIndexHandler extends AbstractFtlHandler {

	private static final Logger logger = LoggerFactory.getLogger(DefaultAppIndexHandler.class);

	@Override
	protected String getTemplateName() {
		return "DefaultApp.ftl";
	}

	@Override
	protected void loadPageModel(HttpServerRequest request, Map<String, Object> model) {
		String defaultApp = (String) model.get("BMDefaultApp");
		String rolesAsString = (String) model.get("BMRoles");
		Set<String> roles = rolesAsString != null ? //
				new HashSet<>(Arrays.asList(rolesAsString.split(","))) //
				: Collections.<String>emptySet();

		if (defaultApp == null) {
			defaultApp = "/webmail/";
		}
		LinkedHashSet<String> apps = new LinkedHashSet<>();
		apps.add(defaultApp);
		apps.add("/webmail/");
		apps.add("/adminconsole/");
		apps.add("/contact/");
		apps.add("/cal/");
		apps.add("/task/");
		apps.add("/settings/");

		logger.debug("defaultApp {}, possible apps {}, roles {}", defaultApp, apps, roles);
		for (Iterator<String> it = apps.iterator(); it.hasNext();) {
			String app = it.next();
			boolean ok = true;
			switch (app) {
			case "/webmail/":
				ok = roles.contains(BasicRoles.ROLE_MAIL);
				break;
			case "/contact/":
				ok = roles.contains(BasicRoles.ROLE_MAIL);
				break;
			case "/cal/":
				ok = roles.contains(BasicRoles.ROLE_CALENDAR);
				break;
			case "/task/":
				ok = roles.contains(BasicRoles.ROLE_CALENDAR);
				break;
			case "/adminconsole/":
				ok = roles.contains(BasicRoles.ROLE_ADMIN);
				break;
			case "/settings/":
				ok = true;
				break;
			default:
				ok = false;
				break;
			}

			if (!ok) {
				it.remove();
			}
		}

		model.put("BMDefaultApp", apps.iterator().next());

	}

}
