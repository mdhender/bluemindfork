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

import net.fortuna.ical4j.vcard.GroupRegistry;
import net.fortuna.ical4j.vcard.ParameterFactoryRegistry;
import net.fortuna.ical4j.vcard.VCardBuilder;

public class Builder {

	public static VCardBuilder from(Reader reader) {
		BmPropertyFactoryRegistry pfr = new BmPropertyFactoryRegistry();

		VCardBuilder builder = new VCardBuilder(reader, new GroupRegistry(), pfr, new ParameterFactoryRegistry());
		return builder;
	}

}