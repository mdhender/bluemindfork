/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2017
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
package net.bluemind.milter.action.signature;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

import org.apache.james.mime4j.codec.QuotedPrintableInputStream;
import org.apache.james.mime4j.dom.Body;
import org.apache.james.mime4j.dom.Entity;
import org.apache.james.mime4j.dom.Multipart;
import org.apache.james.mime4j.stream.Field;
import org.junit.Test;

import com.google.common.io.ByteStreams;

import net.bluemind.addressbook.api.VCard;
import net.bluemind.addressbook.api.VCard.Communications.Email;
import net.bluemind.addressbook.api.VCard.Identification.Name;
import net.bluemind.mailflow.rbe.IClientContext;
import net.bluemind.milter.action.UpdatedMailMessage;
import net.bluemind.mime4j.common.Mime4JHelper;
import net.bluemind.signature.commons.action.DisclaimerVariables;

public class AddSignatureActionTests {

	public class MockAddSignatureAction extends AddSignatureAction {

		@Override
		protected Optional<VCard> getVCard(IClientContext context, String sender, String domain) {
			VCard card = new VCard();
			card.communications.emails = Arrays.asList(Email.create(sender, Arrays.<VCard.Parameter>asList()));
			card.identification.name = Name.create("Doe", "John", null, null, null, null);
			return Optional.of(card);
		}

	}

	@Test
	public void testAppleMailNoBody() throws Exception {
		String template = "apple_mail_no_body.eml";
		UpdatedMailMessage mm = loadTemplate(template);

		AddSignatureAction action = new MockAddSignatureAction();
		Map<String, String> configuration = new HashMap<>();
		configuration.put("plain", "plain-signature");
		configuration.put("html", "html-signature");
		boolean added = action.addDisclaimer(mm, null, configuration, "test@bm.loc", "bm.loc");
		assertFalse(added);

	}

	@Test
	public void testSingleHtmlPart() throws Exception {
		String template = "single_html_non_mp.eml";
		UpdatedMailMessage mm = loadTemplate(template);

		AddSignatureAction action = new MockAddSignatureAction();
		Map<String, String> configuration = new HashMap<>();
		configuration.put("plain", "plain-signature");
		configuration.put("html", "html-signature<img src=\"http://someimg\">");
		boolean added = action.addDisclaimer(mm, null, configuration, "test@bm.loc", "bm.loc");
		assertTrue(added);
		added = action.addDisclaimer(mm, null, configuration, "test@bm.loc", "bm.loc");
		assertTrue(added);

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		Mime4JHelper.serializeBody(mm.getMessage(), baos);

		String eml = toEml(baos);
		assertTrue(eml.startsWith("MIME-Version")); // test for broken multipart declaration
	}

	@Test
	public void testBase64EncodedBodyPart() throws Exception {
		String template = "template1.eml";
		UpdatedMailMessage mm = loadTemplate(template);

		AddSignatureAction action = new MockAddSignatureAction();
		Map<String, String> configuration = new HashMap<>();
		configuration.put("plain", "plain-signature");
		configuration.put("html", "html-signature");
		boolean added = action.addDisclaimer(mm, null, configuration, "test@bm.loc", "bm.loc");
		assertTrue(added);

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		Mime4JHelper.serializeBody(mm.getMessage(), baos);

		String eml = toEml(baos);

		assertTrue(eml.contains("Content-Type: text/html; charset=utf-8"));
		assertTrue(eml.contains("Content-Transfer-Encoding: quoted-printable"));
		assertTrue(eml.contains("Mail de synth=C3=A8se - Antispam"));
	}

	@Test
	public void testIosB64() throws Exception {
		String template = "ios_b64.eml";
		UpdatedMailMessage mm = loadTemplate(template);

		AddSignatureAction action = new MockAddSignatureAction();
		Map<String, String> configuration = new HashMap<>();
		configuration.put("plain", "plain-signature");
		configuration.put("html", "html-signature");
		boolean added = action.addDisclaimer(mm, null, configuration, "test@bm.loc", "bm.loc");
		assertTrue(added);

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		Body body = mm.getMessage().getBody();
		Mime4JHelper.serializeBody(body, baos);
		String eml = toEml(baos);

		assertTrue(eml.contains("Envoyé avec mes doigts depuis mon iPad"));
		assertTrue(eml.contains("plain-signature"));
	}

	@Test
	public void testIosUtf8() throws Exception {
		String template = "ios_utf-8.eml";
		UpdatedMailMessage mm = loadTemplate(template);

		AddSignatureAction action = new MockAddSignatureAction();
		Map<String, String> configuration = new HashMap<>();
		configuration.put("plain", "et des accents é è l'a");
		configuration.put("html", "et des accents é è l'a");
		boolean added = action.addDisclaimer(mm, null, configuration, "test@bm.loc", "bm.loc");
		assertTrue(added);

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		Body body = mm.getMessage().getBody();
		Mime4JHelper.serializeBody(body, baos);
		String eml = toEml(baos);

		System.out.println(eml);

		assertTrue(eml.contains("É 4€"));
		assertTrue(eml.contains("Envoyé avec mes doigts depuis mon iPad"));
		assertTrue(eml.contains("et des accents é è l'a"));
	}

	private String toEml(ByteArrayOutputStream baos) {
		return new String(baos.toByteArray());
	}

	@Test
	public void testIosAscii() throws Exception {
		String template = "ios_ascii.eml";
		UpdatedMailMessage mm = loadTemplate(template);

		AddSignatureAction action = new MockAddSignatureAction();
		Map<String, String> configuration = new HashMap<>();
		configuration.put("plain", "et des accents é è l'a");
		configuration.put("html", "et des accents é è l'a");
		boolean added = action.addDisclaimer(mm, null, configuration, "test@bm.loc", "bm.loc");
		assertTrue(added);

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		Body body = mm.getMessage().getBody();
		Mime4JHelper.serializeBody(body, baos);
		String eml = toEml(baos);

		assertTrue(eml.contains("et des accents é è l'a"));
	}

	@Test
	public void testIso8859_1BodyPart() throws Exception {
		String template = "template_iso.eml";
		UpdatedMailMessage mm = loadTemplate(template);

		AddSignatureAction action = new MockAddSignatureAction();
		Map<String, String> configuration = new HashMap<>();
		configuration.put("plain", "plain-signature");
		configuration.put("html", "html-signature");
		boolean added = action.addDisclaimer(mm, null, configuration, "test@bm.loc", "bm.loc");
		assertTrue(added);

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		Body body = mm.getMessage().getBody();
		Mime4JHelper.serializeBody(body, baos);
		String eml = toEml(baos);

		assertTrue(eml.contains("l=C2=92avez pas d=C3=A9j=C3=A0"));
	}

	@Test
	public void testRemovePreviousSignature() throws Exception {
		String template = "template1.eml";
		UpdatedMailMessage mm = loadTemplate(template);

		AddSignatureAction action = new MockAddSignatureAction();
		Map<String, String> configuration = new HashMap<>();
		configuration.put("removePrevious", "true");
		configuration.put("plain", "plain-signature");
		configuration.put("html", "html-signature");
		boolean added = action.addDisclaimer(mm, null, configuration, "test@bm.loc", "bm.loc");
		assertTrue(added);
		added = action.addDisclaimer(mm, null, configuration, "test@bm.loc", "bm.loc");
		assertTrue(added);

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		Body body = mm.getMessage().getBody();
		Mime4JHelper.serializeBody(body, baos);
		String eml = toEml(baos);

		assertEquals(eml.indexOf("html-signature"), eml.lastIndexOf("html-signature"));
	}

	@Test
	public void testRemoveImageFromPreviousSignature() throws Exception {
		String template = "template1.eml";

		UpdatedMailMessage mm = loadTemplate(template);
		Map<String, String> configuration = new HashMap<>();
		configuration.put("removePrevious", "true");

		configuration.put("plain", "plain-signature");
		configuration.put("html",
				"<img src=\"data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAA4AAAAOCAMAAAAolt3jAAAAA1BMVEUAAACnej3aAAAAFUlEQVR42t3BAQEAAACAkP6v9iPaAQDSAAHMIBAUAAAAAElFTkSuQmCC\" />");
		AddSignatureAction action = new MockAddSignatureAction();

		boolean added = action.addDisclaimer(mm, null, configuration, "test@bm.loc", "bm.loc");
		assertTrue(added);
		added = action.addDisclaimer(mm, null, configuration, "test@bm.loc", "bm.loc");
		assertTrue(added);

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		Body body = mm.getMessage().getBody();
		Mime4JHelper.serializeBody(body, baos);
		Pattern myCid = Pattern
				.compile("<" + DisclaimerVariables.generateDisclaimerId("test@bm.loc") + "-[0-9]+.png@bm-disclaimer>");
		boolean first = false;
		try {
			Multipart related = (Multipart) mm.getMessage().getBody();
			for (Entity part : related.getBodyParts()) {
				Field cid = part.getHeader().getField("Content-ID");
				if (cid != null && myCid.matcher(cid.getBody()).matches()) {
					assertFalse("Previous disclaimer image part should have been removed", first);
					first = true;
				}
			}
		} catch (Exception e) {
			fail(e.getMessage());
		}
		assertTrue("Did not found any disclaimer image part...", first);

	}

	@Test
	public void testSignedEmailShouldSkipProcessing() throws Exception {
		String template = "email_signed.eml";
		UpdatedMailMessage mm = loadTemplate(template);

		AddSignatureAction action = new MockAddSignatureAction();
		Map<String, String> configuration = new HashMap<>();
		configuration.put("plain", "plain-signature");
		configuration.put("html", "html-signature");
		action.execute(mm, configuration, null, null);

		// verify that X-BM-Disclaimer has not been added
		assertTrue(mm.newHeaders.isEmpty());
	}

	@Test
	public void testEncryptedEmailShouldSkipProcessing() throws Exception {
		String template = "email_encrypted.eml";
		UpdatedMailMessage mm = loadTemplate(template);

		AddSignatureAction action = new MockAddSignatureAction();
		Map<String, String> configuration = new HashMap<>();
		configuration.put("plain", "plain-signature");
		configuration.put("html", "html-signature");
		action.execute(mm, configuration, null, null);

		// verify that X-BM-Disclaimer has not been added
		assertTrue(mm.newHeaders.isEmpty());
	}

	@Test
	public void testSmimeEmailShouldSkipProcessing() throws Exception {
		String template = "smime.eml";
		UpdatedMailMessage mm = loadTemplate(template);

		AddSignatureAction action = new MockAddSignatureAction();
		Map<String, String> configuration = new HashMap<>();
		configuration.put("plain", "plain-signature");
		configuration.put("html", "html-signature");
		action.execute(mm, configuration, null, null);

		// verify that X-BM-Disclaimer has not been added
		assertTrue(mm.newHeaders.isEmpty());
	}

	private UpdatedMailMessage loadTemplate(String name) throws IOException {
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();

		InputStream is = this.getClass().getResourceAsStream("/templates/" + name);

		int nRead;
		byte[] data = new byte[1024];

		while ((nRead = is.read(data, 0, data.length)) != -1) {
			buffer.write(data, 0, nRead);
		}

		return new UpdatedMailMessage(Collections.emptyMap(),
				Mime4JHelper.parse(new ByteArrayInputStream(buffer.toByteArray())));
	}

	@Test
	public void testPlaceholder() throws Exception {
		String template = "template_placeholder.eml";
		UpdatedMailMessage mm = loadTemplate(template);

		AddSignatureAction action = new MockAddSignatureAction();
		Map<String, String> configuration = new HashMap<>();
		configuration.put("usePlaceholder", "true");
		configuration.put("plain", "plain-signature");
		configuration.put("html", "html-signature");
		boolean added = action.addDisclaimer(mm, null, configuration, "test@bm.loc", "bm.loc");
		assertTrue(added);

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		Body body = mm.getMessage().getBody();
		Mime4JHelper.serializeBody(body, baos);
		String eml = toEml(baos);
		assertTrue("Placeholder still present in eml", eml.indexOf("X-BM-SIGNATURE") < 0);
		assertTrue("Text Disclaimer is not present ", eml.indexOf("plain-signature") >= 0);
		assertTrue("HTML Disclaimer is not present ", eml.indexOf("html-signature") >= 0);

		assertTrue("Text disclaimer was placed before placeholder",
				eml.indexOf("TextBefore") < eml.indexOf("plain-signature"));
		assertTrue("Text disclaimer was placed after placeholder",
				eml.indexOf("TextAfter") > eml.indexOf("plain-signature"));
		assertTrue("Html disclaimer was placed before placeholder",
				eml.indexOf("HTMLBefore") < eml.indexOf("html-signature"));
		assertTrue("Html disclaimer was placed after placeholder",
				eml.indexOf("HTMLAfter") > eml.indexOf("html-signature"));

	}

	@Test
	public void testNoPlaceholder() throws Exception {
		String template = "template_no_placeholder.eml";
		UpdatedMailMessage mm = loadTemplate(template);

		AddSignatureAction action = new MockAddSignatureAction();
		Map<String, String> configuration = new HashMap<>();
		configuration.put("usePlaceholder", "true");
		configuration.put("plain", "plain-signature");
		configuration.put("html", "html-signature");
		boolean added = action.addDisclaimer(mm, null, configuration, "test@bm.loc", "bm.loc");
		assertTrue(added);

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		Body body = mm.getMessage().getBody();
		Mime4JHelper.serializeBody(body, baos);
		String eml = toEml(baos);
		assertTrue("Text Disclaimer is not present ", eml.indexOf("plain-signature") >= 0);
		assertTrue("HTML Disclaimer is not present ", eml.indexOf("html-signature") >= 0);

		assertTrue("Text disclaimer was not placed at the end of mail",
				eml.indexOf("TextAfter") < eml.indexOf("plain-signature"));
		assertTrue("HTML disclaimer was not placed at the end of mail",
				eml.indexOf("HTMLAfter") < eml.indexOf("html-signature"));

	}

	@Test
	public void testMultiplePlaceholders() throws Exception {
		String template = "template_placeholders.eml";
		UpdatedMailMessage mm = loadTemplate(template);

		AddSignatureAction action = new MockAddSignatureAction();
		Map<String, String> configuration = new HashMap<>();
		configuration.put("usePlaceholder", "true");
		configuration.put("plain", "plain-signature");
		configuration.put("html", "html-signature");
		boolean added = action.addDisclaimer(mm, null, configuration, "test@bm.loc", "bm.loc");
		assertTrue(added);

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		Body body = mm.getMessage().getBody();
		Mime4JHelper.serializeBody(body, baos);
		String eml = toEml(baos);
		QuotedPrintableInputStream qip = new QuotedPrintableInputStream(new ByteArrayInputStream(eml.getBytes()));
		String emlDec = new String(ByteStreams.toByteArray(qip));
		eml = emlDec;

		assertTrue("Placeholder still present in eml", eml.indexOf("X-BM-SIGNATURE") < 0);
		assertTrue("Text Disclaimer is not present ", eml.indexOf("plain-signature") >= 0);
		assertTrue("HTML Disclaimer is not present ", eml.indexOf("html-signature") >= 0);

		assertTrue("Text disclaimer was not placed at the end of mail",
				eml.indexOf("TextAfter") < eml.indexOf("plain-signature"));
		assertTrue("HTML disclaimer was not placed at the end of mail",
				eml.indexOf("HTMLAfter") < eml.indexOf("html-signature"));

	}

}
