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
package net.bluemind.proxy.http.impl;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.proxy.http.config.ForwardedLocation;
import net.bluemind.proxy.http.config.HPSConfiguration;

public class ConfigIndex {

	ConcurrentHashMap<String, ForwardedLocation> fwds;

	private HPSConfiguration conf;

	private static final Logger logger = LoggerFactory.getLogger(ConfigIndex.class);

	public ConfigIndex(HPSConfiguration conf) {
		this.conf = conf;
		this.fwds = new ConcurrentHashMap<String, ForwardedLocation>();
		Collection<ForwardedLocation> locs = conf.getForwardedLocations();
		for (ForwardedLocation fl : locs) {
			fwds.put(fl.getPathPrefix(), fl);
		}
	}

	public ForwardedLocation getLocation(String turi) {
		String uri = turi;
		int qidx = uri.indexOf('?');
		if (qidx > 0) {
			uri = turi.substring(0, qidx);
		}

		ForwardedLocation ret = null;
		String cur = uri;
		do {
			ret = fwds.get(cur);
			if (logger.isDebugEnabled()) {
				logger.debug(" * fwd for " + cur + " => " + ret);
			}
			int idx = cur.lastIndexOf('/');
			if (idx >= 0) {
				cur = cur.substring(0, idx);
			}
		} while (ret == null && cur.length() > 0);
		return ret != null ? ret : fwds.get("/");
	}

	public HPSConfiguration getConfig() {
		return conf;
	}
}
