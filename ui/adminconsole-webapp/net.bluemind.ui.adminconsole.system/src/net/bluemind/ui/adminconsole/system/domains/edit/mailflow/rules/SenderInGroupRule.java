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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.Panel;

import net.bluemind.group.api.IGroupPromise;
import net.bluemind.group.api.gwt.endpoint.GroupGwtEndpoint;
import net.bluemind.gwtconsoleapp.base.editor.Ajax;
import net.bluemind.mailflow.api.MailRuleDescriptor;
import net.bluemind.mailflow.api.MailflowRule;
import net.bluemind.ui.adminconsole.system.domains.edit.mailflow.RuleAssignmentWidget;

public class SenderInGroupRule extends RuleTreeItem {

	Panel disclaimerConfig = new FlowPanel();
	private Grid tbl;
	private ListBox selectedValue = new ListBox();
	private Map<String, String> groupMapping = new HashMap<>();
	private Map<String, String> groupReverseMapping = new HashMap<>();

	@Override
	public CompletableFuture<Void> init() {
		return CompletableFuture.runAsync(() -> fillGroupListBox(domainUid));
	}

	public SenderInGroupRule(RuleAssignmentWidget parent, MailRuleDescriptor descriptor,
			List<MailRuleDescriptor> ruleIdentifiers, Panel config, String domainUid) {
		super(parent, descriptor, config, domainUid);
		selectedValue.getElement().setAttribute("style", "width: 200px");
		tbl = new Grid(1, 1);
		tbl.setCellPadding(10);
		tbl.setWidget(0, 0, selectedValue);
		disclaimerConfig.add(tbl);
		addListener();
	}

	private CompletableFuture<Void> fillGroupListBox(String domainUid) {
		CompletableFuture<Void> future = new CompletableFuture<>();
		resetFields();
		IGroupPromise groupPromise = new GroupGwtEndpoint(Ajax.TOKEN.getSessionId(), domainUid).promiseApi();
		groupPromise.allUids().thenAccept(list -> {
			List<CompletableFuture<Void>> futures = new ArrayList<>();
			for (String group : list) {
				CompletableFuture<Void> subTask = new CompletableFuture<>();
				futures.add(subTask);
				groupPromise.getComplete(group).thenAccept(complete -> {
					groupMapping.put(complete.uid, complete.value.name);
					groupReverseMapping.put(complete.value.name, complete.uid);
					selectedValue.addItem(complete.value.name);
					subTask.complete(null);
				});
			}
			CompletableFuture.allOf(futures.toArray(new CompletableFuture[futures.size()]))
					.thenRun(() -> future.complete(null));
		});
		return future;
	}

	private void resetFields() {
		while (selectedValue.getItemCount() > 0) {
			selectedValue.removeItem(0);
		}
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
		if (selectedValue.getSelectedIndex() == -1 || groupMapping.isEmpty()) {
			return null;
		}
		MailflowRule rule = new MailflowRule();
		rule.ruleIdentifier = super.ruleIdentifier;
		rule.configuration = new HashMap<>();
		String selectedItemText = ((ListBox) tbl.getWidget(0, 0)).getSelectedItemText();
		String value = groupReverseMapping.get(selectedItemText);
		rule.configuration.put("groupUid", value);
		rule.configuration.put("groupName", selectedItemText);
		return rule;
	}

	@Override
	public void set(Map<String, String> configuration) {
		String group = configuration.get("groupUid");
		if (null != group) {
			fillGroupListBox(domainUid).thenRun(() -> {
				String name = groupMapping.get(group);
				for (Entry<String, String> a : groupMapping.entrySet()) {
					GWT.log(a.getKey() + " --> " + a.getValue());
				}
				for (int i = 0; i < selectedValue.getItemCount(); i++) {
					if (selectedValue.getItemText(i).equals(name)) {
						selectedValue.setSelectedIndex(i);
						break;
					}
				}
			});
		}
	}

}
