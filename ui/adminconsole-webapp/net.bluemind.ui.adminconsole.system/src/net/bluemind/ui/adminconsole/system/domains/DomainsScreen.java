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
package net.bluemind.ui.adminconsole.system.domains;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.shared.GWT;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SelectionChangeEvent.Handler;

import net.bluemind.core.api.AsyncHandler;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.core.task.api.TaskStatus;
import net.bluemind.core.task.api.gwt.endpoint.TaskGwtEndpoint;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.api.gwt.endpoint.DomainsGwtEndpoint;
import net.bluemind.gwtconsoleapp.base.editor.ScreenRoot;
import net.bluemind.gwtconsoleapp.base.editor.gwt.GwtScreenRoot;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtDelegateFactory;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtScreenRoot;
import net.bluemind.gwtconsoleapp.base.handler.DefaultAsyncHandler;
import net.bluemind.gwtconsoleapp.base.notification.Notification;
import net.bluemind.ui.adminconsole.base.Actions;
import net.bluemind.ui.adminconsole.system.domains.l10n.DomainConstants;
import net.bluemind.ui.adminconsole.system.hosts.l10n.HostConstants;
import net.bluemind.ui.common.client.forms.Ajax;

public class DomainsScreen extends Composite implements IGwtScreenRoot {

	private DockLayoutPanel dlp;

	private static HostConstants constants = HostConstants.INST;

	@UiField
	DomainsGrid grid;

	@UiField
	Button deleteButton;

	@UiField
	Button newButton;

	@UiHandler("deleteButton")
	void deleteClick(ClickEvent e) {
		Collection<ItemValue<Domain>> selection = grid.getSelected();
		List<ItemValue<Domain>> listSelection = new ArrayList<ItemValue<Domain>>(selection);

		String confirm = constants.deleteConfirmation();
		if (selection.size() > 1) {
			confirm = constants.massDeleteConfirmation();
		}

		deleteDomains(listSelection, confirm);
	}

	interface DomainsUiBinder extends UiBinder<DockLayoutPanel, DomainsScreen> {
	}

	private static DomainsUiBinder uiBinder = GWT.create(DomainsUiBinder.class);

	public static final String TYPE = "bm.ac.DomainsScreen";

	private DomainsScreen(ScreenRoot screenRoot) {
		this.dlp = uiBinder.createAndBindUi(this);
		initWidget(dlp);
		dlp.setHeight("100%");
		newButton.addClickHandler(new ClickHandler() {

			@Override
			public void onClick(ClickEvent event) {
				Actions.get().show("qcDomain", null);
			}
		});

		Handler handler = new Handler() {
			@Override
			public void onSelectionChange(SelectionChangeEvent event) {
				Collection<ItemValue<Domain>> entries = grid.getSelected();
				deleteButton.setEnabled(!entries.isEmpty());
			}
		};
		grid.addSelectionChangeHandler(handler);

	}

	public static void registerType() {
		GwtScreenRoot.register(TYPE, new IGwtDelegateFactory<IGwtScreenRoot, ScreenRoot>() {

			@Override
			public IGwtScreenRoot create(ScreenRoot screenRoot) {
				return new DomainsScreen(screenRoot);
			}
		});
	}

	public void attach(Element parent) {
		parent.appendChild(getElement());
		onScreenShown();
		onAttach();
	}

	private void onScreenShown() {
		DomainsGwtEndpoint service = new DomainsGwtEndpoint(Ajax.TOKEN.getSessionId());
		service.all(new AsyncHandler<List<ItemValue<Domain>>>() {

			@Override
			public void success(List<ItemValue<Domain>> value) {

				List<ItemValue<Domain>> filteredList = value.stream().filter(d -> !"global.virt".equals(d.uid))
						.collect(Collectors.toList());
				GWT.log("Filling domains grid with " + filteredList.size() + " domains.");

				grid.setValues(filteredList);
				grid.selectAll(false);
			}

			@Override
			public void failure(Throwable e) {
				Notification.get().reportError(e);
			}
		});
	}

	private <T> void deleteDomains(List<ItemValue<Domain>> listSelection, String confirm) {
		final List<ItemValue<Domain>> currentValues = grid.getValues();
		final DomainsGwtEndpoint service = new DomainsGwtEndpoint(Ajax.TOKEN.getSessionId());
		if (Window.confirm(confirm)) {
			for (Iterator<ItemValue<Domain>> iterator = listSelection.iterator(); iterator.hasNext();) {
				final ItemValue<Domain> itemValue = iterator.next();
				currentValues.remove(itemValue);
				deepDeleteDomain(service, itemValue);
			}
			grid.clearSelectionModel();
			grid.setValues(currentValues);
		}

	}

	private void deepDeleteDomain(final DomainsGwtEndpoint service, final ItemValue<Domain> itemValue) {
		service.deleteDomainItems(itemValue.uid, new DefaultAsyncHandler<TaskRef>() {

			@Override
			public void success(TaskRef value) {
				TaskGwtEndpoint taskService = new TaskGwtEndpoint(Ajax.TOKEN.getSessionId(), String.valueOf(value.id));
				Notification.get().reportInfo(DomainConstants.INST.deletingDomain(itemValue.value.name));
				waitForDeleteDomainItemsTask(taskService, service, itemValue);
			}

			private void waitForDeleteDomainItemsTask(final TaskGwtEndpoint taskService,
					final DomainsGwtEndpoint service, final ItemValue<Domain> itemValue) {
				taskService.status(new DefaultAsyncHandler<TaskStatus>() {

					@Override
					public void success(TaskStatus value) {
						if (value.state.ended) {
							if (value.state.succeed) {
								deleteDomain(service, itemValue);
							} else {
								Notification.get().reportError("Cannot delete domain items: " + value.lastLogEntry);
							}
						} else {
							Timer t = new Timer() {
								@Override
								public void run() {
									waitForDeleteDomainItemsTask(taskService, service, itemValue);
								}
							};
							t.schedule(500);
						}
					}
				});
			}
		});
	}

	private void deleteDomain(DomainsGwtEndpoint service, final ItemValue<Domain> itemValue) {
		service.delete(itemValue.uid, new AsyncHandler<Void>() {

			@Override
			public void success(Void value) {
				Notification.get().reportInfo(DomainConstants.INST.domainDeleted(itemValue.value.name));
				RootPanel.get().getElement()
						.dispatchEvent(Document.get().createHtmlEvent("refresh-domains", true, true));
			}

			@Override
			public void failure(Throwable e) {
				Notification.get().reportError(e);
			}
		});
	}

	@Override
	public void doLoad(final ScreenRoot instance) {
	}

	@Override
	public void loadModel(JavaScriptObject model) {
	}

	@Override
	public void saveModel(JavaScriptObject model) {
	}

}
