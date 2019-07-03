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
package net.fortuna.ical4j.vcard.property;

import static net.fortuna.ical4j.util.Strings.unescape;

import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fortuna.ical4j.model.Date;
import net.fortuna.ical4j.model.DateTime;
import net.fortuna.ical4j.vcard.Group;
import net.fortuna.ical4j.vcard.Parameter;
import net.fortuna.ical4j.vcard.Property;
import net.fortuna.ical4j.vcard.PropertyFactory;

// SOURCE BDay
public class BMBDay {

	private static final Logger logger = LoggerFactory.getLogger(BMBDay.class);

	public static final PropertyFactory<Property> FACTORY = new Factory();

	private static class Factory implements PropertyFactory<Property> {

		/**
		 * {@inheritDoc}
		 */
		public BDay createProperty(final List<Parameter> params, String value) throws ParseException {

			if (value != null) {
				value = value.replace("-", "");
			}
			try {
				new Date(value);
			} catch (ParseException e) {
				try {
					new DateTime(value);
				} catch (ParseException e2) {
					try {
						new Date(value, "yyyy'-'MM'-'dd");
					} catch (ParseException e3) {

						try {
							new DateTime(value, "yyyy'-'MM'-'dd'T'HH':'mm':'ss'Z'", true);
						} catch (ParseException e4) {
							// logger warn
							logger.warn("unparseable date {}", value);
							value = "1900-01-01";
						}
					}
				}
			}

			return new BDay(params, unescape(value));
		}

		/**
		 * {@inheritDoc}
		 */
		public BDay createProperty(final Group group, final List<Parameter> params, final String value)
				throws URISyntaxException, ParseException {
			// TODO Auto-generated method stub
			return null;
		}
	}
}