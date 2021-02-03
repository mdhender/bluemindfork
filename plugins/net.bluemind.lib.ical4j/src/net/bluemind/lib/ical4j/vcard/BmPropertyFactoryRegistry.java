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
package net.bluemind.lib.ical4j.vcard;

import net.bluemind.lib.ical4j.vcard.property.AddressbookServerKind;
import net.bluemind.lib.ical4j.vcard.property.AddressbookServerMember;
import net.bluemind.lib.ical4j.vcard.property.NoteAsHtml;
import net.bluemind.lib.ical4j.vcard.property.Url;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.vcard.Property.Id;
import net.fortuna.ical4j.vcard.PropertyFactory;
import net.fortuna.ical4j.vcard.PropertyFactoryRegistry;
import net.fortuna.ical4j.vcard.property.BMBDay;

public class BmPropertyFactoryRegistry extends PropertyFactoryRegistry {

	public BmPropertyFactoryRegistry() {
		super();

		super.register(Id.BDAY.toString(), BMBDay.FACTORY);
		super.register("X-ADDRESSBOOKSERVER-KIND", AddressbookServerKind.FACTORY);
		super.register("X-ADDRESSBOOKSERVER-MEMBER", AddressbookServerMember.FACTORY);
		super.register("X-NOTE-HTML", NoteAsHtml.FACTORY);
		super.register(Property.URL, Url.FACTORY);
	}

	@Override
	protected boolean factorySupports(PropertyFactory factory, String name) {
		switch (name) {
		case Property.URL:
		case "BDAY":
			return false;
		default:
			return factory.supports(name.toUpperCase());
		}
	}
}
