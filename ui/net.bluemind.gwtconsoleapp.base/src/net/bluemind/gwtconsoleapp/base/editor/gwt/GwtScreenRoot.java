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

import net.bluemind.gwtconsoleapp.base.editor.CompositeElement;
import net.bluemind.gwtconsoleapp.base.editor.JsHelper;
import net.bluemind.gwtconsoleapp.base.editor.ScreenElement;
import net.bluemind.gwtconsoleapp.base.editor.ScreenRoot;

public class GwtScreenRoot {

	protected static final class ScreenCompositeRootWidget implements IGwtCompositeScreenRoot {
		private IGwtCompositeScreenRoot delegate;

		public ScreenCompositeRootWidget(IGwtCompositeScreenRoot widget) {
			this.delegate = widget;
		}

		public Element getCenter() {
			return delegate.getCenter();
		}

		@Override
		public void attach(Element e) {
			delegate.attach(e);
		}

		@Override
		public void loadModel(JavaScriptObject model) {
			delegate.loadModel(model);
		}

		@Override
		public void saveModel(JavaScriptObject model) {
			delegate.saveModel(model);
		}

		@Override
		public void doLoad(final ScreenRoot instance) {
			delegate.doLoad(instance);
		}
	}

	protected static final class ScreenRootWidget implements IGwtScreenRoot {
		private IGwtScreenRoot delegate;

		public ScreenRootWidget(IGwtScreenRoot widget) {
			this.delegate = widget;
		}

		@Override
		public void attach(Element e) {
			delegate.attach(e);
		}

		@Override
		public void loadModel(JavaScriptObject model) {
			delegate.loadModel(model);
		}

		@Override
		public void saveModel(JavaScriptObject model) {
			delegate.saveModel(model);
		}

		@Override
		public void doLoad(final ScreenRoot instance) {
			delegate.doLoad(instance);
		}
	}

	public static void registerComposite(String className,
			IGwtDelegateFactory<IGwtCompositeScreenRoot, ScreenRoot> factory) {
		String packageName = className.substring(0, className.lastIndexOf('.'));
		String classSimpleName = className.substring(className.lastIndexOf('.') + 1);

		JavaScriptObject jsPackage = JsHelper.createPackage(packageName);
		registerCompositeType(jsPackage, classSimpleName, new GwtFactory<>(factory));
	}

	public static void register(String className, IGwtDelegateFactory<IGwtScreenRoot, ScreenRoot> factory) {
		String packageName = className.substring(0, className.lastIndexOf('.'));
		String classSimpleName = className.substring(className.lastIndexOf('.') + 1);

		JavaScriptObject jsPackage = JsHelper.createPackage(packageName);
		registerType(jsPackage, classSimpleName, new GwtFactory<>(factory));
	}

	private static void contribute(ScreenElement elt, String attribute, ScreenElement contribution) {
		CompositeElement.contribute((CompositeElement) elt.cast(), attribute, contribution);
		ScreenRoot.contribute(elt, attribute, contribution);
	}

	private static native void registerCompositeType(JavaScriptObject jsPackage, String className,
			GwtFactory<IGwtCompositeScreenRoot, ScreenRoot> factory)

	/*-{
	
		jsPackage[className] = function(model, context) {
			if (!model) {
				model = {};
			}
			$wnd.bm.ScreenRoot.call(this, model, context);
			var nat = factory.@net.bluemind.gwtconsoleapp.base.editor.gwt.GwtFactory::create(Lcom/google/gwt/core/client/JavaScriptObject;)(this);
			this['decor_'] = @net.bluemind.gwtconsoleapp.base.editor.gwt.GwtScreenRoot.ScreenCompositeRootWidget::new(Lnet/bluemind/gwtconsoleapp/base/editor/gwt/IGwtCompositeScreenRoot;)(nat);
			
		}
	
		jsPackage[className].prototype = new $wnd.bm.ScreenRoot();
	
		jsPackage[className].prototype.attach = function(parent) {
			this['decor_'].@net.bluemind.gwtconsoleapp.base.editor.gwt.GwtScreenRoot.ScreenCompositeRootWidget::attach(Lcom/google/gwt/dom/client/Element;)(parent);
			if (this["content"]) {
				var center = this['decor_'].@net.bluemind.gwtconsoleapp.base.editor.gwt.GwtScreenRoot.ScreenCompositeRootWidget::getCenter()();
				this["content"].attach(center);
			}
	
			this['decor_'].@net.bluemind.gwtconsoleapp.base.editor.gwt.GwtScreenRoot.ScreenCompositeRootWidget::doLoad(Lnet/bluemind/gwtconsoleapp/base/editor/ScreenRoot;)(this);
		};
	
		jsPackage[className].prototype.loadModel = function(model) {
			this['decor_'].@net.bluemind.gwtconsoleapp.base.editor.gwt.GwtScreenRoot.ScreenCompositeRootWidget::loadModel(Lcom/google/gwt/core/client/JavaScriptObject;)(model);
			// FIXME should call super.loadModel if it's exists
			if (this["content"]) {
				this['content'].loadModel(model);
			}
	
		};
	
		jsPackage[className].prototype.saveModel = function(model) {
			this['decor_'].@net.bluemind.gwtconsoleapp.base.editor.gwt.GwtScreenRoot.ScreenCompositeRootWidget::saveModel(Lcom/google/gwt/core/client/JavaScriptObject;)(model);
			// FIXME should call super.loadModel if it's exists
			if (this["content"]) {
				this['content'].saveModel(model);
			}
		};
	
		jsPackage[className].contribute = function(elt, attribute, contribution) {
			@net.bluemind.gwtconsoleapp.base.editor.CompositeElement::contribute(Lnet/bluemind/gwtconsoleapp/base/editor/CompositeElement;Ljava/lang/String;Lnet/bluemind/gwtconsoleapp/base/editor/ScreenElement;)(elt, attribute, contribution);
			@net.bluemind.gwtconsoleapp.base.editor.ScreenRoot::contribute(Lnet/bluemind/gwtconsoleapp/base/editor/ScreenRoot;Ljava/lang/String;Lnet/bluemind/gwtconsoleapp/base/editor/ScreenElement;)(elt,attribute,contribution);
		};
	}-*/;

	private static native void registerType(JavaScriptObject jsPackage, String className,
			GwtFactory<IGwtScreenRoot, ScreenRoot> factory)

	/*-{
	
		jsPackage[className] = function(model, context) {
	
			if (!model) {
				model = {};
			}
			var nat = factory.@net.bluemind.gwtconsoleapp.base.editor.gwt.GwtFactory::create(Lcom/google/gwt/core/client/JavaScriptObject;)(this);
			this['decor_'] = @net.bluemind.gwtconsoleapp.base.editor.gwt.GwtScreenRoot.ScreenRootWidget::new(Lnet/bluemind/gwtconsoleapp/base/editor/gwt/IGwtScreenRoot;)(nat);
	
			$wnd.bm.ScreenRoot.call(this, model, context);
		};
	
		jsPackage[className].prototype = new $wnd.bm.ScreenRoot();
	
		jsPackage[className].prototype.attach = function(parent) {
			this['decor_'].@net.bluemind.gwtconsoleapp.base.editor.gwt.GwtScreenRoot.ScreenRootWidget::attach(Lcom/google/gwt/dom/client/Element;)(parent);
			this['decor_'].@net.bluemind.gwtconsoleapp.base.editor.gwt.GwtScreenRoot.ScreenRootWidget::doLoad(Lnet/bluemind/gwtconsoleapp/base/editor/ScreenRoot;)(this);
		};
	
		jsPackage[className].prototype.loadModel = function(model) {
			this['decor_'].@net.bluemind.gwtconsoleapp.base.editor.gwt.GwtScreenRoot.ScreenRootWidget::loadModel(Lcom/google/gwt/core/client/JavaScriptObject;)(model);
		};
	
		jsPackage[className].prototype.saveModel = function(model) {
			this['decor_'].@net.bluemind.gwtconsoleapp.base.editor.gwt.GwtScreenRoot.ScreenRootWidget::saveModel(Lcom/google/gwt/core/client/JavaScriptObject;)(model);
		};
	
	}-*/;

}
