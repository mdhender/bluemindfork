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
package net.bluemind.lmtp.filter.tnef;

import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

public class MapiEndpoint {

	private static final Logger logger = LoggerFactory.getLogger(MapiEndpoint.class);

	private static final Cache<String, Boolean> KNOWN_ENDPOINTS = Caffeine.newBuilder()
			.removalListener((k, v, cause) -> logger.info("Expiring mapi endpoint {}", k))
			.expireAfterWrite(10, TimeUnit.SECONDS).build();

	public static final void register(String endpoint) {
		KNOWN_ENDPOINTS.put(endpoint, Boolean.TRUE);
	}

	public static Optional<String> any() {
		String[] eps = KNOWN_ENDPOINTS.asMap().keySet().toArray(new String[0]);
		if (eps.length == 0) {
			return Optional.empty();
		}
		int slot = ThreadLocalRandom.current().nextInt(eps.length);
		return Optional.of(eps[slot]);
	}

}
