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
package net.bluemind.ui.adminconsole.directory.mailshare;

import java.util.HashMap;
import java.util.Map;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;

import net.bluemind.core.commons.gwt.JsMapStringJsObject;
import net.bluemind.gwtconsoleapp.base.editor.WidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.GwtWidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtDelegateFactory;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtWidgetElement;
import net.bluemind.ui.adminconsole.directory.mailshare.l10n.MailshareConstants;
import net.bluemind.ui.gwtsharing.client.BaseSharingEditor;
import net.bluemind.ui.gwtsharing.client.SharingModel;
import net.bluemind.ui.mailbox.sharing.CrossShardSharingValidator;
import net.bluemind.ui.mailbox.sharing.IMailboxSharingEditor;

public class MailshareMailboxSharingEditor extends BaseSharingEditor implements IMailboxSharingEditor {

	public static final String TYPE = "bm.ac.MailshareMailboxSharingEditor";
	private String dataLocation;

	@Override
	public void loadModel(JavaScriptObject model) {
		super.loadModel(model);
		SharingModel sm = SharingModel.get(model, MailshareMailboxSharingModelHandler.MODEL_ID);
		dataLocation = sm.getDataLocation();
		edit.setPublicSharingVisible(false);
	}

	@Override
	protected Map<String, String> getVerbs() {
		Map<String, String> verbs = new HashMap<>();

		MailshareConstants constants = GWT.create(MailshareConstants.class);

		verbs.put("read", constants.aclRead());
		verbs.put("write", constants.aclWrite());
		verbs.put("admin", constants.aclAdmin());

		return verbs;
	}

	public static void registerType() {

		GwtWidgetElement.register(TYPE, new IGwtDelegateFactory<IGwtWidgetElement, WidgetElement>() {

			@Override
			public IGwtWidgetElement create(WidgetElement e) {
				return new MailshareMailboxSharingEditor();
			}
		});
		GWT.log("bm.ac.MailshareMailboxSharingEditor registred");
	}

	protected MailshareMailboxSharingEditor() {
		super(MailshareMailboxSharingModelHandler.MODEL_ID, "mailboxacl");
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
