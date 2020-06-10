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
package net.bluemind.ui.adminconsole.directory.ou;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.view.client.AsyncDataProvider;
import com.google.gwt.view.client.HasData;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SelectionChangeEvent.Handler;

import net.bluemind.directory.api.BaseDirEntry.Kind;
import net.bluemind.directory.api.IOrgUnitsPromise;
import net.bluemind.directory.api.OrgUnitPath;
import net.bluemind.directory.api.OrgUnitQuery;
import net.bluemind.directory.api.gwt.endpoint.OrgUnitsGwtEndpoint;
import net.bluemind.gwtconsoleapp.base.editor.ScreenRoot;
import net.bluemind.gwtconsoleapp.base.editor.gwt.GwtScreenRoot;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtDelegateFactory;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtScreenRoot;
import net.bluemind.gwtconsoleapp.base.notification.Notification;
import net.bluemind.ui.adminconsole.base.Actions;
import net.bluemind.ui.adminconsole.base.DomainsHolder;
import net.bluemind.ui.adminconsole.directory.ou.l10n.OrgUnitConstants;
import net.bluemind.ui.common.client.forms.Ajax;

public class OrgUnitsBrowser extends Composite implements IGwtScreenRoot {

	@UiField
	TextBox search;

	@UiHandler("search")
	void searchOnKeyPress(KeyPressEvent event) {
		if (event.getNativeEvent().getKeyCode() == KeyCodes.KEY_ENTER) {
			find();
		}
	}

	@UiField
	OrgUnitGrid grid;

	@UiField
	Button newButton;

	@UiField
	Button deleteButton;

	@UiHandler("deleteButton")
	void deleteClick(ClickEvent e) {
		Collection<OrgUnitPath> selection = grid.getSelected();
		List<OrgUnitPath> listSelection = new ArrayList<>(selection);

		GWT.log("Selection Size = " + selection.size());

		String confirm = OrgUnitConstants.INST.deleteConfirmation();

		if (selection.size() > 1) {
			confirm = OrgUnitConstants.INST.massDeleteConfirmation();
		}

		if (Window.confirm(confirm)) {
			String domainUid = DomainsHolder.get().getSelectedDomain().uid;

			IOrgUnitsPromise units = new OrgUnitsGwtEndpoint(Ajax.TOKEN.getSessionId(), domainUid).promiseApi();
			CompletableFuture.allOf(listSelection.stream().map(o -> units.delete(o.uid)).toArray(f -> {
				return new CompletableFuture[f];
			})).thenAccept(v -> find()).exceptionally(t -> {
				Notification.get().reportError(t);
				return null;
			});
		}
		grid.clearSelectionModel();
	}

	interface OrgUnitsCenterUiBinder extends UiBinder<DockLayoutPanel, OrgUnitsBrowser> {
	}

	private static OrgUnitsCenterUiBinder uiBinder = GWT.create(OrgUnitsCenterUiBinder.class);
	public static final int PAGE_SIZE = 250;

	public static final String TYPE = "bm.ac.OrgUnitsBrowser";

	private OrgUnitsBrowser(ScreenRoot screenRoot) {

		DockLayoutPanel dlp = uiBinder.createAndBindUi(this);
		dlp.setHeight("100%");
		initWidget(dlp);

		search.getElement().setAttribute("placeholder", OrgUnitConstants.INST.addFilter());

		newButton.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				Actions.get().showWithParams2("qcOrgUnit", null);
				event.stopPropagation();
			}
		});

		Handler handler = new Handler() {
			@Override
			public void onSelectionChange(SelectionChangeEvent event) {
				Collection<OrgUnitPath> units = grid.getSelected();
				deleteButton.setEnabled(!units.isEmpty());
			}
		};
		grid.addSelectionChangeHandler(handler);
	}

	protected void onScreenShown() {
		initTable();
	}

	private void initTable() {
		find();
	}

	private void find() {
		grid.selectAll(false);

		AsyncDataProvider<OrgUnitPath> provider = new AsyncDataProvider<OrgUnitPath>() {

			@Override
			protected void onRangeChanged(HasData<OrgUnitPath> display) {
				String domainUid = DomainsHolder.get().getSelectedDomain().uid;

				IOrgUnitsPromise units = new OrgUnitsGwtEndpoint(Ajax.TOKEN.getSessionId(), domainUid).promiseApi();
				OrgUnitQuery q = new OrgUnitQuery();
				q.query = search.getValue();
				q.managableKinds = new HashSet<>(Arrays.asList(Kind.ORG_UNIT));
				units.search(q).thenAccept(result -> {
					grid.setValues(result);
				}).exceptionally(t -> {
					GWT.log("error " + t.getMessage());
					return null;
				});
			}

		};
		provider.addDataDisplay(grid);
	}

	@Override
	public void attach(Element elt) {
		DOM.appendChild(elt, getElement());
		onScreenShown();
		onAttach();
	}

	@Override
	public void loadModel(JavaScriptObject model) {

	}

	@Override
	public void saveModel(JavaScriptObject model) {
	}

	@Override
	public void doLoad(ScreenRoot instance) {
	}

	public static void registerType() {
		GwtScreenRoot.register(TYPE, new IGwtDelegateFactory<IGwtScreenRoot, ScreenRoot>() {

			@Override
			public IGwtScreenRoot create(ScreenRoot screenRoot) {
				return new OrgUnitsBrowser(screenRoot);
			}
		});
		GWT.log("bm.ac.ResourceTypesBrowser registred");
	}

}
