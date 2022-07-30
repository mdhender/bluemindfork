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
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.HTMLPanel;

import net.bluemind.core.commons.gwt.JsMapStringJsObject;
import net.bluemind.gwtconsoleapp.base.editor.WidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.CompositeGwtWidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.GwtWidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtDelegateFactory;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtWidgetElement;
import net.bluemind.gwtconsoleapp.base.notification.Notification;
import net.bluemind.mailflow.api.IMailflowRulesPromise;
import net.bluemind.mailflow.api.MailActionDescriptor;
import net.bluemind.mailflow.api.MailRuleActionAssignment;
import net.bluemind.mailflow.api.MailRuleDescriptor;
import net.bluemind.mailflow.api.gwt.endpoint.MailflowRulesGwtEndpoint;
import net.bluemind.ui.adminconsole.system.domains.DomainKeys;
import net.bluemind.ui.adminconsole.system.domains.edit.mailflow.exceptions.MailflowException;
import net.bluemind.ui.adminconsole.system.domains.edit.mailflow.rules.SenderInGroupRule;
import net.bluemind.ui.common.client.forms.Ajax;

public class EditMailflowRulesEditor extends CompositeGwtWidgetElement {

	public static final String TYPE = "bm.ac.EditMailflowRulesEditor";

	private static EditMailflowRulesEditorUiBinder uiBinder = GWT.create(EditMailflowRulesEditorUiBinder.class);

	interface EditMailflowRulesEditorUiBinder extends UiBinder<HTMLPanel, EditMailflowRulesEditor> {
	}

	@UiField
	Button addRuleAssignment;

	@UiField
	HTMLPanel ruleTable;

	protected EditMailflowRulesEditor() {
		HTMLPanel panel = uiBinder.createAndBindUi(this);
		initWidget(panel);
	}

	public static void registerType() {
		GwtWidgetElement.register(TYPE, new IGwtDelegateFactory<IGwtWidgetElement, WidgetElement>() {

			@Override
			public IGwtWidgetElement create(WidgetElement e) {
				return new EditMailflowRulesEditor();
			}
		});
	}

	@Override
	public void loadModel(JavaScriptObject model) {
		ruleTable.clear();

		final JsMapStringJsObject map = model.cast();
		final String domainUid = map.getString(DomainKeys.domainUid.name());
		final String sessionId = Ajax.TOKEN.getSessionId();

		addRuleAssignment.setStyleName("button");
		SenderInGroupRule.loadGroups(sessionId, domainUid)
				.thenRun(() -> initRuleAssignmentWidgets(sessionId, domainUid));
	}

	private void initRuleAssignmentWidgets(String sessionId, String domainUid) {
		IMailflowRulesPromise ruleService = new MailflowRulesGwtEndpoint(sessionId, domainUid).promiseApi();
		CompletableFuture<List<MailRuleDescriptor>> listRules = ruleService.listRules();
		CompletableFuture<List<MailActionDescriptor>> listActions = ruleService.listActions();
		CompletableFuture<List<MailRuleActionAssignment>> listAssignments = ruleService.listAssignments();

		listRules.thenAccept(ruleIdentifiers -> {
			listActions.thenAccept(actionIdentifiers -> {
				listAssignments.thenAccept(assignments -> {
					assignments.stream().sorted((a, b) -> Integer.compare(a.position, b.position))
							.forEach(assignment -> ruleTable.add(new RuleAssignmentWidget(ruleIdentifiers,
									actionIdentifiers, assignment, domainUid)));
					addRuleAssignment.addClickHandler(c -> ruleTable
							.add(new RuleAssignmentWidget(ruleIdentifiers, actionIdentifiers, domainUid)));
				});
			});
		});
	}

	@Override
	public void saveModel(JavaScriptObject model) {
		final JsMapStringJsObject map = model.cast();
		final String domainUid = map.getString(DomainKeys.domainUid.name());

		IMailflowRulesPromise ruleService = new MailflowRulesGwtEndpoint(Ajax.TOKEN.getSessionId(), domainUid)
				.promiseApi();

		ruleService.listAssignments().thenAccept(assignments -> {
			List<String> uids = assignments.stream().map(a -> a.uid).collect(Collectors.toList());

			int widgetCount = ruleTable.getWidgetCount();
			for (int i = 0; i < widgetCount; i++) {
				try {
					RuleAssignmentWidget widget = (RuleAssignmentWidget) ruleTable.getWidget(i);
					Optional<MailRuleActionAssignment> ruleAssignment = widget.get();
					ruleAssignment.ifPresent(assignment -> {
						if (assignment.uid == null) {
							String uid = net.bluemind.ui.common.client.forms.tag.UUID.uuid();
							ruleService.create(uid, assignment);
						} else {
							uids.remove(assignment.uid);
							ruleService.update(assignment.uid, assignment);
						}
					});
				} catch (MailflowException e) {
					Notification.get().reportError(e.getMessage());
					if (e.uid != null) {
						uids.remove(e.uid);
					}
				}
			}
			uids.forEach(ruleService::delete);
		});
	}
}
