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
package net.bluemind.ui.common.client;

import java.util.LinkedList;
import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.AbsolutePanel;
import com.google.gwt.user.client.ui.RootLayoutPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * Modal progress dialog with task log output.
 * 
 * 
 */
public class OverlayScreen extends AbsolutePanel {

	private Widget dlp;

	private AbsolutePanel ap;

	public static final int DEF_WIDTH = 720;
	public static final int DEF_HEIGHT = 320;

	private List<IHideListener> hideListeners;

	private int width;
	private int height;

	public OverlayScreen(Widget widget, int width, int height) {
		this.width = width;
		this.height = height;
		this.hideListeners = new LinkedList<IHideListener>();
		setWidth(Window.getClientWidth() + "px");
		setHeight(Window.getClientHeight() + "px");
		addStyleName("modalOverlay");

		this.dlp = widget;
		dlp.asWidget().addStyleName("dialog");
		dlp.asWidget().setSize(width + "px", height + "px");
	}

	public void center() {
		RootLayoutPanel rlp = RootLayoutPanel.get();
		rlp.add(this);

		int left = (Window.getClientWidth() - width) >> 1;
		int top = (Window.getClientHeight() - height) >> 1;
		left = Math.max(Window.getScrollLeft() + left, 0);
		top = Math.max(Window.getScrollTop() + top, 0);

		this.ap = new AbsolutePanel();
		ap.add(dlp, left, top);
		rlp.add(ap);
	}

	/**
	 * Calling hide will call onScreenShow on the screen displayed under the
	 * overlay. For 'create & edit' actions, hide must be called before
	 * switching to the edit screen.
	 */
	public void hide() {
		hide(false);
	}

	public void hide(boolean skipNotify) {
		if (ap != null) {
			RootLayoutPanel.get().remove(ap);
		}
		RootLayoutPanel.get().remove(this);

		if (!skipNotify) {
			for (IHideListener hl : hideListeners) {
				hl.overlayHidden();
			}
		}
		destroy();
	}

	public void registerHideListener(IHideListener hl) {
		hideListeners.add(hl);
	}

	private void destroy() {
		if (hideListeners == null) {
			// we are already destroyed, A reset may have been triggered by
			// browser back button
			return;
		}
		hideListeners.clear();
		hideListeners = null;
		dlp = null;
		ap.clear();
		ap = null;
		GWT.log("overlay destroyed");
	}

}
