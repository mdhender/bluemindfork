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

package net.bluemind.ui.adminconsole.jobs;

import com.google.gwt.core.client.GWT;

import net.bluemind.gwtconsoleapp.base.editor.ScreenElementContributor;
import net.bluemind.gwtconsoleapp.base.menus.MenuContributor;

public class JobsPlugin {

	public static void init() {
		GWT.log("Init jobs plugin");
		JobsManager.registerType();
		EditJob.registerType();
		JobLogsViewer.registerType();
		LiveLogsViewer.registerType();

		ScreenElementContributor.exportAsfunction("NetBluemindUiAdminconsoleJobsScreensContributor",
				ScreenElementContributor.create(new JobsScreenContributor()));
		MenuContributor.exportAsfunction("NetBluemindUiAdminconsoleJobsMenuContributor",
				MenuContributor.create(new JobsMenusContributor()));

	}

}
