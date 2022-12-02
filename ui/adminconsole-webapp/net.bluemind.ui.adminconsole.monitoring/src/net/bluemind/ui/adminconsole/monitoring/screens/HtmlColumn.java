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
package net.bluemind.ui.adminconsole.monitoring.screens;

import com.google.gwt.cell.client.AbstractCell;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.cellview.client.Column;

public abstract class HtmlColumn<T> extends Column<T, String> {

	/**
	 * Construct a new HTMLColumn.
	 */
	public HtmlColumn() {
		super(new HTMLCell());
	}

	public static class HTMLCell extends AbstractCell<String> {

		public HTMLCell() {
		}

		@SuppressWarnings("serial")
		@Override
		public void render(Context context, String value, SafeHtmlBuilder sb) {
			if (value != null) {
				sb.append(new SafeHtml() {

					@Override
					public String asString() {
						return value;
					}
				});
			}
		}
	}
}