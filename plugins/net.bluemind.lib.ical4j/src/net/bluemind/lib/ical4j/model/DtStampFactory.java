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
package net.bluemind.lib.ical4j.model;

import java.io.IOException;
import java.net.URISyntaxException;
import java.text.ParseException;

import net.bluemind.lib.ical4j.model.property.DtStamp;
import net.fortuna.ical4j.model.ParameterList;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.PropertyFactory;

public class DtStampFactory implements PropertyFactory {

	private static final long serialVersionUID = 6843338171533599746L;

	@Override
	public Property createProperty(String name) {
		return new DtStamp();
	}

	@Override
	public Property createProperty(String name, ParameterList parameters, String value)
			throws IOException, URISyntaxException, ParseException {
		return new DtStamp(parameters, value);
	}

}
