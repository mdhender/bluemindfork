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
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.notes.service.internal;

import net.bluemind.notes.api.VNote;
import net.bluemind.notes.api.VNote.Color;

public class VNoteSanitizer {

	public void sanitize(VNote vnote) {
		if (null == vnote) {
			return;
		}
		if (vnote.color == null) {
			vnote.color = Color.YELLOW;
		}
		if (vnote.posX == null) {
			vnote.posX = 0;
		}
		if (vnote.posY == null) {
			vnote.posY = 0;
		}
		if (vnote.width == null || vnote.width <= 0) {
			vnote.width = 150;
		}
		if (vnote.height == null || vnote.height <= 0) {
			vnote.height = 150;
		}
	}

}
