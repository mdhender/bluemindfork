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
package net.bluemind.ui.adminconsole.directory.user;

import java.util.HashMap;
import java.util.Map;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;

import net.bluemind.core.commons.gwt.JsMapStringJsObject;
import net.bluemind.gwtconsoleapp.base.editor.WidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.GwtWidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtDelegateFactory;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtWidgetElement;
import net.bluemind.ui.common.client.forms.acl.AclConstants;
import net.bluemind.ui.gwtsharing.client.BaseSharingEditor;
import net.bluemind.ui.gwtsharing.client.SharingModel;
import net.bluemind.ui.mailbox.sharing.CrossShardSharingValidator;
import net.bluemind.ui.mailbox.sharing.IMailboxSharingEditor;

public class MailboxSharingEditor extends BaseSharingEditor implements IMailboxSharingEditor {

	public static final String TYPE = "bm.ac.MailboxSharingEditor";
	private String dataLocation;

	@Override
	public void loadModel(JavaScriptObject model) {
		super.loadModel(model);
		SharingModel sm = SharingModel.get(model, MailboxSharingModelHandler.MODEL_ID);
		if (sm != null) {
			dataLocation = sm.getDataLocation();
		}
		edit.setPublicSharingVisible(false);
	}

	protected Map<String, String> getVerbs() {
		Map<String, String> verbs = new HashMap<>();
		AclConstants constants = GWT.create(AclConstants.class);

		verbs.put("send-on-behalf", constants.aclMailSendOnBehalf());
		verbs.put("read", constants.aclMailRead());
		verbs.put("write", constants.aclMailWrite());
		verbs.put("admin", constants.aclMailAdmin());
		return verbs;
	}

	public static void registerType() {

		GwtWidgetElement.register(TYPE, new IGwtDelegateFactory<IGwtWidgetElement, WidgetElement>() {

			@Override
			public IGwtWidgetElement create(WidgetElement e) {
				return new MailboxSharingEditor();
			}
		});
		GWT.log("bm.ac.MailboxSharingEditor registred");
	}

	protected MailboxSharingEditor() {
		super(MailboxSharingModelHandler.MODEL_ID, "mailboxacl");
		edit.registerValidator(new CrossShardSharingValidator(this));
	}

	@Override
	protected String getContainerUid(JavaScriptObject model) {

		JsMapStringJsObject map = model.cast();
		return "mailbox:acls-" + map.getString("mailboxUid");
	}

	@Override
	public String getMailboxDataLocation() {
		return dataLocation;
	}
}
