/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2022
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
package net.bluemind.central.reverse.proxy.vertx;

import com.typesafe.config.Config;

import net.bluemind.central.reverse.proxy.common.config.CrpConfig;
import net.bluemind.central.reverse.proxy.vertx.impl.ProxyVerticleFactory;
import net.bluemind.central.reverse.proxy.vertx.impl.postfix.PostfixMapsVerticleFactory;

public class ConfigHolder {

	public static final Config config = CrpConfig.get("Proxy", ProxyVerticleFactory.class.getClassLoader());
	public static final Config postfixMapsConfig = CrpConfig.get("PostfixMaps",
			PostfixMapsVerticleFactory.class.getClassLoader());
	public static final Config milterConfig = CrpConfig.get("Milter",
			PostfixMapsVerticleFactory.class.getClassLoader());
}
