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
package net.bluemind.ui.adminconsole.directory.ou;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;

public class OrgUnitStyle {

	public static interface Resources extends ClientBundle {
		@Source("OrgUnitItem.css")
		Style orgUnitItemStyle();
	}

	public static interface Style extends CssResource {
		String itemTree();

		String checkboxLabel();

		String checkboxTitle();

	}

	private static final Resources RES = GWT.create(Resources.class);

	public static Style getOrgUnitStyle() {
		return RES.orgUnitItemStyle();
	}
}
