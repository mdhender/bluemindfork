/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2023
  *
  * This file is part of BlueMind. BlueMind is a messaging and collaborative
  * solution.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of either the GNU Affero General Public License as
  * published by the Free Software Foundation (version 3 of the License).
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.eas.dto.find;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import net.bluemind.eas.dto.base.Picture;
import net.bluemind.eas.dto.base.Range;
import net.bluemind.eas.dto.email.Importance;

public class FindResponse {

	public enum Status {

		SUCCESS(1), // Sync OK
		INVALID_REQUEST(2), // The client's search failed to validate.
		FOLDER_SYNC_REQUIRED(3), // The folder hierarchy is out of date.
		START_WITH_RANGE_ZERO(4); // The requested range does not begin with 0.

		private final String xmlValue;

		private Status(int value) {
			xmlValue = Integer.toString(value);
		}

		public String xmlValue() {
			return xmlValue;
		}

	}

	public static class Response {

		public static class Result {

			public static class Properties {
				public String subject; // NamespaceMapping.Email
				public Date dateReceived; // NamespaceMapping.Email
				public String displayTo; // NamespaceMapping.Email

				public String displayCc;
				public String displayBcc;

				public Importance importance; // NamespaceMapping.Email
				public boolean read; // NamespaceMapping.Email

				public boolean isDraft; // NamespaceMapping.Email2
				/**
				 * MS-ASCMD 2.2.3.137 Preview Optional, max lenght 255
				 */
				public String preview;
				public boolean hasAttachments;

				public String from; // NamespaceMapping.Email

				public String displayName; // NamespaceMapping.GAL
				public String phone; // NamespaceMapping.GAL
				public String office; // NamespaceMapping.GAL
				public String title; // NamespaceMapping.GAL
				public String company; // NamespaceMapping.GAL
				public String alias; // NamespaceMapping.GAL
				public String firstName; // NamespaceMapping.GAL
				public String lastName; // NamespaceMapping.GAL
				public String homePhone; // NamespaceMapping.GAL
				public String mobilePhone; // NamespaceMapping.GAL
				public String emailAddress; // NamespaceMapping.GAL
				public Picture picture; // NamespaceMapping.GAL
			}

			/**
			 * MS-ASCMD 2.2.3.27.1 Class (Find)
			 * 
			 * In Find command requests, the only supported value for the airsync:Class
			 * element is "Email".
			 */
			public String airsyncClass = "Email";
			/**
			 * CollectionItem collectionId:itemId
			 */
			public String serverId;
			public String collectionId;
			public Properties properties;
		}

		/**
		 * MS-ASCMD 2.2.3.178.1 Store (Find) In the Find command response, the value of
		 * the Store element will be "Mailbox".
		 */
		public String store = "Mailbox";
		public Status status;
		public List<Result> results = Collections.emptyList();
		public Range range;
		public Integer total;
	}

	public Status status;
	public Response response;

}
