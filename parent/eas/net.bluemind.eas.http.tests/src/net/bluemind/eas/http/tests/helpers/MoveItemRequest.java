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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Document;

import net.bluemind.eas.http.tests.builders.DocumentRequestBuilder;
import net.bluemind.eas.http.tests.builders.DocumentRequestBuilder.TemplateKey;

public record MoveItemRequest(List<ClientMoveItems> clientChangesMove) {

	public static class MoveItemRequestBuilder {
		boolean getChanges = false;
		List<ClientMoveItems> clientMoveItems = new ArrayList<>();

		public MoveItemRequestBuilder withClientChangesMove(String serverId, String dstFldId) throws Exception {
			Map<TemplateKey, String> values = new HashMap<>();
			values.put(TemplateKey.SrcMsgId, serverId);
			values.put(TemplateKey.SrcFldId, MoveHelper.extractSrcFldId(serverId));
			values.put(TemplateKey.DstFldId, dstFldId);
			this.clientMoveItems.add(new ClientMoveItems(
					DocumentRequestBuilder.getDocumentRequestUpdate("MoveItemsRequest.xml", values)));
			return this;
		}

		public MoveItemRequest build() {
			return new MoveItemRequest(clientMoveItems);
		}

	}

	public MoveItemRequestBuilder copy() {
		MoveItemRequestBuilder moveItemRequestBuilder = new MoveItemRequestBuilder();
		moveItemRequestBuilder.clientMoveItems = clientChangesMove;
		return moveItemRequestBuilder;
	}

	@Override
	public String toString() {
		return getClass().getName();
	}

	public record ClientMoveItems(Document data) {
	}
}
