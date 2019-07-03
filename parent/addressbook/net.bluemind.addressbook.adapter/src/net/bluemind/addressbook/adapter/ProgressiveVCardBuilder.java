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
package net.bluemind.addressbook.adapter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.Iterator;
import java.util.NoSuchElementException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.google.common.base.Throwables;

import net.bluemind.lib.ical4j.vcard.Builder;
import net.fortuna.ical4j.data.ParserException;
import net.fortuna.ical4j.vcard.VCard;
import net.fortuna.ical4j.vcard.VCardBuilder;

public class ProgressiveVCardBuilder implements Iterator<VCard>, AutoCloseable {

	private static final Logger LOGGER = LoggerFactory.getLogger(ProgressiveVCardBuilder.class);
	private BufferedReader reader;

	private boolean endOfFile = false;
	private StringBuilder currentElement;
	private static final String CRLF = "\r\n";

	public ProgressiveVCardBuilder(Reader reader) {
		this.reader = new BufferedReader(reader);
		currentElement = new StringBuilder();
	}

	public VCard next() {
		if (!hasNext()) {
			throw new NoSuchElementException();
		}
		try {
			return nextImpl();
		} catch (IOException | ParserException e) {
			LOGGER.warn("Cannot parse vcard stream", e);
			throw Throwables.propagate(e);
		}
	}

	private VCard nextImpl() throws IOException, ParserException {
		boolean endOfCard = false;
		String line = null;
		while (!endOfCard && ((line = reader.readLine()) != null)) {

			// Yahoo! Crap vcard workaround.
			// SOURCE:Yahoo! AddressBook (http://address.yahoo.com) =>
			// invalid
			// see http://tools.ietf.org/html/rfc2425#section-6.1
			//
			// REV;CHARSET=utf-8:53 => invalid
			// see http://tools.ietf.org/html/rfc6350#section-6.7.4
			if (line.startsWith("SOURCE") || line.startsWith("REV")) {
				continue;
			}
			String data = line.replace("\\:", ":");

			currentElement.append(data);
			currentElement.append(CRLF);

			if (data.startsWith("END:VCARD")) {
				endOfCard = true;
			}
		}
		if (Strings.isNullOrEmpty(line)) {
			endOfFile = true;
		} else {
			String lookAhead = reader.readLine();
			if (Strings.isNullOrEmpty(lookAhead)) {
				endOfFile = true;
			} else {
				currentElement.setLength(0);
				currentElement.append(lookAhead).append(CRLF);
			}
		}
		String asString = currentElement.toString();
		if (asString.trim().length() == 0) {
			return null;
		}

		VCardBuilder builder = Builder.from(new StringReader(asString));
		return builder.buildAll().get(0);
	}

	@Override
	public void close() throws Exception {
		reader.close();
	}

	@Override
	public boolean hasNext() {
		return !endOfFile;
	}

}
