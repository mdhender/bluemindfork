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
import net.bluemind.ui.adminconsole.directory.mailshare.l10n.MailshareConstants;

public class QCreateMailshareScreen extends BaseQCreateScreen {
	private static final String TYPE = "bm.ac.QCreateMailshareScreen";

	private QCreateMailshareScreen(ScreenRoot screen) {
		super(screen);
		title.setInnerText(MailshareConstants.INST.newMailshare());
		icon.setStyleName("fa fa-2x fa-inbox");
	}

	public static void registerType() {
		GwtScreenRoot.registerComposite(TYPE, new IGwtDelegateFactory<IGwtCompositeScreenRoot, ScreenRoot>() {

			@Override
			public IGwtCompositeScreenRoot create(ScreenRoot screenRoot) {
				return new QCreateMailshareScreen(screenRoot);
			}
		});
		GWT.log("bm.ac.QCreateMailshareScreen registred");
	}

	@Override
	protected void doCancel() {
		Actions.get().showWithParams2("directory", null);
	}

	@Override
	protected void doEditCreated() {
		String mailshareId = rootScreen.getModel().<JsMapStringJsObject> cast().getString("id");
		String domainUid = rootScreen.getModel().<JsMapStringJsObject> cast().getString("domainUid");

		HashMap<String, String> params = new HashMap<>();
		params.put("entryUid", mailshareId);
		params.put("domainUid", domainUid);

		Actions.get().showWithParams2("editMailshare", params);

	}

	public static ScreenElement screenModel() {

		ScreenRoot screenRoot = ScreenRoot.create("qcMailshare", TYPE).cast();
		screenRoot.setOverlay(true);
		screenRoot.setSizeHint(SizeHint.create(550, 450));
		screenRoot.getHandlers()
				.push(ModelHandler.create(null, QCreateMailshareModelHandler.TYPE).<ModelHandler> cast());
		screenRoot.setContent(ScreenElement.create(null, NewMailshare.TYPE).<CompositeElement> cast());
		return screenRoot;
	}

}
