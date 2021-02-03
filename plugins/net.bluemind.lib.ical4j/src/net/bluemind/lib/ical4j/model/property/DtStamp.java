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

import java.text.ParseException;

import net.bluemind.lib.ical4j.model.DtStampFactory;
import net.fortuna.ical4j.model.DateTime;
import net.fortuna.ical4j.model.ParameterList;

public class DtStamp extends UtcProperty {

	private static final long serialVersionUID = 8650440368171447234L;

	/**
	 * Default constructor. Initialises the dateTime value to the time of
	 * instantiation.
	 */
	public DtStamp() {
		super(DTSTAMP, new DtStampFactory());
	}

	/**
	 * @param aValue a string representation of a DTSTAMP value
	 * @throws ParseException if the specified value is not a valid representation
	 */
	public DtStamp(final String aValue) throws ParseException {
		this(new ParameterList(), aValue);
	}

	/**
	 * @param aList  a list of parameters for this component
	 * @param aValue a value string for this component
	 * @throws ParseException where the specified value string is not a valid
	 *                        date-time/date representation
	 */
	public DtStamp(final ParameterList aList, final String aValue) throws ParseException {
		super(DTSTAMP, aList, new DtStampFactory());
		setValue(aValue);
	}

	/**
	 * @param aDate a date representing a date-time
	 */
	public DtStamp(final DateTime aDate) {
		super(DTSTAMP, new DtStampFactory());
		// time must be in UTC..
		aDate.setUtc(true);
		setDate(aDate);
	}

	/**
	 * @param aList a list of parameters for this component
	 * @param aDate a date representing a date-time
	 */
	public DtStamp(final ParameterList aList, final DateTime aDate) {
		super(DTSTAMP, aList, new DtStampFactory());
		// time must be in UTC..
		aDate.setUtc(true);
		setDate(aDate);
	}
}
