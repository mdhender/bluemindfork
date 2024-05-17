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

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import net.bluemind.eas.client.MoveItemsResponse;
import net.bluemind.eas.client.MoveStatus;
import net.bluemind.eas.http.tests.helpers.EasTestHelper;
import net.bluemind.eas.http.tests.helpers.MoveHelper;
import net.bluemind.eas.http.tests.helpers.SyncHelper;
import net.bluemind.utils.DOMUtils;

public class MoveItemsResponseValidator extends DomValidator<MoveItemsResponseValidator> {

	private final MoveItemsResponse currentSyncResponse;
	private final MoveHelper sync;

	public MoveItemsResponseValidator(MoveItemsResponse lastSyncResponse, MoveItemsResponse currentSyncResponse,
			MoveHelper sync) {
		super(currentSyncResponse == null ? null : currentSyncResponse.dom);
		this.currentSyncResponse = currentSyncResponse;
		this.sync = sync;
	}

	public MoveItemsResponseValidator assertEmptyResponse() {
		assertNull(currentSyncResponse);
		return this;
	}

	public MoveItemsResponseValidator assertResponseStatus(String serverId, MoveStatus status) {
		boolean withSubscription = serverId.contains(SyncHelper.SHARED_SEPARATOR);
		String tmpServerId = serverId;
		long subscriptionId = 0L;
		if (withSubscription) {
			String[] explodedServerId = serverId.split(SyncHelper.SHARED_SEPARATOR);
			subscriptionId = Long.parseLong(explodedServerId[0]);
			tmpServerId = explodedServerId[1];
		}

		long collectionId = Long.parseLong(tmpServerId.split(SyncHelper.SERVER_ID_SEPARATOR)[0]);
		long itemId = Long.parseLong(tmpServerId.split(SyncHelper.SERVER_ID_SEPARATOR)[1]);

		if (withSubscription) {
			return assertResponseStatus(subscriptionId, collectionId, itemId, status);
		}
		return assertResponseStatus(collectionId, itemId, status);
	}

	public MoveItemsResponseValidator assertResponseDstMsgFolder(String dstFldId, MoveStatus status) {
		AtomicBoolean ok = new AtomicBoolean();
		DOMUtils.forEachElement(currentSyncResponse.dom.getDocumentElement(), "Response", add -> {
			NodeList children = add.getChildNodes();
			boolean mustValidateStatus = false;
			for (int i = 0; i < children.getLength(); i++) {
				Node node = children.item(i);
				if (node.getNodeName().equals("Status")) {
					mustValidateStatus = node.getTextContent().trim().equals(status.asXmlValue());
				} else if (node.getNodeName().equals("DstMsgId") && mustValidateStatus) {
					String id = node.getTextContent().trim().split(EasTestHelper.SERVER_ID_SEPARATOR)[0];
					ok.set(id.equals(dstFldId));
				}
			}
		});
		assertTrue("Response DstMsgFolder is not correct", ok.get());
		return this;
	}

	public MoveItemsResponseValidator assertResponseStatus(long collectionId, long itemId, MoveStatus status) {
		AtomicBoolean ok = new AtomicBoolean();
		AtomicLong statusRetrieved = new AtomicLong();
		DOMUtils.forEachElement(currentSyncResponse.dom.getDocumentElement(), "Response", add -> {
			NodeList children = add.getChildNodes();
			boolean mustValidateStatus = false;
			for (int i = 0; i < children.getLength(); i++) {
				Node node = children.item(i);
				if (node.getNodeName().equals("SrcMsgId")) {
					String id = node.getTextContent().trim();
					mustValidateStatus = id.equals(collectionId + SyncHelper.SERVER_ID_SEPARATOR + itemId);
				} else if (node.getNodeName().equals("Status") && mustValidateStatus) {
					statusRetrieved.set(Long.parseLong(node.getTextContent()));
					ok.set(node.getTextContent().trim().equals(status.asXmlValue()));
				}
			}
		});
		assertTrue("Response Status is not correct: " + statusRetrieved.get() + " for command <Move>, expected "
				+ status.asXmlValue(), ok.get());
		return this;
	}

	public MoveItemsResponseValidator assertResponseStatus(long subscriptionId, long collectionId, long itemId,
			MoveStatus status) {
		AtomicBoolean ok = new AtomicBoolean();
		AtomicLong statusRetrieved = new AtomicLong();
		DOMUtils.forEachElement(currentSyncResponse.dom.getDocumentElement(), "Response", add -> {
			NodeList children = add.getChildNodes();
			boolean mustValidateStatus = false;
			for (int i = 0; i < children.getLength(); i++) {
				Node node = children.item(i);
				if (node.getNodeName().equals("SrcMsgId")) {
					String id = node.getTextContent().trim();
					mustValidateStatus = id.equals(subscriptionId + SyncHelper.SHARED_SEPARATOR + collectionId
							+ SyncHelper.SERVER_ID_SEPARATOR + itemId);
				} else if (node.getNodeName().equals("Status") && mustValidateStatus) {
					statusRetrieved.set(Long.parseLong(node.getTextContent()));
					ok.set(node.getTextContent().trim().equals(status.asXmlValue()));
				}
			}
		});
		assertTrue("Response Status is not correct: " + statusRetrieved.get() + " for command <Move>, expected "
				+ status.asXmlValue(), ok.get());
		return this;
	}

	public MoveHelper endValidation() {
		return sync;
	}

}
