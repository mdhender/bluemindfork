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
package net.bluemind.ui.adminconsole.system.hosts.edit;

import com.google.gwt.core.client.JsArray;

import net.bluemind.gwtconsoleapp.base.editor.ScreenElement;
import net.bluemind.gwtconsoleapp.base.editor.ScreenElementContribution;
import net.bluemind.gwtconsoleapp.base.editor.Tab;
import net.bluemind.gwtconsoleapp.base.editor.TabContainer;
import net.bluemind.ui.adminconsole.system.hosts.l10n.HostConstants;

public class EditHostScreens {

	public static JsArray<ScreenElementContribution> contribution() {

		ScreenElement contribution = ScreenElement.create("editHost", EditHostScreen.TYPE);

		JsArray<ScreenElementContribution> contribs = JsArray.createArray().cast();
		contribs.push(ScreenElementContribution.create(null, null, contribution));

		contribs.push(ScreenElementContribution.create("editHost", "modelHandlers",
				ScreenElement.create(null, DomainTemplateModelHandler.TYPE)));
		contribs.push(ScreenElementContribution.create("editHost", "modelHandlers",
				ScreenElement.create(null, UserLanguageModelHandler.TYPE)));
		contribs.push(ScreenElementContribution.create("editHost", "modelHandlers",
				ScreenElement.create(null, ServerModelHandler.TYPE)));

		JsArray<Tab> tabs = JsArray.createArray().cast();

		tabs.push(Tab.create(null, HostConstants.INST.generalTab(),
				ScreenElement.create("editHostBasic", EditHostBasicEditor.TYPE)));

		tabs.push(Tab.create(null, HostConstants.INST.tagsTab(),
				ScreenElement.create("editHostServerRoles", EditHostServerRolesEditor.TYPE)));

		contribs.push(
				ScreenElementContribution.create("editHost", "content", TabContainer.create("editHostTabs", tabs)));
		return contribs;
	}

}
