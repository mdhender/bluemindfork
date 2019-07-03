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
package net.bluemind.ui.im.client.subscription;

import com.google.gwt.user.client.Command;

import net.bluemind.ui.common.client.forms.window.WindowConfirm;
import net.bluemind.ui.im.client.IMConstants;
import net.bluemind.ui.im.client.IMCtrl;
import net.bluemind.ui.im.client.IScreen;

public class RemoveFromFavorites extends WindowConfirm implements IScreen {

	private String jabberId;

	public RemoveFromFavorites() {
		setHeaderMsg(IMConstants.INST.removeFromFavoritePopupHeader());
		setOkCmd(new Command() {
			@Override
			public void execute() {
				IMCtrl.getInstance().removeBuddy(jabberId);
			}
		});
	}

	public void setJabberId(String jabberId) {
		this.jabberId = jabberId;
		showDialog();
	}

	private void showDialog() {
		setContentMsg(IMConstants.INST.removeFromFavorites(jabberId));
		show();
	}
}
