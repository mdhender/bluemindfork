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
package net.bluemind.ui.common.client.ui;

import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.Widget;

public class HorizontalTabLayoutPanel extends TabLayoutPanel {

	private static final String CONTENT_STYLE = "horizontalTabLayoutPanelContent";
	protected static final String TAB_STYLE = "horizontalTabLayoutPanelTab";
	protected static final String TAB_INNER_STYLE = "horizontalTabLayoutPanelTabInner";

	private FlexTable tabBar = new FlexTable();

	/**
	 * Create an empty horizontal tab panel
	 * 
	 * @param barWidth
	 * @param barUnit
	 */
	public HorizontalTabLayoutPanel(double barWidth, Unit barUnit) {
		DockLayoutPanel dlp = new DockLayoutPanel(barUnit);
		initWidget(dlp);

		// Add the tab bar to the panel.
		dlp.addNorth(tabBar, barWidth);

		// Add the deck panel to the panel.
		dlp.add(deckPanel);

		tabBar.setStyleName("horizontalTabLayoutPanelTabs");
		setStyleName("horizontalTabLayoutPanel");
	}

	/**
	 * Create an empty horizontal tab panel with a custom tabBar
	 * 
	 * @param barWidth
	 * @param barUnit
	 * @param tabBar
	 */
	public HorizontalTabLayoutPanel(double barWidth, Unit barUnit, FlexTable tabBar) {
		DockLayoutPanel dlp = new DockLayoutPanel(barUnit);
		initWidget(dlp);

		this.tabBar = tabBar;

		// Add the deck panel to the panel.
		dlp.add(deckPanel);

		tabBar.setStyleName("horizontalTabLayoutPanelTabs");
		setStyleName("horizontalTabLayoutPanel");
	}

	@Override
	public boolean remove(int index) {
		if ((index < 0) || (index >= getWidgetCount())) {
			return false;
		}

		Widget child = getWidget(index);
		tabBar.removeCell(0, index);
		deckPanel.removeProtected(child);
		child.removeStyleName(CONTENT_STYLE);

		Tab tab = tabs.remove(index);
		tab.getWidget().removeFromParent();

		if (index == selectedIndex) {
			// If the selected tab is being removed, select the first tab (if
			// there
			// is one).
			selectedIndex = -1;
			if (getWidgetCount() > 0) {
				selectTab(0);
			}
		} else if (index < selectedIndex) {
			// If the selectedIndex is greater than the one being removed, it
			// needs
			// to be adjusted.
			--selectedIndex;
		}
		return true;
	}

	@Override
	public void insert(final Widget child, Tab tab, int beforeIndex) {
		assert(beforeIndex >= 0) && (beforeIndex <= getWidgetCount()) : "beforeIndex out of bounds";

		// Check to see if the TabPanel already contains the Widget. If so,
		// remove it and see if we need to shift the position to the left.
		int idx = getWidgetIndex(child);
		if (idx != -1) {
			remove(child);
			if (idx < beforeIndex) {
				beforeIndex--;
			}
		}

		deckPanel.insertProtected(child, beforeIndex);
		tabs.add(beforeIndex, tab);

		tabBar.setWidget(0, beforeIndex, tab);

		tab.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent event) {
				selectTab(child);
			}
		});

		child.addStyleName(CONTENT_STYLE);

		if (selectedIndex == -1) {
			selectTab(0);
		} else if (selectedIndex >= beforeIndex) {
			// If we inserted before the currently selected tab, its index has
			// just
			// increased.
			selectedIndex++;
		}
	}

	@Override
	public String getTabStyle() {
		return TAB_STYLE;
	}

	@Override
	public String getTabInnerStyle() {
		return TAB_INNER_STYLE;
	}
}
