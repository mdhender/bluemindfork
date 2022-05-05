/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2022
  *
  * This file is part of BlueMind. BlueMind is a messaging and collaborative
  * solution.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of either the GNU Affero General Public License as
  * published by the Free Software Foundation (version 3 of the License).
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.ui.adminconsole.videoconferencing.teams;

import com.google.gwt.core.client.EntryPoint;

import net.bluemind.gwtconsoleapp.base.editor.BasePlugin;
import net.bluemind.gwtconsoleapp.base.editor.ScreenElementContributor;

public class TeamsPlugin implements EntryPoint {
	@Override
	public void onModuleLoad() {
		init();
	}

	private void init() {
		BasePlugin.install();

		TeamsEditor.registerType();

		ScreenElementContributor.exportAsfunction("NetBluemindUiAdminconsoleVideoConferencingTeamsScreenContributor",
				ScreenElementContributor.create(new TeamsScreenContributor()));
	}

}
