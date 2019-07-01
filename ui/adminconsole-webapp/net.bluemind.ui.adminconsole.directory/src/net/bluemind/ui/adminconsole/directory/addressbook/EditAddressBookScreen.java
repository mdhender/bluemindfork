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

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.shared.GWT;

import net.bluemind.addressbook.api.gwt.js.JsAddressBookDescriptor;
import net.bluemind.core.commons.gwt.JsMapStringJsObject;
import net.bluemind.gwtconsoleapp.base.editor.ModelHandler;
import net.bluemind.gwtconsoleapp.base.editor.ScreenElement;
import net.bluemind.gwtconsoleapp.base.editor.ScreenRoot;
import net.bluemind.gwtconsoleapp.base.editor.Tab;
import net.bluemind.gwtconsoleapp.base.editor.TabContainer;
import net.bluemind.gwtconsoleapp.base.editor.gwt.GwtScreenRoot;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtCompositeScreenRoot;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtDelegateFactory;
import net.bluemind.role.api.BasicRoles;
import net.bluemind.ui.adminconsole.base.Actions;
import net.bluemind.ui.adminconsole.base.ui.BaseEditScreen;
import net.bluemind.ui.adminconsole.directory.addressbook.l10n.AddressBookMenusConstants;

public class EditAddressBookScreen extends BaseEditScreen {

	private static final String TYPE = "bm.ac.EditAddressBookScreen";

	private EditAddressBookScreen(ScreenRoot screenRoot) {
		super(screenRoot);
		icon.setStyleName("fa fa-2x fa-book");
	}

	@Override
	public void loadModel(JavaScriptObject model) {
		JsMapStringJsObject map = model.cast();
		JsAddressBookDescriptor ab = map.get("addressbook").cast();
		title.setInnerText(ab.getName());
	}

	public static void registerType() {
		GwtScreenRoot.registerComposite(TYPE, new IGwtDelegateFactory<IGwtCompositeScreenRoot, ScreenRoot>() {

			@Override
			public IGwtCompositeScreenRoot create(ScreenRoot screenRoot) {
				return new EditAddressBookScreen(screenRoot);
			}
		});
	}

	@Override
	protected void doCancel() {
		Actions.get().showWithParams2("directory", null);
	}

	@Override
	public void doLoad(ScreenRoot screenRoot) {
		screenRoot.getState().put("bookId", screenRoot.getState().get("entryUid"));
		super.doLoad(screenRoot);
	}

	public static ScreenElement screenModel() {
		AddressBookMenusConstants c = GWT.create(AddressBookMenusConstants.class);

		ScreenRoot screenRoot = ScreenRoot.create("editBook", TYPE).cast();
		screenRoot.getHandlers().push(ModelHandler.create(null, AddressBookModelHandler.TYPE).readOnly()
				.withRole(BasicRoles.ROLE_MANAGE_DOMAIN_AB).<ModelHandler> cast());
		screenRoot.getHandlers().push(ModelHandler.create(null, AddressBookSharingModelHandler.TYPE)
				.withRole(BasicRoles.ROLE_MANAGE_DOMAIN_AB_SHARING).<ModelHandler> cast());

		JsArray<Tab> tabs = JavaScriptObject.createArray().cast();
		tabs.push(Tab.create(null, c.generalTab(), ScreenElement.create(null, EditAddressBook.TYPE).readOnly()
				.withRole(BasicRoles.ROLE_MANAGE_DOMAIN_AB)));
		tabs.push(Tab.create(null, c.sharingTab(), ScreenElement.create(null, AddressBookSharingEditor.TYPE)
				.withRole(BasicRoles.ROLE_MANAGE_DOMAIN_AB_SHARING)));

		TabContainer tab = TabContainer.create(null, tabs);
		screenRoot.setContent(tab);
		return screenRoot;
	}

}
