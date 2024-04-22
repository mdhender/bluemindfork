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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

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

	public SyncResponseValidator assertResponseStatus(long collectionId, int itemId, SyncStatus status) {
		AtomicBoolean ok = new AtomicBoolean();
		DOMUtils.forEachElement(currentSyncResponse.dom.getDocumentElement(), "Change", add -> {
			NodeList children = add.getChildNodes();
			boolean mustValidateStatus = false;
			for (int i = 0; i < children.getLength(); i++) {
				Node node = children.item(i);
				if (node.getNodeName().equals("ServerId")) {
					String id = node.getTextContent().trim();
					mustValidateStatus = id.equals(collectionId + ":" + itemId);
				} else if (node.getNodeName().equals("Status") && mustValidateStatus) {
					ok.set(node.getTextContent().trim().equals(status.asXmlValue()));
				}
			}
		});
		assertTrue(ok.get());
		return this;
	}

	public SyncHelper endValidation() {
		return sync;
	}

}
