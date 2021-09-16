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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.Panel;

import net.bluemind.core.container.model.ItemValue;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.IDirectoryPromise;
import net.bluemind.directory.api.gwt.endpoint.DirectoryGwtEndpoint;
import net.bluemind.group.api.IGroupPromise;
import net.bluemind.group.api.gwt.endpoint.GroupGwtEndpoint;
import net.bluemind.gwtconsoleapp.base.notification.Notification;
import net.bluemind.mailflow.api.MailRuleDescriptor;
import net.bluemind.mailflow.api.MailflowRule;
import net.bluemind.ui.adminconsole.system.domains.edit.mailflow.RuleAssignmentWidget;

public class SenderInGroupRule extends RuleTreeItem {

	Panel disclaimerConfig = new FlowPanel();
	private Grid tbl;
	private ListBox selectedValue = new ListBox();
	private String assignmentDesc = "";
	private static List<ItemValue<DirEntry>> groups;

	public SenderInGroupRule(RuleAssignmentWidget parent, MailRuleDescriptor descriptor, Panel config,
			String domainUid) {
		super(parent, descriptor, config, domainUid);
		this.assignmentDesc = parent.getDescription();
		selectedValue.getElement().setAttribute("style", "width: 200px");
		tbl = new Grid(1, 1);
		tbl.setCellPadding(10);
		tbl.setWidget(0, 0, selectedValue);
		disclaimerConfig.add(tbl);
		addListener();
		fillGroupListBox();
	}

	public static CompletableFuture<Void> loadGroups(String sessionId, String domainUid) {
		IGroupPromise groupPromise = new GroupGwtEndpoint(sessionId, domainUid).promiseApi();
		return groupPromise.allUids().thenCompose(list -> {
			IDirectoryPromise directoryPromise = new DirectoryGwtEndpoint(sessionId, domainUid).promiseApi();
			return directoryPromise.getMultiple(list);
		}).thenCompose(list -> {
			SenderInGroupRule.groups = list;
			return CompletableFuture.completedFuture(null);
		});
	}

	private void fillGroupListBox() {
		resetFields();
		SenderInGroupRule.groups.forEach(group -> selectedValue.addItem(group.value.displayName, group.uid));
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
			config.forEach(config::remove);
			config.add(disclaimerConfig);
		});
	}

	@Override
	public MailflowRule toRule() {
		if (selectedValue.getSelectedIndex() == -1) {
			return null;
		}
		MailflowRule rule = new MailflowRule();
		rule.ruleIdentifier = super.ruleIdentifier;
		rule.configuration = new HashMap<>();
		String selectedItemText = ((ListBox) tbl.getWidget(0, 0)).getSelectedItemText();
		String value = ((ListBox) tbl.getWidget(0, 0)).getSelectedValue();
		rule.configuration.put("groupUid", value);
		rule.configuration.put("groupName", selectedItemText);
		return rule;
	}

	@Override
	public void set(Map<String, String> configuration) {
		String group = configuration.get("groupUid");
		String name = configuration.get("groupName");
		if (null != group) {
			fillGroupListBox();
			boolean hasFoundSelected = false;
			for (int i = 0; i < selectedValue.getItemCount(); i++) {
				if (selectedValue.getItemText(i).equals(name)) {
					selectedValue.setSelectedIndex(i);
					hasFoundSelected = true;
					break;
				}
			}
			if (!hasFoundSelected) {
				selectedValue.addItem(name, group);
				selectedValue.setSelectedIndex(selectedValue.getItemCount() - 1);
				// throw an error if group has been deleted and still referenced in a mailflow
				// rule
				String errorMsg = "A mailflow rule contains a deleted group, you need to update it (assignment: "
						+ assignmentDesc + " , deleted group: " + name + ")";
				Notification.get().reportError(errorMsg);
			}
		}
	}

}
