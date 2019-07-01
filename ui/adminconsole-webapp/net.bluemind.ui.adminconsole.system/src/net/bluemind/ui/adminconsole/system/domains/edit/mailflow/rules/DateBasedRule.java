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

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.i18n.client.DateTimeFormat.PredefinedFormat;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.datepicker.client.DateBox;

import net.bluemind.mailflow.api.MailRuleDescriptor;
import net.bluemind.mailflow.api.MailflowRule;
import net.bluemind.ui.adminconsole.system.domains.edit.mailflow.RuleAssignmentWidget;

public class DateBasedRule extends RuleTreeItem {

	Panel disclaimerConfig = new FlowPanel();
	private Grid tbl;
	private DateBox date = new DateBox();

	public DateBasedRule(RuleAssignmentWidget parent, MailRuleDescriptor descriptor,
			List<MailRuleDescriptor> ruleIdentifiers, Panel config, String domainUid) {
		super(parent, descriptor, config, domainUid);
		date.setFormat(new DateBox.DefaultFormat(DateTimeFormat.getFormat(PredefinedFormat.DATE_LONG)));
		tbl = new Grid(1, 1);
		tbl.setCellPadding(10);
		tbl.setWidget(0, 0, date);
		disclaimerConfig.add(tbl);
		addListener();
	}

	private void addListener() {
		ClickHandler clickHandler = createClickHandler();
		super.getWidget().addDomHandler(clickHandler, ClickEvent.getType());
	}

	private ClickHandler createClickHandler() {
		return (c -> {
			config.forEach(w -> config.remove(w));
			config.add(disclaimerConfig);
		});
	}

	@Override
	public MailflowRule toRule() {
		if (date.getValue() == null) {
			return null;
		}
		MailflowRule rule = new MailflowRule();
		rule.ruleIdentifier = super.ruleIdentifier;
		rule.configuration = new HashMap<>();
		Long timestamp = date.getValue().getTime();
		rule.configuration.put("timestamp", timestamp.toString());
		return rule;
	}

	@Override
	public void set(Map<String, String> configuration) {
		String timestamp = configuration.get("timestamp");
		if (null != timestamp) {
			try {
				Long time = Long.parseLong(timestamp);
				date.setValue(new Date(time));
			} catch (NumberFormatException e) {
			}
		}
	}

}
