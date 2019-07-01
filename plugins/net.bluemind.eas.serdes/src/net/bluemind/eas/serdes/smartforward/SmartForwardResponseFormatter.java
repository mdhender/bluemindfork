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
package net.bluemind.eas.serdes.smartforward;

import net.bluemind.eas.dto.NamespaceMapping;
import net.bluemind.eas.dto.base.Callback;
import net.bluemind.eas.dto.smartforward.SmartForwardResponse;
import net.bluemind.eas.serdes.IEasResponseFormatter;
import net.bluemind.eas.serdes.IResponseBuilder;

public class SmartForwardResponseFormatter implements IEasResponseFormatter<SmartForwardResponse> {

	@Override
	public void format(IResponseBuilder builder, double protocolVersion, SmartForwardResponse response,
			Callback<Void> completion) {
		if (response == null) {
			completion.onResult(null);
		} else {
			builder.start(NamespaceMapping.SmartForward);
			builder.text("Status", response.status.xmlValue());
			builder.end(completion);
		}
	}

}
