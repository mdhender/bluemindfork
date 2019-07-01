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
package net.bluemind.resource.api;

import java.util.Collections;
import java.util.List;

import net.bluemind.core.api.BMApi;
import net.bluemind.directory.api.DirBaseValue;

/**
 * Resources are used, for example, to create an entity like a vehicle, a
 * meeting room, a video-projector, etc. They can be categorized by type.
 * 
 * Once created, you can invite a resource to a calendar event. It simply means
 * the resource is booked during the time of the event.
 */
@BMApi(version = "3")
public class ResourceDescriptor extends DirBaseValue {

	/**
	 * Name your resource
	 */
	public String label;

	/**
	 * { @link net.bluemind.resource.api.type.ResourceType } uid or "default" if
	 * resource has no type
	 */
	public String typeIdentifier;

	/**
	 * Description
	 */
	public String description;

	/**
	 * { @link ResourceReservationMode } default reservation mode is
	 * {@link ResourceReservationMode#AUTO_ACCEPT_REFUSE}
	 */
	public ResourceReservationMode reservationMode = ResourceReservationMode.AUTO_ACCEPT_REFUSE;

	/**
	 * Custom properties
	 */
	public List<PropertyValue> properties = Collections.emptyList();

	@BMApi(version = "3")
	public static class PropertyValue {
		public String propertyId;
		public String value;

		public static PropertyValue create(String propertyId, String value) {
			PropertyValue v = new PropertyValue();
			v.propertyId = propertyId;
			v.value = value;
			return v;
		}
	}

}
