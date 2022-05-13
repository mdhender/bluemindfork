/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2022
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
package net.bluemind.signature.commons.action;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import org.apache.james.mime4j.dom.Entity;
import org.apache.james.mime4j.dom.TextBody;
import org.apache.james.mime4j.message.BasicBodyFactory;
import org.apache.james.mime4j.message.BodyPart;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.Before;
import org.junit.Test;

import com.google.common.io.CharStreams;

import net.bluemind.addressbook.api.VCard;

public class AddSignatureToHtmlPartTest {

	private static final String INSIDE_PLACEHOLDER = "inside placeholder";
	private static final String BEGIN_OF_MESSAGE = "Begin of message";
	private static final String MIDDLE_OF_MESSAGE = "Middle of the message";
	private static final String END_OF_MESSAGE = "End of message";

	private Map<String, String> configuration;

	String sign = "<div>   my-html-signature  </div>";
	String expectedSign = " <div class=\"x-disclaimer0\">  " + sign + " </div>";
	AddDisclaimer addDisclaimer;

	@Before
	public void before() {
		Supplier<Optional<VCard>> vCardSupplier = () -> null;
		addDisclaimer = new AddDisclaimer(vCardSupplier);
		configuration = new HashMap<>();
		configuration.put("usePlaceholder", "true");
		configuration.put("html", sign);
	}

	@Test
	public void testNoPlaceholderReplacement() {
		String message = BEGIN_OF_MESSAGE;
		String expectedMessage = BEGIN_OF_MESSAGE + expectedSign;
		Entity e = buildMessage(message);

		addDisclaimer.addToHtmlPart(e, configuration);
		assertEquals(expectedMessage, getHtmlBodyContent(e));
	}

	@Test
	public void testLegacyPlaceholderReplacement() {
		String message = BEGIN_OF_MESSAGE + AddDisclaimer.LEGACY_PLACEHOLDER + END_OF_MESSAGE;
		String expectedMessage = BEGIN_OF_MESSAGE + expectedSign + END_OF_MESSAGE;
		Entity e = buildMessage(message);

		addDisclaimer.addToHtmlPart(e, configuration);
		assertEquals(expectedMessage, getHtmlBodyContent(e));
	}

	@Test
	public void testMultipleLegacyPlaceholderReplacement() {
		String message = BEGIN_OF_MESSAGE + AddDisclaimer.LEGACY_PLACEHOLDER + MIDDLE_OF_MESSAGE
				+ AddDisclaimer.LEGACY_PLACEHOLDER + END_OF_MESSAGE + AddDisclaimer.LEGACY_PLACEHOLDER;
		// first found placeholder is replaced and the others are deleted
		String expectedMessage = BEGIN_OF_MESSAGE + expectedSign + MIDDLE_OF_MESSAGE + END_OF_MESSAGE;
		Entity e = buildMessage(message);

		addDisclaimer.addToHtmlPart(e, configuration);
		assertEquals(expectedMessage, getHtmlBodyContent(e));
	}

	@Test
	public void testPlaceholderReplacement() {
		String message = BEGIN_OF_MESSAGE + AddDisclaimer.PLACEHOLDER_PREFIX + INSIDE_PLACEHOLDER
				+ AddDisclaimer.PLACEHOLDER_SUFFIX + END_OF_MESSAGE;
		String expectedMessage = BEGIN_OF_MESSAGE + expectedSign + END_OF_MESSAGE;
		Entity e = buildMessage(message);

		addDisclaimer.addToHtmlPart(e, configuration);
		assertEquals(expectedMessage, getHtmlBodyContent(e));
	}

	@Test
	public void testMultiplePlaceholdersReplacement() {
		String message = BEGIN_OF_MESSAGE + AddDisclaimer.PLACEHOLDER_PREFIX + INSIDE_PLACEHOLDER
				+ AddDisclaimer.PLACEHOLDER_SUFFIX + AddDisclaimer.PLACEHOLDER_PREFIX + MIDDLE_OF_MESSAGE
				+ AddDisclaimer.PLACEHOLDER_SUFFIX + END_OF_MESSAGE;
		// first found placeholder is replaced, and the others are preserved
		String expectedMessage = BEGIN_OF_MESSAGE + expectedSign + AddDisclaimer.PLACEHOLDER_PREFIX + MIDDLE_OF_MESSAGE
				+ AddDisclaimer.PLACEHOLDER_SUFFIX + END_OF_MESSAGE;
		Entity e = buildMessage(message);

		addDisclaimer.addToHtmlPart(e, configuration);
		assertEquals(expectedMessage, getHtmlBodyContent(e));
	}

	@Test
	public void testInvalidInvertedPlaceholders() {
		String message = BEGIN_OF_MESSAGE + AddDisclaimer.PLACEHOLDER_SUFFIX + INSIDE_PLACEHOLDER
				+ AddDisclaimer.PLACEHOLDER_PREFIX + END_OF_MESSAGE;
		// signature is placed at end because no valid placeholder found, placeholder
		// suffix & prefix are preserved
		String expectedMessage = BEGIN_OF_MESSAGE + AddDisclaimer.PLACEHOLDER_SUFFIX + INSIDE_PLACEHOLDER
				+ AddDisclaimer.PLACEHOLDER_PREFIX + END_OF_MESSAGE + expectedSign;
		Entity e = buildMessage(message);

		addDisclaimer.addToHtmlPart(e, configuration);
		assertEquals(expectedMessage, getHtmlBodyContent(e));
	}

	@Test
	public void testInvalidPlaceholderButLegacyReplacement() {
		String message = BEGIN_OF_MESSAGE + AddDisclaimer.PLACEHOLDER_PREFIX + INSIDE_PLACEHOLDER + MIDDLE_OF_MESSAGE
				+ AddDisclaimer.LEGACY_PLACEHOLDER + END_OF_MESSAGE;
		// signature is placed at end because no valid placeholder found, placeholder
		// suffix & prefix are preserved
		String expectedMessage = BEGIN_OF_MESSAGE + AddDisclaimer.PLACEHOLDER_PREFIX + INSIDE_PLACEHOLDER
				+ MIDDLE_OF_MESSAGE + expectedSign + END_OF_MESSAGE;
		Entity e = buildMessage(message);

		addDisclaimer.addToHtmlPart(e, configuration);
		assertEquals(expectedMessage, getHtmlBodyContent(e));
	}

	@Test
	public void testPlaceholdersTakePriorityOnLegacyOne() {
		String message = BEGIN_OF_MESSAGE + AddDisclaimer.PLACEHOLDER_PREFIX + INSIDE_PLACEHOLDER
				+ AddDisclaimer.PLACEHOLDER_SUFFIX + MIDDLE_OF_MESSAGE + AddDisclaimer.LEGACY_PLACEHOLDER
				+ END_OF_MESSAGE;
		// if valid placeholders are found, the legacy one is not used and removed
		String expectedMessage = BEGIN_OF_MESSAGE + expectedSign + MIDDLE_OF_MESSAGE + END_OF_MESSAGE;
		Entity e = buildMessage(message);

		addDisclaimer.addToHtmlPart(e, configuration);
		assertEquals(expectedMessage, getHtmlBodyContent(e));
	}

	private Entity buildMessage(String content) {
		BodyPart part = new BodyPart();
		TextBody body = new BasicBodyFactory().textBody(content.toString(), StandardCharsets.UTF_8);
		part.setBody(body, "text/html");
		return part;
	}

	private static String getHtmlBodyContent(Entity entity) {
		TextBody tb = (TextBody) entity.getBody();
		String partContent = null;
		try (InputStream in = tb.getInputStream()) {
			partContent = CharStreams.toString(new InputStreamReader(in, StandardCharsets.UTF_8));
			Document doc = Jsoup.parse(partContent);
			return doc.body().html().replace("\n", "").replace("\r", "");
		} catch (IOException e) {
			fail("fail converting body entity into a string..." + e.getMessage());
		}
		return partContent;
	}
}
