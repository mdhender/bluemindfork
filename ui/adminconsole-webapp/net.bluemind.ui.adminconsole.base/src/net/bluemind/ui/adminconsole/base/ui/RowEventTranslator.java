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
package net.bluemind.ui.adminconsole.base.ui;

import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.view.client.CellPreviewEvent;
import com.google.gwt.view.client.DefaultSelectionEventManager.EventTranslator;
import com.google.gwt.view.client.DefaultSelectionEventManager.SelectAction;

public class RowEventTranslator<T> implements EventTranslator<T> {

	private IEditHandler<T> handler;

	public RowEventTranslator() {
		this(null);
	}

	public RowEventTranslator(IEditHandler<T> handler) {
		this.handler = handler;
	}

	@Override
	public boolean clearCurrentSelection(CellPreviewEvent<T> event) {
		return false;
	}

	@Override
	public SelectAction translateSelectionEvent(CellPreviewEvent<T> event) {
		NativeEvent ne = event.getNativeEvent();

		if ((ne.getShiftKey() || ne.getCtrlKey() || ne.getMetaKey()) && "click".equals(ne.getType())) {
			return SelectAction.TOGGLE;
		} else if ("click".equals(ne.getType())) {
			if (handler != null) {
				return handler.edit(event);
			} else {
				return SelectAction.TOGGLE;
			}
		}
		return SelectAction.IGNORE;
	}

}
