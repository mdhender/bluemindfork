/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2015
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

import net.bluemind.gwtconsoleapp.base.editor.ScreenElementContribution;
import net.bluemind.gwtconsoleapp.base.editor.ScreenElementContributorUnwrapper;
import net.bluemind.gwtconsoleapp.base.editor.ScreenRoot;
import net.bluemind.ui.adminconsole.monitoring.screens.GlobalStatusScreen;
import net.bluemind.ui.adminconsole.monitoring.screens.ServiceInformationScreen;

public class MonitoringScreensContributor implements ScreenElementContributorUnwrapper {

	@Override
	public JsArray<ScreenElementContribution> contribution() {

		JsArray<ScreenElementContribution> contribs = JsArray.createArray().cast();

		contribs.push(ScreenElementContribution.create(null, null,
				ScreenRoot.create("checkGlobalStatus", GlobalStatusScreen.TYPE)));
		contribs.push(ScreenElementContribution.create(null, null,
				ScreenRoot.create("checkInformation", ServiceInformationScreen.TYPE)));

		return contribs;
	}
}
