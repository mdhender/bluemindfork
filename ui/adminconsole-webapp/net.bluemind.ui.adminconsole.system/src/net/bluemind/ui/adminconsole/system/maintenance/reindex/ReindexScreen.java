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
package net.bluemind.ui.adminconsole.system.maintenance.reindex;

import java.util.HashMap;
import java.util.Map;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Element;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;

import net.bluemind.addressbook.api.gwt.endpoint.AddressBooksMgmtGwtEndpoint;
import net.bluemind.calendar.api.gwt.endpoint.CalendarsMgmtGwtEndpoint;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.gwtconsoleapp.base.editor.Ajax;
import net.bluemind.gwtconsoleapp.base.editor.ScreenElement;
import net.bluemind.gwtconsoleapp.base.editor.ScreenRoot;
import net.bluemind.gwtconsoleapp.base.editor.gwt.GwtScreenRoot;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtDelegateFactory;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtScreenRoot;
import net.bluemind.gwtconsoleapp.base.handler.DefaultAsyncHandler;
import net.bluemind.mailbox.api.gwt.endpoint.MailboxMgmtGwtEndpoint;
import net.bluemind.todolist.api.gwt.endpoint.TodoListsMgmtGwtEndpoint;
import net.bluemind.ui.adminconsole.base.Actions;
import net.bluemind.ui.adminconsole.base.DomainsHolder;

public class ReindexScreen extends Composite implements IGwtScreenRoot {

	private static final String TYPE = "bm.ac.ReindexScreen";

	private ScreenRoot screenRoot;

	interface UpdateBinder extends UiBinder<HTMLPanel, ReindexScreen> {
	}

	@UiField
	Button mails;

	@UiField
	Button contacts;

	@UiField
	Button calendar;

	@UiField
	Button todolist;

	private static UpdateBinder uiBinder = GWT.create(UpdateBinder.class);

	private ReindexScreen(ScreenRoot screenRoot) {
		this.screenRoot = screenRoot;
		HTMLPanel dlp = uiBinder.createAndBindUi(this);
		dlp.setHeight("100%");
		initWidget(dlp);

		mails.addClickHandler((e) -> {
			MailboxMgmtGwtEndpoint service = new MailboxMgmtGwtEndpoint(Ajax.TOKEN.getSessionId(),
					DomainsHolder.get().getSelectedDomain().uid);
			service.consolidateDomain(new DefaultAsyncHandler<TaskRef>() {

				@Override
				public void success(TaskRef value) {
					progress(value);
				}

			});
		});

		contacts.addClickHandler((e) -> {
			AddressBooksMgmtGwtEndpoint service = new AddressBooksMgmtGwtEndpoint(Ajax.TOKEN.getSessionId());
			service.reindexAll(new DefaultAsyncHandler<TaskRef>() {

				@Override
				public void success(TaskRef value) {
					progress(value);
				}

			});
		});

		calendar.addClickHandler((e) -> {
			CalendarsMgmtGwtEndpoint service = new CalendarsMgmtGwtEndpoint(Ajax.TOKEN.getSessionId());
			service.reindexAll(new DefaultAsyncHandler<TaskRef>() {

				@Override
				public void success(TaskRef value) {
					progress(value);
				}

			});
		});

		todolist.addClickHandler((e) -> {
			TodoListsMgmtGwtEndpoint service = new TodoListsMgmtGwtEndpoint(Ajax.TOKEN.getSessionId());
			service.reindexAll(new DefaultAsyncHandler<TaskRef>() {

				@Override
				public void success(TaskRef value) {
					progress(value);
				}

			});
		});

	}

	private void progress(TaskRef value) {
		Map<String, String> ssr = new HashMap<>();
		ssr.put("task", value.id + "");
		ssr.put("pictures", null);
		ssr.put("return", "system");
		ssr.put("success", "system");
		Actions.get().showWithParams2("progress", ssr);
	}

	public static void registerType() {
		GwtScreenRoot.register(TYPE, new IGwtDelegateFactory<IGwtScreenRoot, ScreenRoot>() {

			@Override
			public IGwtScreenRoot create(ScreenRoot screenRoot) {
				return new ReindexScreen(screenRoot);
			}
		});
	}

	public void attach(Element parent) {
		parent.appendChild(getElement());
		onAttach();
	}

	@Override
	public void doLoad(final ScreenRoot instance) {
		instance.load(new DefaultAsyncHandler<Void>() {

			@Override
			public void success(Void value) {
				instance.loadModel(instance.getModel());
			}
		});
	}

	@Override
	public void loadModel(JavaScriptObject model) {
	}

	@Override
	public void saveModel(JavaScriptObject model) {
	}

	public static ScreenElement screenModel() {
		ScreenRoot screenRoot = ScreenRoot.create("indexMaintenance", TYPE).cast();
		return screenRoot;
	}

}
