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

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import io.vertx.core.buffer.Buffer;
import net.bluemind.authentication.api.IAuthentication;
import net.bluemind.authentication.api.LoginResponse;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.http.ClientSideServiceProvider;
import net.bluemind.eas.command.folder.sync.FolderSyncEndpoint;
import net.bluemind.eas.command.sync.SyncEndpoint;
import net.bluemind.eas.dto.tasks.TasksResponse;
import net.bluemind.eas.http.wbxml.WbxmlHandlerBase;
import net.bluemind.eas.testhelper.mock.ResponseObject;
import net.bluemind.eas.utils.DOMUtils;
import net.bluemind.eas.wbxml.WBXMLTools;
import net.bluemind.todolist.api.ITodoList;
import net.bluemind.todolist.api.VTodo;

public class SyncEndpointTaskTests extends AbstractEndpointTest {

	private int todolistId;
	private String syncKey;
	private ITodoList service;

	private static final int WINDOW_SIZE = 25;

	public void setUp() throws Exception {
		super.setUp();

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
			if ("7".equals(folderType)) { // DEFAULT_TASK_FOLDER
				todolistId = Integer.parseInt(DOMUtils.getUniqueElement(el, "ServerId").getTextContent());
			}
		}
		assertNotNull(todolistId);

		Document sync = DOMUtils.createDoc("AirSync", "Sync");
		root = sync.getDocumentElement();
		Element collections = DOMUtils.createElement(root, "Collections");
		Element collection = DOMUtils.createElement(collections, "Collection");
		DOMUtils.createElementAndText(collection, "SyncKey", "0");
		DOMUtils.createElementAndText(collection, "CollectionId", Integer.toString(todolistId));

		ResponseObject response = runEndpoint(sync);
		assertEquals(200, response.getStatusCode());
		content = response.content;
		Document resp = WBXMLTools.toXml(content.getBytes());
		syncKey = DOMUtils.getUniqueElement(resp.getDocumentElement(), "SyncKey").getTextContent();
		assertNotNull(syncKey);

		// FULL SYNC first
		boolean done = false;
		while (!done) {
			resp = sync();
			syncKey = DOMUtils.getUniqueElement(resp.getDocumentElement(), "SyncKey").getTextContent();
			Element moreAvailable = DOMUtils.getUniqueElement(resp.getDocumentElement(), "MoreAvailable");
			done = (moreAvailable == null);
			if (!done) {
				Element commands = DOMUtils.getUniqueElement(resp.getDocumentElement(), "Commands");
				assertNotNull(commands);
				assertTrue(commands.getChildNodes().getLength() <= WINDOW_SIZE);
				for (int i = 0; i < commands.getChildNodes().getLength(); i++) {
					Element item = (Element) commands.getChildNodes().item(i);
					assertEquals("Add", item.getNodeName());
				}
			}
		}

		IAuthentication authService = ClientSideServiceProvider.getProvider(testDevice.coreUrl, null)
				.instance(IAuthentication.class);
		LoginResponse token = authService.login(login, password, "sync-endpoint-calendar-tests");
		service = ClientSideServiceProvider.getProvider(testDevice.coreUrl, token.authKey).instance(ITodoList.class,
				"todolist:default_" + testDevice.owner.uid);

	}

	public void testCreate() throws Exception {
		// next sync (0 item)
		Document resp = sync();
		syncKey = DOMUtils.getUniqueElement(resp.getDocumentElement(), "SyncKey").getTextContent();
		Element commands = DOMUtils.getUniqueElement(resp.getDocumentElement(), "Commands");
		assertNull(commands);

		String clientId = System.currentTimeMillis() + "";
		Document sync = DOMUtils.createDoc("AirSync", "Sync");
		Element root = sync.getDocumentElement();
		Element collections = DOMUtils.createElement(root, "Collections");
		Element collection = DOMUtils.createElement(collections, "Collection");
		DOMUtils.createElementAndText(collection, "SyncKey", syncKey);
		DOMUtils.createElementAndText(collection, "CollectionId", Integer.toString(todolistId));
		DOMUtils.createElement(collection, "GetChanges");
		DOMUtils.createElementAndText(collection, "WindowSize", WINDOW_SIZE + "");

		Element options = DOMUtils.createElement(collection, "Options");
		DOMUtils.createElementAndText(options, "FilterType", "3");
		Element bodyPreference = DOMUtils.createElement(options, "AirSyncBase:BodyPreference");
		DOMUtils.createElementAndText(bodyPreference, "Type", "1");
		DOMUtils.createElementAndText(bodyPreference, "TruncationSize", "32768");

		commands = DOMUtils.createElement(collection, "Commands");
		Element add = DOMUtils.createElement(commands, "Add");
		DOMUtils.createElementAndText(add, "ClientId", clientId);

		Element appData = DOMUtils.createElement(add, "ApplicationData");

		Element body = DOMUtils.createElement(appData, "AirSyncBase:Body");
		DOMUtils.createElementAndText(body, "Type", "1");
		DOMUtils.createElementAndText(body, "Data", "description w00t w00t");

		DOMUtils.createElementAndText(appData, "Tasks:Subject", "acheter du pain");
		DOMUtils.createElementAndText(appData, "Tasks:Importance", TasksResponse.Importance.High.xmlValue());
		DOMUtils.createElementAndText(appData, "Tasks:UtcStartDate", "2016-01-17T14:00:00.000Z");
		DOMUtils.createElementAndText(appData, "Tasks:StartDate", "2016-01-17T15:00:00.000Z");
		DOMUtils.createElementAndText(appData, "Tasks:UtcDueDate", "2016-01-17T14:00:00.000Z");
		DOMUtils.createElementAndText(appData, "Tasks:DueDate", "2016-01-17T15:00:00.000Z");
		DOMUtils.createElementAndText(appData, "Tasks:Complete", "0");
		DOMUtils.createElementAndText(appData, "Tasks:Sensitivity", "0");
		DOMUtils.createElementAndText(appData, "Tasks:ReminderTime", "2016-01-17T14:00:00.000Z");
		DOMUtils.createElementAndText(appData, "Tasks:ReminderSet", "1");

		ResponseObject response = runEndpoint(sync);
		assertEquals(200, response.getStatusCode());
		Buffer content = response.content;

		resp = WBXMLTools.toXml(content.getBytes());
		syncKey = DOMUtils.getUniqueElement(resp.getDocumentElement(), "SyncKey").getTextContent();
		String collectionId = DOMUtils.getUniqueElement(resp.getDocumentElement(), "CollectionId").getTextContent();
		assertEquals(todolistId + "", collectionId);
		commands = DOMUtils.getUniqueElement(resp.getDocumentElement(), "Commands");
		assertNull(commands);
		Element responses = DOMUtils.getUniqueElement(resp.getDocumentElement(), "Responses");
		assertNotNull(responses);

		String serverId = DOMUtils.getUniqueElement(resp.getDocumentElement(), "ServerId").getTextContent();
		assertNotNull(serverId);

		String uid = serverId.split(":")[1];
		ItemValue<VTodo> check = service.getComplete(uid);
		assertNotNull(check);
		assertEquals(1, check.value.priority.intValue()); // 1 == high

		// next sync should be empty
		resp = sync();
		syncKey = DOMUtils.getUniqueElement(resp.getDocumentElement(), "SyncKey").getTextContent();
		commands = DOMUtils.getUniqueElement(resp.getDocumentElement(), "Commands");
		assertNull(commands);
		responses = DOMUtils.getUniqueElement(resp.getDocumentElement(), "Responses");
		assertNull(responses);
	}

	private static interface IClientChangeProvider {
		void setClientChanges(int collectionId, Element collectionElement);
	}

	private Document sync() throws IOException {
		return runSyncEndpoint(todolistId, new IClientChangeProvider() {

			@Override
			public void setClientChanges(int collectionId, Element collectionElement) {
			}
		});
	}

	private Document runSyncEndpoint(int collectionId, IClientChangeProvider clientChanges) throws IOException {
		Document sync = DOMUtils.createDoc("AirSync", "Sync");
		Element root = sync.getDocumentElement();
		Element collections = DOMUtils.createElement(root, "Collections");
		Element collection = DOMUtils.createElement(collections, "Collection");
		DOMUtils.createElementAndText(collection, "SyncKey", syncKey);
		DOMUtils.createElementAndText(collection, "CollectionId", Integer.toString(collectionId));
		DOMUtils.createElement(collection, "GetChanges");
		DOMUtils.createElementAndText(collection, "WindowSize", WINDOW_SIZE + "");
		Element options = DOMUtils.createElement(collection, "Options");
		DOMUtils.createElementAndText(options, "FilterType", "3");
		Element bodyPreference = DOMUtils.createElement(options, "AirSyncBase:BodyPreference");
		DOMUtils.createElementAndText(bodyPreference, "Type", "2");
		DOMUtils.createElementAndText(bodyPreference, "TruncationSize", "200000");
		clientChanges.setClientChanges(todolistId, collection);
		ResponseObject response = runEndpoint(sync);
		assertEquals(200, response.getStatusCode());
		Buffer content = response.content;
		Document resp = WBXMLTools.toXml(content.getBytes());
		return resp;
	}

	@Override
	public WbxmlHandlerBase createEndpoint() {
		return new SyncEndpoint();
	}

}
