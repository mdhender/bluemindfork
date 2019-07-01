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
package net.bluemind.ui.adminconsole.directory;

import java.util.HashMap;
import java.util.Map;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.view.client.CellPreviewEvent;
import com.google.gwt.view.client.DefaultSelectionEventManager.EventTranslator;
import com.google.gwt.view.client.DefaultSelectionEventManager.SelectAction;
import com.google.gwt.view.client.Range;

import net.bluemind.core.container.model.ItemValue;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.ui.adminconsole.base.Actions;
import net.bluemind.ui.adminconsole.base.DomainsHolder;
import net.bluemind.ui.adminconsole.base.ui.ScreenShowRequest;

public class RowEventTranslator<T> implements EventTranslator<T> {

	private ScreenShowRequest ssr;

	public RowEventTranslator(ScreenShowRequest ssr) {
		this.ssr = ssr;
	}

	@Override
	public boolean clearCurrentSelection(CellPreviewEvent<T> event) {
		return false;
	}

	@Override
	public SelectAction translateSelectionEvent(CellPreviewEvent<T> event) {
		NativeEvent nativeEvent = event.getNativeEvent();
		if ((nativeEvent.getShiftKey() || nativeEvent.getCtrlKey() || nativeEvent.getMetaKey())
				&& "click".equals(nativeEvent.getType())) {

			return SelectAction.TOGGLE;

		} else if ("click".equals(nativeEvent.getType())) {
			if (event.getColumn() == 0) {
				return SelectAction.TOGGLE;
			} else {
				editElement(event);
			}
			return SelectAction.IGNORE;
		}
		return SelectAction.IGNORE;

	}

	private void editElement(CellPreviewEvent<T> event) {

		Range range = event.getDisplay().getVisibleRange();
		int start = range.getStart();
		int IndexOnPage = event.getIndex() - start;
		ItemValue<DirEntry> de = (ItemValue<DirEntry>) event.getDisplay().getVisibleItem(IndexOnPage);

		ScreenShowRequest ssr;
		if (this.ssr != null) {
			ssr = this.ssr;
		} else {
			ssr = new ScreenShowRequest();
		}
		ssr.put("entry", de);

		String screen = null;
		switch (de.value.kind) {
		case MAILSHARE: {
			screen = null;
			Map<String, String> params = new HashMap<>();
			params.put("entryUid", de.value.entryUid);
			params.put("domainUid", DomainsHolder.get().getSelectedDomain().uid);
			Actions.get().showWithParams2("editMailshare", params);
		}
			break;
		case EXTERNALUSER: {
			screen = null;
			Map<String, String> params = new HashMap<>();
			params.put("entryUid", de.value.entryUid);
			params.put("domainUid",
					DomainsHolder.get().getSelectedDomain().uid);
			Actions.get().showWithParams2("editExternalUser", params);
		}
			break;
		case GROUP: {
			screen = null;
			Map<String, String> params = new HashMap<>();
			params.put("entryUid", de.value.entryUid);
			params.put("domainUid", DomainsHolder.get().getSelectedDomain().uid);
			Actions.get().showWithParams2("editGroup", params);
		}
			break;
		case RESOURCE: {
			screen = null;
			Map<String, String> params = new HashMap<>();
			params.put("entryUid", de.value.entryUid);
			params.put("domainUid", DomainsHolder.get().getSelectedDomain().uid);
			Actions.get().showWithParams2("editResource", params);
		}
			break;
		case USER: {
			screen = null;
			Map<String, String> params = new HashMap<>();
			params.put("entryUid", de.value.entryUid);
			params.put("domainUid", DomainsHolder.get().getSelectedDomain().uid);
			Actions.get().showWithParams2("editUser", params);

			break;
		}
		case ADDRESSBOOK: {
			screen = null;
			Map<String, String> params = new HashMap<>();
			params.put("entryUid", de.value.entryUid);
			params.put("domainUid", DomainsHolder.get().getSelectedDomain().uid);
			Actions.get().showWithParams2("editBook", params);
			break;
		}

		case CALENDAR: {
			screen = null;
			Map<String, String> params = new HashMap<>();
			params.put("entryUid", de.value.entryUid);
			params.put("domainUid", DomainsHolder.get().getSelectedDomain().uid);
			Actions.get().showWithParams2("editCalendar", params);
			break;
		}
		case DOMAIN:
			screen = null;
			break;
		}
		if (screen != null) {
			Actions.get().show(screen, ssr);
		} else {
			// Window.alert("Edit Directory Entry: " + de.displayName + ", "
			// + de.value.kind.toString());
		}
	}

	public ScreenShowRequest getSsr() {
		return ssr;
	}

	public void setSsr(ScreenShowRequest ssr) {
		this.ssr = ssr;
	}
}
