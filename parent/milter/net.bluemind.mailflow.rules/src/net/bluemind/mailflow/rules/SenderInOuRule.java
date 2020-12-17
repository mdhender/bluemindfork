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
package net.bluemind.mailflow.rules;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;

import net.bluemind.core.caches.registry.CacheRegistry;
import net.bluemind.core.caches.registry.ICacheRegistration;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.IDirectory;
import net.bluemind.directory.api.IOrgUnits;
import net.bluemind.directory.api.OrgUnitPath;
import net.bluemind.directory.api.OrgUnitQuery;
import net.bluemind.mailflow.common.api.Message;
import net.bluemind.mailflow.rbe.IClientContext;
import net.bluemind.mailflow.rbe.MailRule;
import net.bluemind.mailflow.rbe.MailRuleEvaluation;

public class SenderInOuRule extends DefaultRule implements MailRule {
	@Override
	public String identifier() {
		return "SenderInOuRule";
	}

	@Override
	public String description() {
		return "Rule matches against sender in given OU";
	}

	private static IClientContext mailflowContext;

	private static LoadingCache<String, Map<String, List<String>>> ouMaouPath = Caffeine.newBuilder().recordStats()
			.expireAfterWrite(10, TimeUnit.MINUTES).build(new CacheLoader<String, Map<String, List<String>>>() {
				public Map<String, List<String>> load(String domain) throws Exception {
					return createOuStructure(domain);
				}
			});

	public static class CacheRegistration implements ICacheRegistration {
		@Override
		public void registerCaches(CacheRegistry cr) {
			cr.register(SenderInOuRule.class, ouMaouPath);
		}
	}

	@Override
	public MailRuleEvaluation evaluate(Message message, IClientContext mailflowContext) {
		SenderInOuRule.mailflowContext = mailflowContext;
		String from = message.sendingAs.from;
		String senderDomain = mailflowContext.getSenderDomain().uid;

		IDirectory dir = mailflowContext.provider().instance(IDirectory.class, senderDomain);
		DirEntry entry = dir.getByEmail(from);

		if (entry == null) {
			return MailRuleEvaluation.rejected();
		}

		if (getOuMapping(senderDomain, entry.orgUnitUid).contains(configuration.get("orgUnitUid"))) {
			Map<String, String> data = getOrgUnitMap(entry.entryUid, configuration.get("orgUnitUid"));
			return MailRuleEvaluation.accepted(data);
		}

		return MailRuleEvaluation.rejected();
	}

	private List<String> getOuMapping(String domain, String ou) {
		if (null == ou) {
			return Collections.emptyList();
		}
		List<String> ous = ouMaouPath.get(domain).get(ou);
		return Optional.ofNullable(ous).orElseGet(Collections::emptyList);
	}

	private Map<String, String> getOrgUnitMap(String entryUid, String orgUnitId) {
		Map<String, String> data = new HashMap<String, String>();
		data.put("entryUid", entryUid);
		data.put("orgUnitUid", orgUnitId);
		return data;
	}

	private static Map<String, List<String>> createOuStructure(String domain) {
		IOrgUnits orgUnitService = mailflowContext.provider().instance(IOrgUnits.class, domain);
		OrgUnitQuery query = new OrgUnitQuery();
		query.query = "";
		List<OrgUnitPath> result = orgUnitService.search(query);
		Map<String, List<String>> hierarchy = new HashMap<>();
		result.forEach(p -> {
			hierarchy.put(p.uid, p.path());
		});
		return hierarchy;
	}
}
