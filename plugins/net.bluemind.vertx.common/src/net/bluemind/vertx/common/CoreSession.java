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
package net.bluemind.vertx.common;

import org.vertx.java.core.eventbus.EventBus;

import net.bluemind.lib.vertx.VertxPlatform;

public final class CoreSession {

	private static final EventBus eb = VertxPlatform.eventBus();

	public static final void attempt(String login, String pass, String orig, LoginHandler lh) {
		eb.send("core.login", LoginRequest.of(login, pass, orig), lh);
	}

	public static final void attemptWithRole(String login, String pass, String orig, String role, LoginHandler lh) {
		eb.send("core.login", LoginRequest.of(login, pass, orig, role), lh);
	}

	public static final void validate(String login, String pass, String orig, ValidationHandler vh) {
		eb.send("core.validate", LoginRequest.of(login, pass, orig), vh);
	}

	public static final void logout(String token) {
		eb.send("core.logout", token);
	}

}
