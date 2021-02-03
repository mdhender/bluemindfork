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

import java.util.ArrayList;
import java.util.List;

import net.fortuna.ical4j.data.DefaultPropertyFactorySupplier;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.PropertyFactory;

public class PropertyFactoryImpl extends DefaultPropertyFactorySupplier {

	@Override
	public List<PropertyFactory<? extends Property>> get() {
		List<PropertyFactory<? extends Property>> list = new ArrayList<>(super.get());
		list.add(new DtStampFactory());

		return list;
	}
}