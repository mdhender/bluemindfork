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
package net.bluemind.eas.dto.base;

import java.util.Collection;

public class AirSyncBaseResponse {

	public static final class Attachment {
		public enum Method {

			Normal(1), Embedded(5), OLE(6);

			private final String xmlValue;

			private Method(int value) {
				xmlValue = Integer.toString(value);
			}

			public String xmlValue() {
				return xmlValue;
			}

		}

		public String displayName;
		public String fileReference;
		public Method method;
		public Integer estimateDataSize;
		public String contentId;
		public boolean isInline;
	}

	public static class Body {
		public BodyType type;
		public Integer estimatedDataSize;
		public Boolean truncated;
		public DisposableByteSource data;
		public String preview;
		// itemOperationPart
		public boolean base64;

	}

	public static class BodyPart extends Body {

		public static enum Status {

			Success(1), TooLarge(176);

			private final String xmlValue;

			private Status(int value) {
				xmlValue = Integer.toString(value);
			}

			public String xmlValue() {
				return xmlValue;
			}

		}

		public Status status;

	}

	public enum NativeBodyType {

		PlainText(1), HTML(2), RTF(3);

		private final String xmlValue;

		private NativeBodyType(int value) {
			xmlValue = Integer.toString(value);
		}

		public String xmlValue() {
			return xmlValue;
		}

	}

	public Collection<Attachment> attachments;
	public Body body;
	public BodyPart bodyPart;
	public NativeBodyType nativeBodyType;
	public String contentType;

}
