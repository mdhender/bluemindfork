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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.dom.client.Style.Cursor;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.HTMLTable.Cell;
import com.google.gwt.user.client.ui.Label;

import net.bluemind.core.commons.gwt.GwtSerDerUtils;
import net.bluemind.core.commons.gwt.JsMapStringJsObject;
import net.bluemind.gwtconsoleapp.base.editor.Ajax;
import net.bluemind.gwtconsoleapp.base.editor.WidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.CompositeGwtWidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.GwtWidgetElement;
import net.bluemind.mailbox.api.MailFilter;
import net.bluemind.mailbox.api.rules.MailFilterRule;
import net.bluemind.mailbox.api.rules.gwt.js.JsMailFilterRule;
import net.bluemind.mailbox.api.rules.gwt.serder.MailFilterRuleGwtSerDer;
import net.bluemind.ui.common.client.icon.Trash;

public class SieveEdit extends CompositeGwtWidgetElement {
	public static interface Resources extends ClientBundle {
		@Source("SieveEdit.css")
		Style editStyle();
	}

	public static interface Style extends CssResource {

		String filters();

		String headers();

		String filter();

		String criteria();

		String filterCriteria();

		String label();

		String value();

		String action();

		String columnActive();

		String columnUpdate();

		String columnTrash();

		String columnMove();

	}

	interface SieveUiBinder extends UiBinder<HTMLPanel, SieveEdit> {
	}

	private static SieveUiBinder uiBinder = GWT.create(SieveUiBinder.class);
	private static final SieveConstants constants = GWT.create(SieveConstants.class);
	private static final Resources res = GWT.create(Resources.class);
	private final Style s;
	private List<MailFilterRule> sieveFilters;
	private List<MailFilterRule> loadedFilters;

	@UiField
	Button addFilter;

	@UiField
	FlexTable filters;

	@UiField
	Label noFilter;

	private int entityId;
	private String mbox;
	private String domainUid;
	private String datalocation;
	private String entityType;

	public SieveEdit() {
		super();
		sieveFilters = new ArrayList<>();
		setLoadedFilters(new ArrayList<>());
		initWidget(uiBinder.createAndBindUi(this));

		filters.setVisible(false);
		noFilter.setVisible(false);

		s = res.editStyle();
		s.ensureInjected();

		filters.setWidget(0, 0, new Label(constants.criteria()));
		filters.setWidget(0, 1, new Label(constants.action()));
		filters.setWidget(0, 2, new Label(constants.active()));
		filters.setWidget(0, 3, new Label("")); // update
		filters.setWidget(0, 4, new Label("")); // move up and down
		filters.setWidget(0, 5, new Label("")); // trash

		filters.setStyleName(s.filters());
		filters.getRowFormatter().setStyleName(0, s.headers());
		filters.getCellFormatter().setStyleName(0, 0, s.criteria());
		filters.getCellFormatter().setStyleName(0, 1, s.action());
		filters.getCellFormatter().setStyleName(0, 2, s.columnActive());
		filters.getCellFormatter().setStyleName(0, 3, s.columnUpdate());
		filters.getCellFormatter().setStyleName(0, 4, s.columnMove());
		filters.getCellFormatter().setStyleName(0, 5, s.columnTrash());
		setFormHandlers();

		addFilter.getElement().setId("sieve-add-filter");
	}

	protected String getEntity() {
		return entityType;
	}

	private void hasFilter(boolean hasFilter) {
		filters.setVisible(hasFilter);
		noFilter.setVisible(!hasFilter);
	}

	private void setFormHandlers() {
		addFilter.addClickHandler((ClickEvent event) -> SieveRuleEditorDialog.openRuleEditor(domainUid,
				new MailFilterRule(), new SieveRuleEditorDialog.DialogHandler() {

					@Override
					public void validate(final MailFilterRule value) {
						addFilter(value);
					}

					@Override
					public void cancel() {
					}
				}, getEntity(), entityId, mbox, datalocation));

	}

	private void createSieveFilterRow(final MailFilterRule rule, int row) {
		final MailFilterRule sf = rule;
		Trash trash = new Trash();
		trash.setId("sieve-edit-trash-" + row);
		trash.getElement().getStyle().setMarginLeft(0, Unit.PX);
		trash.addClickHandler((ClickEvent event) -> {
			Cell c = filters.getCellForEvent(event);
			filters.removeRow(c.getRowIndex());
			sieveFilters.remove(sf);
			if (filters.getRowCount() < 2) {
				noFilterFound();
			}
		});

		Label editAnchor = new Label();
		editAnchor.setStyleName("fa fa-lg fa-pencil-square-o");
		editAnchor.getElement().getStyle().setCursor(Cursor.POINTER);

		editAnchor.addClickHandler((ClickEvent event) -> {
			Cell c = filters.getCellForEvent(event);
			final int currentIndex = c.getRowIndex() - 1;
			MailFilterRule copy = MailFilterRule.copy(sieveFilters.get(currentIndex));

			SieveRuleEditorDialog.openRuleEditor(domainUid, copy, new SieveRuleEditorDialog.DialogHandler() {

				@Override
				public void validate(MailFilterRule value) {
					sieveFilters.set(currentIndex, value);
					filters.removeRow(currentIndex + 1);
					filters.insertRow(currentIndex + 1);
					createSieveFilterRow(value, currentIndex + 1);
				}

				@Override
				public void cancel() {
				}
			}, getEntity(), entityId, mbox, datalocation);
		});

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

		FlowPanel moveFilterPanel = new FlowPanel();
		Label moveUp = new Label();
		moveUp.setStyleName("fa fa-lg fa-arrow-up");
		moveUp.getElement().getStyle().setCursor(Cursor.POINTER);
		moveUp.getElement().getStyle().setMarginLeft(5, Unit.PX);
		Label moveDown = new Label();
		moveDown.setStyleName("fa fa-lg fa-arrow-down");
		moveDown.getElement().getStyle().setCursor(Cursor.POINTER);
		moveDown.getElement().getStyle().setMarginLeft(5, Unit.PX);
		moveFilterPanel.add(moveDown);
		moveFilterPanel.add(moveUp);

		moveUp.addClickHandler((ClickEvent event) -> {

			Cell c = filters.getCellForEvent(event);
			final int currentIndex = c.getRowIndex() - 1;
			if (currentIndex > 0) {
				MailFilterRule e = sieveFilters.remove(currentIndex);
				sieveFilters.add(currentIndex - 1, e);
				filters.removeRow(currentIndex + 1);
				filters.insertRow(currentIndex);
				createSieveFilterRow(e, currentIndex);
				// FIXME
				// dispatchChange();
			}

		});

		moveDown.addClickHandler((ClickEvent event) -> {

			Cell c = filters.getCellForEvent(event);
			final int currentIndex = c.getRowIndex() - 1;
			if (currentIndex != (sieveFilters.size() - 1)) {
				MailFilterRule e = sieveFilters.remove(currentIndex);
				sieveFilters.add(currentIndex + 1, e);
				filters.removeRow(currentIndex + 1);
				filters.insertRow(currentIndex + 2);
				createSieveFilterRow(e, currentIndex + 2);
			}

		});

		filters.setWidget(row, 0, criteria);
		filters.setWidget(row, 1, actionsFP);

		CheckBox activeCb = new CheckBox();
		activeCb.setValue(rule.active);
		activeCb.addValueChangeHandler((ValueChangeEvent<Boolean> event) -> {
			rule.active = event.getValue(); // FIXME
		});
		filters.setWidget(row, 2, activeCb);
		filters.setWidget(row, 3, editAnchor);
		filters.setWidget(row, 4, moveFilterPanel);
		filters.setWidget(row, 5, trash);

		filters.getRowFormatter().setStyleName(row, s.filter());
	}

	private void addFilter(MailFilterRule rule) {
		hasFilter(true);

		MailFilterRule copy = MailFilterRule.copy(rule);
		int row = filters.getRowCount();
		createSieveFilterRow(copy, row);
		sieveFilters.add(copy);
	}

	private void noFilterFound() {
		filters.setVisible(false);
		noFilter.setVisible(true);
	}

	public List<MailFilterRule> getLoadedFilters() {
		return loadedFilters;
	}

	public void setLoadedFilters(List<MailFilterRule> loadedRules) {
		this.loadedFilters = loadedRules.stream().map(rule -> MailFilterRule.copy(rule)).collect(Collectors.toList());
	}

	public boolean hasChanged() {
		if (loadedFilters.size() != sieveFilters.size())
			return true;

		for (int i = 0; i < loadedFilters.size(); i++) {
			if (!loadedFilters.get(i).equals(sieveFilters.get(i))) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void saveModel(JavaScriptObject m) {
		MailSettingsModel model = MailSettingsModel.get(m);
		if (model.getJsMailFilter() != null) {
			model.getJsMailFilter()
					.setRules(new GwtSerDerUtils.ListSerDer<MailFilterRule>(new MailFilterRuleGwtSerDer())
							.serialize(sieveFilters).isArray().getJavaScriptObject().<JsArray<JsMailFilterRule>>cast());
		}
	}

	@Override
	public void loadModel(JavaScriptObject m) {
		MailSettingsModel model = MailSettingsModel.get(m);

		final JsMapStringJsObject map = model.cast();
		if (null != map.get("userId")) {
			mbox = map.getString("userId");
			this.datalocation = map.getString("datalocation");
			this.entityType = "user";
		} else if (null != map.get("mailshareId")) {
			mbox = map.getString("mailshareId");
			this.datalocation = map.getString("datalocation");
			this.entityType = "mailshare";
		} else {
			mbox = Ajax.TOKEN.getSubject();
			// FIXME dataLocation, entityType
		}

		this.domainUid = map.getString("domainUid");
		sieveFilters.clear();
		MailFilter mf = model.getMailFilter();

		if (mf == null) {
			asWidget().setVisible(false);
		} else {
			asWidget().setVisible(true);
			setLoadedFilters(mf.rules);
			hasFilter(!mf.rules.isEmpty());
			while (filters.getRowCount() > 1) {
				filters.removeRow(1);
			}
			for (MailFilterRule f : mf.rules) {
				addFilter(f);
			}
		}

	}

	public static final String TYPE = "bm.mailbox.MailFiltersEditor";

	public static void registerType() {
		GwtWidgetElement.register(TYPE, (WidgetElement e) -> new SieveEdit());
	}

}
