/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2019
  *
  * This file is part of BlueMind. BlueMind is a messaging and collaborative
  * solution.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of either the GNU Affero General Public License as
  * published by the Free Software Foundation (version 3 of the License).
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.directory.hollow.datamodel.producer.impl;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import net.bluemind.core.caches.registry.CacheHolder;
import net.bluemind.core.caches.registry.CacheRegistry;
import net.bluemind.core.caches.registry.ICacheRegistration;

public class DomainVersions extends CacheHolder<String, Long> {
	private static final Logger logger = LoggerFactory.getLogger(DomainVersions.class);

	private static Cache<String, Long> build() {
		return CacheBuilder.newBuilder()
				.recordStats()
				.expireAfterWrite(5, TimeUnit.MINUTES)
				.build();
	}

	private static final DomainVersions VERSIONS = new DomainVersions(build());

	public static DomainVersions get() {
		return VERSIONS;
	}

	protected DomainVersions(Cache<String, Long> c) {
		super(c);
	}

	public static class Reg implements ICacheRegistration {

		@Override
		public void registerCaches(CacheRegistry cr) {
			Cache<String, Long> internalCache = VERSIONS.cache.orElse(null);
			logger.debug("Registering {}", internalCache);
			cr.register("hollow.dir.versions", internalCache);
		}

	}

}
