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
package net.bluemind.ui.adminconsole.directory.resourcetype;

import java.util.HashMap;
import java.util.Map;

import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.view.client.CellPreviewEvent;
import com.google.gwt.view.client.DefaultSelectionEventManager.EventTranslator;
import com.google.gwt.view.client.DefaultSelectionEventManager.SelectAction;
import com.google.gwt.view.client.Range;

import net.bluemind.resource.api.type.ResourceType;
import net.bluemind.ui.adminconsole.base.Actions;
import net.bluemind.ui.adminconsole.base.DomainsHolder;

public class RowEventTranslator implements EventTranslator<ResourceType> {

	@Override
	public boolean clearCurrentSelection(CellPreviewEvent<ResourceType> event) {
		return false;
	}

	@Override
	public SelectAction translateSelectionEvent(CellPreviewEvent<ResourceType> event) {
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

	private void editElement(CellPreviewEvent<ResourceType> event) {

		Range range = event.getDisplay().getVisibleRange();
		int start = range.getStart();
		int IndexOnPage = event.getIndex() - start;
		ResourceType de = (ResourceType) event.getDisplay().getVisibleItem(IndexOnPage);

		Map<String, String> map = new HashMap<>();
		map.put("resourceTypeId", de.identifier);
		// FIXME
		map.put("domainUid", DomainsHolder.get().getSelectedDomain().uid);
		Actions.get().showWithParams2("editResourceType", map);
	}
}
