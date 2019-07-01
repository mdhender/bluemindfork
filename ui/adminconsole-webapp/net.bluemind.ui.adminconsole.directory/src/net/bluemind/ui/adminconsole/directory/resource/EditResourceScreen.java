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

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.shared.GWT;

import net.bluemind.core.commons.gwt.JsMapStringJsObject;
import net.bluemind.gwtconsoleapp.base.editor.ContainerElement;
import net.bluemind.gwtconsoleapp.base.editor.ModelHandler;
import net.bluemind.gwtconsoleapp.base.editor.ScreenElement;
import net.bluemind.gwtconsoleapp.base.editor.ScreenRoot;
import net.bluemind.gwtconsoleapp.base.editor.Tab;
import net.bluemind.gwtconsoleapp.base.editor.TabContainer;
import net.bluemind.gwtconsoleapp.base.editor.gwt.GwtScreenRoot;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtCompositeScreenRoot;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtDelegateFactory;
import net.bluemind.resource.api.type.gwt.js.JsResourceTypeDescriptor;
import net.bluemind.role.api.BasicRoles;
import net.bluemind.ui.adminconsole.base.Actions;
import net.bluemind.ui.adminconsole.base.ui.BaseEditScreen;
import net.bluemind.ui.adminconsole.directory.calendar.CalendarSettingsEditor;
import net.bluemind.ui.adminconsole.directory.calendar.CalendarSettingsModelHandler;
import net.bluemind.ui.adminconsole.directory.mailshare.DomainLoader;
import net.bluemind.ui.adminconsole.directory.resource.l10n.ResourceMenusConstants;
import net.bluemind.ui.gwtuser.client.FreebusySharingEditor;
import net.bluemind.ui.gwtuser.client.FreebusySharingModelHandler;
import net.bluemind.ui.gwtuser.client.l10n.FreeBusyConstants;

public class EditResourceScreen extends BaseEditScreen {

	public static final String TYPE = "bm.ac.EditResourceScreen";

	private EditResourceScreen(ScreenRoot screenRoot) {
		super(screenRoot);
		icon.setStyleName("fa fa-2x fa-briefcase");
	}

	@Override
	public void loadModel(JavaScriptObject model) {
		JsMapStringJsObject map = model.cast();
		JsResourceTypeDescriptor rt = map.get("resource").cast();
		title.setInnerText(rt.getLabel());
	}

	public static void registerType() {
		GwtScreenRoot.registerComposite("bm.ac.EditResourceScreen",
				new IGwtDelegateFactory<IGwtCompositeScreenRoot, ScreenRoot>() {

					@Override
					public IGwtCompositeScreenRoot create(ScreenRoot screenRoot) {
						return new EditResourceScreen(screenRoot);
					}
				});
	}

	@Override
	public void doLoad(ScreenRoot screenRoot) {
		screenRoot.getState().put("resourceId", screenRoot.getState().get("entryUid"));
		screenRoot.getState().put("entryUid", screenRoot.getState().get("entryUid"));
		super.doLoad(screenRoot);
	}

	@Override
	protected void doCancel() {
		Actions.get().showWithParams2("directory", null);
	}

	public static ScreenElement screenModel() {
		ResourceMenusConstants c = GWT.create(ResourceMenusConstants.class);

		ScreenRoot screenRoot = ScreenRoot.create("editResource", TYPE).cast();
		screenRoot.getHandlers().push(ModelHandler.create(null, ResourceModelHandler.TYPE).readOnly()
				.withRole(BasicRoles.ROLE_MANAGE_RESOURCE).<ModelHandler> cast());
		screenRoot.getHandlers().push(ModelHandler.create(null, ResourceCalendarSharingModelHandler.TYPE)
				.withRole(BasicRoles.ROLE_MANAGE_RESOURCE_SHARINGS).<ModelHandler> cast());
		screenRoot.getHandlers().push(ModelHandler.create(null, DomainLoader.TYPE).<ModelHandler> cast());

		screenRoot.getHandlers().push(ModelHandler.create(null, FreebusySharingModelHandler.TYPE)
				.withRole(BasicRoles.ROLE_MANAGE_RESOURCE_SHARINGS).<ModelHandler> cast());

		screenRoot.getHandlers().push(ModelHandler.create(null, CalendarSettingsModelHandler.TYPE)
				.withRole(BasicRoles.ROLE_MANAGE_RESOURCE).<ModelHandler> cast());

		JsArray<Tab> tabs = JavaScriptObject.createArray().cast();
		tabs.push(Tab.create(null, c.basicParameterTab(),
				ScreenElement.create(null, EditResource.TYPE).readOnly().withRole(BasicRoles.ROLE_MANAGE_RESOURCE)));

		tabs.push(Tab.create(null, c.workingDayTab(),
				ScreenElement.create(null, CalendarSettingsEditor.TYPE).withRole(BasicRoles.ROLE_MANAGE_RESOURCE)));

		JsArray<ScreenElement> calendarsContent = JsArray.createArray().cast();

		ScreenElement calendarSharing = ScreenElement.create(null, ResourceCalendarSharingEditor.TYPE)
				.withRole(BasicRoles.ROLE_MANAGE_RESOURCE_SHARINGS);
		calendarSharing.setTitle(c.calendarSharing());
		calendarsContent.push(calendarSharing);

		ScreenElement fbSharingEditor = ScreenElement.create(null, FreebusySharingEditor.TYPE)
				.withRole(BasicRoles.ROLE_MANAGE_RESOURCE_SHARINGS);
		fbSharingEditor.setTitle(FreeBusyConstants.INST.sharing());
		calendarsContent.push(fbSharingEditor);

		tabs.push(Tab.create(null, c.calendarSharing(),
				ContainerElement.create("resourceCalendarSharing", calendarsContent)));

		TabContainer tab = TabContainer.create(null, tabs);
		screenRoot.setContent(tab);
		return screenRoot;
	}

}
