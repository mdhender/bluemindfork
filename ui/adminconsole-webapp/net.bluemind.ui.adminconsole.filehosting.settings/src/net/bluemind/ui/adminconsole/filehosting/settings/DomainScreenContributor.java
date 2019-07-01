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
package net.bluemind.ui.adminconsole.filehosting.settings;

import com.google.gwt.core.client.JsArray;

import net.bluemind.gwtconsoleapp.base.editor.ScreenElement;
import net.bluemind.gwtconsoleapp.base.editor.ScreenElementContribution;
import net.bluemind.gwtconsoleapp.base.editor.ScreenElementContributorUnwrapper;
import net.bluemind.gwtconsoleapp.base.editor.Tab;
import net.bluemind.ui.adminconsole.filehosting.settings.l10n.FileHosting;

public class DomainScreenContributor implements ScreenElementContributorUnwrapper {

	@Override
	public JsArray<ScreenElementContribution> contribution() {
		JsArray<ScreenElementContribution> contribs = JsArray.createArray().cast();

		ScreenElementContribution fileHostingTabSystemConfig = ScreenElementContribution.create("editDomainTabs",
				"tabs", Tab.create("fileHostingSystemTab", FileHosting.INST.tabName(),
						ScreenElement.create(null, DomainFileHostingSettingsEditor.TYPE)));
		contribs.push(fileHostingTabSystemConfig);

		ScreenElementContribution fileHostingTabDomainConfig = ScreenElementContribution.create("editSystemConfTabs",
				"tabs", Tab.create("fileHostingDomainTab", FileHosting.INST.tabName(),
						ScreenElement.create(null, EditFileHostingSettingsEditor.TYPE)));

		contribs.push(fileHostingTabDomainConfig);
		return contribs;
	}
}
