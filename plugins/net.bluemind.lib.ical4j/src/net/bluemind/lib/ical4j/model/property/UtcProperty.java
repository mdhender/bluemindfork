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
package net.bluemind.lib.ical4j.model.property;

import net.fortuna.ical4j.model.DateTime;
import net.fortuna.ical4j.model.ParameterList;
import net.fortuna.ical4j.model.PropertyFactory;
import net.fortuna.ical4j.model.TimeZone;
import net.fortuna.ical4j.model.property.DateProperty;
import net.fortuna.ical4j.validate.ValidationException;

public class UtcProperty extends DateProperty {

	private static final long serialVersionUID = -313366313496943917L;

	/**
	 * @param name       a property name
	 * @param parameters list of parameters
	 */
	public UtcProperty(final String name, final ParameterList parameters, PropertyFactory factory) {
		super(name, parameters, factory);
		setDate(new DateTime(true));
	}

	/**
	 * @param name a property name
	 */
	public UtcProperty(final String name, PropertyFactory factory) {
		super(name, factory);
		setDate(new DateTime(true));
	}

	/**
	 * @return Returns the date-time.
	 */
	public final DateTime getDateTime() {
		return (DateTime) getDate();
	}

	/**
	 * @param dateTime The dateTime to set.
	 */
	public void setDateTime(final DateTime dateTime) {
		// time must be in UTC..
		if (dateTime != null) {
			final DateTime utcDateTime = new DateTime(dateTime);
			utcDateTime.setUtc(true);
			setDate(utcDateTime);
		} else {
			setDate(dateTime);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void setTimeZone(TimeZone timezone) {
		// throw new UnsupportedOperationException(
		// "Cannot set timezone for UTC properties");
	}

	/**
	 * {@inheritDoc}
	 */
	public void validate() {
		super.validate();

		if (getDate() != null && !(getDate() instanceof DateTime)) {
			throw new ValidationException("Property must have a DATE-TIME value");
		}

		final DateTime dateTime = (DateTime) getDate();

		if (dateTime != null && !dateTime.isUtc()) {
			throw new ValidationException(getName() + ": DATE-TIME value must be specified in UTC time");
		}
	}
}
