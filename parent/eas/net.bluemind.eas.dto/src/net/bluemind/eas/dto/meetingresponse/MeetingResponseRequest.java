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
package net.bluemind.eas.dto.meetingresponse;

import java.util.Date;
import java.util.List;

public class MeetingResponseRequest {

	public static final class Request {

		public static enum UserResponse {

			Accepted(1), TentativelyAccepted(2), Declined(3);

			private final String xmlValue;

			private UserResponse(int value) {
				xmlValue = Integer.toString(value);
			}

			public String xmlValue() {
				return xmlValue;
			}

			public static UserResponse get(String value) {
				switch (value) {
				case "1":
					return Accepted;
				case "2":
					return TentativelyAccepted;
				case "3":
					return Declined;
				}
				return null;
			}

		}

		public UserResponse userResponse;
		public String collectionId;
		public String requestId;
		public Integer LongId; // unused
		public Date instanceId;
	}

	public List<Request> requests;
}
