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
package net.bluemind.ui.gwttag.client;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;

import net.bluemind.core.container.model.gwt.js.JsItemValue;
import net.bluemind.tag.api.gwt.js.JsTag;

public final class TagsModel extends JavaScriptObject {

	protected TagsModel() {

	}

	public native String getDomainUid()
	/*-{
		return this["domainUid"];
	}-*/;

	public native JsArray<JsItemValue<JsTag>> getTags()
	/*-{
		return this["tags"];
	}-*/;

	public native void setTags(JsArray<JsItemValue<JsTag>> tags)
	/*-{
		 this["tags"] = tags;
	}-*/;

	public native JsArray<JsItemValue<JsTag>> getCurrentTags()
	/*-{
		return this["current-tags"];
	}-*/;

	public native void setCurrentTags(JsArray<JsItemValue<JsTag>> tags)
	/*-{
		 this["current-tags"] = tags;
	}-*/;

	public native String getUserId()
	/*-{
		return this["userId"];
	}-*/;

}
