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
package net.bluemind.ui.mailbox.filter;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.json.client.JSONObject;

import net.bluemind.mailbox.api.MailFilter;
import net.bluemind.mailbox.api.gwt.js.JsMailFilter;
import net.bluemind.mailbox.api.gwt.serder.MailFilterGwtSerDer;

public class MailSettingsModel extends JavaScriptObject {

	protected MailSettingsModel() {
	}

	public static void populate(JavaScriptObject mainModel, MailFilter filter) {
		MailSettingsModel model = mainModel.cast();

		// no filter
		if (filter == null) {
			model.setFilters(null);
		} else {
			model.setFilters(
					new MailFilterGwtSerDer().serialize(filter).isObject().getJavaScriptObject().<JsMailFilter> cast());
		}
	}

	public static MailSettingsModel get(JavaScriptObject mainModel) {
		return mainModel.cast();
	}

	public final native void setFilters(JsMailFilter filter)
	/*-{
		this['mail-settings'] = filter;
	}-*/;

	public final native JsMailFilter getJsMailFilter()
	/*-{
		return this['mail-settings'];
	}-*/;

	public final MailFilter getMailFilter() {
		JsMailFilter r = getJsMailFilter();
		if (r == null) {
			return null;
		} else {
			return new MailFilterGwtSerDer().deserialize(new JSONObject(r));
		}
	}

}
