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
package net.bluemind.ui.mailbox.sharing;

import java.util.HashMap;
import java.util.Map;

import com.google.gwt.core.client.JavaScriptObject;

import net.bluemind.core.commons.gwt.JsMapStringJsObject;
import net.bluemind.gwtconsoleapp.base.editor.WidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.GwtWidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtDelegateFactory;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtWidgetElement;
import net.bluemind.ui.gwtsharing.client.BaseSharingsEditor;
import net.bluemind.ui.gwtsharing.client.SharingsModel;

public class MailboxesSharingsEditor extends BaseSharingsEditor implements IMailboxSharingEditor {

	public static final String TYPE = "bm.mailbox.MailboxesSharingsEditor";
	private Map<String, String> datalocations;
	
	@Override
	public void loadModel(JavaScriptObject model) {
		super.loadModel(model);
		datalocations.clear();
		SharingsModel sharingModel = model.<JsMapStringJsObject>cast().get(MailboxesSharingsModelHandler.MODEL_ID).cast();
		for(String containerUid: sharingModel.getContainers()) {
			datalocations.put(containerUid, sharingModel.getDataLocation(containerUid));
		}
		edit.setPublicSharingVisible(false);

	}

	public MailboxesSharingsEditor() {
		super(MailboxesSharingsModelHandler.MODEL_ID, "mailboxacl");
		datalocations = new HashMap<>();
		edit.registerValidator(new CrossShardSharingValidator(this));
	}

	public static void registerType() {
		GwtWidgetElement.register(TYPE, new IGwtDelegateFactory<IGwtWidgetElement, WidgetElement>() {

			@Override
			public IGwtWidgetElement create(WidgetElement e) {
				return new MailboxesSharingsEditor();
			}
		});

	}

	@Override
	public String getMailboxDataLocation() {
		return datalocations.get(edit.getContainerUid());
	}
}
