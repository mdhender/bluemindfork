/*
 * Copyright 2010 Traction Software, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package net.bluemind.ui.im.client.viewport;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.BlurHandler;
import com.google.gwt.event.dom.client.DomEvent;
import com.google.gwt.event.dom.client.FocusEvent;
import com.google.gwt.event.dom.client.FocusHandler;
import com.google.gwt.event.dom.client.HasBlurHandlers;
import com.google.gwt.event.dom.client.HasFocusHandlers;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Event;

/**
 * Provides a browser Window like object that tracks focus and supports focus
 * and blur events handlers.
 */
public class Viewport implements HasFocusHandlers, HasBlurHandlers {

	static final ViewportImpl impl = GWT.create(ViewportImpl.class);

	// ----------------------------------------------------------------------
	// singleton

	private static Viewport instance;

	/**
	 * Provides access to the singleton instance.
	 *
	 * @return returns the instance
	 */
	public static Viewport get() {
		if (instance == null) {
			instance = new Viewport();
			impl.addEventListeners();
		}
		return instance;
	}

	/**
	 * Determines whether or not the Viewport has focus.
	 *
	 * @return returns true if the Viewport has focus
	 */
	public static boolean hasFocus() {
		return get().hasFocus;
	}

	// ----------------------------------------------------------------------

	// keep track of focus so that hasFocus() can work
	private boolean hasFocus = true;

	// only allow get() to construct this
	private Viewport() {
	}

	/**
	 * Adds a {@link FocusEvent} handler.
	 * 
	 * @param handler
	 *            the handler
	 * @return returns the handler registration
	 */
	@Override
	public HandlerRegistration addFocusHandler(FocusHandler handler) {
		return ensureHandlers().addHandler(FocusEvent.getType(), handler);
	}

	/**
	 * Adds a {@link BlurEvent} handler.
	 * 
	 * @param handler
	 *            the handler
	 * @return returns the handler registration
	 */
	@Override
	public HandlerRegistration addBlurHandler(BlurHandler handler) {
		return ensureHandlers().addHandler(BlurEvent.getType(), handler);
	}

	// ----------------------------------------------------------------------
	// event callbacks from ViewportImpl

	static void dispatchFocusEvent(Event event) {
		dispatchEvent(event, true);
	}

	static void dispatchBlurEvent(Event event) {
		dispatchEvent(event, false);
	}

	static void dispatchEvent(Event event, boolean hasFocus) {
		if (instance != null) {
			instance.hasFocus = hasFocus;
			DomEvent.fireNativeEvent(event, instance);
		}
	}

	// ----------------------------------------------------------------------
	// borrowed from GWT

	private HandlerManager handlerManager;

	@Override
	public void fireEvent(GwtEvent<?> event) {
		if (handlerManager != null) {
			handlerManager.fireEvent(event);
		}
	}

	/**
	 * Ensures the existence of the handler manager.
	 * 
	 * @return the handler manager
	 */
	HandlerManager ensureHandlers() {
		return handlerManager == null ? handlerManager = new HandlerManager(this) : handlerManager;
	}

	HandlerManager getHandlerManager() {
		return handlerManager;
	}

}
