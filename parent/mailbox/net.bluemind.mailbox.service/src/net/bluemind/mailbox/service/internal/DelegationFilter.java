/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2024
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
/**
 * 
 */
package net.bluemind.mailbox.service.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.api.Email;
import net.bluemind.mailbox.api.rules.Delegate;
import net.bluemind.mailbox.api.rules.DelegationRule;
import net.bluemind.mailbox.api.rules.MailFilterRule;
import net.bluemind.mailbox.api.rules.actions.MailFilterRuleActionName;
import net.bluemind.mailbox.api.rules.actions.MailFilterRuleActionRedirect;
import net.bluemind.mailbox.api.rules.actions.MailFilterRuleActionSetFlags;
import net.bluemind.mailbox.api.rules.conditions.MailFilterRuleCondition;
import net.bluemind.mailbox.api.rules.conditions.MailFilterRuleCondition.Operator;
import net.bluemind.mailbox.api.rules.conditions.MailFilterRuleFilterContains;
import net.bluemind.mailbox.api.rules.conditions.MailFilterRuleFilterEquals;
import net.bluemind.mailbox.api.rules.conditions.MailFilterRuleOperatorName;

/**
 * 
 */
public class DelegationFilter extends MailFilterRule {
	private static final Logger logger = LoggerFactory.getLogger(DelegationFilter.class);

	public static final String NAME = "Copy iMIP to Delegates";
	public static final String CLIENT = "system";
	private static final String CAL_HEADER = "headers.X-BM-Calendar";
	private static final String PRIVATE_EVENT_HEADER = "headers.X-BM-Event-Private";

	public DelegationFilter(boolean active) {
		super.name = NAME;
		super.client = CLIENT;
		super.active = active;
	}

	public DelegationFilter(MailFilterRule mf) {
		this(mf.active);
		super.actions = mf.actions;
		super.conditions = mf.conditions;
		super.clientProperties = mf.clientProperties;
		super.deferred = mf.deferred;
		super.stop = mf.stop;
		super.trigger = mf.trigger;
		super.type = mf.type;
	}

	public Optional<String> getCalendarUid() {
		return this.conditions.stream().map(c -> c.filter)
				.filter(rf -> rf.operator == MailFilterRuleOperatorName.CONTAINS && rf.fields.contains(CAL_HEADER))
				.map(rf -> (MailFilterRuleFilterContains) rf).findFirst().stream().map(c -> c.values.get(0))
				.findFirst();
	}

	public boolean getBmEventReadOnlyFlag() {
		return this.actions.stream().filter(a -> a.name == MailFilterRuleActionName.SET_FLAGS)
				.map(a -> (MailFilterRuleActionSetFlags) a).findFirst().stream()
				.anyMatch(a -> a.flags.contains("BmEventReadOnly"));
	}

	public DelegationRule createDelegationRule(String mailboxUid) {
		Optional<String> containerCalUid = getCalendarUid();
		if (!containerCalUid.isPresent()) {
			return null;
		}

		List<MailFilterRuleActionRedirect> redirectActions = this.actions.stream()
				.filter(a -> a.name == MailFilterRuleActionName.REDIRECT).map(a -> (MailFilterRuleActionRedirect) a)
				.toList();
		if (redirectActions.isEmpty()) {
			return null;
		}

		List<Delegate> delegates = new ArrayList<>();
		redirectActions.forEach(redirectAction -> {
			boolean delegateActionRule = redirectAction.clientProperties.entrySet().stream()
					.anyMatch(e -> e.getKey().equals("type") && e.getValue().equals("delegation"));
			if (delegateActionRule) {
				redirectAction.clientProperties.entrySet().stream().filter(e -> e.getKey().equals("delegate"))
						.map(e -> e.getValue()).findFirst()
						.ifPresent(uid -> delegates.add(new Delegate(uid, redirectAction.keepCopy)));
			}
		});

		return new DelegationRule(containerCalUid.get(), delegates, mailboxUid, getBmEventReadOnlyFlag());
	}

	public static boolean isDelegationRule(MailFilterRule rule) {
		return rule.name.equals(NAME) && rule.client.equals(CLIENT);
	}

	public static DelegationRule getDelegationFilterRule(List<MailFilterRule> imipFilterRule, String mailboxUid) {
		imipFilterRule.stream().filter(r -> isDelegationRule(r)).collect(Collectors.toList());
		if (imipFilterRule.isEmpty()) {
			return null;
		}

		if (imipFilterRule.size() > 1) {
			logger.warn("Too many '" + NAME + "' rules found for mailbox " + mailboxUid);
		}

		DelegationFilter delegationFilter = new DelegationFilter(imipFilterRule.get(0));

		Optional<String> containerCalUid = delegationFilter.getCalendarUid();
		if (!containerCalUid.isPresent()) {
			return null;
		}

		List<MailFilterRuleActionRedirect> redirectActions = delegationFilter.actions.stream()
				.filter(a -> a.name == MailFilterRuleActionName.REDIRECT).map(a -> (MailFilterRuleActionRedirect) a)
				.toList();
		if (redirectActions.isEmpty()) {
			return null;
		}

		List<Delegate> delegates = new ArrayList<>();
		redirectActions.forEach(redirectAction -> {
			boolean delegateActionRule = redirectAction.clientProperties.entrySet().stream()
					.anyMatch(e -> e.getKey().equals("type") && e.getValue().equals("delegation"));
			if (delegateActionRule) {
				redirectAction.clientProperties.entrySet().stream().filter(e -> e.getKey().equals("delegate"))
						.map(e -> e.getValue()).findFirst()
						.ifPresent(uid -> delegates.add(new Delegate(uid, redirectAction.keepCopy)));
			}
		});

		return new DelegationRule(containerCalUid.get(), delegates, mailboxUid,
				delegationFilter.getBmEventReadOnlyFlag());
	}

	public static DelegationFilter createDelegateFilterWithConditions(DelegationRule delegationRule) {
		DelegationFilter filter = new DelegationFilter(true);

		MailFilterRuleFilterContains calendarRuleFilter = new MailFilterRuleFilterContains(Arrays.asList(CAL_HEADER),
				Arrays.asList(delegationRule.delegatorCalendarUid));

		MailFilterRuleFilterEquals privateEventRuleFilter = new MailFilterRuleFilterEquals(
				Arrays.asList(PRIVATE_EVENT_HEADER), Arrays.asList("true"));

		List<MailFilterRuleCondition> conditions = Arrays.asList(
				new MailFilterRuleCondition(Operator.AND, calendarRuleFilter, false),
				new MailFilterRuleCondition(Operator.AND, privateEventRuleFilter, true));

		filter.conditions = conditions;
		return filter;
	}

	public void addDelegateFilterRedirectAction(Delegate delegate, Collection<Email> emails) {
		MailFilterRuleActionRedirect redirect = new MailFilterRuleActionRedirect(
				emails.stream().map(e -> e.address).toList(), true);

		Map<String, String> clientProps = new HashMap<>();
		clientProps.put("type", "delegation");
		clientProps.put("delegate", delegate.uid);
		redirect.clientProperties.putAll(clientProps);
		redirect.keepCopy = delegate.keepCopy;
		super.actions.add(redirect);
	}

	public void addDelegateFilterSetFlagAction() {
		MailFilterRuleActionSetFlags setFlags = new MailFilterRuleActionSetFlags(Arrays.asList("BmEventReadOnly"));
		setFlags.clientProperties.put("type", "delegation");
		super.actions.add(setFlags);
	}

}
