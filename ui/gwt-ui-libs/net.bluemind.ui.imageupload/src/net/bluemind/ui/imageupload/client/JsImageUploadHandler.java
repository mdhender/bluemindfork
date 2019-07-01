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
package net.bluemind.ui.imageupload.client;

import com.google.gwt.core.client.JavaScriptObject;

public class JsImageUploadHandler implements ImageUploadHandler {
	JavaScriptObject newImageFunction;
	JavaScriptObject cancelFunction;
	JavaScriptObject deleteFunction;
	JavaScriptObject failureFunction;

	public JsImageUploadHandler(JavaScriptObject newImageFunction, JavaScriptObject cancelFunction,
			JavaScriptObject deleteFunction, JavaScriptObject failureFunction) {
		super();
		this.newImageFunction = newImageFunction;
		this.cancelFunction = cancelFunction;
		this.deleteFunction = deleteFunction;
		this.failureFunction = failureFunction;
	}

	private native void callFailure(Throwable e)
	/*-{
		var f = this.@net.bluemind.ui.imageupload.client.JsImageUploadHandler::failureFunction;
		f.apply(null,[e]);
	}-*/;

	private native void callNewImage(String success)
	/*-{
		var f = this.@net.bluemind.ui.imageupload.client.JsImageUploadHandler::newImageFunction;
		f.apply(null,[ success ]);		
	}-*/;

	private native void callCancel()
	/*-{
		var f = this.@net.bluemind.ui.imageupload.client.JsImageUploadHandler::cancelFunction;
		f.apply(null,[  ]);		
	}-*/;

	private native void callDelete()
	/*-{
		var f = this.@net.bluemind.ui.imageupload.client.JsImageUploadHandler::deleteFunction;
		f.apply(null,[  ]);		
	}-*/;

	@Override
	public void newImage(String text) {
		callNewImage(text);
	}

	@Override
	public void cancel() {
		callCancel();
	}

	@Override
	public void failure(Throwable e) {
		callFailure(e);
	}

	@Override
	public void deleteCurrent() {
		callDelete();
	}

}
