/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2020
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
package net.bluemind.lib.ical4j.model;

import java.io.IOException;
import java.net.URISyntaxException;
import java.text.ParseException;

import net.fortuna.ical4j.model.Content;
import net.fortuna.ical4j.model.ParameterList;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.PropertyFactory;
import net.fortuna.ical4j.validate.ValidationException;

@SuppressWarnings("serial")
public class UnknownProperty extends Property {
	private String value;

	public UnknownProperty(String name) {
		super(name, new Factory(name));
	}

	@Override
	public void setValue(String value) throws IOException, URISyntaxException, ParseException {
		this.value = value;
	}

	@Override
	public void validate() throws ValidationException {

	}

	@Override
	public String getValue() {
		return value;
	}

	public static class Factory extends Content.Factory implements PropertyFactory<UnknownProperty> {
		private static final long serialVersionUID = 1L;
		private final String name;

		public Factory(String name) {
			super(name);
			this.name = name;
		}

		public UnknownProperty createProperty(final ParameterList parameters, final String value)
				throws IOException, URISyntaxException, ParseException {

			return new UnknownProperty(name);
		}

		@Override
		public UnknownProperty createProperty() {
			return new UnknownProperty(name);
		}

	}

}
