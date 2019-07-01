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
package net.bluemind.lib.ical4j.vcard;

import java.util.List;

import net.fortuna.ical4j.vcard.Group;
import net.fortuna.ical4j.vcard.Parameter;
import net.fortuna.ical4j.vcard.PropertyFactory;
import net.fortuna.ical4j.vcard.property.Uid;

/**
 * @author mehdi
 * 
 */
public class UidFactory implements PropertyFactory<Uid> {

	public static final PropertyFactory<Uid> INSTANCE = new UidFactory();

	/**
	 * {@inheritDoc}
	 */
	public Uid createProperty(final List<Parameter> params, final String value) {
		try {
			return Uid.FACTORY.createProperty(params, value);
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public Uid createProperty(final Group group, final List<Parameter> params, final String value) {
		try {
			return Uid.FACTORY.createProperty(group, params, value);
		} catch (Exception e) {
			return null;
		}
	}

}
