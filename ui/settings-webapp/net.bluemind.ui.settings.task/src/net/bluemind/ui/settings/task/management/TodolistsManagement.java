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
package net.bluemind.ui.settings.task.management;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.Document;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.HTMLTable.Cell;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

import net.bluemind.core.api.AsyncHandler;
import net.bluemind.core.container.api.ContainerQuery;
import net.bluemind.core.container.api.ContainerSubscription;
import net.bluemind.core.container.api.IContainersAsync;
import net.bluemind.core.container.api.gwt.endpoint.ContainerManagementGwtEndpoint;
import net.bluemind.core.container.api.gwt.endpoint.ContainersGwtEndpoint;
import net.bluemind.core.container.model.ContainerDescriptor;
import net.bluemind.core.container.model.ContainerModifiableDescriptor;
import net.bluemind.core.container.model.acl.AccessControlEntry;
import net.bluemind.core.container.model.acl.Verb;
import net.bluemind.gwtconsoleapp.base.handler.DefaultAsyncHandler;
import net.bluemind.gwtconsoleapp.base.notification.INotification;
import net.bluemind.todolist.api.ITodoListsAsync;
import net.bluemind.todolist.api.ITodoUids;
import net.bluemind.todolist.api.gwt.endpoint.TodoListsGwtEndpoint;
import net.bluemind.ui.common.client.forms.Ajax;
import net.bluemind.ui.common.client.forms.CommonForm;
import net.bluemind.ui.common.client.forms.InlineEditBox;
import net.bluemind.ui.common.client.forms.extensions.ICommonEditor;
import net.bluemind.ui.common.client.icon.Trash;
import net.bluemind.user.api.gwt.endpoint.UserSubscriptionGwtEndpoint;

public class TodolistsManagement extends CommonForm implements ICommonEditor {

	@UiField
	FlexTable table;

	@UiField
	TextBox label;

	@UiField
	Button addList;

	private static BookManagementUiBinder uiBinder = GWT.create(BookManagementUiBinder.class);

	interface BookManagementUiBinder extends UiBinder<HTMLPanel, TodolistsManagement> {
	}

	public static interface Resources extends ClientBundle {

		@Source("TodolistsManagement.css")
		Style editStyle();

	}

	private static final Resources res = GWT.create(Resources.class);

	public static interface Style extends CssResource {

		String container();

		String icon();

		String action();

		String label();

	}

	private final Style s;
	private List<String> books;
	private IContainersAsync containers = new ContainersGwtEndpoint(Ajax.TOKEN.getSessionId());
	private ITodoListsAsync tdMgmt = new TodoListsGwtEndpoint(Ajax.TOKEN.getSessionId());
	private INotification notification;

	public TodolistsManagement(INotification notification) {
		super();
		s = res.editStyle();
		s.ensureInjected();
		form = uiBinder.createAndBindUi(this);
		this.notification = notification;
		books = new ArrayList<String>();
		table.setStyleName(s.container());
		table.getColumnFormatter().addStyleName(0, s.icon());
		table.getColumnFormatter().addStyleName(2, s.action());
		table.getColumnFormatter().addStyleName(3, s.action());
		table.getColumnFormatter().addStyleName(4, s.action());

		initValues();

		addList.addClickHandler(new ClickHandler() {

			@Override
			public void onClick(ClickEvent event) {
				doCreateList();
			}
		});

	}

	protected void doCreateList() {
		if (label.getText().isEmpty()) {
			notification.reportError(TodolistsManagementConstants.INST.emptyLabel());
			return;
		}

		final String uid = GUID.get();
		// FIXME addressbookMgmt should autosubscribe owner if owner is user
		tdMgmt.create(
				uid, ContainerDescriptor.create(uid, label.getText(), Ajax.TOKEN.getSubject(),
						ITodoUids.TYPE, Ajax.TOKEN.getContainerUid(), false),
				new DefaultAsyncHandler<Void>() {

					@Override
					public void success(Void value) {
						new ContainerManagementGwtEndpoint(Ajax.TOKEN.getSessionId(), uid).setAccessControlList(
								Arrays.asList(AccessControlEntry.create(Ajax.TOKEN.getSubject(), Verb.All)),
								new DefaultAsyncHandler<Void>() {

									@Override
									public void success(Void value) {
										new UserSubscriptionGwtEndpoint(Ajax.TOKEN.getSessionId(),
												Ajax.TOKEN.getContainerUid()).subscribe(Ajax.TOKEN.getSubject(),
														Arrays.asList(ContainerSubscription.create(uid, false)),
														new DefaultAsyncHandler<Void>() {

															@Override
															public void success(Void value) {
																notification.reportInfo("OK");
																resetForm();
																initValues();

																form.getElement()
																		.dispatchEvent(Document.get().createHtmlEvent(
																				"refresh-container", true, true));
															}
														});
									}
								});
					}
				});
	}

	private void initValues() {
		ContainerQuery query = ContainerQuery.type(ITodoUids.TYPE);
		query.owner = Ajax.TOKEN.getSubject();
		containers.all(query, new AsyncHandler<List<ContainerDescriptor>>() {

			@Override
			public void success(List<ContainerDescriptor> value) {
				for (ContainerDescriptor f : value) {
					addEntry(f);
				}
			}

			@Override
			public void failure(Throwable e) {
				notification.reportError(e);
			}
		});

	}

	public void addEntry(final ContainerDescriptor f) {
		final String key = f.uid;
		if (!books.contains(key)) {
			int row = table.getRowCount();
			int i = 0;
			Label icon = new Label();
			icon.setStyleName("fa fa-2x fa-calendar-check-o");
			table.setWidget(row, i++, icon);

			Trash trash = null;
			String label = f.name;
			if (f.defaultContainer) {
				Label l = new Label(label);
				l.setStyleName(s.label());
				table.setWidget(row, i++, l);
			} else {
				trash = new Trash();
				trash.addClickHandler(new ClickHandler() {
					@Override
					public void onClick(ClickEvent event) {
						if (Window.confirm(TodolistsManagementConstants.INST.confirmDelete(f.name))) {
							delete(f);
							Cell c = table.getCellForEvent(event);
							table.removeRow(c.getRowIndex());
							books.remove(key);
						}
					}
				});
				final InlineEditBox edit = new InlineEditBox(label);
				edit.setAction(new ScheduledCommand() {

					@Override
					public void execute() {
						if (edit.getValue().trim().isEmpty()) {
							notification.reportError(TodolistsManagementConstants.INST.emptyLabel());
						} else {
							f.name = edit.getValue();
							ContainerModifiableDescriptor desc = new ContainerModifiableDescriptor();
							desc.name = f.name;
							desc.defaultContainer = f.defaultContainer;
							containers.update(f.uid,

									desc, new AsyncHandler<Void>() {

										@Override
										public void success(Void value) {
											notification.reportInfo(TodolistsManagementConstants.INST.updateOk());
											form.getElement().dispatchEvent(
													Document.get().createHtmlEvent("refresh-container", true, true));
										}

										@Override
										public void failure(Throwable e) {
											notification.reportError(e);
										}
									});

						}
					}
				});
				table.setWidget(row, i++, edit);

			}

			table.setWidget(row, i++, trash);

			books.add(key);
		}

	}

	private void delete(final ContainerDescriptor f) {
		tdMgmt.delete(f.uid, new AsyncHandler<Void>() {

			@Override
			public void success(Void value) {
				notification.reportInfo(TodolistsManagementConstants.INST.deleteOk());
				form.getElement().dispatchEvent(Document.get().createHtmlEvent("refresh-container", true, true));
			}

			@Override
			public void failure(Throwable e) {
				notification.reportError(e);
			}
		});
	}

	private void resetForm() {
		label.setValue("");
	}

	@Override
	public void setTitleText(String s) {
	}

	@Override
	public String getStringValue() {
		return null;
	}

	@Override
	public void setStringValue(String v) {
	}

	@Override
	public void setPropertyName(String string) {
	}

	@Override
	public Widget asWidget() {
		return form;
	}

	@Override
	public String getPropertyName() {
		return null;
	}

	@Override
	public void setReadOnly(boolean readOnly) {
	}
}
