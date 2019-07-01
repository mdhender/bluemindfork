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

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.BlurHandler;
import com.google.gwt.event.dom.client.DomEvent.Type;
import com.google.gwt.event.dom.client.FocusEvent;
import com.google.gwt.event.dom.client.FocusHandler;

/**
 * Implementation used for all versions of Internet Explorer.
 */
public class ViewportImplIE implements ViewportImpl {

	private static JavaScriptObject dispatchFocusEvent;
	private static JavaScriptObject dispatchBlurEvent;
	private static JavaScriptObject activeElement;
	private static boolean lastEventWasBlur = false;

	// special DomEvent subclasses for IE window focus events
	private static final class FocusInEvent extends FocusEvent {
	}

	private static final class FocusOutEvent extends BlurEvent {
	}

	@Override
	public void addEventListeners() {

		// we create these to map the event names to their events. see
		// DomEvent.Type for details.
		new Type<FocusHandler>("focusin", new FocusInEvent());
		new Type<BlurHandler>("focusout", new FocusOutEvent());

		addEventListeners_();
	}

	public native void addEventListeners_()
	/*-{
		@net.bluemind.ui.im.client.viewport.ViewportImplIE::dispatchFocusEvent = $entry(function(
				evt) {
	
			// only focus if previous event was a blur or we get lots of focus events
			if (@net.bluemind.ui.im.client.viewport.ViewportImplIE::lastEventWasBlur) {
				@net.bluemind.ui.im.client.viewport.ViewportImplIE::lastEventWasBlur = false;
				@net.bluemind.ui.im.client.viewport.Viewport::dispatchFocusEvent(Lcom/google/gwt/user/client/Event;)($wnd.event);
			}
	
		});
	
		@net.bluemind.ui.im.client.viewport.ViewportImplIE::dispatchBlurEvent = $entry(function(
				evt) {
	
			// see http://code.google.com/p/google-web-toolkit/issues/detail?id=68
			if (@net.bluemind.ui.im.client.viewport.ViewportImplIE::activeElement
					&& @net.bluemind.ui.im.client.viewport.ViewportImplIE::activeElement != $doc.activeElement) {
				@net.bluemind.ui.im.client.viewport.ViewportImplIE::activeElement = $doc.activeElement;
			} else {
				@net.bluemind.ui.im.client.viewport.ViewportImplIE::lastEventWasBlur = true;
				@net.bluemind.ui.im.client.viewport.Viewport::dispatchBlurEvent(Lcom/google/gwt/user/client/Event;)($wnd.event);
			}
	
		});
	
		$doc
				.attachEvent("onfocusin",
						@net.bluemind.ui.im.client.viewport.ViewportImplIE::dispatchFocusEvent);
		$doc
				.attachEvent("onfocusout",
						@net.bluemind.ui.im.client.viewport.ViewportImplIE::dispatchBlurEvent);
	}-*/;

}
