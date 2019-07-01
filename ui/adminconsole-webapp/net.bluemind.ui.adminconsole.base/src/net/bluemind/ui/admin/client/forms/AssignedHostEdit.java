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
package net.bluemind.ui.admin.client.forms;

import java.util.List;

import com.google.gwt.editor.client.IsEditor;
import com.google.gwt.editor.client.LeafValueEditor;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.ListBox;

import net.bluemind.core.api.AsyncHandler;
import net.bluemind.core.api.ListResult;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.gwtconsoleapp.base.notification.Notification;
import net.bluemind.server.api.Server;
import net.bluemind.ui.common.client.forms.finder.ServerFinder;

public class AssignedHostEdit extends Composite implements IsEditor<LeafValueEditor<String>> {

	private ListBox listBox = new ListBox();
	private ServerFinder finder;
	private List<ItemValue<Server>> servers;
	private String serverUid;
	private LeafValueEditor<String> editor = new LeafValueEditor<String>() {

		@Override
		public void setValue(String value) {
			AssignedHostEdit.this.serverUid = value;
			updateValue();
		}

		@Override
		public String getValue() {
			return serverUid;
		}

	};

	public AssignedHostEdit() {
		initWidget(listBox);
		finder = new ServerFinder(null);

		listBox.addChangeHandler(new ChangeHandler() {

			@Override
			public void onChange(ChangeEvent event) {
				serverUid = listBox.getSelectedValue();
			}
		});
	}

	protected void updateValue() {
		if (servers != null && serverUid != null) {
			int i = 0;
			for (ItemValue<Server> server : servers) {
				if (serverUid.equals(server.uid)) {
					listBox.setSelectedIndex(i);
					break;
				}
				i++;
			}
		}
	}

	public void setTagFilter(String tagFilter) {
		finder.setTagFilter(tagFilter);
	}

	public void setDomainUid(String domainUid) {
		finder.setDomain(domainUid);
		reload();
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
		for (ItemValue<Server> s : value.values) {
			listBox.addItem(s.value.address(), s.uid);
		}
		updateValue();
	}

	@Override
	public LeafValueEditor<String> asEditor() {
		return editor;
	}
}
