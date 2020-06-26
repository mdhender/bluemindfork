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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import org.apache.james.mime4j.dom.Message;
import org.junit.Assert;
import org.junit.Test;

import com.google.common.io.ByteStreams;

import io.netty.buffer.ByteBufUtil;
import io.vertx.core.buffer.Buffer;
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
		FileOutputStream out = new FileOutputStream(new File(Bodies.STAGING, partAddr + ".part"));
		out.write("YEAH YEAH".getBytes());
		out.close();
		MessageBodyData result = BodyStreamProcessor.processBody(stream).get(2, TimeUnit.SECONDS);
		updateAddr(partAddr, result.body.structure);
		Message rebuilt = EmlBuilder.of(result.body, "owner");
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
		Assert.assertEquals(DispositionType.ATTACHMENT, secondChildDispositionType);
		final DispositionType thirdChildDispositionType = result.body.structure.children.get(2).dispositionType;
		Assert.assertEquals(DispositionType.ATTACHMENT, thirdChildDispositionType);
		// the first child should not have one
		final DispositionType firstChildDispositionType = result.body.structure.children.get(0).dispositionType;
		Assert.assertNull(firstChildDispositionType);
		// should have real attachments (it is based on disposition type)
		Assert.assertTrue(result.body.structure.hasRealAttachments());
		Assert.assertEquals(2, result.body.structure.nonInlineAttachments().size());
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
		Assert.assertEquals(DispositionType.ATTACHMENT, secondChild.dispositionType);
		Assert.assertEquals("details.txt", secondChild.fileName);
		Part thirdChild = result.body.structure.children.get(2);
		Assert.assertEquals(DispositionType.ATTACHMENT, thirdChild.dispositionType);
		Assert.assertEquals("Forwarded message.eml", thirdChild.fileName);
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
		Assert.assertEquals(DispositionType.ATTACHMENT, secondChild.dispositionType);
		Assert.assertEquals("details.txt", secondChild.fileName);
		Part thirdChild = result.body.structure.children.get(2);
		Assert.assertEquals(DispositionType.ATTACHMENT, thirdChild.dispositionType);
		Assert.assertEquals("Undelivred Message Headers.txt", thirdChild.fileName);
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
}
