/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2017
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
package net.bluemind.milter.action;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.platform.Verticle;

import net.bluemind.config.Token;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.core.rest.http.ClientSideServiceProvider;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.api.IDomains;
import net.bluemind.milter.mq.MilterMessageForwarder;
import net.bluemind.network.topology.Topology;

public class DomainAliasCache extends Verticle {

	private static final Logger logger = LoggerFactory.getLogger(DomainAliasCache.class);
	private static Map<String, Optional<ItemValue<Domain>>> domainCache = new ConcurrentHashMap<>();

	public static ItemValue<Domain> getDomain(String domainPart) {
		if (domainPart == null) {
			return null;
		}
		return domainCache.computeIfAbsent(domainPart, domOrAlias -> {
			String host = "http://" + Topology.get().core().value.address() + ":8090";
			IServiceProvider prov = ClientSideServiceProvider.getProvider(host, Token.admin0());
			return Optional.ofNullable(prov.instance(IDomains.class).findByNameOrAliases(domOrAlias));
		}).orElse(null);
	}

	@Override
	public void start() {
		logger.info("Starting domain cache {}", this);
		vertx.eventBus().registerHandler(MilterMessageForwarder.domainChanged, (message) -> {
			logger.info("Invalidating domain <-> alias cache with {} entries", domainCache.size());
			domainCache.clear();
		});
	}
}
