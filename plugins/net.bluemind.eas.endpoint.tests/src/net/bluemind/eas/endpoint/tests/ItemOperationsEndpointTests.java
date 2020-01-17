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

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.google.common.collect.ImmutableMap;
import com.google.common.io.ByteStreams;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.vertx.core.buffer.Buffer;
import net.bluemind.eas.command.folder.sync.FolderSyncEndpoint;
import net.bluemind.eas.command.itemoperations.ItemOperationsEndpoint;
import net.bluemind.eas.command.sync.SyncEndpoint;
import net.bluemind.eas.dto.NamespaceMapping;
import net.bluemind.eas.dto.foldersync.FolderType;
import net.bluemind.eas.endpoint.tests.helpers.TestMail;
import net.bluemind.eas.http.EasHeaders;
import net.bluemind.eas.http.wbxml.WbxmlHandlerBase;
import net.bluemind.eas.testhelper.mock.ResponseObject;
import net.bluemind.eas.utils.DOMUtils;
import net.bluemind.eas.wbxml.WBXMLTools;
import net.bluemind.hornetq.client.MQ;
import net.bluemind.hornetq.client.MQ.IMQConnectHandler;
import net.bluemind.imap.Flag;
import net.bluemind.imap.FlagsList;
import net.bluemind.imap.IMAPException;
import net.bluemind.imap.SearchQuery;
import net.bluemind.imap.StoreClient;

public class ItemOperationsEndpointTests extends AbstractEndpointTest {

	private int inboxServerId;
	private int trashServerId;
	private String lastFolderSyncKey;

	public void setUp() throws Exception {
		super.setUp();
		fetchInboxAndTrash();
		final CountDownLatch mqLatch = new CountDownLatch(1);
		MQ.init(new IMQConnectHandler() {

			@Override
			public void connected() {
				mqLatch.countDown();
			}
		});
		mqLatch.await(5, TimeUnit.SECONDS);
	}

	public void tearDown() throws Exception {
		super.tearDown();
	}

	@Override
	public WbxmlHandlerBase createEndpoint() {
		return new ItemOperationsEndpoint();
	}

	private void fetchInboxAndTrash() throws IOException {
		Document folderSync = runFolderSync("0");
		NodeList added = folderSync.getDocumentElement().getElementsByTagNameNS("FolderHierarchy", "Add");
		Integer inboxId = null;
		Integer trashId = null;
		for (int i = 0; i < added.getLength(); i++) {
			Element el = (Element) added.item(i);
			FolderType folderType = FolderType
					.getValue(Integer.parseInt(DOMUtils.getUniqueElement(el, "Type").getTextContent()));
			switch (folderType) {
			case DEFAULT_INBOX_FOLDER:
				inboxId = Integer.parseInt(DOMUtils.getUniqueElement(el, "ServerId").getTextContent());
				break;
			case DEFAULT_DELETED_ITEMS_FOLDERS:
				trashId = Integer.parseInt(DOMUtils.getUniqueElement(el, "ServerId").getTextContent());
				break;
			default:
				break;
			}
		}
		assertNotNull(inboxId);
		inboxServerId = inboxId;
		System.out.println("Inbox has serverId " + inboxServerId);
		assertNotNull(trashServerId);
		trashServerId = trashId;
		System.out.println("Trash has serverId " + trashServerId);
		assertTrue(inboxServerId > 0);
		assertTrue(trashServerId > 0);
	}

	private Document runFolderSync(String syncKey) throws IOException {
		FolderSyncEndpoint fse = new FolderSyncEndpoint();
		Document document = DOMUtils.createDoc("FolderHierarchy", "FolderSync");
		Element root = document.getDocumentElement();
		Element sk = document.createElementNS("FolderHierarchy", "SyncKey");
		sk.setTextContent(syncKey);
		root.appendChild(sk);
		ResponseObject folderSyncResponse = runEndpoint(fse, document);
		Buffer content = folderSyncResponse.content;
		Document folderSync = WBXMLTools.toXml(content.getBytes());
		assertNotNull(folderSync);
		lastFolderSyncKey = DOMUtils.getElementText(folderSync.getDocumentElement(), "SyncKey");
		assertNotNull(lastFolderSyncKey);
		System.out.println("*** Next folder sync key must be " + lastFolderSyncKey);
		return folderSync;
	}

	private TestMail addMail(String mailbox) throws Exception {
		FlagsList fl = new FlagsList();
		fl.add(Flag.SEEN);
		TestMail addedEml = appendEml(mailbox, "data/ItemOperations/with_attachments.eml", fl);
		return addedEml;
	}

	public void testItemOperationsMove() throws FactoryConfigurationError, Exception {
		TestMail added = addMail("INBOX");
		System.out.println("Added mail has uid " + added);
		Document moveReqXml = prepareDocument("move_req.xml", added.uid);
		assertNotNull(moveReqXml);
		ResponseObject resp = runEndpoint(moveReqXml);
		assertNotNull(resp);
		assertEquals("Move http status must be 200", 200, resp.getStatusCode());
		Buffer content = resp.content;
		Document response = WBXMLTools.toXml(content.getBytes());
		String status = DOMUtils.getUniqueElement(response.getDocumentElement(), "Status").getTextContent();
		assertEquals("1", status);
		String conversationId = DOMUtils.getUniqueElement(response.getDocumentElement(), "ConversationId")
				.getTextContent();
		assertEquals(inboxServerId + ":" + added.uid, conversationId);
	}

	private Document prepareDocument(String template, int mailUid)
			throws IOException, SAXException, ParserConfigurationException, FactoryConfigurationError {
		Document moveReqXml = null;
		try (InputStream in = stream("data/ItemOperations/" + template)) {
			String xmlString = new String(ByteStreams.toByteArray(in));
			xmlString = xmlString.replace("${inboxId}", Integer.toString(inboxServerId));
			xmlString = xmlString.replace("${trashId}", Integer.toString(trashServerId));
			xmlString = xmlString.replace("${mailUid}", Integer.toString(mailUid));
			moveReqXml = DOMUtils.parse(new ByteArrayInputStream(xmlString.getBytes()));
		}
		return moveReqXml;
	}

	public void testItemOperationsEmptyFolder()
			throws IMAPException, IOException, SAXException, ParserConfigurationException, FactoryConfigurationError {
		try (StoreClient sc = new StoreClient(vmHostname, 143, login, password)) {
			assertTrue(sc.login());
			TestMail added = addMail("Trash");
			assertTrue(sc.select("Trash"));
			Collection<Integer> content = sc.uidSearch(new SearchQuery());
			assertFalse("Trash folder should have at least one mail", content.isEmpty());
			System.out.println("Added mail has uid " + added);
			Document emptyTrash = prepareDocument("empty_trash.xml", added.uid);
			assertNotNull(emptyTrash);
			ResponseObject resp = runEndpoint(emptyTrash);
			assertNotNull(resp);
			assertEquals("Move http status must be 200", 200, resp.getStatusCode());
			assertTrue(sc.select("Trash"));
			content = sc.uidSearch(new SearchQuery());
			assertTrue("Trash folder is not empty", content.isEmpty());
		} catch (Exception e) {
			fail(e.getMessage());
		}
	}

	/**
	 * Tested feature is not implemented
	 * 
	 * @throws IMAPException
	 * @throws IOException
	 * @throws SAXException
	 * @throws ParserConfigurationException
	 * @throws FactoryConfigurationError
	 */
	public void testItemOperationsEmptyFolderAndSubfolders()
			throws IMAPException, IOException, SAXException, ParserConfigurationException, FactoryConfigurationError {
		try (StoreClient sc = new StoreClient(vmHostname, 143, login, password)) {
			assertTrue(sc.login());
			TestMail added = addMail("Trash");
			assertTrue(sc.select("Trash"));
			String name = "Trash/sub" + System.currentTimeMillis();
			sc.create(name);
			assertTrue(sc.select(name));
			runFolderSync(lastFolderSyncKey);
			TestMail inSubfolder = addMail(name);
			assertTrue(inSubfolder.uid > 0);
			Collection<Integer> content = sc.uidSearch(new SearchQuery());
			assertFalse("Trash folder should have at least one mail", content.isEmpty());
			System.out.println("Added mail has uid " + added);
			Document moveReqXml = prepareDocument("empty_trash_and_subfolders.xml", added.uid);
			assertNotNull(moveReqXml);
			ResponseObject resp = runEndpoint(moveReqXml);
			assertNotNull(resp);
			assertEquals("Move http status must be 200", 200, resp.getStatusCode());
			assertTrue(sc.select("Trash"));
			boolean selectSub = sc.select(name);
			assertFalse("Trash should not contain any subfolder after deep delete", selectSub);
			content = sc.uidSearch(new SearchQuery());
			assertTrue("Trash folder is not empty", content.isEmpty());
		} catch (Exception e) {
			fail(e.getMessage());
		}
	}

	public void testItemOperationsFetchInline() throws Exception {
		TestMail added = addMail("INBOX");
		System.out.println("Added mail has uid " + added);
		Document moveReqXml = prepareDocument("fetch_uid_inline.xml", added.uid);
		assertNotNull(moveReqXml);
		ResponseObject resp = runEndpoint(moveReqXml);
		assertNotNull(resp);
		assertEquals("Fetch inline http status must be 200", 200, resp.getStatusCode());
		Document fetchInline = WBXMLTools.toXml(resp.content.getBytes());
		assertNotNull(fetchInline);
		NodeList dataNodes = fetchInline.getElementsByTagNameNS("AirSyncBase", "Data");
		assertNotNull(dataNodes);
		assertEquals("1 AirSyncBase:Data node must be in the DOM", 1, dataNodes.getLength());
		NodeList toNode = fetchInline.getElementsByTagNameNS("Email", "To");
		assertEquals(1, toNode.getLength());
	}

	public void testItemOperationsFetchMultipart() throws FactoryConfigurationError, Exception {
		TestMail added = addMail("INBOX");
		System.out.println("Added mail has uid " + added);
		Document moveReqXml = prepareDocument("fetch_uid_multipart.xml", added.uid);
		assertNotNull(moveReqXml);
		ResponseObject resp = runEndpoint(endpoint, moveReqXml,
				ImmutableMap.of(EasHeaders.Client.ACCEPT_MULTIPART, "T"));
		assertNotNull(resp);
		assertEquals("Fetch multipart http status must be 200 (" + resp.getStatusMessage() + ")", 200,
				resp.getStatusCode());
		Buffer multipart = resp.content;
		assertNotNull(multipart);
		ByteBuf buf = multipart.getByteBuf();
		String hexDump = ByteBufUtil.hexDump(buf);
		System.out.println("code: " + hexDump);
		buf = buf.order(ByteOrder.LITTLE_ENDIAN);
		int partCount = buf.readInt();
		System.out.println("partCount: " + partCount);
		List<Integer> length = new LinkedList<>();
		for (int i = 0; i < partCount; i++) {
			int offset = buf.readInt();
			int len = buf.readInt();
			length.add(len);
			System.out.println("Found part meta with offset " + offset + " and length: " + len);
		}
		List<byte[]> reRead = new ArrayList<>(partCount);
		for (int l : length) {
			byte[] dest = new byte[l];
			buf.readBytes(dest);
			reRead.add(dest);
		}
		Document doc = WBXMLTools.toXml(reRead.get(0));
		assertNotNull(doc);
		String eml = new String(reRead.get(1));
		assertNotNull(eml);
		System.out.println(eml);
	}

	public void testItemOperationsFetchFileReferenceMultipart() throws FactoryConfigurationError, Exception {
		TestMail added = addMail("INBOX");
		System.out.println("Added mail has uid " + added.uid);
		Document moveReqXml = prepareDocument("fetch_uid_fileref.xml", added.uid);
		assertNotNull(moveReqXml);
		ResponseObject resp = runEndpoint(endpoint, moveReqXml,
				ImmutableMap.of(EasHeaders.Client.ACCEPT_MULTIPART, "T"));
		assertNotNull(resp);
		assertEquals("Fetch multipart http status must be 200 (" + resp.getStatusMessage() + ")", 200,
				resp.getStatusCode());
		Buffer multipart = resp.content;
		assertNotNull(multipart);
		ByteBuf buf = multipart.getByteBuf().order(ByteOrder.LITTLE_ENDIAN);
		String hex = ByteBufUtil.hexDump(buf);
		System.out.println("hexDump: " + hex);
		int partCount = buf.readInt();
		System.out.println("partCount: " + partCount);
		List<Integer> length = new LinkedList<>();
		for (int i = 0; i < partCount; i++) {
			int offset = buf.readInt();
			int len = buf.readInt();
			length.add(len);
			System.out.println("Found part meta with offset " + offset + " and length: " + len);
		}
		List<byte[]> reRead = new ArrayList<>(partCount);
		for (int l : length) {
			byte[] dest = new byte[l];
			buf.readBytes(dest);
			reRead.add(dest);
		}
		Document doc = WBXMLTools.toXml(reRead.get(0));
		assertNotNull(doc);
		checkJPEG(reRead.get(1));
	}

	private void checkJPEG(byte[] bytes) throws IOException {
		System.setProperty("java.awt.headless", "true");
		BufferedImage theJpeg = ImageIO.read(new ByteArrayInputStream(bytes));
		assertNotNull(theJpeg);
		System.out.println("theJpeg: " + theJpeg);
	}

	public void testItemOperationsFetchFileReferenceInline() throws Exception {
		TestMail added = addMail("INBOX");
		System.out.println("Added mail has uid " + added.uid);
		Document moveReqXml = prepareDocument("fetch_uid_fileref.xml", added.uid);
		assertNotNull(moveReqXml);
		ResponseObject resp = runEndpoint(endpoint, moveReqXml);
		assertNotNull(resp);
		assertEquals("Fetch multipart http status must be 200 (" + resp.getStatusMessage() + ")", 200,
				resp.getStatusCode());
		Buffer multipart = resp.content;
		assertNotNull(multipart);
		Document doc = WBXMLTools.toXml(multipart.getBytes());
		assertNotNull(doc);
		NodeList nodes = doc.getElementsByTagNameNS(NamespaceMapping.ItemOperations.namespace(), "Data");
		assertEquals(1, nodes.getLength());
		Element dataNode = (Element) nodes.item(0);
		byte[] imageBytes = Base64.getDecoder().decode(dataNode.getTextContent());
		checkJPEG(imageBytes);
	}

	public void testMultipleFetchFileReferenceInline() throws Exception {
		TestMail added = addMail("INBOX");
		System.out.println("Added mail has uid " + added.uid);
		Document moveReqXml = prepareDocument("fetch_uid_fileref_multiple.xml", added.uid);
		assertNotNull(moveReqXml);
		ResponseObject resp = runEndpoint(endpoint, moveReqXml);
		assertNotNull(resp);
		assertEquals("Fetch multipart http status must be 200 (" + resp.getStatusMessage() + ")", 200,
				resp.getStatusCode());
		Buffer multipart = resp.content;
		assertNotNull(multipart);
		Document doc = WBXMLTools.toXml(multipart.getBytes());
		assertNotNull(doc);
		NodeList dataNodes = doc.getElementsByTagNameNS(NamespaceMapping.ItemOperations.namespace(), "Data");
		assertEquals(15, dataNodes.getLength());
		NodeList typeNodes = doc.getElementsByTagNameNS(NamespaceMapping.AirSyncBase.namespace(), "ContentType");
		assertEquals(15, typeNodes.getLength());
		for (int i = 0; i < dataNodes.getLength(); i++) {
			Element dataNode = (Element) dataNodes.item(i);
			checkJPEG(Base64.getDecoder().decode(dataNode.getTextContent()));
			dataNode.setTextContent("[base64 of jpeg file]");
		}
		DOMUtils.logDom(doc);
	}

	public void testMultipleFetchBodyInline() throws Exception {
		TestMail added = addMail("INBOX");
		System.out.println("Added mail has uid " + added.uid);
		Document moveReqXml = prepareDocument("fetch_multiple_eml_inline.xml", added.uid);
		assertNotNull(moveReqXml);
		ResponseObject resp = runEndpoint(endpoint, moveReqXml);
		assertNotNull(resp);
		assertEquals("Fetch multipart http status must be 200 (" + resp.getStatusMessage() + ")", 200,
				resp.getStatusCode());
		Buffer multipart = resp.content;
		assertNotNull(multipart);
		Document doc = WBXMLTools.toXml(multipart.getBytes());
		assertNotNull(doc);
		NodeList dataNodes = doc.getElementsByTagNameNS(NamespaceMapping.AirSyncBase.namespace(), "Data");
		assertEquals(5, dataNodes.getLength());
		for (int i = 0; i < dataNodes.getLength(); i++) {
			Element dataNode = (Element) dataNodes.item(i);
			assertTrue(dataNode.getTextContent().contains("--Apple-Mail=_2D981F70-61F7-493C-8494-D563E07B8DA7"));
		}
	}

	public void testItemOperationsFetchFileReference() throws Exception {
		String mbox = null;
		try (StoreClient sc = new StoreClient(vmHostname, 143, login, password)) {
			assertTrue(sc.login());
			mbox = UUID.randomUUID().toString();
			sc.create(mbox);
		} catch (Exception e) {
			fail(e.getMessage());
		}

		Document folderSync = runFolderSync("0");
		NodeList added = folderSync.getDocumentElement().getElementsByTagNameNS("FolderHierarchy", "Add");
		Integer mboxId = null;
		for (int i = 0; i < added.getLength(); i++) {
			Element el = (Element) added.item(i);
			FolderType folderType = FolderType
					.getValue(Integer.parseInt(DOMUtils.getUniqueElement(el, "Type").getTextContent()));
			switch (folderType) {
			case USER_CREATED_EMAIL_FOLDER:
				if (mbox.equals(DOMUtils.getUniqueElement(el, "DisplayName").getTextContent())) {
					mboxId = Integer.parseInt(DOMUtils.getUniqueElement(el, "ServerId").getTextContent());
				}
				break;
			default:
				break;
			}
		}
		assertNotNull(mboxId);

		Document sync = DOMUtils.createDoc("AirSync", "Sync");
		Element root = sync.getDocumentElement();
		Element collections = DOMUtils.createElement(root, "Collections");
		Element collection = DOMUtils.createElement(collections, "Collection");
		DOMUtils.createElementAndText(collection, "SyncKey", "0");
		DOMUtils.createElementAndText(collection, "CollectionId", Integer.toString(mboxId));

		ResponseObject response = runEndpoint(new SyncEndpoint(), sync);
		assertEquals(200, response.getStatusCode());
		Buffer content = response.content;
		Document resp = WBXMLTools.toXml(content.getBytes());
		String syncKey = DOMUtils.getUniqueElement(resp.getDocumentElement(), "SyncKey").getTextContent();
		assertNotNull(syncKey);

		// FULL SYNC first
		sync = DOMUtils.createDoc("AirSync", "Sync");
		root = sync.getDocumentElement();
		collections = DOMUtils.createElement(root, "Collections");
		collection = DOMUtils.createElement(collections, "Collection");
		DOMUtils.createElementAndText(collection, "SyncKey", syncKey);
		DOMUtils.createElementAndText(collection, "CollectionId", Integer.toString(mboxId));
		DOMUtils.createElement(collection, "GetChanges");
		DOMUtils.createElementAndText(collection, "WindowSize", "50");
		Element options = DOMUtils.createElement(collection, "Options");
		DOMUtils.createElementAndText(options, "FilterType", "3");
		Element bodyPreference = DOMUtils.createElement(options, "AirSyncBase:BodyPreference");
		DOMUtils.createElementAndText(bodyPreference, "Type", "2");
		DOMUtils.createElementAndText(bodyPreference, "TruncationSize", "200000");
		response = runEndpoint(new SyncEndpoint(), sync);
		assertEquals(200, response.getStatusCode());
		content = response.content;
		resp = WBXMLTools.toXml(content.getBytes());
		syncKey = DOMUtils.getUniqueElement(resp.getDocumentElement(), "SyncKey").getTextContent();
		assertNotNull(syncKey);

		FlagsList fl = new FlagsList();
		fl.add(Flag.SEEN);
		appendEml(mbox, "data/ItemOperations/attachment.eml", fl);

		// Sync new email
		sync = DOMUtils.createDoc("AirSync", "Sync");
		root = sync.getDocumentElement();
		collections = DOMUtils.createElement(root, "Collections");
		collection = DOMUtils.createElement(collections, "Collection");
		DOMUtils.createElementAndText(collection, "SyncKey", syncKey);
		DOMUtils.createElementAndText(collection, "CollectionId", Integer.toString(mboxId));
		DOMUtils.createElement(collection, "GetChanges");
		DOMUtils.createElementAndText(collection, "WindowSize", "50");
		options = DOMUtils.createElement(collection, "Options");
		DOMUtils.createElementAndText(options, "FilterType", "3");
		bodyPreference = DOMUtils.createElement(options, "AirSyncBase:BodyPreference");
		DOMUtils.createElementAndText(bodyPreference, "Type", "2");
		DOMUtils.createElementAndText(bodyPreference, "TruncationSize", "200000");
		response = runEndpoint(new SyncEndpoint(), sync);
		assertEquals(200, response.getStatusCode());
		content = response.content;
		resp = WBXMLTools.toXml(content.getBytes());
		syncKey = DOMUtils.getUniqueElement(resp.getDocumentElement(), "SyncKey").getTextContent();
		assertNotNull(syncKey);
		syncKey = DOMUtils.getUniqueElement(resp.getDocumentElement(), "SyncKey").getTextContent();
		Element commands = DOMUtils.getUniqueElement(resp.getDocumentElement(), "Commands");
		assertNotNull(commands);
		assertEquals(1, commands.getChildNodes().getLength());
		Element cmd = (Element) commands.getChildNodes().item(0);
		assertEquals("Add", cmd.getNodeName());
		String serverId = DOMUtils.getUniqueElement(resp.getDocumentElement(), "ServerId").getTextContent();
		assertNotNull(serverId);

		Element appData = DOMUtils.getUniqueElement(resp.getDocumentElement(), "ApplicationData");
		assertNotNull(appData);

		Element attachments = DOMUtils.getUniqueElement(appData, "Attachments");
		assertNotNull(attachments);
		assertEquals(1, attachments.getChildNodes().getLength());
		Element attachment = (Element) attachments.getChildNodes().item(0);
		String fileRef = DOMUtils.getUniqueElement(attachment, "FileReference").getTextContent();
		assertNotNull(fileRef);

		// Fetch fileReference request
		// <ItemOperations xmlns="ItemOperations:">
		// <Fetch>
		// <Store>Mailbox</Store>
		// <FileReference xmlns="AirSyncBase:">8%3a6%3a0</FileReference>
		// </Fetch>
		// </ItemOperations>

		Document itemOp = DOMUtils.createDoc("ItemOperations", "ItemOperations");
		Element fetch = DOMUtils.createElement(itemOp.getDocumentElement(), "Fetch");
		DOMUtils.createElementAndText(fetch, "Store", "Mailbox");
		DOMUtils.createElementAndText(fetch, "AirSyncBase:FileReference", fileRef);

		response = runEndpoint(itemOp);
		assertEquals(200, response.getStatusCode());
		content = response.content;

		// response (ex2k10)
		// <?xml version="1.0" encoding="utf-8" ?>
		// <ItemOperations xmlns="ItemOperations:">
		// <Status>1</Status>
		// <Response>
		// <Fetch>
		// <Status>1</Status>
		// <FileReference xmlns="AirSyncBase:">8%3a6%3a0</FileReference>
		// <Properties>
		// <ContentType xmlns="AirSyncBase:">image/gif</ContentType>
		// <Data bytes="12"/>
		// </Properties>
		// </Fetch>
		// </Response>
		// </ItemOperations>

		resp = WBXMLTools.toXml(content.getBytes());
		String status = DOMUtils.getUniqueElement(resp.getDocumentElement(), "Status").getTextContent();
		assertNotNull("1", status);
		Element r = DOMUtils.getUniqueElement(resp.getDocumentElement(), "Response");
		assertNotNull(r);

		Element f = DOMUtils.getUniqueElement(r, "Fetch");
		assertNotNull(f);

		status = DOMUtils.getUniqueElement(f, "Status").getTextContent();
		assertNotNull("1", status);

		String fileReference = DOMUtils.getUniqueElement(f, "FileReference").getTextContent();
		assertNotNull(fileRef, fileReference);

		Element p = DOMUtils.getUniqueElement(f, "Properties");
		assertNotNull(p);

		Element contentType = DOMUtils.getUniqueElement(p, "ContentType");
		assertNotNull(contentType);
		assertEquals("application/octet-stream", contentType.getTextContent());

		Element range = DOMUtils.getUniqueElement(p, "Range");
		assertNotNull(range);

		Element total = DOMUtils.getUniqueElement(p, "Total");
		assertNotNull(total);

		Element data = DOMUtils.getUniqueElement(p, "Data");
		assertNotNull(data);

	}

	public void testItemOperationsFetchLongId() throws Exception {
		TestMail added = addMail("INBOX");
		System.err.println("Added mail has uid " + added);
		Document doc = prepareDocument("fetch_longId.xml", added.uid);
		ResponseObject resp = runEndpoint(doc);
		assertNotNull(resp);
		assertEquals("Fetch inline http status must be 200", 200, resp.getStatusCode());
		Document fetchInline = WBXMLTools.toXml(resp.content.getBytes());
		assertNotNull(fetchInline);
		NodeList dataNodes = fetchInline.getElementsByTagNameNS("AirSyncBase", "Data");
		assertNotNull(dataNodes);
		assertEquals("1 AirSyncBase:Data node must be in the DOM", 1, dataNodes.getLength());
	}
}
