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
package net.bluemind.eas.wbxml.builder.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.xml.transform.TransformerException;

import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import net.bluemind.core.rest.base.SlowWriteStream;
import net.bluemind.eas.dto.NamespaceMapping;
import net.bluemind.eas.dto.base.Callback;
import net.bluemind.eas.dto.base.DisposableByteSource;
import net.bluemind.eas.serdes.AsyncBuildHelper;
import net.bluemind.eas.serdes.AsyncBuildHelper.IBuildOperation;
import net.bluemind.eas.serdes.IResponseBuilder;
import net.bluemind.eas.testhelper.vertx.Deploy;
import net.bluemind.eas.testhelper.vertx.Deploy.VerticleConstructor;
import net.bluemind.eas.utils.DOMUtils;
import net.bluemind.eas.wbxml.WBXMLTools;
import net.bluemind.eas.wbxml.WbxmlOutput;
import net.bluemind.eas.wbxml.builder.WbxmlResponseBuilder;
import net.bluemind.eas.wbxml.builder.vertx.ByteSourceEventProducer;
import net.bluemind.lib.vertx.VertxPlatform;

public class ResponseBuilderTests {

	private Set<String> deployements;

	public void setUp() {
		deployements = Deploy.beforeTest(new VerticleConstructor[0],
				VerticleConstructor.of(ByteSourceEventProducer::new));
	}

	private static final class LatchCountdown implements Callback<Void> {
		private CountDownLatch cdl = new CountDownLatch(1);

		public void expectCompletion() {
			try {
				assertTrue(cdl.await(10, TimeUnit.SECONDS));
			} catch (InterruptedException e) {
			}
		}

		@Override
		public void onResult(Void data) {
			cdl.countDown();
		}

	}

	@Test
	public void testRootOnlyDocument() throws IOException, TransformerException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		WbxmlOutput output = WbxmlOutput.of(bos);
		WbxmlResponseBuilder builder = new WbxmlResponseBuilder(14.1, null, output);
		LatchCountdown completion = new LatchCountdown();
		builder.start(NamespaceMapping.SYNC).end(completion);
		completion.expectCompletion();
		byte[] wbxmlBytes = bos.toByteArray();
		assertNotNull(wbxmlBytes);
		assertTrue(wbxmlBytes.length > 0);
		Document doc = WBXMLTools.toXml(wbxmlBytes);
		assertNotNull(doc);
	}

	@Test
	public void testNestedContainer() throws IOException, TransformerException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		WbxmlOutput output = WbxmlOutput.of(bos);
		WbxmlResponseBuilder builder = new WbxmlResponseBuilder(14.1, null, output);
		LatchCountdown completion = new LatchCountdown();
		builder.start(NamespaceMapping.SYNC);
		builder.container("Collections").container("Collection").endContainer().endContainer().end(completion);
		completion.expectCompletion();
		byte[] wbxmlBytes = bos.toByteArray();
		assertNotNull(wbxmlBytes);
		assertTrue(wbxmlBytes.length > 0);
		Document doc = WBXMLTools.toXml(wbxmlBytes);
		DOMUtils.logDom(doc);
		NodeList nl = doc.getDocumentElement().getElementsByTagName("Collections");
		assertEquals(1, nl.getLength());
		Element collections = (Element) nl.item(0);
		assertEquals(1, collections.getChildNodes().getLength());
	}

	@Test
	public void testSiblingContainer() throws IOException, TransformerException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		WbxmlOutput output = WbxmlOutput.of(bos);
		WbxmlResponseBuilder builder = new WbxmlResponseBuilder(14.1, null, output);
		LatchCountdown completion = new LatchCountdown();
		builder.start(NamespaceMapping.SYNC);
		builder.container("Collections");
		builder.container("Collection").endContainer();
		builder.container(NamespaceMapping.SYNC, "Collection").endContainer();
		builder.endContainer(); // collections
		builder.end(completion);
		completion.expectCompletion();
		byte[] wbxmlBytes = bos.toByteArray();
		assertNotNull(wbxmlBytes);
		assertTrue(wbxmlBytes.length > 0);
		Document doc = WBXMLTools.toXml(wbxmlBytes);
		DOMUtils.logDom(doc);
	}

	@Test
	public void testContainerWithContent() throws IOException, TransformerException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		WbxmlOutput output = WbxmlOutput.of(bos);
		WbxmlResponseBuilder builder = new WbxmlResponseBuilder(14.1, null, output);
		LatchCountdown completion = new LatchCountdown();
		builder.start(NamespaceMapping.SYNC);
		builder.container("Collections").container("Collection");
		builder.text("SyncKey", "123456");
		builder.token("MoreAvailable");
		builder.endContainer().endContainer().end(completion);
		completion.expectCompletion();
		byte[] wbxmlBytes = bos.toByteArray();
		assertNotNull(wbxmlBytes);
		assertTrue(wbxmlBytes.length > 0);
		Document doc = WBXMLTools.toXml(wbxmlBytes);
		DOMUtils.logDom(doc);
	}

	@Test
	public void testBackpressureHandling() throws Exception {
		VertxPlatform.getVertx().deployVerticle(new ByteSourceEventProducer());

		SlowWriteStream sl = new SlowWriteStream();
		WbxmlOutput output = new VertxTestOutput(sl);

		WbxmlResponseBuilder builder = new WbxmlResponseBuilder(14.1, null, output);
		final LatchCountdown completion = new LatchCountdown();
		builder.start(NamespaceMapping.SYNC);
		builder.container("Collections").container("Collection");
		builder.text("SyncKey", "123456");
		builder.container("Commands");
		builder.container("Add");
		builder.text("ServerId", "42:666");
		builder.container("ApplicationData");
		builder.container(NamespaceMapping.AIR_SYNC_BASE, "Body");
		DisposableByteSource streamable = randomStreamData(4 * 65536);
		builder.stream("Data", streamable, new Callback<IResponseBuilder>() {

			@Override
			public void onResult(IResponseBuilder b) {
				b.endContainer(); // body
				b.endContainer(); // appdata
				b.endContainer(); // add
				b.endContainer(); // commands
				b.endContainer().endContainer();
				b.end(completion);
			}
		});
		completion.expectCompletion();
	}

	@Test
	public void testContainerWithDataStream() throws Exception {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		WbxmlOutput output = WbxmlOutput.of(bos);

		WbxmlResponseBuilder builder = new WbxmlResponseBuilder(14.1, null, output);
		final LatchCountdown completion = new LatchCountdown();
		builder.start(NamespaceMapping.SYNC);
		builder.container("Collections").container("Collection");
		builder.text("SyncKey", "123456");
		builder.container("Commands");
		builder.container("Add");
		builder.text("ServerId", "42:666");
		builder.container("ApplicationData");
		builder.container(NamespaceMapping.AIR_SYNC_BASE, "Body");
		DisposableByteSource streamable = randomStreamData(4 * 8192);
		builder.stream("Data", streamable, new Callback<IResponseBuilder>() {

			@Override
			public void onResult(IResponseBuilder b) {
				b.endContainer(); // body
				b.endContainer(); // appdata
				b.endContainer(); // add
				b.endContainer(); // commands
				b.endContainer().endContainer();
				b.end(completion);
			}
		});
		completion.expectCompletion();
		byte[] wbxmlBytes = bos.toByteArray();
		assertNotNull(wbxmlBytes);
		assertTrue(wbxmlBytes.length > 0);
		Document doc = WBXMLTools.toXml(wbxmlBytes);
		DOMUtils.logDom(doc);
	}

	@Test
	public void testContainerWithMultipleStreams() throws IOException, TransformerException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		WbxmlOutput output = WbxmlOutput.of(bos);
		IResponseBuilder builder = new WbxmlResponseBuilder(14.1, null, output);
		final LatchCountdown completion = new LatchCountdown();
		builder.start(NamespaceMapping.SYNC);
		builder.container("Collections").container("Collection");
		builder.text("SyncKey", "123456");
		builder.container("Commands");

		int loops = 10;
		List<DisposableByteSource> toStream = new LinkedList<>();
		for (int i = 0; i < loops; i++) {
			toStream.add(randomStreamData(1 * 1024 * 1024));
		}
		Iterator<DisposableByteSource> it = toStream.iterator();
		IBuildOperation<DisposableByteSource, IResponseBuilder> op = new IBuildOperation<DisposableByteSource, IResponseBuilder>() {

			@Override
			public void beforeAsync(IResponseBuilder b, DisposableByteSource t, Callback<IResponseBuilder> forAsync) {
				b.container(NamespaceMapping.SYNC, "Add");
				b.text(NamespaceMapping.SYNC, "ServerId", "42:" + System.currentTimeMillis());
				b.container(NamespaceMapping.SYNC, "ApplicationData");
				b.container(NamespaceMapping.AIR_SYNC_BASE, "Body");
				b.stream(NamespaceMapping.AIR_SYNC_BASE, "Data", t, forAsync);
			}

			@Override
			public void afterAsync(IResponseBuilder b, DisposableByteSource t) {
				b.endContainer(); // Body
				b.endContainer(); // ApplicationData
				b.endContainer(); // Add
			}
		};

		Callback<IResponseBuilder> afterBuild = new Callback<IResponseBuilder>() {

			@Override
			public void onResult(IResponseBuilder builder) {
				builder.endContainer(); // commands
				builder.endContainer().endContainer();
				builder.end(completion);
			}
		};
		AsyncBuildHelper<DisposableByteSource, IResponseBuilder> abh = new AsyncBuildHelper<>(it, op, afterBuild);
		abh.build(builder);

		completion.expectCompletion();
		byte[] wbxmlBytes = bos.toByteArray();
		assertNotNull(wbxmlBytes);
		assertTrue(wbxmlBytes.length > 0);
		Document doc = WBXMLTools.toXml(wbxmlBytes);
		NodeList dataElements = doc.getElementsByTagName("Data");
		assertEquals(loops, dataElements.getLength());
	}

	private DisposableByteSource randomStreamData(int size) {
		StringBuilder sb = new StringBuilder();
		while (sb.length() < size) {
			sb.append("Lorem ipsum sid amet. ");
		}
		return DisposableByteSource.wrap(sb.toString());
	}

	public void tearDown() {
		Deploy.afterTest(deployements);
	}

}
