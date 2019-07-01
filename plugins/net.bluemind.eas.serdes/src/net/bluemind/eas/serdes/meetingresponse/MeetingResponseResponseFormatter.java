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
package net.bluemind.eas.serdes.meetingresponse;

import net.bluemind.eas.dto.NamespaceMapping;
import net.bluemind.eas.dto.base.Callback;
import net.bluemind.eas.dto.meetingresponse.MeetingResponseResponse;
import net.bluemind.eas.dto.meetingresponse.MeetingResponseResponse.Result;
import net.bluemind.eas.serdes.IEasResponseFormatter;
import net.bluemind.eas.serdes.IResponseBuilder;

public class MeetingResponseResponseFormatter implements IEasResponseFormatter<MeetingResponseResponse> {

	@Override
	public void format(IResponseBuilder builder, double protocolVersion, MeetingResponseResponse response,
			Callback<Void> completion) {

		builder.start(NamespaceMapping.MeetingResponse);

		if (response.results != null && !response.results.isEmpty()) {
			for (Result res : response.results) {
				builder.container("Result");

				if (res.requestId != null) {
					builder.text("RequestId", res.requestId);
				}
				if (res.status != null) {
					builder.text("Status", res.status.xmlValue());
				}
				if (res.calendarId != null) {
					builder.text("CalendarId", res.calendarId);
				}
				builder.endContainer();
			}
		}

		builder.end(completion);
	}

}
