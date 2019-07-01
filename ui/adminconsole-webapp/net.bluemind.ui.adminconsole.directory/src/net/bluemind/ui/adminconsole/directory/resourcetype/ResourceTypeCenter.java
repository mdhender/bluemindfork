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
package net.bluemind.ui.adminconsole.directory.resourcetype;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.view.client.AsyncDataProvider;
import com.google.gwt.view.client.HasData;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SelectionChangeEvent.Handler;

import net.bluemind.core.api.AsyncHandler;
import net.bluemind.gwtconsoleapp.base.editor.ScreenRoot;
import net.bluemind.gwtconsoleapp.base.editor.gwt.GwtScreenRoot;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtDelegateFactory;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtScreenRoot;
import net.bluemind.gwtconsoleapp.base.notification.Notification;
import net.bluemind.resource.api.IResourcesPromise;
import net.bluemind.resource.api.gwt.endpoint.ResourcesGwtEndpoint;
import net.bluemind.resource.api.type.ResourceType;
import net.bluemind.resource.api.type.gwt.endpoint.ResourceTypesGwtEndpoint;
import net.bluemind.ui.adminconsole.base.Actions;
import net.bluemind.ui.adminconsole.base.DomainsHolder;
import net.bluemind.ui.adminconsole.directory.resourcetype.l10n.ResourceTypeConstants;
import net.bluemind.ui.common.client.forms.Ajax;

public class ResourceTypeCenter extends Composite implements IGwtScreenRoot {

	@UiField
	TextBox search;

	@UiHandler("search")
	void searchOnKeyPress(KeyPressEvent event) {
		if (event.getNativeEvent().getKeyCode() == KeyCodes.KEY_ENTER) {
			find();
		}
	}

	@UiField
	ResourceTypeGrid grid;

	@UiField
	Button newButton;

	@UiField
	Button deleteButton;

	@UiHandler("deleteButton")
	void deleteClick(ClickEvent e) {
		Collection<ResourceType> selection = grid.getSelected();
		List<ResourceType> listSelection = new ArrayList<ResourceType>(selection);

		GWT.log("Selection Size = " + selection.size());

		String confirm = constants.deleteConfirmation();

		String domainUid = DomainsHolder.get().getSelectedDomain().uid;
		String typeId = listSelection.get(0).identifier;
		grid.clearSelectionModel();

		IResourcesPromise r = new ResourcesGwtEndpoint(Ajax.TOKEN.getSessionId(), domainUid).promiseApi();
		r.byType(typeId).thenAccept(res -> {
			if (!res.isEmpty()) {
				Notification.get().reportError(constants.deleteTypeUsedByResources());
			} else {

				ResourceTypesGwtEndpoint resourceTypes = new ResourceTypesGwtEndpoint(Ajax.TOKEN.getSessionId(),
						domainUid);

				resourceTypes.delete(typeId, new AsyncHandler<Void>() {

					@Override
					public void success(Void result) {
						GWT.log("delete resource type");
						find();
					}

					@Override
					public void failure(Throwable e) {
						Notification.get().reportError(e);
					}

				});
			}
		}).exceptionally(ex -> {
			Notification.get().reportError(ex);
			return null;
		});

	}

	interface ResourceTypeUiBinder extends UiBinder<DockLayoutPanel, ResourceTypeCenter> {
	}

	private static ResourceTypeUiBinder uiBinder = GWT.create(ResourceTypeUiBinder.class);
	private static final ResourceTypeConstants constants = GWT.create(ResourceTypeConstants.class);
	public static final int PAGE_SIZE = 25;

	public static final String TYPE = "bm.ac.ResourceTypesBrowser";

	public ResourceTypeCenter(ScreenRoot screenRoot) {

		DockLayoutPanel dlp = uiBinder.createAndBindUi(this);
		dlp.setHeight("100%");
		initWidget(dlp);

		search.getElement().setAttribute("placeholder", constants.addFilter());

		newButton.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				Actions.get().showWithParams2("qcResourceType", null);
				event.stopPropagation();
			}
		});

		Handler handler = new Handler() {
			@Override
			public void onSelectionChange(SelectionChangeEvent event) {
				Collection<ResourceType> rtypes = grid.getSelected();
				deleteButton.setEnabled(!rtypes.isEmpty());
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

		AsyncDataProvider<ResourceType> provider = new AsyncDataProvider<ResourceType>() {

			@Override
			protected void onRangeChanged(HasData<ResourceType> display) {
				String domainUid = DomainsHolder.get().getSelectedDomain().uid;

				ResourceTypesGwtEndpoint resourceTypes = new ResourceTypesGwtEndpoint(Ajax.TOKEN.getSessionId(),
						domainUid);

				resourceTypes.getTypes(new AsyncHandler<List<ResourceType>>() {

					@Override
					public void success(List<ResourceType> result) {
						grid.setValues(result);
					}

					@Override
					public void failure(Throwable e) {
						GWT.log("error " + e.getMessage());
					}
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
				return new ResourceTypeCenter(screenRoot);
			}
		});
		GWT.log("bm.ac.ResourceTypesBrowser registred");
	}

}
