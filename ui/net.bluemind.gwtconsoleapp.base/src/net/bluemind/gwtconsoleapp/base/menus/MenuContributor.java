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
package net.bluemind.gwtconsoleapp.base.menus;

import com.google.gwt.core.client.JavaScriptObject;

public class MenuContributor extends JavaScriptObject {

	protected MenuContributor() {
	}

	public native final MenuContribution contribution() /*-{
														return this.contribute();
														}-*/;

	public native final static MenuContributor create(final MenuContributorUnwrapped wrapper)
	/*-{
	return {
	
	  'contribute' : function() {
	    return wrapper.@net.bluemind.gwtconsoleapp.base.menus.MenuContributorUnwrapped::contribution()();
	  }
	};
	}-*/;

	public native final static void exportAsfunction(String functionName, final MenuContributor wrapper)
	/*-{
	
	$wnd[functionName] = function() {
	  return wrapper;
	};
	}-*/;

}
