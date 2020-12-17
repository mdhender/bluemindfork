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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import net.bluemind.core.caches.registry.CacheRegistry;
import net.bluemind.core.caches.registry.ICacheRegistration;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.IDirectory;
import net.bluemind.group.api.IGroup;
import net.bluemind.mailflow.common.api.Message;
import net.bluemind.mailflow.rbe.IClientContext;
import net.bluemind.mailflow.rbe.MailRule;
import net.bluemind.mailflow.rbe.MailRuleEvaluation;
import net.bluemind.user.api.IUser;

public class SenderInGroupRule extends DefaultRule implements MailRule {
	@Override
	public String identifier() {
		return "SenderInGroupRule";
	}

	@Override
	public String description() {
		return "Rule matches against sender in given group";
	}

	private static Cache<String, List<String>> groupMapping = Caffeine.newBuilder().recordStats().maximumSize(1000)
			.expireAfterWrite(10, TimeUnit.MINUTES).build();

	public static class CacheRegistration implements ICacheRegistration {
		@Override
		public void registerCaches(CacheRegistry cr) {
			cr.register(SenderInGroupRule.class, groupMapping);
		}
	}

	@Override
	public MailRuleEvaluation evaluate(Message message, IClientContext mailflowContext) {
		String from = message.sendingAs.from;

		List<String> groups = groupMapping.getIfPresent(from);
		if (null == groups) {
			IDirectory dir = mailflowContext.provider().instance(IDirectory.class,
					mailflowContext.getSenderDomain().uid);
			DirEntry entry = dir.getByEmail(from);
			if (null == entry) {
				return MailRuleEvaluation.rejected();
			} else {
				switch (entry.kind) {
				case USER:
					IUser userService = mailflowContext.provider().instance(IUser.class,
							mailflowContext.getSenderDomain().uid);
					groups = userService.memberOfGroups(entry.entryUid);
					break;
				case GROUP:
					IGroup groupService = mailflowContext.provider().instance(IGroup.class,
							mailflowContext.getSenderDomain().uid);
					groups = new ArrayList<>(groupService.getParents(entry.entryUid).stream().map(g -> g.uid)
							.collect(Collectors.toList()));
					groups.add(entry.entryUid);
					break;
				default:
					groups = Collections.emptyList();
				}
			}
			groupMapping.put(from, groups);
		}

		if (groups.contains(configuration.get("groupUid"))) {
			return MailRuleEvaluation.accepted();
		} else {
			return MailRuleEvaluation.rejected();
		}
	}

}
