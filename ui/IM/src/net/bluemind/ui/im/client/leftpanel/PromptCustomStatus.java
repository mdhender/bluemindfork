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
package net.bluemind.ui.im.client.leftpanel;

import net.bluemind.ui.common.client.forms.window.WindowPrompt;
import net.bluemind.ui.im.client.IMCtrl;

public class PromptCustomStatus extends WindowPrompt {

	private String mode;

	public PromptCustomStatus(String header, String content, int priority) {
		super(header, content);
	}

	@Override
	protected void onOk() {
		String status = getValue();
		IMCtrl.getInstance().setPresence(mode, status);
	}

	public void setMode(String mode) {
		this.mode = mode;
	}

}
