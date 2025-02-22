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
package net.bluemind.ui.mailbox.filter;

import java.util.stream.Collectors;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Label;

import net.bluemind.core.api.AsyncHandler;
import net.bluemind.core.commons.gwt.JsMapStringJsObject;
import net.bluemind.gwtconsoleapp.base.editor.Ajax;
import net.bluemind.gwtconsoleapp.base.editor.WidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.CompositeGwtWidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.GwtWidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtDelegateFactory;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtWidgetElement;
import net.bluemind.mailbox.api.IMailboxesAsync;
import net.bluemind.mailbox.api.MailFilter;
import net.bluemind.mailbox.api.gwt.endpoint.MailboxesGwtEndpoint;
import net.bluemind.mailbox.api.rules.MailFilterRule;
import net.bluemind.ui.mailbox.filter.SieveEdit.Resources;
import net.bluemind.ui.mailbox.filter.SieveEdit.Style;

public class DomainFilters extends CompositeGwtWidgetElement {

	public static final String TYPE = "bm.mailbox.DomainMailFilters";

	static {
		registerType();
	}

	public static void registerType() {
		GwtWidgetElement.register(TYPE, new IGwtDelegateFactory<IGwtWidgetElement, WidgetElement>() {

			@Override
			public IGwtWidgetElement create(WidgetElement e) {
				return new DomainFilters();
			}
		});

	}

	private static final SieveConstants constants = GWT.create(SieveConstants.class);

	interface DomainFiltersUiBinder extends UiBinder<HTMLPanel, DomainFilters> {
	}

	private static DomainFiltersUiBinder uiBinder = GWT.create(DomainFiltersUiBinder.class);
	private static final Resources res = GWT.create(Resources.class);
	private final Style s;

	@UiField
	FlexTable filters;

	private DomainFilters() {
		s = res.editStyle();
		s.ensureInjected();
		initWidget(uiBinder.createAndBindUi(this));

		filters.setWidget(0, 0, new Label(constants.criteria()));
		filters.setWidget(0, 1, new Label(constants.action()));
		filters.setWidget(0, 2, new Label(constants.active()));

		filters.setStyleName(s.filters());
		filters.getRowFormatter().setStyleName(0, s.headers());
		filters.getCellFormatter().setStyleName(0, 0, s.criteria());
		filters.getCellFormatter().setStyleName(0, 1, s.action());
		filters.getCellFormatter().setStyleName(0, 2, s.columnActive());
	}

	@Override
	public void loadModel(JavaScriptObject model) {

		final JsMapStringJsObject map = model.cast();

		IMailboxesAsync mboxes = new MailboxesGwtEndpoint(Ajax.TOKEN.getSessionId(), map.getString("domainUid"));
		mboxes.getDomainFilter(new AsyncHandler<MailFilter>() {

			@Override
			public void success(MailFilter value) {
				loadFilter(value);
			}

			@Override
			public void failure(Throwable e) {

			}

		});
	}

	protected void loadFilter(MailFilter value) {
		filters.removeAllRows();
		value.rules.forEach(this::addFilter);
	}

	private void addFilter(MailFilterRule rule) {
		MailFilterRule copy = MailFilterRule.copy(rule);
		int row = filters.getRowCount();
		createSieveFilterRow(copy, row);
	}

	private void createSieveFilterRow(final MailFilterRule rule, int row) {
		final MailFilterRule sf = rule;

		SieveCriteriaLabelBuilder criteriaLabelBuilder = new SieveCriteriaLabelBuilder(s, constants);
		FlexTable criteria = criteriaLabelBuilder.buildCriteria(rule);

		FlowPanel actionsFP = new FlowPanel();
		rule.markAsRead().ifPresent(markAsRead -> actionsFP.add(new Label(constants.markAsRead())));
		rule.markAsImportant().ifPresent(markAsImportant -> actionsFP.add(new Label(constants.markAsImportant())));
		rule.discard().ifPresent(discard -> actionsFP.add(new Label(constants.discard())));
		rule.move().ifPresent(move -> {
			String target = move.folder().toLowerCase();
			if (target.equalsIgnoreCase("inbox")) {
				target = constants.inbox();
			} else if (target.equalsIgnoreCase("sent")) {
				target = constants.sent();
			} else if (target.equalsIgnoreCase("trash")) {
				target = constants.trash();
			} else if (target.equalsIgnoreCase("junk")) {
				target = constants.spam();
			} else if (target.equalsIgnoreCase("drafts")) {
				target = constants.drafts();
			}

			actionsFP.add(new Label(constants.moveTo() + ": " + target));
		});
		sf.redirect().ifPresent(redirect -> {
			String forwardTo = redirect.emails().stream().collect(Collectors.joining(", "));
			String l = constants.forwardTo() + ": " + forwardTo;
			if (redirect.keepCopy()) {
				l += " (" + constants.forwardToWithLocalCopy() + ")";
			}
			actionsFP.add(new Label(l));
		});

		filters.setWidget(row, 0, criteria);
		filters.setWidget(row, 1, actionsFP);

		CheckBox activeCb = new CheckBox();
		activeCb.setValue(rule.active);
		activeCb.setEnabled(false);
		activeCb.addValueChangeHandler(new ValueChangeHandler<Boolean>() {

			@Override
			public void onValueChange(ValueChangeEvent<Boolean> event) {
				rule.active = event.getValue();
			}
		});
		filters.setWidget(row, 2, activeCb);
		filters.getRowFormatter().setStyleName(row, s.filter());
	}
}
