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

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.shared.GWT;

import net.bluemind.core.api.AsyncHandler;

public class ModelHandler extends ScreenElement {

	protected ModelHandler() {
	}

	public final native void load(JavaScriptObject model, AsyncHandler<Void> handler)
	/*-{
    this["load"](model, function() {
      handler.@net.bluemind.core.api.AsyncHandler::success(Ljava/lang/Object;)(null);
    }, function(e) {
      handler.@net.bluemind.core.api.AsyncHandler::failure(Ljava/lang/Throwable;)(e);
    });
	}-*/;

	public final native void save(JavaScriptObject model, AsyncHandler<Void> handler)
	/*-{
    this["save"](model, function() {
      handler.@net.bluemind.core.api.AsyncHandler::success(Ljava/lang/Object;)(null);
    }, function(e) {
      handler.@net.bluemind.core.api.AsyncHandler::failure(Ljava/lang/Throwable;)(e);
    });
	}-*/;

	private static native void rt()
	/*-{
    $wnd.bm.ModelHandler = function() {
    }

    $wnd.bm.ModelHandler.prototype = new $wnd.bm.ScreenElement();

	}-*/;

	public static void registerType() {
		rt();
		GWT.log("bm.ModelHandler registred");
	}

}
