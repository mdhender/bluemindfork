/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2016
  *
  * This file is part of Blue Mind. Blue Mind is a messaging and collaborative
  * solution.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of either the GNU Affero General Public License as
  * published by the Free Software Foundation (version 3 of the License)
  * or the CeCILL as published by CeCILL.info (version 2 of the License).
  *
  * There are special exceptions to the terms and conditions of the
  * licenses as they are applied to this program. See LICENSE.txt in
  * the directory of this program distribution.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.ui.adminconsole.monitoring.util;

import java.util.Date;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style.TextAlign;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.user.client.ui.HTMLPanel;

import net.bluemind.monitoring.api.Status;

/**
 * Utility class to format information
 * 
 * @author vincent
 *
 */
public abstract class UIFormatter {

	public static String WIDGET_WIDTH = "750px";
	public static String TITLE_COLOR = "white";
	/**
	 * Size (height) in terms of visible elements of a list box
	 */
	public static int LIST_VISIBLE_ITEM_COUNT = 7;

	public static HTMLPanel newTitle(String tag, String title, Status status) {

		HTMLPanel theTitle = new HTMLPanel(tag, title);
		UIFormatter.formatTitle(theTitle.getElement(), status);

		return theTitle;

	}

	public static HTMLPanel newTitle(String tag, String title) {

		HTMLPanel theTitle = new HTMLPanel(tag, title);

		theTitle.getElement().getStyle().setPadding(0.35, Unit.EM);
		theTitle.getElement().getStyle().setTextAlign(TextAlign.CENTER);

		return theTitle;

	}

	/**
	 * Returns a color according to the given status
	 * 
	 * @param status
	 * @return the name of the color to be returned
	 */
	public static String statusColor(Status status) {

		switch (status) {
		case KO:
			return "red";
		case OK:
			return "green";
		case UNKNOWN:
			return "gray";
		case WARNING:
			return "orange";
		default:
			return "gray";
		}

	}

	public static void formatTitle(Element e, Status status) {
		e.getStyle().setColor(UIFormatter.statusColor(status));
		e.getStyle().setPadding(0.35, Unit.EM);
		e.getStyle().setTextAlign(TextAlign.CENTER);
	}

	public static String getDateTime() {
		Date date = new Date();
		DateTimeFormat dtf = DateTimeFormat.getFormat("dd/MM/yyyy HH:mm:ss");
		return dtf.format(date);
	}

}
