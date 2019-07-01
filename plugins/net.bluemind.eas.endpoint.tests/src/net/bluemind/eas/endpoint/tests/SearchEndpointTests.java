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
import java.util.Base64;
import java.util.UUID;

import org.vertx.java.core.buffer.Buffer;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.google.common.io.ByteStreams;

import net.bluemind.addressbook.api.IAddressBook;
import net.bluemind.addressbook.api.VCard;
import net.bluemind.addressbook.api.VCard.Identification.Name;
import net.bluemind.authentication.api.IAuthentication;
import net.bluemind.authentication.api.LoginResponse;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.rest.http.ClientSideServiceProvider;
import net.bluemind.eas.command.folder.sync.FolderSyncEndpoint;
import net.bluemind.eas.command.search.SearchEndpoint;
import net.bluemind.eas.dto.search.GAL;
import net.bluemind.eas.dto.search.SearchResponse;
import net.bluemind.eas.http.wbxml.WbxmlHandlerBase;
import net.bluemind.eas.testhelper.mock.ResponseObject;
import net.bluemind.eas.utils.DOMUtils;
import net.bluemind.eas.wbxml.WBXMLTools;

public class SearchEndpointTests extends AbstractEndpointTest {

	public void testInvalidNullStoreName() throws IOException {
		Document document = DOMUtils.createDoc("Search", "Search");
		Element root = document.getDocumentElement();
		DOMUtils.createElement(root, "Store");

		ResponseObject response = runEndpoint(document);
		assertEquals("Status should be 200", 200, response.getStatusCode());

		Buffer content = response.content;
		Document d = WBXMLTools.toXml(content.getBytes());

		Element status = DOMUtils.getUniqueElement(d.getDocumentElement(), "Status");
		assertEquals(SearchResponse.Status.ServerError.xmlValue(), status.getTextContent());

	}

	public void testInvalidStoreName() throws IOException {
		Document document = DOMUtils.createDoc("Search", "Search");
		Element root = document.getDocumentElement();
		Element store = DOMUtils.createElement(root, "Store");
		DOMUtils.createElementAndText(store, "Name", "foo");

		ResponseObject response = runEndpoint(document);
		assertEquals("Status should be 200", 200, response.getStatusCode());

		Buffer content = response.content;
		Document d = WBXMLTools.toXml(content.getBytes());

		Element status = DOMUtils.getUniqueElement(d.getDocumentElement(), "Status");
		assertEquals(SearchResponse.Status.ServerError.xmlValue(), status.getTextContent());

	}

	public void testGALSearch() throws Exception {

		String lastname = "lastname" + System.currentTimeMillis();
		createContact(lastname, false);
		Thread.sleep(1000); // ES indexing

		Document document = DOMUtils.createDoc("Search", "Search");
		Element root = document.getDocumentElement();
		Element store = DOMUtils.createElement(root, "Store");
		DOMUtils.createElementAndText(store, "Name", "GAL");
		DOMUtils.createElementAndText(store, "Query", lastname);
		Element options = DOMUtils.createElement(store, "Options");
		DOMUtils.createElementAndText(options, "Range", "0-99");

		ResponseObject response = runEndpoint(document);
		assertEquals("Status should be 200", 200, response.getStatusCode());

		Buffer content = response.content;
		Document d = WBXMLTools.toXml(content.getBytes());

		Element status = DOMUtils.getUniqueElement(d.getDocumentElement(), "Status");
		assertEquals("1", status.getTextContent());
		Element result = DOMUtils.getUniqueElement(d.getDocumentElement(), "Result");
		assertNotNull(result);

		NodeList properties = result.getElementsByTagName("Properties");
		assertNotNull(properties);
		assertEquals(1, properties.getLength());

		Element prop = (Element) properties.item(0);
		Element displayName = DOMUtils.getUniqueElement(prop, "DisplayName");
		assertEquals("firstname " + lastname, displayName.getTextContent());

		Element lastName = DOMUtils.getUniqueElement(prop, "LastName");
		assertEquals(lastname, lastName.getTextContent());

		Element firstName = DOMUtils.getUniqueElement(prop, "FirstName");
		assertEquals("firstname", firstName.getTextContent());

		Element picture = DOMUtils.getUniqueElement(prop, "Picture");
		assertNull(picture);

		Element range = DOMUtils.getUniqueElement(d.getDocumentElement(), "Range");
		assertNotNull(range);

		Element total = DOMUtils.getUniqueElement(d.getDocumentElement(), "Total");
		assertNotNull(total);
	}

	public void testGALSearchNoPicture() throws Exception {

		String lastname = "lastname" + System.currentTimeMillis();
		createContact(lastname, false);
		Thread.sleep(1000); // ES indexing

		Document document = DOMUtils.createDoc("Search", "Search");
		Element root = document.getDocumentElement();
		Element store = DOMUtils.createElement(root, "Store");
		DOMUtils.createElementAndText(store, "Name", "GAL");
		DOMUtils.createElementAndText(store, "Query", lastname);
		Element options = DOMUtils.createElement(store, "Options");
		DOMUtils.createElementAndText(options, "Range", "0-99");
		Element picutre = DOMUtils.createElement(options, "Picture");
		DOMUtils.createElementAndText(picutre, "MaxPictures", "1");

		ResponseObject response = runEndpoint(document);
		assertEquals("Status should be 200", 200, response.getStatusCode());

		Buffer content = response.content;
		Document d = WBXMLTools.toXml(content.getBytes());

		Element status = DOMUtils.getUniqueElement(d.getDocumentElement(), "Status");
		assertEquals("1", status.getTextContent());
		Element result = DOMUtils.getUniqueElement(d.getDocumentElement(), "Result");
		assertNotNull(result);

		NodeList properties = result.getElementsByTagName("Properties");
		assertNotNull(properties);
		assertEquals(1, properties.getLength());

		Element prop = (Element) properties.item(0);
		Element picture = DOMUtils.getUniqueElement(prop, "Picture");
		assertNotNull(picture);

		Element pictureStatus = DOMUtils.getUniqueElement(picture, "Status");
		assertEquals(GAL.Picture.Status.NoPhoto.xmlValue(), pictureStatus.getTextContent());
	}

	public void testGALSearchPictureMaxSize() throws Exception {
		String lastname = "lastname" + System.currentTimeMillis();
		createContact(lastname, true);
		Thread.sleep(1000); // ES indexing

		Document document = DOMUtils.createDoc("Search", "Search");
		Element root = document.getDocumentElement();
		Element store = DOMUtils.createElement(root, "Store");
		DOMUtils.createElementAndText(store, "Name", "GAL");
		DOMUtils.createElementAndText(store, "Query", lastname);
		Element options = DOMUtils.createElement(store, "Options");
		DOMUtils.createElementAndText(options, "Range", "0-99");
		Element picutre = DOMUtils.createElement(options, "Picture");
		DOMUtils.createElementAndText(picutre, "MaxPictures", "1");
		DOMUtils.createElementAndText(picutre, "MaxSize", "1");

		ResponseObject response = runEndpoint(document);
		assertEquals("Status should be 200", 200, response.getStatusCode());

		Buffer content = response.content;
		Document d = WBXMLTools.toXml(content.getBytes());

		Element status = DOMUtils.getUniqueElement(d.getDocumentElement(), "Status");
		assertEquals("1", status.getTextContent());
		Element result = DOMUtils.getUniqueElement(d.getDocumentElement(), "Result");
		assertNotNull(result);

		NodeList properties = result.getElementsByTagName("Properties");
		assertNotNull(properties);
		assertEquals(1, properties.getLength());

		Element prop = (Element) properties.item(0);
		Element picture = DOMUtils.getUniqueElement(prop, "Picture");
		assertNotNull(picture);

		Element pictureStatus = DOMUtils.getUniqueElement(picture, "Status");
		assertEquals(GAL.Picture.Status.MaxSizeExceeded.xmlValue(), pictureStatus.getTextContent());
	}

	public void testGALSearchPicture() throws Exception {
		String lastname = "lastname" + System.currentTimeMillis();
		createContact(lastname, true);
		Thread.sleep(1000); // ES indexing

		Document document = DOMUtils.createDoc("Search", "Search");
		Element root = document.getDocumentElement();
		Element store = DOMUtils.createElement(root, "Store");
		DOMUtils.createElementAndText(store, "Name", "GAL");
		DOMUtils.createElementAndText(store, "Query", lastname);
		Element options = DOMUtils.createElement(store, "Options");
		DOMUtils.createElementAndText(options, "Range", "0-99");
		Element picutre = DOMUtils.createElement(options, "Picture");
		DOMUtils.createElementAndText(picutre, "MaxPictures", "1");

		ResponseObject response = runEndpoint(document);
		assertEquals("Status should be 200", 200, response.getStatusCode());

		Buffer content = response.content;
		Document d = WBXMLTools.toXml(content.getBytes());

		Element status = DOMUtils.getUniqueElement(d.getDocumentElement(), "Status");
		assertEquals("1", status.getTextContent());
		Element result = DOMUtils.getUniqueElement(d.getDocumentElement(), "Result");
		assertNotNull(result);

		NodeList properties = result.getElementsByTagName("Properties");
		assertNotNull(properties);
		assertEquals(1, properties.getLength());

		Element prop = (Element) properties.item(0);
		Element picture = DOMUtils.getUniqueElement(prop, "Picture");
		assertNotNull(picture);

		Element pictureStatus = DOMUtils.getUniqueElement(picture, "Status");
		assertEquals(GAL.Picture.Status.Success.xmlValue(), pictureStatus.getTextContent());

		Element data = DOMUtils.getUniqueElement(picture, "Data");
		assertNotNull(data);

	}

	public void testGALSearchNoResult() throws IOException {
		Document document = DOMUtils.createDoc("Search", "Search");
		Element root = document.getDocumentElement();
		Element store = DOMUtils.createElement(root, "Store");
		DOMUtils.createElementAndText(store, "Name", "GAL");
		DOMUtils.createElementAndText(store, "Query", UUID.randomUUID().toString());
		Element options = DOMUtils.createElement(store, "Options");
		DOMUtils.createElementAndText(options, "Range", "0-99");

		ResponseObject response = runEndpoint(document);
		assertEquals("Status should be 200", 200, response.getStatusCode());

		Buffer content = response.content;
		Document d = WBXMLTools.toXml(content.getBytes());

		Element status = DOMUtils.getUniqueElement(d.getDocumentElement(), "Status");
		assertEquals("1", status.getTextContent());
		Element result = DOMUtils.getUniqueElement(d.getDocumentElement(), "Result");
		assertNotNull(result);

		assertEquals(0, result.getChildNodes().getLength());

		Element range = DOMUtils.getUniqueElement(d.getDocumentElement(), "Range");
		assertNull(range);

		Element total = DOMUtils.getUniqueElement(d.getDocumentElement(), "Total");
		assertNull(total);
	}

	public void testEmailSearch() throws Exception {

		initFolder();

		String subject = UUID.randomUUID().toString();
		appendEmail(subject);

		Document document = DOMUtils.createDoc("Search", "Search");
		Element root = document.getDocumentElement();

		Element store = DOMUtils.createElement(root, "Store");
		DOMUtils.createElementAndText(store, "Name", "Mailbox");

		Element query = DOMUtils.createElement(store, "Query");
		Element and = DOMUtils.createElement(query, "And");
		DOMUtils.createElementAndText(and, "AirSync:Class", "Email");
		DOMUtils.createElementAndText(and, "FreeText", subject);

		Element options = DOMUtils.createElement(store, "Options");
		DOMUtils.createElement(options, "RebuildResults");
		DOMUtils.createElement(options, "DeepTraversal");
		DOMUtils.createElementAndText(options, "Range", "0-99");
		Element bodyPref = DOMUtils.createElement(options, "AirSyncBase:BodyPreference");
		DOMUtils.createElementAndText(bodyPref, "Type", "2");
		DOMUtils.createElementAndText(bodyPref, "TruncationSize", "20000");

		ResponseObject response = runEndpoint(document);
		assertEquals("Status should be 200", 200, response.getStatusCode());

		// Expected
		// <?xml version="1.0" encoding="UTF-8"?><Search xmlns="Search">
		// <Status>1</Status>
		// <Response>
		// <Store>
		// <Status>1</Status>
		// <Result>
		// <Class xmlns="AirSync">Email</Class>
		// <LongId>1166</LongId>
		// <CollectionId xmlns="AirSync">null</CollectionId>
		// <Properties>
		// <To xmlns="Email">"admin SetupWizard" &lt;admin@bm.lan&gt; </To>
		// <From xmlns="Email">"John Bang" &lt;john.bang@local.lan&gt; </From>
		// <Subject xmlns="Email">ed6b60c7-1da1-43e7-ad40-e72171ce8d8e</Subject>
		// <DateReceived xmlns="Email">2015-10-05T07:50:04.000Z</DateReceived>
		// <DisplayTo xmlns="Email">admin SetupWizard</DisplayTo>
		// <ThreadTopic
		// xmlns="Email">ed6b60c7-1da1-43e7-ad40-e72171ce8d8e</ThreadTopic>
		// <Importance xmlns="Email">1</Importance>
		// <Read xmlns="Email">0</Read>
		// <Body xmlns="AirSyncBase">
		// <Type>2</Type>
		// <EstimatedDataSize>30</EstimatedDataSize>
		// <Data>&lt;html&gt;&lt;body&gt;osef&lt;/body&gt;&lt;/html&gt;</Data>
		// </Body>
		// <ContentClass
		// xmlns="Email">urn:content-classes:message</ContentClass>
		// <NativeBodyType xmlns="AirSyncBase">2</NativeBodyType>
		// <MessageClass xmlns="Email">IPM.Note</MessageClass>
		// <InternetCPID xmlns="Email">65001</InternetCPID>
		// <Flag xmlns="Email">
		// <Status>0</Status>
		// </Flag>
		// </Properties>
		// </Result>
		// <Range>0-1</Range>
		// <Total>1</Total>
		// </Store>
		// </Response>
		// </Search>

		Buffer content = response.content;
		Document d = WBXMLTools.toXml(content.getBytes());
		DOMUtils.logDom(d);

		Element status = DOMUtils.getUniqueElement(d.getDocumentElement(), "Status");
		assertEquals("1", status.getTextContent());

		NodeList results = d.getElementsByTagName("Result");
		assertEquals(1, results.getLength());

		Element result = (Element) results.item(0);

		Element clazz = DOMUtils.getUniqueElement(result, "Class");
		assertEquals("Email", clazz.getTextContent());
		Element longId = DOMUtils.getUniqueElement(result, "LongId");
		assertNotNull(longId);
		Element collectionId = DOMUtils.getUniqueElement(result, "CollectionId");
		assertNotNull(collectionId);

		Element to = DOMUtils.getUniqueElement(result, "To");
		assertEquals("\"" + owner.displayName + "\" <" + owner.value.defaultEmail().address + ">", to.getTextContent());

		Element from = DOMUtils.getUniqueElement(result, "From");
		assertEquals("\"John Bang\" <john.bang@local.lan>", from.getTextContent());

		Element dateReceived = DOMUtils.getUniqueElement(result, "DateReceived");
		assertNotNull(dateReceived);

		Element displayTo = DOMUtils.getUniqueElement(result, "DisplayTo");
		assertEquals(owner.displayName, displayTo.getTextContent());

		Element threadTopic = DOMUtils.getUniqueElement(result, "ThreadTopic");
		assertNotNull(threadTopic);

		Element importance = DOMUtils.getUniqueElement(result, "Importance");
		assertEquals("1", importance.getTextContent());

		Element read = DOMUtils.getUniqueElement(result, "Read");
		assertEquals("0", read.getTextContent());

		Element body = DOMUtils.getUniqueElement(result, "Body");
		assertNotNull(body);

		Element type = DOMUtils.getUniqueElement(body, "Type");
		assertEquals("2", type.getTextContent());

		Element estimateDataSize = DOMUtils.getUniqueElement(body, "EstimatedDataSize");
		assertNotNull(estimateDataSize);

		Element data = DOMUtils.getUniqueElement(body, "Data");
		assertEquals("<html><body>osef</body></html>", data.getTextContent());

		Element contentClass = DOMUtils.getUniqueElement(result, "ContentClass");
		assertEquals("urn:content-classes:message", contentClass.getTextContent());

		Element nativeBodyType = DOMUtils.getUniqueElement(result, "NativeBodyType");
		assertEquals("2", nativeBodyType.getTextContent());

		Element messageClass = DOMUtils.getUniqueElement(result, "MessageClass");
		assertEquals("IPM.Note", messageClass.getTextContent());

		Element internetCPID = DOMUtils.getUniqueElement(result, "InternetCPID");
		assertEquals("65001", internetCPID.getTextContent());

		Element flag = DOMUtils.getUniqueElement(result, "Flag");
		assertNotNull(flag);

		Element flagStatus = DOMUtils.getUniqueElement(flag, "Status");
		assertEquals("0", flagStatus.getTextContent());

		Element range = DOMUtils.getUniqueElement(d.getDocumentElement(), "Range");
		assertEquals("0-1", range.getTextContent());

		Element total = DOMUtils.getUniqueElement(d.getDocumentElement(), "Total");
		assertEquals("1", total.getTextContent());
	}

	private void initFolder() {
		Document document = DOMUtils.createDoc("FolderHierarchy", "FolderSync");
		DOMUtils.createElementAndText(document.getDocumentElement(), "FolderHierarchy:SyncKey", "0");
		ResponseObject response = runEndpoint(new FolderSyncEndpoint(), document);
		assertEquals("Status should be 200", 200, response.getStatusCode());

	}

	private void createContact(String lastname, boolean picture) throws ServerFault {

		IAuthentication authService = ClientSideServiceProvider.getProvider(testDevice.coreUrl, null)
				.instance(IAuthentication.class);
		LoginResponse token = authService.login(testDevice.loginAtDomain, testDevice.password, "search-endpoint-test");

		IAddressBook addressBookService = ClientSideServiceProvider.getProvider(testDevice.coreUrl, token.authKey)
				.instance(IAddressBook.class, "book:Contacts_" + testDevice.owner.uid);

		VCard card = new VCard();

		card.identification = new VCard.Identification();
		card.identification.name = Name.create(lastname, "firstname", null, null, null, null);

		String uid = UUID.randomUUID().toString();
		addressBookService.create(uid, card);

		if (picture) {
			addressBookService.setPhoto(uid, getB64PngContent().getBytes());
		}

	}

	// FIXME "not an image"
	private String getB64PngContent() throws ServerFault {

		InputStream in = SearchEndpointTests.class.getClassLoader().getResourceAsStream("data/pic.jpg");
		try {
			String b64 = Base64.getEncoder().encodeToString(ByteStreams.toByteArray(in));
			in.close();

			return b64;
		} catch (IOException e) {
			throw new ServerFault(e);
		}
	}

	@Override
	public WbxmlHandlerBase createEndpoint() {
		return new SearchEndpoint();
	}

}
