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
package net.bluemind.gwtconsoleapp.base.editor;

public class ScreenElementTypes {

	public native static void contribute(ScreenElement contributed, String attribute, ScreenElement contribution)
	/*-{
	var type = null;
	
	var curpart = $wnd;
	var parts = contributed["type"].split('.');
	
	while (parts.length != 0 && curpart != null) {
	  var part = parts.shift();
	  if (!curpart[part]) {
	    curpart = curpart[part];
	  }
	}
	
	if (curpart != null) {
	  curpart['contribute'](contributed, attribute, contribution);
	}
	}-*/;

	public static boolean instanceOf(String type, String type2) {
		// TODO Auto-generated method stub
		return false;
	}

}
