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
package net.bluemind.ui.adminconsole.directory.addressbook;

import java.util.HashMap;
import java.util.Map;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.json.client.JSONObject;

import net.bluemind.gwtconsoleapp.base.editor.WidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.GwtWidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtDelegateFactory;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtWidgetElement;
import net.bluemind.ui.common.client.forms.acl.AclConstants;
import net.bluemind.ui.gwtsharing.client.BaseSharingEditor;

public class AddressBookSharingEditor extends BaseSharingEditor {

	public static final String TYPE = "bm.ac.AddressBookSharingEditor";

	public AddressBookSharingEditor() {
		super("addressbook-sharing", "addressbook");
	}

	public static void registerType() {
		GwtWidgetElement.register(TYPE, new IGwtDelegateFactory<IGwtWidgetElement, WidgetElement>() {

			@Override
			public IGwtWidgetElement create(WidgetElement e) {
				return new AddressBookSharingEditor();
			}
		});
	}

	protected Map<String, String> getVerbs() {
		Map<String, String> verbs = new HashMap<>();
		AclConstants constants = GWT.create(AclConstants.class);

		verbs.put("read", constants.aclBookRead());
		verbs.put("write", constants.aclBookWrite());
		verbs.put("admin", constants.aclBookAdmin());
		return verbs;
	}

	@Override
	protected String getContainerUid(JavaScriptObject model) {
		return new JSONObject(model).get("bookId").isString().stringValue();
	}

}
