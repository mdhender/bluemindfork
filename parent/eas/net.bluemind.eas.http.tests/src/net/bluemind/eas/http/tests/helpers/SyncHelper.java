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
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.eas.http.tests.helpers;

import javax.xml.transform.TransformerException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import net.bluemind.eas.client.OPClient;
import net.bluemind.eas.client.ProtocolVersion;
import net.bluemind.eas.client.SyncResponse;
import net.bluemind.eas.http.tests.helpers.SyncRequest.ClientChangesDelete;
import net.bluemind.eas.http.tests.helpers.SyncRequest.ClientChangesModify;
import net.bluemind.eas.http.tests.validators.SyncResponseValidator;
import net.bluemind.utils.DOMUtils;

public class SyncHelper extends EasTestHelper<SyncHelper> {

	private final String collectionId;
	private String currentSyncKey = "0";
	private SyncResponse lastSyncResponse;
	private SyncResponse currentSyncResponse;

	private static final Logger logger = LoggerFactory.getLogger(SyncHelper.class);

	private SyncHelper(OPClient client, String collectionId) {
		super(client);
		this.collectionId = collectionId;
	}

	public SyncHelper sync(SyncRequest request) throws Exception {
		logger.info("Synchronizing collection {} using SyncKey {}", collectionId, currentSyncKey);

		if (currentSyncResponse != null) {
			lastSyncResponse = new SyncResponse(currentSyncResponse.getCollections());
			lastSyncResponse.dom = currentSyncResponse.dom;
		}

		Document sync = DOMUtils.createDoc("AirSync", "Sync");
		Element syncRoot = sync.getDocumentElement();
		Element collections = DOMUtils.createElement(syncRoot, "Collections");
		Element collection = DOMUtils.createElement(collections, "Collection");
		DOMUtils.createElementAndText(collection, "SyncKey", currentSyncKey);
		DOMUtils.createElementAndText(collection, "CollectionId", collectionId);
		DOMUtils.createElementAndText(collection, "GetChanges", request.getChanges() ? "1" : "0");

		if (!request.clientChangesAdd().isEmpty() || !request.clientChangesModify().isEmpty()
				|| !request.clientChangesDelete().isEmpty()) {
			Element commands = DOMUtils.createElement(collection, "Commands");
			if (!request.clientChangesAdd().isEmpty()) {
				for (int i = 0; i < request.clientChangesAdd().size(); i++) {
					Document clientChange = request.clientChangesAdd().get(i);
					Element add = DOMUtils.createElement(commands, "Add");
					DOMUtils.createElementAndText(add, "ClientId", "" + i + 1);
					Element appData = DOMUtils.createElement(add, "ApplicationData");
					NodeList children = clientChange.getDocumentElement().getChildNodes();
					for (int j = 0; j < children.getLength(); j++) {
						Node node = children.item(j);
						appData.appendChild(sync.adoptNode(node.cloneNode(true)));
					}
				}
			}
			if (!request.clientChangesModify().isEmpty()) {
				for (int i = 0; i < request.clientChangesModify().size(); i++) {
					ClientChangesModify clientChange = request.clientChangesModify().get(0);
					Element add = DOMUtils.createElement(commands, "Change");
					DOMUtils.createElementAndText(add, "ServerId", clientChange.serverId());
					Element appData = DOMUtils.createElement(add, "ApplicationData");
					NodeList children = clientChange.data().getDocumentElement().getChildNodes();
					for (int j = 0; j < children.getLength(); j++) {
						Node node = children.item(j);
						appData.appendChild(sync.adoptNode(node.cloneNode(true)));
					}
				}
			}
			if (!request.clientChangesDelete().isEmpty()) {
				for (int i = 0; i < request.clientChangesDelete().size(); i++) {
					ClientChangesDelete clientDelete = request.clientChangesDelete().get(0);
					Element add = DOMUtils.createElement(commands, "Delete");
					DOMUtils.createElementAndText(add, "ServerId", clientDelete.serverId());
					if (clientDelete.recurId() != null) {
						DOMUtils.createElementAndText(add, "AirSyncBase", "InstanceId", clientDelete.recurId());
					}
				}
			}
		}
		System.err.println("SYNC");
		logger.info(DOMUtils.logDom(sync));

		currentSyncResponse = client.sync(sync);
		currentSyncKey = extractSyncKey();
		dump();
		return this;
	}

	private String extractSyncKey() {
		if (this.currentSyncResponse == null) {
			return this.currentSyncKey;
		}
		return DOMUtils.getUniqueElement(currentSyncResponse.dom.getDocumentElement(), "SyncKey").getTextContent()
				.trim();
	}

	public static String forgeCollectionId(Long subscriptionId, Long collectionId) {
		return subscriptionId.toString().concat(SHARED_SEPARATOR).concat(collectionId.toString());
	}

	public SyncResponseValidator startValidation() {
		return new SyncResponseValidator(lastSyncResponse, currentSyncResponse, this);
	}

	public SyncHelper dump() {
		if (currentSyncResponse == null) {
			logger.info("SyncKey: {}, <empty response>", this.currentSyncKey);
			return this;
		}
		try {
			logger.info(DOMUtils.logDom(currentSyncResponse.dom));
		} catch (TransformerException e) {
			e.printStackTrace();
		}
		return this;
	}

	public static class SyncHelperBuilder {

		protected OPClient client;
		protected ProtocolVersion protocolVersion;
		private String collectionId;

		public SyncHelperBuilder() {

		}

		public SyncHelperBuilder withAuth(String latd, String password) {
			this.client = new OPClient(latd, password, "devId", "iPad", "Apple-iPad3C1/1208.321",
					"http://localhost:8082/Microsoft-Server-ActiveSync");
			return this;
		}

		public SyncHelperBuilder withProtocolVersion(ProtocolVersion protocolVersion) {
			this.protocolVersion = protocolVersion;
			return this;
		}

		public SyncHelperBuilder withCollectionId(String collectionId) {
			this.collectionId = collectionId;
			return this;
		}

		public SyncHelperBuilder withCollectionId(Long collectionId) {
			this.collectionId = collectionId.toString();
			return this;
		}

		public SyncHelper build() {
			if (client == null || collectionId == null) {
				throw new IllegalStateException("SyncHelper needs client and collectinbId");
			}

			if (protocolVersion != null) {
				client.setProtocolVersion(protocolVersion);
			} else {
				client.setProtocolVersion(ProtocolVersion.V161);
			}
			return new SyncHelper(client, collectionId);
		}

	}

}
