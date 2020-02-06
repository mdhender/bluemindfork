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
package net.bluemind.eas.endpoint.tests;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.james.mime4j.MimeException;
import org.apache.james.mime4j.dom.Header;
import org.apache.james.mime4j.dom.Message;
import org.apache.james.mime4j.dom.TextBody;
import org.apache.james.mime4j.field.LenientFieldParser;
import org.apache.james.mime4j.message.BasicBodyFactory;
import org.apache.james.mime4j.message.MessageImpl;
import org.apache.james.mime4j.stream.Field;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.google.common.io.CharStreams;

import io.vertx.core.buffer.Buffer;
import net.bluemind.eas.command.folder.sync.FolderSyncEndpoint;
import net.bluemind.eas.command.sync.SyncEndpoint;
import net.bluemind.eas.endpoint.tests.bodyoptions.ISyncOptionsProvider;
import net.bluemind.eas.endpoint.tests.bodyoptions.NotTruncatedHtml;
import net.bluemind.eas.endpoint.tests.bodyoptions.NotTruncatedMime;
import net.bluemind.eas.endpoint.tests.bodyoptions.TruncatedText;
import net.bluemind.eas.http.wbxml.WbxmlHandlerBase;
import net.bluemind.eas.testhelper.mock.ResponseObject;
import net.bluemind.eas.utils.DOMUtils;
import net.bluemind.eas.utils.FileUtils;
import net.bluemind.eas.wbxml.WBXMLTools;
import net.bluemind.hornetq.client.MQ;
import net.bluemind.hornetq.client.MQ.IMQConnectHandler;
import net.bluemind.imap.FlagsList;
import net.bluemind.mime4j.common.Mime4JHelper;

public class SyncEndpointBodyOptionsTests extends AbstractEndpointTest {

	private int inboxServerId;

	public void setUp() throws Exception {
		super.setUp();
		final CountDownLatch mqLatch = new CountDownLatch(1);
		MQ.init(new IMQConnectHandler() {

			@Override
			public void connected() {
				mqLatch.countDown();
			}
		});
		mqLatch.await(5, TimeUnit.SECONDS);
		fetchFolderIds();
	}

	private void fetchFolderIds() throws IOException {
		FolderSyncEndpoint fse = new FolderSyncEndpoint();
		Document document = DOMUtils.createDoc("FolderHierarchy", "FolderSync");
		Element root = document.getDocumentElement();
		Element sk = document.createElementNS("FolderHierarchy", "SyncKey");
		sk.setTextContent("0");
		root.appendChild(sk);
		ResponseObject folderSyncResponse = runEndpoint(fse, document);
		Buffer content = folderSyncResponse.content;
		Document folderSync = WBXMLTools.toXml(content.getBytes());
		assertNotNull(folderSync);
		NodeList added = folderSync.getDocumentElement().getElementsByTagNameNS("FolderHierarchy", "Add");
		for (int i = 0; i < added.getLength(); i++) {
			Element el = (Element) added.item(i);
			String folderType = DOMUtils.getUniqueElement(el, "Type").getTextContent();
			if ("2".equals(folderType)) { // INBOX
				inboxServerId = Integer.parseInt(DOMUtils.getUniqueElement(el, "ServerId").getTextContent());
			}
		}
		assertNotNull(inboxServerId);
		assertTrue(inboxServerId > 0);
		System.out.println("Inbox has serverId:" + inboxServerId);
	}

	public void tearDown() throws Exception {
		super.tearDown();
	}

	@Override
	public WbxmlHandlerBase createEndpoint() {
		return new SyncEndpoint();
	}

	public void testSyncTruncatedText40First() throws Exception {
		// SK 0
		String syncKey = initSyncChain();

		appendEml("INBOX", "data/Sync/lorem_ipsum_plain_text.eml", new FlagsList());

		Document resp = runSyncEndpoint(inboxServerId, syncKey, new TruncatedText(40));
		DOMUtils.logDom(resp);
		syncKey = syncKey(resp);
		Element commands = DOMUtils.getUniqueElement(resp.getDocumentElement(), "Commands");
		assertEquals("1 new mail added by test in inbox, we expect that", 1, commands.getChildNodes().getLength());
		Element item = (Element) commands.getChildNodes().item(0);
		assertEquals("Add", item.getNodeName());
		Element appDataElem = DOMUtils.getUniqueElement(item, "ApplicationData");
		assertNotNull("ApplicationData must be present for an added email", appDataElem);
		assertEquals("The truncated element must me present", "1",
				DOMUtils.getUniqueElement(appDataElem, "Truncated").getTextContent());
		Element dataElem = DOMUtils.getUniqueElement(appDataElem, "Data");
		assertNotNull(dataElem);
		String bodyText = dataElem.getTextContent();
		System.out.println("plain.body:[" + bodyText + "]");
		assertTrue(bodyText.startsWith("Lorem ipsum"));
		assertFalse(bodyText.contains("elementum"));
	}

	public void testSyncWantHtmlOnTextOnly() throws Exception {
		// SK 0
		String syncKey = initSyncChain();

		appendEml("INBOX", "data/Sync/lorem_ipsum_plain_text.eml", new FlagsList());

		Document resp = runSyncEndpoint(inboxServerId, syncKey, new NotTruncatedHtml());
		syncKey = syncKey(resp);
		Element commands = DOMUtils.getUniqueElement(resp.getDocumentElement(), "Commands");
		assertEquals("1 new mail added by test in inbox, we expect that", 1, commands.getChildNodes().getLength());
		Element item = (Element) commands.getChildNodes().item(0);
		assertEquals("Add", item.getNodeName());
		Element appDataElem = DOMUtils.getUniqueElement(item, "ApplicationData");
		assertNotNull("ApplicationData must be present for an added email", appDataElem);
		assertEquals("The truncated element must me present", "0",
				DOMUtils.getUniqueElement(appDataElem, "Truncated").getTextContent());
		Element dataElem = DOMUtils.getUniqueElement(appDataElem, "Data");
		assertNotNull(dataElem);
		String bodyText = dataElem.getTextContent();
		System.out.println("html.body:[" + bodyText + "]");
		assertTrue(bodyText.startsWith("<html><body>Lorem ipsum"));
		assertTrue(bodyText.contains("elementum"));
	}

	public void testSyncWantHtmlStrangeBody() throws Exception {
		// SK 0
		String syncKey = initSyncChain();

		appendEml("INBOX", "data/Sync/strange_body.eml", new FlagsList());

		Document resp = runSyncEndpoint(inboxServerId, syncKey, new NotTruncatedHtml());
		syncKey = syncKey(resp);
		Element commands = DOMUtils.getUniqueElement(resp.getDocumentElement(), "Commands");
		assertEquals("1 new mail added by test in inbox, we expect that", 1, commands.getChildNodes().getLength());
		Element item = (Element) commands.getChildNodes().item(0);
		assertEquals("Add", item.getNodeName());
		Element appDataElem = DOMUtils.getUniqueElement(item, "ApplicationData");
		assertNotNull("ApplicationData must be present for an added email", appDataElem);
		assertEquals("The truncated element must me present", "0",
				DOMUtils.getUniqueElement(appDataElem, "Truncated").getTextContent());
		Element dataElem = DOMUtils.getUniqueElement(appDataElem, "Data");
		assertNotNull(dataElem);
		String bodyText = dataElem.getTextContent();
		System.out.println("html.body:[" + bodyText + "]");
		assertTrue(bodyText.contains("\"Votre invitation aux argus d’or 2016\""));
	}

	public void testSyncTruncatedText1000First() throws Exception {
		// SK 0
		String syncKey = initSyncChain();

		appendEml("INBOX", "data/Sync/lorem_ipsum_plain_text.eml", new FlagsList());

		Document resp = runSyncEndpoint(inboxServerId, syncKey, new TruncatedText(1000));
		syncKey = syncKey(resp);
		Element commands = DOMUtils.getUniqueElement(resp.getDocumentElement(), "Commands");
		assertEquals("1 new mail added by test in inbox, we expect that", 1, commands.getChildNodes().getLength());
		Element item = (Element) commands.getChildNodes().item(0);
		assertEquals("Add", item.getNodeName());
		Element appDataElem = DOMUtils.getUniqueElement(item, "ApplicationData");
		assertNotNull("ApplicationData must be present for an added email", appDataElem);
		assertEquals("The truncated element must me present", "1",
				DOMUtils.getUniqueElement(appDataElem, "Truncated").getTextContent());
		Element dataElem = DOMUtils.getUniqueElement(appDataElem, "Data");
		assertNotNull(dataElem);
		String bodyText = dataElem.getTextContent();
		System.out.println("plain.body:[" + bodyText + "]");
		assertTrue(bodyText.startsWith("Lorem ipsum"));
		assertTrue(bodyText.contains("elementum"));
	}

	public void testSyncLargerThanSizeTruncation() throws Exception {
		// SK 0
		String syncKey = initSyncChain();

		appendEml("INBOX", "data/Sync/lorem_ipsum_plain_text.eml", new FlagsList());

		Document resp = runSyncEndpoint(inboxServerId, syncKey, new TruncatedText(3000));
		syncKey = syncKey(resp);
		Element commands = DOMUtils.getUniqueElement(resp.getDocumentElement(), "Commands");
		assertEquals("1 new mail added by test in inbox, we expect that", 1, commands.getChildNodes().getLength());
		Element item = (Element) commands.getChildNodes().item(0);
		assertEquals("Add", item.getNodeName());
		Element appDataElem = DOMUtils.getUniqueElement(item, "ApplicationData");
		assertNotNull("ApplicationData must be present for an added email", appDataElem);
		assertEquals("The truncated element must be = 0", "0",
				DOMUtils.getUniqueElement(appDataElem, "Truncated").getTextContent());
		Element dataElem = DOMUtils.getUniqueElement(appDataElem, "Data");
		assertNotNull(dataElem);
		String bodyText = dataElem.getTextContent();
		System.out.println("plain.body:[" + bodyText + "]");
		assertTrue(bodyText.length() > 2000);
		assertTrue(bodyText.length() < 3000);
	}

	public void testSyncFullMime() throws Exception {
		// SK 0
		String syncKey = initSyncChain();

		// append emails
		appendEml("INBOX", "data/Sync/not_truncated_mime.eml", new FlagsList());

		Document resp = runSyncEndpoint(inboxServerId, syncKey, new NotTruncatedMime());
		syncKey = syncKey(resp);
		Element commands = DOMUtils.getUniqueElement(resp.getDocumentElement(), "Commands");
		assertEquals("1 new mail added by test in inbox, we expect that", 1, commands.getChildNodes().getLength());
		Element item = (Element) commands.getChildNodes().item(0);
		assertEquals("Add", item.getNodeName());
		Element appDataElem = DOMUtils.getUniqueElement(item, "ApplicationData");
		assertNotNull("ApplicationData must be present for an added email", appDataElem);
		Element dataElem = DOMUtils.getUniqueElement(appDataElem, "Data");
		assertNotNull(dataElem);
		String emlText = dataElem.getTextContent();
		System.out.println("eml:[" + emlText + "]");
		assertTrue("The mime part boundary was not found in the returned body",
				emlText.contains("--Apple-Mail=_2A9EAB5A-679F-40F1-AC00-5230637E3DDF"));
	}

	public void XXtestGenerate8bitCrap() throws Exception {
		try (MessageImpl mi = new MessageImpl()) {
			mi.setSubject("8bit email");
			BasicBodyFactory bbf = new BasicBodyFactory();
			TextBody body = bbf.textBody("€uro àccentué", Charset.forName("iso-8859-15"));
			mi.setBody(body);
			Header h = mi.getHeader();
			h.addField(field("Content-Type", "text/plain; charset=iso-8859-15"));
			h.addField(field("Content-Transfer-Encoding", "8bit"));
			InputStream in = Mime4JHelper.asStream(mi);
			File f = new File("/Users/tom/8bit_msg.eml");
			FileOutputStream out = new FileOutputStream(f);
			FileUtils.transfer(in, out, true);
		}
	}

	private Field field(String k, String v) throws MimeException {
		return LenientFieldParser.parse(k + ": " + v);
	}

	public void testSyncFullMimeNotUtf8Safe() throws Exception {
		// SK 0
		String syncKey = initSyncChain();

		// append emails
		appendEml("INBOX", "data/Sync/mime_not_utf8_safe.eml", new FlagsList());

		Document resp = runSyncEndpoint(inboxServerId, syncKey, new NotTruncatedMime());
		syncKey = syncKey(resp);
		Element commands = DOMUtils.getUniqueElement(resp.getDocumentElement(), "Commands");
		assertEquals("1 new mail added by test in inbox, we expect that", 1, commands.getChildNodes().getLength());
		Element item = (Element) commands.getChildNodes().item(0);
		assertEquals("Add", item.getNodeName());
		Element appDataElem = DOMUtils.getUniqueElement(item, "ApplicationData");
		assertNotNull("ApplicationData must be present for an added email", appDataElem);
		Element dataElem = DOMUtils.getUniqueElement(appDataElem, "Data");
		assertNotNull(dataElem);
		String emlText = dataElem.getTextContent();
		Message reparsed = Mime4JHelper.parse(new ByteArrayInputStream(emlText.getBytes()));
		TextBody tb = (TextBody) reparsed.getBody();
		Reader reader = tb.getReader();
		emlText = CharStreams.toString(reader);
		System.out.println("eml:[" + emlText + "]");
		assertTrue("diacritics are not safely transferred", emlText.contains("€uro"));
		assertTrue("diacritics are not safely transferred", emlText.contains("àccentué"));
	}

	public void testSyncDraftWithMissingPieces() throws Exception {
		// SK 0
		String syncKey = initSyncChain();

		// append emails
		appendEml("INBOX", "data/Sync/draft_with_missing_pieces.eml", new FlagsList());

		Document resp = runSyncEndpoint(inboxServerId, syncKey, new NotTruncatedMime());
		syncKey = syncKey(resp);
		Element commands = DOMUtils.getUniqueElement(resp.getDocumentElement(), "Commands");
		assertEquals("1 new mail added by test in inbox, we expect that", 1, commands.getChildNodes().getLength());
		Element item = (Element) commands.getChildNodes().item(0);
		assertEquals("Add", item.getNodeName());
		Element appDataElem = DOMUtils.getUniqueElement(item, "ApplicationData");
		assertNotNull("ApplicationData must be present for an added email", appDataElem);
	}

	private String initSyncChain() throws IOException {
		Document sync = DOMUtils.createDoc("AirSync", "Sync");
		Element root = sync.getDocumentElement();
		Element collections = DOMUtils.createElement(root, "Collections");
		Element collection = DOMUtils.createElement(collections, "Collection");
		DOMUtils.createElementAndText(collection, "SyncKey", "0");
		DOMUtils.createElementAndText(collection, "CollectionId", Integer.toString(inboxServerId));
		ResponseObject response = runEndpoint(sync);
		assertEquals(200, response.getStatusCode());
		Buffer content = response.content;
		Document resp = WBXMLTools.toXml(content.getBytes());
		String syncKey = syncKey(resp);
		assertNotNull(syncKey);
		String collectionId = DOMUtils.getUniqueElement(resp.getDocumentElement(), "CollectionId").getTextContent();
		assertEquals(inboxServerId + "", collectionId);
		String status = DOMUtils.getUniqueElement(resp.getDocumentElement(), "Status").getTextContent();
		assertEquals("1", status);
		Element moreAvailable = DOMUtils.getUniqueElement(resp.getDocumentElement(), "MoreAvailable");
		assertNull(moreAvailable);
		Element commands = DOMUtils.getUniqueElement(resp.getDocumentElement(), "Commands");
		assertNull(commands);

		// FULL SYNC
		boolean done = false;
		while (!done) {
			resp = runSyncEndpoint(inboxServerId, syncKey);
			syncKey = syncKey(resp);
			moreAvailable = DOMUtils.getUniqueElement(resp.getDocumentElement(), "MoreAvailable");
			done = (moreAvailable == null);
		}
		return syncKey;
	}

	private static interface IClientChangeProvider {
		void setClientChanges(int collectionId, Element collectionElement);
	}

	private Document runSyncEndpoint(Integer collectionId, String syncKey, ISyncOptionsProvider options)
			throws IOException {
		return runSyncEndpoint(collectionId, syncKey, new IClientChangeProvider() {

			@Override
			public void setClientChanges(int collectionId, Element collectionElement) {
			}
		}, options);
	}

	private Document runSyncEndpoint(Integer collectionId, String syncKey, IClientChangeProvider clientChanges,
			ISyncOptionsProvider optionsProv) throws IOException {
		Document sync = DOMUtils.createDoc("AirSync", "Sync");
		Element root = sync.getDocumentElement();
		Element collections = DOMUtils.createElement(root, "Collections");
		Element collection = DOMUtils.createElement(collections, "Collection");
		DOMUtils.createElementAndText(collection, "SyncKey", syncKey);
		DOMUtils.createElementAndText(collection, "CollectionId", Integer.toString(collectionId));
		DOMUtils.createElementAndText(collection, "DeletesAsMoves", "1");
		DOMUtils.createElement(collection, "GetChanges");
		DOMUtils.createElementAndText(collection, "WindowSize", "256");
		Element options = DOMUtils.createElement(collection, "Options");
		optionsProv.setSyncOptions(options);
		clientChanges.setClientChanges(collectionId, collection);

		ResponseObject response = runEndpoint(sync);
		assertEquals(200, response.getStatusCode());
		Buffer content = response.content;
		Document resp = WBXMLTools.toXml(content.getBytes());
		return resp;
	}

	private String syncKey(Document doc) {
		return DOMUtils.getUniqueElement(doc.getDocumentElement(), "SyncKey").getTextContent();
	}

	private Document runSyncEndpoint(Integer collectionId, String syncKey) throws IOException {
		return runSyncEndpoint(collectionId, syncKey, new IClientChangeProvider() {

			@Override
			public void setClientChanges(int collectionId, Element collectionElement) {
			}
		}, new ISyncOptionsProvider() {

			@Override
			public void setSyncOptions(Element options) {
				DOMUtils.createElementAndText(options, "FilterType", "3");
				Element bodyPreference = DOMUtils.createElement(options, "AirSyncBase:BodyPreference");
				DOMUtils.createElementAndText(bodyPreference, "Type", "2");
				DOMUtils.createElementAndText(bodyPreference, "TruncationSize", "200000");
			}
		});
	}

}
