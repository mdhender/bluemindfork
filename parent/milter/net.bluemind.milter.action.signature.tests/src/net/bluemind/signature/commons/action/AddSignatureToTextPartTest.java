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
import org.junit.Before;
import org.junit.Test;

import com.google.common.io.CharStreams;

import net.bluemind.addressbook.api.VCard;

public class AddSignatureToTextPartTest {

	private static final String INSIDE_PLACEHOLDER = "inside placeholder";
	private static final String BEGIN_OF_MESSAGE = "Begin of message";
	private static final String MIDDLE_OF_MESSAGE = "Middle of the message";
	private static final String END_OF_MESSAGE = "End of message";

	private Map<String, String> configuration;

	String expectedSign = "\r\n\r\nmy-text-plain-corporate-signature";
	AddDisclaimer addDisclaimer;

	@Before
	public void before() {
		Supplier<Optional<VCard>> vCardSupplier = () -> null;
		addDisclaimer = new AddDisclaimer(vCardSupplier);
		configuration = new HashMap<>();
		configuration.put("usePlaceholder", "true");
		configuration.put("plain", "my-text-plain-corporate-signature");
	}

	@Test
	public void testNoPlaceholderReplacement() {
		String message = BEGIN_OF_MESSAGE;
		String expectedMessage = BEGIN_OF_MESSAGE + expectedSign;
		Entity e = buildMessage(message);

		addDisclaimer.addToTextPart(e, configuration);
		assertEquals(expectedMessage, getBodyContent(e));
	}

	@Test
	public void testLegacyPlaceholderReplacement() {
		String message = BEGIN_OF_MESSAGE + AddDisclaimer.LEGACY_PLACEHOLDER + END_OF_MESSAGE;
		String expectedMessage = BEGIN_OF_MESSAGE + expectedSign + END_OF_MESSAGE;
		Entity e = buildMessage(message);

		addDisclaimer.addToTextPart(e, configuration);
		assertEquals(expectedMessage, getBodyContent(e));
	}

	@Test
	public void testMultipleLegacyPlaceholderReplacement() {
		String message = BEGIN_OF_MESSAGE + AddDisclaimer.LEGACY_PLACEHOLDER + MIDDLE_OF_MESSAGE
				+ AddDisclaimer.LEGACY_PLACEHOLDER + END_OF_MESSAGE + AddDisclaimer.LEGACY_PLACEHOLDER;
		// first found placeholder is replaced and the others are deleted
		String expectedMessage = BEGIN_OF_MESSAGE + expectedSign + MIDDLE_OF_MESSAGE + END_OF_MESSAGE;
		Entity e = buildMessage(message);

		addDisclaimer.addToTextPart(e, configuration);
		assertEquals(expectedMessage, getBodyContent(e));
	}

	@Test
	public void testPlaceholderReplacement() {
		String message = BEGIN_OF_MESSAGE + AddDisclaimer.PLACEHOLDER_PREFIX + INSIDE_PLACEHOLDER
				+ AddDisclaimer.PLACEHOLDER_SUFFIX + END_OF_MESSAGE;
		String expectedMessage = BEGIN_OF_MESSAGE + expectedSign + END_OF_MESSAGE;
		Entity e = buildMessage(message);

		addDisclaimer.addToTextPart(e, configuration);
		assertEquals(expectedMessage, getBodyContent(e));
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

		addDisclaimer.addToTextPart(e, configuration);
		assertEquals(expectedMessage, getBodyContent(e));
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

		addDisclaimer.addToTextPart(e, configuration);
		assertEquals(expectedMessage, getBodyContent(e));
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

		addDisclaimer.addToTextPart(e, configuration);
		assertEquals(expectedMessage, getBodyContent(e));
	}

	@Test
	public void testPlaceholdersTakePriorityOnLegacyOne() {
		String message = BEGIN_OF_MESSAGE + AddDisclaimer.PLACEHOLDER_PREFIX + INSIDE_PLACEHOLDER
				+ AddDisclaimer.PLACEHOLDER_SUFFIX + MIDDLE_OF_MESSAGE + AddDisclaimer.LEGACY_PLACEHOLDER
				+ END_OF_MESSAGE;
		// if valid placeholders are found, the legacy one is not used and removed
		String expectedMessage = BEGIN_OF_MESSAGE + expectedSign + MIDDLE_OF_MESSAGE + END_OF_MESSAGE;
		Entity e = buildMessage(message);

		addDisclaimer.addToTextPart(e, configuration);
		assertEquals(expectedMessage, getBodyContent(e));
	}

	private Entity buildMessage(String content) {
		BodyPart part = new BodyPart();
		TextBody text;
		text = new BasicBodyFactory().textBody(content, StandardCharsets.UTF_8);
		part.setText(text);
		return part;
	}

	private static String getBodyContent(Entity entity) {
		TextBody tb = (TextBody) entity.getBody();
		String partContent = null;
		try (InputStream in = tb.getInputStream()) {
			partContent = CharStreams.toString(new InputStreamReader(in, StandardCharsets.UTF_8));
		} catch (IOException e) {
			fail("fail converting body entity into a string..." + e.getMessage());
		}
		return partContent;
	}
}
