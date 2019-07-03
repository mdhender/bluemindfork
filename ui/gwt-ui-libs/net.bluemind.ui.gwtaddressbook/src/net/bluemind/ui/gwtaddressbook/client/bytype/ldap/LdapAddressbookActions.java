/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2017
  *
  * This file is part of BlueMind. BlueMind is a messaging and collaborative
  * solution.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of either the GNU Affero General Public License as
  * published by the Free Software Foundation (version 3 of the License).
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.ui.gwtaddressbook.client.bytype.ldap;

import java.util.Date;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;

import net.bluemind.addressbook.api.IAddressBookAsync;
import net.bluemind.addressbook.api.gwt.endpoint.AddressBookGwtEndpoint;
import net.bluemind.core.api.AsyncHandler;
import net.bluemind.core.commons.gwt.JsMapStringJsObject;
import net.bluemind.core.commons.gwt.JsMapStringString;
import net.bluemind.core.container.api.IContainerSyncAsync;
import net.bluemind.core.container.api.gwt.endpoint.ContainerSyncGwtEndpoint;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.gwtconsoleapp.base.editor.WidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.CompositeGwtWidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.GwtWidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtDelegateFactory;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtWidgetElement;
import net.bluemind.gwtconsoleapp.base.handler.DefaultAsyncHandler;
import net.bluemind.gwtconsoleapp.base.notification.Notification;
import net.bluemind.ui.common.client.forms.Ajax;
import net.bluemind.ui.gwttask.client.TaskWatcher;

// FIXME make ldap settings editable?
public class LdapAddressbookActions extends CompositeGwtWidgetElement {

	public static final String TYPE = "bm.addressbook.LdapAddressbookActions";

	private Label ldapHostname;
	private Label ldapProtocol;
	private Label ldapBaseDn;
	private Label ldapLoginDn;
	private Label ldapLoginPw;
	private Label ldapUserFilter;
	private Label entryUUID;
	private String containerUid;

	private Anchor sync;

	public LdapAddressbookActions() {
		FlexTable ft = new FlexTable();

		int i = 0;

		Anchor reset = new Anchor(LdapAddressbookConstants.INST.reset());
		reset.addClickHandler(new ClickHandler() {

			@Override
			public void onClick(ClickEvent event) {
				reset();
			}

		});

		sync = new Anchor(LdapAddressbookConstants.INST.launchSync());
		sync.addClickHandler(new ClickHandler() {

			@Override
			public void onClick(ClickEvent event) {
				sync();
			}
		});
		sync.getElement().getStyle().setPaddingLeft(10, Unit.PX);

		HorizontalPanel hp = new HorizontalPanel();
		hp.add(reset);
		hp.add(sync);
		ft.setWidget(i, 0, hp);
		ft.setWidget(i, 1, new Label());
		i++;

		ft.setWidget(i, 0, new Label(LdapAddressbookConstants.INST.ldapHostname()));
		ldapHostname = new Label();
		ft.setWidget(i, 1, ldapHostname);
		i++;

		ft.setWidget(i, 0, new Label(LdapAddressbookConstants.INST.ldapProtocol()));
		ldapProtocol = new Label();
		ft.setWidget(i, 1, ldapProtocol);
		i++;

		ft.setWidget(i, 0, new Label(LdapAddressbookConstants.INST.ldapBaseDn()));
		ldapBaseDn = new Label();
		ft.setWidget(i, 1, ldapBaseDn);
		i++;

		ft.setWidget(i, 0, new Label(LdapAddressbookConstants.INST.ldapLoginDn()));
		ldapLoginDn = new Label();
		ft.setWidget(i, 1, ldapLoginDn);
		i++;

		ft.setWidget(i, 0, new Label(LdapAddressbookConstants.INST.ldapLoginPw()));
		ldapLoginPw = new Label();
		ft.setWidget(i, 1, ldapLoginPw);
		i++;

		ft.setWidget(i, 0, new Label(LdapAddressbookConstants.INST.ldapUserFilter()));
		ldapUserFilter = new Label();
		ft.setWidget(i, 1, ldapUserFilter);
		i++;

		ft.setWidget(i, 0, new Label(LdapAddressbookConstants.INST.entryUUID()));
		entryUUID = new Label();
		ft.setWidget(i, 1, entryUUID);
		i++;

		initWidget(ft);
	}

	private void sync() {
		IContainerSyncAsync sync = new ContainerSyncGwtEndpoint(Ajax.TOKEN.getSessionId(), containerUid);
		sync.sync(new AsyncHandler<TaskRef>() {

			@Override
			public void success(TaskRef value) {
				TaskWatcher.track(value.id);
			}

			@Override
			public void failure(Throwable e) {

			}
		});

	}

	private void reset() {
		if (Window.confirm(LdapAddressbookConstants.INST.confirmReset())) {
			IAddressBookAsync service = new AddressBookGwtEndpoint(Ajax.TOKEN.getSessionId(), containerUid);
			service.reset(new DefaultAsyncHandler<Void>() {

				@Override
				public void success(Void value) {
					Notification.get().reportInfo(LdapAddressbookConstants.INST.resetOk());
				}
			});
		}
	}

	@Override
	public void loadModel(JavaScriptObject model) {
		super.loadModel(model);

		JsMapStringJsObject m = model.cast();

		containerUid = m.getString("container");

		JsMapStringString settings = m.get("settings").cast();
		ldapHostname.setText(settings.get("hostname"));
		ldapProtocol.setText(settings.get("protocol"));
		ldapBaseDn.setText(settings.get("baseDn"));
		ldapLoginDn.setText(settings.get("loginDn"));
		ldapLoginPw.setText(settings.get("loginPw"));
		ldapUserFilter.setText(settings.get("filter"));
		entryUUID.setText(settings.get("entryUUID"));
	}

	@Override
	public void attach(Element parent) {
		super.attach(parent);

		IContainerSyncAsync service = new ContainerSyncGwtEndpoint(Ajax.TOKEN.getSessionId(), containerUid);
		service.getLastSync(new DefaultAsyncHandler<Date>() {

			@Override
			public void success(Date value) {
				if (value != null) {
					sync.setTitle(LdapAddressbookConstants.INST.lastSync() + " " + value.toString());
				}
			}
		});
	}

	public static void registerType() {

		GwtWidgetElement.register(TYPE, new IGwtDelegateFactory<IGwtWidgetElement, WidgetElement>() {

			@Override
			public IGwtWidgetElement create(WidgetElement el) {
				return new LdapAddressbookActions();
			}
		});
	}

}
