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
package net.bluemind.proxy.http.config;

import java.io.File;

import net.bluemind.proxy.http.impl.ExtensionConfigLoader;
import net.bluemind.proxy.http.impl.FSConfigLoader;
import net.bluemind.proxy.http.impl.IConfigLoader;
import net.bluemind.proxy.http.impl.InBundleConfigLoader;

public final class ConfigBuilder {
	private ConfigBuilder() {
	}

	public static HPSConfiguration build() {
		HPSConfiguration conf = new HPSConfiguration();

		File co = new File("/etc/bm-hps/bm_sso.xml");
		IConfigLoader cl = null;
		if (co.exists()) {
			cl = new FSConfigLoader();
		} else {
			cl = new InBundleConfigLoader();
		}
		cl.load(conf);

		IConfigLoader ecl = new ExtensionConfigLoader();
		ecl.load(conf);

		return conf;
	}

}
