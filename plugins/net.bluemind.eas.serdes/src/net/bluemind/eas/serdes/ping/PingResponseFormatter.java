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
package net.bluemind.eas.serdes.ping;

import net.bluemind.eas.dto.NamespaceMapping;
import net.bluemind.eas.dto.base.Callback;
import net.bluemind.eas.dto.ping.PingResponse;
import net.bluemind.eas.serdes.IEasResponseFormatter;
import net.bluemind.eas.serdes.IResponseBuilder;

public class PingResponseFormatter implements IEasResponseFormatter<PingResponse> {

	@Override
	public void format(IResponseBuilder builder, double protocolVersion, PingResponse response,
			Callback<Void> completion) {
		builder.start(NamespaceMapping.Ping);
		builder.text("Status", response.status.xmlValue());
		if (response.folders != null) {
			builder.container("Folders");
			for (String folder : response.folders.folders) {
				builder.text("Folder", folder);
			}
			builder.endContainer();
		}

		if (response.maxFolders != null) {
			builder.text("MaxFolders", response.maxFolders.toString());
		}

		if (response.heartbeatInterval != null) {
			builder.text("HeartbeatInterval", response.heartbeatInterval.toString());
		}
		builder.end(completion);
	}

}
