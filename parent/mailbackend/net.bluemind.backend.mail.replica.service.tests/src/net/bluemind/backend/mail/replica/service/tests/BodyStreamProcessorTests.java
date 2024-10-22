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
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.backend.mail.replica.service.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import org.apache.james.mime4j.dom.Message;
import org.junit.jupiter.api.Test;

import com.google.common.io.ByteStreams;

import io.netty.buffer.ByteBufUtil;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import net.bluemind.backend.mail.api.DispositionType;
import net.bluemind.backend.mail.api.MessageBody;
import net.bluemind.backend.mail.api.MessageBody.Part;
import net.bluemind.backend.mail.api.MessageBody.Recipient;
import net.bluemind.backend.mail.api.MessageBody.RecipientKind;
import net.bluemind.backend.mail.parsing.Bodies;
import net.bluemind.backend.mail.parsing.BodyStreamProcessor;
import net.bluemind.backend.mail.parsing.BodyStreamProcessor.MessageBodyData;
import net.bluemind.backend.mail.parsing.EmlBuilder;
import net.bluemind.core.api.Stream;
import net.bluemind.core.rest.vertx.VertxStream;
import net.bluemind.core.utils.JsonUtils;
import net.bluemind.mime4j.common.Mime4JHelper;

public class BodyStreamProcessorTests {

	private String sid = "sid";

	protected Stream openResource(String path) throws IOException {
		InputStream inputStream = AbstractReplicatedMailboxesServiceTests.class.getClassLoader()
				.getResourceAsStream(path);
		Objects.requireNonNull(inputStream, "Failed to open resource @ " + path);
		Buffer buf = Buffer.buffer(ByteStreams.toByteArray(inputStream));
		inputStream.close();
		return VertxStream.stream(buf);
	}

	@Test
	public void testProcess() throws IOException, InterruptedException, ExecutionException, TimeoutException {
		Stream stream = openResource("data/with_inlines.eml");
		MessageBodyData result = BodyStreamProcessor.processBody(stream).get(2, TimeUnit.SECONDS);
		assertNotNull(result);
		assertEquals("<EE964554-7ACB-46DF-AE48-0F99E92651FB@bluemind.net>", result.body.messageId);
		JsonObject asJs = new JsonObject(JsonUtils.asString(result.body.structure));
		System.out.println("JS: " + asJs.encodePrettily());
	}

	@Test
	public void testUlrStackOverflow() throws IOException, InterruptedException, ExecutionException, TimeoutException {
		Stream stream = openResource("data/ulr-stack-overflow.eml");
		MessageBodyData result = BodyStreamProcessor.processBody(stream).get(2, TimeUnit.SECONDS);
		assertNotNull(result);
		JsonObject asJs = new JsonObject(JsonUtils.asString(result.body.structure));
		System.out.println("JS: " + asJs.encodePrettily());
	}

	@Test
	public void testTwoOriginatorsFooding()
			throws IOException, InterruptedException, ExecutionException, TimeoutException {
		Stream stream = openResource("data/double-originator-fooding.eml");
		MessageBodyData result = BodyStreamProcessor.processBody(stream).get(2, TimeUnit.SECONDS);
		assertNotNull(result);
		JsonArray asJs = new JsonArray(JsonUtils.asString(result.body.recipients));
		System.err.println("JS: " + asJs.encodePrettily());
		long from = result.body.recipients.stream().filter(r -> r.kind == RecipientKind.Originator).count();
		assertEquals(1, from);
	}

	@Test
	public void testTwoOriginatorsConfluence()
			throws IOException, InterruptedException, ExecutionException, TimeoutException {
		Stream stream = openResource("data/double-originator-confluence.eml");
		MessageBodyData result = BodyStreamProcessor.processBody(stream).get(2, TimeUnit.SECONDS);
		assertNotNull(result);
		JsonArray asJs = new JsonArray(JsonUtils.asString(result.body.recipients));
		System.err.println("JS: " + asJs.encodePrettily());
		long from = result.body.recipients.stream().filter(r -> r.kind == RecipientKind.Originator).count();
		assertEquals(1, from);
	}

	@Test
	public void testProcessFeatwebml1562()
			throws IOException, InterruptedException, ExecutionException, TimeoutException {
		Stream stream = openResource("data/featwebml-1562.eml");
		MessageBodyData result = BodyStreamProcessor.processBody(stream).get(2, TimeUnit.SECONDS);
		assertNotNull(result);
		JsonObject asJs = new JsonObject(JsonUtils.asString(result.body.structure));
		System.out.println("JS: " + asJs.encodePrettily());
	}

	@Test
	public void testBM18750() throws IOException, InterruptedException, ExecutionException, TimeoutException {
		Stream stream = openResource("data/BM-18750.eml");
		MessageBodyData result = BodyStreamProcessor.processBody(stream).get(2, TimeUnit.SECONDS);
		assertTrue(result.headers.get("x-bm-folderuid").value
				.contains("calendar:Default:cli-created-d095c2ea-2d5f-31c0-9224-30b8143818c1"));
	}

	@Test
	public void testProcessRescomEML() throws IOException, InterruptedException, ExecutionException, TimeoutException {
		Stream stream = openResource("data/msg_from_rescom.eml");
		MessageBodyData result = BodyStreamProcessor.processBody(stream).get(2, TimeUnit.SECONDS);
		assertNotNull(result);
		assertEquals("<5A538DC1.1060907@rescom.interieur.gouv.fr>", result.body.messageId);
		JsonObject asJs = new JsonObject(JsonUtils.asString(result.body.structure));
		System.out.println("JS: " + asJs.encodePrettily());
	}

	@Test
	public void testProcessOffice365NDR()
			throws IOException, InterruptedException, ExecutionException, TimeoutException {
		Stream stream = openResource("data/bm-15193.eml");
		MessageBodyData result = BodyStreamProcessor.processBody(stream).get(2, TimeUnit.SECONDS);
		assertNotNull(result);
		JsonObject asJs = new JsonObject(JsonUtils.asString(result.body.structure));
		System.out.println("JS: " + asJs.encodePrettily());
		String previewBytes = ByteBufUtil.hexDump(result.body.preview.getBytes());
		System.err.println(previewBytes);
		System.err.println(result.body.preview);
		String clearedPreview = result.body.preview.replace("\u0000", "");
		System.err.println(clearedPreview);
		String clearedBytes = ByteBufUtil.hexDump(clearedPreview.getBytes());
		assertEquals(clearedBytes, previewBytes);
	}

	@Test
	public void testProcessMessageRFC822()
			throws IOException, InterruptedException, ExecutionException, TimeoutException {
		Stream stream = openResource("data/attachment_message-rfc822.eml");
		MessageBodyData result = BodyStreamProcessor.processBody(stream).get(2, TimeUnit.SECONDS);
		assertNotNull(result);
		assertEquals("<44fd595fac96261985d920d7294ac5f1@blue-mind.net>", result.body.messageId);
		JsonObject asJs = new JsonObject(JsonUtils.asString(result.body.structure));
		System.out.println("JS: " + asJs.encodePrettily());
	}

	@Test
	public void testPleziBM14512() throws IOException, InterruptedException, ExecutionException, TimeoutException {
		Stream stream = openResource("data/plezi_BM14512.eml");
		MessageBodyData result = BodyStreamProcessor.processBody(stream).get(2, TimeUnit.SECONDS);
		assertNotNull(result);
		assertEquals("<5c9e279abe570_1224111f30dc374c0@spawn.mail>", result.body.messageId);
		JsonObject asJs = new JsonObject(JsonUtils.asString(result.body.structure));
		System.out.println("JS: " + asJs.encodePrettily());

		Optional<Part> excelPart = result.body.structure.children.stream()
				.filter(p -> p.fileName != null && p.fileName.endsWith(".xlsx")).findFirst();
		assertTrue(excelPart.isPresent());
		DispositionType excelPartDispoType = excelPart.isPresent() ? excelPart.get().dispositionType : null;
		assertEquals(DispositionType.ATTACHMENT, excelPartDispoType);
	}

	@Test
	public void testWorteks195() throws IOException, InterruptedException, ExecutionException, TimeoutException {

		Stream stream = openResource("data/worteks-195.eml");
		MessageBodyData result = BodyStreamProcessor.processBody(stream).get(2, TimeUnit.SECONDS);
		assertNotNull(result);
		JsonArray asJs = new JsonArray(JsonUtils.asString(result.body.recipients));
		System.out.println("JS: " + asJs.encodePrettily());

		asJs.forEach(obj -> {
			JsonObject o = (JsonObject) obj;
			String dn = o.getString("dn");
			if (dn != null) {
				assertFalse(dn.contains("=?"), "DN should not contain encoded words: " + dn);
			}
		});

	}

	@Test
	public void testWrongPartMimeForRoundcubeMessage() throws Exception {
		Stream stream = openResource("data/wrong_part_mime.eml");
		MessageBodyData result = BodyStreamProcessor.processBody(stream).get(2, TimeUnit.SECONDS);
		assertNotNull(result);
		JsonObject asJs = new JsonObject(JsonUtils.asString(result.body.structure));
		System.out.println("JS: " + asJs.encodePrettily());
		JsonObject secondPartInAlternative = asJs.getJsonArray("children").getJsonObject(1);
		assertNotNull(secondPartInAlternative);
		assertEquals("text/html", secondPartInAlternative.getString("mime"));
		assertEquals("UTF-8", secondPartInAlternative.getString("charset"));
	}

	@Test
	public void testReferencesHeader() throws IOException, InterruptedException, ExecutionException, TimeoutException {
		Stream stream = openResource("data/plezi_BM14512_with_references.eml");
		MessageBodyData result = BodyStreamProcessor.processBody(stream).get(2, TimeUnit.SECONDS);
		assertNotNull(result);
		assertEquals("<5c9e279abe570_1224111f30dc374c0@spawn.mail>", result.body.messageId);
		List<String> expectedRefs = Arrays.asList("<521DD9C9-6E9A-4F51-B809-8FABA51D742B@bluemind.net>",
				"<21174FB9-A2EB-4CD2-8383-1230243FBB2B@bluemind.net>",
				"<E562F887-8BBA-4DAD-B4A2-04E58B3DF4AB@blue-mind.net>");
		for (int i = 0; i < expectedRefs.size(); i++) {
			assertEquals(expectedRefs.get(i), result.body.references.get(i));
		}
		JsonObject asJs = new JsonObject(JsonUtils.asString(result.body.structure));
		System.out.println("JS: " + asJs.encodePrettily());

		Optional<Part> excelPart = result.body.structure.children.stream()
				.filter(p -> p.fileName != null && p.fileName.endsWith(".xlsx")).findFirst();
		assertTrue(excelPart.isPresent());
		DispositionType excelPartDispoType = excelPart.isPresent() ? excelPart.get().dispositionType : null;
		assertEquals(DispositionType.ATTACHMENT, excelPartDispoType);
	}

	@Test
	public void testProcessAttachmentAccent()
			throws IOException, InterruptedException, ExecutionException, TimeoutException {
		Stream stream = openResource("data/PJ_accent.eml");
		MessageBodyData result = BodyStreamProcessor.processBody(stream).get(2, TimeUnit.SECONDS);
		assertNotNull(result);
		JsonObject asJs = new JsonObject(JsonUtils.asString(result.body.structure));
		System.out.println("JS: " + asJs.encodePrettily());

		assertTrue(asJs.encodePrettily().contains("\"fileName\" : \"numérisé.pdf\","));
	}

	@Test
	public void testEmlBuilder() throws IOException, InterruptedException, ExecutionException, TimeoutException {
		Stream stream = openResource("data/with_inlines.eml");
		String partAddr = "junit";
		FileOutputStream out = new FileOutputStream(new File(Bodies.getFolder(sid), partAddr + ".part"));
		out.write("YEAH YEAH".getBytes());
		out.close();
		MessageBodyData result = BodyStreamProcessor.processBody(stream).get(2, TimeUnit.SECONDS);
		updateAddr(partAddr, result.body.structure);
		Message rebuilt = EmlBuilder.of(result.body, sid);
		ByteArrayOutputStream msgOut = new ByteArrayOutputStream();
		Mime4JHelper.serialize(rebuilt, msgOut);
		System.out.println("Re-created:\n" + msgOut.toString());
	}

	private void updateAddr(String partAddr, Part structure) {
		structure.address = partAddr;
		structure.children.forEach(p -> updateAddr(partAddr, p));
	}

	@Test
	public void testDispositionTypeFixed()
			throws InterruptedException, ExecutionException, TimeoutException, IOException {
		Stream stream = openResource("data/with_inlines.eml");
		// Apple-Mail inline attachments not displayed by other client

		MessageBodyData result = BodyStreamProcessor.processBody(stream).get(2, TimeUnit.SECONDS);
		assertNotNull(result);

		JsonObject asJs = new JsonObject(JsonUtils.asString(result.body.structure));
		System.out.println("JS: " + asJs.encodePrettily());

		// the second and third children of the multipart should have a fixed attachment
		// disposition type
		final DispositionType secondChildDispositionType = result.body.structure.children.get(1).dispositionType;
		assertEquals(DispositionType.ATTACHMENT, secondChildDispositionType);
		final DispositionType thirdChildDispositionType = result.body.structure.children.get(2).dispositionType;
		assertEquals(DispositionType.ATTACHMENT, thirdChildDispositionType);
		// the first child should not have one
		final DispositionType firstChildDispositionType = result.body.structure.children.get(0).dispositionType;
		assertNull(firstChildDispositionType);
		// should have real attachments (it is based on disposition type)
		assertTrue(result.body.structure.hasRealAttachments());
		assertEquals(2, result.body.structure.nonInlineAttachments().size());
	}

	@Test
	public void testSingleBodyHavingContentId()
			throws InterruptedException, ExecutionException, TimeoutException, IOException {
		Stream stream = openResource("data/single_body_cid.eml");
		// Apple-Mail inline attachments not displayed by other client

		MessageBodyData result = BodyStreamProcessor.processBody(stream).get(2, TimeUnit.SECONDS);
		assertNotNull(result);

		Part bodyPart = result.body.structure.children.get(0).children.get(0);
		DispositionType firstChildDispositionType = bodyPart.dispositionType;
		assertEquals("text/html", bodyPart.mime);
		assertEquals("<CZT9PGZUJFU4.ZWYKFCEACM0U3@valerie>", bodyPart.contentId);
		assertEquals(DispositionType.INLINE, firstChildDispositionType);
		DispositionType secondChildDispositionType = result.body.structure.children.get(1).dispositionType;
		assertEquals(DispositionType.ATTACHMENT, secondChildDispositionType);
	}

	@Test
	public void testInvalidBase64InEml()
			throws InterruptedException, ExecutionException, TimeoutException, IOException {
		Stream stream = openResource("data/invalid_base64.eml");
		// Apple-Mail inline attachments not displayed by other client

		MessageBodyData result = BodyStreamProcessor.processBody(stream).get(2, TimeUnit.SECONDS);
		assertNotNull(result);

		Part bodyPart = result.body.structure.children.get(0).children.get(0);
		DispositionType firstChildDispositionType = bodyPart.dispositionType;
		assertEquals("text/html", bodyPart.mime);
		assertEquals("<CZT9PGZUJFU4.ZWYKFCEACM0U3@valerie>", bodyPart.contentId);
		assertEquals(DispositionType.INLINE, firstChildDispositionType);
		DispositionType secondChildDispositionType = result.body.structure.children.get(1).dispositionType;
		assertEquals(DispositionType.ATTACHMENT, secondChildDispositionType);
	}

	/**
	 * When Content-Id is set without CID nor Disposition-Type then we should keep a
	 * NULL disposition type (Linkedin mails).
	 */
	@Test
	public void testDispositionTypeFixed2()
			throws InterruptedException, ExecutionException, TimeoutException, IOException {
		Stream stream = openResource("data/BM-16700-contentid-no-cid-no-dispotype.eml");

		MessageBodyData result = BodyStreamProcessor.processBody(stream).get(2, TimeUnit.SECONDS);
		assertNotNull(result);

		JsonObject asJs = new JsonObject(JsonUtils.asString(result.body.structure));
		System.out.println("JS: " + asJs.encodePrettily());

		final DispositionType firstChildDispositionType = result.body.structure.children.get(0).dispositionType;
		assertEquals(null, firstChildDispositionType);
		final DispositionType secondChildDispositionType = result.body.structure.children.get(1).dispositionType;
		assertEquals(null, secondChildDispositionType);
	}

	/**
	 * {@link MessageBody#preview} should not contain HTML nor have more than one
	 * line nor have unnecessary white characters.
	 */
	@Test
	public void testMessageBodyPreview()
			throws IOException, InterruptedException, ExecutionException, TimeoutException {
		// single part mail
		this.testMessageBodyPreview("data/simple_text_html.eml", "My body is a temple !!!");

		// multipart mail
		this.testMessageBodyPreview("data/with_inlines.eml", "Some text, An inline image : And a PDF.");

		// preview should not contain html tags
		this.testMessageBodyPreview("data/BM-15740.eml",
				"RELAI D’INFORMATION DE LA MAIRIE AUX ETABLISSEMENTS PUBLICS ET PRIVES DE LABEGE Mesdames, Messieurs, Vous trouverez en Pièce Jointe une note de recommandations");

	}

	private void testMessageBodyPreview(final String file, final String expectedPreview)
			throws IOException, InterruptedException, ExecutionException, TimeoutException {
		final Stream stream = openResource(file);
		final MessageBodyData result = BodyStreamProcessor.processBody(stream).get(2, TimeUnit.SECONDS);
		assertNotNull(result);
		assertNotNull(result.body);
		System.err.println(expectedPreview + " " + expectedPreview.length());
		System.err.println(result.body.preview + " " + result.body.preview.length());
		assertEquals(expectedPreview, result.body.preview);
	}

	@Test
	public void testMessageBodyPreview_Attachment()
			throws IOException, InterruptedException, ExecutionException, TimeoutException {
		this.testMessageBodyPreview("data/attachment.eml", "my body is a wonderland");

	}

	@Test
	public void testNonDeliveryReportWithEml()
			throws IOException, InterruptedException, ExecutionException, TimeoutException {
		Stream stream = openResource("data/undelivered_with_eml.eml");
		MessageBodyData result = BodyStreamProcessor.processBody(stream).get(2, TimeUnit.SECONDS);
		assertNotNull(result);
		JsonObject asJs = new JsonObject(JsonUtils.asString(result.body.structure));
		System.out.println("JS: " + asJs.encodePrettily());

		Part secondChild = result.body.structure.children.get(1);
		assertEquals(DispositionType.ATTACHMENT, secondChild.dispositionType);
		assertEquals("details.txt", secondChild.fileName);
		Part thirdChild = result.body.structure.children.get(2);
		assertEquals(DispositionType.ATTACHMENT, thirdChild.dispositionType);
		assertEquals("Forwarded message.eml", thirdChild.fileName);
	}

	@Test
	public void testNonDeliveryReportWithHeaders()
			throws IOException, InterruptedException, ExecutionException, TimeoutException {
		Stream stream = openResource("data/undelivered_with_headers.eml");
		MessageBodyData result = BodyStreamProcessor.processBody(stream).get(2, TimeUnit.SECONDS);
		assertNotNull(result);
		JsonObject asJs = new JsonObject(JsonUtils.asString(result.body.structure));
		System.out.println("JS: " + asJs.encodePrettily());

		Part secondChild = result.body.structure.children.get(1);
		assertEquals(DispositionType.ATTACHMENT, secondChild.dispositionType);
		assertEquals("details.txt", secondChild.fileName);
		Part thirdChild = result.body.structure.children.get(2);
		assertEquals(DispositionType.ATTACHMENT, thirdChild.dispositionType);
		assertEquals("Original Message Headers.txt", thirdChild.fileName);
	}

	@Test
	public void testEmptyFromAddress() throws IOException, InterruptedException, ExecutionException, TimeoutException {
		Stream stream = openResource("data/empty_from_address.eml");
		MessageBodyData result = BodyStreamProcessor.processBody(stream).get(2, TimeUnit.SECONDS);
		assertNotNull(result);

		List<Recipient> originators = result.body.recipients.stream().filter(r -> r.kind == RecipientKind.Originator)
				.collect(Collectors.toList());
		assertEquals(1, originators.size());
		Recipient originator = originators.get(0);
		assertEquals(null, originator.dn);
		assertEquals("Christian Bergere", originator.address);

		JsonObject asJs = new JsonObject(JsonUtils.asString(result.body.structure));
		System.out.println("JS: " + asJs.encodePrettily());

	}

	@Test
	public void testEncodedContentType() throws Exception {
		Stream stream = openResource("data/encoded_contentType.eml");
		MessageBodyData result = BodyStreamProcessor.processBody(stream).get(2, TimeUnit.SECONDS);
		assertNotNull(result);

		// part 0 is multipart/alternative

		Part attachment = result.body.structure.children.get(1);
		assertEquals("application/pdf", attachment.mime);

	}
}
