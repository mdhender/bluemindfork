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
package net.bluemind.ui.adminconsole.base.orgunit;

import com.google.gwt.core.client.JavaScriptObject;

import jsinterop.annotations.JsConstructor;
import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;
import net.bluemind.core.commons.gwt.JsMapStringJsObject;

@JsType(isNative = false, name = "OrgUnitsAdministratorModel", namespace = "bm.orgunit")
public class OrgUnitsAdministratorModel {

	@JsConstructor
	public OrgUnitsAdministratorModel() {

	}

	@JsProperty(name = "orgUnits")
	public OrgUnitAdministratorModel[] orgUnits;

	public void set(JavaScriptObject model) {
		JsMapStringJsObject map = model.cast();
		map.put("delegationModel", this);
	}

	public static OrgUnitsAdministratorModel get(JavaScriptObject model) {
		JsMapStringJsObject map = model.cast();
		return map.getObject("delegationModel");
	}
}
