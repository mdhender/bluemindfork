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

public class ScreenElementContribution extends JavaScriptObject {

	protected ScreenElementContribution() {
	}

	public native final String getContributedElementId()
	/*-{
		return this["contributedElementId"];
	}-*/;

	public native final String getContributedAttribute()
	/*-{
		return this["contributedAttribute"];
	}-*/;

	public native final ScreenElement getContribution()
	/*-{
		return this["contribution"];
	}-*/;

	public native static ScreenElementContribution create(String contributedElementId, String contributedAttribute,
			ScreenElement contribution)

	/*-{
		return {
			'contributedElementId' : contributedElementId,
			'contributedAttribute' : contributedAttribute,
			'contribution' : contribution
		};
	}-*/;

}