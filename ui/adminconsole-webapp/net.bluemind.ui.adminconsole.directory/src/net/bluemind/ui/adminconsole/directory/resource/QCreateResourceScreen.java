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
package net.bluemind.ui.adminconsole.directory.resource;

import java.util.HashMap;

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
import net.bluemind.ui.adminconsole.directory.resource.l10n.ResourceConstants;

public class QCreateResourceScreen extends BaseQCreateScreen {

	public static final String TYPE = "bm.ac.QCreateResourceScreen";

	private QCreateResourceScreen(ScreenRoot screen) {
		super(screen);
		icon.setStyleName("fa fa-2x fa-briefcase");
		title.setInnerText(ResourceConstants.INST.newResource());
	}

	public static void registerType() {
		GwtScreenRoot.registerComposite(TYPE, new IGwtDelegateFactory<IGwtCompositeScreenRoot, ScreenRoot>() {

			@Override
			public IGwtCompositeScreenRoot create(ScreenRoot screenRoot) {
				return new QCreateResourceScreen(screenRoot);
			}
		});
		GWT.log("bm.ac.QCreateResourceScreen registred");
	}

	@Override
	protected void doCancel() {
		Actions.get().showWithParams2("directory", null);
	}

	@Override
	protected void doEditCreated() {
		String userId = rootScreen.getModel().<JsMapStringJsObject> cast().getString("id");
		String domainUid = rootScreen.getModel().<JsMapStringJsObject> cast().getString("domainUid");
		HashMap<String, String> params = new HashMap<>();
		params.put("entryUid", userId);
		params.put("domainUid", domainUid);
		Actions.get().showWithParams2("editResource", params);
	}

	public static ScreenElement screenModel() {
		ScreenRoot screenRoot = ScreenRoot.create("qcResource", TYPE).cast();
		screenRoot.setOverlay(true);
		screenRoot.setSizeHint(SizeHint.create(500, 450));
		screenRoot.getHandlers()
				.push(ModelHandler.create(null, QCreateResourceModelHandler.TYPE).<ModelHandler> cast());
		screenRoot.setContent(ScreenElement.create(null, NewResource.TYPE).<CompositeElement> cast());
		return screenRoot;
	}

}
