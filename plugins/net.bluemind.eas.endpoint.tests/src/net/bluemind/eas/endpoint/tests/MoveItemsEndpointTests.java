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
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Date;

import org.apache.james.mime4j.dom.Header;
import org.apache.james.mime4j.dom.Message;
import org.apache.james.mime4j.dom.MessageBuilder;
import org.apache.james.mime4j.dom.MessageServiceFactory;
import org.apache.james.mime4j.dom.MessageWriter;
import org.apache.james.mime4j.dom.TextBody;
import org.apache.james.mime4j.dom.address.Mailbox;
import org.apache.james.mime4j.field.Fields;
import org.apache.james.mime4j.message.BasicBodyFactory;
import org.apache.james.mime4j.util.MimeUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import io.vertx.core.buffer.Buffer;
import net.bluemind.eas.command.folder.crud.FolderCreateEndpoint;
import net.bluemind.eas.command.folder.sync.FolderSyncEndpoint;
import net.bluemind.eas.command.moveitems.MoveItemsEndpoint;
import net.bluemind.eas.dto.moveitems.MoveItemsResponse;
import net.bluemind.eas.http.wbxml.WbxmlHandlerBase;
import net.bluemind.eas.testhelper.mock.ResponseObject;
import net.bluemind.eas.utils.DOMUtils;
import net.bluemind.eas.wbxml.WBXMLTools;
import net.bluemind.imap.FlagsList;
import net.bluemind.imap.StoreClient;

public class MoveItemsEndpointTests extends AbstractEndpointTest {

	@Override
	public WbxmlHandlerBase createEndpoint() {
		return new MoveItemsEndpoint();
	}

	private String initFolder() throws IOException {
		Document document = DOMUtils.createDoc("FolderHierarchy", "FolderSync");
		Element root = document.getDocumentElement();
		DOMUtils.createElementAndText(root, "SyncKey", "0");
		ResponseObject response = runEndpoint(new FolderSyncEndpoint(), document);
		assertEquals("Status should be 200", 200, response.getStatusCode());

		Buffer content = response.content;
		Document d = WBXMLTools.toXml(content.getBytes());
		Element sk = DOMUtils.getUniqueElement(d.getDocumentElement(), "SyncKey");
		assertNotNull(sk.getTextContent());

		return sk.getTextContent();

	}

	public void testMoveItems() throws Exception {
		String syncKey = initFolder();
		String srcFolderName = "SRC FOLDER " + System.currentTimeMillis();

		String folderIdSrc = createMailFolder(syncKey, srcFolderName);
		String folderIdDst = createMailFolder(syncKey, "DST FOLDER " + System.currentTimeMillis());

		int mailId = addOneEmail(srcFolderName, "gg");
		addOneEmail(srcFolderName, "gg");

		Document document = DOMUtils.createDoc("Move", "MoveItems");
		Element root = document.getDocumentElement();
		String movedId = String.format("%s:%d", folderIdSrc, mailId);
		appendMove(root, movedId, folderIdSrc, folderIdDst);
		ResponseObject response = runEndpoint(document);
		assertEquals("Status should be 200", 200, response.getStatusCode());

		Buffer content = response.content;
		Document d = WBXMLTools.toXml(content.getBytes());
		Element oneMoveResp = DOMUtils.getUniqueElement(d.getDocumentElement(), "Response");
		assertNotNull(oneMoveResp);
		Element src = DOMUtils.getUniqueElement(oneMoveResp, "SrcMsgId");
		assertNotNull(src);
		assertEquals(movedId, src.getTextContent());
		Element status = DOMUtils.getUniqueElement(oneMoveResp, "Status");
		assertNotNull(status);

		// Sucess
		assertEquals(MoveItemsResponse.Response.Status.Success.xmlValue(), status.getTextContent());
		Element dst = DOMUtils.getUniqueElement(oneMoveResp, "DstMsgId");
		assertNotNull(dst);

	}

	public void testMoveItemsWithSomeErrors() throws Exception {
		String syncKey = initFolder();
		String srcFolderName = "ZZtestCreateFolder" + System.currentTimeMillis();
		String folderIdSrc = createMailFolder(syncKey, srcFolderName);
		String folderIdDst = createMailFolder(syncKey, "testCreateFolder" + System.currentTimeMillis());
		int mailId = addOneEmail(srcFolderName, "gg");
		int mailId2 = addOneEmail(srcFolderName, "gg");

		Document document = DOMUtils.createDoc("Move", "MoveItems");
		Element root = document.getDocumentElement();
		appendMove(root, String.format("%s:%d", folderIdSrc, mailId), folderIdSrc, folderIdDst);

		// dst folder doesnt exists
		appendMove(root, String.format("%s:%d", folderIdSrc, mailId2), folderIdSrc, "666667");

		// src folder doestn exists
		appendMove(root, String.format("%s:%d", "666667", mailId), "666667", folderIdDst);

		// msg doesnt exists
		appendMove(root, String.format("%s:%d", folderIdSrc, 666), folderIdSrc, folderIdDst);

		ResponseObject response = runEndpoint(document);
		assertEquals("Status should be 200", 200, response.getStatusCode());

		Buffer content = response.content;
		Document d = WBXMLTools.toXml(content.getBytes());
		NodeList childs = d.getDocumentElement().getChildNodes();
		assertEquals(4, childs.getLength());

		assertEquals(Node.ELEMENT_NODE, childs.item(0).getNodeType());
		Element e = (Element) childs.item(0);
		Element status = DOMUtils.getUniqueElement(e, "Status");
		assertNotNull(status);
		// Sucess
		assertEquals(MoveItemsResponse.Response.Status.Success.xmlValue(), status.getTextContent());

		assertEquals(Node.ELEMENT_NODE, childs.item(1).getNodeType());
		e = (Element) childs.item(1);
		status = DOMUtils.getUniqueElement(e, "Status");
		assertNotNull(status);
		// dst folder does not exists
		assertEquals(MoveItemsResponse.Response.Status.InvalidDestinationCollectionId.xmlValue(),
				status.getTextContent());

		assertEquals(Node.ELEMENT_NODE, childs.item(2).getNodeType());
		e = (Element) childs.item(2);
		status = DOMUtils.getUniqueElement(e, "Status");
		assertNotNull(status);
		// src folder does not exists
		assertEquals(MoveItemsResponse.Response.Status.InvalidSourceCollectionId.xmlValue(), status.getTextContent());

		assertEquals(Node.ELEMENT_NODE, childs.item(3).getNodeType());
		e = (Element) childs.item(3);
		status = DOMUtils.getUniqueElement(e, "Status");
		assertNotNull(status);
	}

	private void appendMove(Element root, String msgId, String folderIdSrc, String folderIdDst) {
		Element move = DOMUtils.createElement(root, "Move");
		DOMUtils.createElementAndText(move, "SrcMsgId", msgId);
		DOMUtils.createElementAndText(move, "SrcFldId", folderIdSrc);
		DOMUtils.createElementAndText(move, "DstFldId", folderIdDst);

	}

	private String createMailFolder(String syncKey, String folderDisplayName) throws IOException {
		Document document = DOMUtils.createDoc("FolderHierarchy", "FolderCreate");
		Element root = document.getDocumentElement();
		DOMUtils.createElementAndText(root, "SyncKey", syncKey);
		DOMUtils.createElementAndText(root, "ParentId", "0");
		DOMUtils.createElementAndText(root, "DisplayName", folderDisplayName);
		DOMUtils.createElementAndText(root, "Type", "12"); // USER_CREATED_EMAIL_FOLDER
		ResponseObject response = runEndpoint(new FolderCreateEndpoint(), document);
		assertEquals("Status should be 200", 200, response.getStatusCode());

		Buffer content = response.content;
		Document d = WBXMLTools.toXml(content.getBytes());
		Element serverId = DOMUtils.getUniqueElement(d.getDocumentElement(), "ServerId");
		assertNotNull(serverId.getTextContent());

		return serverId.getTextContent();
	}

	private int addOneEmail(String folder, String subject) throws Exception {
		try (StoreClient sc = new StoreClient(vmHostname, 1143, login, password)) {
			assertTrue(sc.login());
			sc.select(folder);

			ByteArrayOutputStream out = new ByteArrayOutputStream();
			MessageServiceFactory msf = MessageServiceFactory.newInstance();
			Message mm = getRandomMessage(msf, subject);
			MessageWriter writer = msf.newMessageWriter();
			writer.writeMessage(mm, out);
			ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());

			int id = sc.append(folder, in, new FlagsList());
			assertTrue(id > 0);
			return id;
		} catch (Exception e) {
			fail(e.getMessage());
		}
		return 0;
	}

	private Message getRandomMessage(MessageServiceFactory msf, String subject) throws UnsupportedEncodingException {
		MessageBuilder builder = msf.newMessageBuilder();
		Message mm = builder.newMessage();
		BasicBodyFactory bbf = new BasicBodyFactory();
		Header h = builder.newHeader();
		h.setField(Fields.contentType("text/html; charset=UTF-8"));
		h.setField(Fields.contentTransferEncoding(MimeUtil.ENC_8BIT));
		mm.setHeader(h);
		TextBody text = bbf.textBody("<html><body>osef</body></html>", "UTF-8");
		mm.setBody(text);
		Date now = new Date();
		mm.setSubject(subject);
		mm.setDate(now);

		Mailbox mbox = new Mailbox("John Bang", "john.bang", "local.lan");
		mm.setFrom(mbox);

		Mailbox to = new Mailbox(owner.displayName, owner.value.login, domainUid);
		mm.setTo(to);

		return mm;
	}
}
