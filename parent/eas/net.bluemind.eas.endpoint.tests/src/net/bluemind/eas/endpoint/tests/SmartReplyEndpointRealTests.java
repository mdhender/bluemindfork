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

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.apache.james.mime4j.dom.Body;
import org.apache.james.mime4j.dom.Message;
import org.apache.james.mime4j.dom.Multipart;
import org.apache.james.mime4j.dom.TextBody;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.google.common.base.Charsets;
import com.google.common.io.ByteStreams;
import com.google.common.io.CharStreams;

import io.vertx.core.buffer.Buffer;
import net.bluemind.eas.command.folder.sync.FolderSyncEndpoint;
import net.bluemind.eas.command.mail.sendmail.SendMailEndpoint;
import net.bluemind.eas.command.mail.smartreply.SmartReplyEndpoint;
import net.bluemind.eas.http.wbxml.WbxmlHandlerBase;
import net.bluemind.eas.testhelper.mock.ResponseObject;
import net.bluemind.eas.utils.DOMUtils;
import net.bluemind.eas.wbxml.WBXMLTools;
import net.bluemind.imap.IMAPByteSource;
import net.bluemind.imap.SearchQuery;
import net.bluemind.imap.StoreClient;
import net.bluemind.mime4j.common.AddressableEntity;
import net.bluemind.mime4j.common.Mime4JHelper;

public class SmartReplyEndpointRealTests extends AbstractEndpointTest {

	public void testWP8SmartReply() throws Exception {
		Integer inbox = fetchInboxId();
		String subject = UUID.randomUUID().toString();
		Integer uid = sendMail(subject);

		Document document = DOMUtils.createDoc("ComposeMail", "SmartReply");
		Element root = document.getDocumentElement();

		String clientId = UUID.randomUUID().toString();

		DOMUtils.createElementAndText(root, "ClientId", clientId);
		DOMUtils.createElement(root, "SaveInSentItems");

		Element source = DOMUtils.createElement(root, "Source");
		DOMUtils.createElementAndText(source, "FolderId", inbox.toString());
		DOMUtils.createElementAndText(source, "ItemId", inbox.toString() + ":" + uid.toString());

		StringBuilder sb = new StringBuilder();
		sb.append("MIME-Version: 1.0\n");
		sb.append("To: ").append(owner.value.defaultEmail().address).append("\n");
		sb.append("From: <").append(owner.value.defaultEmail().address).append("\n");
		sb.append("Subject: RE: ").append(subject).append("\n");
		sb.append("Date: Mon, 4 Jul 2016 13:48:37 +0200\n");
		sb.append("content-class: urn:content-classes:message\n");
		sb.append("Content-Transfer-Encoding: quoted-printable\n");
		sb.append("Content-Type: text/html; charset=\"utf-8\"\n");
		sb.append("\n");
		sb.append("<html><head><meta http-equiv=3D\"Content-Type\" content=3D\"text/html; charset=\n");
		sb.append("=3Dutf-8\"></head><body><div><div style=3D\"font-family: Calibri,sans-serif; =\n");
		sb.append("font-size: 11pt;\">Salut<br><br>Envoy=C3=A9 =C3=A0 partir de mon Windows Pho=\n");
		sb.append("ne</div></div><div dir=3D\"ltr\"><hr><span style=3D\"font-family: Calibri,sans=\n");
		sb.append("-serif; font-size: 11pt; font-weight: bold;\">De : </span><span style=3D\"fon=\n");
		sb.append("t-family: Calibri,sans-serif; font-size: 11pt;\"><a href=3D\"mailto:david@bm.=\n");
		sb.append("lan\">david phan</a></span><br><span style=3D\"font-family: Calibri,sans-seri=\n");
		sb.append("f; font-size: 11pt; font-weight: bold;\">Envoy=C3=A9 : </span><span style=3D=\n");
		sb.append("\"font-family: Calibri,sans-serif; font-size: 11pt;\">=E2=80=8E01/=E2=80=8E07=\n");
		sb.append("/=E2=80=8E2016 11:01</span><br><span style=3D\"font-family: Calibri,sans-ser=\n");
		sb.append("if; font-size: 11pt; font-weight: bold;\">=C3=80&nbsp;: </span><span style=\n");
		sb.append("=3D\"font-family: Calibri,sans-serif; font-size: 11pt;\"><a href=3D\"mailto:da=\n");
		sb.append("vid@bm.lan\">david phan</a></span><br><span style=3D\"font-family: Calibri,sa=\n");
		sb.append("ns-serif; font-size: 11pt; font-weight: bold;\">Objet : </span><span style=\n");
		sb.append("=3D\"font-family: Calibri,sans-serif; font-size: 11pt;\">SmartStuff</span><br=\n");
		sb.append("><br></div></body></html>=\n");

		DOMUtils.createElementAndText(root, "Mime",
				java.util.Base64.getEncoder().encodeToString(sb.toString().getBytes()));

		ResponseObject response = runEndpoint(document);
		assertEquals("Status should be 200", 200, response.getStatusCode());
		// success = empty response
		assertEquals(0, response.content.getBytes().length);

		Thread.sleep(1000);

		// Check check
		StoreClient sc = new StoreClient(vmHostname, 1143, login, password);
		assertTrue(sc.login());
		sc.select("INBOX");
		SearchQuery sq = new SearchQuery();
		sq.setSubject("Re: " + subject);
		Collection<Integer> res = sc.uidSearch(sq);
		assertEquals(1, res.size());
		Integer repUid = (Integer) res.iterator().next();

		IMAPByteSource mailContent = sc.uidFetchMessage(repUid);
		try (Message msg = Mime4JHelper.parse(mailContent.source().openStream())) {
			sc.logout();
			sc.close();

			Body body = msg.getBody();
			assertTrue(body instanceof Multipart);
			Multipart mparts = (Multipart) body;
			List<AddressableEntity> parts = Mime4JHelper.expandTree(mparts.getBodyParts());
			assertEquals(1, parts.size());
			AddressableEntity e = parts.get(0);
			assertEquals("text/html", e.getMimeType());
			TextBody pb = (TextBody) e.getBody();
			InputStream in = pb.getInputStream();
			String replyContent = new String(ByteStreams.toByteArray(in), pb.getMimeCharset()).trim();
			System.err.println(replyContent);
		}
	}

	public void testAndroidSmartReply() throws Exception {
		Integer inbox = fetchInboxId();
		String subject = UUID.randomUUID().toString();
		Integer uid = sendMail(subject);

		Document document = DOMUtils.createDoc("ComposeMail", "SmartReply");
		Element root = document.getDocumentElement();

		String clientId = UUID.randomUUID().toString();

		DOMUtils.createElementAndText(root, "ClientId", clientId);
		DOMUtils.createElement(root, "SaveInSentItems");

		Element source = DOMUtils.createElement(root, "Source");
		DOMUtils.createElementAndText(source, "FolderId", inbox.toString());
		DOMUtils.createElementAndText(source, "ItemId", inbox.toString() + ":" + uid.toString());

		StringBuilder sb = new StringBuilder();
		sb.append("Date: Fri, 01 Jul 2016 16:11:07 +0200\n");
		sb.append("Subject: Re: ").append(subject).append("\n");
		sb.append("Message-ID: <").append(clientId).append("@email.android.com>\n");
		sb.append("X-Android-Message-ID: <").append(clientId).append("@email.android.com>\n");
		sb.append("From: ").append(owner.value.defaultEmail().address).append("\n");
		sb.append("To: ").append(owner.value.defaultEmail().address).append("\n");
		sb.append("Importance: Normal\n");
		sb.append("X-Priority: 3\n");
		sb.append("X-MSMail-Priority: Normal\n");
		sb.append("MIME-Version: 1.0\n");
		sb.append("Content-Type: text/html; charset=utf-8\n");
		sb.append("Content-Transfer-Encoding: base64\n");
		sb.append("\n");
		sb.append("PHAgZGlyPSJsdHIiPlNtYXJ0UmVwbHk8L3A+CjxkaXYgY2xhc3M9ImdtYWlsX3F1b3RlIj5PbiBK\n");
		sb.append("dWwgMSwgMjAxNiAxNjowOCwgRGF2aWQgUGhhbiAmbHQ7ZGF2aWQucGhhbkBibHVlLW1pbmQubmV0\n");
		sb.append("Jmd0OyB3cm90ZTo8YnIgdHlwZT0nYXR0cmlidXRpb24nPjxibG9ja3F1b3RlIGNsYXNzPSJxdW90\n");
		sb.append("ZSIgc3R5bGU9Im1hcmdpbjowIDAgMCAuOGV4O2JvcmRlci1sZWZ0OjFweCAjY2NjIHNvbGlkO3Bh\n");
		sb.append("ZGRpbmctbGVmdDoxZXgiPjxkaXY+DTxiciAvPg08YnIgLz4tLSANPGJyIC8+RGF2aWQgUGhhbjwv\n");
		sb.append("ZGl2PjwvYmxvY2txdW90ZT48L2Rpdj4=");

		DOMUtils.createElementAndText(root, "Mime",
				java.util.Base64.getEncoder().encodeToString(sb.toString().getBytes()));

		ResponseObject response = runEndpoint(document);
		assertEquals("Status should be 200", 200, response.getStatusCode());
		// success = empty response
		assertEquals(0, response.content.getBytes().length);

		Thread.sleep(1000);

		// Check check
		StoreClient sc = new StoreClient(vmHostname, 1143, login, password);
		assertTrue(sc.login());
		sc.select("INBOX");
		SearchQuery sq = new SearchQuery();
		sq.setSubject("Re: " + subject);
		Collection<Integer> res = sc.uidSearch(sq);
		assertEquals(1, res.size());
		Integer repUid = (Integer) res.iterator().next();

		IMAPByteSource mailContent = sc.uidFetchMessage(repUid);
		try (Message msg = Mime4JHelper.parse(mailContent.source().openStream())) {
			sc.logout();
			sc.close();

			Body body = msg.getBody();
			assertTrue(body instanceof Multipart);
			Multipart mparts = (Multipart) body;
			List<AddressableEntity> parts = Mime4JHelper.expandTree(mparts.getBodyParts());
			assertEquals(1, parts.size());
			AddressableEntity e = parts.get(0);
			assertEquals("text/html", e.getMimeType());
			TextBody pb = (TextBody) e.getBody();
			InputStream in = pb.getInputStream();
			String replyContent = new String(ByteStreams.toByteArray(in), pb.getMimeCharset()).trim();
			System.err.println(replyContent);
		}
	}

	private Integer fetchInboxId() throws IOException {
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
		Integer inboxId = null;
		for (int i = 0; i < added.getLength(); i++) {
			Element el = (Element) added.item(i);
			String folderType = DOMUtils.getUniqueElement(el, "Type").getTextContent();
			switch (folderType) {
			case "2":
				inboxId = Integer.parseInt(DOMUtils.getUniqueElement(el, "ServerId").getTextContent());
				break;
			}
		}
		assertNotNull(inboxId);
		return inboxId;
	}

	private Integer sendMail(String subject) throws Exception {
		Document document = DOMUtils.createDoc("ComposeMail", "SendMail");
		Element root = document.getDocumentElement();

		String clientId = UUID.randomUUID().toString();

		DOMUtils.createElementAndText(root, "ClientId", clientId);
		DOMUtils.createElement(root, "SaveInSentItems");

		InputStream template = AbstractEndpointTest.class.getClassLoader()
				.getResourceAsStream("data/SmartReply/template.eml");
		String email = CharStreams.toString(new InputStreamReader(template, Charsets.UTF_8));
		email = email.replace("##FROM##", owner.value.defaultEmail().address);
		email = email.replace("##TO##", owner.value.defaultEmail().address);
		email = email.replace("##SUBJECT##", subject);
		email = email.replace("##CLIENTID##", clientId);
		DOMUtils.createElementAndText(root, "Mime", java.util.Base64.getEncoder().encodeToString(email.getBytes()));
		ResponseObject response = runEndpoint(new SendMailEndpoint(), document);
		assertEquals("Status should be 200", 200, response.getStatusCode());
		assertEquals(0, response.content.getBytes().length);

		Thread.sleep(1000);

		StoreClient sc = new StoreClient(vmHostname, 1143, login, password);
		assertTrue(sc.login());
		sc.select("INBOX");
		SearchQuery sq = new SearchQuery();
		sq.setSubject(subject);
		Collection<Integer> res = sc.uidSearch(sq);
		assertEquals(1, res.size());
		Integer ret = (Integer) res.iterator().next();
		sc.logout();
		sc.close();
		return ret;
	}

	@Override
	public WbxmlHandlerBase createEndpoint() {
		return new SmartReplyEndpoint();
	}

}
