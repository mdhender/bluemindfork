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

import com.google.gwt.user.client.ui.Anchor;

public class AppAnchor extends Anchor {

	public AppAnchor() {

	}

	public AppAnchor(String label, final String appName) {
		setText(label);
		addStyleName("appAnchor");
		addStyleName("clickable");
	}

	public void setSelected(boolean selected) {
		if (selected) {
			addStyleName("selected");
		} else {
			removeStyleName("selected");
		}
	}
}
