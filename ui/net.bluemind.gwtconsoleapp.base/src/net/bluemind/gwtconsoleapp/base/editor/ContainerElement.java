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

import com.google.gwt.core.client.JsArray;

public class ContainerElement extends WidgetElement {

	protected ContainerElement() {
	}

	public final native JsArray<WidgetElement> getChildrens()
	/*-{
	return this["childrens"];
	}-*/;

	public static void registerType() {
		JsHelper.createPackage("bm");
		rt();
	}

	private static native void rt()
	/*-{
	$wnd.bm.ContainerElement = function(model, context) {
	  $wnd.bm.WidgetElement.call(this, model, context);
	  this['childrens'] = [];
	  if (model && model['childrens']) {
	    for (var i = 0; i < model['childrens'].length; i++) {
	      var child = @net.bluemind.gwtconsoleapp.base.editor.ScreenElement::build(Lnet/bluemind/gwtconsoleapp/base/editor/ScreenElement;Lnet/bluemind/gwtconsoleapp/base/editor/EditorContext;)(model['childrens'][i], context);
	      if (child != null) {
	        if (model['childrens'][i]['title']) {
	          child['title'] = model['childrens'][i]['title'];
	        } else {
	          console.log("no title for " + model['childrens'][i]['type']);
	        }
	        this['childrens'].push(child);
	      }
	    }
	
	  }
	}
	
	$wnd.bm.ContainerElement.prototype = new $wnd.bm.WidgetElement();
	$wnd.bm.ContainerElement.prototype.attach = function(parent) {
	  var comp = document.createElement("div");
	  parent.appendChild(comp);
	  for (var i = 0; i < this['childrens'].length; i++) {
	    if (this['childrens'][i]['title']) {
	      var title = document.createElement("div");
	      title.classList.add('sectionTitle');
	      title.appendChild(document.createTextNode(this['childrens'][i]['title']));
	      comp.appendChild(title);
	    }
	    this['childrens'][i].attach(comp);
	  }
	};
	
	$wnd.bm.ContainerElement.prototype.saveModel = function(model) {
	  for (var i = 0; i < this['childrens'].length; i++) {
	    this['childrens'][i].saveModel(model);
	  }
	};
	$wnd.bm.ContainerElement.prototype.loadModel = function(model) {
	  for (var i = 0; i < this['childrens'].length; i++) {
	    this['childrens'][i].loadModel(model);
	  }
	};
	
	$wnd.bm.ContainerElement.contribute = function(elt, attribute, contribution) {
	  @net.bluemind.gwtconsoleapp.base.editor.ContainerElement::contribute(Lnet/bluemind/gwtconsoleapp/base/editor/ContainerElement;Ljava/lang/String;Lnet/bluemind/gwtconsoleapp/base/editor/ScreenElement;)(elt,attribute,contribution);
	};
	}-*/;

	private static void contribute(ContainerElement comp, String attribute, ScreenElement contribution) {

		if ("childrens".equals(attribute) || attribute == null) {
			WidgetElement el = contribution.cast();
			comp.getChildrens().push(el);
		}

	}

	public static native ContainerElement create(String id, JsArray<ScreenElement> childrens)
	/*-{
	return {
	  'id' : id,
	  'type' : 'bm.ContainerElement',
	  'childrens' : childrens
	};
	}-*/;

	public static native ContainerElement createWithType(String id, String type, JsArray<ScreenElement> childrens)
	/*-{
	return {
	  'id' : id,
	  'type' : type,
	  'childrens' : childrens
	};
	}-*/;

}
