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

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ContextMenuEvent;
import com.google.gwt.event.dom.client.ContextMenuHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.TreeItem;

import net.bluemind.mailflow.api.MailRuleDescriptor;
import net.bluemind.mailflow.api.MailflowRule;
import net.bluemind.ui.adminconsole.system.domains.edit.mailflow.RuleAssignmentWidget;
import net.bluemind.ui.adminconsole.system.domains.edit.mailflow.RuleTexts;
import net.bluemind.ui.adminconsole.system.domains.l10n.DomainConstants;

public abstract class RuleTreeItem extends TreeItem implements ContextMenuHandler {

	protected final String ruleIdentifier;
	protected final Panel config;
	protected final RuleAssignmentWidget parent;
	protected final String domainUid;

	public abstract MailflowRule toRule();

	public abstract void set(Map<String, String> configuration);

	public CompletableFuture<Void> init() {
		return CompletableFuture.completedFuture(null);
	}

	private static final DomainConstants TEXTS = GWT.create(DomainConstants.class);

	public RuleTreeItem(RuleAssignmentWidget parent, MailRuleDescriptor descriptor, Panel config, String domainUid) {
		super(new Label(RuleTexts.resolve(descriptor.ruleIdentifier)));
		this.domainUid = domainUid;
		this.parent = parent;
		this.ruleIdentifier = descriptor.ruleIdentifier;
		this.config = config;
		getWidget().addDomHandler(this, ContextMenuEvent.getType());
	}

	@Override
	public void onContextMenu(ContextMenuEvent event) {
		event.preventDefault();
		event.stopPropagation();
		PopupPanel popup = new PopupPanel(true);
		Button deleteButton = new Button(TEXTS.delete());
		deleteButton.setStyleName("button");
		deleteButton.addClickHandler(c -> {
			config.forEach(w -> config.remove(w));
			if (null != getParentItem()) {
				getParentItem().removeItem(this);
			} else {
				getTree().removeItems();
				parent.treeReset();
			}
			popup.clear();
			popup.setVisible(false);
		});
		popup.setWidget(deleteButton);
		popup.setModal(true);
		popup.setPopupPosition(event.getNativeEvent().getClientX(), event.getNativeEvent().getClientY());
		popup.show();
	}

}
