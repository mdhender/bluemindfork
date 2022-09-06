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
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.backend.mail.replica.service.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.james.mime4j.dom.Message;
import org.apache.james.mime4j.dom.Multipart;
import org.junit.Test;

import com.google.common.base.Strings;
import com.google.common.io.FileBackedOutputStream;

import io.vertx.core.buffer.Buffer;
import net.bluemind.backend.mail.api.MessageBody.Part;
import net.bluemind.backend.mail.parsing.BodyStreamProcessor;
import net.bluemind.backend.mail.parsing.BodyStreamProcessor.MessageBodyData;
import net.bluemind.core.rest.vertx.VertxStream;
import net.bluemind.mime4j.common.AddressableEntity;
import net.bluemind.mime4j.common.Mime4JHelper;

public class EmlStructureTests {

	@Test
	public void altospam() {
		compareStructure("altospam.eml");
	}

	@Test
	public void testExpandMpartAlt() throws Exception {
		compareStructure("mpart.alternative.eml");
	}

	@Test
	public void testExpandMpartWithAttach() throws Exception {
		compareStructure("mpart.alt.with.attach.eml");
	}

	@Test
	public void testMpartRelative() throws Exception {
		compareStructure("test.img.inline.from.tbird.eml");
	}

	@Test
	public void testSigned() throws Exception {
		compareStructure("mpart.signed.eml");
	}

	@Test
	public void testMime4jTreeIsCorrect() throws Exception {
		compareStructure("with_attachments.eml");
	}

	@Test
	public void testAttachmentWithAccents() throws Exception {
		compareStructure("attachment_with_accents.eml");
	}

	@Test
	public void testAttachmentWithAccentsAndSpaces() throws Exception {
		compareStructure("attachment_with_accents_and_spaces.eml");
	}

	@Test
	public void testAttachmentWithoutAccents() throws Exception {
		compareStructure("attachment_without_accents.eml");
	}

	@Test
	public void testAttachmentNullFilename() throws Exception {
		compareStructure("attachment_null_filename.eml");
	}

	@Test
	public void noBodyOneAttachment() throws Exception {
		compareStructure("bm-17397.eml");
	}

	@Test
	public void bodyIsApplicationPDF() throws Exception {
		compareStructure("bm-16865.eml");
	}

	@Test
	public void mpartAttachRFC822() throws Exception {
		compareStructure("BM-15743.eml");
	}

	@Test
	public void testNestedRFC822WithManyRecipients() throws Exception {
		compareStructure("bm-15237.eml");
	}

	@Test
	public void adaptNdrReport() throws IOException {
		compareStructure("bm_ndr.eml");
	}

	private void compareStructure(String eml) {
		Set<String> addressesFromMime4j = new LinkedHashSet<>();
		Set<String> addressesFromMailboxItem = new LinkedHashSet<>();

		try (InputStream inputStream = EmlStructureTests.class.getClassLoader().getResourceAsStream("data/" + eml);
				FileBackedOutputStream fbos = new FileBackedOutputStream(32000)) {
			inputStream.transferTo(fbos);

			MessageBodyData result = BodyStreamProcessor
					.processBody(VertxStream.stream(Buffer.buffer(fbos.asByteSource().read())))
					.get(2, TimeUnit.SECONDS);
			fillSet(addressesFromMailboxItem, result.body.structure);
			assertFalse(addressesFromMailboxItem.isEmpty());

			Message parsed = Mime4JHelper.parse(fbos.asByteSource().openBufferedStream());
			if (parsed.isMultipart()) {
				Multipart multipart = (Multipart) parsed.getBody();
				List<AddressableEntity> asParts = Mime4JHelper.expandTree(multipart.getBodyParts());
				for (AddressableEntity ae : asParts) {
					addressesFromMime4j.add(ae.getMimeAddress());
				}
			} else {
				// single body, only 1 part
				addressesFromMime4j.add("1");
			}
			assertFalse(addressesFromMime4j.isEmpty());

			assertEquals(addressesFromMailboxItem, addressesFromMime4j);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

	}

	private void fillSet(Set<String> addresses, Part part) {
		// "TEXT" addr == ROOT PART, skip it
		if (!Strings.isNullOrEmpty(part.address) && !"TEXT".equals(part.address)) {
			addresses.add(part.address);
		}
		if (part.children == null || part.children.isEmpty()) {
			return;
		}
		for (Part child : part.children) {
			fillSet(addresses, child);
		}

	}

}
