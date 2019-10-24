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

import net.bluemind.core.container.model.ItemValue;
import net.bluemind.directory.api.BaseDirEntry;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.DirEntryQuery;
import net.bluemind.directory.api.IDirectoryPromise;
import net.bluemind.directory.api.gwt.endpoint.DirectoryGwtEndpoint;
import net.bluemind.mailflow.api.MailRuleDescriptor;
import net.bluemind.mailflow.api.MailflowRule;
import net.bluemind.ui.adminconsole.system.domains.edit.mailflow.RuleAssignmentWidget;
import net.bluemind.ui.common.client.forms.Ajax;

public class SenderIsRule extends RuleTreeItem {

	Panel disclaimerConfig = new FlowPanel();
	private Grid tbl;
	private ListBox selectedValue = new ListBox();
	private Map<String, String> dirEntryMapping = new HashMap<>();

	public SenderIsRule(RuleAssignmentWidget parent, MailRuleDescriptor descriptor,
			List<MailRuleDescriptor> ruleIdentifiers, Panel config, String domainUid) {
		super(parent, descriptor, config, domainUid);
		selectedValue.getElement().setAttribute("style", "width: 200px");
		tbl = new Grid(2, 2);
		tbl.setCellPadding(10);
		tbl.setWidget(0, 0, new Label("Directory entry"));
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

		IDirectoryPromise directoryService = new DirectoryGwtEndpoint(Ajax.TOKEN.getSessionId(), domainUid)
				.promiseApi();

		String searchedValue = ((TextBox) tbl.getWidget(0, 1)).getValue();

		DirEntryQuery query = new DirEntryQuery();
		query.kindsFilter = this.supportedDirEntryKinds();
		query.nameOrEmailFilter = searchedValue;
		query.size = 30;

		directoryService.search(query).thenAccept(result -> {
			for (ItemValue<DirEntry> entry : result.values) {
				selectedValue.addItem(entry.value.email);
				dirEntryMapping.put(entry.value.email, entry.uid);
			}
		}).exceptionally(t -> {
			GWT.log("error " + t.getMessage());
			return null;
		});
		;
	}

	private List<BaseDirEntry.Kind> supportedDirEntryKinds() {
		return Arrays.asList(BaseDirEntry.Kind.USER, BaseDirEntry.Kind.RESOURCE, BaseDirEntry.Kind.MAILSHARE,
				BaseDirEntry.Kind.EXTERNALUSER, BaseDirEntry.Kind.GROUP);
	}

	private void resetFields() {
		while (selectedValue.getItemCount() > 0) {
			selectedValue.removeItem(0);
		}
		dirEntryMapping.clear();
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
		if (dirEntryMapping.isEmpty()) {
			return null;
		}
		MailflowRule rule = new MailflowRule();
		rule.ruleIdentifier = super.ruleIdentifier;
		rule.configuration = new HashMap<>();
		String selectedItemText = ((ListBox) tbl.getWidget(1, 1)).getSelectedItemText();
		rule.configuration.put("dirEntryUid", dirEntryMapping.get(selectedItemText));
		rule.configuration.put("dirEntryEmail", selectedItemText);
		return rule;
	}

	@Override
	public void set(Map<String, String> configuration) {
		resetFields();
		String dirEntryUid = configuration.get("dirEntryUid");
		if (dirEntryUid != null) {
			String dirEntryEmail = configuration.get("dirEntryEmail");
			selectedValue.addItem(dirEntryEmail);
			dirEntryMapping.put(dirEntryEmail, dirEntryUid);
		}
	}

}
