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
package net.bluemind.ysnp.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.Cache;

import net.bluemind.hornetq.client.MQ;
import net.bluemind.hornetq.client.OOPMessage;
import net.bluemind.hornetq.client.Topic;

public class TokenCacheSync {

	private static final Logger logger = LoggerFactory.getLogger(TokenCacheSync.class);

	public void start(final Cache<String, String> cache, final Cache<String, String> pwCache) {

		MQ.init(() -> MQ.registerConsumer(Topic.CORE_SESSIONS, (OOPMessage cm) -> {
			String operation = cm.getStringProperty("operation");
			if ("login".equals(operation)) {
				String latd = cm.getStringProperty("login") + "@" + cm.getStringProperty("domain");
				cache.put(cm.getStringProperty("sid"), latd);
				if (logger.isDebugEnabled()) {
					logger.debug("cached token for {}, origin: {}", latd, cm.getStringProperty("origin"));
				}
			} else if ("logout".equals(operation)) {
				String sid = cm.getStringProperty("sid");
				cache.invalidate(sid);
				if (logger.isDebugEnabled()) {
					logger.debug("invalidate token {}", sid);
				}
			} else if ("pwchange".equals(operation)) {
				String latd = cm.getStringProperty("latd");
				pwCache.invalidate(latd);
			}
		}));
	}

}
