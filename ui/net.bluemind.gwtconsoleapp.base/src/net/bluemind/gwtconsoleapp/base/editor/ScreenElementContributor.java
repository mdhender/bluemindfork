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

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;

public class ScreenElementContributor extends JavaScriptObject {

	protected ScreenElementContributor() {
	}

	public native final JsArray<ScreenElementContribution> contribution()
	/*-{
		return this.contribute();
	}-*/;

	public native final static ScreenElementContributor create(final ScreenElementContributorUnwrapper wrapper)
	/*-{
		return {
	
			'contribute' : function() {
				return wrapper.@net.bluemind.gwtconsoleapp.base.editor.ScreenElementContributorUnwrapper::contribution()();
			}
		};
	}-*/;

	public native final static void exportAsfunction(String functionName, final ScreenElementContributor wrapper)
	/*-{
	
		$wnd[functionName] = function() {
			return wrapper;
		};
	}-*/;

}
