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
package net.bluemind.ui.settings.client;

import net.bluemind.gwtconsoleapp.base.editor.gwt.ProgressDialogPanel;

public class SettingsCtrl {

	private static final SettingsCtrl inst = new SettingsCtrl();

	private ProgressDialogPanel progress = new ProgressDialogPanel();

	public static SettingsCtrl get() {
		return inst;
	}

	public void showProgressbar(String text) {
		progress.setText(text);
		progress.center();
	}

	public void hideProgressbar() {
		progress.hide();
	}

}
