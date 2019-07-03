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
package net.bluemind.ui.adminconsole.directory.mailbox;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.HTMLPanel;

import net.bluemind.core.commons.gwt.JsMapStringJsObject;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.gwtconsoleapp.base.editor.WidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.CompositeGwtWidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.GwtWidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtDelegateFactory;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtWidgetElement;
import net.bluemind.gwtconsoleapp.base.handler.DefaultAsyncHandler;
import net.bluemind.mailbox.api.gwt.endpoint.MailboxMgmtGwtEndpoint;
import net.bluemind.ui.common.client.forms.Ajax;
import net.bluemind.ui.gwttask.client.TaskWatcher;

public class MailboxMaintenance extends CompositeGwtWidgetElement implements IGwtWidgetElement {

	interface GenralUiBinder extends UiBinder<HTMLPanel, MailboxMaintenance> {
	}

	public static final String TYPE = "bm.ac.MailboxMaintenance";

	private static GenralUiBinder uiBinder = GWT.create(GenralUiBinder.class);

	private MailboxMaintenance() {
		HTMLPanel panel = uiBinder.createAndBindUi(this);
		initWidget(panel);
		addIndexMailboxHandler();
	}

	@UiField
	Button consolidateMailboxIndex;

	private String mailbox;

	private String domain;

	private void addIndexMailboxHandler() {

		consolidateMailboxIndex.addClickHandler(new ClickHandler() {

			@Override
			public void onClick(ClickEvent event) {
				MailboxMgmtGwtEndpoint indexingService = new MailboxMgmtGwtEndpoint(Ajax.TOKEN.getSessionId(), domain);
				indexingService.consolidateMailbox(mailbox, new DefaultAsyncHandler<TaskRef>() {

					@Override
					public void success(TaskRef value) {
						indexSuccess(value);
					}

				});
			}
		});

	}

	private void indexSuccess(TaskRef value) {
		TaskWatcher.track(value.id);
	}

	@Override
	public void loadModel(JavaScriptObject model) {
		JsMapStringJsObject map = model.cast();
		domain = map.getString("domainUid");
		mailbox = map.getString("mailboxUid");
	}

	public static void registerType() {
		GwtWidgetElement.register(TYPE, new IGwtDelegateFactory<IGwtWidgetElement, WidgetElement>() {

			@Override
			public IGwtWidgetElement create(WidgetElement e) {
				return new MailboxMaintenance();
			}
		});
	}
}
