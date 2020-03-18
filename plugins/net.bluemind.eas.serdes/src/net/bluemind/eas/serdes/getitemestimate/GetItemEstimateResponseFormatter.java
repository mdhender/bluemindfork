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
package net.bluemind.eas.serdes.getitemestimate;

import net.bluemind.eas.dto.NamespaceMapping;
import net.bluemind.eas.dto.base.Callback;
import net.bluemind.eas.dto.getitemestimate.GetItemEstimateResponse;
import net.bluemind.eas.dto.getitemestimate.GetItemEstimateResponse.Response;
import net.bluemind.eas.dto.getitemestimate.GetItemEstimateResponse.Response.Status;
import net.bluemind.eas.serdes.IEasResponseFormatter;
import net.bluemind.eas.serdes.IResponseBuilder;

public class GetItemEstimateResponseFormatter implements IEasResponseFormatter<GetItemEstimateResponse> {

	@Override
	public void format(IResponseBuilder builder, double protocolVersion, GetItemEstimateResponse response,
			Callback<Void> completion) {
		builder.start(NamespaceMapping.GetItemEstimate);

		if (response.responses != null && !response.responses.isEmpty()) {

			for (Response r : response.responses) {
				builder.container("Response");
				builder.text("Status", r.status.xmlValue());
				builder.container("Collection");
				builder.text("CollectionId", r.collectionId.getValue());
				if (r.status == Status.Success) {
					builder.text("Estimate", "1");
				} else {
					builder.text("Estimate", "0");
				}
				builder.endContainer().endContainer();
			}

		}
		builder.end(completion);
	}

}
