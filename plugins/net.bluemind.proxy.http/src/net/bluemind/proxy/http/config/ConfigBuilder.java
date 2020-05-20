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
import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.network.topology.IServiceTopology;
import net.bluemind.network.topology.Topology;
import net.bluemind.proxy.http.impl.ExtensionConfigLoader;
import net.bluemind.proxy.http.impl.FSConfigLoader;
import net.bluemind.proxy.http.impl.IConfigLoader;
import net.bluemind.proxy.http.impl.InBundleConfigLoader;

public final class ConfigBuilder {

	private static final Logger logger = LoggerFactory.getLogger(ConfigBuilder.class);

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

		Collection<ForwardedLocation> locations = conf.getForwardedLocations();
		IServiceTopology topo = Topology.get();
		for (ForwardedLocation loc : locations) {
			String tgtUrl = loc.getTargetUrl();
			if (tgtUrl.startsWith("locator://")) {
				int portIndex = tgtUrl.lastIndexOf(':');
				String tag = tgtUrl.substring("locator://".length(), portIndex);
				String host = topo.anyIfPresent(tag).map(s -> s.value.address()).orElse("127.0.0.1");
				int port = Integer.parseInt(tgtUrl.substring(portIndex + 1, tgtUrl.indexOf('/', portIndex)));
				logger.debug("located {} for {} => h: {}, p: {}", tag, tgtUrl, host, port);
				tgtUrl = "http://" + host + tgtUrl.substring(portIndex);
				loc.setTargetUrl(tgtUrl);
				loc.setHost(host);
				loc.setPort(port);
			} else {
				int portIndex = tgtUrl.lastIndexOf(':');
				String host = tgtUrl.substring("http://".length(), portIndex);
				int port = Integer.parseInt(tgtUrl.substring(portIndex + 1, tgtUrl.indexOf('/', portIndex)));
				logger.debug("parsed {}  => h: {}, p: {}", tgtUrl, host, port);
				loc.setHost(host);
				loc.setPort(port);
			}
		}

		return conf;
	}

}
