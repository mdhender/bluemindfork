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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.core.shared.GWT;
import com.google.gwt.json.client.JSONBoolean;
import com.google.gwt.json.client.JSONObject;

import net.bluemind.core.api.AsyncHandler;
import net.bluemind.core.commons.gwt.JsMapStringJsObject;
import net.bluemind.core.commons.gwt.JsMapStringString;
import net.bluemind.gwtconsoleapp.base.editor.gwt.ProgressDialogPanel;

public class ScreenRoot extends CompositeElement {

	public static final class SizeHint extends JavaScriptObject {
		protected SizeHint() {
		}

		public native int getWidth()
		/*-{
      return this['width'];
		}-*/;

		public native int getHeight()
		/*-{
      return this['height'];
		}-*/;

		public native static SizeHint create(int width, int height)
		/*-{
      return {
        'width' : width,
        'height' : height
      };
		}-*/;
	}

	protected ScreenRoot() {
	}

	public final native JsArray<ModelHandler> getHandlers()
	/*-{
    return this['modelHandlers'];
	}-*/;

	public final native JsMapStringString getState()
	/*-{
    return this['state'];
	}-*/;

	public final native void setState(JsMapStringString state)
	/*-{
    this['state'] = state;
	}-*/;

	public final native void save(AsyncHandler<Void> handler)
	/*-{
    this['save'](handler);
	}-*/;

	public final native void load(AsyncHandler<Void> handler)
	/*-{
    this['load'](handler);
	}-*/;

	public final native JavaScriptObject getModel()
	/*-{
    return this['model_'];
	}-*/;

	public final native boolean isOverlay()
	/*-{
    return this['overlay'];
	}-*/;

	public final native void setOverlay(boolean b)
	/*-{
    this['overlay'] = b;
	}-*/;

	public final native SizeHint getSizeHint()
	/*-{
    return this['sizehint'];
	}-*/;

	public final native void setSizeHint(SizeHint s)
	/*-{
    this['sizehint'] = s;
	}-*/;

	private static native void rt()
	/*-{
    $wnd.bm.ScreenRoot = function(model, context) {
      $wnd.bm.CompositeElement.call(this, model, context);
      this["overlay"] = false;
      this["modelHandlers"] = [];

      if (model && model["modelHandlers"]) {
        for (var i = 0; i < model["modelHandlers"].length; i++) {
          var modelHandler = @net.bluemind.gwtconsoleapp.base.editor.ScreenElement::build(Lnet/bluemind/gwtconsoleapp/base/editor/ScreenElement;Lnet/bluemind/gwtconsoleapp/base/editor/EditorContext;)(model["modelHandlers"][i], context );
          if (modelHandler != null) {
            this["modelHandlers"].push(modelHandler);
          }
        }

      }

      if (model && model["sizehint"]) {
        this["sizehint"] = model["sizehint"];
      }

      if (model && model["overlay"]) {
        this["overlay"] = model["overlay"];
      }

      if (model && model["model_"]) {
        this["model_"] = model["model_"];
      } else {
        this["model_"] = {};
      }
    }

    $wnd.bm.ScreenRoot.prototype = new $wnd.bm.CompositeElement();
    $wnd.bm.ScreenRoot.prototype.save = function(handler) {
      @net.bluemind.gwtconsoleapp.base.editor.ScreenRoot::saveImpl(Lnet/bluemind/gwtconsoleapp/base/editor/ScreenRoot;Lcom/google/gwt/core/client/JavaScriptObject;Lnet/bluemind/core/api/AsyncHandler;)(this,this.model_,handler);
    };
    $wnd.bm.ScreenRoot.prototype.load = function(handler) {
      @net.bluemind.gwtconsoleapp.base.editor.ScreenRoot::loadImpl(Lnet/bluemind/gwtconsoleapp/base/editor/ScreenRoot;Lcom/google/gwt/core/client/JavaScriptObject;Lnet/bluemind/core/api/AsyncHandler;)(this,this.model_,handler);
    };
    $wnd.bm.ScreenRoot.prototype.contribute = function(elt, attribute, contribution) {
      @net.bluemind.gwtconsoleapp.base.editor.ScreenRoot::contribute(Lnet/bluemind/gwtconsoleapp/base/editor/ScreenRoot;Ljava/lang/String;Lnet/bluemind/gwtconsoleapp/base/editor/ScreenElement;)(this, attribute, contribution);
    };

    $wnd.bm.ScreenRoot.modelHandlers = [];

	}-*/;

	protected static final void saveImpl(ScreenRoot instance, JavaScriptObject model, AsyncHandler<Void> handler) {
		try {
			instance.saveModel(model);
		} catch (Exception e) {
			handler.failure(e);
			return;
		}

		JsArray<ModelHandler> jsHandlers = instance.getHandlers();
		final List<ModelHandler> handlers = new ArrayList<>(jsHandlers.length());
		for (int i = 0; i < jsHandlers.length(); i++) {
			handlers.add(jsHandlers.get(i));
		}
		ProgressDialogPanel progress = new ProgressDialogPanel();
		progress.setSteps(handlers.size() * 2);
		progress.setText("saving..");
		progress.center();
		progress.show();
		List<Throwable> errors = new ArrayList<>();
		saveHandlers(progress, model, handlers, errors, handler);

	}

	protected static final void loadImpl(ScreenRoot instance, JavaScriptObject model, AsyncHandler<Void> handler) {

		JsMapStringString state = instance.getState();

		JsMapStringJsObject map = model.cast();
		JsArrayString keys = state.keys();
		for (int i = 0; i < keys.length(); i++) {
			map.putString(keys.get(i), state.get(keys.get(i)));
		}

		if (handler == null) {
			handler = new AsyncHandler<Void>() {

				@Override
				public void success(Void value) {
					GWT.log("screenroot model loaded (handler was null)");

				}

				@Override
				public void failure(Throwable e) {
					GWT.log("error during screenroot model loading (handler was null)");
				}
			};
		}
		JsArray<ModelHandler> jsHandlers = instance.getHandlers();
		final List<ModelHandler> handlers = new ArrayList<>(jsHandlers.length());
		for (int i = 0; i < jsHandlers.length(); i++) {
			handlers.add(jsHandlers.get(i));
		}

		ProgressDialogPanel progress = new ProgressDialogPanel();
		progress.setSteps(handlers.size() * 2);
		progress.setText("loading..");
		progress.center();
		List<Throwable> errors = new ArrayList<>();
		loadHandlers(progress, model, handlers, errors, handler);

	}

	private static void saveHandlers(final ProgressDialogPanel progress, final JavaScriptObject model,
			final List<ModelHandler> handlers, final List<Throwable> errors, final AsyncHandler<Void> handler) {
		GWT.log(" model to save " + handlers.size());
		if (handlers.isEmpty()) {
			if (errors.isEmpty()) {
				progress.hide();
				handler.success(null);
			} else {
				progress.hide();
				StringBuilder sb = new StringBuilder();
				for (Throwable e : errors) {
					sb.append(e.getMessage() + "\n");
				}
				handler.failure(new Exception(sb.toString()));
			}
		} else {
			ModelHandler h = handlers.remove(0);
			if (h.isReadOnly()) {
				GWT.log(" readonly modelhandler !  " + h.getType());
				progress.progress(1, "saving...");
				progress.progress(1, "saving...");
				saveHandlers(progress, model, handlers, errors, handler);
			} else {
				GWT.log(" modelhandler  " + h.getType());
				progress.progress(1, "saving...");
				h.save(model, new AsyncHandler<Void>() {

					@Override
					public void success(Void value) {
						progress.progress(1, "saving...");
						saveHandlers(progress, model, handlers, errors, handler);
					}

					@Override
					public void failure(Throwable e) {
						progress.progress(1, "saving...");
						errors.add(e);
						saveHandlers(progress, model, handlers, errors, handler);
					}
				});
			}
		}
	}

	private static void loadHandlers(final ProgressDialogPanel progress, final JavaScriptObject model,
			final List<ModelHandler> handlers, final List<Throwable> errors, final AsyncHandler<Void> handler) {

		if (handlers.isEmpty()) {

			if (errors.isEmpty()) {
				GWT.log(" model loading finished !");
				progress.hide();
				handler.success(null);
			} else {
				progress.hide();
				StringBuilder sb = new StringBuilder();
				for (Throwable e : errors) {
					sb.append(e.getMessage() + "\n");
				}
				handler.failure(new Exception(sb.toString()));
			}
		} else {
			final ModelHandler h = handlers.remove(0);
			GWT.log(" modelhandler  " + h.getType() + " remaing " + handlers.size());
			final JSONObject marq = new JSONObject();
			progress.progress(1, "loading...");

			h.load(model, new AsyncHandler<Void>() {

				@Override
				public void success(Void value) {
					if (marq.containsKey("loaded")) {
						GWT.log("double call of success by  " + h.getType() + ": BAD!!");
						return;
					}
					marq.put("loaded", JSONBoolean.getInstance(true));
					GWT.log(" modelhandler  " + h.getType() + " loaded");
					progress.progress(1, "loading...");
					loadHandlers(progress, model, handlers, errors, handler);
				}

				@Override
				public void failure(Throwable e) {
					progress.progress(1, "loading...");
					errors.add(e);
					GWT.log("error during loading with handler " + h.getType());
					loadHandlers(progress, model, handlers, errors, handler);
				}
			});
		}

	}

	private static void contribute(ScreenRoot instance, String attribute, ScreenElement contribution) {

		if ("modelHandlers".equals(attribute) || attribute == null) {
			GWT.log("contribute modelHandler " + contribution.getType() + " to " + instance.getType());
			ModelHandler handler = contribution.castElement("bm.ModelHandler");
			instance.getHandlers().push(handler);
		} else {

		}
	}

	public static void registerType() {
		rt();
		GWT.log("bm.ScreenRoot registred");
	}

	public static <T extends ScreenRoot> T build(T model, Map<String, String> state, EditorContext context) {
		JsMapStringString map = JsMapStringString.create(state);
		T screenRoot = JsHelper.construct(null, model.getType(), model, context);
		screenRoot.setState(map);
		return screenRoot;
	}

}
