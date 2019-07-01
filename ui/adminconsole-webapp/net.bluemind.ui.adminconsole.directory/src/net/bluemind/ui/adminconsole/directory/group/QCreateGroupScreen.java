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

import com.google.gwt.core.shared.GWT;

import net.bluemind.core.commons.gwt.JsMapStringJsObject;
import net.bluemind.gwtconsoleapp.base.editor.CompositeElement;
import net.bluemind.gwtconsoleapp.base.editor.ModelHandler;
import net.bluemind.gwtconsoleapp.base.editor.ScreenElement;
import net.bluemind.gwtconsoleapp.base.editor.ScreenRoot;
import net.bluemind.gwtconsoleapp.base.editor.ScreenRoot.SizeHint;
import net.bluemind.gwtconsoleapp.base.editor.gwt.GwtScreenRoot;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtCompositeScreenRoot;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtDelegateFactory;
import net.bluemind.ui.adminconsole.base.Actions;
import net.bluemind.ui.adminconsole.directory.BaseQCreateScreen;
import net.bluemind.ui.adminconsole.directory.group.l10n.GroupMenusConstants;

public class QCreateGroupScreen extends BaseQCreateScreen {

	public static final String TYPE = "bm.ac.QCreateGroupScreen";

	public QCreateGroupScreen(ScreenRoot screen) {
		super(screen);
		GroupMenusConstants constants = GWT.create(GroupMenusConstants.class);
		title.setInnerHTML(constants.qcGroup());
		icon.setStyleName("fa fa-2x fa-users");
	}

	public static void registerType() {
		GwtScreenRoot.registerComposite(TYPE, new IGwtDelegateFactory<IGwtCompositeScreenRoot, ScreenRoot>() {

			@Override
			public IGwtCompositeScreenRoot create(ScreenRoot screenRoot) {
				return new QCreateGroupScreen(screenRoot);
			}
		});
		GWT.log("bm.ac.QCreateGroupScreen registred");
	}

	@Override
	protected void doCancel() {
		Actions.get().showWithParams2("directory", null);
	}

	@Override
	protected void doEditCreated() {
		String domainUid = rootScreen.getModel().<JsMapStringJsObject> cast().getString("domainUid");
		String groupUid = rootScreen.getModel().<JsMapStringJsObject> cast().getString("groupUid");
		Map<String, String> params = new HashMap<>();
		params.put("domainUid", domainUid);
		params.put("entryUid", groupUid);
		Actions.get().showWithParams2("editGroup", params);
	}

	public static ScreenElement screenModel() {
		ScreenRoot screenRoot = ScreenRoot.create("qcGroup", TYPE).cast();
		screenRoot.setOverlay(true);
		screenRoot.setSizeHint(SizeHint.create(600, 400));
		screenRoot.getHandlers().push(ModelHandler.create(null, QCreateGroupModelHandler.TYPE).<ModelHandler> cast());
		screenRoot.setContent(ScreenElement.create(null, NewGroup.TYPE).<CompositeElement> cast());
		return screenRoot;
	}

}
