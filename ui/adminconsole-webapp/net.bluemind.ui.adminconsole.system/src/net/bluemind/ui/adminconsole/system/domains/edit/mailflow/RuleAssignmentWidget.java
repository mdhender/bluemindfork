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
package net.bluemind.ui.adminconsole.system.domains.edit.mailflow;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Tree;

import net.bluemind.mailflow.api.ExecutionMode;
import net.bluemind.mailflow.api.MailActionDescriptor;
import net.bluemind.mailflow.api.MailRuleActionAssignment;
import net.bluemind.mailflow.api.MailRuleDescriptor;
import net.bluemind.mailflow.api.MailflowRouting;
import net.bluemind.mailflow.api.MailflowRule;
import net.bluemind.ui.adminconsole.system.domains.edit.mailflow.actions.MailflowActionConfig;
import net.bluemind.ui.adminconsole.system.domains.edit.mailflow.exceptions.MailflowException;
import net.bluemind.ui.adminconsole.system.domains.edit.mailflow.rules.RuleTreeItem;
import net.bluemind.ui.adminconsole.system.domains.l10n.DomainConstants;

public class RuleAssignmentWidget extends Composite {

	private static final DomainConstants TEXTS = GWT.create(DomainConstants.class);

	Button removeAssignment = new Button(TEXTS.removeAssignment());
	Button addRule = new Button(TEXTS.addRule());
	Button addAction = new Button(TEXTS.addAction());
	Button removeAction = new Button(TEXTS.removeAction());
	private String uid;
	private Tree tree;
	private TextArea description = new TextArea();
	private TextBox group = new TextBox();
	private ListBox executionMode = new ListBox();
	private ListBox routing = new ListBox();
	private FlowPanel ruleConfig = new FlowPanel();
	private FlowPanel actionConfig = new FlowPanel();
	private Label actionTitle = new Label();
	private CheckBox isActive = new CheckBox();
	private MailflowActionConfig action;
	private ListBox positionSelect;
	private String domainUid;

	public static interface Resources extends ClientBundle {

		@Source("RuleAssignmentWidget.css")
		Style editStyle();

	}

	public static interface Style extends CssResource {

		String tree();

		String widget();

		String grid();

		String rowBottom();

		String description();

		String descriptionArea();

		String ruleConfig();

		String actionTitle();

	}

	private static final Resources resource = GWT.create(Resources.class);
	private Style style;

	public RuleAssignmentWidget(List<MailRuleDescriptor> ruleIdentifiers, List<MailActionDescriptor> actionIdentifiers,
			String domainUid) {
		this.domainUid = domainUid;
		style = resource.editStyle();
		style.ensureInjected();
		Panel widget = new FlowPanel();

		widget.setStyleName(style.widget());

		Grid grid = new Grid(7, 2);
		grid.setStyleName(style.grid());
		grid.setCellPadding(3);
		for (int i = 0; i < 4; i++) {
			grid.getCellFormatter().setStyleName(i, 0, style.rowBottom());
			grid.getCellFormatter().setStyleName(i, 1, style.rowBottom());
		}
		grid.getCellFormatter().setStyleName(1, 1, style.description());

		description.setStyleName(style.descriptionArea());

		tree = new Tree();
		tree.setStyleName(style.tree());

		ruleConfig.setStyleName(style.ruleConfig());
		actionConfig.setStyleName(style.ruleConfig());

		executionMode.addItem(ExecutionMode.CONTINUE.name());
		executionMode.addItem(ExecutionMode.STOP_AFTER_EXECUTION.name());

		routing.addItem(MailflowRouting.OUTGOING.name());
		routing.addItem(MailflowRouting.INCOMING.name());
		routing.addItem(MailflowRouting.ALL.name());

		removeAssignment.setStyleName("button");
		addRule.setStyleName("button");
		addAction.setStyleName("button");
		removeAction.setStyleName("button");
		actionTitle.setStyleName(style.actionTitle());
		actionTitle.setVisible(false);

		positionSelect = new ListBox();
		for (int i = 1; i < 100; i++) {
			positionSelect.addItem(String.valueOf(i));
		}
		positionSelect.setSelectedIndex(0);

		isActive.setValue(true);

		grid.setWidget(0, 0, removeAssignment);
		grid.setWidget(0, 1, addRule);
		grid.setWidget(1, 0, new Label(TEXTS.description()));
		grid.setWidget(1, 1, description);
		grid.setWidget(2, 0, new Label(TEXTS.isActive()));
		grid.setWidget(2, 1, isActive);
		grid.setWidget(3, 0, new Label(TEXTS.executionMode()));
		grid.setWidget(3, 1, executionMode);
		grid.setWidget(4, 0, new Label(TEXTS.routing()));
		grid.setWidget(4, 1, routing);
		grid.setWidget(5, 0, new Label(TEXTS.position()));
		grid.setWidget(5, 1, positionSelect);
		grid.setWidget(6, 0, new Label(TEXTS.group()));
		grid.setWidget(6, 1, group);
		widget.add(grid);
		widget.add(tree);
		widget.add(ruleConfig);
		widget.add(addAction);
		widget.add(actionTitle);
		widget.add(actionConfig);
		widget.add(removeAction);
		removeAction.setVisible(false);

		addRule.addClickHandler(c -> {
			List<String> ruleIds = ruleIdentifiers.stream().map(r -> r.ruleIdentifier).collect(Collectors.toList());
			RuleActionPopup popup = new RuleActionPopup(ruleIds, ruleIdentifier -> {
				addRule.setVisible(false);
				RuleTreeItem ruleByIdentifier = RuleActionElementFactory.getRuleByIdentifier(this, ruleIdentifier,
						ruleIdentifiers, ruleConfig, domainUid);
				ruleByIdentifier.init().thenRun(() -> tree.addItem(ruleByIdentifier));
			});
			popup.setModal(true);
			popup.setPopupPosition(c.getClientX(), c.getClientY());
			popup.show();
		});

		addAction.addClickHandler(c -> {
			List<String> actionIds = actionIdentifiers.stream().map(a -> a.actionIdentifier)
					.collect(Collectors.toList());
			RuleActionPopup popup = new RuleActionPopup(actionIds, actionIdentifier -> {
				addAction.setVisible(false);
				actionTitle.setVisible(true);
				actionTitle.setText(RuleTexts.resolve(actionIdentifier));
				removeAction.setVisible(true);
				action = RuleActionElementFactory.getActionByIdentifier(actionIdentifier);
				actionConfig.add(action.getWidget());
			});
			popup.setModal(true);
			popup.setPopupPosition(c.getClientX(), c.getClientY());
			popup.show();
		});

		removeAction.addClickHandler(c -> {
			actionConfig.remove(0);
			addAction.setVisible(true);
			actionTitle.setVisible(false);
			actionTitle.setText("");
			removeAction.setVisible(false);
		});

		removeAssignment.addClickHandler(c -> {
			this.removeFromParent();
		});

		super.initWidget(widget);
	}

	public RuleAssignmentWidget(List<MailRuleDescriptor> ruleIdentifiers, List<MailActionDescriptor> actionIdentifiers,
			MailRuleActionAssignment assignment, String domainUid) {
		this(ruleIdentifiers, actionIdentifiers, domainUid);
		this.uid = assignment.uid;
		addRule.setVisible(false);
		addAction.setVisible(false);
		actionTitle.setVisible(true);
		actionTitle.setText(RuleTexts.resolve(assignment.actionIdentifier));
		removeAction.setVisible(true);
		positionSelect.setSelectedIndex(Math.max(0, assignment.position - 1));
		if (assignment.mode == ExecutionMode.STOP_AFTER_EXECUTION) {
			executionMode.setSelectedIndex(1);
		}

		if (assignment.routing == MailflowRouting.INCOMING) {
			routing.setSelectedIndex(1);
		} else if (assignment.routing == MailflowRouting.ALL) {
			routing.setSelectedIndex(2);
		}

		description.setText(assignment.description);
		isActive.setValue(assignment.isActive);

		group.setText(assignment.group);

		buildRuleTree(assignment.rules, ruleIdentifiers);
		buildActionField(assignment.actionIdentifier, assignment.actionConfiguration);

	}

	private void buildActionField(String actionIdentifier, Map<String, String> actionConfiguration) {
		action = RuleActionElementFactory.getActionByIdentifier(actionIdentifier);
		action.set(actionConfiguration);
		actionConfig.add(action.getWidget());
	}

	private void buildRuleTree(MailflowRule rules, List<MailRuleDescriptor> ruleIdentifiers) {
		buildRuleTree(null, rules, ruleIdentifiers);
	}

	private void buildRuleTree(RuleTreeItem parent, MailflowRule rules, List<MailRuleDescriptor> ruleIdentifiers) {
		RuleTreeItem rule = RuleActionElementFactory.getRuleByIdentifier(this, rules.ruleIdentifier, ruleIdentifiers,
				ruleConfig, domainUid);
		rule.set(rules.configuration);
		if (null == parent) {
			tree.addItem(rule);
		} else {
			parent.addItem(rule);
		}
		for (MailflowRule child : rules.children) {
			buildRuleTree(rule, child, ruleIdentifiers);
		}
	}

	public void treeReset() {
		addRule.setVisible(true);
	}

	public String getDescription() {
		return this.description.getText();
	}

	public Optional<MailRuleActionAssignment> get() throws MailflowException {
		MailRuleActionAssignment assignment;
		try {
			assignment = new MailRuleActionAssignment();
			assignment.description = description.getText().trim();
			assignment.isActive = isActive.getValue();
			assignment.group = group.getText().trim();
			assignment.mode = executionMode.getSelectedIndex() == 0 ? ExecutionMode.CONTINUE
					: ExecutionMode.STOP_AFTER_EXECUTION;

			if (routing.getSelectedIndex() == 1) {
				assignment.routing = MailflowRouting.INCOMING;
			} else if (routing.getSelectedIndex() == 2) {
				assignment.routing = MailflowRouting.ALL;
			} else {
				assignment.routing = MailflowRouting.OUTGOING;
			}

			assignment.rules = ((RuleTreeItem) tree.getItem(0)).toRule();
			assignment.actionIdentifier = action.getIdentifier();
			assignment.actionConfiguration = action.get();
			assignment.position = Integer.parseInt(positionSelect.getSelectedItemText());
			assignment.uid = uid;
		} catch (MailflowException e) {
			e.uid = uid;
			throw e;
		} catch (Exception e) {
			return Optional.empty();
		}
		return Optional.of(assignment);
	}

}
