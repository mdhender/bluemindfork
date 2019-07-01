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
package net.bluemind.ui.adminconsole.directory.group;

import com.google.gwt.core.client.JavaScriptObject;

import net.bluemind.core.commons.gwt.JsMapStringJsObject;
import net.bluemind.gwtconsoleapp.base.editor.ModelHandler;
import net.bluemind.gwtconsoleapp.base.editor.gwt.GwtModelHandler;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtDelegateFactory;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtModelHandler;
import net.bluemind.ui.gwtsharing.client.BaseSharingModelHandler;

public class MailboxGroupSharingModelHandler extends BaseSharingModelHandler {

	public static final String TYPE = "bm.ac.MailboxGroupSharingModelHandler";
	public static final String MODEL_ID = "groupmailbox-sharing";

	private MailboxGroupSharingModelHandler() {
		super(MODEL_ID);
	}

	@Override
	protected String getContainerUid(JavaScriptObject model) {
		JsMapStringJsObject map = model.cast();
		// FIXME
		return "mailbox:acls-" + map.getString("groupId");
	}

	public static void registerType() {
		GwtModelHandler.register(TYPE, new IGwtDelegateFactory<IGwtModelHandler, ModelHandler>() {

			@Override
			public IGwtModelHandler create(ModelHandler e) {
				return new MailboxGroupSharingModelHandler();
			}
		});
	}
}
