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

import net.fortuna.ical4j.model.PropertyFactory;

public class PropertyFactoryRegistry extends PropertyFactoryImpl {

	private static final long serialVersionUID = 6933696749438420388L;

	/**
	 * @param name
	 *            a non-standard property name
	 * @param factory
	 *            a factory for the non-standard property
	 */
	public void register(String name, PropertyFactory factory) {
		registerExtendedFactory(name, factory);
	}
}
