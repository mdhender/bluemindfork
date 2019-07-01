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

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fortuna.ical4j.vcard.Property;
import net.fortuna.ical4j.vcard.PropertyFactory;

/**
 * @author mehdi
 * 
 */
public class PropertyFactoryRegistry extends net.fortuna.ical4j.vcard.PropertyFactoryRegistry {

	private static final Logger logger = LoggerFactory.getLogger(PropertyFactoryRegistry.class);
	private final Map<String, PropertyFactory<? extends Property>> factories;

	/**
	 * Support overwriting default factories
	 */
	public PropertyFactoryRegistry() {
		factories = new HashMap<String, PropertyFactory<? extends Property>>();
		factories.put(Property.Id.UID.name(), UidFactory.INSTANCE);
		factories.put(Property.Id.URL.name(), UrlFactory.INSTANCE);
	}

	/**
	 * @param value
	 *            a string representation of a property identifier
	 * @return a property factory for creating a property of the resolved type
	 */
	public PropertyFactory<? extends Property> getFactory(final String value) {
		try {
			PropertyFactory<? extends Property> pf = factories.get(value);
			if (pf != null) {
				return pf;
			}
		} catch (Exception e) {
		}
		PropertyFactory<? extends Property> ret = super.getFactory(value);
		logger.debug("returned: {}", ret);
		return ret;
	}

	/**
	 * @param name
	 *            a non-standard property name to register
	 * @param factory
	 *            a property factory for creating instances of the non-standard
	 *            property type
	 */
	public void register(String value, PropertyFactory<Property> factory) {
		try {
			factories.put(value, factory);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			super.register(value, factory);
		}
	}
}
