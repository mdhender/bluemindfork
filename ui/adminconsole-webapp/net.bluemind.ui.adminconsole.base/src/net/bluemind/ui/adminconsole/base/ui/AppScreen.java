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
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;

import net.bluemind.ui.adminconsole.base.AppState;
import net.bluemind.ui.common.client.SizeHint;

/**
 * Main class for application screens
 * 
 * 
 */
public class AppScreen extends Composite {

	public static class QuickKeyEvent extends JavaScriptObject {
		protected QuickKeyEvent() {
		}

		public final native String getKey()
		/*-{
		return this.key;
		}-*/;

		public native static QuickKeyEvent create(String key)
		/*-{
		return {
		"key" : key
		};
		}-*/
		;
	}

	public interface IQuickKeyListener {
		public void escapeEvent();

		public void enterEvent();

		public void ctrlEnterEvent();
	}

	private AppState state;
	private ScreenShowRequest ssr;

	protected ScreenContext ctx;
	private boolean overlayDisplay;

	public AppScreen(boolean overlay) {
		this.state = AppState.SLEEPING;
		this.overlayDisplay = overlay;
		if (overlay) {
			sinkKeyEvents();
		}

	}

	public AppScreen(boolean overlay, Widget mainWidget) {
		this(overlay);
		initWidget(mainWidget);
	}

	private void sinkKeyEvents() {
		sinkEvents(Event.KEYEVENTS);
		sinkEvents(Event.ONKEYPRESS);
	}

	public final void showScreen(ScreenShowRequest ssr) {
		this.setSsr(ssr);
		state = AppState.ACTIVE;
		// GWT.log("Showing screen with params:\n" + ssr);
		onScreenShown(ssr);
	}

	protected void onScreenShown(ScreenShowRequest ssr) {
		// TODO Auto-generated method stub

	}

	public void construct() {

	}

	public final AppState getState() {
		return state;
	}

	public boolean isOverlayDisplay() {
		return overlayDisplay;
	}

	public SizeHint getSizeHint() {
		return null;
	}

	@Override
	public void onBrowserEvent(Event event) {
		GWT.log("sink event in overlay...");

		if (!isOverlayDisplay()) {
			// tom: don't mess with keyboard when not a popup
			return;
		}

		// Ctrl + Enter = create and Edit
		if (event.getCtrlKey() && (event.getKeyCode() == KeyCodes.KEY_ENTER)
				&& (event.getTypeInt() == Event.ONKEYDOWN)) {
			GWT.log("DOUBLEEE  " + event.getType());
			ctrlEnterEvent();
		}

		// enter = create
		if (!event.getCtrlKey() && (event.getKeyCode() == KeyCodes.KEY_ENTER)
				&& (event.getTypeInt() == Event.ONKEYDOWN)) {
			GWT.log("ENTER  " + event.getType());
			enterEvent();
		}

		// escape = cancel
		if ((event.getKeyCode() == KeyCodes.KEY_ESCAPE) && (event.getTypeInt() == Event.ONKEYDOWN)) {
			GWT.log("CANCEL  " + event.getType());
			escapeEvent();
		}
	}

	public final void escapeEvent() {
		// JsEvent.trigger(getWidget().getElement(), "quick-key",
		// QuickKeyEvent.create("escape"));
	}

	public final void enterEvent() {
		// JsEvent.trigger(getWidget().getElement(), "quick-key",
		// QuickKeyEvent.create("enter"));
	}

	public final void ctrlEnterEvent() {
		// JsEvent.trigger(getWidget().getElement(), "quick-key",
		// QuickKeyEvent.create("ctrl-enter"));
	}

	/**
	 * @return null if the screen can't be extended.
	 */
	public String getExtensionID() {
		return null;
	}

	public void setContext(ScreenContext ctx) {
		this.ctx = ctx;
	}

	public void settingsExtendComplete() {
	}

	public ScreenShowRequest getSsr() {
		return ssr;
	}

	public void setSsr(ScreenShowRequest ssr) {
		this.ssr = ssr;
	}

	public ResetReply doReset() {
		// TODO Auto-generated method stub
		return null;
	}

	protected ResetReply reset() {
		// TODO Auto-generated method stub
		return null;
	}

}
