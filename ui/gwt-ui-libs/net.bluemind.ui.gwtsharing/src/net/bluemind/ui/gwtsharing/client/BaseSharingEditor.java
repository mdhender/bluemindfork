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
package net.bluemind.ui.gwtsharing.client;

import java.util.HashMap;
import java.util.Map;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.shared.GWT;

import net.bluemind.core.commons.gwt.JsMapStringJsObject;
import net.bluemind.gwtconsoleapp.base.editor.gwt.CompositeGwtWidgetElement;
import net.bluemind.ui.common.client.forms.acl.AclConstants;

public abstract class BaseSharingEditor extends CompositeGwtWidgetElement {

	protected final AclEdit edit;
	private final String modelId;
	private String type;

	public BaseSharingEditor(String modelId, String type) {
		this.modelId = modelId;
		this.type = type;
		Map<String, String> verbs = getVerbs();

		// FIXME mailboxacl
		edit = new AclEdit(verbs, AbstractDirEntryOpener.defaultOpener);
		initWidget(edit.asWidget());
		edit.setEnable(false);
	}

	protected abstract String getContainerUid(JavaScriptObject model);

	protected Map<String, String> getVerbs() {
		Map<String, String> verbs = new HashMap<>();
		AclConstants constants = GWT.create(AclConstants.class);

		if (type.equals("calendar")) {
			verbs.put("access", constants.aclCalendarAccess());
			verbs.put("read", constants.aclCalendarRead());
			verbs.put("write", constants.aclCalendarWrite());
			verbs.put("admin", constants.aclCalendarAdmin());
		} else if ("freebusy".equals(type)) {
			verbs.put("read", constants.aclFreebusyRead());
			verbs.put("admin", constants.aclFreebusyAdmin());
		} else if ("addressbook".equals(type)) {
			verbs.put("read", constants.aclBookRead());
			verbs.put("write", constants.aclBookWrite());
			verbs.put("admin", constants.aclBookAdmin());
		} else if ("mailboxacl".equals(type)) {
			verbs.put("send-on-behalf", constants.aclMailSendOnBehalf());
			verbs.put("read", constants.aclMailRead());
			verbs.put("write", constants.aclMailWrite());
			verbs.put("admin", constants.aclMailAdmin());
		} else {
			verbs.put("read", constants.aclRead());
			verbs.put("write", constants.aclWrite());
			verbs.put("admin", constants.aclAdmin());
		}
		return verbs;
	}

	@Override
	public void saveModel(JavaScriptObject model) {
		SharingModel sm = SharingModel.get(model, modelId);
		if (sm != null) {
			SharingModel.populate(model, modelId, edit.getValue());
		}
	}

	@Override
	public void loadModel(JavaScriptObject model) {

		SharingModel sm = SharingModel.get(model, modelId);
		if (sm != null) {
			setVisible(true);
			JsMapStringJsObject map = model.cast();
			String domainUid = map.getString("domainUid");
			edit.setDomainUid(domainUid);
			edit.setEnable(true);
			edit.setContainerUid(getContainerUid(model));
			if ("calendar".equals(type)) {
				edit.setAddressesSharing(type);
			}
			edit.setValue(sm.getAcl());
		} else {
			setVisible(false);
		}

	}

}
