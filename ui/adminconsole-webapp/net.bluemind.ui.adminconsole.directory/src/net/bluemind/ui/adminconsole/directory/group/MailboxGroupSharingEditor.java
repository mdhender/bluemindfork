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

import java.util.HashMap;
import java.util.Map;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.shared.GWT;

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

public class MailboxGroupSharingEditor extends BaseSharingEditor implements IMailboxSharingEditor {

	public static final String TYPE = "bm.ac.MailboxGroupSharingEditor";
	private String dataLocation;

	public MailboxGroupSharingEditor() {
		super(MailboxGroupSharingModelHandler.MODEL_ID, "mailboxacl");
		edit.registerValidator(new CrossShardSharingValidator(this));
		edit.setPublicSharingVisible(false);
	}

	@Override
	public void loadModel(JavaScriptObject model) {
		super.loadModel(model);
		SharingModel sm = SharingModel.get(model, MailboxGroupSharingModelHandler.MODEL_ID);
		dataLocation = sm.getDataLocation();
	}

	protected Map<String, String> getVerbs() {
		Map<String, String> verbs = new HashMap<>();
		AclConstants constants = GWT.create(AclConstants.class);

		verbs.put("read", constants.aclMailRead());
		verbs.put("write", constants.aclMailWrite());
		verbs.put("admin", constants.aclMailAdmin());
		return verbs;
	}

	@Override
	protected String getContainerUid(JavaScriptObject model) {
		JsMapStringJsObject m = model.cast();
		return "mailbox:acls-" + m.getString("groupId");
	}

	public static void registerType() {
		GwtWidgetElement.register(TYPE, new IGwtDelegateFactory<IGwtWidgetElement, WidgetElement>() {

			@Override
			public IGwtWidgetElement create(WidgetElement e) {
				return new MailboxGroupSharingEditor();
			}
		});
	}

	@Override
	public String getMailboxDataLocation() {
		return dataLocation;
	}

}
