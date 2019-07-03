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

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.TextBox;

import net.bluemind.directory.api.BaseDirEntry.Kind;
import net.bluemind.directory.api.IOrgUnitsPromise;
import net.bluemind.directory.api.OrgUnitPath;
import net.bluemind.directory.api.OrgUnitQuery;
import net.bluemind.directory.api.gwt.endpoint.OrgUnitsGwtEndpoint;
import net.bluemind.mailflow.api.MailRuleDescriptor;
import net.bluemind.mailflow.api.MailflowRule;
import net.bluemind.ui.adminconsole.system.domains.edit.mailflow.RuleAssignmentWidget;
import net.bluemind.ui.common.client.forms.Ajax;

public class SenderInOuRule extends RuleTreeItem {

	Panel disclaimerConfig = new FlowPanel();
	private Grid tbl;
	private ListBox selectedValue = new ListBox();
	private Map<String, String> ouMapping = new HashMap<>();

	public SenderInOuRule(RuleAssignmentWidget parent, MailRuleDescriptor descriptor,
			List<MailRuleDescriptor> ruleIdentifiers, Panel config, String domainUid) {
		super(parent, descriptor, config, domainUid);
		selectedValue.getElement().setAttribute("style", "width: 200px");
		tbl = new Grid(2, 2);
		tbl.setCellPadding(10);
		tbl.setWidget(0, 0, new Label("OU"));
		TextBox widget = new TextBox();
		tbl.setWidget(0, 1, widget);
		tbl.setWidget(1, 1, selectedValue);

		disclaimerConfig.add(tbl);
		widget.addKeyPressHandler((e) -> {
			if (e.getNativeEvent().getKeyCode() == KeyCodes.KEY_ENTER) {
				find();
			}
		});
		addListener();
	}

	private void find() {
		resetFields();

		IOrgUnitsPromise units = new OrgUnitsGwtEndpoint(Ajax.TOKEN.getSessionId(), domainUid).promiseApi();
		OrgUnitQuery q = new OrgUnitQuery();
		q.query = ((TextBox) tbl.getWidget(0, 1)).getValue();
		q.managableKinds = new HashSet<>(Arrays.asList(Kind.ORG_UNIT));
		units.search(q).thenAccept(result -> {
			for (OrgUnitPath res : result) {
				String ou = res.toString();
				selectedValue.addItem(ou);
				ouMapping.put(ou, res.uid);
			}
		}).exceptionally(t -> {
			GWT.log("error " + t.getMessage());
			return null;
		});
	}

	private void resetFields() {
		while (selectedValue.getItemCount() > 0) {
			selectedValue.removeItem(0);
		}
		ouMapping.clear();
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
		if (ouMapping.isEmpty()) {
			return null;
		}
		MailflowRule rule = new MailflowRule();
		rule.ruleIdentifier = super.ruleIdentifier;
		rule.configuration = new HashMap<>();
		String selectedItemText = ((ListBox) tbl.getWidget(1, 1)).getSelectedItemText();
		rule.configuration.put("orgUnitUid", ouMapping.get(selectedItemText));
		rule.configuration.put("orgUnitName", selectedItemText);
		return rule;
	}

	@Override
	public void set(Map<String, String> configuration) {
		resetFields();
		String ou = configuration.get("orgUnitUid");
		if (null != ou) {
			IOrgUnitsPromise units = new OrgUnitsGwtEndpoint(Ajax.TOKEN.getSessionId(), domainUid).promiseApi();
			units.getPath(ou).thenAccept(ouPath -> {
				String ouValue = ouPath.toString();
				selectedValue.addItem(ouValue);
				ouMapping.put(ouValue, ouPath.uid);
			});
		}
	}

}
