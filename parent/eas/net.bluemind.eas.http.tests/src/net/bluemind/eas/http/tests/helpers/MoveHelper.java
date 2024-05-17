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

import net.bluemind.eas.client.MoveItemsResponse;
import net.bluemind.eas.client.OPClient;
import net.bluemind.eas.client.ProtocolVersion;
import net.bluemind.eas.http.tests.helpers.MoveItemRequest.ClientMoveItems;
import net.bluemind.eas.http.tests.validators.MoveItemsResponseValidator;
import net.bluemind.utils.DOMUtils;

public class MoveHelper extends EasTestHelper<MoveHelper> {

//	private final MoveItemsRequest moveItemsRequest;
	private String currentSyncKey = "0";
	private MoveItemsResponse lastMoveResponse;
	private MoveItemsResponse currentMoveResponse;

	private String srcMsgId;
	private String srcFldId;
	private String dstFldId;

	private static final Logger logger = LoggerFactory.getLogger(MoveHelper.class);

	private MoveHelper(OPClient client, String srcMsgId, String srcFldId, String dstFldId) {
		super(client);
		this.srcMsgId = srcMsgId;
		this.srcFldId = srcFldId;
		this.dstFldId = dstFldId;
	}

	public MoveHelper sync(MoveItemRequest request) throws Exception {
		logger.info("Synchronizing srcMsgId {} using MoveItems", srcMsgId);

		if (currentMoveResponse != null) {
			lastMoveResponse = new MoveItemsResponse(currentMoveResponse.getMoves());
		}

		Document moveItems = DOMUtils.createDoc("Move", "MoveItems");
		Element moveRoot = moveItems.getDocumentElement();

		if (!request.clientChangesMove().isEmpty()) {
			if (!request.clientChangesMove().isEmpty()) {
				for (int i = 0; i < request.clientChangesMove().size(); i++) {
					ClientMoveItems clientMove = request.clientChangesMove().get(0);
					NodeList children = clientMove.data().getDocumentElement().getChildNodes();
					for (int j = 0; j < children.getLength(); j++) {
						Node node = children.item(j);
						moveRoot.appendChild(moveItems.adoptNode(node.cloneNode(true)));
					}
				}
			}
		}

		logger.info(DOMUtils.logDom(moveItems));
		currentMoveResponse = client.moveSync(moveItems);

		dump();
		return this;
	}

	public MoveItemsResponseValidator startValidation() {
		return new MoveItemsResponseValidator(lastMoveResponse, currentMoveResponse, this);
	}

	public MoveHelper dump() {
		if (currentMoveResponse == null) {
			logger.info("MoveItems: {}, <empty response>", this.currentSyncKey);
			return this;
		}
		try {
			logger.info(DOMUtils.logDom(currentMoveResponse.dom));
		} catch (TransformerException e) {
			e.printStackTrace();
		}
		return this;
	}

	public static String forgeDstMsgId(String serverId, String dstFolderId) {
		return dstFolderId + EasTestHelper.SERVER_ID_SEPARATOR + serverId.split(EasTestHelper.SERVER_ID_SEPARATOR)[1];
	}

	public static class MoveHelperBuilder {

		protected OPClient client;
		protected ProtocolVersion protocolVersion;
		private String srcMsgId;
		private String srcFldId;
		private String dstFldId;

		public MoveHelperBuilder() {

		}

		public MoveHelperBuilder withAuth(String latd, String password) {
			this.client = new OPClient(latd, password, "devId", "iPad", "Apple-iPad3C1/1208.321",
					"http://localhost:8082/Microsoft-Server-ActiveSync");
			return this;
		}

		public MoveHelperBuilder withProtocolVersion(ProtocolVersion protocolVersion) {
			this.protocolVersion = protocolVersion;
			return this;
		}

		public MoveHelperBuilder withSrcFldId(String srcFldId) {
			this.srcFldId = srcFldId;
			return this;
		}

		public MoveHelperBuilder withSrcFldId(Long srcFldId) {
			this.srcFldId = srcFldId.toString();
			return this;
		}

		public MoveHelperBuilder withDstFldId(String dstFldId) {
			this.dstFldId = dstFldId;
			return this;
		}

		public MoveHelperBuilder withDstFldId(Long dstFldId) {
			this.dstFldId = dstFldId.toString();
			return this;
		}

		public MoveHelperBuilder withSrcMsgId(String srcMsgId) {
			this.srcMsgId = srcMsgId;
			return this;
		}

		public MoveHelperBuilder withSrcMsgId(Long srcMsgId) {
			this.srcMsgId = srcMsgId.toString();
			return this;
		}

		public MoveHelper build() {
			if (client == null || srcMsgId == null) {
				throw new IllegalStateException("MoveHelper needs client, srcMsgId");
			}

			if (protocolVersion != null) {
				client.setProtocolVersion(protocolVersion);
			} else {
				client.setProtocolVersion(ProtocolVersion.V161);
			}
			return new MoveHelper(client, srcMsgId, srcFldId, dstFldId);
		}

	}

	public static String extractSrcFldId(String serverId) {
		return serverId.split(SERVER_ID_SEPARATOR)[0];
	}

}
