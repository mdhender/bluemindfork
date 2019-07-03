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
package net.bluemind.lib.neko.tests;

import java.io.InputStream;
import java.io.StringWriter;

import org.apache.xerces.xni.Augmentations;
import org.apache.xerces.xni.XMLString;
import org.apache.xerces.xni.XNIException;
import org.apache.xerces.xni.parser.XMLDocumentFilter;
import org.apache.xerces.xni.parser.XMLInputSource;
import org.apache.xerces.xni.parser.XMLParserConfiguration;
import org.cyberneko.html.HTMLConfiguration;
import org.cyberneko.html.filters.ElementRemover;
import org.cyberneko.html.filters.Writer;

import junit.framework.TestCase;

public class NekoTests extends TestCase {

	public void testInfiniteRecur() {
		parse(open("recur.html"));
	}

	public void testValid() {
		parse(open("valid.html"));
	}

	private void parse(InputStream in) {
		ElementRemover er = new ElementRemover() {

			@Override
			public void comment(XMLString text, Augmentations augs) throws XNIException {
				// strip out comments, outlook loves comments in its html
			}

		};
		er.removeElement("script");
		er.removeElement("style");
		StringWriter sw = new StringWriter();

		XMLDocumentFilter[] filters = { er, new Writer(sw, "UTF-8") };

		XMLParserConfiguration xpc = new HTMLConfiguration();
		xpc.setProperty("http://cyberneko.org/html/properties/filters", filters);
		xpc.setProperty("http://cyberneko.org/html/properties/names/elems", "lower");

		XMLInputSource xis = new XMLInputSource(null, null, null, in, null);

		try {
			xpc.parse(xis);
			String ret = sw.toString();
			assertNotNull(ret);
			System.out.println(ret);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	private InputStream open(String file) {
		return NekoTests.class.getClassLoader().getResourceAsStream("data/" + file);
	}

}
