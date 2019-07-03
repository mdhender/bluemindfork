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
package net.bluemind.ui.adminconsole.dataprotect;

import com.google.gwt.cell.client.AbstractCell;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;

public class TooltipedImageCell extends AbstractCell<TippedResource> {

	public TooltipedImageCell() {

	}

	@Override
	public void render(com.google.gwt.cell.client.Cell.Context context, TippedResource value, SafeHtmlBuilder sb) {
		if (value != null) {
			sb.appendHtmlConstant("<i class=\"fa fa-lg " + value.iconStyle + "\"></i>");
		}
	}
}
