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
package net.bluemind.ui.mailbox.backend;

import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.editor.client.IsEditor;
import com.google.gwt.editor.client.LeafValueEditor;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.ListBox;

import net.bluemind.core.api.AsyncHandler;
import net.bluemind.core.api.ListResult;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.directory.api.IDirectoryPromise;
import net.bluemind.directory.api.gwt.endpoint.DirectoryGwtEndpoint;
import net.bluemind.gwtconsoleapp.base.notification.Notification;
import net.bluemind.server.api.Server;
import net.bluemind.ui.common.client.forms.Ajax;
import net.bluemind.ui.common.client.forms.finder.ServerFinder;
import net.bluemind.ui.gwttask.client.TaskWatcher;
import net.bluemind.ui.mailbox.l10n.MailboxConstants;

public class MailBackendServerEdit extends Composite implements IsEditor<LeafValueEditor<String>> {

	private ListBox listBox = new ListBox();
	private ServerFinder finder;
	private List<ItemValue<Server>> servers;
	private String serverUid;
	private String dirEntryUid;

	private LeafValueEditor<String> editor = new LeafValueEditor<String>() {

		@Override
		public void setValue(String value) {
			MailBackendServerEdit.this.serverUid = value;
			updateValue();
		}

		@Override
		public String getValue() {
			return serverUid;
		}

	};
	private String domainUid;

	public MailBackendServerEdit() {
		initWidget(listBox);
		finder = new ServerFinder(null);

		listBox.addChangeHandler(new ChangeHandler() {

			@Override
			public void onChange(ChangeEvent event) {
				if (Window.confirm(MailboxConstants.INST.moveBackendQuestion(serverName(serverUid),
						serverName(listBox.getSelectedValue())))) {
					changeValue(listBox.getSelectedValue());
				} else {
					updateValue();
				}
			}

		});
	}

	public void setActive(boolean active) {
		this.listBox.setEnabled(active);
	}

	private String serverName(String serverUid) {
		return servers.stream().filter(serverItem -> serverItem.uid.equals(serverUid)).map(si -> si.value.name)
				.findFirst().orElse(null);
	}

	protected void changeValue(String selectedValue) {
		IDirectoryPromise service = new DirectoryGwtEndpoint(Ajax.TOKEN.getSessionId(), domainUid).promiseApi();
		service.xfer(dirEntryUid, selectedValue).thenCompose(tr -> {
			return TaskWatcher.track(tr.id, false);
		}).thenAccept((v) -> {
			serverUid = selectedValue;
		});
	}

	protected void updateValue() {
		GWT.log("servers " + servers);
		GWT.log("server Uid " + serverUid);
		if (servers != null && serverUid != null) {
			int i = 0;
			for (ItemValue<Server> server : servers) {
				if (serverUid.equals(server.uid)) {
					listBox.setSelectedIndex(i);
					break;
				}
				i++;
			}

			// Window.alert(" server not found ?!");
		}
	}

	public void setTagFilter(String tagFilter) {
		finder.setTagFilter(tagFilter);
	}

	public void setDomainUid(String domainUid) {
		this.domainUid = domainUid;
		finder.setDomain(domainUid);
		reload();
	}

	public void setDirEntryUid(String uid) {
		this.dirEntryUid = uid;
	}

	public void reload() {
		finder.find(null, new AsyncHandler<ListResult<ItemValue<Server>>>() {

			@Override
			public void success(ListResult<ItemValue<Server>> value) {
				loadValues(value);
			}

			@Override
			public void failure(Throwable e) {
				Notification.get().reportError(e);
			}

		});
	}

	protected void loadValues(ListResult<ItemValue<Server>> value) {
		listBox.clear();
		this.servers = value.values;
		for (ItemValue<Server> s : value.values) {
			listBox.addItem(s.value.name + " (" + s.value.address() + ")", s.uid);
		}
		updateValue();
	}

	@Override
	public LeafValueEditor<String> asEditor() {
		return editor;
	}
}
