/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2014
 *
 * This file is part of Blue Mind. Blue Mind is a messaging and collaborative
 * solution.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of either the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License)
 * or the CeCILL as published by CeCILL.info (version 2 of the License).
 *
 * There are special exceptions to the terms and conditions of the
 * licenses as they are applied to this program. See LICENSE.txt in
 * the directory of this program distribution.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.ui.adminconsole.monitoring;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.shared.GWT;

import net.bluemind.gwtconsoleapp.base.menus.Contributed;
import net.bluemind.gwtconsoleapp.base.menus.MenuContribution;
import net.bluemind.gwtconsoleapp.base.menus.MenuContributorUnwrapped;
import net.bluemind.gwtconsoleapp.base.menus.Screen;
import net.bluemind.gwtconsoleapp.base.menus.Section;
import net.bluemind.ui.adminconsole.monitoring.l10n.MonitoringMenuConstants;

public class MonitoringMenusContributor implements MenuContributorUnwrapped {

	@Override
	public MenuContribution contribution() {
		MonitoringMenuConstants v = GWT.create(MonitoringMenuConstants.class);

		JsArray<Contributed<Section>> sections = JsArray.createArray().cast();
		JsArray<Contributed<Screen>> screens = JsArray.createArray().cast();
		JsArray<Section> ssections = JsArray.createArray().cast();

		ssections.push(Section.createVerySimple("bmHealth", v.bmHealthSectionTitle(), null));

		// rajouter la nouvelle section à la console d'administration
		sections.push(Contributed.create(null, Section.create("monitoring", v.monitoringGeneralTitle(), 97,
				"fa-pie-chart", JsArray.createArray().<JsArray<Screen>>cast(), ssections)));

		screens.push(Contributed.create("bmHealth",
				Screen.create("checkGlobalStatus", v.globalStatusScreenTitle(), "bmMetrics", true)));

		return MenuContribution.create(sections, screens);
	}
}
