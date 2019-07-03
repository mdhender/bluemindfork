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
package net.bluemind.gwtconsoleapp.base.editor.gwt;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.Composite;

import net.bluemind.gwtconsoleapp.base.editor.ContainerElement;
import net.bluemind.gwtconsoleapp.base.editor.JsHelper;
import net.bluemind.gwtconsoleapp.base.editor.WidgetElement;

public abstract class GwtContainerElement extends Composite implements IGwtContainerElement {

	protected final ContainerElement containerElement;

	public GwtContainerElement(ContainerElement container) {
		this.containerElement = container;
	}

	@Override
	public void attach(Element parent) {
		DOM.appendChild(parent, getElement());
		onAttach();
		GWT.log("attache container with " + getChildrens().length());
		JsArray<WidgetElement> childs = getChildrens();
		for (int i = 0; i < childs.length(); i++) {
			attachChild(childs.get(i));
		}

	}

	protected abstract void attachChild(WidgetElement widgetElement);

	@Override
	public void loadModel(JavaScriptObject model) {

	}

	@Override
	public void saveModel(JavaScriptObject model) {

	}

	@Override
	public void detach() {

	}

	@Override
	public JsArray<WidgetElement> getChildrens() {
		return containerElement.getChildrens();
	}

	public static void register(String className, IGwtDelegateFactory<IGwtContainerElement, ContainerElement> factory) {
		String packageName = className.substring(0, className.lastIndexOf('.'));
		String classSimpleName = className.substring(className.lastIndexOf('.') + 1);

		JavaScriptObject jsPackage = JsHelper.createPackage(packageName);
		registerType(jsPackage, classSimpleName, new GwtFactory<>(factory));
	}

	private static native void registerType(JavaScriptObject jsPackage, String className,
			GwtFactory<IGwtContainerElement, ContainerElement> factory)

	/*-{
	
		jsPackage[className] = function(model, context) {
			$wnd.bm.ContainerElement.call(this, model, context);
			var nat = factory.@net.bluemind.gwtconsoleapp.base.editor.gwt.GwtFactory::create(Lcom/google/gwt/core/client/JavaScriptObject;)(this);
			this["widget_"] = nat;
		}
	
		jsPackage[className].prototype = new $wnd.bm.ContainerElement();
	
		jsPackage[className].prototype.attach = function(parent) {
			this["widget_"].@net.bluemind.gwtconsoleapp.base.editor.gwt.GwtContainerElement::attach(Lcom/google/gwt/dom/client/Element;)(parent);
		};
	
		jsPackage[className].prototype.loadModel = function(model) {
			this["widget_"].@net.bluemind.gwtconsoleapp.base.editor.gwt.GwtContainerElement::loadModel(Lcom/google/gwt/core/client/JavaScriptObject;)(model);
			for (var i = 0; i < this['childrens'].length; i++) {
				this['childrens'][i].loadModel(model);
			}
		};
	
		jsPackage[className].prototype.saveModel = function(model) {
			this["widget_"].@net.bluemind.gwtconsoleapp.base.editor.gwt.GwtContainerElement::saveModel(Lcom/google/gwt/core/client/JavaScriptObject;)(model);
			for (var i = 0; i < this['childrens'].length; i++) {
				this['childrens'][i].saveModel(model);
			}
		};
	
		jsPackage[className].contribute = function(elt, attribute, contribution) {
			@net.bluemind.gwtconsoleapp.base.editor.ContainerElement::contribute(Lnet/bluemind/gwtconsoleapp/base/editor/ContainerElement;Ljava/lang/String;Lnet/bluemind/gwtconsoleapp/base/editor/ScreenElement;)(elt,attribute,contribution);
		};
	}-*/;

	@Override
	public void show() {

	}

}
