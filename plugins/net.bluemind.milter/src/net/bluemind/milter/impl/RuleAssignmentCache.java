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
package net.bluemind.milter.impl;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Verticle;

import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.mailflow.api.IMailflowRules;
import net.bluemind.mailflow.api.MailRuleActionAssignment;
import net.bluemind.mailflow.rbe.IClientContext;
import net.bluemind.milter.mq.MilterMessageForwarder;

public class RuleAssignmentCache extends Verticle {

	private static Map<String, List<MailRuleActionAssignment>> cache = new ConcurrentHashMap<>();

	private static final Logger logger = LoggerFactory.getLogger(RuleAssignmentCache.class);

	@Override
	public void start() {
		logger.info("Registering rule assignment cache listener");
		VertxPlatform.eventBus().registerHandler(MilterMessageForwarder.eventAddressChanged, (message) -> {
			String domainUid = ((JsonObject) message.body()).getString("domainUid");
			logger.info("Invalidating rule assignment cache for domain {}", domainUid);
			cache.remove(domainUid);
		});
	}

	public static List<MailRuleActionAssignment> getStoredRuleAssignments(IClientContext mailflowContext,
			String domain) {
		if (!cache.containsKey(domain)) {
			cache.put(domain, mailflowContext.provider().instance(IMailflowRules.class, domain).listAssignments());
		}
		return cache.get(domain);
	}

}
