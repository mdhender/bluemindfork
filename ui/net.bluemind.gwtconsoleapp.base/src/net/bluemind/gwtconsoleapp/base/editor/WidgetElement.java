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
package net.bluemind.gwtconsoleapp.base.editor;

import com.google.gwt.dom.client.Element;

public class WidgetElement extends ScreenElement {

	protected WidgetElement() {
	}

	public final native void show()
	/*-{
		if( this['show']) {
			this['show']();
		}
	}-*/;

	public final native int getIndex()
	/*-{
		return this['index'];
	}-*/;

	public final native void attach(Element parent)
	/*-{
		return this['attach'](parent);
	}-*/;

	public final native void detach()
	/*-{
		return this['detach']();
	}-*/;

	private static native void rt()
	/*-{
		// constructor
		$wnd.bm.WidgetElement = function(model) {
			$wnd.bm.ScreenElement.call(this, model);
			if (model) {
				this['index'] = model['index'];
			}
		}
	
		// prototype
		$wnd.bm.WidgetElement.prototype = new $wnd.bm.ScreenElement();
		$wnd.bm.WidgetElement.prototype.attach = function(parent) {
			var e = document.createElement("div");
			e.innertHTML = "not implemented !!!!";
			parent.appendChild(e);
		}
	}-*/;

	public static void registerType() {
		JsHelper.createPackage("bm");
		rt();
	}
}
