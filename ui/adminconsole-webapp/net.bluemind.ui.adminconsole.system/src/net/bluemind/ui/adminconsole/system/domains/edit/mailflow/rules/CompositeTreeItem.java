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
package net.bluemind.ui.adminconsole.system.domains.edit.mailflow.rules;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Panel;

import net.bluemind.mailflow.api.MailRuleDescriptor;
import net.bluemind.mailflow.api.MailflowRule;
import net.bluemind.ui.adminconsole.system.domains.edit.mailflow.RuleActionElementFactory;
import net.bluemind.ui.adminconsole.system.domains.edit.mailflow.RuleActionPopup;
import net.bluemind.ui.adminconsole.system.domains.edit.mailflow.RuleAssignmentWidget;

public class CompositeTreeItem extends RuleTreeItem {

	public CompositeTreeItem(RuleAssignmentWidget parent, MailRuleDescriptor descriptor,
			List<MailRuleDescriptor> ruleIdentifiers, Panel config, String domainUid) {
		super(parent, descriptor, config, domainUid);
		addListener(ruleIdentifiers);
	}

	private void addListener(List<MailRuleDescriptor> ruleIdentifiers) {
		ClickHandler clickHandler = createClickHandler(ruleIdentifiers);
		super.getWidget().addDomHandler(clickHandler, ClickEvent.getType());
	}

	private ClickHandler createClickHandler(List<MailRuleDescriptor> ruleIdentifiers) {
		return (c -> {
			config.forEach(w -> config.remove(w));
			List<String> ruleIds = ruleIdentifiers.stream().map(r -> r.ruleIdentifier).collect(Collectors.toList());
			RuleActionPopup popup = new RuleActionPopup(ruleIds, ruleIdentifier -> {
				this.addItem(RuleActionElementFactory.getRuleByIdentifier(parent, ruleIdentifier, ruleIdentifiers,
						config, domainUid));
			});
			popup.setModal(true);
			popup.setPopupPosition(c.getClientX(), c.getClientY());
			popup.show();
		});
	}

	public List<MailflowRule> resolveChildren() {
		List<MailflowRule> children = new ArrayList<MailflowRule>();

		for (int i = 0; i < getChildCount(); i++) {
			children.add(((RuleTreeItem) getChild(i)).toRule());
		}

		return children;
	}

	@Override
	public MailflowRule toRule() {
		MailflowRule rule = new MailflowRule();
		rule.configuration = Collections.emptyMap();
		rule.ruleIdentifier = super.ruleIdentifier;
		rule.children = resolveChildren();
		return rule;
	}

	@Override
	public void set(Map<String, String> configuration) {
	}

}
