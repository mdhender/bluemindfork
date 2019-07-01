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

import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.List;

import org.apache.commons.codec.DecoderException;

import net.fortuna.ical4j.vcard.Group;
import net.fortuna.ical4j.vcard.Parameter;
import net.fortuna.ical4j.vcard.PropertyFactory;
import net.fortuna.ical4j.vcard.property.Url;

public class UrlFactory implements PropertyFactory<Url> {
	public static final PropertyFactory<Url> INSTANCE = new UrlFactory();

	@Override
	public Url createProperty(List<Parameter> params, String value)
			throws URISyntaxException, ParseException, DecoderException {
		try {
			return Url.FACTORY.createProperty(params, value);
		} catch (Exception e) {
			return null;
		}
	}

	@Override
	public Url createProperty(Group group, List<Parameter> params, String value)
			throws URISyntaxException, ParseException, DecoderException {
		try {
			return Url.FACTORY.createProperty(group, params, value);
		} catch (Exception e) {
			return null;
		}
	}

}
