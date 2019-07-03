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
package net.bluemind.eas.dto.resolverecipients;

import java.util.List;

public class ResolveRecipientsResponse {

	public static enum Status {

		Success(1), ProtocolError(5), ServerError(6);

		private final String xmlValue;

		private Status(int value) {
			xmlValue = Integer.toString(value);
		}

		public String xmlValue() {
			return xmlValue;
		}

	}

	public static final class Response {
		public static enum Status {

			Success(1), Ambiguous(2), AmbiguousAndPartial(3), NotResolve(4);

			private final String xmlValue;

			private Status(int value) {
				xmlValue = Integer.toString(value);
			}

			public String xmlValue() {
				return xmlValue;
			}

		}

		public static final class Recipient {
			public static enum Type {

				GAL(1), Contact(2);

				private final String xmlValue;

				private Type(int value) {
					xmlValue = Integer.toString(value);
				}

				public String xmlValue() {
					return xmlValue;
				}

			}

			public static final class Availability {
				public static enum Status {

					Success(1), TooManyRecipients(160), TooManyDistributionGroup(161), TemporaryFailure(
							162), PermissionDenied(163);

					private final String xmlValue;

					private Status(int value) {
						xmlValue = Integer.toString(value);
					}

					public String xmlValue() {
						return xmlValue;
					}
				}

				public Status status;
				public String mergedFreeBusy;

			}

			public static final class Certificate {
				public static enum Status {

					Success(1), InvalidSMIMECertificate(7), Error(8);

					private final String xmlValue;

					private Status(int value) {
						xmlValue = Integer.toString(value);
					}

					public String xmlValue() {
						return xmlValue;
					}

				}

				public Status status;
				public Integer certificateCount;
				public Integer recipientCount;
				public String certificate;
				public String miniCertificate;

			}

			public static final class Picture {

				public static enum Status {

					Success(1), NoPhoto(173), MaxSizeExceeded(174), //
					MaxPicturesExceeded(175);

					private final String xmlValue;

					private Status(int value) {
						xmlValue = Integer.toString(value);
					}

					public String xmlValue() {
						return xmlValue;
					}

				}

				public Status status;
				public String data;

			}

			public Type type;
			public String displayName;
			public String emailAddress;
			public Availability availability;
			public Certificate certificate;
			public Picture picture;
			// volatile to make it explicit that it is not part of the
			// response
			// transmitted on the wire
			public volatile String entryUid;
			public volatile String to;
		}

		public String to;
		public Status status;
		public Integer recipientCount;
		public List<Recipient> recipients;
	}

	public Status status;
	public List<Response> responses;

}
