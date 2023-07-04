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
package net.bluemind.eas.dto.folderupdate;

public class FolderUpdateResponse {

	public enum Status {

		SUCCESS(1), ALREADY_EXISTS(2), SYSTEM_FOLDER(3), DOES_NOT_EXIST(4), //
		PARENT_FOLDER_NOT_FOUND(5), SERVER_ERROR(6), INVALID_SYNC_KEY(9), INVALID_REQUEST(10), UNKNOWN_ERROR(11);

		private final String xmlValue;

		private Status(int value) {
			xmlValue = Integer.toString(value);
		}

		public String xmlValue() {
			return xmlValue;
		}

	}

	public Status status;
	public String syncKey;

}
