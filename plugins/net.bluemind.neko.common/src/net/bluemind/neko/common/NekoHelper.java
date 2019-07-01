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
package net.bluemind.neko.common;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.xerces.xni.Augmentations;
import org.apache.xerces.xni.XMLString;
import org.apache.xerces.xni.XNIException;
import org.apache.xerces.xni.parser.XMLDocumentFilter;
import org.apache.xerces.xni.parser.XMLInputSource;
import org.apache.xerces.xni.parser.XMLParserConfiguration;
import org.cyberneko.html.HTMLConfiguration;
import org.cyberneko.html.filters.ElementRemover;
import org.cyberneko.html.filters.Writer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.ByteStreams;

import net.bluemind.common.io.FileBackedOutputStream;
import net.bluemind.neko.common.impl.FBOSInput;

public class NekoHelper {

	private static final Logger logger = LoggerFactory.getLogger(NekoHelper.class);

	/**
	 * Pattern matching white characters sequence. Since '\s' does not include
	 * non breaking spaces, we add them with p{Z}.
	 * 
	 * @see https://www.regular-expressions.info/unicode.html
	 */
	final static Pattern WHITE_SPACES_PATTERN = Pattern.compile("[\\s\\p{Z}]+");

	public static String identityTransform(String html) throws IOException {
		ByteArrayInputStream in = new ByteArrayInputStream(html.getBytes());
		InputStream transformed = identityTransform(in, "UTF-8");
		byte[] strData = ByteStreams.toByteArray(transformed);
		transformed.close();
		return new String(strData);
	}

	public static InputStream rawText(InputStream html, String encoding) throws IOException {
		FileBackedOutputStream bout = new FileBackedOutputStream(32768, "nekohelper");
		XMLDocumentFilter writer = new RawTextWriter(bout);
		XMLDocumentFilter[] filters = { writer };

		XMLParserConfiguration parser = new HTMLConfiguration();
		parser.setProperty("http://cyberneko.org/html/properties/default-encoding", encoding);
		parser.setProperty("http://cyberneko.org/html/properties/filters", filters);
		parser.setProperty("http://cyberneko.org/html/properties/names/elems", "lower");
		parser.setFeature("http://cyberneko.org/html/features/augmentations", true);

		parser.setFeature("http://apache.org/xml/features/scanner/notify-builtin-refs", true);
		parser.setFeature("http://cyberneko.org/html/features/scanner/ignore-specified-charset", true);

		XMLInputSource in = new XMLInputSource(null, null, null, html, null);
		parser.parse(in);
		return FBOSInput.from(bout);
	}

	public static final String rawText(String html) {
		String retVal = "[html message]\n";

		String ret = html;
		ret = ret.replace("<br/>", "\n");
		ret = ret.replace("<BR/>", "\n");
		ret = ret.replace("<BR>", "\n");
		ret = ret.replace("<br>", "\n");

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

		XMLInputSource xis = new XMLInputSource(null, null, null, new StringReader(ret), null);

		try {
			xpc.parse(xis);
			retVal = sw.toString();
			retVal = StringEscapeUtils.unescapeHtml(retVal);
			retVal = retVal.replace("&apos;", "'");
		} catch (Exception e) {
			logger.error(e.getMessage() + ". HTML was:\n" + html);
		}

		return retVal;
	}

	/**
	 * Call {@link #rawText(String)}, then flatten (one line) and compact (trim
	 * and shorten white-spaces).
	 */
	public static String flatCompactRawText(final String html) {
		final String trimmedRawText = rawText(html).trim();
		return trimWhiteSpace(trimmedRawText);
	}

	public static String trimWhiteSpace(final String txt) {
		final Matcher matcher = WHITE_SPACES_PATTERN.matcher(txt);
		return matcher.replaceAll(" ");
	}

	public static InputStream identityTransform(InputStream html, String encoding) throws IOException {
		FileBackedOutputStream bout = new FileBackedOutputStream(32768, "nekohelper-identity");
		XMLDocumentFilter writer = new ExpandEntitiesWriter(bout);
		XMLDocumentFilter[] filters = { writer };

		XMLParserConfiguration parser = new HTMLConfiguration();
		parser.setProperty("http://cyberneko.org/html/properties/default-encoding", encoding);
		parser.setProperty("http://cyberneko.org/html/properties/filters", filters);
		parser.setProperty("http://cyberneko.org/html/properties/names/elems", "lower");
		parser.setFeature("http://cyberneko.org/html/features/augmentations", true);

		parser.setFeature("http://apache.org/xml/features/scanner/notify-builtin-refs", true);
		parser.setFeature("http://cyberneko.org/html/features/scanner/ignore-specified-charset", true);

		XMLInputSource in = new XMLInputSource(null, null, null, html, null);
		parser.parse(in);
		return FBOSInput.from(bout);
	}
}
