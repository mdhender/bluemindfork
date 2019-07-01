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
package net.bluemind.ui.adminconsole.base.client;

import com.google.gwt.cell.client.Cell;
import com.google.gwt.cell.client.Cell.Context;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.InputElement;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.user.cellview.client.DataGrid;
import com.google.gwt.user.cellview.client.Header;
import com.google.gwt.view.client.MultiSelectionModel;

public class CellHeader<T> extends Header<Boolean> {

	private DataGrid<T> cellTable;
	private MultiSelectionModel<T> selectionModel;

	public CellHeader(Cell<Boolean> cell, DataGrid<T> cellTable, MultiSelectionModel<T> selectionModel) {
		super(cell);
		this.cellTable = cellTable;
		this.selectionModel = selectionModel;
	}

	@Override
	public Boolean getValue() {
		return false;
	}

	@Override
	public void onBrowserEvent(Context context, Element elem, NativeEvent event) {
		InputElement ie = elem.getFirstChild().cast();
		for (T item : cellTable.getVisibleItems()) {
			selectionModel.setSelected(item, ie.isChecked());
		}
		super.onBrowserEvent(context, elem, event);
	}
}
