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
package net.bluemind.resource.api.type;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import net.bluemind.core.api.BMApi;

@BMApi(version = "3")
public class ResourceTypeDescriptor {

	/** Name. */
	public String label;

	/**
	 * User defined extra properties for this resource type. Example: may define a
	 * 'Quality definition' property for a 'Video projector' type.
	 */
	public List<Property> properties = Collections.emptyList();

	/**
	 * Templates keyed by language tags.<br>
	 * A template uses {@link #properties} names as variables.<br>
	 * <br>
	 * Example: <i>This template uses the property WhiteBoard having the value
	 * ${WhiteBoard} and the property Seats having the value ${Seats}.</i>
	 * 
	 * @see Locale#toLanguageTag()
	 */
	public Map<String, String> templates = new HashMap<>();

	@BMApi(version = "3")
	public static class Property {
		/**
		 * Property unique id
		 */
		public String id;

		/**
		 * Property name
		 */
		public String label;

		/**
		 * {@link Type}
		 */
		public Type type;

		@BMApi(version = "3")
		public static enum Type {
			Number, String, Boolean
		}

		public static Property create(String id, Type type, String label) {
			Property p = new Property();
			p.id = id;
			p.type = type;
			p.label = label;
			return p;
		}
	}

	public Property property(String id) {
		Property ret = null;
		for (Property p : properties) {
			if (p.id.equals(id)) {
				ret = p;
				break;
			}
		}
		return ret;
	}

	public static ResourceTypeDescriptor create(String label, Property... props) {
		ResourceTypeDescriptor ret = new ResourceTypeDescriptor();
		ret.label = label;
		ret.properties = Arrays.asList(props);
		return ret;
	}
}
