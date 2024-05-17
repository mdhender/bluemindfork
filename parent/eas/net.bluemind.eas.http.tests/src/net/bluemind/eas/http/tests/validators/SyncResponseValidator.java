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
package net.bluemind.eas.http.tests.validators;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import net.bluemind.eas.client.SyncResponse;
import net.bluemind.eas.dto.sync.SyncStatus;
import net.bluemind.eas.http.tests.helpers.SyncHelper;
import net.bluemind.utils.DOMUtils;

public class SyncResponseValidator extends DomValidator<SyncResponseValidator> {

	private final SyncResponse lastSyncResponse;
	private final SyncResponse currentSyncResponse;
	private final SyncHelper sync;

	public SyncResponseValidator(SyncResponse lastSyncResponse, SyncResponse currentSyncResponse, SyncHelper sync) {
		super(currentSyncResponse == null ? null : currentSyncResponse.dom);
		this.lastSyncResponse = lastSyncResponse;
		this.currentSyncResponse = currentSyncResponse;
		this.sync = sync;
	}

	public SyncResponseValidator assertSyncKeyChanged() {
		String lastSyncKey = lastSyncResponse == null ? "0"
				: DOMUtils.getUniqueElement(lastSyncResponse.dom.getDocumentElement(), "SyncKey").getTextContent()
						.trim();
		String currentSyncKey = currentSyncResponse == null ? "0"
				: DOMUtils.getUniqueElement(currentSyncResponse.dom.getDocumentElement(), "SyncKey").getTextContent()
						.trim();
		assertNotEquals(lastSyncKey, currentSyncKey);
		return this;
	}

	public SyncResponseValidator assertSyncStatus(String collectionId, SyncStatus status) {
		assertNotNull(DOMUtils.getUniqueElement(currentSyncResponse.dom.getDocumentElement(), "SyncKey"));
		assertElementText(currentSyncResponse.dom.getDocumentElement(), collectionId, "Collections", "Collection",
				"CollectionId");
		assertElementText(currentSyncResponse.dom.getDocumentElement(), status.asXmlValue(), "Collections",
				"Collection", "Status");
		return this;
	}

	public SyncResponseValidator assertEmptyResponse() {
		assertNull(currentSyncResponse);
		return this;
	}

	public SyncResponseValidator assertServerConfirmation(int eventCount) {
		AtomicInteger count = new AtomicInteger(0);
		DOMUtils.forEachElement(currentSyncResponse.dom.getDocumentElement(), "Add", add -> {
			count.incrementAndGet();
			NodeList children = add.getChildNodes();
			boolean validated = false;
			boolean idValid = false;
			for (int i = 0; i < children.getLength(); i++) {
				Node node = children.item(i);
				if (node.getNodeName().equals("ClientId")) {
					int id = Integer.parseInt(node.getTextContent().trim());
					if (id <= eventCount) {
						idValid = true;
					}
				} else if (node.getNodeName().equals("Status")) {
					String status = node.getTextContent().trim();
					if (status.equals(SyncStatus.OK.asXmlValue())) {
						validated = true;
					}
				}
			}
			assertTrue(validated);
			assertTrue(idValid);
		});
		assertEquals(eventCount, count.get());
		return this;
	}

	public SyncResponseValidator assertResponseStatus(String serverId, SyncStatus status, ResponseSyncType response) {
		AtomicBoolean ok = new AtomicBoolean();
		AtomicLong statusRetrieved = new AtomicLong();

		DOMUtils.forEachElement(currentSyncResponse.dom.getDocumentElement(), response.key, add -> {
			NodeList children = add.getChildNodes();
			boolean mustValidateStatus = false;
			for (int i = 0; i < children.getLength(); i++) {
				Node node = children.item(i);
				if (node.getNodeName().equals("ServerId")) {
					String id = node.getTextContent().trim();
					mustValidateStatus = id.equals(serverId);
				} else if (node.getNodeName().equals("Status") && mustValidateStatus) {
					statusRetrieved.set(Long.parseLong(node.getTextContent()));
					ok.set(node.getTextContent().trim().equals(status.asXmlValue()));
				}
			}
		});
		assertTrue("Response Status is not correct: " + statusRetrieved.get() + " for command <" + response.key
				+ ">, expected " + status.asXmlValue(), ok.get());
		return this;
	}

	public SyncResponseValidator assertResponseStatus(long collectionId, long itemId, SyncStatus status,
			ResponseSyncType response) {
		String composedServerId = collectionId + SyncHelper.SERVER_ID_SEPARATOR + itemId;
		return assertResponseStatus(composedServerId, status, response);
	}

	public SyncResponseValidator assertResponseStatus(long subscriptionId, long collectionId, long itemId,
			SyncStatus status, ResponseSyncType response) {
		String composedServerId = subscriptionId + SyncHelper.SHARED_SEPARATOR + collectionId
				+ SyncHelper.SERVER_ID_SEPARATOR + itemId;
		return assertResponseStatus(composedServerId, status, response);
	}

	public SyncResponseValidator assertResponseServerId(String serverId, ResponseSyncType response) {
		AtomicBoolean ok = new AtomicBoolean();

		DOMUtils.forEachElement(currentSyncResponse.dom.getDocumentElement(), response.key, add -> {
			NodeList children = add.getChildNodes();
			for (int i = 0; i < children.getLength(); i++) {
				Node node = children.item(i);
				if (node.getNodeName().equals("ServerId")) {
					String id = node.getTextContent().trim();
					ok.set(id.equals(serverId));
				}
			}
		});
		assertTrue("Response ServerId is not correct for command <" + response.key + ">, expected " + serverId,
				ok.get());
		return this;
	}

	public SyncResponseValidator assertResponseServerId(long collectionId, long itemId, ResponseSyncType response) {
		return assertResponseServerId(composedServerId(collectionId, itemId), response);
	}

	public SyncResponseValidator assertResponseServerId(long subscriptionId, long collectionId, long itemId,
			ResponseSyncType response) {
		return assertResponseServerId(composedServerId(subscriptionId, collectionId, itemId), response);
	}

	private static String composedServerId(long collectionId, long itemId) {
		return collectionId + SyncHelper.SERVER_ID_SEPARATOR + itemId;
	}

	private static String composedServerId(long subscriptionId, long collectionId, long itemId) {
		return subscriptionId + SyncHelper.SHARED_SEPARATOR + composedServerId(collectionId, itemId);
	}

	public SyncHelper endValidation() {
		return sync;
	}

	public enum ResponseSyncType {
		ADD("Add"), CHANGE("Change"), DELETE("Delete");

		String key;

		private ResponseSyncType(String key) {
			this.key = key;
		}
	}

}
