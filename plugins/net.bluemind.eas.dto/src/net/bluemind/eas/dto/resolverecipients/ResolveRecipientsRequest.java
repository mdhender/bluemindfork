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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ResolveRecipientsRequest {

	public static final class Options {
		public static enum CertificateRetrieval {

			DoNotRetrieveCertificate(1), RetrieveFullCertificate(2), RetrieveMiniCertificate(3);

			private final String xmlValue;

			private CertificateRetrieval(int value) {
				xmlValue = Integer.toString(value);
			}

			public String xmlValue() {
				return xmlValue;
			}

			public static CertificateRetrieval get(String value) {
				switch (value) {
				case "1":
					return DoNotRetrieveCertificate;
				case "2":
					return RetrieveFullCertificate;
				case "3":
					return RetrieveMiniCertificate;
				}
				return DoNotRetrieveCertificate;
			}
		}

		public static final class Availability {
			public Date startTime;
			public Date endTime;
		}

		public static final class Picture {
			public Integer maxSize;
			public Integer maxPictures;
		}

		public CertificateRetrieval certificateRetrieval;
		public Integer maxCertificates;
		public Integer maxAmbiguousRecipients;
		public Availability availability;
		public Picture picture;
	}

	public List<String> to = new ArrayList<String>();
	public Options options;
}
