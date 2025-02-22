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
package net.bluemind.milter.cache;

import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import net.bluemind.config.Token;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.http.ClientSideServiceProvider;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.api.IDomains;
import net.bluemind.milter.mq.MilterMessageForwarder;
import net.bluemind.network.topology.Topology;

public class DomainAliasCache extends AbstractVerticle {
	private static final Logger logger = LoggerFactory.getLogger(DomainAliasCache.class);

	protected static Map<String, ItemValue<Domain>> domainCache = new ConcurrentHashMap<>();

	public static ItemValue<Domain> getDomain(String domainPart) {
		if (domainPart == null) {
			return null;
		}

		return domainCache.get(domainPart);
	}

	public static Set<String> allAliases() {
		return domainCache.keySet();
	}

	private static void fillCache() {
		logger.debug("Invalidating domain <-> alias cache with {} entries", domainCache.size());
		domainCache = provider().instance(IDomains.class).all().stream().map(DomainAliasCache::expandAliases)
				.map(Map::entrySet).flatMap(Set::stream)
				.collect(Collectors.toConcurrentMap(Entry::getKey, Entry::getValue));
		logger.info("Alias cache contains {} entries", domainCache.size());
	}

	public static ClientSideServiceProvider provider() {
		String host = "http://" + Topology.get().core().value.address() + ":8090";
		return ClientSideServiceProvider.getProvider(host, Token.admin0());
	}

	private static Map<String, ItemValue<Domain>> expandAliases(ItemValue<Domain> domain) {
		return Stream.concat(Arrays.asList(domain.uid).stream(), domain.value.aliases.stream())
				.collect(Collectors.toMap(alias -> alias, alias -> domain, (alias1, alias2) -> alias1));
	}

	public static Optional<String> getDomainFromEmail(String email) {
		return Optional.ofNullable(email).map(e -> e.split("@")).filter(parts -> parts.length == 2)
				.map(parts -> parts[1]);
	}

	public static Optional<String> getLeftPartFromEmail(String email) {
		return Optional.ofNullable(email).map(e -> e.split("@")).map(parts -> parts[0])
				.filter(leftPart -> !Strings.isNullOrEmpty(leftPart));
	}

	public static String getDomainAlias(String domain) {
		// return domain defaultAlias if domain is domainUid or domain is not an alias.
		// Otherwise, return domain
		return Optional.ofNullable(domain).map(DomainAliasCache::getDomain)
				.filter(domainValue -> domainValue.uid.equals(domain) || !domainValue.value.aliases.contains(domain))
				.map(domainValue -> domainValue.value.defaultAlias).orElse(domain);
	}

	@Override
	public void start() {
		logger.info("Starting domain cache {}", this);
		vertx.eventBus().consumer(MilterMessageForwarder.domainChanged,
				(Message<JsonObject> message) -> DomainAliasCache.fillCache());
	}
}
