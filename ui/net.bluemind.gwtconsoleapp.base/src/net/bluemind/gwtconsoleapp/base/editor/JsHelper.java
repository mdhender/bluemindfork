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

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.JsArrayString;

public class JsHelper {

	public static <T extends JavaScriptObject> List<T> asList(JsArray<T> jsArray) {
		ArrayList<T> ret = new ArrayList<>(jsArray.length());
		for (int i = 0; i < jsArray.length(); i++) {
			ret.add(jsArray.get(i));
		}
		return ret;
	}

	public static List<String> asList(JsArrayString jsArray) {
		ArrayList<String> ret = new ArrayList<>(jsArray.length());
		for (int i = 0; i < jsArray.length(); i++) {
			ret.add(jsArray.get(i));
		}
		return ret;
	}

	public native static JavaScriptObject createPackage(String packageName)
	/*-{
    var curpart = $wnd;
    var parts = packageName.split('.');

    for (var i = 0; i < parts.length; i++) {
      var part = parts[i];
      if (!curpart[part]) {
        curpart[part] = {};

      }
      curpart = curpart[part];

    }

    return curpart
	}-*/;

	public static native <T extends JavaScriptObject> T construct(Class<T> klass, String className,
			JavaScriptObject model, EditorContext editorContext)

	/*-{
    var curpart = $wnd;
    var parts = className.split('.');

    for (var i = 0; i < parts.length; i++) {
      var part = parts[i];
      if (!curpart[part]) {
        throw "class " + className + " doesnt exists";
      }
      curpart = curpart[part];
    }

    return new curpart(model, editorContext);

	}-*/;

	public interface JsFunction {
		public JavaScriptObject call(Object... parameters);
	}

	public static native void method(JavaScriptObject prototype, String methodName, JsFunction jsfunction)
	/*-{
    prototype[methodName] = function() {
      @net.bluemind.gwtconsoleapp.base.editor.JsHelper::call(Lnet/bluemind/gwtconsoleapp/base/editor/JsHelper$JsFunction;[Lcom/google/gwt/core/client/JavaScriptObject;)(jsfunction,parameters);
    };
	}-*/;

	protected static void call(JsFunction function, JavaScriptObject... parameters) {
		function.call(parameters);
	}
}
