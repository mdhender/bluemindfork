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

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Element;

import net.bluemind.gwtconsoleapp.base.editor.JsHelper;
import net.bluemind.gwtconsoleapp.base.editor.WidgetElement;

public class GwtWidgetElement {

	protected static final class GwtWidgetElementWrapper implements IGwtWidgetElement {
		private IGwtWidgetElement delegate;

		public GwtWidgetElementWrapper(IGwtWidgetElement delegate) {
			this.delegate = delegate;
		}

		public void attach(Element parent) {
			delegate.attach(parent);
		}

		public void loadModel(JavaScriptObject model) {
			delegate.loadModel(model);
		}

		public void saveModel(JavaScriptObject model) {
			delegate.saveModel(model);
		}

		@Override
		public void detach() {
			delegate.detach();
		}

		@Override
		public void show() {
			delegate.show();
		}

	}

	public static void register(String className, IGwtDelegateFactory<IGwtWidgetElement, WidgetElement> factory) {
		String packageName = className.substring(0, className.lastIndexOf('.'));
		String classSimpleName = className.substring(className.lastIndexOf('.') + 1);

		JavaScriptObject jsPackage = JsHelper.createPackage(packageName);
		registerType(jsPackage, classSimpleName, new GwtFactory<>(factory));
	}

	private static native void registerType(JavaScriptObject jsPackage, String className,
			GwtFactory<IGwtWidgetElement, WidgetElement> factory)

	/*-{
	
	jsPackage[className] = function(model) {
	  $wnd.bm.WidgetElement.call(this, model);
	  var nat = factory.@net.bluemind.gwtconsoleapp.base.editor.gwt.GwtFactory::create(Lcom/google/gwt/core/client/JavaScriptObject;)(this);
	  this["widget_"] = @net.bluemind.gwtconsoleapp.base.editor.gwt.GwtWidgetElement.GwtWidgetElementWrapper::new(Lnet/bluemind/gwtconsoleapp/base/editor/gwt/IGwtWidgetElement;)(nat);
	}
	
	jsPackage[className].prototype = new $wnd.bm.WidgetElement();
	
	jsPackage[className].prototype.attach = function(parent) {
	  this["widget_"].@net.bluemind.gwtconsoleapp.base.editor.gwt.GwtWidgetElement.GwtWidgetElementWrapper::attach(Lcom/google/gwt/dom/client/Element;)(parent);
	};
	
	jsPackage[className].prototype.detach = function() {
	  this["widget_"].@net.bluemind.gwtconsoleapp.base.editor.gwt.GwtWidgetElement.GwtWidgetElementWrapper::detach()();
	};
	
	jsPackage[className].prototype.loadModel = function(model) {
	  this["widget_"].@net.bluemind.gwtconsoleapp.base.editor.gwt.GwtWidgetElement.GwtWidgetElementWrapper::loadModel(Lcom/google/gwt/core/client/JavaScriptObject;)(model);
	};
	jsPackage[className].prototype.saveModel = function(model) {
	  this["widget_"].@net.bluemind.gwtconsoleapp.base.editor.gwt.GwtWidgetElement.GwtWidgetElementWrapper::saveModel(Lcom/google/gwt/core/client/JavaScriptObject;)(model);
	};
	
	jsPackage[className].prototype.show = function() {
	  this["widget_"].@net.bluemind.gwtconsoleapp.base.editor.gwt.GwtWidgetElement.GwtWidgetElementWrapper::show()();
	};
	}-*/;

}
