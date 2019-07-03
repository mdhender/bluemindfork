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
package net.bluemind.eas.serdes.moveitems;

import net.bluemind.eas.dto.NamespaceMapping;
import net.bluemind.eas.dto.base.Callback;
import net.bluemind.eas.dto.moveitems.MoveItemsResponse;
import net.bluemind.eas.dto.moveitems.MoveItemsResponse.Response;
import net.bluemind.eas.serdes.IEasResponseFormatter;
import net.bluemind.eas.serdes.IResponseBuilder;

public class MoveItemsFormatter implements IEasResponseFormatter<MoveItemsResponse> {

	@Override
	public void format(IResponseBuilder builder, double protocolVersion, MoveItemsResponse response,
			Callback<Void> completion) {
		builder.start(NamespaceMapping.MoveItems);
		for (Response item : response.moveItems) {
			builder.container("Response");
			builder.text("SrcMsgId", item.srcMsgId);
			builder.text("Status", item.status.xmlValue());
			if (item.dstMsgId != null) {
				builder.text("DstMsgId", item.dstMsgId);
			}
			builder.endContainer();
		}
		builder.end(completion);
	}

}
