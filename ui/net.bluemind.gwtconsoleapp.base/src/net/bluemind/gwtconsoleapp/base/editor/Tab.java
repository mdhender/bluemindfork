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

import com.google.gwt.core.shared.GWT;

public class Tab extends CompositeElement {

	protected Tab() {
	}

	private static native void rt()
	/*-{
		$wnd.bm.Tab = function(model, context) {
			$wnd.bm.CompositeElement.call(this, model, context);
			if (model) {
				this['title'] = model['title'];
			}
		}
	
		$wnd.bm.Tab.prototype = new $wnd.bm.CompositeElement();
		$wnd.bm.Tab.prototype.attach = function(parent) {
			if( this['content']) {
				this['content'].attach(parent);
			}
		};
		
		$wnd.bm.Tab.prototype.show = function(parent) {
			if( this['content']) {
			if( this['content']['show'] ) {
				this['content'].show();
			}
			}
		};
	}-*/;

	public static void registerType() {
		rt();
		GWT.log("bm.Tab registred");
	}

	public static native Tab create(String id, String title, ScreenElement content)
	/*-{
		return {
			'id' : id,
			'type' : 'bm.Tab',
			'title' : title,
			'content' : content
		};
	}-*/;

}
