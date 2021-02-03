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
package net.bluemind.lib.ical4j.vcard.property;

import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.List;

import net.fortuna.ical4j.model.Date;
import net.fortuna.ical4j.model.DateTime;
import net.fortuna.ical4j.model.Escapable;
import net.fortuna.ical4j.util.Strings;
import net.fortuna.ical4j.vcard.Group;
import net.fortuna.ical4j.vcard.Parameter;
import net.fortuna.ical4j.vcard.Property;
import net.fortuna.ical4j.vcard.PropertyFactory;

public class Anniversary extends Property implements Escapable {

	public static final PropertyFactory<Property> FACTORY = new Factory();

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Date value;

	public Anniversary(Date value) {
		super("ANNIVERSARY");
		this.value = value;
	}

	/**
	 * Factory constructor.
	 * 
	 * @param params property parameters
	 * @param value  string representation of a property value
	 */
	public Anniversary(List<Parameter> params, String value) throws ParseException {
		super("ANNIVERSARY", params);

		// try default patterns first, then fall back on vCard-specific patterns
		try {
			this.value = new Date(value);
		} catch (ParseException e) {
			try {
				this.value = new DateTime(value);
			} catch (ParseException e2) {
				try {
					this.value = new Date(value, "yyyy'-'MM'-'dd");
				} catch (ParseException e3) {
					this.value = new DateTime(value, "yyyy'-'MM'-'dd'T'HH':'mm':'ss'Z'", true);
				}
			}
		}
	}

	@Override
	public String getValue() {
		return Strings.valueOf(value);
	}

	@Override
	public void validate() {
	}

	private static class Factory implements PropertyFactory<Property> {

		/**
		 * {@inheritDoc}
		 */
		public Anniversary createProperty(final List<Parameter> params, final String value) throws ParseException {
			return new Anniversary(params, value);
		}

		/**
		 * {@inheritDoc}
		 */
		public Manager createProperty(final Group group, final List<Parameter> params, final String value)
				throws URISyntaxException, ParseException {
			return null;
		}

		@Override
		public boolean supports(String id) {
			return id.equals("ANNIVERSARY");
		}
	}
}