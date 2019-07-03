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

import java.io.Reader;

import net.bluemind.lib.ical4j.vcard.property.AddressbookServerKind;
import net.bluemind.lib.ical4j.vcard.property.AddressbookServerMember;
import net.fortuna.ical4j.vcard.GroupRegistry;
import net.fortuna.ical4j.vcard.ParameterFactory;
import net.fortuna.ical4j.vcard.ParameterFactoryRegistry;
import net.fortuna.ical4j.vcard.Property.Id;
import net.fortuna.ical4j.vcard.VCardBuilder;
import net.fortuna.ical4j.vcard.property.BMBDay;

public class Builder {

	public static VCardBuilder from(Reader reader) {
		PropertyFactoryRegistry pfr = new PropertyFactoryRegistry();

		pfr.register(Id.BDAY.toString(), BMBDay.FACTORY);
		pfr.register("X-ADDRESSBOOKSERVER-KIND", AddressbookServerKind.FACTORY);
		pfr.register("X-ADDRESSBOOKSERVER-MEMBER", AddressbookServerMember.FACTORY);

		VCardBuilder builder = new VCardBuilder(reader, new GroupRegistry(), pfr, new ParameterFactoryRegistry() {

			public ParameterFactory<? extends net.fortuna.ical4j.vcard.Parameter> getFactory(final String value) {
				ParameterFactory<? extends net.fortuna.ical4j.vcard.Parameter> factory = super.getFactory(value);
				if (factory == null) {
					factory = UnknownParameter.createFactory(value);
				}
				return factory;
			}

		});
		return builder;
	}

}
