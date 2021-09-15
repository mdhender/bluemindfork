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
package net.bluemind.ui.gwtuser.client;

import java.util.HashMap;
import java.util.Map;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;

import net.bluemind.calendar.api.IFreebusyUids;
import net.bluemind.core.commons.gwt.JsMapStringJsObject;
import net.bluemind.gwtconsoleapp.base.editor.WidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.GwtWidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtDelegateFactory;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtWidgetElement;
import net.bluemind.ui.common.client.forms.acl.AclConstants;
import net.bluemind.ui.gwtsharing.client.BaseSharingEditor;

public class FreebusySharingEditor extends BaseSharingEditor {

	public static final String TYPE = "bm.ac.FreebusySharingEditor";

	protected FreebusySharingEditor() {
		super("freebusy-sharing", "freebusy");
	}

	@Override
	protected String getContainerUid(JavaScriptObject model) {
		JsMapStringJsObject map = model.cast();
		String uid = map.get("userUid") != null ? map.getString("userUid") : map.getString("entryUid");
		return IFreebusyUids.getFreebusyContainerUid(uid);
	}

	protected Map<String, String> getVerbs() {
		Map<String, String> verbs = new HashMap<>();
		AclConstants constants = GWT.create(AclConstants.class);

		verbs.put("read", constants.aclFreebusyRead());
		verbs.put("admin", constants.aclFreebusyAdmin());

		return verbs;
	}

	public static void registerType() {
		GwtWidgetElement.register(TYPE, new IGwtDelegateFactory<IGwtWidgetElement, WidgetElement>() {

			@Override
			public IGwtWidgetElement create(WidgetElement e) {
				return new FreebusySharingEditor();
			}
		});
	}

}
