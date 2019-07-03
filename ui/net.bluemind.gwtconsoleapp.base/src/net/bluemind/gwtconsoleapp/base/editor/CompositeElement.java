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

public class CompositeElement extends WidgetElement {

	protected CompositeElement() {
	}

	public static void registerType() {
		JsHelper.createPackage("bm");
		rt();
	}

	public native final WidgetElement getContent()
	/*-{
		return this['content'];
	}-*/;

	public native final void setContent(WidgetElement el)
	/*-{
		this['content'] = el;
	}-*/;

	private static native void rt()
	/*-{
	
		$wnd.bm.CompositeElement = function(model, context) {
			$wnd.bm.WidgetElement.call(this, model, context);
			if (model && model['content']) {
				this['content'] = @net.bluemind.gwtconsoleapp.base.editor.CompositeElement::createContent(Lnet/bluemind/gwtconsoleapp/base/editor/WidgetElement;Lnet/bluemind/gwtconsoleapp/base/editor/EditorContext;)(model['content'], context);
			}
		}
	
		$wnd.bm.CompositeElement.prototype = new $wnd.bm.WidgetElement();
		$wnd.bm.CompositeElement.prototype.attach = function(parent) {
			var comp = document.createElement("div");
			comp.className = "deubg composit";
			parent.appendChild(comp);
			this['content'].attach(comp);
	
		};
	
		$wnd.bm.CompositeElement.prototype.saveModel = function(model) {
			if( this['content']) {
				this['content'].saveModel(model);
			}
		};
		$wnd.bm.CompositeElement.prototype.loadModel = function(model) {
			if( this['content']) {
				this['content'].loadModel(model);
			}
		};
	
		$wnd.bm.CompositeElement.contribute = function(elt, attribute,
				contribution) {
			@net.bluemind.gwtconsoleapp.base.editor.CompositeElement::contribute(Lnet/bluemind/gwtconsoleapp/base/editor/CompositeElement;Ljava/lang/String;Lnet/bluemind/gwtconsoleapp/base/editor/ScreenElement;)(elt, attribute, contribution);
		};
	}-*/;

	private static void contribute(CompositeElement comp, String attribute, ScreenElement contribution) {

		if ("content".equals(attribute) || attribute == null) {
			WidgetElement el = contribution.cast();
			comp.setContent(el);
		}

	}

	protected static WidgetElement createContent(WidgetElement e, EditorContext context) {
		ScreenElement ret = ScreenElement.build(e, context);
		if (ret != null) {
			return ScreenElement.build(e, context).cast();
		} else {
			return null;
		}
	}

}
