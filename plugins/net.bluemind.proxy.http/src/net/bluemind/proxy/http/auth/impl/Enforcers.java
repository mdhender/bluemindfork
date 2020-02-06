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
package net.bluemind.proxy.http.auth.impl;

import java.util.List;

import io.vertx.core.Vertx;
import net.bluemind.eclipse.common.RunnableExtensionLoader;
import net.bluemind.proxy.http.NeedVertx;
import net.bluemind.proxy.http.auth.api.IAuthEnforcer;

public final class Enforcers {

	public static List<IAuthEnforcer> enforcers(Vertx vertx) {
		RunnableExtensionLoader<IAuthEnforcer> rel = new RunnableExtensionLoader<IAuthEnforcer>();
		List<IAuthEnforcer> list = rel.loadExtensionsWithPriority("net.bluemind.proxy.http", "authenforcer", "enforcer",
				"impl");
		list.stream().forEach(auth -> {
			if (auth instanceof NeedVertx) {
				((NeedVertx) auth).setVertx(vertx);
			}
		});
		return list;
	}

}
