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

import net.bluemind.core.api.AsyncHandler;
import net.bluemind.gwtconsoleapp.base.editor.JsHelper;
import net.bluemind.gwtconsoleapp.base.editor.JsHelper.JsFunction;
import net.bluemind.gwtconsoleapp.base.editor.ModelHandler;

public class GwtModelHandler {

	protected static final class JsFunctionWrapper implements JsFunction {

		private final JavaScriptObject func;

		public JsFunctionWrapper(JavaScriptObject func) {
			this.func = func;
		}

		@Override
		public JavaScriptObject call(Object... parameters) {
			return doCall(parameters);
		}

		public native final JavaScriptObject doCall(Object[] parameters) /*-{
      var f = this.@net.bluemind.gwtconsoleapp.base.editor.gwt.GwtModelHandler.JsFunctionWrapper::func;
      f.apply(null, parameters);
		}-*/;

	}

	protected static final class ModelHandlerWrapper implements IGwtModelHandler {

		private IGwtModelHandler delegate;

		public ModelHandlerWrapper(IGwtModelHandler modelHandler) {
			this.delegate = modelHandler;
		}

		@Override
		public void load(JavaScriptObject model, AsyncHandler<Void> handler) {
			delegate.load(model, handler);
		}

		@Override
		public void save(JavaScriptObject model, AsyncHandler<Void> handler) {
			delegate.save(model, handler);
		}

		public void load(JavaScriptObject model, JsFunction succ, JsFunction error) {
			delegate.load(model, new AsyncHandler<Void>() {

				@Override
				public void success(Void value) {
					JsFunction f = succ;
					f.call(value);
				}

				@Override
				public void failure(Throwable e) {
					error.call(e);
				}

			});

		}

		public void save(JavaScriptObject model, JsFunction succ, JsFunction error) {
			delegate.save(model, new AsyncHandler<Void>() {

				@Override
				public void success(Void value) {
					succ.call(value);
				}

				@Override
				public void failure(Throwable e) {
					error.call(e);
				}

			});

		}

	}

	public static void register(String className, IGwtDelegateFactory<IGwtModelHandler, ModelHandler> factory) {
		String packageName = className.substring(0, className.lastIndexOf('.'));
		String classSimpleName = className.substring(className.lastIndexOf('.') + 1);

		JavaScriptObject jsPackage = JsHelper.createPackage(packageName);
		registerType(jsPackage, classSimpleName, className, new GwtFactory<>(factory));
	}

	private static native void registerType(JavaScriptObject jsPackage, String className, String total,
			GwtFactory<IGwtModelHandler, ModelHandler> wrapper)

	/*-{

    jsPackage[className] = function(model) {
      this['type'] = total;
      if (model['attributes']) {
        this['attributes'] = model['attributes'];
      }

      this['id'] = model['id'];
      this['roles'] = model['roles'];
      this['role'] = model['role'];
      this['activeRoles'] = model['activeRoles'];
      this['title'] = model['title'];
      this.readOnly = model['readOnly'];
      var nat = wrapper.@net.bluemind.gwtconsoleapp.base.editor.gwt.GwtFactory::create(Lcom/google/gwt/core/client/JavaScriptObject;)(this);
      this["wrapped_"] = @net.bluemind.gwtconsoleapp.base.editor.gwt.GwtModelHandler.ModelHandlerWrapper::new(Lnet/bluemind/gwtconsoleapp/base/editor/gwt/IGwtModelHandler;)(nat);
    }

    jsPackage[className].prototype = new $wnd.bm.ModelHandler();
    jsPackage[className].prototype.save = function(model, success, failure) {
      var funcSucc = @net.bluemind.gwtconsoleapp.base.editor.gwt.GwtModelHandler.JsFunctionWrapper::new(Lcom/google/gwt/core/client/JavaScriptObject;)(success);
      var funcFail = @net.bluemind.gwtconsoleapp.base.editor.gwt.GwtModelHandler.JsFunctionWrapper::new(Lcom/google/gwt/core/client/JavaScriptObject;)(failure);
      this["wrapped_"].@net.bluemind.gwtconsoleapp.base.editor.gwt.GwtModelHandler.ModelHandlerWrapper::save(Lcom/google/gwt/core/client/JavaScriptObject;Lnet/bluemind/gwtconsoleapp/base/editor/JsHelper$JsFunction;Lnet/bluemind/gwtconsoleapp/base/editor/JsHelper$JsFunction;)(model,funcSucc,funcFail);
    };
    jsPackage[className].prototype.load = function(model, success, failure) {
      var funcSucc = @net.bluemind.gwtconsoleapp.base.editor.gwt.GwtModelHandler.JsFunctionWrapper::new(Lcom/google/gwt/core/client/JavaScriptObject;)(success);
      var funcFail = @net.bluemind.gwtconsoleapp.base.editor.gwt.GwtModelHandler.JsFunctionWrapper::new(Lcom/google/gwt/core/client/JavaScriptObject;)(failure);
      this["wrapped_"].@net.bluemind.gwtconsoleapp.base.editor.gwt.GwtModelHandler.ModelHandlerWrapper::load(Lcom/google/gwt/core/client/JavaScriptObject;Lnet/bluemind/gwtconsoleapp/base/editor/JsHelper$JsFunction;Lnet/bluemind/gwtconsoleapp/base/editor/JsHelper$JsFunction;)(model,funcSucc,funcFail);
    };
	}-*/;

}
