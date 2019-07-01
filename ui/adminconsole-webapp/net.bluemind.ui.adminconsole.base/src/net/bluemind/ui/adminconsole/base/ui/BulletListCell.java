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
package net.bluemind.ui.adminconsole.base.ui;

import java.util.Collection;

import com.google.gwt.cell.client.AbstractCell;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;

public class BulletListCell extends AbstractCell<Collection<String>> {

	@Override
	public void render(com.google.gwt.cell.client.Cell.Context context, Collection<String> value, SafeHtmlBuilder sb) {
		sb.appendHtmlConstant("<ul style=\"margin:0px; padding-left: 10px;\">");

		for (String s : value) {
			sb.appendHtmlConstant("<li>");
			sb.appendEscaped(s);
			sb.appendHtmlConstant("</li>");
		}
		sb.appendHtmlConstant("</ul>");
	}

}
