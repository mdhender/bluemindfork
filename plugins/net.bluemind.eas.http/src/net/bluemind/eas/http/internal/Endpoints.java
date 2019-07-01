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
package net.bluemind.eas.http.internal;

import java.util.List;

import net.bluemind.eas.http.IEasRequestEndpoint;
import net.bluemind.eas.utils.RunnableExtensionLoader;

public class Endpoints {

	private static List<IEasRequestEndpoint> endpoints;

	public static List<IEasRequestEndpoint> get() {
		return endpoints;
	}

	public static void classLoad() {
		RunnableExtensionLoader<IEasRequestEndpoint> rel = new RunnableExtensionLoader<>();
		endpoints = rel.loadExtensions("net.bluemind.eas.http", "endpoint", "handler", "impl");
	}

}
