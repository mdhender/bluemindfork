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

import com.google.gwt.core.client.GWT;
import com.google.gwt.i18n.client.Constants;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.user.cellview.client.SimplePager;
import com.google.gwt.view.client.HasRows;
import com.google.gwt.view.client.Range;

public class ACSimplePager extends SimplePager {

	public ACSimplePager(TextLocation location, Resources resources, boolean showFastForwardButton, int fastForwardRows,
			boolean showLastPageButton, int pageSize) {
		super(location, resources, showFastForwardButton, fastForwardRows, showLastPageButton);
		setPageSize(pageSize);
		setPage(0);
	}

	public static interface DirectorySimplePagerConstant extends Constants {
		String pagerOf();

	}

	private static final DirectorySimplePagerConstant constants = GWT.create(DirectorySimplePagerConstant.class);

	// Page size is normally derieved from the visibleRange
	private int pageSize = 10;

	@Override
	public void setPageSize(int pageSize) {
		this.pageSize = pageSize;
		super.setPageSize(pageSize);
	}

	// We want pageSize to remain constant
	@Override
	public int getPageSize() {
		return pageSize;
	}

	// Page forward by an exact size rather than the number of visible
	// rows as is in the norm in the underlying implementation
	@Override
	public void nextPage() {
		if (getDisplay() != null) {
			Range range = getDisplay().getVisibleRange();
			setPageStart(range.getStart() + getPageSize());
		}
	}

	// Page back by an exact size rather than the number of visible rows
	// as is in the norm in the underlying implementation
	@Override
	public void previousPage() {
		if (getDisplay() != null) {
			Range range = getDisplay().getVisibleRange();
			setPageStart(range.getStart() - getPageSize());
		}
	}

	// Override so the last page is shown with a number of rows less
	// than the pageSize rather than always showing the pageSize number
	// of rows and possibly repeating rows on the last and penultimate
	// page
	@Override
	public void setPageStart(int index) {
		if (getDisplay() != null) {
			Range range = getDisplay().getVisibleRange();
			int displayPageSize = getPageSize();
			if (isRangeLimited() && getDisplay().isRowCountExact()) {
				displayPageSize = Math.min(getPageSize(), getDisplay().getRowCount() - index);
			}
			index = Math.max(0, index);
			if (index != range.getStart()) {
				getDisplay().setVisibleRange(index, displayPageSize);
			}
		}
	}

	// Override to display "0 of 0" when there are no records (otherwise
	// you get "1-1 of 0") and "1 of 1" when there is only one record
	// (otherwise you get "1-1 of 1"). Not internationalised (but
	// neither is SimplePager)
	protected String createText() {
		NumberFormat formatter = NumberFormat.getFormat("#,###");
		HasRows display = getDisplay();
		Range range = display.getVisibleRange();
		int pageStart = range.getStart() + 1;
		int pageSize = range.getLength();
		int dataSize = display.getRowCount();
		int endIndex = Math.min(dataSize, pageStart + pageSize - 1);
		endIndex = Math.max(pageStart, endIndex);
		boolean exact = display.isRowCountExact();
		if (dataSize == 0) {
			return "0 " + constants.pagerOf() + " 0";
		} else if (pageStart == endIndex) {
			return formatter.format(pageStart) + " " + constants.pagerOf() + " " + formatter.format(dataSize);
		}
		return formatter.format(pageStart) + "-" + formatter.format(endIndex)
				+ (exact ? " " + constants.pagerOf() + " " : " of over ") + formatter.format(dataSize);
	}

}
