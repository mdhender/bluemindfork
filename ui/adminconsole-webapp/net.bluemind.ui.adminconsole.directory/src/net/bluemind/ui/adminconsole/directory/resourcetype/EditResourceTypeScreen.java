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
package net.bluemind.ui.adminconsole.directory.resourcetype;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.shared.GWT;

import net.bluemind.core.commons.gwt.JsMapStringJsObject;
import net.bluemind.gwtconsoleapp.base.editor.ModelHandler;
import net.bluemind.gwtconsoleapp.base.editor.ScreenElement;
import net.bluemind.gwtconsoleapp.base.editor.ScreenRoot;
import net.bluemind.gwtconsoleapp.base.editor.Tab;
import net.bluemind.gwtconsoleapp.base.editor.TabContainer;
import net.bluemind.gwtconsoleapp.base.editor.gwt.GwtScreenRoot;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtCompositeScreenRoot;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtDelegateFactory;
import net.bluemind.resource.api.type.gwt.js.JsResourceTypeDescriptor;
import net.bluemind.ui.adminconsole.base.Actions;
import net.bluemind.ui.adminconsole.base.ui.BaseEditScreen;
import net.bluemind.ui.adminconsole.directory.resourcetype.l10n.ResourceTypeConstants;

public class EditResourceTypeScreen extends BaseEditScreen {

	private static final String TYPE = "bm.ac.EditResourceTypeScreen";

	private EditResourceTypeScreen(ScreenRoot screenRoot) {
		super(screenRoot);
		icon.setStyleName("fa fa-2x fa-briefcase");
	}

	@Override
	public void loadModel(JavaScriptObject model) {
		JsMapStringJsObject map = model.cast();
		JsResourceTypeDescriptor rt = map.get("resourceType").cast();
		title.setInnerText(rt.getLabel());
		icon.setStyleName("fa fa-2x fa-briefcase");
	}

	public static void registerType() {
		GwtScreenRoot.registerComposite(EditResourceTypeScreen.TYPE,
				new IGwtDelegateFactory<IGwtCompositeScreenRoot, ScreenRoot>() {

					@Override
					public IGwtCompositeScreenRoot create(ScreenRoot screenRoot) {
						return new EditResourceTypeScreen(screenRoot);
					}
				});
	}

	@Override
	protected void doCancel() {
		Actions.get().showWithParams2("resourcetype", null);
	}

	public static ScreenElement screenModel() {
		ResourceTypeConstants c = GWT.create(ResourceTypeConstants.class);

		ScreenRoot screenRoot = ScreenRoot.create("editResourceType", TYPE).cast();
		screenRoot.getHandlers().push(ModelHandler.create(null, ResourceTypeModelHandler.TYPE).<ModelHandler> cast());

		JsArray<Tab> tabs = JavaScriptObject.createArray().cast();
		tabs.push(Tab.create(null, c.generalTab(), ScreenElement.create(null, ResourceTypeGeneralEditor.TYPE)));

		TabContainer tab = TabContainer.create(null, tabs);
		screenRoot.setContent(tab);
		return screenRoot;
	}

}
