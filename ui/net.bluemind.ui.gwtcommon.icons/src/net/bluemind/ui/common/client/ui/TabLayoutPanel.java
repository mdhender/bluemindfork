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

import java.util.ArrayList;
import java.util.Iterator;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.BeforeSelectionEvent;
import com.google.gwt.event.logical.shared.BeforeSelectionHandler;
import com.google.gwt.event.logical.shared.HasBeforeSelectionHandlers;
import com.google.gwt.event.logical.shared.HasSelectionHandlers;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.layout.client.Layout.AnimationCallback;
import com.google.gwt.resources.client.CommonResources;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.user.client.ui.AnimatedLayout;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DeckLayoutPanel;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasWidgets;
import com.google.gwt.user.client.ui.IndexedPanel;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ProvidesResize;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;

public abstract class TabLayoutPanel extends Composite implements HasWidgets, ProvidesResize, IndexedPanel.ForIsWidget,
		AnimatedLayout, HasBeforeSelectionHandlers<Integer>, HasSelectionHandlers<Integer> {

	protected class Tab extends SimplePanel {
		private Element inner;
		private boolean replacingWidget;

		public Tab(Widget child) {
			super(Document.get().createDivElement());
			getElement().appendChild(inner = Document.get().createDivElement());

			setWidget(child);
			setStyleName(getTabStyle());
			inner.setClassName(getTabInnerStyle());

			getElement().addClassName(CommonResources.getInlineBlockStyle());
		}

		public HandlerRegistration addClickHandler(ClickHandler handler) {
			return addDomHandler(handler, ClickEvent.getType());
		}

		@Override
		public boolean remove(Widget w) {
			/*
			 * Removal of items from the TabBar is delegated to the
			 * TabLayoutPanel to ensure consistency.
			 */
			int index = tabs.indexOf(this);
			if (replacingWidget || index < 0) {
				/*
				 * The tab contents are being replaced, or this tab is no longer
				 * in the panel, so just remove the widget.
				 */
				return super.remove(w);
			} else {
				// Delegate to the TabLayoutPanel.
				return TabLayoutPanel.this.remove(index);
			}
		}

		public void setSelected(boolean selected) {
			if (selected) {
				addStyleDependentName("selected");
			} else {
				removeStyleDependentName("selected");
			}
		}

		@Override
		public void setWidget(Widget w) {
			replacingWidget = true;
			super.setWidget(w);
			replacingWidget = false;
		}

		@Override
		protected com.google.gwt.user.client.Element getContainerElement() {
			return inner.cast();
		}
	}

	/**
	 * This extension of DeckLayoutPanel overrides the public mutator methods to
	 * prevent external callers from adding to the state of the DeckPanel.
	 * <p>
	 * Removal of Widgets is supported so that WidgetCollection.WidgetIterator
	 * operates as expected.
	 * </p>
	 * <p>
	 * We ensure that the DeckLayoutPanel cannot become of of sync with its
	 * associated TabBar by delegating all mutations to the TabBar to this
	 * implementation of DeckLayoutPanel.
	 * </p>
	 */
	protected class TabbedDeckLayoutPanel extends DeckLayoutPanel {

		@Override
		public void add(Widget w) {
			throw new UnsupportedOperationException("Use TabLayoutPanel.add() to alter the DeckLayoutPanel");
		}

		@Override
		public void clear() {
			throw new UnsupportedOperationException("Use TabLayoutPanel.clear() to alter the DeckLayoutPanel");
		}

		@Override
		public void insert(Widget w, int beforeIndex) {
			throw new UnsupportedOperationException("Use TabLayoutPanel.insert() to alter the DeckLayoutPanel");
		}

		@Override
		public boolean remove(Widget w) {
			/*
			 * Removal of items from the DeckLayoutPanel is delegated to the
			 * TabLayoutPanel to ensure consistency.
			 */
			return TabLayoutPanel.this.remove(w);
		}

		public void insertProtected(Widget w, int beforeIndex) {
			super.insert(w, beforeIndex);
		}

		public void removeProtected(Widget w) {
			super.remove(w);
		}
	}

	public abstract String getTabStyle();

	public abstract String getTabInnerStyle();

	protected FlowPanel tabBar;
	protected final TabbedDeckLayoutPanel deckPanel = new TabbedDeckLayoutPanel();
	protected final ArrayList<Tab> tabs = new ArrayList<Tab>();
	protected int selectedIndex = -1;

	public TabLayoutPanel() {

	}

	/**
	 * Convenience overload to allow {@link IsWidget} to be used directly.
	 */
	public void add(IsWidget w) {
		add(asWidgetOrNull(w));
	}

	/**
	 * Convenience overload to allow {@link IsWidget} to be used directly.
	 */
	public void add(IsWidget w, IsWidget tab) {
		add(asWidgetOrNull(w), asWidgetOrNull(tab));
	}

	/**
	 * Convenience overload to allow {@link IsWidget} to be used directly.
	 */
	public void add(IsWidget w, String text) {
		add(asWidgetOrNull(w), text);
	}

	/**
	 * Convenience overload to allow {@link IsWidget} to be used directly.
	 */
	public void add(IsWidget w, String text, boolean asHtml) {
		add(asWidgetOrNull(w), text, asHtml);
	}

	public void add(Widget w) {
		insert(w, getWidgetCount());
	}

	/**
	 * Adds a widget to the panel. If the Widget is already attached, it will be
	 * moved to the right-most index.
	 * 
	 * @param child
	 *            the widget to be added
	 * @param text
	 *            the text to be shown on its tab
	 */
	public void add(Widget child, String text) {
		insert(child, text, getWidgetCount());
	}

	/**
	 * Adds a widget to the panel. If the Widget is already attached, it will be
	 * moved to the right-most index.
	 * 
	 * @param child
	 *            the widget to be added
	 * @param html
	 *            the html to be shown on its tab
	 */
	public void add(Widget child, SafeHtml html) {
		add(child, html.asString(), true);
	}

	/**
	 * Adds a widget to the panel. If the Widget is already attached, it will be
	 * moved to the right-most index.
	 * 
	 * @param child
	 *            the widget to be added
	 * @param text
	 *            the text to be shown on its tab
	 * @param asHtml
	 *            <code>true</code> to treat the specified text as HTML
	 */
	public void add(Widget child, String text, boolean asHtml) {
		insert(child, text, asHtml, getWidgetCount());
	}

	/**
	 * Adds a widget to the panel. If the Widget is already attached, it will be
	 * moved to the right-most index.
	 * 
	 * @param child
	 *            the widget to be added
	 * @param tab
	 *            the widget to be placed in the associated tab
	 */
	public void add(Widget child, Widget tab) {
		insert(child, tab, getWidgetCount());
	}

	public HandlerRegistration addBeforeSelectionHandler(BeforeSelectionHandler<Integer> handler) {
		return addHandler(handler, BeforeSelectionEvent.getType());
	}

	public HandlerRegistration addSelectionHandler(SelectionHandler<Integer> handler) {
		return addHandler(handler, SelectionEvent.getType());
	}

	public void animate(int duration) {
		animate(duration, null);
	}

	public void animate(int duration, AnimationCallback callback) {
		deckPanel.animate(duration, callback);
	}

	public void clear() {
		Iterator<Widget> it = iterator();
		while (it.hasNext()) {
			it.next();
			it.remove();
		}
	}

	public void forceLayout() {
		deckPanel.forceLayout();
	}

	/**
	 * Get the duration of the animated transition between tabs.
	 * 
	 * @return the duration in milliseconds
	 */
	public int getAnimationDuration() {
		return deckPanel.getAnimationDuration();
	}

	/**
	 * Gets the index of the currently-selected tab.
	 * 
	 * @return the selected index, or <code>-1</code> if none is selected.
	 */
	public int getSelectedIndex() {
		return selectedIndex;
	}

	/**
	 * Gets the widget in the tab at the given index.
	 * 
	 * @param index
	 *            the index of the tab to be retrieved
	 * @return the tab's widget
	 */
	public Widget getTabWidget(int index) {
		checkIndex(index);
		return tabs.get(index).getWidget();
	}

	/**
	 * Convenience overload to allow {@link IsWidget} to be used directly.
	 */
	public Widget getTabWidget(IsWidget child) {
		return getTabWidget(asWidgetOrNull(child));
	}

	/**
	 * Gets the widget in the tab associated with the given child widget.
	 * 
	 * @param child
	 *            the child whose tab is to be retrieved
	 * @return the tab's widget
	 */
	public Widget getTabWidget(Widget child) {
		checkChild(child);
		return getTabWidget(getWidgetIndex(child));
	}

	/**
	 * Returns the widget at the given index.
	 */
	public Widget getWidget(int index) {
		return deckPanel.getWidget(index);
	}

	/**
	 * Returns the number of tabs and widgets.
	 */
	public int getWidgetCount() {
		return deckPanel.getWidgetCount();
	}

	/**
	 * Convenience overload to allow {@link IsWidget} to be used directly.
	 */
	public int getWidgetIndex(IsWidget child) {
		return getWidgetIndex(asWidgetOrNull(child));
	}

	/**
	 * Returns the index of the given child, or -1 if it is not a child.
	 */
	public int getWidgetIndex(Widget child) {
		return deckPanel.getWidgetIndex(child);
	}

	/**
	 * Convenience overload to allow {@link IsWidget} to be used directly.
	 */
	public void insert(IsWidget child, int beforeIndex) {
		insert(asWidgetOrNull(child), beforeIndex);
	}

	/**
	 * Convenience overload to allow {@link IsWidget} to be used directly.
	 */
	public void insert(IsWidget child, IsWidget tab, int beforeIndex) {
		insert(asWidgetOrNull(child), asWidgetOrNull(tab), beforeIndex);
	}

	/**
	 * Convenience overload to allow {@link IsWidget} to be used directly.
	 */
	public void insert(IsWidget child, String text, boolean asHtml, int beforeIndex) {
		insert(asWidgetOrNull(child), text, asHtml, beforeIndex);
	}

	/**
	 * Convenience overload to allow {@link IsWidget} to be used directly.
	 */
	public void insert(IsWidget child, String text, int beforeIndex) {
		insert(asWidgetOrNull(child), text, beforeIndex);
	}

	/**
	 * Inserts a widget into the panel. If the Widget is already attached, it
	 * will be moved to the requested index.
	 * 
	 * @param child
	 *            the widget to be added
	 * @param beforeIndex
	 *            the index before which it will be inserted
	 */
	public void insert(Widget child, int beforeIndex) {
		insert(child, "", beforeIndex);
	}

	/**
	 * Inserts a widget into the panel. If the Widget is already attached, it
	 * will be moved to the requested index.
	 * 
	 * @param child
	 *            the widget to be added
	 * @param html
	 *            the html to be shown on its tab
	 * @param beforeIndex
	 *            the index before which it will be inserted
	 */
	public void insert(Widget child, SafeHtml html, int beforeIndex) {
		insert(child, html.asString(), true, beforeIndex);
	}

	/**
	 * Inserts a widget into the panel. If the Widget is already attached, it
	 * will be moved to the requested index.
	 * 
	 * @param child
	 *            the widget to be added
	 * @param text
	 *            the text to be shown on its tab
	 * @param asHtml
	 *            <code>true</code> to treat the specified text as HTML
	 * @param beforeIndex
	 *            the index before which it will be inserted
	 */
	public void insert(Widget child, String text, boolean asHtml, int beforeIndex) {
		Widget contents;
		if (asHtml) {
			contents = new HTML(text);
		} else {
			contents = new Label(text);
		}
		insert(child, contents, beforeIndex);
	}

	/**
	 * Inserts a widget into the panel. If the Widget is already attached, it
	 * will be moved to the requested index.
	 * 
	 * @param child
	 *            the widget to be added
	 * @param text
	 *            the text to be shown on its tab
	 * @param beforeIndex
	 *            the index before which it will be inserted
	 */
	public void insert(Widget child, String text, int beforeIndex) {
		insert(child, text, false, beforeIndex);
	}

	/**
	 * Inserts a widget into the panel. If the Widget is already attached, it
	 * will be moved to the requested index.
	 * 
	 * @param child
	 *            the widget to be added
	 * @param tab
	 *            the widget to be placed in the associated tab
	 * @param beforeIndex
	 *            the index before which it will be inserted
	 */
	public void insert(Widget child, Widget tab, int beforeIndex) {
		insert(child, new Tab(tab), beforeIndex);
	}

	/**
	 * Check whether or not transitions slide in vertically or horizontally.
	 * Defaults to horizontally.
	 * 
	 * @return true for vertical transitions, false for horizontal
	 */
	public boolean isAnimationVertical() {
		return deckPanel.isAnimationVertical();
	}

	public Iterator<Widget> iterator() {
		return deckPanel.iterator();
	}

	public boolean remove(Widget w) {
		int index = getWidgetIndex(w);
		if (index == -1) {
			return false;
		}

		return remove(index);
	}

	/**
	 * Programmatically selects the specified tab and fires events.
	 * 
	 * @param index
	 *            the index of the tab to be selected
	 */
	public void selectTab(int index) {
		selectTab(index, true);
	}

	/**
	 * Programmatically selects the specified tab.
	 * 
	 * @param index
	 *            the index of the tab to be selected
	 * @param fireEvents
	 *            true to fire events, false not to
	 */
	public void selectTab(int index, boolean fireEvents) {
		checkIndex(index);
		if (index == selectedIndex) {
			return;
		}

		// Fire the before selection event, giving the recipients a chance to
		// cancel the selection.
		if (fireEvents) {
			BeforeSelectionEvent<Integer> event = BeforeSelectionEvent.fire(this, index);
			if ((event != null) && event.isCanceled()) {
				return;
			}
		}

		// Update the tabs being selected and unselected.
		if (selectedIndex != -1) {
			tabs.get(selectedIndex).setSelected(false);
		}

		deckPanel.showWidget(index);
		tabs.get(index).setSelected(true);
		selectedIndex = index;

		// Fire the selection event.
		if (fireEvents) {
			SelectionEvent.fire(this, index);
		}
	}

	/**
	 * Convenience overload to allow {@link IsWidget} to be used directly.
	 */
	public void selectTab(IsWidget child) {
		selectTab(asWidgetOrNull(child));
	}

	/**
	 * Convenience overload to allow {@link IsWidget} to be used directly.
	 */
	public void selectTab(IsWidget child, boolean fireEvents) {
		selectTab(asWidgetOrNull(child), fireEvents);
	}

	/**
	 * Programmatically selects the specified tab and fires events.
	 * 
	 * @param child
	 *            the child whose tab is to be selected
	 */
	public void selectTab(Widget child) {
		selectTab(getWidgetIndex(child));
	}

	/**
	 * Programmatically selects the specified tab.
	 * 
	 * @param child
	 *            the child whose tab is to be selected
	 * @param fireEvents
	 *            true to fire events, false not to
	 */
	public void selectTab(Widget child, boolean fireEvents) {
		selectTab(getWidgetIndex(child), fireEvents);
	}

	/**
	 * Set the duration of the animated transition between tabs.
	 * 
	 * @param duration
	 *            the duration in milliseconds.
	 */
	public void setAnimationDuration(int duration) {
		deckPanel.setAnimationDuration(duration);
	}

	/**
	 * Set whether or not transitions slide in vertically or horizontally.
	 * 
	 * @param isVertical
	 *            true for vertical transitions, false for horizontal
	 */
	public void setAnimationVertical(boolean isVertical) {
		deckPanel.setAnimationVertical(isVertical);
	}

	/**
	 * Sets a tab's HTML contents.
	 * 
	 * Use care when setting an object's HTML; it is an easy way to expose
	 * script-based security problems. Consider using
	 * {@link #setTabHTML(int, SafeHtml)} or {@link #setTabText(int, String)}
	 * whenever possible.
	 * 
	 * @param index
	 *            the index of the tab whose HTML is to be set
	 * @param html
	 *            the tab's new HTML contents
	 */
	public void setTabHTML(int index, String html) {
		checkIndex(index);
		tabs.get(index).setWidget(new HTML(html));
	}

	/**
	 * Sets a tab's HTML contents.
	 * 
	 * @param index
	 *            the index of the tab whose HTML is to be set
	 * @param html
	 *            the tab's new HTML contents
	 */
	public void setTabHTML(int index, SafeHtml html) {
		setTabHTML(index, html.asString());
	}

	/**
	 * Sets a tab's text contents.
	 * 
	 * @param index
	 *            the index of the tab whose text is to be set
	 * @param text
	 *            the object's new text
	 */
	public void setTabText(int index, String text) {
		checkIndex(index);
		tabs.get(index).setWidget(new Label(text));
	}

	protected void checkChild(Widget child) {
		assert getWidgetIndex(child) >= 0 : "Child is not a part of this panel";
	}

	protected void checkIndex(int index) {
		assert(index >= 0) && (index < getWidgetCount()) : "Index out of bounds";
	}

	public abstract void insert(final Widget child, Tab tab, int beforeIndex);

	public abstract boolean remove(int index);

}
