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
package net.bluemind.ui.domain.client;

import java.util.Arrays;
import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Document;
import com.google.gwt.editor.client.IsEditor;
import com.google.gwt.editor.client.LeafValueEditor;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.DomEvent;
import com.google.gwt.event.dom.client.HasChangeHandlers;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.i18n.client.Constants;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.ListBox;

import net.bluemind.core.api.AsyncHandler;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.api.gwt.endpoint.DomainsGwtEndpoint;
import net.bluemind.gwtconsoleapp.base.notification.Notification;
import net.bluemind.ui.common.client.forms.Ajax;

public class DomainsListBox extends Composite implements IsEditor<LeafValueEditor<String>>, HasChangeHandlers {

	public static interface DCConstants extends Constants {
		String allDomain();
	}

	public static final DCConstants dcc = GWT.create(DCConstants.class);

	private ListBox box;
	private String selectedDomain;

	private LeafValueEditor<String> editor = new LeafValueEditor<String>() {

		@Override
		public void setValue(String value) {
			selectedDomain = value;
			updateBoxValue();
		}

		@Override
		public String getValue() {
			return selectedDomain;
		}
	};

	private List<ItemValue<Domain>> domains = Arrays.asList();

	public DomainsListBox() {
		box = new ListBox();
		box.addChangeHandler(evt -> updateValue());
		loadDomains();
		initWidget(box);
	}

	private void loadDomains() {
		DomainsGwtEndpoint ep = new DomainsGwtEndpoint(Ajax.TOKEN.getSessionId());
		ep.all(new AsyncHandler<List<ItemValue<Domain>>>() {

			@Override
			public void success(List<ItemValue<Domain>> value) {
				loadValues(value);
				DomEvent.fireNativeEvent(Document.get().createChangeEvent(), box);
			}

			@Override
			public void failure(Throwable e) {
				Notification.get().reportError(e);
			}
		});
	}

	protected void loadValues(List<ItemValue<Domain>> value) {
		box.clear();
		this.domains = value;
		for (ItemValue<Domain> domain : value) {
			box.addItem(domain.uid, domain.value.name);
		}

		updateBoxValue();

	}

	private void updateBoxValue() {
		if (selectedDomain != null) {
			for (int i = 0; i < box.getItemCount(); i++) {
				if (box.getValue(i).equals(selectedDomain)) {
					box.setSelectedIndex(i);
					break;
				}
			}
		}
	}

	protected void updateValue() {
		selectedDomain = box.getSelectedValue();
	}

	@Override
	public LeafValueEditor<String> asEditor() {
		return editor;
	}

	@Override
	public HandlerRegistration addChangeHandler(ChangeHandler handler) {
		return box.addChangeHandler(handler);
	}

	public ItemValue<Domain> getDomain() {
		ItemValue<Domain> ret = null;

		for (ItemValue<Domain> d : domains) {
			if (d.uid.equals(selectedDomain)) {
				ret = d;
				break;
			}
		}
		return ret;
	}

	public void setSelectedValue(String uid) {
		selectedDomain = uid;
		updateBoxValue();
	}
}
