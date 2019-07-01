/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2018
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
package net.bluemind.ui.settings.client.myaccount.external;

import com.google.gwt.core.client.JavaScriptObject;

import net.bluemind.system.api.gwt.js.JsExternalSystem;

public class JsCompleteExternalSystem extends JavaScriptObject {

	protected JsCompleteExternalSystem() {
	}

	public final native void setSystem(JsExternalSystem system, String logo)
	/*-{
    this["identifier"] = system['identifier'];
    this["description"] = system['description'];
    this["authKind"] = system['authKind'];
    this["logo"] = logo;
	}-*/;

	public final native String getIdentifier()
	/*-{
    return this["identifier"];
	}-*/;

	public final native String getDescription()
	/*-{
    return this["description"];
	}-*/;

	public final native String getLogo()
	/*-{
    return this["logo"];
	}-*/;

	public final native net.bluemind.system.api.gwt.js.JsExternalSystemAuthKind getAuthKind()
	/*-{
    return this["authKind"];
	}-*/;

	public static native JsCompleteExternalSystem create()
	/*-{
    var ret = {};
    return ret;
	}-*/;

}
