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
package net.bluemind.ui.common.client.forms;

import com.google.gwt.user.client.ui.ListBox;

public class TimePickerMs extends ListBox {

	public TimePickerMs() {
		initValues();

	}

	protected void initValues() {
		String s;
		String v;
		for (int i = 0; i < 48; i++) {
			if ((i % 2) == 0) {
				s = i / 2 + ":00";
			} else {
				s = i / 2 + ":30";
			}

			v = "" + ((i * 1000 * 60 * 60) / 2);

			addItem(s, v);
		}
	}

	public void setValue(Integer value) {
		if (value == null) {
			return;
		}
		String v = String.valueOf(value);
		for (int i = 0; i < getItemCount(); i++) {
			if (v.equals(getValue(i))) {
				setSelectedIndex(i);
				break;
			}
		}
	}
}