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
package net.bluemind.ui.gwtuser.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.Document;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.HTMLTable.Cell;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.Widget;

import net.bluemind.calendar.api.CalendarDescriptor;
import net.bluemind.calendar.api.ICalendarsMgmtAsync;
import net.bluemind.calendar.api.gwt.endpoint.CalendarsMgmtGwtEndpoint;
import net.bluemind.calendar.api.gwt.js.JsCalendarDescriptor;
import net.bluemind.calendar.api.gwt.serder.CalendarDescriptorGwtSerDer;
import net.bluemind.core.api.AsyncHandler;
import net.bluemind.core.commons.gwt.JsMapStringString;
import net.bluemind.core.container.api.ContainerSubscription;
import net.bluemind.core.container.api.IContainersAsync;
import net.bluemind.core.container.api.gwt.endpoint.ContainerManagementGwtEndpoint;
import net.bluemind.core.container.api.gwt.endpoint.ContainersGwtEndpoint;
import net.bluemind.core.container.model.ContainerModifiableDescriptor;
import net.bluemind.core.container.model.acl.AccessControlEntry;
import net.bluemind.core.container.model.acl.Verb;
import net.bluemind.core.container.model.gwt.js.JsContainerDescriptor;
import net.bluemind.core.utils.GUID;
import net.bluemind.gwtconsoleapp.base.editor.WidgetElement;
import net.bluemind.gwtconsoleapp.base.handler.DefaultAsyncHandler;
import net.bluemind.gwtconsoleapp.base.notification.INotification;
import net.bluemind.gwtconsoleapp.base.notification.Notification;
import net.bluemind.ui.common.client.forms.Ajax;
import net.bluemind.ui.common.client.forms.CommonForm;
import net.bluemind.ui.common.client.forms.InlineEditBox;
import net.bluemind.ui.common.client.forms.extensions.ICommonEditor;
import net.bluemind.ui.common.client.icon.Trash;
import net.bluemind.ui.gwtcalendar.client.bytype.CalendarTypeExtension;
import net.bluemind.ui.gwtcalendar.client.bytype.CreateCalendarWidget;
import net.bluemind.ui.gwtsharing.client.SharingsModel;
import net.bluemind.ui.gwtuser.client.l10n.CalendarManagementConstants;
import net.bluemind.user.api.gwt.endpoint.UserSubscriptionGwtEndpoint;

public class CalendarManagement extends CommonForm implements ICommonEditor {

	private String domainUid = Ajax.TOKEN.getContainerUid();
	private String owner = Ajax.TOKEN.getSubject();

	private JavaScriptObject model;

	@UiField
	FlexTable table;

	@UiField
	HTMLPanel newCalendarPanel;

	@UiField
	ListBox calendarType;

	private static CalendarManagementUiBinder uiBinder = GWT.create(CalendarManagementUiBinder.class);

	interface CalendarManagementUiBinder extends UiBinder<HTMLPanel, CalendarManagement> {
	}

	public static interface Resources extends ClientBundle {

		@Source("CalendarManagement.css")
		Style editStyle();

	}

	private static final Resources res = GWT.create(Resources.class);

	public static interface Style extends CssResource {

		String container();

		String icon();

		String action();

		String label();

		String deleteCalendar();

		String header();
	}

	private final Style s;
	private List<String> calendars;
	private IContainersAsync containers = new ContainersGwtEndpoint(Ajax.TOKEN.getSessionId());
	private CreateCalendarWidget createCalendarWidget;
	private INotification notification;
	private ICalendarsMgmtAsync calendarsMgmt = new CalendarsMgmtGwtEndpoint(Ajax.TOKEN.getSessionId());
	private List<String> freebusyUids;

	public CalendarManagement(INotification notification) {
		super();
		s = res.editStyle();
		s.ensureInjected();
		form = uiBinder.createAndBindUi(this);
		this.notification = notification;
		calendars = new ArrayList<String>();
		table.setStyleName(s.container());
		int i = 0;
		table.getColumnFormatter().addStyleName(i++, s.label());
		table.getColumnFormatter().addStyleName(i++, s.icon());
		table.getColumnFormatter().addStyleName(i++, s.action());
		table.getColumnFormatter().addStyleName(i++, s.icon());

		initHeaders();

		createCalendarWidget = new CreateCalendarWidget();

		newCalendarPanel.add(createCalendarWidget);
		for (CalendarTypeExtension t : createCalendarWidget.types()) {
			calendarType.addItem(t.getLabel(), t.getType());
		}

		calendarType.addChangeHandler(new ChangeHandler() {

			@Override
			public void onChange(ChangeEvent event) {
				initCalendarType();
			}
		});
		initCalendarType();
		freebusyUids = new ArrayList<String>();
	}

	public void setDomainAndOwner(String domainUid, String owner) {
		this.domainUid = domainUid;
		this.owner = owner;
	}

	private void initHeaders() {
		int row = table.getRowCount();
		int i = 0;
		table.setWidget(row, i++, new Label(CalendarManagementConstants.INST.label()));
		table.setWidget(row, i++, new Label(CalendarManagementConstants.INST.freebusy()));
		table.setWidget(row, i++, new Label(CalendarManagementConstants.INST.actions()));
		table.setWidget(row, i++, new Label(""));
		table.getRowFormatter().addStyleName(0, s.header());
	}

	@UiHandler("addCalendar")
	public void doCreateCalendar(ClickEvent event) {
		JsCalendarDescriptor cal = JsCalendarDescriptor.create();
		cal.setSettings((JsMapStringString) JavaScriptObject.createObject().cast());

		createCalendarWidget.saveModel(cal);

		CalendarDescriptor descriptor = new CalendarDescriptorGwtSerDer().deserialize(new JSONObject(cal));

		if (descriptor.name == null && descriptor.settings.isEmpty()) {
			notification.reportError(CalendarManagementConstants.INST.notValid());
			return;
		}

		if (descriptor.name == null || descriptor.name.trim().isEmpty()) {
			notification.reportError(CalendarManagementConstants.INST.emptyLabel());
			return;
		}

		final String uid = GUID.get();

		descriptor.domainUid = domainUid;
		descriptor.owner = owner;
		if (cal.getSettings().get("type") != null && cal.getSettings().get("type").equals("externalIcs")) {
			descriptor.settings.put("readonly", "true");
		}
		calendarsMgmt.create(uid, descriptor, new DefaultAsyncHandler<Void>() {

			@Override
			public void success(Void value) {
				new ContainerManagementGwtEndpoint(Ajax.TOKEN.getSessionId(), uid).setAccessControlList(
						Arrays.asList(AccessControlEntry.create(owner, Verb.All)), new DefaultAsyncHandler<Void>() {

							@Override
							public void success(Void value) {
								new UserSubscriptionGwtEndpoint(Ajax.TOKEN.getSessionId(), domainUid).subscribe(owner,
										Arrays.asList(ContainerSubscription.create(uid, false)),
										new DefaultAsyncHandler<Void>() {

											@Override
											public void success(Void value) {
												Notification.get().reportInfo("Created !");
												form.getElement().dispatchEvent(Document.get()
														.createHtmlEvent("refresh-container", true, true));
											}
										});
							}
						});
			}
		});
	}

	private void initCalendarType() {
		String value = calendarType.getSelectedValue();
		if (value == null) {
			value = "internal";
		}

		createCalendarWidget.show(value);

	}

	public void initValues(JavaScriptObject model) {
		this.model = model;
		table.removeAllRows();
		initHeaders();
		freebusyUids.clear();
		calendars.clear();

		CalendarManagementModel cmm = model.cast();
		List<String> freebusy = cmm.getFreebusyAsList();

		for (int i = 0; i < cmm.getCalendars().length(); i++) {
			addEntry(cmm.getCalendars().get(i), freebusy);
		}
	}

	private void addEntry(final JsContainerDescriptor cal, List<String> freebusy) {
		final String key = cal.getUid();
		if (!calendars.contains(key)) {
			int row = table.getRowCount();
			int i = 0;

			CheckBox cb = new CheckBox();
			cb.setName(cal.getUid());
			cb.setFormValue(cal.getUid());
			if (cal.getDefaultContainer()) {
				cb.setValue(true);
				cb.setEnabled(false);
				freebusyUids.add(cal.getUid());
			} else {
				if (freebusy != null) {
					boolean checked = freebusy.contains(cal.getUid());
					cb.setValue(checked);
					if (checked) {
						freebusyUids.add(cal.getUid());
					}
					cb.addValueChangeHandler(new ValueChangeHandler<Boolean>() {

						@Override
						public void onValueChange(ValueChangeEvent<Boolean> event) {
							if (event.getValue()) {
								freebusyUids.add(cal.getUid());
							} else {
								freebusyUids.remove(cal.getUid());
							}

						}
					});
				}
			}

			Trash trash = null;
			String label = cal.getName();
			if (cal.getDefaultContainer()) {
				Label l = new Label(label);
				l.setStyleName(s.label());
				table.setWidget(row, i++, l);
			} else {
				trash = new Trash();
				trash.setId("calendar-management-trash-" + key);
				trash.addClickHandler(new ClickHandler() {
					@Override
					public void onClick(ClickEvent event) {
						if (Window.confirm(CalendarManagementConstants.INST.confirmDelete(cal.getName()))) {
							Cell c = table.getCellForEvent(event);
							delete(cal.getUid(), c, key);
						}
					}
				});
				final InlineEditBox edit = new InlineEditBox(label);
				edit.setAction(new ScheduledCommand() {

					@Override
					public void execute() {
						if (edit.getValue().trim().isEmpty()) {
							notification.reportError(CalendarManagementConstants.INST.emptyLabel());
						} else {
							cal.setName(edit.getValue());
							ContainerModifiableDescriptor desc = new ContainerModifiableDescriptor();
							desc.name = cal.getName();
							desc.defaultContainer = cal.getDefaultContainer();
							containers.update(cal.getUid(), desc, new AsyncHandler<Void>() {

								@Override
								public void success(Void value) {
									notification.reportInfo(CalendarManagementConstants.INST.updateOk());
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

			table.setWidget(row, i++, cb);

			String type = cal.getSettings().get("type");
			if (type == null) {
				type = "internal";
			}
			CalendarTypeExtension ext = CalendarTypeExtension.getExtensionByType(type);
			if (ext != null) {
				WidgetElement widget = ext.actionsWidget(cal.getUid(), cal.getSettings());
				Label wrapper = new Label();
				table.setWidget(row, i++, wrapper);
				widget.attach(wrapper.getElement());
			} else {
				i++;
			}
			table.setWidget(row, i++, trash);

			calendars.add(key);
		}

	}

	private void delete(final String uid, Cell c, String key) {
		CalendarsMgmtGwtEndpoint endpoint = new CalendarsMgmtGwtEndpoint(Ajax.TOKEN.getSessionId());
		endpoint.delete(uid, new AsyncHandler<Void>() {

			@Override
			public void success(Void value) {
				SharingsModel.get(model, UserSettingsCalendarsSharingModelHandler.MODEL_ID).removeData(uid);
				table.removeRow(c.getRowIndex());
				calendars.remove(key);
				notification.reportInfo(CalendarManagementConstants.INST.deleteOk());
				form.getElement().dispatchEvent(Document.get().createHtmlEvent("refresh-container", true, true));
			}

			@Override
			public void failure(Throwable e) {
				notification.reportError(e);
			}
		});
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

	public List<String> getFreebusyUids() {
		return freebusyUids;
	}
}
