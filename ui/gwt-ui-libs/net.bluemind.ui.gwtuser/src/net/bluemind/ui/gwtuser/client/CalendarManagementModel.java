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
package net.bluemind.ui.gwtuser.client;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.json.client.JSONArray;

import net.bluemind.core.commons.gwt.GwtSerDerUtils;
import net.bluemind.core.container.model.ContainerDescriptor;
import net.bluemind.core.container.model.gwt.js.JsContainerDescriptor;
import net.bluemind.core.container.model.gwt.serder.ContainerDescriptorGwtSerDer;

public class CalendarManagementModel extends JavaScriptObject {

	protected CalendarManagementModel() {

	}

	public final native JsArray<JsContainerDescriptor> getCalendars()
	/*-{
		return this['calendars'];
	}-*/;

	public final native void setCalendars(JsArray<JsContainerDescriptor> calendars)
	/*-{
		this['calendars'] = calendars;
	}-*/;

	public final native JsArrayString getFreebusy()
	/*-{
		return this['freebusy'];
	}-*/;

	public final native void setFreebusy(JsArrayString freebusy)
	/*-{
		this['freebusy'] = freebusy;
	}-*/;

	public final void setCalendars(List<ContainerDescriptor> value) {
		setCalendars(new GwtSerDerUtils.ListSerDer<>(new ContainerDescriptorGwtSerDer()).serialize(value).isArray()
				.getJavaScriptObject().<JsArray<JsContainerDescriptor>> cast());
	}

	public final void setFreebusy(List<String> value) {
		setFreebusy(new GwtSerDerUtils.ListSerDer<>(GwtSerDerUtils.STRING).serialize(value).isArray()
				.getJavaScriptObject().<JsArrayString> cast());
	}

	public final List<String> getFreebusyAsList() {
		if (getFreebusy() != null) {
			return new GwtSerDerUtils.ListSerDer<>(GwtSerDerUtils.STRING)
					.deserialize(new JSONArray(getFreebusy().cast()));
		}
		return new ArrayList<String>();
	}

}
